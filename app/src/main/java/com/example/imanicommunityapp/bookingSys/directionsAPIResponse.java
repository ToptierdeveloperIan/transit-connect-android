package com.example.imanicommunityapp.bookingSys;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class directionsAPIResponse {
    @SerializedName("routes")
    private List<Route> routes;

    public List<Route> getRoutes() {
        return routes;
    }

    public static class Route {
        @SerializedName("overview_polyline")
        private OverviewPolyline overviewPolyline;

        public OverviewPolyline getOverviewPolyline() {
            return overviewPolyline;
        }
    }

    public static class OverviewPolyline {
        @SerializedName("points")
        private String points;

        public String getPoints() {
            return points;
        }
    }
}


