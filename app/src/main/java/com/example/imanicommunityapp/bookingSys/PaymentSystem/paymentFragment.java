package com.example.imanicommunityapp.bookingSys.PaymentSystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.imanicommunityapp.R;
import com.google.android.material.button.MaterialButton;

public class paymentFragment extends DialogFragment {

    private TextView scooterIdLabel;
    private TextView rangeText;
    private TextView paymentMethodText;
    private TextView pricingText;
    private MaterialButton startRideButton;
    private View paymentMethodRow;
    private View pricingRow;
    private ImageView scooterImage;

    public paymentFragment() {
        // Required empty public constructor
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
        return inflater.inflate(R.layout.payment_ui, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        bindDummyContent();
        bindClicks();
    }

    private void bindViews(View view) {
        scooterIdLabel = view.findViewById(R.id.scooterIdLabel);
        rangeText = view.findViewById(R.id.rangeText);
        paymentMethodText = view.findViewById(R.id.paymentMethodText);
        pricingText = view.findViewById(R.id.pricingText);
        startRideButton = view.findViewById(R.id.startRideButton);
        paymentMethodRow = view.findViewById(R.id.paymentMethodRow);
        pricingRow = view.findViewById(R.id.pricingRow);
        scooterImage = view.findViewById(R.id.scooterImage);
    }

    private void bindDummyContent() {
        scooterIdLabel.setText("N\u00B0 01030");
        rangeText.setText("40 km");
        paymentMethodText.setText("Payment method *3334");
        pricingText.setText("Pricing 0.90 \u20AC + 0.09 \u20AC/min");
        scooterImage.setImageResource(R.drawable.ic_scooter_placeholder);
    }

    private void bindClicks() {
        startRideButton.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Start the ride tapped", Toast.LENGTH_SHORT).show()
        );

        paymentMethodRow.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Payment method tapped", Toast.LENGTH_SHORT).show()
        );

        pricingRow.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Pricing tapped", Toast.LENGTH_SHORT).show()
        );
    }
}

