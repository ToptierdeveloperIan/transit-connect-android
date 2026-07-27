package com.example.imanicommunityapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.imanicommunityapp.auth.Repository.TokenManager;
import com.example.imanicommunityapp.support.terms.TermsGate;

public class SplashFragment extends Fragment {

    private TokenManager tokenManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.splashfragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = new TokenManager(requireContext());

        handler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            String accessToken = tokenManager.getAccessToken();
            NavController navController = Navigation.findNavController(view);
            String role = tokenManager.getUserRole();

            if (accessToken == null || accessToken.isEmpty()) {
                navController.navigate(R.id.action_splashFragment_to_VerificationFragment);
                return;
            }

            // Logged-in: enforce Terms acceptance before home/driver home.
            TermsGate.routeAfterAuth(
                    requireContext(),
                    navController,
                    role != null ? role : "user",
                    R.id.action_splashFragment_to_terms
            );
        }, 2500);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
