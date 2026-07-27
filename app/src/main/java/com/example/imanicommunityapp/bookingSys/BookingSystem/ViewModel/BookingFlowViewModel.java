package com.example.imanicommunityapp.bookingSys.BookingSystem.ViewModel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.imanicommunityapp.auth.Repository.TokenManager;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingFlowState;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingStatus;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Repository.BookingRepository;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingEvent;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.CheckoutRequest;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.CheckoutResponse;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.userCoordinates;
import com.example.imanicommunityapp.bookingSys.LocationSystem.Repository.UICoordinateRepo;

public class BookingFlowViewModel extends ViewModel {
    private final MutableLiveData<BookingFlowState> bookingState =
            new MutableLiveData<>(new BookingFlowState());
    private BookingRepository repository;
    private TokenManager tokenManager;
    public LiveData<String> NavigationLiveData;

    public LiveData<BookingFlowState> getBookingState() {
        return bookingState;
    }

    public UICoordinateRepo getUiCoordinateRepo() {
        return repository != null ? repository.getUiCoordinateRepo() : null;
    }

    //Initialise to avoid nullexception errors
    public void initialize(Context context) {
        Context appContext = context.getApplicationContext();
        if (repository == null) {
            repository = new BookingRepository(appContext);
        }
        if (tokenManager == null) {
            tokenManager = new TokenManager(appContext);
        }
    }

    public void dispatch(BookingEvent event) {
        BookingFlowState current = value();
        BookingFlowState next = nextState(current, event);
        bookingState.setValue(next);
        handleStateEntry(current, next);
    }

    // Function responsible for changing states between the state machine and the User Interface
    private BookingFlowState nextState(BookingFlowState current, BookingEvent event) {
        switch (event.getType()) {
            case OPEN_BOOKING:
                if (current.getStatus() == BookingStatus.IDLE
                        || current.getStatus() == BookingStatus.CANCELLED
                        || current.getStatus() == BookingStatus.ERROR) {
                    return new BookingFlowState(
                            BookingStatus.SELECTING_ROUTE,
                            null,
                            null,
                            null,
                            -1,
                            null
                    );
                }
                return current;
            case ROUTE_SELECTED:
                if (current.getStatus() == BookingStatus.SELECTING_ROUTE && event.getRouteName() != null) {
                    return new BookingFlowState(
                            BookingStatus.SELECTING_DROPOFF,
                            event.getRouteName(),
                            null,
                            null,
                            -1,
                            null
                    );
                }
                return current;
            case DROPOFF_SELECTED:
                if (current.getStatus() == BookingStatus.SELECTING_DROPOFF && event.getDropOffName() != null) {
                    return new BookingFlowState(
                            BookingStatus.SELECTING_DROPOFF,
                            current.getRouteName(),
                            event.getDropOffName(),
                            null,
                            -1,
                            null
                    );
                }
                return current;
            case SUBMIT_BOOKING:
                if (current.getStatus() == BookingStatus.SELECTING_DROPOFF
                        && current.getRouteName() != null
                        && current.getDropOffName() != null) {
                    return new BookingFlowState(
                            BookingStatus.SUBMITTING_BOOKING,
                            current.getRouteName(),
                            current.getDropOffName(),
                            null,
                            -1,
                            null
                    );
                }
                return current;
            case BOOKING_SUCCESS:
                if (current.getStatus() == BookingStatus.SUBMITTING_BOOKING) {
                    return new BookingFlowState(
                            BookingStatus.BOOKING_CONFIRMED,
                            current.getRouteName(),
                            current.getDropOffName(),
                            null,
                            event.getBookingId(),
                            event.getCoordinates()
                    );
                }
                return current;
            case BOOKING_FAILURE:
                if (current.getStatus() == BookingStatus.SUBMITTING_BOOKING
                        || current.getStatus() == BookingStatus.CANCELLING) {
                    return new BookingFlowState(
                            BookingStatus.ERROR,
                            current.getRouteName(),
                            current.getDropOffName(),
                            event.getErrorMessage(),
                            -1,
                            null
                    );
                }
                return current;
            case CANCEL_REQUESTED:
                if (current.getStatus() == BookingStatus.BOOKING_CONFIRMED
                        || current.getStatus() == BookingStatus.DRIVER_MATCHING
                        || current.getStatus() == BookingStatus.RIDE_ACTIVE) {
                    return new BookingFlowState(
                            BookingStatus.CANCELLING,
                            current.getRouteName(),
                            current.getDropOffName(),
                            null,
                            current.getBookingId(),
                            current.getCoordinates()
                    );
                }
                return current;
            case CANCEL_SUCCESS:
                if (current.getStatus() == BookingStatus.CANCELLING) {
                    return new BookingFlowState(
                            BookingStatus.CANCELLED,
                            current.getRouteName(),
                            current.getDropOffName(),
                            null,
                            -1,
                            null
                    );
                }
                return current;
            case RESET:
                return new BookingFlowState();
            default:
                return current;
        }
    }

    private void handleStateEntry(BookingFlowState previous, BookingFlowState next) {
        if (previous.getStatus() == next.getStatus()) return;

        if (next.getStatus() == BookingStatus.SUBMITTING_BOOKING) {
            onEnterSubmittingBooking(next);
            return;
        }

        if (next.getStatus() == BookingStatus.CANCELLING) {
            onEnterCancelling(next);
        }
    }

    /**
     * Light path: POST bookings/checkout/ (not create).
     * On success: store coords/quote in RAM and advance state with bookingId=-1
     * (no canonical Booking yet).
     */
    private void onEnterSubmittingBooking(BookingFlowState state) {
        if (repository == null || tokenManager == null) {
            dispatch(BookingEvent.bookingFailure("Booking service is not initialized."));
            return;
        }

        CheckoutRequest request = new CheckoutRequest(
                state.getRouteName(),
                state.getDropOffName(),
                null
        );

        repository.checkout(request, new BookingRepository.CheckoutCallback() {
            @Override
            public void onSuccess(CheckoutResponse response) {
                userCoordinates coords = response.getCoordinates();
                // booking_id is null on checkout — use -1 until pay confirms
                dispatch(BookingEvent.bookingSuccess(-1, coords));
            }

            @Override
            public void onError(String message) {
                dispatch(BookingEvent.bookingFailure(
                        message != null ? message : "Checkout failed."
                ));
            }
        });
    }

    private void onEnterCancelling(BookingFlowState state) {
        if (repository == null) {
            dispatch(BookingEvent.bookingFailure("Booking service is not initialized."));
            return;
        }
        if (state.getBookingId() <= 0) {
            dispatch(BookingEvent.cancelSuccess());
            return;
        }

        repository.cancelBooking(state.getBookingId());
    }

    private BookingFlowState value() {
        BookingFlowState state = bookingState.getValue();
        return state != null ? state : new BookingFlowState();
    }
}
