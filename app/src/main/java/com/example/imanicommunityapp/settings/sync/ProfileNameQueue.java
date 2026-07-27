package com.example.imanicommunityapp.settings.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.imanicommunityapp.Sync.Queue.EventItem;
import com.example.imanicommunityapp.Sync.Queue.EventPoster;
import com.example.imanicommunityapp.Sync.Queue.SqliteEventQueueDb;
import com.example.imanicommunityapp.settings.SettingsRepository;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline queue for PROFILE_NAME_UPDATE only.
 *
 * <p>Phone changes are intentionally never enqueued (OTP must be online).
 *
 * <p>Coalescing: when draining, only the latest name event is pushed; older
 * unprocessed name events with different mutation_ids are deleted after success
 * of the newest, or skipped if superseded.
 *
 * Payload is JSON in {@link EventItem#getPayload()} with type discriminator.
 */
public final class ProfileNameQueue {

    private static final String TAG = "ProfileNameQueue";
    public static final String TYPE_NAME = "PROFILE_NAME_UPDATE";

    private static volatile ProfileNameQueue INSTANCE;

    private final SqliteEventQueueDb db;
    private final EventPoster poster;
    private final Gson gson = new Gson();
    private final Context appContext;

    private ProfileNameQueue(Context context) {
        this.appContext = context.getApplicationContext();
        this.db = new SqliteEventQueueDb(appContext);
        this.poster = new EventPoster(db);
    }

    public static ProfileNameQueue getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ProfileNameQueue.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ProfileNameQueue(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Persist a name mutation for later replay. uid = mutation_id for dedupe visibility.
     */
    public void enqueueNameUpdate(
            @NonNull String firstName,
            @NonNull String secondName,
            @NonNull String mutationId,
            int baseVersion
    ) {
        NamePayload payload = new NamePayload(TYPE_NAME, firstName, secondName, mutationId, baseVersion);
        String json = gson.toJson(payload);
        EventItem item = new EventItem(System.currentTimeMillis(), json, mutationId);
        boolean ok = poster.post(item);
        Log.d(TAG, "enqueue name mutation_id=" + mutationId + " ok=" + ok);
    }

    /**
     * Drain name events: coalesce to latest by timestamp, push once, delete processed.
     */
    public void drain(@NonNull Runnable onComplete) {
        new Thread(() -> {
            try {
                List<EventItem> batch = db.fetchNext(50);
                if (batch == null || batch.isEmpty()) {
                    onComplete.run();
                    return;
                }

                List<EventItem> nameEvents = new ArrayList<>();
                for (EventItem e : batch) {
                    NamePayload p = parse(e);
                    if (p != null && TYPE_NAME.equals(p.type)) {
                        nameEvents.add(e);
                    }
                }
                if (nameEvents.isEmpty()) {
                    onComplete.run();
                    return;
                }

                // Latest wins (list is timestamp ASC — take last).
                EventItem latest = nameEvents.get(nameEvents.size() - 1);
                NamePayload payload = parse(latest);
                if (payload == null) {
                    onComplete.run();
                    return;
                }

                SettingsRepository repo = new SettingsRepository(appContext);
                final Object lock = new Object();
                final boolean[] done = {false};

                repo.pushNameMutation(
                        payload.firstName,
                        payload.secondName,
                        payload.mutationId,
                        payload.baseVersion,
                        new SettingsRepository.SimpleCallback() {
                            @Override
                            public void onSuccess(String message) {
                                // Drop all name events we coalesced (including older).
                                List<Long> ids = new ArrayList<>();
                                for (EventItem e : nameEvents) {
                                    if (e.getId() > 0) {
                                        ids.add(e.getId());
                                    }
                                }
                                if (!ids.isEmpty()) {
                                    db.deleteByIds(ids);
                                }
                                Log.i(TAG, "Name queue drained: " + message);
                                synchronized (lock) {
                                    done[0] = true;
                                    lock.notifyAll();
                                }
                            }

                            @Override
                            public void onError(@NonNull String message) {
                                Log.w(TAG, "Name queue drain failed (will retry later): " + message);
                                synchronized (lock) {
                                    done[0] = true;
                                    lock.notifyAll();
                                }
                            }
                        });

                synchronized (lock) {
                    if (!done[0]) {
                        lock.wait(30_000);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "drain error", ex);
            } finally {
                onComplete.run();
            }
        }, "ProfileNameQueueDrain").start();
    }

    private NamePayload parse(EventItem e) {
        if (e == null || e.getPayload() == null) {
            return null;
        }
        try {
            return gson.fromJson(e.getPayload(), NamePayload.class);
        } catch (Exception ex) {
            Log.w(TAG, "bad payload", ex);
            return null;
        }
    }

    /** JSON shape stored in the event queue payload column. */
    public static class NamePayload {
        @SerializedName("type")
        public String type;
        @SerializedName("first_name")
        public String firstName;
        @SerializedName("second_name")
        public String secondName;
        @SerializedName("mutation_id")
        public String mutationId;
        @SerializedName("base_version")
        public int baseVersion;

        public NamePayload(String type, String firstName, String secondName, String mutationId, int baseVersion) {
            this.type = type;
            this.firstName = firstName;
            this.secondName = secondName;
            this.mutationId = mutationId;
            this.baseVersion = baseVersion;
        }
    }
}
