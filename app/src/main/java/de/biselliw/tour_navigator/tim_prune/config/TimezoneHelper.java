package de.biselliw.tour_navigator.tim_prune.config;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Abstract class to return the system wide timezone
 *
 * @author BiselliW
 */
public abstract class TimezoneHelper
{
	private static TimeZone timeZone = null;
	private static String timeZoneStr = "Europe/Berlin";

	/**
	 * @return the system timezone
	 */
	public static TimeZone getSelectedTimezone()
	{
		if (timeZone == null)
		{
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				ZoneId zone = ZoneId.systemDefault();
				timeZoneStr = zone.toString();
			}
			timeZone = TimeZone.getTimeZone(timeZoneStr);
		}
		return timeZone;
	}

	public static String getSelectedTimezoneStr()
	{
		if (timeZone == null)
			getSelectedTimezone();
		return timeZoneStr;
	}
}
