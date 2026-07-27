package com.example.imanicommunityapp.supportProfile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.auth.Repository.TokenManager;
import com.example.imanicommunityapp.auth.Repository.UserProfileRepository;

public class SettingsFragment extends Fragment {

    private ImageView profileImage;
    private TextView usernameText;
    private View v;
    private View changeNameItem;
    private View changePhoneItem;
    private View changeEmailItem;
    private TokenManager tokenManager;
    private UserProfileRepository userProfileRepository;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout file (fragment_settings.xml)
        return inflater.inflate(R.layout.fragment_settings, container, false);


    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize TokenManager
        tokenManager = new TokenManager(requireContext());
        userProfileRepository = new UserProfileRepository(requireContext());

        // ---- Profile Card ----
        profileImage = view.findViewById(R.id.profile_image);
        usernameText = view.findViewById(R.id.username_text);
        changeNameItem = view.findViewById(R.id.ChangeName);
        changePhoneItem = view.findViewById(R.id.ChangePhoneNo);
        changeEmailItem = view.findViewById(R.id.ChangeEmail);


        if (profileImage != null) {
            profileImage.setImageResource(R.drawable.personicon);
        }
        if (usernameText != null) {
            usernameText.setText("");
        }

        loadUserProfile();

        // ---- Setup all sections ----
        setupProfileSection(view);
        setupSupportSection(view);

        // Drain offline name mutations when Settings opens online.
        com.example.imanicommunityapp.settings.sync.ProfileNameQueue
                .getInstance(requireContext())
                .drain(() -> { });

        // Refresh profile snapshot so version + pending badge stay honest.
        new com.example.imanicommunityapp.settings.SettingsRepository(requireContext())
                .refreshProfile(new com.example.imanicommunityapp.settings.SettingsRepository.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        loadUserProfile();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        // Offline: keep Room projection.
                    }
                });
    }

    /**
     * Sets up the Profile section (Change Name, Change Phone, Change Email)
     * plus Wallet entry (Compose wallet UI → backend /api/wallet/).
     */
    private void setupProfileSection(View view) {
        setupItem(view, R.id.ChangeName, R.drawable.personicon, "Change Name", v -> openChangeName());
        setupItem(view, R.id.ChangePhoneNo, R.drawable.personicon, "Change Phone Number", v -> openChangePhone());
        setupItem(view, R.id.ChangeEmail, R.drawable.personicon, "Change Email", v -> openChangeEmail());
        // Wallet row only — do not repurpose the drawer nav "wallet" payment dialog.
        setupItem(view, R.id.Wallet, R.drawable.personicon, "Wallet", v -> openWallet());
    }

    /**
     * Sets up the Support & Legal section (Help, Terms, Privacy, Logout)
     */
    private void setupSupportSection(View view) {
        setupItem(view, R.id.Help_support, R.drawable.personicon, "Help & Support", v -> openSupportPage());
        setupItem(view, R.id.Terms_conditions, R.drawable.personicon, "Terms & Conditions", v -> openTermsPage());
        setupItem(view, R.id.Privacy_policy, R.drawable.personicon, "Privacy Policy", v -> openPrivacyPage());
        setupItem(view, R.id.logout, R.drawable.personicon, "Logout", v -> performLogout());
    }

    /**
     * Generic method to initialize each settings row (from the included layout).
     */
    private void setupItem(View root, int id, int iconRes, String title, View.OnClickListener listener) {
        View item = root.findViewById(id);
        if (item == null) return;

        ImageView icon = item.findViewById(R.id.item_icon);
        TextView titleView = item.findViewById(R.id.item_title);

        if (icon != null) icon.setImageResource(iconRes);
        if (titleView != null) titleView.setText(title);

        item.setOnClickListener(listener);
    }

    // ---- Profile Actions ----
    private void openChangeName() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_settingsFragment_to_ChangeName);
    }

    private void openChangePhone() {
        // OTP-gated phone change (online only) — UserSettings backend.
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_settingsFragment_to_changePhoneNumber);
    }

    private void openChangeEmail() {
        Toast.makeText(getContext(), "Email change coming soon", Toast.LENGTH_SHORT).show();
    }

    /** Opens Compose Wallet (balance / top-up / ledger). See docs/WALLET_ANDROID.md. */
    private void openWallet() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_settingsFragment_to_wallet);
    }

    // ---- Support & Legal Actions ----
    private void openSupportPage() {
        Toast.makeText(getContext(), "Opening Help & Support", Toast.LENGTH_SHORT).show();
        // TODO: Navigate or open WebView/Support Activity
    }

    private void openTermsPage() {
        // Compose Terms (read / re-accept). requireAccept=false (default).
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_settingsFragment_to_Terms_Conditions);
    }

    private void openPrivacyPage() {
        Toast.makeText(getContext(), "Opening Privacy Policy", Toast.LENGTH_SHORT).show();
        // TODO: Navigate or open WebView/Privacy Activity
    }

    private void performLogout() {
        tokenManager.clearAll();
        userProfileRepository.clearUserProfile(() -> {
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigate(R.id.verification);
        });
    }

    private void loadUserProfile() {
        userProfileRepository.getUserProfile(profile -> {
            if (profile == null || usernameText == null) {
                return;
            }

            String fullName = (profile.firstName + " " + profile.secondName).trim();
            if (profile.pendingNameMutation) {
                fullName = fullName + " · syncing";
            }
            usernameText.setText(fullName);
        });
    }
}
