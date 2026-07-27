package com.example.imanicommunityapp.paymentsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.imanicommunityapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class PaymentFragment extends DialogFragment {

    private static final String ARG_BOOKING_ID = "booking_id";
    private static final String ARG_AMOUNT = "amount";

    private TextInputEditText amountInput;
    private TextInputEditText bookingIdInput;
    private MaterialButton btnSendStk;
    private ProgressBar paymentProgress;
    private TextView paymentResult;

    private PaymentViewModel paymentViewModel;

    public static PaymentFragment newInstance(int bookingId, double amount) {
        PaymentFragment fragment = new PaymentFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BOOKING_ID, bookingId);
        args.putDouble(ARG_AMOUNT, amount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_ui, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        paymentViewModel = new ViewModelProvider(this).get(PaymentViewModel.class);
        paymentViewModel.initialize(requireContext());

        bindViews(view);
        prefillFromArgs();
        observeState();
        bindClicks();
    }

    private void bindViews(View view) {
        amountInput = view.findViewById(R.id.amountInput);
        bookingIdInput = view.findViewById(R.id.bookingIdInput);
        btnSendStk = view.findViewById(R.id.btnSendStk);
        paymentProgress = view.findViewById(R.id.paymentProgress);
        paymentResult = view.findViewById(R.id.paymentResult);
    }

    private void prefillFromArgs() {
        if (getArguments() == null) return;
        int bookingId = getArguments().getInt(ARG_BOOKING_ID, -1);
        double amount = getArguments().getDouble(ARG_AMOUNT, 0);
        if (bookingId > 0) bookingIdInput.setText(String.valueOf(bookingId));
        if (amount > 0) amountInput.setText(String.valueOf((int) amount));
    }

    private void observeState() {
        paymentViewModel.getPaymentState().observe(getViewLifecycleOwner(), state -> {
            switch (state.getStatus()) {
                case IDLE:
                    showIdle();
                    break;
                case INITIATING:
                    showLoading("Sending STK push to your phone\u2026");
                    break;
                case AWAITING_PIN:
                    showLoading("Enter your M-Pesa PIN on your phone to confirm payment.");
                    break;
                case POLLING:
                    showLoading("Confirming payment\u2026");
                    break;
                case SUCCESS:
                    showResult(true, "Payment successful! Your ride is confirmed.");
                    break;
                case FAILED:
                    showResult(false, state.getErrorMessage() != null
                            ? state.getErrorMessage() : "Payment failed. Please try again.");
                    break;
                case ERROR:
                    showResult(false, "An unexpected error occurred. Please try again.");
                    break;
            }
        });
    }

    private void bindClicks() {
        btnSendStk.setOnClickListener(v -> {
            String amountText = amountInput.getText() != null
                    ? amountInput.getText().toString().trim() : "";
            String bookingIdText = bookingIdInput.getText() != null
                    ? bookingIdInput.getText().toString().trim() : "";

            if (amountText.isEmpty()) {
                amountInput.setError("Please enter an amount");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountText);
            } catch (NumberFormatException e) {
                amountInput.setError("Invalid amount");
                return;
            }

            int bookingId = -1;
            if (!bookingIdText.isEmpty()) {
                try {
                    bookingId = Integer.parseInt(bookingIdText);
                } catch (NumberFormatException e) {
                    bookingIdInput.setError("Invalid booking ID");
                    return;
                }
            }

            paymentViewModel.dispatch(PaymentEvent.initiatePayment(amount, bookingId));
        });
    }

    private void showIdle() {
        paymentProgress.setVisibility(View.GONE);
        paymentResult.setVisibility(View.GONE);
        btnSendStk.setEnabled(true);
        btnSendStk.setText("Send STK Push");
    }

    private void showLoading(String message) {
        paymentProgress.setVisibility(View.VISIBLE);
        paymentResult.setVisibility(View.VISIBLE);
        paymentResult.setText(message);
        paymentResult.setTextColor(requireContext().getColor(android.R.color.black));
        btnSendStk.setEnabled(false);
    }

    private void showResult(boolean success, String message) {
        paymentProgress.setVisibility(View.GONE);
        paymentResult.setVisibility(View.VISIBLE);
        paymentResult.setText(message);
        if (success) {
            paymentResult.setTextColor(requireContext().getColor(android.R.color.holo_green_dark));
            btnSendStk.setEnabled(false);
            btnSendStk.setText("Paid");
        } else {
            paymentResult.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
            btnSendStk.setEnabled(true);
            btnSendStk.setText("Retry");
        }
    }
}
