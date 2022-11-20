package de.biselliw.tour_navigator.tim_prune.gui.MapUtils;

/**
 * Stub class to manage coordinate conversions and other stuff for maps
 *
 * @author tim.prune
 */
public abstract class MapUtils
{
	/**
	 * Transform a longitude into an x coordinate
	 * @param inLon longitude in degrees
	 * @return scaled X value from 0 to 1
	 */
	public static double getXFromLongitude(double inLon)
	{
		return (inLon + 180.0) / 360.0;
	}

	/**
	 * Transform a latitude into a y coordinate
	 * @param inLat latitude in degrees
	 * @return scaled Y value from 0 to 1
	 */
	public static double getYFromLatitude(double inLat)
	{
		return (1 - Math.log(Math.tan(inLat * Math.PI / 180) + 1 / Math.cos(inLat * Math.PI / 180)) / Math.PI) / 2;
	}

}
