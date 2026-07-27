package com.example.imanicommunityapp.Sync.Queue;



import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Coordinates enqueue/dequeue of sync work. Keeps queue logic separate from the
 * DataSyncStatus state machine. Uses ActionQueueRepository (Room-backed) for persistence.
 */
public class SyncQueueManager {
    // Source of truth for pending actions, backed by Room DB.
    private final ActionQueueRepository actionRepo;
    // Our executor for marking actions processed.
    private final ExecutorService executor;

    // constructor
    public SyncQueueManager(ActionQueueRepository actionRepo, ExecutorService executor) {
        this.actionRepo = actionRepo;
        this.executor = executor;
    }

    /**
     * Enqueue a sync action (idempotent payload recommended).
     */
    // Class method signature definition.
    public void enqueueSync(String type, String payloadJson) {
        PendingAction action = new PendingAction();
        action.type = type;
        action.payloadJson = payloadJson;
        action.createdAt = System.currentTimeMillis();
        action.processed = false;
        // Persist on repo's executor/thread
        actionRepo.enqueue(action);
    }

    /**
     * Check whether an equivalent unprocessed action is already queued.
     * Results delivered on the repo's callback thread.
     */
    // Idempotency very important here. We don't want to enqueue duplicate actions.
    public void isQueued(String type, String payloadJson, ActionQueueRepository.Callback<Boolean> callback) {
        actionRepo.fetchUnprocessed(actions -> {
            boolean found = false;
            if (actions != null) {
                for (PendingAction a : actions) {
                    if (type.equals(a.type) && payloadJson.equals(a.payloadJson)) {
                        found = true;
                        break;
                    }
                }
            }
            callback.onResult1(found);
        });
    }

    /**
     * Mark a specific action as processed.
     * Call from the executor handling the sync processing or via repo.
     */
    //This is very vague and needs to be made unambiguous. We need to be able to mark an action as processed after we successfully process it in the sync engine
    public void markProcessed(PendingAction action) {
        executor.execute(() -> actionRepo.markProcessed(action));
    }

    /**
     * Drain/peek all pending actions for replay/rehydration. Uses the repo callback to return list.
     */
    public void fetchPending(ActionQueueRepository.Callback<List<PendingAction>> callback) {
        actionRepo.fetchUnprocessed(callback);
    }
    // Executor contract.
    public void queueExecutor(){

    }
}

