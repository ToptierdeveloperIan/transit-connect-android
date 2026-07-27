package com.example.imanicommunityapp.paymentsystem;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.imanicommunityapp.bookingSys.PaymentSystem.StkPushResponse;

public class PaymentViewModel extends ViewModel {

    private static final int MAX_POLLS = 10;
    private static final long POLL_INTERVAL_MS = 3000L;

    private final MutableLiveData<PaymentState> paymentState =
            new MutableLiveData<>(new PaymentState());

    private PaymentRepository repository;
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private int pollCount = 0;

    public LiveData<PaymentState> getPaymentState() {
        return paymentState;
    }

    // Call from the fragment before dispatching any events
    public void initialize(Context context) {
        if (repository == null) {
            repository = new PaymentRepository(context.getApplicationContext());
        }
    }

    public void dispatch(PaymentEvent event) {
        PaymentState current = value();
        PaymentState next = nextState(current, event);
        paymentState.setValue(next);
        handleStateEntry(current, next);
    }

    private PaymentState nextState(PaymentState current, PaymentEvent event) {
        switch (event.getType()) {

            case INITIATE_PAYMENT:
                if (current.getStatus() == PaymentStatus.IDLE
                        || current.getStatus() == PaymentStatus.FAILED
                        || current.getStatus() == PaymentStatus.ERROR) {
                    return new PaymentState(
                            PaymentStatus.INITIATING,
                            event.getAmount(),
                            event.getBookingId(),
                            null,
                            null
                    );
                }
                return current;

            case STK_SENT:
                if (current.getStatus() == PaymentStatus.INITIATING) {
                    return new PaymentState(
                            PaymentStatus.AWAITING_PIN,
                            current.getAmount(),
                            current.getBookingId(),
                            event.getCheckoutRequestId(),
                            null
                    );
                }
                return current;

            case STK_FAILED:
                if (current.getStatus() == PaymentStatus.INITIATING) {
                    return new PaymentState(
                            PaymentStatus.FAILED,
                            current.getAmount(),
                            current.getBookingId(),
                            null,
                            event.getErrorMessage()
                    );
                }
                return current;

            case PAYMENT_CONFIRMED:
                if (current.getStatus() == PaymentStatus.AWAITING_PIN
                        || current.getStatus() == PaymentStatus.POLLING) {
                    return new PaymentState(
                            PaymentStatus.SUCCESS,
                            current.getAmount(),
                            current.getBookingId(),
                            current.getCheckoutRequestId(),
                            null
                    );
                }
                return current;

            case PAYMENT_FAILED:
                return new PaymentState(
                        PaymentStatus.FAILED,
                        current.getAmount(),
                        current.getBookingId(),
                        current.getCheckoutRequestId(),
                        event.getErrorMessage()
                );

            case RESET:
                stopPolling();
                return new PaymentState();

            default:
                return current;
        }
    }

    private void handleStateEntry(PaymentState previous, PaymentState next) {
        if (previous.getStatus() == next.getStatus()) return;

        switch (next.getStatus()) {
            case INITIATING:
                onEnterInitiating(next);
                break;
            case AWAITING_PIN:
                startPolling(next.getCheckoutRequestId());
                break;
            case SUCCESS:
            case FAILED:
            case ERROR:
                stopPolling();
                break;
            default:
                break;
        }
    }

    private void onEnterInitiating(PaymentState state) {
        if (repository == null) {
            dispatch(PaymentEvent.stkFailed("Payment service not initialized."));
            return;
        }

        // M-Pesa expects integer amounts as strings
        String amount = String.valueOf((int) state.getAmount());

        repository.initiateStkPush(amount, state.getBookingId(),
                new PaymentRepository.PaymentCallback<StkPushResponse>() {
                    @Override
                    public void onSuccess(StkPushResponse response) {
                        if (response.success && response.checkoutRequestId != null) {
                            dispatch(PaymentEvent.stkSent(response.checkoutRequestId));
                        } else {
                            String msg = response.message != null ? response.message : "STK push failed.";
                            dispatch(PaymentEvent.stkFailed(msg));
                        }
                    }

                    @Override
                    public void onError(String message) {
                        dispatch(PaymentEvent.stkFailed(message));
                    }
                });
    }

    private void startPolling(String checkoutRequestId) {
        pollCount = 0;
        scheduleNextPoll(checkoutRequestId);
    }

    private void scheduleNextPoll(String checkoutRequestId) {
        pollingHandler.postDelayed(() -> {
            PaymentStatus currentStatus = value().getStatus();
            if (currentStatus != PaymentStatus.AWAITING_PIN
                    && currentStatus != PaymentStatus.POLLING) {
                return;
            }

            pollCount++;
            repository.checkPaymentStatus(checkoutRequestId,
                    new PaymentRepository.PaymentCallback<PaymentStatusResponse>() {
                        @Override
                        public void onSuccess(PaymentStatusResponse response) {
                            if ("completed".equals(response.status)) {
                                dispatch(PaymentEvent.paymentConfirmed());
                            } else if ("failed".equals(response.status)
                                    || "cancelled".equals(response.status)) {
                                String msg = response.message != null
                                        ? response.message : "Payment was not completed.";
                                dispatch(PaymentEvent.paymentFailed(msg));
                            } else if (pollCount < MAX_POLLS) {
                                // Still pending — schedule the next check
                                scheduleNextPoll(checkoutRequestId);
                            } else {
                                dispatch(PaymentEvent.paymentFailed(
                                        "Payment timed out. Please check your M-Pesa messages."));
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (pollCount < MAX_POLLS) {
                                scheduleNextPoll(checkoutRequestId);
                            } else {
                                dispatch(PaymentEvent.paymentFailed(
                                        "Unable to confirm payment. Please check your M-Pesa messages."));
                            }
                        }
                    });
        }, POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacksAndMessages(null);
        pollCount = 0;
    }

    private PaymentState value() {
        PaymentState state = paymentState.getValue();
        return state != null ? state : new PaymentState();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPolling();
    }
}
