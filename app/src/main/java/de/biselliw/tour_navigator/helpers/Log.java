package de.biselliw.tour_navigator.helpers;
import android.os.Build;
import android.text.format.Time;

import java.io.File;
import java.io.FileWriter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;

import androidx.annotation.NonNull;
import de.biselliw.tour_navigator.BuildConfig;

import static de.biselliw.tour_navigator.files.FileUtils.getDownloadsDir;

/// class for sending log output.
/// @link https://developer.android.com/reference/android/util/Log
public final class Log {
    private static boolean _writing_enabled = false;
    private static String _prefix = "";
    private static FileWriter _writer = null;

    /**
     * Enable writing to log file
     * @param enabled true if writing is enabled
     * @param prefix prefix of the log file name in internal download dir
     */
    public static void setWritingEnabled (boolean enabled, String prefix) {
        _writing_enabled = enabled;
        _prefix = prefix;
        if (!enabled)
            Close ();
    }

    /**
     * Create a log file
     * @return true if log file writing is enabled and log file has been created
     */
    private static boolean CreateLogFile () {
        boolean result = false;
        if (_writing_enabled) {
            result = true;
            try {
                if (_writer == null) {
                    File dir = new File(getDownloadsDir(), "logs");
                    if (!dir.exists()) result = dir.mkdirs();
                    if (result) {
                        String date_time;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            LocalDateTime now = LocalDateTime.now();
                            date_time = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                        }
                        else {
                            Time now = new Time(); now.setToNow();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                            Calendar calender = Calendar.getInstance();
                            calender.setTimeInMillis(now.toMillis(true));
                            date_time = sdf.format(calender.getTime());
                        }

                        File _file = new File(dir, _prefix + date_time + ".log");
                        _writer = new FileWriter(_file);
                    }
                }
            } catch (Exception e) {
                _writer = null;
                result = false;
                Log.e("LOG", "Error while creating log file", e);
            }
        }
        return result;
    }

    /**
     * Write to log file
     * @param msg string to log
     * @return 1 if successful
     */
    private static int Write (String msg)
    {
        int result = 0;
        if (CreateLogFile () )
            try {
                String date_time;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalDateTime now = LocalDateTime.now();
                    date_time = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS: "));
                }
                else {
                    Time now = new Time(); now.setToNow();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS: ");
                    Calendar calender = Calendar.getInstance();
                    calender.setTimeInMillis(now.toMillis(true));
                    date_time = sdf.format(calender.getTime());
                }

                _writer.write (date_time + msg + "\r\n");
                _writer.flush();
                result = 1;
            } catch (Exception e) {
                _writer = null;
                Log.e("LOG", "Error while writing log file", e);
            }
        return result;
    }

    public static void Close () {
        try {
            if (_writer != null)
                _writer.close();
        } catch (Exception e) {
            Log.e("LOG", "Error while closing log file", e);
        }
        _writer = null;
    }

    public static int d(java.lang.String tag, java.lang.String msg) {
        Write("D " + tag + " - " + msg);
        if (BuildConfig.DEBUG)
            return android.util.Log.d(tag, msg);
        else  return 0;
    }

    public static int e(java.lang.String tag, java.lang.String msg) {
        Write("E " + tag + " - " + msg);
        if (BuildConfig.DEBUG)
            return android.util.Log.e(tag, msg);
        else  return 0;
    }

    public static int e(java.lang.String tag, java.lang.String msg, @NonNull Throwable tr) {
        Write("E " + tag + " - " + msg + ": "
                + tr.toString()
                + " called by "
                + Arrays.toString(tr.getStackTrace())) ;
        if (BuildConfig.DEBUG)
            return android.util.Log.e(tag, msg, tr);
        else  return 0;
    }
 }

