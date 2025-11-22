package de.biselliw.tour_navigator.helpers;

import java.util.Arrays;

import androidx.annotation.NonNull;
import de.biselliw.tour_navigator.helpers.Log;


public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    /**
     * TAG for log messages.
     */
    static final String TAG = "GlobalExceptionHandler";

    private final Thread.UncaughtExceptionHandler defaultHandler;

    public GlobalExceptionHandler() {
        // Keep a reference to the system’s default handler
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        // Log or save the crash data here
        Log.e(TAG, "Uncaught exception", throwable);
        Log.Close();

        // Pass the exception on to the default handler so the app can terminate normally
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}