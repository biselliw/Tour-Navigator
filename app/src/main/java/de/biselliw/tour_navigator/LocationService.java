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

import de.biselliw.tour_navigator.helpers.Log;

import static de.biselliw.tour_navigator.Notifications.ACTION_LOCATION_UPDATE;

/**
 * service class for Fused Location Provider running in background
 * @since 1.6
 */
public class LocationService extends Service {
    /**
     * TAG for log messages.
     */
    static final String TAG = "LocationService";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    private static final String CHANNEL_ID = "location_channel";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        boolean allPermissionsGranted = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!allPermissionsGranted) {
            if (DEBUG) Log.w(TAG,"foreground service for Fused Location Provider could not be started: no GPS permission");
            stopSelf();
            return;
        }

        try {
            createNotificationChannel();
            startForeground(1, buildNotification());
            Log.i(TAG, "foreground service started for Fused Location Provider");

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // Create a LocationRequest
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1000
            ).setMinUpdateDistanceMeters(0)
                    .build();

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    for (Location location : result.getLocations()) {
                        handleLocation(location);
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (Exception e) {
            Log.e(TAG,"error: ",e);
        }
    }

    /**
     * handle a single location and send a broadcast message
     * @param location received location data
     * todo store locations in background
     */
    private void handleLocation(Location location) {
        if (DEBUG) Log.d(TAG, location.getLatitude() + ", " + location.getLongitude() + " acc: " + location.getAccuracy());

        Intent intent = new Intent(ACTION_LOCATION_UPDATE);
        intent.setPackage(getPackageName());   // important for implicit broadcasts
        intent.putExtra("location", location);
        sendBroadcast(intent);
    }

    /**
     *  Notification needed to fulfill Androids security restrictions for background operations
     *  todo: check if requirement is need in AndroidManifest
     */
    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notify_gps_active))
                .setContentText(getString(R.string.notify_gps_reason))
                .setSmallIcon(R.drawable.hiking) // R.id.image_location_on) //
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
            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
