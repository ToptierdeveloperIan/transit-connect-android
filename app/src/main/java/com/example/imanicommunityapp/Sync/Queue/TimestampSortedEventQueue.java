package com.example.imanicommunityapp.Sync.Queue;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Consumer that polls the DB and optionally maintains a small in-memory sorted cache.
 */
public class TimestampSortedEventQueue {
    private static final String TAG = "TimestampSortedEventQueue";
    private final EventQueueDb db;
    private final int pollIntervalMs;
    private final int batchSize;
    private final Lock lock = new ReentrantLock();
    private final List<EventItem> inMemory = new ArrayList<>();
    private Timer timer;
    private EventProcessor processor;

    public interface EventProcessor {
        void process(@NonNull List<EventItem> events);
    }

    public TimestampSortedEventQueue(@NonNull EventQueueDb db) {
        this(db, 500, 50);
    }

    public TimestampSortedEventQueue(@NonNull EventQueueDb db, int pollIntervalMs, int batchSize) {
        this.db = db;
        this.pollIntervalMs = pollIntervalMs;
        this.batchSize = batchSize;
    }

    public void setProcessor(@Nullable EventProcessor processor) {
        this.processor = processor;
    }

    public void start() {
        if (timer != null) return;
        timer = new Timer("TimestampSortedEventQueueTimer");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    doPoll();
                } catch (Throwable t) {
                    Log.w(TAG, "poll failed", t);
                }
            }
        }, 0, pollIntervalMs);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void doPoll() {
        List<EventItem> items = db.fetchNext(batchSize);
        if (items.isEmpty()) return;
        // Optionally merge into in-memory list and pass to processor
        lock.lock();
        try {
            for (EventItem e : items) {
                // insert by binary search
                int idx = Collections.binarySearch(inMemory, e);
                if (idx < 0) idx = -(idx + 1);
                inMemory.add(idx, e);
            }
            // hand off copy to processor
            if (processor != null) {
                List<EventItem> toProcess = new ArrayList<>(inMemory);
                // release lock while calling processor for safety
                lock.unlock();
                try {
                    processor.process(toProcess);
                } finally {
                    lock.lock();
                }
                // on success, delete processed ids
                List<Long> ids = new ArrayList<>();
                for (EventItem e : toProcess) ids.add(e.getId());
                db.deleteByIds(ids);
                inMemory.clear();
            }
        } finally {
            if (lock.tryLock()) {
                try { lock.unlock(); } catch (IllegalMonitorStateException ignored) {}
            }
        }
    }

    /**
     * Synchronous poll that returns the next batch from DB (does not touch in-memory cache).
     */
    @NonNull
    public List<EventItem> pollOnce() {
        return db.fetchNext(batchSize);
    }

    /**
     * Acknowledge processed ids and delete them from DB.
     */
    public void ackProcessed(@NonNull List<Long> ids) {
        db.deleteByIds(ids);
    }
}
