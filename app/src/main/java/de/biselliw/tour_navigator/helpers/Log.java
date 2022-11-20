package de.biselliw.tour_navigator.helpers;

import de.biselliw.tour_navigator.BuildConfig;

public final class Log {

    public static int d(java.lang.String tag, java.lang.String msg) {
        if (BuildConfig.DEBUG)
            return android.util.Log.d(tag, msg);
        else  return 0;
    }
 }

