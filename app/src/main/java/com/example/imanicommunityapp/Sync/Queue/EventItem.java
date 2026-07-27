package com.example.imanicommunityapp.Sync.Queue;

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Simple event model stored in-memory and persisted to SQLite.
 */
public class EventItem implements Comparable<EventItem> {
    private long id; // 0 means not yet persisted
    private long timestamp; // epoch millis
    @Nullable
    private String payload; // simple textual payload; can be null
    @Nullable
    private String uid; // optional unique id provided by producer

    public EventItem(long timestamp, @Nullable String payload, @Nullable String uid) {
        this.id = 0L;
        this.timestamp = timestamp;
        this.payload = payload;
        this.uid = uid;
    }

    public EventItem(long id, long timestamp, @Nullable String payload, @Nullable String uid) {
        this.id = id;
        this.timestamp = timestamp;
        this.payload = payload;
        this.uid = uid;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Nullable
    public String getPayload() { return payload; }
    public void setPayload(@Nullable String payload) { this.payload = payload; }

    @Nullable
    public String getUid() { return uid; }
    public void setUid(@Nullable String uid) { this.uid = uid; }

    @Override
    public int compareTo(EventItem other) {
        if (other == null) return 1;
        int cmp = Long.compare(this.timestamp, other.timestamp);
        if (cmp != 0) return cmp;
        // tie-break: if both have ids > 0, prefer lower id; otherwise compare uid
        if (this.id > 0 && other.id > 0) return Long.compare(this.id, other.id);
        if (this.uid != null && other.uid != null) return this.uid.compareTo(other.uid);
        if (this.uid != null) return 1;
        if (other.uid != null) return -1;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventItem)) return false;
        EventItem that = (EventItem) o;
        return id == that.id && timestamp == that.timestamp && Objects.equals(payload, that.payload) && Objects.equals(uid, that.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, payload, uid);
    }

    @Override
    public String toString() {
        return "EventItem{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", uid='" + uid + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
