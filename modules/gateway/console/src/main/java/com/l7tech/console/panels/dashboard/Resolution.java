/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.dashboard;

import com.l7tech.gateway.common.service.MetricsBin;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Encapsulates info relevant to a particular metrics bin resolution.
 * Can be used as a {@link javax.swing.ComboBoxModel} item.
 *
 * @author rmak
 */
/* package */ class Resolution {
    private static final ResourceBundle _resources = ResourceBundle.getBundle("com.l7tech.console.panels.dashboard.resources.ServiceMetricsPanel");

    /** Metrics bin resolution ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE}, {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY}). */
    private final int _resolution;

    /** Metrics bin nominal time period interval in milliseconds. */
    private int _binInterval;

    /** Maximum time range to display in chart in milliseconds. */
    private final long _chartTimeRange;

    /** Text string to display when this is used as a ComboxBox item. */
    private String _comboItemText;

    /** Title to display in Latest Summary tab. */
    private String _latestTabTitle;

    /**
     * @param resolution        metrics bin resolution ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE}, {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY})
     * @param binInterval       metrics bin nominal time period interval in milliseconds
     * @param chartTimeRange    maximum time range to display in chart in milliseconds
     */
    public Resolution(final int resolution,
                      final int binInterval,
                      final long chartTimeRange) {
        _resolution = resolution;
        _binInterval = binInterval;
        _chartTimeRange = chartTimeRange;

        switch (resolution) {
            case MetricsBin.RES_FINE: {
                setBinInterval(binInterval);
                break;
            }
            case MetricsBin.RES_HOURLY: {
                _comboItemText = _resources.getString("resolutionCombo.hourlyValue");
                _latestTabTitle = _resources.getString("latestTab.title.hourly");
                break;
            }
            case MetricsBin.RES_DAILY: {
                _comboItemText = _resources.getString("resolutionCombo.dailyValue");
                _latestTabTitle = _resources.getString("latestTab.title.daily");
                break;
            }
        }
    }


    /** @return metrics bin resolution ({@link MetricsBin#RES_FINE}, {@link MetricsBin#RES_HOURLY} or {@link MetricsBin#RES_DAILY}) */
    public int getResolution() {
        return _resolution;
    }

    /**
     * @return metrics bin nominal time period interval in milliseconds
     */
    public long getBinInterval() {
        return _binInterval;
    }

    /**
     * @param binInterval   metrics bin nominal time period interval in milliseconds
     */
    public void setBinInterval(final int binInterval) {
        _binInterval = binInterval;
        _comboItemText = MessageFormat.format(_resources.getString("resolutionCombo.fineValue"), _binInterval / 1000.);
        _latestTabTitle = MessageFormat.format(_resources.getString("latestTab.title.fine"), _binInterval / 1000.);
    }

    /**
     * @return maximum time range to display in chart in milliseconds
     */
    public long getChartTimeRange() {
        return _chartTimeRange;
    }

    public String getLatestTabTitle() {
        return _latestTabTitle;
    }

    public String toString() {
        return _comboItemText;
    }
}
