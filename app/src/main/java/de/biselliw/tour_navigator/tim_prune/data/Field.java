package de.biselliw.tour_navigator.tim_prune.data;

import de.biselliw.tour_navigator.tim.prune.data.FileType;

/**
 * Class to represent a field of a data point
 * @implNote BiselliW: new fields WAYPT_SYM, WAYPT_DUR, WAYPT_FLAG, WAYPT_LINK
 * @since 26.1
*/
public class Field
{
	private final String _labelKey;
	private final FileType _fileType = null;

	public static final Field LATITUDE = new Field("latitude");
	public static final Field LONGITUDE = new Field("longitude");
	public static final Field ALTITUDE = new Field("altitude");
	public static final Field TIMESTAMP = new Field("timestamp");
	public static final Field WAYPT_NAME = new Field("waypointname");
	public static final Field WAYPT_TYPE = new Field("waypointtype");
	public static final Field DESCRIPTION = new Field("description");
	public static final Field COMMENT = new Field("comment");
	public static final Field SYMBOL = new Field("symbol");
	public static final Field NEW_SEGMENT = new Field("newsegment");
	private String _customLabel = null;


//	public static final Field SPEED = new Field("speed");
//	public static final Field VERTICAL_SPEED = new Field("verticalspeed");
//	public static final Field GRADIENT = new Field("gradient");

	public static final Field PHOTO = new Field("photo");
	public static final Field AUDIO = new Field("audio");

	public static final Field WAYPT_DUR = new Field("fieldname.waypointdur", true);
	public static final Field WAYPT_FLAG = new Field("fieldname.waypointflag", true);
	public static final Field WAYPT_LINK = new Field("fieldname.waypointlink", true);


	/** List of all the available built-in fields */
	private static final Field[] ALL_AVAILABLE_FIELDS = {
		LATITUDE, LONGITUDE, ALTITUDE, TIMESTAMP, WAYPT_NAME, WAYPT_TYPE, DESCRIPTION, NEW_SEGMENT
	//	SPEED, VERTICAL_SPEED

        ,SYMBOL, WAYPT_DUR, WAYPT_FLAG, WAYPT_LINK,
		new Field(("custom"))
	};

	/**
	 * Private constructor
	 * @param inLabelKey Key for label texts
	 */
	private Field(String inLabelKey)
	{
		_labelKey = "fieldname." + inLabelKey;
	//	_fileType = null;
	}

	/**
	 * Constructor for subtypes
	 * @param inFileType file type for which this field is valid
	 */
	protected Field(FileType inFileType)
	{
		_labelKey = null;
	//	_fileType = inFileType;
	}

	/**
	 * Private constructor
	 * @param inLabelKey Key for label texts
	 * @param inBuiltin true for built-in types, false for custom
	 */
	private Field(String inLabelKey, boolean inBuiltin)
	{
		if (inBuiltin) {
			_labelKey = inLabelKey;
			_customLabel = null;
		}
		else {
			_labelKey = null;
			_customLabel = inLabelKey;
		}
	}

	/**
	 * Public constructor for custom fields
	 * @param // inLabel label to use for display
	 * /
	public Field(String inLabel)
	{
		this(inLabel, false);
	}

	/**
	 * @return the name of the field
	 */
	public String getName()
	{
		if (_labelKey != null) {
			return _labelKey;
		}
		return _customLabel;
	}

	public void setName(String inName) {
		// ignored, only used for custom fields
	}

	/** @return true if this field is specific to the given file type */
	public boolean isSpecificToFileType(FileType inFileType) {
		return _fileType == inFileType;
	}

	/** @return true if this field can be saved as the given file type */
	public boolean matchesFileType(FileType inFileType) {
		return _fileType == null || _fileType == inFileType;
	}

	/**
	 * Checks if the two fields are equal
	 * @param inOther other Field object
	 * @return true if Fields identical
	 */
	public boolean equals(Object inOther)
	{
		return inOther != null
				&& getClass().equals(inOther.getClass())
				&& getName().equals(((Field) inOther).getName());
	}

	/** @return true if this is a built-in field, not a custom field or extension */
	public final boolean isBuiltIn() {
		return getClass() == Field.class;
	}

	/**
	 * Get the field for the given field name
	 * @param inFieldName name of field to look for
	 * @return Field if found, or null otherwise
	 */
	public static Field getField(String inFieldName)
	{
		for (Field field : ALL_AVAILABLE_FIELDS)
		{
			if (field.getName().equals(inFieldName)) {
				return field;
			}
		}
		// not found
		return null;
	}

	/**
	 * @return array of field names
	 */
	public static String[] getFieldNames()
	{
		String[] names = new String[ALL_AVAILABLE_FIELDS.length];
		for (int i=0; i<ALL_AVAILABLE_FIELDS.length; i++) {
			names[i] = ALL_AVAILABLE_FIELDS[i].getName();
		}
		return names;
	}
}
