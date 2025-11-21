package de.biselliw.tour_navigator.helpers;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import de.biselliw.tour_navigator.BuildConfig;

import static de.biselliw.tour_navigator.files.FileUtils.getDownloadsDir;

public final class Log {
    private static boolean _writing_enabled = false;
    private static File _file = null;
    private static FileWriter _writer = null;


    public static void setWritingEnabled (boolean enabled, String prefix) {
        try {
            if (enabled && (_writer == null)) {
                File dir = new File(getDownloadsDir(), "logs");
                if (!dir.exists()) enabled = dir.mkdirs();
                if (enabled) {
                    LocalDateTime now = LocalDateTime.now();
                    String date_time = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                    _file = new File(dir, prefix + date_time + ".log");
                    _writer = new FileWriter(_file);
                }
                _writing_enabled = enabled;
            }
            if (!enabled)
                Close ();
        } catch (Exception e) {
            _writer = null;
            Log.e("LOG", "Error while creating log file", e);
        }
    }

    private static int Write (String msg)
    {
        int result = 0;
        try {
            if( _writing_enabled && (_writer != null) ) {
                LocalDateTime now = LocalDateTime.now();
                String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss: "));

                _writer.write (formatted + msg + "\r\n");
                _writer.flush();
                result = 1;
            }
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
    public static int e(java.lang.String tag, java.lang.String msg, Throwable tr) {
        Write("E " + tag + " - " + msg);
        if (BuildConfig.DEBUG)
            return android.util.Log.e(tag, msg, tr);
        else  return 0;
    }
 }

