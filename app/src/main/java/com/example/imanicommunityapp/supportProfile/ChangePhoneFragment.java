package com.example.imanicommunityapp.supportProfile;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.imanicommunityapp.Network.ConnectivityChecker;
import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.auth.Repository.UserProfileRepository;
import com.example.imanicommunityapp.settings.SettingsModels;
import com.example.imanicommunityapp.settings.SettingsRepository;

/**
 * Production phone change: OTP to the NEW number, commit only after confirm.
 *
 * <p>Offline: blocked (phone is login identity — never queued).
 * <p>Room account phone updates only after confirm success.
 */
public class ChangePhoneFragment extends Fragment {

    private EditText etCountryCode;
    private EditText etPhoneNumber;
    private EditText etVerifyCode;
    private Button btnConfirm;
    private Button btnVerify;
    private LinearLayout verifyContainer;
    private TextView tvResult;
    private UserProfileRepository userProfileRepository;
    private SettingsRepository settingsRepository;
    private ConnectivityChecker connectivityChecker;

    /** Bound to the in-flight challenge after request succeeds. */
    private String challengeId;
    private String mutationId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.editingphonenumber, container, false);

        userProfileRepository = new UserProfileRepository(requireContext());
        settingsRepository = new SettingsRepository(requireContext());
        connectivityChecker = new ConnectivityChecker(requireContext());

        etCountryCode = view.findViewById(R.id.etCountryCode);
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber);
        etVerifyCode = view.findViewById(R.id.etVerifyCode);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        btnVerify = view.findViewById(R.id.btnVerify);
        verifyContainer = view.findViewById(R.id.verifyContainer);
        tvResult = view.findViewById(R.id.tvResult);

        etCountryCode.setText("+254");
        loadSavedProfile();

        // Step 1 — request OTP to new number
        btnConfirm.setOnClickListener(v -> sendVerificationCode());
        // Step 2 — confirm OTP and commit
        btnVerify.setOnClickListener(v -> verifyCode());

        return view;
    }

    private void sendVerificationCode() {
        if (!connectivityChecker.hasInternetConnection()) {
            showResult("Connect to the internet to change your phone number.", false);
            return;
        }

        String code = etCountryCode.getText().toString().trim();
        String number = etPhoneNumber.getText().toString().trim();
        if (number.isEmpty()) {
            Toast.makeText(getContext(), "Enter phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        String full = code + number;
        btnConfirm.setEnabled(false);
        showResult("Sending code...", true);

        settingsRepository.requestPhoneChange(full, new SettingsRepository.DataCallback<>() {
            @Override
            public void onSuccess(SettingsModels.PhoneRequestResult data) {
                btnConfirm.setEnabled(true);
                challengeId = data.challengeId;
                mutationId = data.mutationId;
                showVerificationBox();
                String masked = data.maskedDestination != null ? data.maskedDestination : "your new number";
                showResult("Code sent to " + masked + ". Enter OTP to finish.", true);
            }

            @Override
            public void onError(@NonNull String message) {
                btnConfirm.setEnabled(true);
                showResult(message, false);
            }
        });
    }

    private void verifyCode() {
        if (!connectivityChecker.hasInternetConnection()) {
            showResult("Connect to verify your new number.", false);
            return;
        }
        String code = etVerifyCode.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(getContext(), "Enter verification code", Toast.LENGTH_SHORT).show();
            return;
        }
        if (challengeId == null || mutationId == null) {
            showResult("Request a code first.", false);
            return;
        }

        btnVerify.setEnabled(false);
        settingsRepository.confirmPhoneChange(challengeId, code, mutationId, new SettingsRepository.SimpleCallback() {
            @Override
            public void onSuccess(@Nullable String message) {
                btnVerify.setEnabled(true);
                showResult(message != null ? message : "Phone number updated. Use it next time you sign in.", true);
                // Reload current account phone from Room (server-confirmed).
                loadSavedProfile();
                challengeId = null;
            }

            @Override
            public void onError(@NonNull String message) {
                btnVerify.setEnabled(true);
                showResult(message, false);
            }
        });
    }

    private void loadSavedProfile() {
        userProfileRepository.getUserProfile(profile -> {
            if (profile != null && profile.phoneNo != null) {
                // Show current account phone for reference (not the draft).
                String phone = profile.phoneNo;
                if (phone.startsWith("+254") && phone.length() > 4) {
                    etPhoneNumber.setText(phone.substring(4));
                } else {
                    etPhoneNumber.setText(phone);
                }
            }
        });
    }

    private void showVerificationBox() {
        verifyContainer.setVisibility(View.VISIBLE);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(verifyContainer, "alpha", 0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.start();
    }

    private void showResult(String message, boolean success) {
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText(message);
        tvResult.setTextColor(getResources().getColor(
                success ? R.color.blue_primary : R.color.black, null));
    }
}
