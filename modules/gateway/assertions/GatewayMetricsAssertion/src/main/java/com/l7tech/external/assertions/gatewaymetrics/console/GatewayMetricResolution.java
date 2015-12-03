/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.gatewaymetrics.console;

import com.l7tech.gateway.common.service.MetricsBin;

import java.text.MessageFormat;

/**
 * Encapsulates info relevant to a particular metrics bin resolution.
 * Can be used as a {@link javax.swing.ComboBoxModel} item.
 *
 * @author rmak
 */
/* package */ public class GatewayMetricResolution {

    /** Metrics bin resolution ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE}, {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY}). */
    private final int _resolution;

    /** Metrics bin nominal time period interval in milliseconds. */
    private int _binInterval;

    /** Text string to display when this is used as a ComboxBox item. */
    private String _comboItemText;

    /**
     * @param resolution        metrics bin resolution ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE}, {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY})
     * @param binInterval       metrics bin nominal time period interval in milliseconds
     */
    public GatewayMetricResolution(final int resolution,
                                   final int binInterval) {
        _resolution = resolution;
        _binInterval = binInterval;
        /* Maximum time range to display in chart in milliseconds. */

        switch (resolution) {
            case MetricsBin.RES_FINE: {
                setBinInterval(binInterval);
                break;
            }
            case MetricsBin.RES_HOURLY: {
                _comboItemText = "Hourly";
                break;
            }
            case MetricsBin.RES_DAILY: {
                _comboItemText = "Daily";
                break;
            }
        }
    }


    /** @return metrics bin resolution ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE}, {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY}) */
    public int getResolution() {
        return _resolution;
    }

    /**
     * @param binInterval   metrics bin nominal time period interval in milliseconds
     */
    public void setBinInterval(final int binInterval) {
        _binInterval = binInterval;
        _comboItemText = MessageFormat.format("Fine ({0,number} seconds)", _binInterval / 1000.);
    }

    public String toString() {
        return _comboItemText;
    }
}
