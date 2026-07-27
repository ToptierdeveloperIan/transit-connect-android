package com.example.imanicommunityapp.Sync;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Database model representing cached route destinations and their GPS coordinates.
 * Used to store static route coordinates offline for clean autocomplete and maps loading.
 */
@Entity(
        tableName = "route_destinations",
        indices = {@Index(value = {"route", "destination"}, unique = true)}
)
public class RouteDestination {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "route")
    public String route;

    @NonNull
    @ColumnInfo(name = "destination")
    public String destination;


    @ColumnInfo(name = "lat")
    public double lat;

    @ColumnInfo(name = "lng")
    public double lng;

    public RouteDestination(@NonNull String route, @NonNull String destination, double lat, double lng) {
        this.route = route;
        this.destination = destination;
        this.lat = lat;
        this.lng = lng;
    }

    @NonNull
    public String getRoute() {
        return route;
    }

    @NonNull
    public String getDestination() {
        return destination;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    @NonNull
    @Override
    public String toString() {
        return "RouteDestination{" +
                "id=" + id +
                ", route='" + route + '\'' +
                ", destination='" + destination + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                '}';
    }
}
