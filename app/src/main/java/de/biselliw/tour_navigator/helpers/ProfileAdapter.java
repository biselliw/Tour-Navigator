package de.biselliw.tour_navigator.helpers;

/*
 * Drawer for the profile
 *
 * This file is based on
 * https://github.com/halfhp/androidplot/blob/master/demoapp/src/main/java/com/androidplot/demos/DynamicXYPlotActivity.java
 *
 * Copyright 2015 AndroidPlot.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */
import android.graphics.Color;
import android.graphics.Paint;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.RectRegion;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYRegionFormatter;
import com.androidplot.xy.XYSeries;

import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.data.TrackSegments;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

/**
 *  @link <a href="https://github.com/halfhp/androidplot/blob/master/demoapp/src/main/java/com/androidplot/demos/DynamicXYPlotActivity.java">DynamicXYPlotActivity.java on github.com</a>
 */
public class ProfileAdapter {
    int numPoints = 0;
    Number lastDistance = 0;
    double prevDistance;
    int prevIndex;
    double lastAltitude = 0.0;
    TrackDetails _trackDetails = null;

    private static final boolean SHOW_SERIES_SEGMENTS = true;
    private XYPlot dynamicPlot;
    DynamicXYDatasource data;
    DynamicXYDatasource bar;

    int _cursorX;
    double _distanceX;

    private RectRegion profileRegion;
    private XYRegionFormatter profileRegionFormatter;

    LineAndPointFormatter lineFormatter, segmentFormatter;

    public ProfileAdapter(MainActivity inActivity) {
        createPlot(inActivity);
    }

    /**
     * redraws a plot whenever an update is received:
     * @apiNote todo java.util.Observer' is deprecated as of API 33 ("Tiramisu"; Android 13.0)
      */
    private static class MyPlotUpdater implements Observer {
        // todo check Raw use of parameterized class 'Plot'
        Plot plot;

        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }

        @Override
        public void update(Observable o, Object arg) {
            plot.redraw();
        }
    }

    final static int SERIES_ALTITUDES = 0, SERIES_CURSOR = 1, SERIES_SEGMENTS = 2;
    public void createPlot(MainActivity inActivity) {
        /*
         * @todo Material3 conform:
         * val surface = MaterialColors.getColor(plot, R.attr.plotSurfaceColor)
         * val grid = MaterialColors.getColor(plot, R.attr.plotGridLineColor)
         * val text = MaterialColors.getColor(plot, R.attr.plotTextColor)
         *
         * plot.graph.backgroundPaint.color = surface
         * plot.graph.gridBackgroundPaint.color = surface
         * plot.graph.domainGridLinePaint.color = grid
         * plot.graph.rangeGridLinePaint.color = grid
         * plot.title.labelPaint.color = text
         */

        // get handles to our View defined in layout.xml:
        dynamicPlot = inActivity.findViewById(R.id.plot);
        if (dynamicPlot == null) return;
/*
            int surface = MaterialColors.getColor(dynamicPlot, R.attr.plotSurfaceColor);
            // todo Unable to start activity ComponentInfo{de.biselliw.tour_navigator/de.biselliw.tour_navigator.activities.MainActivity}:
                java.lang.IllegalArgumentException: com.androidplot.xy.XYPlot requires a value for the de.biselliw.tour_navigator:attr/plotSurfaceColor attribute
                to be set in your app theme. You can either set the attribute in your theme or update your theme to inherit from Theme.MaterialComponents (or a descendant).
            int grid = MaterialColors.getColor(dynamicPlot, R.attr.plotGridLineColor);
            int text = MaterialColors.getColor(dynamicPlot, R.attr.plotTextColor);

            dynamicPlot.getGraph().getBackgroundPaint().setColor(surface);
            dynamicPlot.getGraph().getGridBackgroundPaint().setColor(surface);
            dynamicPlot.getGraph().getDomainGridLinePaint().setColor(grid);
            dynamicPlot.getGraph().getRangeGridLinePaint().setColor(grid);
//            dynamicPlot.getGraph().labelPaint.color = text;

 */
        MyPlotUpdater plotUpdater = new MyPlotUpdater(dynamicPlot);

        // only display whole numbers in domain labels
        dynamicPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new DecimalFormat("0.0"));

        // getInstance and position datasets:
        data = new DynamicXYDatasource();
        bar = new DynamicXYDatasource();

        /* SERIES_ALTITUDES */
        AltitudeSeries altitudeSeries = new AltitudeSeries(data,    SERIES_ALTITUDES, "");
        lineFormatter = new LineAndPointFormatter(
                Color.rgb(0, 200, 0), null, null, null);
        lineFormatter.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        lineFormatter.getLinePaint().setStrokeWidth(10);
        dynamicPlot.addSeries(altitudeSeries, lineFormatter);

        /* SERIES_CURSOR */
        AltitudeSeries barSeries      = new AltitudeSeries(data,    SERIES_CURSOR, "");
        LineAndPointFormatter cursorFormatter = new LineAndPointFormatter(
                Color.rgb(200, 0, 0), null, null, null);
        cursorFormatter.setLegendIconEnabled(false);
        dynamicPlot.addSeries(barSeries, cursorFormatter);
        dynamicPlot.getLegend().setVisible(false);

        if (SHOW_SERIES_SEGMENTS) {
            AltitudeSeries segmentSeries = new AltitudeSeries(data, SERIES_SEGMENTS, "");
            segmentFormatter =
                    new LineAndPointFormatter(
                            Color.rgb(0, 0, 200),   // line color
                            Color.rgb(0, 0, 200),   // vertex (point) color
                            null,
                            null);

            /* --- Line --- */
            Paint linePaint = segmentFormatter.getLinePaint();
            linePaint.setStrokeWidth(6f);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            linePaint.setAntiAlias(true);

            dynamicPlot.addSeries(segmentSeries, this.segmentFormatter);
        }

        // hook up the plotUpdater to the data model:
        data.addObserver(plotUpdater);

        // thin out domain tick labels so they dont overlap each other:
        dynamicPlot.setDomainStepMode(StepMode.INCREMENT_BY_FIT); //  INCREMENT_BY_VAL);
        dynamicPlot.setDomainStepValue(2.5);

        dynamicPlot.setRangeStepMode(StepMode.INCREMENT_BY_FIT);
        dynamicPlot.setRangeStepValue(200);

        dynamicPlot.getGraph().getLineLabelStyle(
                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));
    }


    /**
     * @implNote Observable class was deprecated in API level 33.
     */
    class DynamicXYDatasource implements Runnable {

        // encapsulates management of the observers watching this datasource for update events:
        class ProfileObservable extends Observable {
            @Override
            public void notifyObservers() {
                setChanged();
                super.notifyObservers();
            }
        }

        private ProfileObservable notifier = new ProfileObservable();
        private boolean keepRunning = false;

        void stopThread() {
            keepRunning = false;
        }

        //@Override
        public void run() {
            try {
                keepRunning = true;
                while (keepRunning) {
                    Thread.sleep(10); // FIXME Call to 'Thread.sleep()' in a loop, probably busy-waiting: decrease or remove to speed up the refresh rate.
                    notifier.notifyObservers();
                }
            } catch (InterruptedException ignored) {
            }
        }

        int getItemCount(int series) {
            switch (series) {
                case SERIES_ALTITUDES:
                    if (numPoints == 0)
                        initPlot();
                    return numPoints;
                case SERIES_CURSOR:
                    return numPoints;
                case SERIES_SEGMENTS:
                    if (numPoints == 0)
                        return 0;
                    else
                        if (_trackDetails != null)
                            return _trackDetails.getSegmentsCount() + 1;
            }
            return 0;
        }

        Number getX(int series, int index) {
            switch (series) {
                case SERIES_ALTITUDES:
                case SERIES_CURSOR:
                {
                    if (index >= numPoints) {
                        throw new IllegalArgumentException();
                    }
                    if (index == 0) {
                        lastDistance = 0.0;
                        prevDistance = 0.0;
                        prevIndex = 0;
                    }

                    if (_trackDetails != null) {
                        DataPoint currPoint = _trackDetails.getPoint(index);
                        if ((currPoint != null) && !currPoint.isWayPoint()) {
                            double dist = currPoint.getDistance();
                            Number distance = dist;
                            if (dist > 0) {
                                lastDistance = distance;
                                if (_cursorX < 0 && _distanceX <= dist)
                                    _cursorX = index;
                                if (dist > prevDistance)
                                    prevDistance = dist;
                            }
                            if (index > 0)
                                if (index == prevIndex + 1)
                                    prevIndex++;
                        }
                    }
                    return lastDistance;
                }
                case SERIES_SEGMENTS:
                    if (numPoints == 0)
                        return 0;
                    else if (_trackDetails != null) {
                        double distance;
                        if (index < _trackDetails.getSegmentsCount())
                            distance = _trackDetails.getSegmentStartDistance(index);
                        else
                            distance = _trackDetails.getSegmentEndDistance(index-1);
                        return distance;
                    }
                    break;
            }
            return 0;
        }

        Number getY(int series, int index) {
            int altitude = 0;
            switch (series) {
                case SERIES_ALTITUDES:
                case SERIES_CURSOR: {
                    if (index == 0)
                        lastAltitude = -999.99;
                    else if (index >= numPoints)
                        throw new IllegalArgumentException();

                    if (_trackDetails != null) {
                        for (int i = index; i <= (lastAltitude < 0.0 ? numPoints - 1 : index); i++) {
                            DataPoint currPoint = _trackDetails.getPoint(i);
                            if ((currPoint != null) && !currPoint.isWayPoint() && currPoint.hasAltitude()) {
                                altitude = (int) currPoint.getAltitude().getValue();
                                if (altitude > 0) {
                                    lastAltitude = altitude;
                                    break;
                                }
                            }
                        }
                    }
                    break;
                }
                case SERIES_SEGMENTS:
                    break;
            }
            switch (series) {
                case SERIES_ALTITUDES:
                    return lastAltitude;
                case SERIES_CURSOR: {
                    // draw cursor
                    if (index == _cursorX) {
                        // show current altitude
                        String title = String.valueOf(altitude);
                        dynamicPlot.setTitle(title + " m");
                        return TrackSegments.getMaxAltitude();
                    }
                    break;
                }
                case SERIES_SEGMENTS: {
                    if (numPoints == 0)
                        return 0;
                    else if (_trackDetails != null) {
                        double elevation;
                        if (index < _trackDetails.getSegmentsCount())
                            elevation = _trackDetails.getSegmentStartElevation(index);
                        else
                            elevation = _trackDetails.getSegmentEndElevation(index - 1);
                        return elevation;
                    }
                    break;
                }
            }
            return 0;
        }

        void addObserver(Observer observer) {
            notifier.addObserver(observer);
        }

        public void removeObserver(Observer observer) {
            notifier.deleteObserver(observer);
        }
    }

    class AltitudeSeries implements XYSeries {
        private DynamicXYDatasource datasource;
        private int seriesIndex;
        private String title;

        AltitudeSeries(DynamicXYDatasource datasource, int seriesIndex, String title) {
            this.datasource = datasource;
            this.seriesIndex = seriesIndex;
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int size() {
            return datasource.getItemCount(seriesIndex);
        }

        @Override
        public Number getX(int index) {
            return datasource.getX(seriesIndex, index);
        }

        @Override
        public Number getY(int index) {
            return datasource.getY(seriesIndex, index);
        }
    }

    /**
     * Initialize the chart:
     */
    public void initPlot(TrackDetails inTrack) {
        _trackDetails = inTrack;
        initPlot();
    }
    private void initPlot() {
        numPoints = 0;
        clearXRange();
        if (_trackDetails != null) {
            numPoints = _trackDetails.getNumPoints();
            // set the horizontal grid steps
            if (numPoints > 0) {
                double totalDistance = TrackSegments.summary.totalDistance_km;
                if (totalDistance > 0.0) {
                    double domainStepValue = 2.5;
                    int domainSteps = (int) (totalDistance / domainStepValue);
                    while (domainSteps > 8) {
                        domainStepValue = domainStepValue * 2.0;
                        domainSteps = domainSteps / 2;
                    }
                    if (domainSteps == 1)
                    {
                        domainStepValue = domainStepValue / 2.5;
                    }
                    dynamicPlot.setDomainStepValue(domainStepValue);
                    dynamicPlot.setDomainBoundaries(0.0, totalDistance, BoundaryMode.FIXED);

                    // set the vertical grid steps
                    double rangeStepValue = 25.0;
                    int minAltitude = (int) TrackSegments.getMinAltitude();
                    int maxAltitude = (int) TrackSegments.getMaxAltitude();

                    int rangeAltitude = maxAltitude - minAltitude;
                    int rangeSteps = rangeAltitude / (int)rangeStepValue;
                    while (rangeSteps > 5) {
                        rangeStepValue = rangeStepValue * 2.0;
                        rangeSteps = rangeSteps / 2;
                    }
                    if (rangeSteps == 1)
                    {
                        rangeStepValue = rangeStepValue / 2.5;
                    }
                    minAltitude = (minAltitude / (int)rangeStepValue) * (int)rangeStepValue;
                    maxAltitude = ((maxAltitude + (int)rangeStepValue) / (int)rangeStepValue) * (int)rangeStepValue;
                    dynamicPlot.setRangeStepValue(rangeStepValue);
                    dynamicPlot.setRangeBoundaries(minAltitude, maxAltitude, BoundaryMode.FIXED);
                    dynamicPlot.redraw();
                }
            }
        }
    }

    /**
     * Clear the previous fill range between two places
     */
    public void clearXRange()
    {
        if (profileRegion != null)
            lineFormatter.removeRegion(profileRegion);
        profileRegion = null;
        if (dynamicPlot != null)
            dynamicPlot.redraw();
    }

    /**
     * Set the fill range between two places
     * @param minX start distance [km]
     * @param maxX   end distance [km]
     */
    public void setXRange(Number minX, Number maxX)
    {
        if (profileRegion != null)
            lineFormatter.removeRegion(profileRegion);
        profileRegion = new RectRegion(minX, maxX, 0.0, TrackSegments.getMaxAltitude(), "Short");
        if (profileRegionFormatter == null)
            profileRegionFormatter = new XYRegionFormatter(Color.YELLOW);

        lineFormatter.addRegion(profileRegion, profileRegionFormatter);
        if (dynamicPlot != null)
            dynamicPlot.redraw();
    }

    /**
     * Show the current distance in the chart
     * @param inIndex index of the track point
     */
    public void setCursor (int inIndex) {
        _cursorX = inIndex;
        if (dynamicPlot != null)
            dynamicPlot.redraw();
    }

    /**
     * Show the current distance in the chart
     * @param inDistance distance of the track point [km]
     */
    public void setCursor (double inDistance) {
        _cursorX = -1;
        _distanceX = inDistance;
        if (dynamicPlot != null)
            dynamicPlot.redraw();
    }
}