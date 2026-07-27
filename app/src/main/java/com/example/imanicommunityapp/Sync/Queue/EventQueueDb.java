package com.example.imanicommunityapp.Sync.Queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Database contract for the event queue. Implementations must be thread-safe or run on a single DB thread.
 */
public interface EventQueueDb {
    /**
     * Inserts an event and returns the assigned row id (>0) or -1 on failure.
     */
    long insert(@NonNull EventItem event);

    /**
     * Fetch up to `limit` earliest events ordered by timestamp ASC, id ASC.
     */
    @NonNull
    List<EventItem> fetchNext(int limit);

    /**
     * Delete the given ids in a single transaction.
     */
    void deleteByIds(@NonNull List<Long> ids);

    /**
     * Returns approximate count of events in the queue.
     */
    int count();

    /**
     * Remove all events older than the provided timestamp (exclusive).
     */
    void compactOlderThan(long timestamp);
}
