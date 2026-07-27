package com.example.imanicommunityapp.RandRsystem;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.imanicommunityapp.Network.ConnectivityChecker;
import com.example.imanicommunityapp.Network.InternetState;
import com.example.imanicommunityapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;
import java.util.regex.Pattern;

public class RedeemCodeFragment extends Fragment {

    private static final Pattern REDEEM_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]{8}$");

    private TextInputLayout redeemCodeInputLayout;
    private TextInputEditText redeemCodeInput;
    private MaterialButton redeemButton;
    private MaterialButton clearCodeButton;
    private ProgressBar redeemProgress;
    private TextView redeemStatusText;
    private TextView pointsValue;
    private View offlineNoticeCard;
    private TextView offlineNoticeText;

    private RedeemCodeRepository redeemCodeRepository;
    private ConnectivityChecker connectivityChecker;
    private ConnectivityChecker.NetworkStatusListener networkStatusListener;
    private boolean isInternetAvailable;

    public RedeemCodeFragment() {
        super(R.layout.fragment_redeem_code);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        redeemCodeInputLayout = view.findViewById(R.id.redeemCodeInputLayout);
        redeemCodeInput = view.findViewById(R.id.redeemCodeInput);
        redeemButton = view.findViewById(R.id.redeemButton);
        clearCodeButton = view.findViewById(R.id.clearCodeButton);
        redeemProgress = view.findViewById(R.id.redeemProgress);
        redeemStatusText = view.findViewById(R.id.redeemStatusText);
        pointsValue = view.findViewById(R.id.pointsValue);
        offlineNoticeCard = view.findViewById(R.id.offlineNoticeCard);
        offlineNoticeText = view.findViewById(R.id.offlineNoticeText);

        redeemCodeRepository = new RedeemCodeRepository(requireContext());
        connectivityChecker = new ConnectivityChecker(requireContext());
        networkStatusListener = isConnected -> {
            if (!isAdded()) {
                return;
            }
            updateInternetUiState(isConnected);
        };

        redeemButton.setOnClickListener(v -> handleRedeemClick());
        clearCodeButton.setOnClickListener(v -> clearCodeInput());
        redeemCodeInput.addTextChangedListener(new RedeemCodeWatcher());

        connectivityChecker.addListener(networkStatusListener);
        updateInternetUiState(connectivityChecker.hasInternetConnection());
    }

    public void setAvailablePoints(int points) {
        if (pointsValue == null) {
            return;
        }
        pointsValue.setText(String.format(Locale.getDefault(), "%,d", points));
    }

    private void handleRedeemClick() {
        String code = getNormalizedCode();
        if (!isCodeValid(code)) {
            return;
        }

        if (!isInternetAvailable) {
            showOfflineState();
            return;
        }

        redeemCodeInput.setText(code);
        setLoading(true);
        redeemCodeRepository.validateRedeemCode(code, new RedeemCodeRepository.RedeemCodeCallback() {
            @Override
            public void onSuccess(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                showStatus(message, R.color.green);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                showStatus(message, R.color.red);
            }
        });
    }

    private boolean isCodeValid(String code) {
        if (TextUtils.isEmpty(code)) {
            redeemCodeInputLayout.setError(getString(R.string.redeem_code_error_empty));
            return false;
        }

        if (!REDEEM_CODE_PATTERN.matcher(code).matches()) {
            redeemCodeInputLayout.setError(getString(R.string.redeem_code_error_invalid));
            return false;
        }

        redeemCodeInputLayout.setError(null);
        return true;
    }

    private String getNormalizedCode() {
        Editable editable = redeemCodeInput.getText();
        if (editable == null) {
            return "";
        }
        return editable.toString().trim().toUpperCase(Locale.getDefault());
    }

    private void clearCodeInput() {
        redeemCodeInputLayout.setError(null);
        redeemCodeInput.setText("");
        renderIdleState();
    }

    private void renderIdleState() {
        setLoading(false);
        redeemStatusText.setVisibility(View.GONE);
        if (!isInternetAvailable) {
            showOfflineState();
        }
    }

    private void setLoading(boolean loading) {
        redeemProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        redeemButton.setEnabled(!loading && isInternetAvailable);
        clearCodeButton.setEnabled(!loading);
        redeemCodeInput.setEnabled(!loading);
    }

    private void showStatus(@NonNull String message, int colorResId) {
        redeemStatusText.setVisibility(View.VISIBLE);
        redeemStatusText.setText(message);
        redeemStatusText.setTextColor(requireContext().getColor(colorResId));
    }

    private void updateInternetUiState(boolean isConnected) {
        isInternetAvailable = isConnected;

        if (isConnected) {
            offlineNoticeCard.setVisibility(View.GONE);
            redeemCodeInputLayout.setHelperText(getString(R.string.redeem_code_helper_online));
            redeemCodeInputLayout.setError(null);
            redeemButton.setEnabled(redeemProgress.getVisibility() != View.VISIBLE);
            clearOfflineStatusIfNeeded();
            return;
        }

        showOfflineState();
    }

    private void showOfflineState() {
        offlineNoticeCard.setVisibility(View.VISIBLE);
        offlineNoticeText.setText(resolveOfflineMessage());
        redeemCodeInputLayout.setHelperText(getString(R.string.redeem_code_helper_offline));
        redeemButton.setEnabled(false);
        showStatus(getString(R.string.redeem_code_try_again_later), R.color.red);
    }

    @NonNull
    private String resolveOfflineMessage() {
        if (connectivityChecker == null) {
            return getString(R.string.redeem_code_offline_message);
        }

        if (connectivityChecker.getInternetStatus().getState() == InternetState.LIMITED) {
            return getString(R.string.redeem_code_limited_message);
        }

        return getString(R.string.redeem_code_offline_message);
    }

    private void clearOfflineStatusIfNeeded() {
        if (redeemStatusText.getVisibility() != View.VISIBLE) {
            return;
        }

        CharSequence currentMessage = redeemStatusText.getText();
        if (TextUtils.equals(currentMessage, getString(R.string.redeem_code_try_again_later))) {
            redeemStatusText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        if (connectivityChecker != null && networkStatusListener != null) {
            connectivityChecker.removeListener(networkStatusListener);
            connectivityChecker.shutdown();
        }
        super.onDestroyView();
    }

    private final class RedeemCodeWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            redeemCodeInputLayout.setError(null);
            if (isInternetAvailable) {
                redeemCodeInputLayout.setHelperText(getString(R.string.redeem_code_helper_online));
            }
            if (redeemStatusText.getVisibility() == View.VISIBLE) {
                redeemStatusText.setVisibility(View.GONE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
