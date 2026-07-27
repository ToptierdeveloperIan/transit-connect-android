package com.example.imanicommunityapp.support.terms;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.ui.terms.TermsComposeFragment;

/**
 * Java-facing gate: after auth, route to Compose Terms when {@code must_accept}
 * is true. On network failure we proceed into the app (availability over hard lock).
 */
public final class TermsGate {

    private static final String TAG = "TermsGate";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Destination {
        void goHome();

        void goDriverHome();
    }

    private TermsGate() {
    }

    /**
     * Check server status then navigate to Terms (requireAccept) or role home.
     */
    public static void routeAfterAuth(
            @NonNull Context context,
            @NonNull NavController navController,
            @NonNull String role,
            int termsActionId
    ) {
        TermsRepository repo = new TermsRepository(context);
        repo.fetchStatus(new TermsRepository.TermsCallback<>() {
            @Override
            public void onSuccess(TermsStatusDto data) {
                boolean mustAccept = data != null
                        && Boolean.TRUE.equals(data.getMustAccept());
                MAIN.post(() -> {
                    if (mustAccept) {
                        openTermsGate(navController, termsActionId);
                    } else {
                        goRoleHome(navController, role);
                    }
                });
            }

            @Override
            public void onError(@NonNull String message) {
                Log.w(TAG, "Terms status unavailable, proceeding: " + message);
                MAIN.post(() -> goRoleHome(navController, role));
            }
        });
    }

    private static void openTermsGate(NavController nav, int termsActionId) {
        Bundle args = new Bundle();
        args.putBoolean(TermsComposeFragment.ARG_REQUIRE_ACCEPT, true);
        try {
            nav.navigate(termsActionId, args);
        } catch (Exception e) {
            // Fallback by destination id if action missing
            Log.e(TAG, "navigate terms action failed, using destination id", e);
            nav.navigate(R.id.Terms_conditions, args);
        }
    }

    private static void goRoleHome(NavController nav, String role) {
        if (role != null && role.equalsIgnoreCase("driver")) {
            nav.navigate(R.id.driverhomeFragment);
        } else {
            nav.navigate(R.id.homeFragment);
        }
    }
}
