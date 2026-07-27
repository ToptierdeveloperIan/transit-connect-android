package com.example.imanicommunityapp.Network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks <em>where the user is</em> for contextual offline UI.
 *
 * <p>Uses a stack so nested screens restore the previous scope on leave
 * (e.g. Settings hub → Change phone → back to hub).
 *
 * <p><b>This class does not touch Fragments or Navigation.</b>
 * Feature screens call {@link #enter(OfflineScope)} / {@link #leave(OfflineScope)}
 * from their lifecycle. That keeps the Network module free of UI dependencies.
 *
 * <p><b>Thread-safety:</b> enter/leave are synchronized; listeners fire on the
 * calling thread — prefer main-thread lifecycle calls (standard for Fragments).
 *
 * @see OfflineScope
 * @see ContextualOfflineController
 */
public final class OfflineScopeTracker {

    private static final String TAG = "OfflineScopeTracker";

    public interface ScopeListener {
        /**
         * Fired when the effective (top-of-stack) scope changes.
         *
         * @param scope new effective scope (never null; {@link OfflineScope#GENERIC} if empty)
         */
        void onScopeChanged(@NonNull OfflineScope scope);
    }

    private static volatile OfflineScopeTracker instance;

    private final Deque<OfflineScope> stack = new ArrayDeque<>();
    private final Set<ScopeListener> listeners = new CopyOnWriteArraySet<>();

    private OfflineScopeTracker() {
    }

    /**
     * Process-wide tracker (no Context required).
     */
    @NonNull
    public static OfflineScopeTracker getInstance() {
        if (instance == null) {
            synchronized (OfflineScopeTracker.class) {
                if (instance == null) {
                    instance = new OfflineScopeTracker();
                }
            }
        }
        return instance;
    }

    /**
     * Push a scope when a screen becomes visible / primary.
     * Call from {@code onResume} or {@code onViewCreated}.
     */
    public void enter(@NonNull OfflineScope scope) {
        OfflineScope previous;
        OfflineScope next;
        synchronized (this) {
            previous = peekLocked();
            stack.push(scope);
            next = scope;
            Log.d(TAG, "enter " + scope + " (depth=" + stack.size() + ")");
        }
        if (previous != next) {
            dispatch(next);
        }
    }

    /**
     * Pop a scope when leaving a screen. Only removes the top if it matches
     * {@code scope} (safe if lifecycle is slightly out of order).
     */
    public void leave(@NonNull OfflineScope scope) {
        OfflineScope previous;
        OfflineScope next;
        synchronized (this) {
            previous = peekLocked();
            if (!stack.isEmpty() && stack.peek() == scope) {
                stack.pop();
            } else {
                // Remove first matching from top side without clearing whole stack.
                Deque<OfflineScope> tmp = new ArrayDeque<>();
                boolean removed = false;
                while (!stack.isEmpty()) {
                    OfflineScope s = stack.pop();
                    if (!removed && s == scope) {
                        removed = true;
                        continue;
                    }
                    tmp.push(s);
                }
                while (!tmp.isEmpty()) {
                    stack.push(tmp.pop());
                }
                if (!removed) {
                    Log.w(TAG, "leave ignored; scope not on stack: " + scope);
                }
            }
            next = peekLocked();
            Log.d(TAG, "leave " + scope + " → effective=" + next + " (depth=" + stack.size() + ")");
        }
        if (previous != next) {
            dispatch(next);
        }
    }

    /**
     * Replace entire stack with a single scope (e.g. after nav graph jump).
     */
    public void replaceWith(@NonNull OfflineScope scope) {
        OfflineScope previous;
        synchronized (this) {
            previous = peekLocked();
            stack.clear();
            stack.push(scope);
        }
        if (previous != scope) {
            dispatch(scope);
        }
    }

    /**
     * Clear stack to {@link OfflineScope#GENERIC}.
     */
    public void clear() {
        OfflineScope previous;
        synchronized (this) {
            previous = peekLocked();
            stack.clear();
        }
        if (previous != OfflineScope.GENERIC) {
            dispatch(OfflineScope.GENERIC);
        }
    }

    /**
     * Effective scope for policy resolution.
     */
    @NonNull
    public OfflineScope getCurrentScope() {
        synchronized (this) {
            return peekLocked();
        }
    }

    public void addListener(@NonNull ScopeListener listener) {
        listeners.add(listener);
        // Immediate snapshot so observers do not wait for a transition.
        final OfflineScope current = getCurrentScope();
        listener.onScopeChanged(current);
    }

    public void removeListener(@NonNull ScopeListener listener) {
        listeners.remove(listener);
    }

    @NonNull
    private OfflineScope peekLocked() {
        OfflineScope top = stack.peek();
        return top != null ? top : OfflineScope.GENERIC;
    }

    private void dispatch(@NonNull OfflineScope scope) {
        for (ScopeListener listener : listeners) {
            try {
                listener.onScopeChanged(scope);
            } catch (Exception ex) {
                Log.e(TAG, "ScopeListener error", ex);
            }
        }
    }
}
