package com.example.imanicommunityapp.supportProfile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.auth.Repository.UserProfileRepository;
import com.example.imanicommunityapp.settings.SettingsRepository;

/**
 * Change display names.
 *
 * <p>Online → PATCH /api/settings/profile/name/
 * <p>Offline → Room pending + PROFILE_NAME_UPDATE queue (synced on reconnect).
 */
public class editingNameFragment extends Fragment {
    private static final String TAG = "editingNameFragment";

    private EditText editFirstName;
    private EditText editLastName;
    private Button btnConfirm;
    private TextView tvStatus;
    private ImageView backBtn;
    private UserProfileRepository userProfileRepository;
    private SettingsRepository settingsRepository;

    public editingNameFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.editingnames, container, false);

        editFirstName = view.findViewById(R.id.editFirstName);
        editLastName = view.findViewById(R.id.editLastName);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        tvStatus = view.findViewById(R.id.tvStatus);
        backBtn = view.findViewById(R.id.backBtn);

        userProfileRepository = new UserProfileRepository(requireContext());
        settingsRepository = new SettingsRepository(requireContext());
        loadSavedProfile();

        btnConfirm.setOnClickListener(v -> updateName());
        backBtn.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        return view;
    }

    private void updateName() {
        String first = editFirstName.getText().toString().trim();
        String last = editLastName.getText().toString().trim();

        if (first.isEmpty()) {
            editFirstName.setError("Please enter your first name");
            editFirstName.requestFocus();
            return;
        }
        if (last.isEmpty()) {
            editLastName.setError("Please enter your last name");
            editLastName.requestFocus();
            return;
        }

        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Saving...");
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_primary));
        btnConfirm.setEnabled(false);

        settingsRepository.updateName(first, last, new SettingsRepository.SimpleCallback() {
            @Override
            public void onSuccess(@Nullable String message) {
                btnConfirm.setEnabled(true);
                showStatus(message != null ? message : "SUCCESS", true);
            }

            @Override
            public void onError(@NonNull String message) {
                btnConfirm.setEnabled(true);
                showStatus(message, false);
            }
        });
    }

    private void loadSavedProfile() {
        userProfileRepository.getUserProfile(profile -> {
            if (profile == null) {
                return;
            }
            editFirstName.setText(profile.firstName);
            editLastName.setText(profile.secondName);
            if (profile.pendingNameMutation) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("Pending sync");
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_primary));
            }
            Log.d(TAG, "loaded profile version=" + profile.profileVersion
                    + " pending=" + profile.pendingNameMutation);
        });
    }

    private void showStatus(String message, boolean success) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
        tvStatus.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        success ? R.color.blue_primary : android.R.color.holo_red_light
                )
        );
    }
}
