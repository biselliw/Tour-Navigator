package de.biselliw.tour_navigator.tim_prune.config;

import de.biselliw.tour_navigator.tim.prune.data.UnitSet;
import de.biselliw.tour_navigator.tim.prune.data.UnitSetLibrary;

/**
 * Abstract class to hold application-wide configuration
 * @author BiselliW
 */
public abstract class Config
{
	/**
	 * @return the current unit set
	 */
	public static UnitSet getUnitSet() {
		return UnitSetLibrary.getUnitSet(null);
	}

}
