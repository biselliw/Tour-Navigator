package de.biselliw.tour_navigator.data;

import androidx.appcompat.app.AppCompatActivity;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.helpers.Log;


public class Resources {
    public static AppCompatActivity activity;   // Not Leaking

    public static android.content.res.Resources resources = null;

    public static String resStrCreator = "";
    public static String resStrSetToDefault;

    public static void getResources() {
        resStrCreator = getString(R.string.app_name);
        resStrSetToDefault = getString(R.string.set_to_default);
    }

    public static java.lang.String getString(int id) throws android.content.res.Resources.NotFoundException {
        return Resources.resources.getString(id);
    }

    public static int get_Color(int resID) {
        int color = 0xAA000000;
        try {
            color = activity.getColor(resID);
        } catch (Exception e) {
            Log.e("Resources", "color resource not found: " + resID);
        }
        return color;
    }

}
