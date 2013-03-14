/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.console.GatewayAuditWindow;
import com.l7tech.console.panels.dashboard.ServiceMetricsPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.jfree.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Either;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.InvalidObjectException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 * Chart panel containing plots of metrics bins data. The chart contains 3 plots
 * stacked vertically sharing the same time (x-)axis.
 * <p>
 * Top plot shows frontend and backend response times; with avg, min and max.
 * Middle plot shows alert indicators of routing failure and policy violation
 * for higher visibility.
 * Bottom plot shows message rates divided into success, policy violation and
 * routing failure.
 *
 * @author rmak
 */
public class MetricsChartPanel extends ChartPanel {
    private static final Logger _logger = Logger.getLogger(MetricsChartPanel.class.getName());

    private static final ResourceBundle _resources = ResourceBundle.getBundle("com.l7tech.console.resources.MetricsChartPanel");

    /** Color for the horizontal line representing average frontend response times. */
    private static final Color FRONTEND_RESPONSE_AVG_COLOR = new Color(95, 83, 173);

    /** Color for the high-low bars representing min-max frontend response times.
        Transparency is used to allow backend response times to be overlaid on
        frontend response times while keeping both remain visible. */
    private static final Color FRONTEND_RESPONSE_MINMAX_COLOR = GetAlphaEquiv(186, 189, 225, 192);

    /** Color for the horizontal line representing average backend response times. */
    private static final Color BACKEND_RESPONSE_AVG_COLOR = new Color(77, 86, 0);

    /** Color for the high-low bars representing min-max backend response times. */
    private static final Color BACKEND_RESPONSE_MINMAX_COLOR = GetAlphaEquiv(223, 235, 91, 192);

    /** Stroke for the horizontal line representing average response times. */
    private static final BasicStroke RESPONSE_AVG_STROKE = new BasicStroke(2.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    /** Color for success messages stack bars and alert indicators. */
    private static final Color SUCCESS_COLOR = new Color(0, 120, 0);

    /** Color for policy violation messages stack bars and alert indicators. */
    private static final Color POLICY_VIOLATION_COLOR = new Color(245, 240, 17);

    /** Color for routing failure messages stack bars and alert indicators. */
    private static final Color ROUTING_FAILURE_COLOR = new Color(255, 20, 20);

    /**
     * Background color of the indicator plot. Chosen to enhance visibility of
     * the alert indicator colors ({@link #POLICY_VIOLATION_COLOR} and
     * {@link #ROUTING_FAILURE_COLOR}).
     */
    private static final Color INDICATOR_PLOT_BACKCOLOR = new Color(128, 128, 128);

    /** Shape for alert indicators (routing failure and policy violation). */
    private static final Rectangle2D.Double INDICATOR_SHAPE = new Rectangle2D.Double(-3., -3., 6., 6.);

    private static final String TIME_AXIS_LABEL = _resources.getString("timeAxisLabel");
    private static final String MESSAGE_RATE_AXIS_LABEL = _resources.getString("messageRateAxisLabel");
    private static final String RESPONSE_TIME_AXIS_LABEL = _resources.getString("responseTimeAxisLabel");

    /**
     * Name of series in {@link #_messageRates} that
     * contains success message rate and counts.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_SUCCESS = _resources.getString("seriesSuccess");

    /**
     * Name of series in {@link #_messageRates} that
     * contains policy violation message rate and counts.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_POLICY_VIOLATION = _resources.getString("seriesPolicyViolation");

    /**
     * Name of series in {@link #_messageRates} that
     * contains routing failure message rate and counts.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_ROUTING_FAILURE = _resources.getString("seriesRoutingFailure");

    /**
     * Name of the series {@link #_frontendResponseTimes} that contains frontend
     * response times.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_FRONTEND_RESPONSE = _resources.getString("seriesFrontend");

    /**
     * Name of the series {@link #_backendResponseTimes} that contains backend
     * response times.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_BACKEND_RESPONSE = _resources.getString("seriesBackend");

    /** Name prefix for the pop-up context menu item to show audits. */
    private static final String SHOW_AUDITS_ITEM_NAME = _resources.getString("showAuditsItemName");

    /** Unique identifier for the pop-up context menu item to show audits. */
    private static final String SHOW_AUDITS_COMMAND = "SHOW AUDITS COMMAND";

    /** Title prefix for our custom GatewayAuditWindow ({@link #_gatewayAuditWindow}). */
    private static final String GATEWAY_AUDIT_WINDOW_TITLE = _resources.getString("gatewayAuditWindowTitle");

    /** A comparator to compare bins by period end. */
    private static final Comparator<MetricsBin> BIN_END_COMPARATOR = new Comparator<MetricsBin>() {
        public int compare(MetricsBin bin1, MetricsBin bin2) {
            final long diff =  bin1.getPeriodEnd() - bin2.getPeriodEnd();
            if (diff > 0L) return 1;
            if (diff < 0L) return -1;
            return 0;
        }
    };

    /** Metrics bin resolution. */
    private int _resolution;

    /** Nominal bin interval (in milliseconds). */
    private long _binInterval;

    /** Maximum time range to display (in milliseconds). */
    private long _maxTimeRange;

    /** The ServiceMetricsPanel containing this chart panel. */
    private final ServiceMetricsPanel _serviceMetricsPanel;

    /** Time zone for displaying time values. */
    private TimeZone _timeZone;

    /** Bins currently displayed in the chart. Keyed and sorted by period end. */
    private final SortedMap<Long, MetricsSummaryBin> _binsInChart;

    /** The bin in {@link #_binsInChart} with the latest period end. */
    private MetricsSummaryBin _latestBinInChart;

    /** Holding area for bins waiting to be added, i.e., when {@link #_updateSuspended}
        is true. Ordered by period end. */
    private final SortedSet<MetricsSummaryBin> _binsToAdd = new TreeSet<MetricsSummaryBin>(BIN_END_COMPARATOR);

    /**
     * Data structure containing frontend and backend response times.
     * The response time plot rely on the series order:
     * Series 0 must be {@link #_frontendResponseTimes}.
     * Series 1 must be {@link #_backendResponseTimes}.
     */
    private final TimePeriodValuesWithHighLowCollection _responseTimes;

    /**
     * Data structure containing frontend response times.
     * As element 0 of {@link #_responseTimes}.
     */
    private final TimePeriodValuesWithHighLow _frontendResponseTimes;

    /**
     * Data structure containing backend response times.
     * As element 1 of {@link #_responseTimes}.
     */
    private final TimePeriodValuesWithHighLow _backendResponseTimes;

    /**
     * Data structure containing message rate data.
     * The alert indicator plot and the stack bar plot rely on the series order:
     * Series 0 must be success message rate.
     * Series 1 must be policy violation message rate.
     * Series 2 must be routing failure message rate.
     */
    private final TimeTableXYDataset _messageRates;

    /** The shared time (x-) axis. */
    private DateAxis _xAxis;

    /** Y-axis for the response time plot. */
    private final NumberAxis _responsesYAxis;

    /** Y-axis for the message rate plot. */
    private final NumberAxis _ratesYAxis;

    /** Combined plot with shared time axis; containing response times, alert
        indicator, and message rates subplots. */
    private final CombinedDomainXYPlot _combinedPlot;

    /** Chart containing the combined plot. */
    private JFreeChart _chart;

    /** Start time of the currently selected period.
        Set to -1 when no period is selected. */
    private long _selectedPeriodStart = -1;

    /** End time of the currently selected period.
        Set to -1 when no period is selected. */
    private long _selectedPeriodEnd   = -1;

    /** Start time of the selected period waiting to be restored when data is available again.
        Set to -1 when no restoring is needed. */
    private long _periodStartToReselect = -1;

    /** End time of the selected period waiting to be restored when data is available again.
        Set to -1 when no restoring is needed. */
    private long _periodEndToReselect   = -1;

    /** Pop-up context menu. */
    private final JPopupMenu _popup;

    /** Pop-up context menu item to display audits for selected a metrics bin. */
    private final JMenuItem _showAuditsItem;

    /** Our custom GatewayAuditWindow to display audits for a selected metrics bin.
        Declared static for singleton; to prevent an explosion of such windows. */
    private static GatewayAuditWindow _gatewayAuditWindow;

    /** Indicates if chart data updating is currently suspended. */
    private boolean _updateSuspended = false;

    /** A tool tip generator for the response time plot. */
    private static class ResponseTimeToolTipGenerator implements XYToolTipGenerator {
        private static final MessageFormat FMT = new MessageFormat(_resources.getString("responseTimeTooltipFormat"));
        private final SimpleDateFormat _timeFormat = new SimpleDateFormat(_resources.getString("tooltipTimeFormat"));
        private final Map<Long, MetricsSummaryBin> _binsInChart;

        public ResponseTimeToolTipGenerator(final TimeZone timeZone, final Map<Long, MetricsSummaryBin> binsInChart) {
            _timeFormat.setTimeZone(timeZone);
            _binsInChart = binsInChart;
        }

        public String generateToolTip(XYDataset dataset, int series, int item) {
            final TimePeriodValuesWithHighLowCollection responseTimes = (TimePeriodValuesWithHighLowCollection) dataset;
            final TimePeriod period = responseTimes.getSeries(series).getTimePeriod(item);
            final Date periodStart = period.getStart();
            final Date periodEnd = period.getEnd();

            final MetricsSummaryBin bin = _binsInChart.get(periodEnd.getTime());
            if (bin == null)
                return null;    // Should not get here.
            if (bin.getNumTotal() == 0)
                return null;    // No tooltip if no message.

            final String frontLabel = responseTimes.getSeriesKey(0).toString();
            final int frontMax = bin.getMaxFrontendResponseTime()==null?0:bin.getMaxFrontendResponseTime();
            final int frontAvg = (int)Math.round(bin.getAverageFrontendResponseTime());
            final int frontMin = bin.getMinFrontendResponseTime()==null?0:bin.getMinFrontendResponseTime();

            final String backLabel = responseTimes.getSeriesKey(1).toString();
            final int backMax = bin.getMaxBackendResponseTime()==null?0:bin.getMaxBackendResponseTime();
            final int backAvg = (int)Math.round(bin.getAverageBackendResponseTime());
            final int backMin = bin.getMinBackendResponseTime()==null?0:bin.getMinBackendResponseTime();

            return FMT.format(new Object[]{
                    _timeFormat.format(periodStart),
                    _timeFormat.format(periodEnd),
                    frontLabel, new Integer(frontMax), new Integer(frontAvg), new Integer(frontMin),
                    backLabel, new Integer(backMax), new Integer(backAvg), new Integer(backMin)});
        }
    }

    /** A tool tip generator for the alert indicator plot. */
    private static class AlertIndicatorToolTipGenerator implements XYToolTipGenerator {
        private static final MessageFormat FMT = new MessageFormat(_resources.getString("alertIndicatorTooltipFormat"));
        private final SimpleDateFormat _timeFormat = new SimpleDateFormat(_resources.getString("tooltipTimeFormat"));

        public AlertIndicatorToolTipGenerator(final TimeZone timeZone) {
            _timeFormat.setTimeZone(timeZone);
        }

        public String generateToolTip(XYDataset dataset, int series, int item) {
            final TimeTableXYDataset messageRates = (TimeTableXYDataset) dataset;
            final TimePeriod period = messageRates.getTimePeriod(item);
            final Date periodStart = period.getStart();
            final Date periodEnd = period.getEnd();

            final String seriesLabel = dataset.getSeriesKey(series).toString();
            final double msgRate = messageRates.getY(series, item).doubleValue();
            final int numMsg = (int)Math.round(msgRate * (periodEnd.getTime() - periodStart.getTime()) / 1000.);

            return FMT.format(new Object[] {
                    _timeFormat.format(periodStart),
                    _timeFormat.format(periodEnd),
                    seriesLabel,
                    new Integer(numMsg),
                    new Double(msgRate)});
        }
    }

    /** A tool tip generator for the message rate plot. */
    private static class MessageRateToolTipGenerator implements XYToolTipGenerator {
        private static final MessageFormat FMT = new MessageFormat(_resources.getString("messageRateTooltipFormat"));
        private final SimpleDateFormat _timeFormat = new SimpleDateFormat(_resources.getString("tooltipTimeFormat"));
        private final Map<Long, MetricsSummaryBin> _binsInChart;

        public MessageRateToolTipGenerator(final TimeZone timeZone, final Map<Long, MetricsSummaryBin> binsInChart) {
            _timeFormat.setTimeZone(timeZone);
            _binsInChart = binsInChart;
        }

        public String generateToolTip(XYDataset dataset, int series, int item) {
            final TimeTableXYDataset messageRates = (TimeTableXYDataset) dataset;
            final TimePeriod period = messageRates.getTimePeriod(item);
            final Date periodStart = period.getStart();
            final Date periodEnd = period.getEnd();

            final MetricsSummaryBin bin = _binsInChart.get(periodEnd.getTime());
            if (bin == null)
                return null;    // Should not get here.
            if (bin.getNumTotal() == 0)
                return null;    // No tooltip if no message.

            final String successLabel = dataset.getSeriesKey(0).toString();
            final double successRate = bin.getNominalSuccessRate();
            final int numSuccess = bin.getNumSuccess();

            final String violationLabel = dataset.getSeriesKey(1).toString();
            final double violationRate = bin.getNominalPolicyViolationRate();
            final int numViolation = bin.getNumPolicyViolation();

            final String failureLabel = dataset.getSeriesKey(2).toString();
            final double failureRate = bin.getNominalRoutingFailureRate();
            final int numFailure = bin.getNumRoutingFailure();

            return FMT.format(new Object[]{
                    _timeFormat.format(periodStart),
                    _timeFormat.format(periodEnd),
                    failureLabel, new Integer(numFailure), new Double(failureRate),
                    violationLabel, new Integer(numViolation), new Double(violationRate),
                    successLabel, new Integer(numSuccess), new Double(successRate)});
        }
    }

    /**
     * Generates a transparent color such that when painted on a white
     * background will look the same as the given opaque color. That is, find
     * (r, g, b, a) such that (r, g, b, a) + white = (r0, g0, b0, 0).
     * <p/>
     * This is used for matching the response time min-max bar color to the right
     * panel icon legend color. It is neccessary because the min-max bar uses
     * transparency while the icon legend is designed with opaque color.
     *
     * @param r0    red component of opague color
     * @param g0    green component of opague color
     * @param b0    blue component of opague color
     * @param a     alpha component of transparent color
     *
     * @return a transparent color
     */
    private static Color GetAlphaEquiv(int r0, int g0, int b0, int a) {
        final int r = Math.round(Math.max(0.f, (r0 + a - 255) * 255.f / a));
        final int g = Math.round(Math.max(0.f, (g0 + a - 255) * 255.f / a));
        final int b = Math.round(Math.max(0.f, (b0 + a - 255) * 255.f / a));
        return new Color(r, g, b, a);
    }

    /**
     * Composes a human friendly text string for a given time range.
     *
     * @param start     start time
     * @param end       end time
     * @param tz        time zone
     * @return text string of the form
     *         yyyy-MM-dd HH:mm:ss - yyyy-MM-dd HH:mm:ss XXX if spanning more than one calendar date,
     *         yyyy-MM-dd HH:mm:ss - HH:mm:ss XXX if within one calendar date;
     *         where XXX is the time zone short name
     */
    private static String timeRangeAsString(final Date start, final Date end, final TimeZone tz) {
        final StringBuilder sb = new StringBuilder();
        final SimpleDateFormat YMD_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        final SimpleDateFormat HMS_FORMAT = new SimpleDateFormat("HH:mm:ss");
        YMD_FORMAT.setTimeZone(tz);
        HMS_FORMAT.setTimeZone(tz);
        final String fromYMD = YMD_FORMAT.format(start);
        final String toYMD = YMD_FORMAT.format(end);
        sb.append(fromYMD);
        sb.append(" ");
        sb.append(HMS_FORMAT.format(start));
        sb.append(" \u2013 ");  // en-dash
        if (! fromYMD.equals(toYMD)) sb.append(toYMD);
        sb.append(HMS_FORMAT.format(end));
        sb.append(" ");
        sb.append(tz.getDisplayName(tz.inDaylightTime(start), TimeZone.SHORT));
        return sb.toString();
    }

    /**
     * Sets x-axis range explicitly if there is no data in the plots; otherwise
     * JFreeChart will use some random range. (Bugzilla # 2277)
     */
    private void setXAxisRangeIfNoData() {
        if (_messageRates.getItemCount() == 0) {
            final long now = System.currentTimeMillis();    // Gateway clock time would be better but there is no accurate way to get that.
            final long max = MetricsBin.periodStartFor(_resolution, (int)_binInterval, now);    // This equals end time of latest completed period.
            _chart.getXYPlot().getDomainAxis().setRange(new Range(max - _maxTimeRange, max), false, true);
        }
    }

    /**
     * Reset the y-axis ranges based on the currently visible time range.
     */
    private synchronized void resetYAxisRanges() {
        final Range displayedRange = _xAxis.getRange();

        // Response times plot.
        double maxResponse = 0.;
        for (int i = 0; i < _frontendResponseTimes.getItemCount(); ++ i) {
            TimePeriodValueWithHighLow value = (TimePeriodValueWithHighLow) _frontendResponseTimes.getDataItem(i);
            if (value.getPeriod().getEnd().getTime() >= displayedRange.getLowerBound() &&
                    value.getPeriod().getStart().getTime() <= displayedRange.getUpperBound())
            {
                maxResponse = Math.max(maxResponse, value.getHighValue().doubleValue());
            }
        }
        maxResponse *= 1. + _responsesYAxis.getUpperMargin();
        final double yMaxResponse = Math.max(maxResponse, _responsesYAxis.getAutoRangeMinimumSize());
        _responsesYAxis.setRange(new Range(0, yMaxResponse));

        // Message rates plot.
        double maxRate = 0.;
        for (int i = 0; i < _messageRates.getItemCount(); ++ i) {
            TimePeriod period = _messageRates.getTimePeriod(i);
            if (period.getEnd().getTime() >= displayedRange.getLowerBound() &&
                    period.getStart().getTime() <= displayedRange.getUpperBound())
            {
                // Calculates the total message rate by adding up the rates
                // for success, policy violation and routing failure.
                double totalRate = 0.;
                for (int series = 0; series < _messageRates.getSeriesCount(); ++ series) {
                    totalRate += _messageRates.getYValue(series, i);
                }
                maxRate = Math.max(maxRate, totalRate);
            }
        }
        maxRate *= 1. + _ratesYAxis.getUpperMargin();
        final double yMaxRate = Math.max(maxRate, _ratesYAxis.getAutoRangeMinimumSize());
        _ratesYAxis.setRange(new Range(0, yMaxRate));
    }

    /**
     * @param resolution    the metric bin resolution ({@link MetricsBin#RES_FINE},
     *                      {@link MetricsBin#RES_HOURLY} or
     *                      {@link MetricsBin#RES_DAILY}); can be changed later
     *                      using {@link #setResolution}.
     * @param binInterval   nominal bin interval (in milliseconds); can be
     *                      changed later using {@link #setBinInterval}.
     * @param maxTimeRange  maximum time range to display; can be changed later
     *                      using {@link #setMaxTimeRange}.
     * @param serviceMetricsPanel   the ServiceMetricsPanel containing this chart panel
     */
    public MetricsChartPanel(int resolution,
                             long binInterval,
                             long maxTimeRange,
                             ServiceMetricsPanel serviceMetricsPanel) {
        super(null);
        _resolution = resolution;
        _binInterval = binInterval;
        _maxTimeRange = maxTimeRange;
        _serviceMetricsPanel = serviceMetricsPanel;

        // Creates the empty data structures.
        _binsInChart = new TreeMap<Long, MetricsSummaryBin>();
        _frontendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_FRONTEND_RESPONSE);
        _backendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_BACKEND_RESPONSE);
        _responseTimes = new TimePeriodValuesWithHighLowCollection();
        _responseTimes.addSeries(_frontendResponseTimes);
        _responseTimes.addSeries(_backendResponseTimes);
        _messageRates = new TimeTableXYDataset();

        // Gets the time zone to use from gateway.
        final Registry registry = Registry.getDefault();
        if (registry.isAdminContextPresent()) {
            _timeZone = TimeZone.getTimeZone(registry.getClusterStatusAdmin().getCurrentClusterTimeZone());
        }
        if (_timeZone == null) {
            _logger.warning("Failed to get time zone from gateway. Falling back to use local time zone for display.");
            _timeZone = TimeZone.getDefault();
        }

        //
        // Top plot for response times.
        //

        _responsesYAxis = new NumberAxis(RESPONSE_TIME_AXIS_LABEL);
        _responsesYAxis.setAutoRange(true);
        _responsesYAxis.setRangeType(RangeType.POSITIVE);
        _responsesYAxis.setAutoRangeMinimumSize(10.);
        final TimePeriodValueWithHighLowRenderer responsesRenderer = new TimePeriodValueWithHighLowRenderer();
        responsesRenderer.setDrawBarOutline(false);
        responsesRenderer.setSeriesStroke(0, RESPONSE_AVG_STROKE);
        responsesRenderer.setSeriesPaint(0, FRONTEND_RESPONSE_AVG_COLOR);
        responsesRenderer.setSeriesFillPaint(0, FRONTEND_RESPONSE_MINMAX_COLOR);
        responsesRenderer.setSeriesStroke(1, RESPONSE_AVG_STROKE);
        responsesRenderer.setSeriesPaint(1, BACKEND_RESPONSE_AVG_COLOR);
        responsesRenderer.setSeriesFillPaint(1, BACKEND_RESPONSE_MINMAX_COLOR);
        responsesRenderer.setBaseToolTipGenerator(new ResponseTimeToolTipGenerator(_timeZone, _binsInChart));
        final XYPlot responsesPlot = new XYPlot(_responseTimes, null, _responsesYAxis, responsesRenderer);
        responsesPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        //
        // Middle plot for alert indicators of policy violations and routing failures.
        // This augments the message rate plot below since small values there
        // may shows up too small to see.
        //

        final NumberAxis alertsYAxis = new NumberAxis() {
            /**
             * Overrides parent method to prevent any zooming, particularly
             * autozoom. We need to do this because although {@link #setRange(double,double) setRange}
             * will unset autozoom, its effect is not persistent.
             */
            public void resizeRange(double percent, double anchorValue) {
                // Do nothing.
            }
        };
        alertsYAxis.setRange(0., 10.);   // This y-range and the forced y values
                                    // below are chosen to space out the
                                    // alert indicator shapes evenly.
        alertsYAxis.setTickLabelsVisible(false);
        alertsYAxis.setTickMarksVisible(false);
        final ReplaceYShapeRenderer alertsRenderer = new ReplaceYShapeRenderer();
        alertsRenderer.setSeriesVisible(0, Boolean.FALSE);   // success message rate
        alertsRenderer.setSeriesYValue(1, 3.);               // policy violation message rate
        alertsRenderer.setSeriesYValue(2, 7.);               // routing failure message rate
        alertsRenderer.setSeriesPaint(1, POLICY_VIOLATION_COLOR);
        alertsRenderer.setSeriesPaint(2, ROUTING_FAILURE_COLOR);
        alertsRenderer.setSeriesShape(1, INDICATOR_SHAPE);
        alertsRenderer.setSeriesShape(2, INDICATOR_SHAPE);
        final AlertIndicatorToolTipGenerator alertIndicatorToolTipGenerator = new AlertIndicatorToolTipGenerator(_timeZone);
        alertsRenderer.setBaseToolTipGenerator(alertIndicatorToolTipGenerator);
        final XYPlot alertsPlot = new XYPlot(_messageRates, null, alertsYAxis, alertsRenderer);
        alertsPlot.setBackgroundPaint(INDICATOR_PLOT_BACKCOLOR);
        alertsPlot.setRangeGridlinesVisible(false);
        alertsPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        //
        // Bottom plot for message rates.
        //

        _ratesYAxis = new NumberAxis(MESSAGE_RATE_AXIS_LABEL);
        _ratesYAxis.setAutoRange(true);
        _ratesYAxis.setRangeType(RangeType.POSITIVE);
        _ratesYAxis.setAutoRangeMinimumSize(0.0001);     // Still allows 1 msg per day to be visible.
        final StackedXYBarRenderer ratesRenderer = new StackedXYBarRendererEx();
        ratesRenderer.setDrawBarOutline(false);
        ratesRenderer.setSeriesPaint(0, SUCCESS_COLOR);         // success message rate
        ratesRenderer.setSeriesPaint(1, POLICY_VIOLATION_COLOR);// policy violation message rate
        ratesRenderer.setSeriesPaint(2, ROUTING_FAILURE_COLOR); // routing failure message rate
        final MessageRateToolTipGenerator messageRateToolTipGenerator = new MessageRateToolTipGenerator(_timeZone, _binsInChart);
        ratesRenderer.setBaseToolTipGenerator(messageRateToolTipGenerator);
        final XYPlot ratesPlot = new XYPlot(_messageRates, null, _ratesYAxis, ratesRenderer);

        //
        // Now combine all plots to share the same time (x-)axis.
        //

        _xAxis = new DateAxis(TIME_AXIS_LABEL /* effective time zone name to be appended later */, _timeZone) {
            public void setRange(Range range, boolean turnOffAutoRange, boolean notify) {
                // Do not zoom in any smaller than the nominal bin interval.
                if ((range.getUpperBound() - range.getLowerBound()) >= _binInterval)
                    super.setRange(range, turnOffAutoRange, notify);
            }
        };
        _xAxis.setAutoRange(true);
        _xAxis.setFixedAutoRange(_maxTimeRange);
        _xAxis.addChangeListener(new AxisChangeListener() {
            public void axisChanged(AxisChangeEvent axisChangeEvent) {
                resetYAxisRanges();
            }
        });
        _combinedPlot = new CombinedDomainXYPlot(_xAxis);
        _combinedPlot.setGap(0.);
        _combinedPlot.add(responsesPlot, 35);
        _combinedPlot.add(alertsPlot, 5);
        _combinedPlot.add(ratesPlot, 60);
        _combinedPlot.setOrientation(PlotOrientation.VERTICAL);

        _chart = new JFreeChart(null,   // chart title
                null,                   // title font
                _combinedPlot,
                false                   // generate legend?
        );
        _chart.setAntiAlias(false);

        setChart(_chart);
        setRangeZoomable(false);        // Suppresses range (y-axis) zooming.
        setFillZoomRectangle(true);
        setInitialDelay(100);           // Makes tool tip respond fast.
        setDismissDelay(Integer.MAX_VALUE); // Makes tool tip display indefinitely.
        setReshowDelay(100);
        setMaximumDrawWidth(1920);
        setMaximumDrawHeight(1200);

        // Adds custom pop-up context menu; replacing the default inherited from {@link ChartPanel}.
        _showAuditsItem = new JMenuItem();  // Name to be customized based on time period selected.
        _showAuditsItem.setActionCommand(SHOW_AUDITS_COMMAND);
        _showAuditsItem.addActionListener(this);
        _popup = new JPopupMenu();
        _popup.add(_showAuditsItem);
        setPopupMenu(_popup);

        setXAxisRangeIfNoData();
    } /* constructor */

    /**
     * @param resolution    bin resolution ({@link MetricsBin#RES_FINE},
    *                       {@link MetricsBin#RES_HOURLY} or {@link MetricsBin#RES_DAILY})
     */
    public void setResolution(final int resolution) {
        _resolution = resolution;
    }

    /**
     * @param binInterval   bin nominal time period interval in milliseconds
     */
    public void setBinInterval(final long binInterval) {
        _binInterval = binInterval;
    }

    /**
     * @param maxTimeRange  maximum time range to display (in milliseconds)
     */
    public void setMaxTimeRange(final long maxTimeRange) {
        _maxTimeRange = maxTimeRange;
        _xAxis.setFixedAutoRange(_maxTimeRange);
        setXAxisRangeIfNoData();
    }

    /**
     * Stores away bins waiting to be added (when chart data updating is suspended).
     *
     * @param bins   metrics bins waiting to be added
     */
    private void storeBinsToAdd(final Collection<MetricsSummaryBin> bins) {
        _binsToAdd.addAll(bins);
        if (_logger.isLoggable(Level.FINER)) {
            _logger.finer("Stored away " + bins.size() + " new bins while chart is suspended. (total=" + _binsToAdd.size() + ")");
        }

        // Limits the memory used by bins waiting to be added; by removing bins
        // older than our maximum allowed time range.
        if (! _binsToAdd.isEmpty()) {
            final long lowerBound = _binsToAdd.last().getPeriodEnd() - _maxTimeRange;
            final int numBefore = _binsToAdd.size();

            for (Iterator<MetricsSummaryBin> i = _binsToAdd.iterator(); i.hasNext();) {
                final MetricsSummaryBin bin = i.next();
                if (bin.getPeriodStart() >= lowerBound) {
                    break;  // The rest are within maximum allowed time range.
                }
                i.remove();
            }

            if (_logger.isLoggable(Level.FINER)) {
                final int numRemoved = numBefore - _binsToAdd.size();
                if (numRemoved != 0) {
                    _logger.finer("Purged " + numRemoved + " bins waiting to be added but too old.");
                }
            }
        }
    }

    /**
     * Adds metric bins to the dataset and update the plots.
     *
     * @param bins  new data to be added
     */
    public synchronized void addData(final Collection<MetricsSummaryBin> bins) {
        if (bins.size() == 0) return;

        if (_updateSuspended) {
            storeBinsToAdd(bins);
            return;
        }

        // Temporarily disable notification so plots won't be redrawn needlessly
        // when datasets are changing. Note that this does not entirely prevent
        // the datasets from being accessed.
        _chart.setNotify(false);

        // Adds new data from the bins to our JFreeChart data structures.
        for (MetricsSummaryBin bin : bins) {
            _binsInChart.put(bin.getPeriodEnd(), bin);
            if (_latestBinInChart == null || _latestBinInChart.getPeriodEnd() < bin.getPeriodEnd()) {
                _latestBinInChart = bin;
            }

            // We are using the bin's nominal start and end times instead of
            // actual times, in order to avoid unsightly gaps in the bar charts.
            final SimpleTimePeriod period = new SimpleTimePeriod(bin.getPeriodStart(), bin.getPeriodEnd());

            _frontendResponseTimes.add(period,
                    bin.getAverageFrontendResponseTime(),
                    (double)(bin.getMaxFrontendResponseTime()==null ? 0 : bin.getMaxFrontendResponseTime()),
                    (double)(bin.getMinFrontendResponseTime()==null ? 0 : bin.getMinFrontendResponseTime()));
            _backendResponseTimes.add(period,
                    bin.getAverageBackendResponseTime(),
                    (double)(bin.getMaxBackendResponseTime()==null?0:bin.getMaxBackendResponseTime()),
                    (double)(bin.getMinBackendResponseTime()==null?0:bin.getMinBackendResponseTime()));

            // Since we are using nominal start and end times, we have to use
            // message rates calculated with nominal time interval to avoid
            // display discrepancy.
            //
            // Make sure the series are added in this order.
            _messageRates.add(period, new Double(bin.getNominalCompletedRate()), SERIES_SUCCESS, false);
            _messageRates.add(period, new Double(bin.getNominalPolicyViolationRate()), SERIES_POLICY_VIOLATION, false);
            _messageRates.add(period, new Double(bin.getNominalRoutingFailureRate()), SERIES_ROUTING_FAILURE, false);
        }

        // Now that the overall time range has change, remove data older than
        // our maximum allowed time range.
        final long newUpperBound = _latestBinInChart.getPeriodEnd();
        final long newLowerBound = newUpperBound - _maxTimeRange;

        for (Iterator<Long> i = _binsInChart.keySet().iterator(); i.hasNext(); ) {
            final Long periodEnd = i.next();
            if (periodEnd <= newLowerBound) {
                i.remove();
            } else {
                break;      // The rest is within limit; since this is sorted.
            }
        }

        for (int i = _frontendResponseTimes.getItemCount() - 1; i >= 0; -- i) {
            if (_frontendResponseTimes.getTimePeriod(i).getEnd().getTime() <= newLowerBound) {
                _frontendResponseTimes.delete(i, i);
            }
        }
        for (int i = _backendResponseTimes.getItemCount() - 1; i >= 0; -- i) {
            if (_backendResponseTimes.getTimePeriod(i).getEnd().getTime() <= newLowerBound) {
                _backendResponseTimes.delete(i, i);
            }
        }

        for (int item = _messageRates.getItemCount() - 1; item >= 0; -- item) {
            final TimePeriod period = _messageRates.getTimePeriod(item);
            if (period.getEnd().getTime() <= newLowerBound) {
                _messageRates.remove(period, SERIES_SUCCESS, false);
                _messageRates.remove(period, SERIES_POLICY_VIOLATION, false);
                _messageRates.remove(period, SERIES_ROUTING_FAILURE, false);
            }
        }

        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest("Sizes of data structures: " +
                           "_binsInChart=" + _binsInChart.size() +
                           ", _frontendResponseTimes=" + _frontendResponseTimes.getItemCount() +
                           ", _backendResponseTimes=" + _backendResponseTimes.getItemCount() +
                           ", _messageRates[0]=" + _messageRates.getItemCount(0) +
                           ", _messageRates[1]=" + _messageRates.getItemCount(1) +
                           ", _messageRates[2]=" + _messageRates.getItemCount(2));
        }

        // Updates current effective time zone in time axis label, e.g., PST vs PDT.
        final boolean inDST = _timeZone.inDaylightTime(new Date(newUpperBound));
        _xAxis.setLabel(TIME_AXIS_LABEL + " (" + _timeZone.getDisplayName(inDST, TimeZone.SHORT) + ")");

        // Re-enable notification now that all dataset change is done.
        _chart.setNotify(true);

        // Now sends notifications to cause plots to update.
        try {
            _responseTimes.validateObject();
            _messageRates.validateObject();
        } catch (InvalidObjectException e) {
            // Should not get here.
        }

        setXAxisRangeIfNoData();
        restoreAutoRange();

        // Restores the selected period using the new bins.
        if (_periodStartToReselect != -1 && _periodEndToReselect != -1) {
            setSelectedPeriod(_periodStartToReselect, _periodEndToReselect);
            _periodStartToReselect = -1;    // Unset to prevent unneccessary repeats.
            _periodEndToReselect   = -1;
        }
    } // addData

    /**
     * Clears all data and updates the plots.
     *
     * @param saveSelectedPeriod    whether to keep selected period (if any)
     *                              around when data is available again
     */
    public synchronized void clearData(final boolean saveSelectedPeriod) {
        if (saveSelectedPeriod) {
            // Saves the selected period to restore when chart data is
            // repopulated. We don't know when that will happen because
            // {@link #addData} is called on a different thread.
            _periodStartToReselect = _selectedPeriodStart;
            _periodEndToReselect   = _selectedPeriodEnd;
        }

        // Temporarily disable notification so plots won't be redrawn needlessly
        // when datasets are changing. Note that this does not entirely prevent
        // the datasets from being accessed.
        _chart.setNotify(false);

        _binsInChart.clear();
        _latestBinInChart = null;
        _binsToAdd.clear();

        _frontendResponseTimes.delete(0, _frontendResponseTimes.getItemCount() - 1);
        _backendResponseTimes.delete(0, _backendResponseTimes.getItemCount() - 1);

        for (int item = _messageRates.getItemCount(0) - 1; item >= 0; -- item) {
            final TimePeriod period = _messageRates.getTimePeriod(item);
            _messageRates.remove(period, SERIES_SUCCESS, false);
            _messageRates.remove(period, SERIES_POLICY_VIOLATION, false);
            _messageRates.remove(period, SERIES_ROUTING_FAILURE, false);
        }

        // Re-enable notification now that all dataset change is done.
        _chart.setNotify(true);

        // Now let the data structures send notifications to cause plots to update.
        try {
            _responseTimes.validateObject();
            _messageRates.validateObject();
        } catch (InvalidObjectException e) {
            // Should not get here.
        }

        restoreAutoDomainBounds();
        setXAxisRangeIfNoData();
        restoreAutoRange();
        resumeUpdate();
        unhighlightPeriod();
    } // clearData

    /**
     * Finds the bin (amongst the charted bins) bracketing the given time.
     *
     * @param t     the time to bracket (in milliseconds since epoch)
     * @return <code>null</code> if none found
     */
    private MetricsSummaryBin findBin(final long t) {
        // First try fast lookup among charted bins using a period end time
        // estimated by resolution interval. This may not find the bin since
        // since resolution interval might have changed.
        long periodEnd = MetricsBin.periodEndFor(_resolution, (int)_binInterval, t);
        MetricsSummaryBin bin = _binsInChart.get(periodEnd);
        if (bin != null) {
            // Verifies that the time is indeed bracketed by this bin period.
            if (bin.getPeriodStart() > t) {
                bin = null;     // Nope.
            }
        }

        // If that didn't work. Try searching one-by-one. This is time consuming.
        if (bin == null) {
            for (MetricsSummaryBin b : _binsInChart.values()) {
                if (t >= b.getPeriodStart() && t < b.getPeriodEnd()) {
                    bin = b;
                    break;
                }
            }
        }

        return bin;
    }

    /**
     * Finds the bin (amongst the charted bins) with the exact period start and end time.
     *
     * @param periodStart   period start to find
     * @param periodEnd     period end to find
     * @return <code>null</code> if no bin found
     */
    private MetricsSummaryBin findBin(final long periodStart, final long periodEnd) {
        final MetricsSummaryBin bin = _binsInChart.get(periodEnd);
        if (bin == null || bin.getPeriodStart() != periodStart) {  // Verifies.
            return null;
        }
        return bin;
    }

    /**
     * Highlights a period in the chart; replacing any previous highlight.
     *
     * @param periodStart   period start time
     * @param periodEnd     period end time
     */
    private void highlightPeriod(final long periodStart, final long periodEnd) {
        // Paints vertical marker strip in selected time period.
        IntervalMarker marker = new IntervalMarker(periodStart,
                                                   periodEnd,
                                                   new Color(255, 0, 255, 64),
                                                   new BasicStroke(0.0f),
                                                   new Color(255, 0, 255, 0),
                                                   new BasicStroke(0.0f),
                                                   0.0f);
        // noinspection unchecked
        for (XYPlot plot : (List<XYPlot>)_combinedPlot.getSubplots()) {
            plot.clearDomainMarkers();  // Clears previous highlight; if any.
            plot.addDomainMarker(marker, Layer.FOREGROUND);
        }
    }

    /**
     * Removes any previous highlight.
     */
    private void unhighlightPeriod() {
        // noinspection unchecked
        for (XYPlot plot : (List<XYPlot>)_combinedPlot.getSubplots()) {
            plot.clearDomainMarkers();
        }
    }

    /**
     * Selects the bin (amongst the charted bins) bracketing the given time
     * highlights it and updates the dashboard "Selection" tab.
     *
     * @param t     the time to bracket (in milliseconds since epoch)
     */
    private void setSelectedPeriod(final long t) {
        if (t >= _xAxis.getMinimumDate().getTime() && t < _xAxis.getMaximumDate().getTime()) {
            // Find the charted bin containing that period.
            final MetricsSummaryBin bin = findBin(t);

            long periodStart;
            long periodEnd;
            if (bin == null) {
                // No charted bin contains that time, then estimate the period based on resolution.
                periodStart = MetricsBin.periodStartFor(_resolution, (int)_binInterval, t);
                periodEnd = MetricsBin.periodEndFor(_resolution, (int)_binInterval, t);
            } else {
                periodStart = bin.getPeriodStart();
                periodEnd = bin.getPeriodEnd();
            }

            highlightPeriod(periodStart, periodEnd);
            _serviceMetricsPanel.setSelectedBin(bin, periodStart, periodEnd, true);
            _selectedPeriodStart = periodStart;
            _selectedPeriodEnd   = periodEnd;
        } else {
            // Out of time axis range. This means to deselect.
            unhighlightPeriod();
            _serviceMetricsPanel.setSelectedBin(null, -1, -1, false);
            _selectedPeriodStart = -1;
            _selectedPeriodEnd   = -1;
        }
    }

    /**
     * Selects the bin (amongst the charted bins) with the exact period start and end time,
     * highlights it and updates the dashboard "Selection" tab.
     *
     * @param periodStart   period start to find
     * @param periodEnd     period end to find
     */
    private void setSelectedPeriod(final long periodStart, final long periodEnd) {
        if (periodStart >= _xAxis.getMinimumDate().getTime() && periodEnd < _xAxis.getMaximumDate().getTime()) {
            // Find the charted bin containing that period.
            final MetricsSummaryBin bin = findBin(periodStart, periodEnd);
            highlightPeriod(periodStart, periodEnd);
            _serviceMetricsPanel.setSelectedBin(bin, periodStart, periodEnd, true);
            _selectedPeriodStart = periodStart;
            _selectedPeriodEnd   = periodEnd;
        } else {
            // Out of time axis range. This means to deselect.
            unhighlightPeriod();
            _serviceMetricsPanel.setSelectedBin(null, -1, -1, false);
            _selectedPeriodStart = -1;
            _selectedPeriodEnd   = -1;
        }
    }

    /**
     * Overrides to bring up our custom pop-up context menu.
     */
    protected synchronized void displayPopupMenu(int x, int y) {
        // Converts the pixel x-coordinate to time value.
        final Point2D p = translateScreenToJava2D(new Point(x, y));
        final Rectangle2D rectangle2d = getChartRenderingInfo().getPlotInfo().getDataArea();
        final long t = (long)_xAxis.java2DToValue(p.getX(), rectangle2d, _combinedPlot.getDomainAxisEdge());

        // Select the period as a side effect, i.e., like left-click.
        setSelectedPeriod(t);

        // Customizes the name of the show audits menu item.
        _showAuditsItem.setText(SHOW_AUDITS_ITEM_NAME + " (" + timeRangeAsString(new Date(_selectedPeriodStart), new Date(_selectedPeriodEnd), _timeZone) + ")");

        _popup.show(this, x, y);
    }

    /** Overrides to execute the selected item in our custom pop-up context menu. */
    public void actionPerformed(ActionEvent event) {
        final String command = event.getActionCommand();
        if (command.equals(SHOW_AUDITS_COMMAND)) {
            showAudits(_selectedPeriodStart, _selectedPeriodEnd);
        } else {
            throw new RuntimeException("Internal error: Missing handler for context menu item: " + command);
        }
    }

    /**
     * Brings up a window to show audits for the given time period.
     *
     * @param startTime     start time of audit records
     * @param endTime       end time of audit records
     */
    private void showAudits(final long startTime, final long endTime) {
        final Date startDate = new Date(startTime);
        final Date endDate   = new Date(endTime);

        String errDialogMsg = null;

        final Registry registry = Registry.getDefault();
        if (registry.isAdminContextPresent()) {
            final AuditAdmin auditAdmin = registry.getAuditAdmin();
            final ClusterNodeInfo nodeSelected = _serviceMetricsPanel.getClusterNodeSelected();
            final EntityHeader serviceSelected = _serviceMetricsPanel.getPublishedServiceSelected();

            final AuditSearchCriteria criteria = new AuditSearchCriteria.Builder().
                    fromTime(startDate).
                    toTime(endDate).
                    nodeId(nodeSelected == null ? null : nodeSelected.getId()).
                    serviceName(serviceSelected == null ? null : serviceSelected.getName()).build();
            try {
                final Either<String, AuditRecordHeader[]> recordsEither = doAsyncAdmin(auditAdmin,
                        SwingUtilities.getWindowAncestor(MetricsChartPanel.this),
                        "Show Audits",
                        "Retrieving Audits",
                        auditAdmin.findHeaders(criteria));
                AuditRecordHeader[] records = recordsEither.right();

                if (_gatewayAuditWindow == null) {
                    _gatewayAuditWindow = new GatewayAuditWindow(false);
                    _gatewayAuditWindow.pack();
                }

                _gatewayAuditWindow.setVisible(true);
                _gatewayAuditWindow.setExtendedState(Frame.NORMAL);
                _gatewayAuditWindow.toFront();
                _gatewayAuditWindow.setTitle(GATEWAY_AUDIT_WINDOW_TITLE + " (" +
                        timeRangeAsString(startDate, endDate, _timeZone) + ")");

                _gatewayAuditWindow.displayAuditHeaders(CollectionUtils.list(records));
            } catch (FindException e) {
                _logger.warning("Failed to query for audit events: " + e.getMessage());
                errDialogMsg = e.getMessage();
            } catch (InterruptedException e) {
                // action cancelled, do nothing
            } catch (InvocationTargetException e) {
                _logger.warning("Failed to query for audit events: " + e.getMessage());
                errDialogMsg = e.getMessage();
            }
        } else {
            _logger.warning("Registry.isAdminContextPresent() returns false.");
            errDialogMsg = "Disconnected from gateway.";
        }

        if (errDialogMsg != null) {
            // ? Should I use ExceptionDialog instead ?
            JOptionPane.showMessageDialog(this, "Failed to get audit events from gateway: " + errDialogMsg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Suspends updating of displayed chart data. */
    private synchronized void suspendUpdate() {
        _updateSuspended = true;
    }

    /** Resumes updating of displayed chart data. */
    public synchronized void resumeUpdate() {
        _updateSuspended = false;

        // Now adds bins waiting to be added.
        if (! _binsToAdd.isEmpty()) {
            addData(_binsToAdd);
            _binsToAdd.clear();
        }
    }

    /** Overrides to handle zooming. */
    public void mousePressed(MouseEvent e) {
        // The user is starting to drag-draw the rubberband zoom box. Need to
        // temporarily suspend updating the chart data, otherwise the rubberband
        // zoom box will appear jumpy.
        // @see http://www.jfree.org/phpBB2/viewtopic.php?t=10022&highlight=zoom+dynamic
        suspendUpdate();
        super.mousePressed(e);
    }

    /** Overrides to handle zooming. */
    public void mouseReleased(MouseEvent e) {
        // The user has finished zooming.
        super.mouseReleased(e);

        // Resumes chart data updating only if not zoomed in.
        if (_xAxis.isAutoRange()) {
            resumeUpdate();
        }
    }

    /** Overrides to highlight selected period being clicked. */
    public void mouseClicked(MouseEvent event) {
        // Converts the pixel x-coordinate to time value.
        final Point2D p = translateScreenToJava2D(event.getPoint());
        final Rectangle2D rectangle2d = getChartRenderingInfo().getPlotInfo().getDataArea();
        final long t = (long)_xAxis.java2DToValue(p.getX(), rectangle2d, _combinedPlot.getDomainAxisEdge());
        setSelectedPeriod(t);
    }

    public void restoreAutoRange() {
        _xAxis.setAutoRange(true);
        _responsesYAxis.setAutoRange(true);
        _ratesYAxis.setAutoRange(true);
    }
}
