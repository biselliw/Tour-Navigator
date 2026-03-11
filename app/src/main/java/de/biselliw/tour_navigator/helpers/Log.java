package de.biselliw.tour_navigator.helpers;

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

import android.os.Build;
import android.text.format.Time;

import java.io.File;
import java.io.FileWriter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;

import de.biselliw.tour_navigator.BuildConfig;

import static de.biselliw.tour_navigator.files.FileUtils.getDownloadsDir;

/**
 * class for sending log output
 * @link <a href="https://developer.android.com/reference/android/util/Log">https://developer.android.com</a>
 */
public final class Log {
    private static boolean _writing_enabled = false;
    private static String _prefix = "";
    private static FileWriter _writer = null;

    private static String _debugHTML = "";
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
     * format a given time stamp
     * @param inTime time in seconds
     * @return formatted string: HH:mm:ss
     */
    public static String formatHourMinSecs (long inTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY); // .GERMAN);;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, (int) (inTime % 60L));
        inTime /= 60L;
        calendar.set(Calendar.MINUTE, (int) (inTime % 60));
        inTime /= 60L;
        calendar.set(Calendar.HOUR, (int) inTime);
        String formatted = sdf.format(calendar.getTime());
        return formatted;
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
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(now.toMillis(true));
                            date_time = sdf.format(calendar.getTime());
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
     * @param tag  String: Used to identify the source of a log message.
     * @param type Log type: "D", "I", "W", "E"
     * @param msg string to log
     * @return 1 if successful
     */
    private static int Write (String tag, String type, String msg)
    {
        int result = 0;
        if (CreateLogFile () )
            try {
                String date_time;
                LocalDateTime now = LocalDateTime.now();
                date_time = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS: "));

                _writer.write (date_time + "\t" + tag + "\t" + type + "\t"+ msg + "\r\n");
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

    /**
     * Send a DEBUG log message
     * @param tag   String: Used to identify the source of a log message. It usually identifies
     *                  the class or activity where the log call occurs. This value may be null.
     * @param msg   String: The message you would like logged.
     * @return      A positive value if the message was loggable
     */
    public static int d(java.lang.String tag, java.lang.String msg) {
        if (_writing_enabled)
            Write(tag,"D", msg);
        if (BuildConfig.DEBUG) {
            return android.util.Log.d(tag, msg);
        }
        return 0;
    }

    /**
     * Send an INFO log message
     * @param tag   String: Used to identify the source of a log message. It usually identifies
     *                  the class or activity where the log call occurs. This value may be null.
     * @param msg   String: The message you would like logged.
     * @return      A positive value if the message was loggable
     */
    public static int i(java.lang.String tag, java.lang.String msg) {
        if (_writing_enabled)
            Write(tag, "I", msg);
        if (BuildConfig.DEBUG) {
            return android.util.Log.i(tag, msg);
        }
        return 0;
    }

    /**
     * Send an ERROR log message and log the exception.
     * @param tag   String: Used to identify the source of a log message. It usually identifies
     *                  the class or activity where the log call occurs. This value may be null.
     * @param msg   String: The message you would like logged.
     * @return      A positive value if the message was loggable
     */
    public static int e(java.lang.String tag, java.lang.String msg) {
        if (_writing_enabled) {
            Write(tag, "E", msg);
            addDebugHTML(tag, "E", msg);
        }
        if (BuildConfig.DEBUG) {
            return android.util.Log.e(tag, msg);
        }
        return 0;
    }

    /**
     * Send an ERROR log message and log the exception.
     * @param tag   String: Used to identify the source of a log message. It usually identifies
     *                  the class or activity where the log call occurs. This value may be null.
     * @param msg   String: The message you would like logged.
     * @param tr    Throwable: An exception to log.
     * @return      A positive value if the message was loggable
     */
    public static int e(java.lang.String tag, java.lang.String msg, @NonNull Throwable tr) {
        msg = msg + ": " + tr
                + " called by " + Arrays.toString(tr.getStackTrace());
        if (_writing_enabled) {
            addDebugHTML(tag, "E", msg);
            Write(tag, "E", msg);
        }
        if (BuildConfig.DEBUG) {
            return android.util.Log.e(tag, msg, tr);
        }
        return 0;
    }

    /**
     * Send a WARN log message and log the exception.
     * @param tag   String: Used to identify the source of a log message. It usually identifies
     *                  the class or activity where the log call occurs. This value may be null.
     * @param msg   String: The message you would like logged.
     * @return      A positive value if the message was loggable
     */
    public static int w(java.lang.String tag, java.lang.String msg) {
        if (_writing_enabled) {
            addDebugHTML(tag, "W", msg);
            Write(tag,"W", msg);
        }
        if (BuildConfig.DEBUG) {
            return android.util.Log.w(tag, msg);
        }
        return 0;
    }

    public static void clearDebugHTML() { _debugHTML = ""; }

    /**
     * Add log to HTML string
     * @param tag  String: Used to identify the source of a log message.
     * @param type Log type: "D", "I", "W", "E"
     * @param msg string to log
     */
    private static void addDebugHTML (String tag, String type, String msg) {
        if (type.equals("E"))
            msg = "<red>E " + msg + "</red>";
        else
            msg = type + " " + msg;
        _debugHTML = _debugHTML + tag + " " + msg + "<br>";
    }

    public static String getDebugHTML() { return getDebugHTML(""); }
    public static boolean isLoggedDebugHTML() { // return false;
        return !_debugHTML.isEmpty();
    }
    public static String getDebugHTML(java.lang.String title)
    {
        String msg = "";
        if (!title.isEmpty()) msg = "<b>" + title + "</b><br>";
        return msg + _debugHTML;
    }
}

