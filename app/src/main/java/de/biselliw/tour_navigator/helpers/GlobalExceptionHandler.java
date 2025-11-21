package de.biselliw.tour_navigator.helpers;

import de.biselliw.tour_navigator.helpers.Log
        ;
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultHandler;

    public GlobalExceptionHandler() {
        // Keep a reference to the system’s default handler
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // Log or save the crash data here
        Log.e("GlobalExceptionHandler", "Uncaught exception", throwable);

        // TODO: Write to file, send to server, etc.

        // Pass the exception on to the default handler so the app can terminate normally
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}