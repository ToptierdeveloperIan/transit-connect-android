package com.example.imanicommunityapp.bookingSys;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingEvent;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingFlowState;
import com.example.imanicommunityapp.bookingSys.BookingSystem.ViewModel.BookingFlowViewModel;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Home → route + drop-off selection → booking submit.
 * Replaces PinSystemFragment / pinsystem; inflates {@link R.layout#selectstop}.
 */
public class SelectStop extends Fragment {

    private enum Step {
        ROUTE,
        DROPOFF
    }

    // Panels
    private View topPanel;
    private View bottomPanel;

    // Included panel controls
    private EditText fieldOne;
    private EditText fieldTwo;
    private EditText fieldThree;
    private ListView optionsList;
    private Button confirmButton;

    // Optional loading (selectstop may not define these)
    @Nullable private View loadingView;
    @Nullable private View loadingDot;
    @Nullable private TextView loadingText;

    private BookingFlowViewModel bookingFlowViewModel;

    private Step currentStep = Step.ROUTE;
    private String selectedRoute;
    private String selectedDropoff;

    private final Map<String, List<String>> routeStopsMap = new HashMap<>();
    private final List<String> routes = Arrays.asList("KITENGELA", "JUJA", "KINOO");

    // -------------------- LIFECYCLE --------------------

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.selectstop, container, false);

        bookingFlowViewModel = new ViewModelProvider(requireActivity()).get(BookingFlowViewModel.class);
        bookingFlowViewModel.initialize(requireContext());

        bindViews(view);
        setupRouteStops();
        setupButtons(view);
        observeBookingState();

        BookingFlowState state = bookingFlowViewModel.getBookingState().getValue();
        if (state == null
                || state.getStatus() == BookingStatus.IDLE
                || state.getStatus() == BookingStatus.CANCELLED
                || state.getStatus() == BookingStatus.ERROR) {
            bookingFlowViewModel.dispatch(BookingEvent.openBooking());
        }

        view.post(this::showPanels);
        return view;
    }

    // -------------------- VIEW SETUP --------------------

    private void bindViews(View view) {
        topPanel = view.findViewById(R.id.topPanel);
        bottomPanel = view.findViewById(R.id.bottomPanel);

        loadingView = view.findViewById(R.id.loadingView);
        loadingDot = view.findViewById(R.id.loadingDot);
        loadingText = view.findViewById(R.id.loadingText);

        fieldOne = view.findViewById(R.id.edit_text_one);
        fieldTwo = view.findViewById(R.id.edit_text_two);
        fieldThree = view.findViewById(R.id.edit_text_three);
        optionsList = view.findViewById(R.id.options_list);
        confirmButton = view.findViewById(R.id.confirmdestination);

        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
        if (optionsList != null) {
            optionsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        if (confirmButton != null) {
            confirmButton.setEnabled(false);
        }
        if (fieldOne != null) {
            fieldOne.setFocusable(false);
        }
        if (fieldTwo != null) {
            fieldTwo.setFocusable(false);
        }
        if (fieldThree != null) {
            fieldThree.setFocusable(false);
        }
    }

    private void setupRouteStops() {
        routeStopsMap.put("KITENGELA", Arrays.asList("CABANAS", "SYOKIMAU", "SABAKI", "ATHI RIVER"));
        routeStopsMap.put("JUJA", Arrays.asList("SURVEY", "MATHARE", "THIKA ROAD", "JUJA STAGE"));
        routeStopsMap.put("KINOO", Arrays.asList("WESTLANDS", "SAFARICOM", "KINOO STAGE"));
    }

    private void setupButtons(View rootView) {
        if (confirmButton != null) {
            confirmButton.setOnClickListener(v -> {
                if (currentStep == Step.ROUTE) {
                    if (selectedRoute == null) return;
                    bookingFlowViewModel.dispatch(BookingEvent.routeSelected(selectedRoute));
                    return;
                }

                if (selectedDropoff == null) return;
                bookingFlowViewModel.dispatch(BookingEvent.submitBooking());
            });
        }

        View cancelBtn = rootView.findViewById(R.id.btnCancel);
        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> hidePanels());
        }
    }

    // -------------------- STEP UI --------------------

    private void renderStep() {
        if (optionsList == null || confirmButton == null) return;

        optionsList.clearChoices();
        confirmButton.setEnabled(false);

        if (currentStep == Step.ROUTE) {
            if (fieldOne != null) fieldOne.setText("Step 1: Select Route");
            if (fieldTwo != null) {
                fieldTwo.setText(selectedRoute != null ? "Selected: " + selectedRoute : "");
            }
            if (fieldThree != null) fieldThree.setText("");
            confirmButton.setText("CONFIRM ROUTE");
            bindOptions(routes, value -> selectedRoute = value);
            return;
        }

        List<String> stops = routeStopsMap.get(selectedRoute);
        if (stops == null) {
            stops = Collections.emptyList();
        }

        if (fieldOne != null) fieldOne.setText("Step 2: Select Drop Off");
        if (fieldTwo != null) fieldTwo.setText("Route: " + selectedRoute);
        if (fieldThree != null) {
            fieldThree.setText(selectedDropoff != null ? "Selected: " + selectedDropoff : "");
        }
        confirmButton.setText("REQUEST RIDE");
        bindOptions(stops, value -> {
            selectedDropoff = value;
            if (fieldThree != null) {
                fieldThree.setText("Selected: " + value);
            }
            bookingFlowViewModel.dispatch(BookingEvent.dropoffSelected(value));
        });
    }

    private interface OptionSelectedListener {
        void onSelected(String value);
    }

    private void bindOptions(List<String> options, OptionSelectedListener listener) {
        if (optionsList == null || confirmButton == null) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_single_choice,
                options
        );
        optionsList.setAdapter(adapter);
        optionsList.setOnItemClickListener((parent, view, position, id) -> {
            String selected = options.get(position);
            listener.onSelected(selected);
            confirmButton.setEnabled(true);
        });
    }

    // -------------------- BOOKING FLOW --------------------

    private void observeBookingState() {
        bookingFlowViewModel.getBookingState().observe(getViewLifecycleOwner(), this::renderBookingState);
    }

    private void renderBookingState(BookingFlowState state) {
        if (state == null) return;

        if (state.getRouteName() != null) selectedRoute = state.getRouteName();
        if (state.getDropOffName() != null) selectedDropoff = state.getDropOffName();

        switch (state.getStatus()) {
            case SELECTING_ROUTE:
                hideLoadingView();
                currentStep = Step.ROUTE;
                renderStep();
                break;
            case SELECTING_DROPOFF:
                hideLoadingView();
                currentStep = Step.DROPOFF;
                renderStep();
                if (selectedDropoff != null && confirmButton != null) {
                    confirmButton.setEnabled(true);
                }
                break;
            case SUBMITTING_BOOKING:
                showLoadingView(
                        "Please wait as we search for available drivers and seats..."
                );
                if (confirmButton != null) {
                    confirmButton.setEnabled(false);
                }
                break;
            case BOOKING_CONFIRMED:
                hideLoadingView();
                sendTripStartedSignal();
                NavHostFragment.findNavController(this).popBackStack();
                break;
            case ERROR:
                hideLoadingView();
                String message = state.getErrorMessage() != null
                        ? state.getErrorMessage()
                        : "Something went wrong.";
                Log.e("SelectStop", message);
                if (loadingView != null && loadingText != null) {
                    loadingView.setVisibility(View.VISIBLE);
                    loadingText.setText(message);
                }
                if (confirmButton != null) {
                    confirmButton.setEnabled(
                            selectedDropoff != null || currentStep == Step.ROUTE
                    );
                }
                break;
            default:
                break;
        }
    }

    private void sendTripStartedSignal() {
        Bundle result = new Bundle();
        result.putBoolean("trip_started_success", true);
        getParentFragmentManager()
                .setFragmentResult("trip_status_key", result);
    }

    // -------------------- LOADING UI (null-safe for selectstop) --------------------

    private void showLoadingView(String message) {
        if (loadingView == null) {
            Log.d("SelectStop", message);
            return;
        }
        if (loadingText != null) {
            loadingText.setText(message);
        }
        loadingView.setVisibility(View.VISIBLE);
        if (loadingDot != null) {
            Animation anim = AnimationUtils.loadAnimation(
                    requireContext(),
                    R.anim.loading_dot_move
            );
            loadingDot.startAnimation(anim);
        }
    }

    private void hideLoadingView() {
        if (loadingDot != null) {
            loadingDot.clearAnimation();
        }
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
    }

    // -------------------- PANEL ANIMATIONS --------------------

    private void showPanels() {
        if (topPanel == null || bottomPanel == null) return;

        topPanel.setTranslationY(-topPanel.getHeight());
        bottomPanel.setTranslationY(bottomPanel.getHeight());
        topPanel.setAlpha(0.92f);
        bottomPanel.setAlpha(0.92f);

        topPanel.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        bottomPanel.animate()
                .translationY(0)
                .alpha(1f)
                .setStartDelay(40)
                .setDuration(380)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void hidePanels() {
        if (topPanel == null || bottomPanel == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        topPanel.animate()
                .translationY(-topPanel.getHeight())
                .alpha(0.92f)
                .setDuration(260)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        bottomPanel.animate()
                .translationY(bottomPanel.getHeight())
                .alpha(0.92f)
                .setDuration(320)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() ->
                        NavHostFragment.findNavController(this).popBackStack()
                )
                .start();
    }
}
