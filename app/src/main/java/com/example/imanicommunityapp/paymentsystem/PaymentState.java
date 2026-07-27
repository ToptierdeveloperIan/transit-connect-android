package com.example.imanicommunityapp.paymentsystem;

import androidx.annotation.Nullable;

public class PaymentState {

    private final PaymentStatus status;
    private final double amount;
    private final int bookingId;
    @Nullable private final String checkoutRequestId;
    @Nullable private final String errorMessage;

    public PaymentState() {
        this(PaymentStatus.IDLE, 0, -1, null, null);
    }

    public PaymentState(PaymentStatus status, double amount, int bookingId,
                        @Nullable String checkoutRequestId, @Nullable String errorMessage) {
        this.status = status;
        this.amount = amount;
        this.bookingId = bookingId;
        this.checkoutRequestId = checkoutRequestId;
        this.errorMessage = errorMessage;
    }

    public PaymentStatus getStatus() { return status; }

    public double getAmount() { return amount; }

    public int getBookingId() { return bookingId; }

    @Nullable
    public String getCheckoutRequestId() { return checkoutRequestId; }

    @Nullable
    public String getErrorMessage() { return errorMessage; }
}
