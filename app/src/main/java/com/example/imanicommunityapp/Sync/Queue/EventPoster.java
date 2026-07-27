package com.example.imanicommunityapp.Sync.Queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Simple producer API that inserts events into the DB-backed queue.
 */
public class EventPoster {
    private final EventQueueDb db;
    private final int maxRetries;

    public EventPoster(@NonNull EventQueueDb db) {
        this(db, 3);
    }

    public EventPoster(@NonNull EventQueueDb db, int maxRetries) {
        this.db = db;
        this.maxRetries = Math.max(1, maxRetries);
    }

    /**
     * Try to post an event; returns true on success.
     * This method is synchronous and will block while the DB write completes.
     */
    public boolean post(@NonNull EventItem event) {
        int attempt = 0;
        while (attempt < maxRetries) {
            attempt++;
            long id = db.insert(event);
            if (id > 0) {
                event.setId(id);
                return true;
            }
            // small backoff
            try { Thread.sleep(100L * attempt); } catch (InterruptedException ignored) {}
        }
        return false;
    }
}
