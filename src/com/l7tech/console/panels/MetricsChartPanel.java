package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.GatewayAuditWindow;
import com.l7tech.console.panels.dashboard.DashboardWindow;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.jfree.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.MetricsBin;
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
import java.awt.geom.Rectangle2D;
import java.io.InvalidObjectException;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

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

    /** Metrics bin resolution. */
    final int _resolution;

    /** Nominal bin interval (in milliseconds). */
    private final long _binInterval;

    /** Maximum time range of data to keep around (in milliseconds). */
    private long _maxTimeRange;

    /** The Dashboard window containing this panel. */
    private final DashboardWindow _dashboardWindow;

    /** Time zone for displaying time values. */
    private TimeZone _timeZone;

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

    /** Pop-up context menu. */
    private final JPopupMenu _popup;

    /** Pop-up context menu item to display audits for selected a metrics bin. */
    private final JMenuItem _showAuditsItem;

    /** Start date of metrics bin selected to show audits. */
    private Date _showAuditsStartDate;

    /** End date of metrics bin selected to show audits. */
    private Date _showAuditsEndDate;

    /** Our custom GatewayAuditWindow to display audits for a selected metrics bin.
        Declared static for singleton; to prevent an explosion of such windows. */
    private static GatewayAuditWindow _gatewayAuditWindow;

    /** Holding area for metrics bins waiting to be added; when {@link #_updateSuspended} is true. */
    private final SortedSet<MetricsBin> _binsToAdd = new TreeSet<MetricsBin>();

    /** Indicates if chart data updating is currently suspended. */
    private boolean _updateSuspended = false;

    /** A tool tip generator for the response time plot. */
    private static class ResponseTimeToolTipGenerator implements XYToolTipGenerator {
        private static final MessageFormat FMT = new MessageFormat(_resources.getString("responseTimeTooltipFormat"));
        private final TimeTableXYDataset _messageRates;

        public ResponseTimeToolTipGenerator(TimeTableXYDataset messageRates) {
            _messageRates = messageRates;
        }

        public String generateToolTip(XYDataset dataset, int series, int item) {
            final double msgRateTotal = _messageRates.getY(0, item).doubleValue()
                                      + _messageRates.getY(1, item).doubleValue()
                                      + _messageRates.getY(2, item).doubleValue();
            if (msgRateTotal == 0.) {
                return null;    // No tooltip if no message.
            }

            final TimePeriodValuesWithHighLowCollection responseTimes = (TimePeriodValuesWithHighLowCollection) dataset;
            final TimePeriod period = responseTimes.getSeries(series).getTimePeriod(item);
            final Date startTime = period.getStart();
            final Date endTime = period.getEnd();

            final String frontLabel = responseTimes.getSeriesKey(0).toString();
            final int frontMax = responseTimes.getEndY(0, item).intValue();
            final int frontAvg = responseTimes.getY(0, item).intValue();
            final int frontMin = responseTimes.getStartY(0, item).intValue();

            final String backLabel = responseTimes.getSeriesKey(1).toString();
            final int backMax = responseTimes.getEndY(1, item).intValue();
            final int backAvg = responseTimes.getY(1, item).intValue();
            final int backMin = responseTimes.getStartY(1, item).intValue();

            return FMT.format(new Object[]{
                    startTime, endTime,
                    frontLabel, new Integer(frontMax), new Integer(frontAvg), new Integer(frontMin),
                    backLabel, new Integer(backMax), new Integer(backAvg), new Integer(backMin)});
        }
    }

    /** A tool tip generator for the alert indicator plot. */
    private static class AlertIndicatorToolTipGenerator implements XYToolTipGenerator {
        private static final MessageFormat FMT = new MessageFormat(_resources.getString("alertIndicatorTooltipFormat"));

        public String generateToolTip(XYDataset dataset, int series, int item) {
            final TimeTableXYDataset messageRates = (TimeTableXYDataset) dataset;
            final TimePeriod period = messageRates.getTimePeriod(item);
            final Date startTime = period.getStart();
            final Date endTime = period.getEnd();

            final String seriesLabel = dataset.getSeriesKey(series).toString();
            final double msgRate = messageRates.getY(series, item).doubleValue();
            final int numMsg = (int)Math.round(msgRate * (endTime.getTime() - startTime.getTime()) / 1000.);

            return FMT.format(new Object[] {startTime, endTime, seriesLabel, new Integer(numMsg), new Double(msgRate)});
        }
    }

    /** A tool tip generator for the message rate plot. */
    private static class MessageRateToolTipGenerator implements XYToolTipGenerator {
        private static final MessageFormat FMT = new MessageFormat(_resources.getString("messageRateTooltipFormat"));

        public String generateToolTip(XYDataset dataset, int series, int item) {
            final TimeTableXYDataset messageRates = (TimeTableXYDataset) dataset;
            final TimePeriod period = messageRates.getTimePeriod(item);
            final Date startTime = period.getStart();
            final Date endTime = period.getEnd();

            final String successLabel = dataset.getSeriesKey(0).toString();
            final double successRate = messageRates.getY(0, item).doubleValue();
            final int numSuccess = (int)Math.round(successRate * (endTime.getTime() - startTime.getTime()) / 1000.);

            final String violationLabel = dataset.getSeriesKey(1).toString();
            final double violationRate = messageRates.getY(1, item).doubleValue();
            final int numViolation = (int)Math.round(violationRate * (endTime.getTime() - startTime.getTime()) / 1000.);

            final String failureLabel = dataset.getSeriesKey(2).toString();
            final double failureRate = messageRates.getY(2, item).doubleValue();
            final int numFailure = (int)Math.round(failureRate * (endTime.getTime() - startTime.getTime()) / 1000.);

            if ((failureRate + violationRate + successRate) == 0.) {
                return null;    // No tooltip if no message.
            }

            return FMT.format(new Object[]{startTime, endTime,
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
            final long now = System.currentTimeMillis();
            _chart.getXYPlot().getDomainAxis().setRange(new Range(now - _maxTimeRange, now), false, true);
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
     * @param resolution    the metric bin resolution ({@link com.l7tech.service.MetricsBin#RES_FINE},
     *                      {@link com.l7tech.service.MetricsBin#RES_HOURLY} or
     *                      {@link com.l7tech.service.MetricsBin#RES_DAILY})
     * @param binInterval   nominal bin interval (in milliseconds)
     * @param maxTimeRange  maximum time range of data to keep around
     * @param dashboardWindow   the Dashboard window containing this panel
     */
    public MetricsChartPanel(int resolution,
                             long binInterval,
                             long maxTimeRange,
                             DashboardWindow dashboardWindow) {
        super(null);
        _resolution = resolution;
        _binInterval = binInterval;
        _maxTimeRange = maxTimeRange;
        _dashboardWindow = dashboardWindow;

        // Creates the empty data structures.
        _frontendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_FRONTEND_RESPONSE);
        _backendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_BACKEND_RESPONSE);
        _responseTimes = new TimePeriodValuesWithHighLowCollection();
        _responseTimes.addSeries(_frontendResponseTimes);
        _responseTimes.addSeries(_backendResponseTimes);
        _messageRates = new TimeTableXYDataset();

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
        responsesRenderer.setBaseToolTipGenerator(new ResponseTimeToolTipGenerator(_messageRates));
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
             * autozoom. We need to do this because although {@link #setRange}
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
        final AlertIndicatorToolTipGenerator alertIndicatorToolTipGenerator = new AlertIndicatorToolTipGenerator();
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
        final MessageRateToolTipGenerator messageRateToolTipGenerator = new MessageRateToolTipGenerator();
        ratesRenderer.setBaseToolTipGenerator(messageRateToolTipGenerator);
        final XYPlot ratesPlot = new XYPlot(_messageRates, null, _ratesYAxis, ratesRenderer);

        //
        // Now combine all plots to share the same time (x-)axis.
        //

        // Gets the time zone to use from gateway.
        try {
            final Registry registry = Registry.getDefault();
            if (registry.isAdminContextPresent()) {
                _timeZone = TimeZone.getTimeZone(registry.getClusterStatusAdmin().getCurrentClusterTimeZone());
            }
        } catch (RemoteException e) {
            // Falls through to use local time zone.
        }
        if (_timeZone == null) {
            _logger.warning("Failed to get time zone from gateway. Falling back to use local time zone for display.");
            _timeZone = TimeZone.getDefault();
        }

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

        // Adds custom pop-up context menu; replacing the default inherited from {@link ChartPanel}.
        _showAuditsItem = new JMenuItem();  // Name to be customized based on time period selected.
        _showAuditsItem.setActionCommand(SHOW_AUDITS_COMMAND);
        _showAuditsItem.addActionListener(this);
        _popup = new JPopupMenu();
        _popup.add(_showAuditsItem);
        setPopupMenu(_popup);

        setXAxisRangeIfNoData();
    }

    /**
     * Stores away metrics bins waiting to be added (when chart data updating is suspended).
     * @param bins   metrics bins waiting to be added
     */
    private void storeBinsToAdd(List<MetricsBin> bins) {
        _binsToAdd.addAll(bins);

        // Limits the memory used by bins waiting to be added; by removing bins
        // older than our maximum allowed time range.
        if (! _binsToAdd.isEmpty()) {
            final MetricsBin oldestBin = (MetricsBin)_binsToAdd.last();
            final long lowerBound = oldestBin.getPeriodEnd() - _maxTimeRange;

            for (Iterator i = _binsToAdd.iterator(); i.hasNext();) {
                final MetricsBin bin = (MetricsBin)i.next();
                if (bin.getPeriodStart() < lowerBound) {
                    i.remove();
                } else {
                    break;  // The rest are within maximum allowed time range.
                }
            }
        }
    }

    /**
     * Adds metric bins to the dataset and update the plots.
     *
     * @param bins new data to be added
     */
    public synchronized void addData(List<MetricsBin> bins) {
        if (_updateSuspended) {
            storeBinsToAdd(bins);
            return;
        }

        // Temporarily disable notification so plots won't be redrawn needlessly
        // when datasets are changing. Note that this does not entirely prevent
        // the datasets from being accessed.
        _chart.setNotify(false);

        // Adds new data from the MetricsBin's to our JFreeChart data structures.
        Iterator itor = bins.iterator();
        while (itor.hasNext()) {
            final MetricsBin bin = (MetricsBin) itor.next();

            // We are using the bin's nominal start and end times instead of
            // actual times, in order to avoid unsightly gaps in the bar charts.
            final SimpleTimePeriod period = new SimpleTimePeriod(bin.getPeriodStart(), bin.getPeriodStart() + bin.getInterval());

            _frontendResponseTimes.add(period,
                    bin.getAverageFrontendResponseTime(),
                    bin.getMaxFrontendResponseTime(),
                    bin.getMinFrontendResponseTime());
            _backendResponseTimes.add(period,
                    bin.getAverageBackendResponseTime(),
                    bin.getMaxBackendResponseTime(),
                    bin.getMinBackendResponseTime());

            // Since we are using nominal start and end times, we have to use
            // message rates calculated with nominal time interval to avoid
            // display discrepancy.
            final double successRate = bin.getNominalCompletedRate();
            final double policyViolationRate = bin.getNominalAttemptedRate() - bin.getNominalAuthorizedRate();
            final double routingFailureRate = bin.getNominalAuthorizedRate() - bin.getNominalCompletedRate();
            _messageRates.add(period, new Double(successRate), SERIES_SUCCESS, false);
            _messageRates.add(period, new Double(policyViolationRate), SERIES_POLICY_VIOLATION, false);
            _messageRates.add(period, new Double(routingFailureRate), SERIES_ROUTING_FAILURE, false);
        }

        // Now that the overall time range has change, remove data older than
        // our maximum allowed time range.
        final long newUpperBound = (long) _responseTimes.getDomainUpperBound(true);
        final long newLowerBound = newUpperBound - _maxTimeRange;

        int deleteStart = -1;
        int deleteEnd = -1;
        for (int i = 0; i < _frontendResponseTimes.getItemCount(); ++ i) {
            if (_frontendResponseTimes.getTimePeriod(i).getStart().getTime() >= newLowerBound)
                break;
            if (deleteStart == -1) deleteStart = i;
            deleteEnd = i;
        }
        if (deleteStart != -1) {
            _frontendResponseTimes.delete(deleteStart, deleteEnd);
            _backendResponseTimes.delete(deleteStart, deleteEnd);
        }

        for (int item = _messageRates.getItemCount() - 1; item >= 0; -- item) {
            final TimePeriod period = _messageRates.getTimePeriod(item);
            if (period.getStart().getTime() < newLowerBound) {
                _messageRates.remove(period, SERIES_SUCCESS, false);
                _messageRates.remove(period, SERIES_POLICY_VIOLATION, false);
                _messageRates.remove(period, SERIES_ROUTING_FAILURE, false);
            }
        }

        // Appends current effective time zone to time axis label, e.g., PST vs PDT.
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
    }

    /** Clears all data and updates the plots. */
    public synchronized void clearData() {
        // Temporarily disable notification so plots won't be redrawn needlessly
        // when datasets are changing. Note that this does not entirely prevent
        // the datasets from being accessed.
        _chart.setNotify(false);

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
    }

    /**
     * Overrides superclass method to handle our custom pop-up context menu.
     */
    protected synchronized void displayPopupMenu(int x, int y) {
        // Converts the pixel x-coordinate to time value.
        final Rectangle2D rectangle2d = getChartRenderingInfo().getPlotInfo().getDataArea();
        final long t = (long)_xAxis.java2DToValue(x, rectangle2d, _combinedPlot.getDomainAxisEdge());

        // Determines the metrics bin period containing that time value.
        _showAuditsStartDate = new Date(MetricsBin.periodStartFor(_resolution, (int)_binInterval, t));
        _showAuditsEndDate = new Date(MetricsBin.periodEndFor(_resolution, (int)_binInterval, t));

        // Customizes the name of the show audits menu item.
        _showAuditsItem.setText(SHOW_AUDITS_ITEM_NAME + " (" + timeRangeAsString(_showAuditsStartDate, _showAuditsEndDate, _timeZone) + ")");

        // Paints vertical marker strip in selected time period.
        IntervalMarker marker = new IntervalMarker(_showAuditsStartDate.getTime(),
                                                   _showAuditsEndDate.getTime(),
                                                   new Color(255, 0, 255, 64),
                                                   new BasicStroke(0.0f),
                                                   new Color(255, 0, 255, 0),
                                                   new BasicStroke(0.0f),
                                                   0.0f);
        // noinspection unchecked
        for (XYPlot plot : (List<XYPlot>)_combinedPlot.getSubplots()) {
            plot.clearDomainMarkers();  // Clears previous selection, if any.
            plot.addDomainMarker(marker, Layer.FOREGROUND);
        }

        _popup.show(this, x, y);
    }

    /** Overrides superclass method to handle our custom pop-up context menu items. */
    public void actionPerformed(ActionEvent event) {
        final String command = event.getActionCommand();
        if (command.equals(SHOW_AUDITS_COMMAND)) {
            showAudits(_showAuditsStartDate, _showAuditsEndDate);
        } else {
            throw new RuntimeException("Missing hanlder for context menu item: " + command);
        }
    }

    /**
     * Brings up a window to show audits for the selected time period.
     *
     * @param startDate     start time of audit records
     * @param endDate       end time of audit records
     */
    private void showAudits(Date startDate, Date endDate) {
        String errDialogMsg = null;

        final Registry registry = Registry.getDefault();
        if (registry.isAdminContextPresent()) {
            final AuditAdmin auditAdmin = registry.getAuditAdmin();
            final AuditSearchCriteria criteria = new AuditSearchCriteria(
                    startDate,
                    endDate,
                    null,                   // fromLevel
                    null,                   // toLevel
                    null,                   // recordClass
                    null,                   // nodeId
                    0L,                     // startMessageNumber,
                    0L,                     // endMessageNumber,
                    0                       // maxRecords
            );
            try {
                final Collection<AuditRecord> records = auditAdmin.find(criteria);

                if (_gatewayAuditWindow == null) {
                    _gatewayAuditWindow = new GatewayAuditWindow(false);
                    Utilities.centerOnScreen(_gatewayAuditWindow);
                }

                _gatewayAuditWindow.setVisible(true);
                _gatewayAuditWindow.setExtendedState(Frame.NORMAL);
                _gatewayAuditWindow.toFront();
                _gatewayAuditWindow.setTitle(GATEWAY_AUDIT_WINDOW_TITLE + " (" +
                        timeRangeAsString(_showAuditsStartDate, _showAuditsEndDate, _timeZone) + ")");

                final ClusterNodeInfo nodeSelected = _dashboardWindow.getClusterNodeSelected();
                _gatewayAuditWindow.getLogPane().setMsgFilterNode(nodeSelected == null ? "" : nodeSelected.getName());

                final EntityHeader serviceSelected = _dashboardWindow.getPublishedServiceSelected();
                _gatewayAuditWindow.getLogPane().setMsgFilterService(serviceSelected == null ? "" : serviceSelected.getName());

                _gatewayAuditWindow.displayAudits(records);
            } catch (RemoteException e) {
                _logger.warning("Failed to query for audit events: " + e.getMessage());
                errDialogMsg = e.getMessage();
            } catch (FindException e) {
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
            addData(Arrays.asList(_binsToAdd.toArray(new MetricsBin[0])));
            _binsToAdd.clear();
        }
    }

    public void mousePressed(MouseEvent e) {
        // The user is starting to drag-draw the rubberband zoom box. Need to
        // temporarily suspend updating the chart data, otherwise the rubberband
        // zoom box will appear jumpy.
        // @see http://www.jfree.org/phpBB2/viewtopic.php?t=10022&highlight=zoom+dynamic
        suspendUpdate();
        super.mousePressed(e);
    }

    public void mouseReleased(MouseEvent e) {
        // The user has finished zooming.
        super.mouseReleased(e);

        // Resumes chart data updating only if not zoomed in.
        if (_xAxis.isAutoRange()) {
            resumeUpdate();
        }
    }

    public void restoreAutoRange() {
        _xAxis.setAutoRange(true);
        _responsesYAxis.setAutoRange(true);
        _ratesYAxis.setAutoRange(true);
    }
}
