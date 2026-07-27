package com.example.imanicommunityapp.Sync.Queue;

import java.util.ArrayList;
import java.util.List;

public class QueueRepository {
    // Database access object
    private final ActionDao actionDao;

    // Runtime cache
    private final List<PendingAction> memoryQueue = new ArrayList<>();
    private boolean queueHydrated = false;

    // Constructor
    public QueueRepository(ActionDao actionDao) {
        this.actionDao = actionDao;
    }

    // rehydrate from DB on startup - this can be called multiple times but will only hit the DB once.
    public void initialize() {
        if (!queueHydrated) {
            List<PendingAction> actions = actionDao.getPending();
            memoryQueue.addAll(actions);
            queueHydrated = true;
        }
    }

    // Enqueue a new action and persist it to the database. This method should be called from a background thread.
    public void enqueue(PendingAction action) {


        // 1. Update RAM - however this needs to be overriden so that we can ensure the list is accessible first.
        memoryQueue.add(action);

        // 2. Persist
        actionDao.insert(action);
    }


    public List<PendingAction> getPendingActions() {
        // Fast path
        //if (!memoryQueue.isEmpty()) {
       //     return new ArrayList<>(memoryQueue);
       // }



        return new ArrayList<>(memoryQueue);
    }


    public boolean contains(String type, String payload) {

        // Fast lookup
        for (PendingAction action : memoryQueue) {
            if (action.matches(type, payload)) {
                return true;
            }
        }

        // Fallback to database
        return actionDao.exists(type, payload);
    }


    public void updateState(
            long id,
            QueueItemState state
    ) {
        // RAM
        PendingAction action = find(id);
        if (action != null) {
            action.setState(state);
        }

        // Persistence
        actionDao.updateState(id, state);
    }


    public void remove(long id) {
        memoryQueue.removeIf(a -> a.id == id);
        actionDao.delete(id);
    }
}
