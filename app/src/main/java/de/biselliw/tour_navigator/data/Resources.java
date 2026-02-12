package de.biselliw.tour_navigator.data;

import de.biselliw.tour_navigator.R;

public class Resources {
    // FIXME potential memory leak
    public static android.content.res.Resources resources = null; // todo make resources non static

    public static String Creator = "";

    public static java.lang.String getString(int id) throws android.content.res.Resources.NotFoundException {
        return Resources.resources.getString(id);
    }

}
