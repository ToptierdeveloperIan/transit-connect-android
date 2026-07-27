package com.example.imanicommunityapp.Sync.Queue;

public enum QueueState {

    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    RETRY_SCHEDULED,
    DEAD_LETTER,
    CANCELLED

}
