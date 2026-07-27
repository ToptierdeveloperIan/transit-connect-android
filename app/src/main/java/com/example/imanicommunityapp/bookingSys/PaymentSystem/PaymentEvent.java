package com.example.imanicommunityapp.bookingSys.PaymentSystem;

import androidx.annotation.Nullable;

public class PaymentEvent {

    public enum Type {
        INITIATE_PAYMENT,
        STK_SENT,
        STK_FAILED,
        PAYMENT_CONFIRMED,
        PAYMENT_FAILED,
        RESET
    }

    private final Type type;
    @Nullable private final String checkoutRequestId;
    @Nullable private final String errorMessage;
    private final double amount;
    private final int bookingId;

    private PaymentEvent(Type type, @Nullable String checkoutRequestId,
                         @Nullable String errorMessage, double amount, int bookingId) {
        this.type = type;
        this.checkoutRequestId = checkoutRequestId;
        this.errorMessage = errorMessage;
        this.amount = amount;
        this.bookingId = bookingId;
    }

    public static PaymentEvent initiatePayment(double amount, int bookingId) {
        return new PaymentEvent(Type.INITIATE_PAYMENT, null, null, amount, bookingId);
    }

    public static PaymentEvent stkSent(String checkoutRequestId) {
        return new PaymentEvent(Type.STK_SENT, checkoutRequestId, null, 0, -1);
    }

    public static PaymentEvent stkFailed(String errorMessage) {
        return new PaymentEvent(Type.STK_FAILED, null, errorMessage, 0, -1);
    }

    public static PaymentEvent paymentConfirmed() {
        return new PaymentEvent(Type.PAYMENT_CONFIRMED, null, null, 0, -1);
    }

    public static PaymentEvent paymentFailed(String errorMessage) {
        return new PaymentEvent(Type.PAYMENT_FAILED, null, errorMessage, 0, -1);
    }

    public static PaymentEvent reset() {
        return new PaymentEvent(Type.RESET, null, null, 0, -1);
    }

    public Type getType() { return type; }

    @Nullable
    public String getCheckoutRequestId() { return checkoutRequestId; }

    @Nullable
    public String getErrorMessage() { return errorMessage; }

    public double getAmount() { return amount; }

    public int getBookingId() { return bookingId; }
}
