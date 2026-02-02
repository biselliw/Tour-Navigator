package de.biselliw.tour_navigator;

/*
    This file is part of Tour Navigator

    Tour Navigator is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    Tour Navigator is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.IntentSender;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.format.Time;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

import de.biselliw.tour_navigator.activities.LocationActivity;
import de.biselliw.tour_navigator.helpers.Log;


public class LocationService extends Service {
    private static final String CHANNEL_ID = "location_channel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        startForeground(1, buildNotification());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create a LocationRequest
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000
        ).setMinUpdateDistanceMeters(0)
                .build();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }
        else {
            // Build a LocationSettingsRequest
            LocationSettingsRequest settingsRequest =
                    new LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest)
                            .setAlwaysShow(false)
                            .build();

            /* Check Location Settings */
            SettingsClient settingsClient =
                    LocationServices.getSettingsClient(this);

            settingsClient.checkLocationSettings(settingsRequest)
                    .addOnSuccessListener(locationSettingsResponse -> {
                        // Location settings are enabled and meet requirements
                        LocationActivity.setGpsStatus (LocationActivity.gpsStatus.WAIT_FOR_GPS_FIX);
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof ResolvableApiException) {
                            // Location is OFF or does not meet requirements
                            LocationActivity.setGpsStatus (LocationActivity.gpsStatus.PROVIDER_DISABLED);
                        }
                    });
        }

        locationCallback = new LocationCallback() {
            @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                for (Location location : result.getLocations()) {
                    handleLocation(location);
                }

                fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                );
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    private void handleLocation(Location location) {
        Log.d("GPS", location.getLatitude() + ", " + location.getLongitude() + " acc: " + location.getAccuracy());
        // use system clock instead of GPS device time
        Time CurrentTime = new Time();
        CurrentTime.setToNow();
/*
        if (gpsSimulation != null) {
            if (ControlElements.isTracking()) {
                location = gpsSimulation.getLocation();
                if (location == null) return;
                Log.w("GPS", "location from simulation");
            }
            else {
                Log.w("GPS", "location simulation finished");
                return;
            }
        }
*/
        /*
        // finally handle the real/simulated geo location in the user activity
        float accuracy = 0; if (location.hasAccuracy()) {
            accuracy = location.getAccuracy();
        }
*/
        LocationActivity.setLocation(location);
        //        locationActivity.handleGpsData(CurrentTime,location.getLatitude(),location.getLongitude(), accuracy);
    }

    /**
     *  Dummy notification needed to fulfill Androids security restrictions for background operations
     */
    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Tracking Active")
                .setContentText("Your location is being tracked")
                .setSmallIcon(R.drawable.location_on)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
