/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.dashboard;

import com.l7tech.console.panels.MetricsChartPanel;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author alex
 */
class Range {
    private final int resolution;
    private final long chartRange;
    private final long rightPanelRange;
    private final String name;
    private final String rightPanelTitle;

    private final MetricsChartPanel metricsChartPanel;
    private final Set chartedPeriods = new HashSet();
    private final TreeMap allPeriods = new TreeMap();

    private volatile long lastPeriodDownloaded;

    Range(int resolution, long chartRange, long rightPanelRange, String name, String rightPanelTitle) {
        this.resolution = resolution;
        this.chartRange = chartRange;
        this.rightPanelRange = rightPanelRange;
        this.name = name;
        this.metricsChartPanel = new MetricsChartPanel(resolution, rightPanelRange, chartRange);
        this.lastPeriodDownloaded = System.currentTimeMillis() - chartRange;
        this.rightPanelTitle = rightPanelTitle;
        clear();
    }

    synchronized void clear() {
        chartedPeriods.clear();
        metricsChartPanel.clearData();
    }

    public String toString() {
        return name;
    }

    int getResolution() {
        return resolution;
    }

    long getChartRange() {
        return chartRange;
    }

    long getRightPanelRange() {
        return rightPanelRange;
    }

    String getName() {
        return name;
    }

    synchronized MetricsChartPanel getMetricsChartPanel() {
        return metricsChartPanel;
    }

    synchronized Set getChartedPeriods() {
        return chartedPeriods;
    }

    TreeMap getAllPeriods() {
        return allPeriods;
    }

    String getRightPanelTitle() {
        return rightPanelTitle;
    }

    long getLastPeriodDownloaded() {
        return lastPeriodDownloaded;
    }

    void setLastPeriodDownloaded(long lastPeriodDownloaded) {
        this.lastPeriodDownloaded = lastPeriodDownloaded;
    }
}
