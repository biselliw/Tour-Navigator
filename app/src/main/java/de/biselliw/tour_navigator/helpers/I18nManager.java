package de.biselliw.tour_navigator.helpers;

import android.content.Context;
import android.content.res.Resources;

import de.biselliw.tour_navigator.R;

/**
 * Stub Manager for all internationalization
 * Responsible for delivering language-specific texts
 *
 * @author BiselliW
 */
public abstract class I18nManager
{
	static private Resources _res = null;

	/**
	 * Initialize the library using the context
	 */
	public static void init (Context inContext) {
		if (inContext != null)
			_res = inContext.getResources();
	}

	/**
	 * Lookup the given key and return the associated text
	 * @param inKey key to lookup
	 * @return associated text, or the key if not found
	 */
	public static String getText(String inKey)
	{
		if (_res != null) {
			if (inKey.equals("fieldname.waypointstart"))
				return _res.getString(R.string.start);
			else if (inKey.equals("fieldname.waypointend"))
				return _res.getString(R.string.destination);
			else if (inKey.equals("pref_hiking_par_set_to_default"))
				return _res.getString(R.string.pref_hiking_par_set_to_default);
			else
				/* return the key itself
				  - "fieldname.custom"
				 */
				return inKey;
		}
		else
			// return the key itself
			return inKey;
	}

}
