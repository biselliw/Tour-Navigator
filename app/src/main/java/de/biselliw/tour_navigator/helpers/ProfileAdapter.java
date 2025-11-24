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
 */

/*
 * Copyright (C) 2022 Walter Biselli
 *
 *  This file is based on Privacy Friendly App Example:
 *
 *      https://github.com/SecUSo/privacy-friendly-app-example
 *
 * Hiking Navigator App (the "Software") is free software:
 *
 * you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or any later version.
 *
 * The Software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * Licensed under the GNU General Public License along with this (the "License");
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License along with Hiking Navigator App.
 * If not, you may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.tim_prune.data.Track;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.TrackInfo;

public class ProfileAdapter {

    private MainActivity _main;
    private App _app;

    int numPoints = 0;
    double lastDistance = 0.0;
    double lastAltitude = 0;
    Track _track = null;

    private XYPlot dynamicPlot;
    DynamicXYDatasource data;
    DynamicXYDatasource bar;

    int _plotX;

    private RectRegion profileRegion;
    private XYRegionFormatter profileRegionFormatter;

    LineAndPointFormatter lineFormatter;

    public ProfileAdapter(MainActivity main, App app) {
        this._main = main;
        this._app = app;

        createPlot();

    }

    // redraws a plot whenever an update is received:
    private static class MyPlotUpdater implements Observer {
        Plot plot;

        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }

        @Override
        public void update(Observable o, Object arg) {
            plot.redraw();
        }
    }

    public void createPlot() {
        // get handles to our View defined in layout.xml:
        dynamicPlot = (XYPlot) _main.findViewById(R.id.plot);

        MyPlotUpdater plotUpdater = new MyPlotUpdater(dynamicPlot);

        // only display whole numbers in domain labels
        dynamicPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new DecimalFormat("0.0"));

        // getInstance and position datasets:
        data = new DynamicXYDatasource();
        bar = new DynamicXYDatasource();

        AltitudeSeries altitudeSeries = new AltitudeSeries(data, 0, "");
        AltitudeSeries barSeries = new AltitudeSeries(data, 1, "");

        lineFormatter = new LineAndPointFormatter(
                Color.rgb(0, 200, 0), null, null, null);
        lineFormatter.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        lineFormatter.getLinePaint().setStrokeWidth(10);
        dynamicPlot.addSeries(altitudeSeries, lineFormatter);

        LineAndPointFormatter cursorFormatter = new LineAndPointFormatter(
                Color.rgb(200, 0, 0), null, null, null);
        cursorFormatter.setLegendIconEnabled(false);
        dynamicPlot.addSeries(barSeries, cursorFormatter);
        dynamicPlot.getLegend().setVisible(false);

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

    public void Pause() {
        data.stopThread();
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
                    Thread.sleep(10); // decrease or remove to speed up the refresh rate.
                    notifier.notifyObservers();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int getItemCount(int series) {
            if (numPoints == 0)
                initPlot();
            return numPoints;
        }

        Number getX(int series, int index) {
            if (index >= numPoints) {
                throw new IllegalArgumentException();
            }
            if (index == 0)
                lastDistance = 0.0;

            if (_track != null) {
                DataPoint currPoint = _track.getPoint(index);
                if ((currPoint != null) && !currPoint.isWayPoint()) {
                    double distance = currPoint.getDistance();
                    if (distance > 0.0)
                        lastDistance = distance;
                }
            }

            return lastDistance;
        }

        Number getY(int series, int index) {
            if (index >= numPoints) {
                throw new IllegalArgumentException();
            }

            double altitude = 0;
            if (_track != null) {
                DataPoint currPoint = _track.getPoint(index);
                if ((currPoint != null) && !currPoint.isWayPoint() && currPoint.hasAltitude()) {
                    altitude = currPoint.getAltitude().getValue();
                    if (altitude > 0)
                        lastAltitude = altitude;
                }
            }

            if (series == 0) {
                // draw line
                return lastAltitude;
            } else {
                // draw cursor
                if (index == _plotX) {
                    // show current altitude
                    dynamicPlot.setTitle(String.valueOf(altitude) + " m");
                    return _app.getMaxAltitude();
                }
                else return 0;
            }
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
    public void initPlot() {
        numPoints = 0;
        TrackInfo trackInfo = _app.getTrackInfo();
        if (trackInfo != null) {
            _track = trackInfo.getTrack();
            if (_track != null) {
                numPoints = _track.getNumPoints();
                // set the horizontal grid steps
                if (numPoints > 0) {
                    double totalDistance = _app.getTotalDistance();
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
                        double minAltitude = _app.getMinAltitude();
                        double rangeAltitude = _app.getMaxAltitude() - _app.getMinAltitude();
                        int rangeSteps = (int) (rangeAltitude / rangeStepValue);
                        while (rangeSteps > 5) {
                            rangeStepValue = rangeStepValue * 2.0;
                            rangeSteps = rangeSteps / 2;
                        }
                        if (rangeSteps == 1)
                        {
                            rangeStepValue = rangeStepValue / 2.5;
                        }
                        minAltitude = (minAltitude / (int)rangeStepValue) * (int)rangeStepValue;
                        dynamicPlot.setRangeStepValue(rangeStepValue);
                        dynamicPlot.setRangeBoundaries(minAltitude, _app.getMaxAltitude(), BoundaryMode.FIXED);
                        dynamicPlot.redraw();
                    }
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
    public void setXRange(double minX, double maxX)
    {
        if (profileRegion != null)
            lineFormatter.removeRegion(profileRegion);
        profileRegion = new RectRegion(minX, maxX, 0.0, _app.getMaxAltitude(), "Short");
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
        _plotX = inIndex;
        if (dynamicPlot != null)
            dynamicPlot.redraw();
    }

}