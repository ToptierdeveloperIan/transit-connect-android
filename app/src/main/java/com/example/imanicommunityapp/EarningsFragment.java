package com.example.imanicommunityapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class EarningsFragment extends Fragment {

    private TextView todayEarnings, totalTrips, hoursOnline;
    private LinearLayout recentTripsContainer;
    private ImageView backBtn;

    public EarningsFragment() {
        super(R.layout.earnings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        todayEarnings = view.findViewById(R.id.todayEarnings);
        totalTrips = view.findViewById(R.id.totalTrips);
        hoursOnline = view.findViewById(R.id.hoursOnline);
        recentTripsContainer = view.findViewById(R.id.recentTripsContainer);
        backBtn = view.findViewById(R.id.backBtn);

        // Program back button navigation
        backBtn.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp()
        );

        loadSampleData();
    }

    private void loadSampleData() {
        todayEarnings.setText("KES 1,480");
        totalTrips.setText("7");
        hoursOnline.setText("5h");

        addTripItem("Imara Daima → CBD", "KES 200", "Completed");
        addTripItem("Mlolongo → Syokimau", "KES 150", "Completed");
        addTripItem("Pipeline → Donholm", "KES 180", "Cancelled");
    }

    private void addTripItem(String route, String fare, String status) {
        TextView tv = new TextView(requireContext());
        tv.setText(route + " • " + fare + " • " + status);
        tv.setTextColor(Color.parseColor("#000000"));
        tv.setTextSize(14f);
        tv.setPadding(10, 20, 10, 20);

        recentTripsContainer.addView(tv);
    }
}

