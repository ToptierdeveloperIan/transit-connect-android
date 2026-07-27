package com.example.imanicommunityapp.auth.View;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.auth.ViewModel.AuthViewModel;
import com.example.imanicommunityapp.auth.Repository.TokenManager;
import com.example.imanicommunityapp.support.terms.TermsGate;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class VerificationFragment extends Fragment {

    private TextInputEditText phoneInput;
    private TextInputEditText otpInput;
    private TextInputLayout otpInputLayout;
    private Button verifyBtn;
    private TextView signupNotice;

    private AuthViewModel authViewModel;
    private TokenManager tokenManager;

    public VerificationFragment() {
        super(R.layout.verification_fragment);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---- Bind views from XML ----
        phoneInput = view.findViewById(R.id.username_text);
        otpInput = view.findViewById(R.id.otp_text);
        otpInputLayout = view.findViewById(R.id.otpInputLayout);
        verifyBtn = view.findViewById(R.id.verify);
        signupNotice = view.findViewById(R.id.signup_notice);

        // ---- Initial UI state ----
        otpInputLayout.setVisibility(View.GONE);
        signupNotice.setVisibility(View.GONE);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        tokenManager = new TokenManager(requireContext());
        observeViewModel();

        verifyBtn.setOnClickListener(v -> {
            Boolean otpWasRequested = authViewModel.getOtpRequested().getValue();
            if (!Boolean.TRUE.equals(otpWasRequested)) {
                requestOtp();
            } else {
                verifyOtp();
            }
        });
    }

    // =========================
    // STEP 1: REQUEST OTP
    // =========================
    private void requestOtp() {
        String phone = phoneInput.getText().toString().trim();

        if (phone.isEmpty()) {
            phoneInput.setError("Phone number required");
            return;
        }

        authViewModel.requestOtp(phone);
    }

    // =========================
    // STEP 2: VERIFY OTP + LOGIN
    // =========================
    private void verifyOtp() {
        String phone = phoneInput.getText().toString().trim();
        String otp = otpInput.getText().toString().trim();

        if (otp.isEmpty()) {
            otpInput.setError("OTP required");
            return;
        }

        authViewModel.loginWithOtp(phone, otp);
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(getViewLifecycleOwner(), isLoading ->
                verifyBtn.setEnabled(!Boolean.TRUE.equals(isLoading)));

        authViewModel.getOtpRequested().observe(getViewLifecycleOwner(), requested -> {
            boolean showOtp = Boolean.TRUE.equals(requested);
            otpInputLayout.setVisibility(showOtp ? View.VISIBLE : View.GONE);
            verifyBtn.setText(showOtp ? "Verify OTP" : "Request OTP");
        });

        authViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isEmpty()) {
                return;
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            authViewModel.onMessageShown();
        });

        authViewModel.getLoginSuccessful().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                navigateBasedOnSavedRole();
                authViewModel.onNavigationHandled();
            }
        });
    }

    // =========================
    // NAVIGATION
    // =========================
    private void navigateBasedOnSavedRole() {
        String role = tokenManager.getUserRole();

        if (role == null) {
            Toast.makeText(requireContext(), "Role missing. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        NavController nav = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        // Gate: require current Terms version before home (Compose Terms screen).
        TermsGate.routeAfterAuth(
                requireContext(),
                nav,
                role,
                R.id.action_verificationFragment_to_terms
        );
    }
}
