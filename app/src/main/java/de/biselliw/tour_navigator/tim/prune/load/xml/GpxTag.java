package de.biselliw.tour_navigator.tim.prune.load.xml;

/**
 * Class to hold a single tag value from a gpx file
 * @since 26.1
 */
public class GpxTag
{
	/** value of tag */
	private String _value = null;

	/**
	 * @param inVal value to set
	 */
	public void setValue(String inVal) {
		_value = inVal;
	}

	public void clear() {
		setValue("");
	}

	/**
	 * @return value
	 */
	public String getValue() {
		return _value == null ? "" : _value.trim();
	}
}
