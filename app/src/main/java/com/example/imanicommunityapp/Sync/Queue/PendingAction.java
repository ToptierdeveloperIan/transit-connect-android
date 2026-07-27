package com.example.imanicommunityapp.Sync.Queue;

public class PendingAction {

        long id;
        String type;
        String payloadJson;
        QueueState state;
        int retryCount;
        long nextAttemptAt;


        //constructor
        public PendingAction(long id, String type, String payloadJson, QueueState state, int retryCount, long nextAttemptAt) {
                this.id = id;
                this.type = type;
                this.payloadJson = payloadJson;
                this.state = state;
                this.retryCount = retryCount;
                this.nextAttemptAt = nextAttemptAt;
        }

}
