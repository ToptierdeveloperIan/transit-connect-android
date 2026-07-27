package com.example.imanicommunityapp;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;


import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.imanicommunityapp.Sync.InitialData;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private InitialData initialdata;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get Booking details from the backend and store it in the Room database
        initialdata = new InitialData();
        initialdata.fetchInitialData();



        //Blending the UI color with the status bar
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        // Set up the BottomNavigationView
        bottomNav = findViewById(R.id.bottomNavigationView);



        // Postpone NavController initialization until the fragment view is created
        new Handler().post(() -> {
            NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment);

            // Set up NavigationUI for bottom navigation
            NavigationUI.setupWithNavController(bottomNav, navController);

            NavigationView navigationView = findViewById(R.id.navigation_view);
            if (navigationView != null) {
                NavigationUI.setupWithNavController(navigationView, navController);
                navigationView.setNavigationItemSelectedListener(item -> {
                    boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                    if (handled) {
                        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
                        if (drawerLayout != null) {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        }
                    }
                    return handled;
                });
            }

            // Show/hide bottom nav based on destination
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.homeFragment ||
                        destination.getId() == R.id.settingsFragment) {
                    bottomNav.setVisibility(View.VISIBLE); // Show nav
                } else {
                    bottomNav.setVisibility(View.GONE); // Hide nav
                }
            });

            // Set the listener for BottomNavigationView item clicks
            bottomNav.setOnNavigationItemSelectedListener(item -> {

                if (item.getItemId() == R.id.nav_home) {
                    // Navigate to the Home fragment
                    navController.navigate(R.id.homeFragment);
                    return true;
                } else if (item.getItemId() == R.id.nav_settings) {
                    // Navigate to the Settings fragment
                    navController.navigate(R.id.settingsFragment);
                    return true;
                } else {
                    return false;
                }
            });
        });
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
