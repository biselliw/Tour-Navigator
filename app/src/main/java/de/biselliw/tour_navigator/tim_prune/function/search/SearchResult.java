package de.biselliw.tour_navigator.tim_prune.function.search;

import androidx.annotation.NonNull;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import tim.prune.data.Distance;
import tim.prune.data.Latitude;
import tim.prune.data.Longitude;

/**
 * Class to hold a search result from wikipedia or other online service
 */
public class SearchResult implements Comparable<SearchResult> {
    /**
     * ID of the node / way
     */
    private String ID = null;
    /**
     * Data Point
     */
    private DataPoint _dataPoint = null;
    /**
     * Track name or title
     */
    private String _trackName = null;
    /**
     * Point type (for POIs)
     */
    private String _pointType = null;
    /**
     * Description
     */
    private String _description = null;
    /**
     * Web page for more details
     */
    private String _webUrl = null;
    /**
     * Distance between found position and track [km]
     */
    private double _trackDistance = 0.0;
    /**
     * Download link
     */
    private String _downloadLink = null;
    /**
     * Coordinates of point
     */
    private String _latitude = null, _longitude = null;

    /**
     * reference if any
     */
    private String _ref = null;

    // todo public boolean isGuidePost;

    public SearchResult() {

    }

    public void setID(String inID) {
        this.ID = inID;
    }

    public String getID() {
        return ID;
    }

    public void setDataPoint(DataPoint inDataPoint) {
        _dataPoint = inDataPoint;
    }

    public DataPoint getDataPoint() {
        return _dataPoint;
    }

    /**
     * @param inName name of track
     */
    public void setTrackName(String inName) {
        _trackName = inName;
    }

    /**
     * @return track name
     */
    @NonNull
    public String getTrackName() {
        return _trackName == null ? "" : _trackName;
    }

    /**
     * @param inType type of point (for POIs)
     */
    public void setPointType(String inType) {
        _pointType = inType;
    }

    /**
     * @return type of point (for POIs)
     */
    @NonNull
    public String getPointType() {
        return _pointType == null ? "" : _pointType;
    }

    /**
     * @param inDesc description
     */
    public void setDescription(String inDesc) {
        _description = inDesc;
    }

    /**
     * @return track description
     */
    @NonNull
    public String getDescription() {
        return _description == null ? "" : _description;
    }

    /**
     * @param inUrl web page url
     */
    public void setWebUrl(String inUrl) {
        _webUrl = inUrl;
    }

    /**
     * @return web url
     */
    @NonNull
    public String getWebUrl() {
        return _webUrl == null ? "" : _webUrl;
    }

    /**
     * @param inDistance distance between found position and track
     */
    public void setDistance(double inDistance) {
        _trackDistance = inDistance;
    }

    /**
     * @return distance between found position and track
     */
    public double getDistance() {
        return _trackDistance;
    }

    /**
     * @param inLink link to download track
     */
    public void setDownloadLink(String inLink) {
        _downloadLink = inLink;
    }

    /**
     * @return download link
     */
    @NonNull
    public String getDownloadLink() {
        return _downloadLink == null ? "" : _downloadLink;
    }

    /**
     * @param inLatitude latitude
     */
    public void setLatitude(String inLatitude) {
        _latitude = inLatitude;
    }

    /**
     * @return latitude
     */
    public String getLatitude() {
        return _latitude;
    }

    /**
     * @param inLongitude longitude
     */
    public void setLongitude(String inLongitude) {
        _longitude = inLongitude;
    }

    /**
     * @return longitude
     */
    public String getLongitude() {
        return _longitude;
    }

    /**
     * @return true if the search result contains coordinates
     */
    public boolean hasCoordinates() {
        return _latitude != null && _longitude != null;
    }


    public void setRef(String inRef) {
        this._ref = inRef;
    }

    public String getRef() {
        return _ref;
    }


    /**
     * Compare two search results for sorting (nearest first, then alphabetic)
     */
    public int compareTo(SearchResult inOther) {
        double distDiff = getDistance() - inOther.getDistance();
        if (distDiff < 0.0) {
            return -1;
        }
        if (distDiff > 0.0) {
            return 1;
        }
        return getTrackName().compareTo(inOther.getTrackName());
    }

    /**
     * Update single search result
     */
    public void update() {
        if (_dataPoint == null)
            try {
                if (_latitude != null && _longitude != null) {
                    _dataPoint = new DataPoint(Latitude.make(_latitude), Longitude.make(_longitude));
                    if (getTrackName().isEmpty()) {
// todo use replacement for empty name                        _dataPoint.setWaypointName(_pointType);
                    } else
                        _dataPoint.setWaypointName(getTrackName());
                    _dataPoint.setFieldValue(Field.DESCRIPTION, getDescription(), false);
                    _dataPoint.setFieldValue(Field.WAYPT_LINK, getWebUrl(), false);
                    _dataPoint.makeProtectedWaypoint();
                }
            } catch (Exception e) {
                _dataPoint = null;
            }

        if (_dataPoint != null)
            _dataPoint.setFieldValue(Field.WAYPT_TYPE, _pointType, false);

    }

}
