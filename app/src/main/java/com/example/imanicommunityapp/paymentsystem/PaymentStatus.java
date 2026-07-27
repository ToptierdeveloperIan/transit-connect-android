package com.example.imanicommunityapp.paymentsystem;

public enum PaymentStatus {
    IDLE,
    INITIATING,      // STK push request being sent to backend
    AWAITING_PIN,    // STK sent; waiting for user to enter M-Pesa PIN on their phone
    POLLING,         // Checking backend for payment confirmation
    SUCCESS,
    FAILED,
    ERROR
}
