package de.biselliw.tour_navigator.stubs;

import java.util.TimeZone;

public abstract class TimezoneHelper
{

	/**
	 * @return the timezone selected in the Config
	 */
	public static TimeZone getSelectedTimezone()
	{
			return TimeZone.getDefault();

	}

}
