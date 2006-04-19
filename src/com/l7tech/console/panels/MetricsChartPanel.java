package com.l7tech.console.panels;

import com.l7tech.console.util.jfree.*;
import com.l7tech.service.MetricsBin;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.InvalidObjectException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

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
    private static final Color POLICY_VIOLATION_COLOR = new Color(255, 255, 0);

    /** Color for routing failure messages stack bars and alert indicators. */
    private static final Color ROUTING_FAILURE_COLOR = new Color(255, 0, 0);

    /**
     * Background color of the indicator plot. Chosen to enhance visibility of
     * the alert indicator colors ({@link POLICY_VIOLATION_COLOR} and
     * {@link ROUTING_FAILURE_COLOR}).
     */
    private static final Color INDICATOR_PLOT_BACKCOLOR = new Color(128, 128, 128);

    /** Shape for alert indicators (routing failure and policy violation). */
    private static final Rectangle2D.Double INDICATOR_SHAPE = new Rectangle2D.Double(-3., -3., 6., 6.);

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

    /** Nominal bin interval (in milliseconds). */
    private final long _binInterval;

    /** Maximum time range of data to keep around (in milliseconds). */
    private long _maxTimeRange;

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
    private final DateAxis _xAxis;

    /** Chart containing all plots with shared time axis. */
    private JFreeChart _chart;

    /** Holding area for metrics bins waiting to be added; when {@link _suspended} is true. */
    private final SortedSet _binsToAdd = new TreeSet();

    /** Indicates if chart data updating is suspended. */
    private boolean _suspended = false;

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
     */
    private static Color GetAlphaEquiv(int r0, int g0, int b0, int a) {
        final int r = Math.round(Math.max(0.f, (r0 + a - 255) * 255.f / a));
        final int g = Math.round(Math.max(0.f, (g0 + a - 255) * 255.f / a));
        final int b = Math.round(Math.max(0.f, (b0 + a - 255) * 255.f / a));
        return new Color(r, g, b, a);
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
     * @param binInterval   nominal bin interval (in milliseconds)
     * @param maxTimeRange  maximum time range of data to keep around
     */
    public MetricsChartPanel(long binInterval, long maxTimeRange) {
        super(null);
        _binInterval = binInterval;
        _maxTimeRange = maxTimeRange;

        // Creates the empty data structures.
        _frontendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_FRONTEND_RESPONSE);
        _backendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_BACKEND_RESPONSE);
        _responseTimes = new TimePeriodValuesWithHighLowCollection();
        _responseTimes.addSeries(_frontendResponseTimes);
        _responseTimes.addSeries(_backendResponseTimes);
        _messageRates = new TimeTableXYDataset();

        //
        // Top plot for response time.
        //

        final NumberAxis rYAxis = new NumberAxis(RESPONSE_TIME_AXIS_LABEL);
        rYAxis.setAutoRange(true);
        rYAxis.setRangeType(RangeType.POSITIVE);
        rYAxis.setAutoRangeMinimumSize(10.);
        final TimePeriodValueWithHighLowRenderer rRenderer = new TimePeriodValueWithHighLowRenderer();
        rRenderer.setDrawBarOutline(false);
        rRenderer.setSeriesStroke(0, RESPONSE_AVG_STROKE);
        rRenderer.setSeriesPaint(0, FRONTEND_RESPONSE_AVG_COLOR);
        rRenderer.setSeriesFillPaint(0, FRONTEND_RESPONSE_MINMAX_COLOR);
        rRenderer.setSeriesStroke(1, RESPONSE_AVG_STROKE);
        rRenderer.setSeriesPaint(1, BACKEND_RESPONSE_AVG_COLOR);
        rRenderer.setSeriesFillPaint(1, BACKEND_RESPONSE_MINMAX_COLOR);
        rRenderer.setBaseToolTipGenerator(new ResponseTimeToolTipGenerator(_messageRates));
        final XYPlot rPlot = new XYPlot(_responseTimes, null, rYAxis, rRenderer);
        rPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        //
        // Middle plot for alert indicators of policy violations and routing failures.
        // This augments the message rate plot below since small values there
        // may shows up too small to see.
        //

        final NumberAxis iYAxis = new NumberAxis() {
            /**
             * Overrides parent method to prevent any zooming, particularly
             * autozoom. We need to do this because although {@link #setRange}
             * will unset autozoom, its effect is not persistent.
             */
            public void resizeRange(double percent, double anchorValue) {
                // Do nothing.
            }
        };
        iYAxis.setRange(0., 10.);   // This y-range and the forced y values
                                    // below are chosen to space out the
                                    // alert indicator shapes evenly.
        iYAxis.setTickLabelsVisible(false);
        iYAxis.setTickMarksVisible(false);
        final ReplaceYShapeRenderer iRenderer = new ReplaceYShapeRenderer();
        iRenderer.setSeriesVisible(0, Boolean.FALSE);   // success message rate
        iRenderer.setSeriesYValue(1, 3.);               // policy violation message rate
        iRenderer.setSeriesYValue(2, 7.);               // routing failure message rate
        iRenderer.setSeriesPaint(1, POLICY_VIOLATION_COLOR);
        iRenderer.setSeriesPaint(2, ROUTING_FAILURE_COLOR);
        iRenderer.setSeriesShape(1, INDICATOR_SHAPE);
        iRenderer.setSeriesShape(2, INDICATOR_SHAPE);
        final AlertIndicatorToolTipGenerator alertIndicatorToolTipGenerator = new AlertIndicatorToolTipGenerator();
        iRenderer.setBaseToolTipGenerator(alertIndicatorToolTipGenerator);
        final XYPlot iPlot = new XYPlot(_messageRates, null, iYAxis, iRenderer);
        iPlot.setBackgroundPaint(INDICATOR_PLOT_BACKCOLOR);
        iPlot.setRangeGridlinesVisible(false);
        iPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        //
        // Bottom plot for message rate.
        //

        final NumberAxis mYAxis = new NumberAxis(MESSAGE_RATE_AXIS_LABEL);
        mYAxis.setAutoRange(true);
        mYAxis.setRangeType(RangeType.POSITIVE);
        mYAxis.setAutoRangeMinimumSize(0.0001);     // Still allows 1 msg per day to be visible.
        final StackedXYBarRenderer mRenderer = new StackedXYBarRendererEx();
        mRenderer.setDrawBarOutline(false);
        mRenderer.setSeriesPaint(0, SUCCESS_COLOR);         // success message rate
        mRenderer.setSeriesPaint(1, POLICY_VIOLATION_COLOR);// policy violation message rate
        mRenderer.setSeriesPaint(2, ROUTING_FAILURE_COLOR); // routing failure message rate
        final MessageRateToolTipGenerator messageRateToolTipGenerator = new MessageRateToolTipGenerator();
        mRenderer.setBaseToolTipGenerator(messageRateToolTipGenerator);
        final XYPlot mPlot = new XYPlot(_messageRates, null, mYAxis, mRenderer);

        //
        // Now combine all plots to share the same time (x-)axis.
        //

        _xAxis = new DateAxis(null) {
            public void setRange(Range range, boolean turnOffAutoRange, boolean notify) {
                // Do not zoom in any smaller than the nominal bin interval.
                if ((range.getUpperBound() - range.getLowerBound()) >= _binInterval)
                    super.setRange(range, turnOffAutoRange, notify);
            }
        };
        _xAxis.setAutoRange(true);
        _xAxis.setFixedAutoRange(_maxTimeRange);
        final CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(_xAxis);
        combinedPlot.setGap(0.);
        combinedPlot.add(rPlot, 35);
        combinedPlot.add(iPlot, 5);
        combinedPlot.add(mPlot, 60);
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);

        _chart = new JFreeChart(null,   // chart title
                null,                   // title font
                combinedPlot,
                false                   // generate legend?
        );
        _chart.setAntiAlias(false);

        setChart(_chart);
        setPopupMenu(null);             // Suppresses right-click pop menu.
        setRangeZoomable(false);        // Suppresses range (y-axis) zooming.
        setFillZoomRectangle(true);
        setInitialDelay(100);           // Makes tool tip respond fast.
        setDismissDelay(Integer.MAX_VALUE); // Makes tool tip display indefinitely.
        setReshowDelay(100);

        setXAxisRangeIfNoData();
    }

    /** Stores away metrics bins waiting to be added (when chart data updating is suspended). */
    private void storeBinsToAdd(List metricsBins) {
        _binsToAdd.addAll(metricsBins);

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
     * @param metricsBins new data to be added
     */
    public synchronized void addData(List metricsBins) {
        if (_suspended) {
            storeBinsToAdd(metricsBins);
            return;
        }

        // Temporarily disable notification so plots won't be redrawn needlessly
        // when datasets are changing. Note that this does not entirely prevent
        // the datasets from being accessed.
        _chart.setNotify(false);

        // Adds new data from the MetricsBin's to our JFreeChart data structures.
        Iterator itor = metricsBins.iterator();
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
        final long newLowerBound = (long) _responseTimes.getDomainUpperBound(true) - _maxTimeRange;

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

        setXAxisRangeIfNoData();
    }

    /** Suspends updating of displayed chart data. */
    private synchronized void suspend() {
        _suspended = true;
    }

    /** Resumes updating of displayed chart data. */
    private synchronized void resume() {
        _suspended = false;

        // Now adds bins waiting to be added.
        if (! _binsToAdd.isEmpty()) {
            addData(Arrays.asList(_binsToAdd.toArray()));
            _binsToAdd.clear();
        }
    }

    public void mousePressed(MouseEvent e) {
        // The user is starting to drag-draw the rubberband zoom box. Need to
        // temporarily suspend updating the chart data, otherwise the rubberband
        // zoom box will appear jumpy.
        // @see http://www.jfree.org/phpBB2/viewtopic.php?t=10022&highlight=zoom+dynamic
        suspend();
        super.mousePressed(e);
    }

    public void mouseReleased(MouseEvent e) {
        // The user has finished zooming.
        super.mouseReleased(e);

        // Resumes chart data updating only if not zoomed in.
        if (_xAxis.isAutoRange()) {
            resume();
        }
    }
}
