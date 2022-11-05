package de.biselliw.tour_navigator.stubs;

// Basic class required for Android app

import java.util.Properties;

import tim.prune.data.UnitSet;
import tim.prune.data.UnitSetLibrary;
/**
 * Abstract class to hold application-wide configuration
 */
public abstract class Config
{

	/** key/value pairs containing all config values */
	private static Properties _configValues = null;
	/** Current unit set */
	private static UnitSet _unitSet = UnitSetLibrary.getUnitSet(null);


	/** Key for altitude tolerance */
	public static final String KEY_ALTITUDE_TOLERANCE = "prune.altitudetolerance";
	/** Initialise the default properties */
	static
	{
		_configValues = getDefaultProperties();
	}
	/**
	/**
	/**
	 * @return Properties object containing default values
	 */
	private static Properties getDefaultProperties()
	{
		Properties props = new Properties();

		return props;
	}
	/**
	 * @param inString String to parse
	 * @return int value of String, or 0 if unparseable
	 */
	private static int parseInt(String inString)
	{
		int val = 0;
		try {
			val = Integer.parseInt(inString);
		}
		catch (Exception e) {} // ignore, value stays zero
		return val;
	}

	/**
	/**
	 * Get the given configuration setting as a String
	 * @param inKey key
	 * @return configuration setting as a String
	 */
	public static String getConfigString(String inKey)
	{
		return _configValues.getProperty(inKey);
	}

	/**
	 * Get the given configuration setting as a boolean
	 * @param inKey key
	 * @return configuration setting as a boolean (default to true)
	 */
	public static boolean getConfigBoolean(String inKey)
	{
		String val = _configValues.getProperty(inKey);
		return (val == null || val.equals("1"));
	}

	/**
	 * Get the given configuration setting as an int
	 * @param inKey key
	 * @return configuration setting as an int
	 */
	public static int getConfigInt(String inKey)
	{
		return parseInt(_configValues.getProperty(inKey));
	}

	/**
	 * @return the current unit set
	 */
	public static UnitSet getUnitSet() {
		return _unitSet;
	}
}
