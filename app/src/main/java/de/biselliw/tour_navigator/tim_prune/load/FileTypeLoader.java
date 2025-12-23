package de.biselliw.tour_navigator.tim_prune.load;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.App;
import tim.prune.data.FieldList;
import tim.prune.data.PointCreateOptions;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.tim_prune.data.SourceInfo;
import de.biselliw.tour_navigator.tim_prune.load.xml.XmlHandler;

/**
 * Superclass of all the type-specific file loaders
 */
public class FileTypeLoader
{
	/** App for callback of file loading */
	private final App _app;

	public FileTypeLoader(App inApp) {
		_app = inApp;
	}

	protected App getApp() {
		return _app;
	}

	/**
	 * Subclasses call this method to create the command and execute it
	 * @param inPointList list of points created from data
	 * @param inSourceInfo information about the data source
	 * @param inAppend true to append, false to replace
	 */
	protected void loadData(List<DataPoint> inPointList, SourceInfo inSourceInfo, boolean inAppend)
	{
		// Set the source info on each of the created points
		int index = 0;
		for (DataPoint point : inPointList) {
			point.setSourceInfo(inSourceInfo);
			point.setOriginalIndex(index++);
		}
		if (inSourceInfo != null) {
			inSourceInfo.setNumPoints(inPointList.size());
		}

        _app.onLoadData(inPointList, inSourceInfo, inAppend);
	}

	/**
	 * @return filename from the source info
	 */
	private String getFilename(SourceInfo inSourceInfo)
	{
		if (inSourceInfo == null || inSourceInfo.getFile() == null) {
			return "";
		}
		String name = inSourceInfo.getFile().getName();
		if (name.length() > 20) {
			return name.substring(0, 20) + "...";
		}
		return name;
	}

	/**
	 * Create a list of points from the loaded data
	 * @param inFields array of fields
	 * @param inData data strings, in order
	 * @param inOptions selected options like units
	 * @return list of created points
	 */
	protected List<DataPoint> createPoints(Field[] inFields, Object[][] inData,
		PointCreateOptions inOptions)
	{
		ArrayList<DataPoint> points = new ArrayList<>();
		boolean firstTrackPoint = true;
		FieldList fields = new FieldList(inFields);
		for (Object[] objects : inData)
		{
			DataPoint point = new DataPoint((String[]) objects, fields, inOptions);
			if (point.isValid())
			{
				if (firstTrackPoint && !point.isWaypoint())
				{
					point.setSegmentStart(true);
					firstTrackPoint = false;
				}
				points.add(point);
			}
		}
		return points;
	}

	/**
	 * Load the data from the xml handler
	 * @param inHandler xml handler which read the data from GPSBabel
	 * @param inSourceInfo info about file (or not)
	 * @param inAutoAppend true to auto-append
	 * // @param inMediaLinks media links, if any
	 */
	public void loadData(XmlHandler inHandler, SourceInfo inSourceInfo,boolean inAutoAppend)
	{
		// give data to App
		List<DataPoint> points = createPoints(inHandler.getFieldArray(),
			inHandler.getDataArray(), null);
		loadData(points, inSourceInfo, inAutoAppend);
	}
}
