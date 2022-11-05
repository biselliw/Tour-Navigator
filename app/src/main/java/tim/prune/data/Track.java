package tim.prune.data;
/** @since WB */

import java.util.List;

import de.biselliw.tour_navigator.stubs.Photo;
import tim.prune.UpdateMessageBroker;
import tim.prune.function.edit.FieldEdit;
import tim.prune.function.edit.FieldEditList;

import de.biselliw.tour_navigator.data.BaseTrack;


/**
 * Class to hold all track information,
 * including track points and waypoints
 * @since WB
 * 
 * - _hasNearestWaypoint
 */
public class Track extends BaseTrack
{

	/**
	 * Constructor for empty track
	 */
	public Track()
	{
		super();
	}

	/**
	 * Constructor using fields and points from another Track
	 * @param inFieldList Field list from another Track object
	 * @param inPoints (edited) point array
	 */
	public Track(FieldList inFieldList, DataPoint[] inPoints)
	{
		_masterFieldList = inFieldList;
		_dataPoints = inPoints;
		if (_dataPoints == null) _dataPoints = new DataPoint[0];
		_numPoints = _dataPoints.length;
		_scaled = false;
	}



	/**
	 * Load the track by transferring the contents from a loaded Track object
	 * @param inOther Track object containing loaded data
	 */
	public void load(Track inOther)
	{
		_numPoints = inOther._numPoints;
		_masterFieldList = inOther._masterFieldList;
		_dataPoints = inOther._dataPoints;
		// needs to be scaled
		_scaled = false;
	}


	/**
	 * Extend the track's field list with the given additional fields
	 * @param inFieldList list of fields to be added
	 */
	public void extendFieldList(FieldList inFieldList)
	{
		_masterFieldList = _masterFieldList.merge(inFieldList);
	}

	////////////////// Modification methods //////////////////////


	/**
	 * Combine this Track with new data
	 * @param inOtherTrack other track to combine
	 */
	public void combine(Track inOtherTrack)
	{
		// merge field list
		_masterFieldList = _masterFieldList.merge(inOtherTrack._masterFieldList);
		// expand data array and add other track's data points
		int totalPoints = getNumPoints() + inOtherTrack.getNumPoints();
		DataPoint[] mergedPoints = new DataPoint[totalPoints];
		System.arraycopy(_dataPoints, 0, mergedPoints, 0, getNumPoints());
		System.arraycopy(inOtherTrack._dataPoints, 0, mergedPoints, getNumPoints(), inOtherTrack.getNumPoints());
		_dataPoints = mergedPoints;
		// combine point count
		_numPoints = totalPoints;
		// needs to be scaled again
		_scaled = false;
		// inform listeners
		UpdateMessageBroker.informSubscribers();
	}


	/**
	 * Crop the track to the given size - subsequent points are not (yet) deleted
	 * @param inNewSize new number of points in track
	 */
	public void cropTo(int inNewSize)
	{
		if (inNewSize >= 0 && inNewSize < getNumPoints())
		{
			_numPoints = inNewSize;
			// needs to be scaled again
			_scaled = false;
			UpdateMessageBroker.informSubscribers();
		}
	}


	/**
	 * Delete the points marked for deletion
	 * @param inSplitSegments true to split segments at deleted points
	 * @return number of points deleted
	 */
	public int deleteMarkedPoints(boolean inSplitSegments)
	{
		int numCopied = 0;
		// Copy selected points into a new point array
		DataPoint[] newPointArray = new DataPoint[_numPoints];
		boolean prevPointDeleted = false;
		for (int i=0; i<_numPoints; i++)
		{
			DataPoint point = _dataPoints[i];
			// Don't delete photo points
			if (point.hasMedia() || !point.getDeleteFlag())
			{
				if (prevPointDeleted && inSplitSegments) {
					point.setSegmentStart(true);
				}
				newPointArray[numCopied] = point;
				numCopied++;
				prevPointDeleted = false;
			}
			else {
				prevPointDeleted = true;
			}
		}

		// Copy array references
		int numDeleted = _numPoints - numCopied;
		if (numDeleted > 0)
		{
			_dataPoints = new DataPoint[numCopied];
			System.arraycopy(newPointArray, 0, _dataPoints, 0, numCopied);
			_numPoints = _dataPoints.length;
			_scaled = false;
		}
		return numDeleted;
	}


	/**
	 * Delete the specified point
	 * @param inIndex point index
	 * @return true if successful
	 */
	public boolean deletePoint(int inIndex) {
		return deleteRange(inIndex, inIndex);
	}


	/**
	 * Delete the specified range of points from the Track
	 * @param inStart start of range (inclusive)
	 * @param inEnd end of range (inclusive)
	 * @return true if successful
	 */
	public boolean deleteRange(int inStart, int inEnd)
	{
		if (inStart < 0 || inEnd < 0 || inEnd < inStart)
		{
			// no valid range selected so can't delete
			return false;
		}
		// check through range to be deleted, and see if any new segment flags present
		boolean hasSegmentStart = false;
		DataPoint nextTrackPoint = getNextTrackPoint(inEnd+1);
		if (nextTrackPoint != null) {
			for (int i=inStart; i<=inEnd && !hasSegmentStart; i++) {
				hasSegmentStart |= _dataPoints[i].getSegmentStart();
			}
			// If segment break found, make sure next trackpoint also has break
			if (hasSegmentStart) {nextTrackPoint.setSegmentStart(true);}
		}
		// valid range, let's delete it
		int numToDelete = inEnd - inStart + 1;
		DataPoint[] newPointArray = new DataPoint[_numPoints - numToDelete];
		// Copy points before the selected range
		if (inStart > 0)
		{
			System.arraycopy(_dataPoints, 0, newPointArray, 0, inStart);
		}
		// Copy points after the deleted one(s)
		if (inEnd < (_numPoints - 1))
		{
			System.arraycopy(_dataPoints, inEnd + 1, newPointArray, inStart,
				_numPoints - inEnd - 1);
		}
		// Copy points over original array
		_dataPoints = newPointArray;
		_numPoints -= numToDelete;
		// needs to be scaled again
		_scaled = false;
		return true;
	}


	/**
	 * Reverse the specified range of points
	 * @param inStart start index
	 * @param inEnd end index
	 * @return true if successful, false otherwise
	 */
	public boolean reverseRange(int inStart, int inEnd)
	{
		if (inStart < 0 || inEnd < 0 || inStart >= inEnd || inEnd >= _numPoints)
		{
			return false;
		}
		// calculate how many point swaps are required
		int numPointsToReverse = (inEnd - inStart + 1) / 2;
		DataPoint p = null;
		for (int i=0; i<numPointsToReverse; i++)
		{
			// swap pairs of points
			p = _dataPoints[inStart + i];
			_dataPoints[inStart + i] = _dataPoints[inEnd - i];
			_dataPoints[inEnd - i] = p;
		}
		// adjust segment starts
		shiftSegmentStarts(inStart, inEnd);
		// Find first track point and following track point, and set segment starts to true
		DataPoint firstTrackPoint = getNextTrackPoint(inStart);
		if (firstTrackPoint != null) {firstTrackPoint.setSegmentStart(true);}
		DataPoint nextTrackPoint = getNextTrackPoint(inEnd+1);
		if (nextTrackPoint != null) {nextTrackPoint.setSegmentStart(true);}
		// needs to be scaled again
		_scaled = false;
		UpdateMessageBroker.informSubscribers();
		return true;
	}


	/**
	 * Add the given time offset to the specified range
	 * @param inStart start of range
	 * @param inEnd end of range
	 * @param inOffset offset to add (-ve to subtract)
	 * @param inUndo true for undo operation
	 * @return true on success
	 */
	public boolean addTimeOffsetSeconds(int inStart, int inEnd, long inOffset, boolean inUndo)
	{
		// sanity check
		if (inStart < 0 || inEnd < 0 || inStart >= inEnd || inEnd >= _numPoints) {
			return false;
		}
		boolean foundTimestamp = false;
		// Loop over all points within range
		for (int i=inStart; i<=inEnd; i++)
		{
			DataPoint p = _dataPoints[i];
			if (p != null && p.hasTimestamp())
			{
				// This point has a timestamp so add the offset to it
				foundTimestamp = true;
				p.addTimeOffsetSeconds(inOffset);
				p.setModified(inUndo);
			}
		}
		return foundTimestamp;
	}

	/**
	 * Add the given altitude offset to the specified range
	 * @param inStart start of range
	 * @param inEnd end of range
	 * @param inOffset offset to add (-ve to subtract)
	 * @param inUnit altitude unit of offset
	 * @param inDecimals number of decimal places in offset
	 * @return true on success
	 */
	public boolean addAltitudeOffset(int inStart, int inEnd, double inOffset,
	 Unit inUnit, int inDecimals)
	{
		// sanity check
		if (inStart < 0 || inEnd < 0 || inStart >= inEnd || inEnd >= _numPoints) {
			return false;
		}
		boolean foundAlt = false;
		// Loop over all points within range
		for (int i=inStart; i<=inEnd; i++)
		{
			DataPoint p = _dataPoints[i];
			if (p != null && p.hasAltitude())
			{
				// This point has an altitude so add the offset to it
				foundAlt = true;
				p.addAltitudeOffset(inOffset, inUnit, inDecimals);
				p.setModified(false);
			}
		}
		// needs to be scaled again
		_scaled = false;
		return foundAlt;
	}



	/**
	 * Cut and move the specified section
	 * @param inSectionStart start index of section
	 * @param inSectionEnd end index of section
	 * @param inMoveTo index of move to point
	 * @return true if move successful
	 */
	public boolean cutAndMoveSection(int inSectionStart, int inSectionEnd, int inMoveTo)
	{
		// TODO: Move cut/move into separate function?
		// Check that indices make sense
		if (inSectionStart >= 0 && inSectionEnd > inSectionStart && inMoveTo >= 0
			&& (inMoveTo < inSectionStart || inMoveTo > (inSectionEnd+1)))
		{
			// do the cut and move
			DataPoint[] newPointArray = new DataPoint[_numPoints];
			// System.out.println("Cut/move section (" + inSectionStart + " - " + inSectionEnd + ") to before point " + inMoveTo);
			// Is it a forward copy or a backward copy?
			if (inSectionStart > inMoveTo)
			{
				int sectionLength = inSectionEnd - inSectionStart + 1;
				// move section to earlier point
				if (inMoveTo > 0) {
					System.arraycopy(_dataPoints, 0, newPointArray, 0, inMoveTo); // unchanged points before
				}
				System.arraycopy(_dataPoints, inSectionStart, newPointArray, inMoveTo, sectionLength); // moved bit
				// after insertion point, before moved bit
				System.arraycopy(_dataPoints, inMoveTo, newPointArray, inMoveTo + sectionLength, inSectionStart - inMoveTo);
				// after moved bit
				if (inSectionEnd < (_numPoints - 1)) {
					System.arraycopy(_dataPoints, inSectionEnd+1, newPointArray, inSectionEnd+1, _numPoints - inSectionEnd - 1);
				}
			}
			else
			{
				// Move section to later point
				if (inSectionStart > 0) {
					System.arraycopy(_dataPoints, 0, newPointArray, 0, inSectionStart); // unchanged points before
				}
				// from end of section to move to point
				if (inMoveTo > (inSectionEnd + 1)) {
					System.arraycopy(_dataPoints, inSectionEnd+1, newPointArray, inSectionStart, inMoveTo - inSectionEnd - 1);
				}
				// moved bit
				System.arraycopy(_dataPoints, inSectionStart, newPointArray, inSectionStart + inMoveTo - inSectionEnd - 1,
					inSectionEnd - inSectionStart + 1);
				// unchanged bit after
				if (inSectionEnd < (_numPoints - 1)) {
					System.arraycopy(_dataPoints, inMoveTo, newPointArray, inMoveTo, _numPoints - inMoveTo);
				}
			}
			// Copy array references
			_dataPoints = newPointArray;
			_scaled = false;
			return true;
		}
		return false;
	}


	/**
	 * Average selected points
	 * @param inStartIndex start index of selection
	 * @param inEndIndex end index of selection
	 * @return true if successful
	 */
	public boolean average(int inStartIndex, int inEndIndex)
	{
		// check parameters
		if (inStartIndex < 0 || inStartIndex >= _numPoints || inEndIndex <= inStartIndex)
			return false;

		DataPoint startPoint = getPoint(inStartIndex);
		double firstLatitude = startPoint.getLatitude().getDouble();
		double firstLongitude = startPoint.getLongitude().getDouble();
		double latitudeDiff = 0.0, longitudeDiff = 0.0;
		double totalAltitude = 0;
		int numAltitudes = 0;
		Unit altUnit = null;
		// loop between start and end points
		for (int i=inStartIndex; i<= inEndIndex; i++)
		{
			DataPoint currPoint = getPoint(i);
			latitudeDiff += (currPoint.getLatitude().getDouble() - firstLatitude);
			longitudeDiff += (currPoint.getLongitude().getDouble() - firstLongitude);
			if (currPoint.hasAltitude())
			{
				totalAltitude += currPoint.getAltitude().getValue(altUnit);
				// Use altitude format of first valid altitude
				if (altUnit == null)
					altUnit = currPoint.getAltitude().getUnit();
				numAltitudes++;
			}
		}
		int numPoints = inEndIndex - inStartIndex + 1;
		double meanLatitude = firstLatitude + (latitudeDiff / numPoints);
		double meanLongitude = firstLongitude + (longitudeDiff / numPoints);
		Altitude meanAltitude = null;
		if (numAltitudes > 0) {
			meanAltitude = new Altitude((int) (totalAltitude / numAltitudes), altUnit);
		}

		DataPoint insertedPoint = new DataPoint(new Latitude(meanLatitude, Coordinate.FORMAT_DECIMAL_FORCE_POINT),
			new Longitude(meanLongitude, Coordinate.FORMAT_DECIMAL_FORCE_POINT), meanAltitude);
		// Make into singleton
		insertedPoint.setSegmentStart(true);
		DataPoint nextPoint = getNextTrackPoint(inEndIndex+1);
		if (nextPoint != null) {nextPoint.setSegmentStart(true);}
		// Insert points into track
		return insertRange(new DataPoint[] {insertedPoint}, inEndIndex + 1);
	}


	/**
	 * Append the specified points to the end of the track
	 * @param inPoints DataPoint objects to add
	 */
	public void appendPoints(DataPoint[] inPoints)
	{
		// Insert points into track
		if (inPoints != null && inPoints.length > 0)
		{
			insertRange(inPoints, _numPoints);
		}
		// needs to be scaled again to recalc x, y
		_scaled = false;
		UpdateMessageBroker.informSubscribers();
	}


	//////// information methods /////////////

	/**
	 * Checks if any data exists for the specified field
	 * @param inField Field to examine
	 * @return true if data exists for this field
	 */
	public boolean hasData(Field inField)
	{
		// Don't use this method for altitudes
		if (inField.equals(Field.ALTITUDE)) {return hasAltitudeData();}
		return hasData(inField, 0, _numPoints-1);
	}


	/**
	 * Checks if any data exists for the specified field in the specified range
	 * @param inField Field to examine
	 * @param inStart start of range to check
	 * @param inEnd end of range to check (inclusive)
	 * @return true if data exists for this field
	 */
	public boolean hasData(Field inField, int inStart, int inEnd)
	{
		// Loop over selected point range
		for (int i=inStart; i<=inEnd; i++)
		{
			if (_dataPoints[i].getFieldValue(inField) != null)
			{
				// Check altitudes and timestamps
				if ((inField != Field.ALTITUDE || _dataPoints[i].getAltitude().isValid())
					&& (inField != Field.TIMESTAMP || _dataPoints[i].getTimestamp().isValid()))
				{
					return true;
				}
			}
		}
		return false;
	}


	/**
	 * @return true if track contains any points marked for deletion
	 */
	public boolean hasMarkedPoints()
	{
		if (_numPoints < 1) {
			return false;
		}
		// Loop over points looking for any marked for deletion
		for (int i=0; i<=_numPoints-1; i++)
		{
			if (_dataPoints[i] != null && _dataPoints[i].getDeleteFlag()) {
				return true;
			}
		}
		// None found
		return false;
	}

	/**
	 * Clear all the deletion markers
	 */
	public void clearDeletionMarkers()
	{
		for (int i=0; i<_numPoints; i++)
		{
			_dataPoints[i].setMarkedForDeletion(false);
		}
	}

	/**
	 * Collect all the waypoints into the given List
	 * @param inList List to fill with waypoints
	 */
	public void getWaypoints(List<DataPoint> inList)
	{
		// clear list
		inList.clear();
		// loop over points and copy all waypoints into list
		for (int i=0; i<=_numPoints-1; i++)
		{
			if (_dataPoints[i] != null && _dataPoints[i].isWayPoint())
			{
				inList.add(_dataPoints[i]);
			}
		}
	}



	///////// Internal processing methods ////////////////

	/**
	 * Shift all the segment start flags in the given range by 1
	 * Method used by reverse range and its undo
	 * @param inStartIndex start of range, inclusive
	 * @param inEndIndex end of range, inclusive
	 */
	public void shiftSegmentStarts(int inStartIndex, int inEndIndex)
	{
		boolean prevFlag = true;
		boolean currFlag = true;
		for (int i=inStartIndex; i<= inEndIndex; i++)
		{
			DataPoint point = getPoint(i);
			if (point != null && !point.isWaypoint())
			{
				// remember flag
				currFlag = point.getSegmentStart();
				// shift flag by 1
				point.setSegmentStart(prevFlag);
				prevFlag = currFlag;
			}
		}
	}

	////////////////// Cloning and replacing ///////////////////

	/**
	 * Clone the array of DataPoints
	 * @return shallow copy of DataPoint objects
	 */
	public DataPoint[] cloneContents()
	{
		DataPoint[] clone = new DataPoint[getNumPoints()];
		System.arraycopy(_dataPoints, 0, clone, 0, getNumPoints());
		return clone;
	}


	/**
	 * Clone the specified range of data points
	 * @param inStart start index (inclusive)
	 * @param inEnd end index (inclusive)
	 * @return shallow copy of DataPoint objects
	 */
	public DataPoint[] cloneRange(int inStart, int inEnd)
	{
		int numSelected = 0;
		if (inEnd >= 0 && inEnd >= inStart)
		{
			numSelected = inEnd - inStart + 1;
		}
		DataPoint[] result = new DataPoint[numSelected>0?numSelected:0];
		if (numSelected > 0)
		{
			System.arraycopy(_dataPoints, inStart, result, 0, numSelected);
		}
		return result;
	}


	/**
	 * Re-insert the specified point at the given index
	 * @param inPoint point to insert
	 * @param inIndex index at which to insert the point
	 * @return true if it worked, false otherwise
	 */
	public boolean insertPoint(DataPoint inPoint, int inIndex)
	{
		if (inIndex > _numPoints || inPoint == null)
		{
			return false;
		}
		// Make new array to copy points over to
		DataPoint[] newPointArray = new DataPoint[_numPoints + 1];
		if (inIndex > 0)
		{
			System.arraycopy(_dataPoints, 0, newPointArray, 0, inIndex);
		}
		newPointArray[inIndex] = inPoint;
		if (inIndex < _numPoints)
		{
			System.arraycopy(_dataPoints, inIndex, newPointArray, inIndex+1, _numPoints - inIndex);
		}
		// Change over to new array
		_dataPoints = newPointArray;
		_numPoints++;
		// needs to be scaled again
		_scaled = false;
		UpdateMessageBroker.informSubscribers();
		return true;
	}


	/**
	 * Re-insert the specified point range at the given index
	 * @param inPoints point array to insert
	 * @param inIndex index at which to insert the points
	 * @return true if it worked, false otherwise
	 */
	public boolean insertRange(DataPoint[] inPoints, int inIndex)
	{
		if (inIndex > _numPoints || inPoints == null)
		{
			return false;
		}
		// Make new array to copy points over to
		DataPoint[] newPointArray = new DataPoint[_numPoints + inPoints.length];
		if (inIndex > 0)
		{
			System.arraycopy(_dataPoints, 0, newPointArray, 0, inIndex);
		}
		System.arraycopy(inPoints, 0, newPointArray, inIndex, inPoints.length);
		if (inIndex < _numPoints)
		{
			System.arraycopy(_dataPoints, inIndex, newPointArray, inIndex+inPoints.length, _numPoints - inIndex);
		}
		// Change over to new array
		_dataPoints = newPointArray;
		_numPoints += inPoints.length;
		// needs to be scaled again
		_scaled = false;
		UpdateMessageBroker.informSubscribers();
		return true;
	}


	/**
	 * Replace the track contents with the given point array
	 * @param inContents array of DataPoint objects
	 * @return true on success
	 */
	public boolean replaceContents(DataPoint[] inContents)
	{
		// master field array stays the same
		// (would need to store field array too if we wanted to redo a load)
		// replace data array
		_dataPoints = inContents;
		_numPoints = _dataPoints.length;
		_scaled = false;
		UpdateMessageBroker.informSubscribers();
		return true;
	}


	/**
	 * Edit the specified point
	 * @param inPoint point to edit
	 * @param inEditList list of edits to make
	 * @param inUndo true if undo operation, false otherwise
	 * @return true if successful
	 */
	public boolean editPoint(DataPoint inPoint, FieldEditList inEditList, boolean inUndo)
	{
		if (inPoint != null && inEditList != null && inEditList.getNumEdits() > 0)
		{
			// remember if coordinates have changed
			boolean coordsChanged = false;
			// go through edits one by one
			int numEdits = inEditList.getNumEdits();
			for (int i=0; i<numEdits; i++)
			{
				FieldEdit edit = inEditList.getEdit(i);
				Field editField = edit.getField();
				inPoint.setFieldValue(editField, edit.getValue(), inUndo);
				// Check that master field list has this field already (maybe point name has been added)
				if (!_masterFieldList.contains(editField)) {
					_masterFieldList.extendList(editField);
				}
				// check coordinates
				coordsChanged |= (editField.equals(Field.LATITUDE)
					|| editField.equals(Field.LONGITUDE) || editField.equals(Field.ALTITUDE));
			}
			// set photo status if coordinates have changed
			if (inPoint.getPhoto() != null && coordsChanged)
			{
				inPoint.getPhoto().setCurrentStatus(Photo.Status.CONNECTED);
			}
			// point possibly needs to be scaled again
			_scaled = false;
			// trigger listeners
			UpdateMessageBroker.informSubscribers();
			return true;
		}
		return false;
	}

	/**
	 * @param inPoint point to check
	 * @return true if this track contains the given point
	 */
	public boolean containsPoint(DataPoint inPoint)
	{		
		return (findPoint(inPoint) >= 0);
	}

	/**
	 * @param inPoint point to check
	 * @return the index of the point within this track or -1 
	 */
	public int findPoint(DataPoint inPoint)
	{
		if (inPoint == null) return -1;
		for (int i=0; i < getNumPoints(); i++)
		{
			if (getPoint(i) == inPoint) return i;
		}
		return -1; // not found
	}

	/**
	 * Interpolate extra points between two selected ones
	 * @param inStartIndex start index of interpolation
	 * @param inNumPoints num points to insert
	 * @return true if successful
	 */
	public boolean interpolate(int inStartIndex, int inNumPoints)
	{
		// check parameters
		if (inStartIndex < 0 || inStartIndex >= _numPoints || inNumPoints <= 0)
			return false;

		// get start and end points
		DataPoint startPoint = getPoint(inStartIndex);
		DataPoint endPoint = getPoint(inStartIndex + 1);

		// Make array of points to insert
		DataPoint[] insertedPoints = startPoint.interpolate(endPoint, inNumPoints);

		// Insert points into track
		return insertRange(insertedPoints, inStartIndex + 1);
	}

	
	/**
	 * Collect all the named trackpoints into the given List
	 * @param inList List to fill with waypoints
	 * @author Walter Biselli
	 * @since WB
    */
	public void getNamedTrackpoints(List<DataPoint> inList)
	{
		// clear list
		inList.clear();
		// loop over points and copy all named trackpoints into list
		for (int i=0; i<=_numPoints-1; i++)
		{
			if (_dataPoints[i] != null && _dataPoints[i].isRoutePoint())
			{
				inList.add(_dataPoints[i]);
			}
		}
	}
	
}
