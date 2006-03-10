package com.l7tech.console.panels;

import com.l7tech.console.util.jfree.ReplaceYShapeRenderer;
import com.l7tech.console.util.jfree.TimePeriodValueWithHighLowRenderer;
import com.l7tech.console.util.jfree.TimePeriodValuesWithHighLow;
import com.l7tech.console.util.jfree.TimePeriodValuesWithHighLowCollection;
import com.l7tech.service.MetricsBin;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.InvalidObjectException;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.text.FieldPosition;
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
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.MetricsChartPanel");

    /** Color for the horizontal line representing average frontend response times. */
    public static final Color FRONTEND_RESPONSE_AVG_COLOR = new Color(95, 83, 173);

    /** Color for the high-low bars representing min-max frontend response times. */
    public static final Color FRONTEND_RESPONSE_MINMAX_COLOR = new Color(186, 189, 255, 128);

    /** Color for the horizontal line representing average backend response times. */
    public static final Color BACKEND_RESPONSE_AVG_COLOR = new Color(49, 53, 17);

    /** Color for the high-low bars representing min-max backend response times. */
    public static final Color BACKEND_RESPONSE_MINMAX_COLOR = new Color(217, 225, 78, 128);

    /** Stroke for the horizontal line representing average response times. */
    public static final BasicStroke RESPONSE_AVG_STROKE = new BasicStroke(2.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    /** Color for success messages stack bars and alert indicators. */
    public static final Color SUCCESS_COLOR = new Color(0, 120, 0);

    /** Color for policy violation messages stack bars and alert indicators. */
    public static final Color POLICY_VIOLATION_COLOR = new Color(255, 255, 0);

    /** Color for routing failure messages stack bars and alert indicators. */
    public static final Color ROUTING_FAILURE_COLOR = new Color(255, 0, 0);

    /**
     * Background color of the indicator plot. Chosen to enhance visibility of
     * the alert indicator colors ({@link POLICY_VIOLATION_COLOR} and
     * {@link ROUTING_FAILURE_COLOR}).
     */
    private static final Color INDICATOR_PLOT_BACKCOLOR = new Color(128, 128, 128);

    /** Shape for alert indicators (routing failure and policy violation). */
    private static final Rectangle2D.Double INDICATOR_SHAPE = new Rectangle2D.Double(-3., -3., 6., 6.);

    /**
     * Name of series in {@link #_messageRates} and {@link #_messageCounts} that
     * contains success message rate and counts.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_SUCCESS = resources.getString("seriesSuccess");

    /**
     * Name of series in {@link #_messageRates} and {@link #_messageCounts} that
     * contains policy violation message rate and counts.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_POLICY_VIOLATION = resources.getString("seriesPolicyViolation");

    /**
     * Name of series in {@link #_messageRates} and {@link #_messageCounts} that
     * contains routing failure message rate and counts.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_ROUTING_FAILURE = resources.getString("seriesRoutingFailure");

    /**
     * Name of the series {@link #_frontendResponseTimes} that contains frontend
     * response times.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_FRONTEND_RESPONSE = resources.getString("seriesFrontend");

    /**
     * Name of the series {@link #_backendResponseTimes} that contains backend
     * response times.
     * This text string will be displayed in tooltips.
     */
    private static final String SERIES_BACKEND_RESPONSE = resources.getString("seriesBackend");

    private static final MessageFormat tooltipFormat = new MessageFormat(resources.getString("tooltipFormat"));

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

    /**
     * Data structure containing message count data.
     * The member series corresponds to those of {@link _messageRates}.
     * Used for tool tip generation only (not for plotting).
     *
     * Note: We cannot combine {@link _messageRates} and {@link _messageCounts}
     * into one data structure because {@link StackedXYBarRenderer} does not
     * honour setSeriesVisible(false).
     */
    private final TimeTableXYDataset _messageCounts;

    /** Chart containing all plots with shared time axis. */
    private JFreeChart _chart;

    /** A tool tip generator for the response time plot. */
    private static class ResponseTimeToolTipGenerator implements XYToolTipGenerator {
        private static final SimpleDateFormat fmt = new SimpleDateFormat(/*"HH:mm:ss"*/);

        public String generateToolTip(XYDataset dataset, int series, int item) {
            final TimePeriodValuesWithHighLowCollection dataset_ = (TimePeriodValuesWithHighLowCollection) dataset;
            final TimePeriod period = dataset_.getSeries(series).getTimePeriod(item);
            final Date startTime = period.getStart();
            final Date endTime = period.getEnd();
            final int avg = dataset_.getY(series, item).intValue();
            final int min = dataset_.getStartY(series, item).intValue();
            final int max = dataset_.getEndY(series, item).intValue();
            final String seriesLabel = dataset_.getSeriesKey(series).toString();
            StringBuffer tooltip = new StringBuffer();
            tooltipFormat.format(
                    new Object[] {
                        seriesLabel,
                        new Integer(avg),
                        new Integer(min),
                        new Integer(max),
                        fmt.format(startTime),
                        fmt.format(endTime)
                    }, tooltip, new FieldPosition(0));
            return tooltip.toString();
/*
            return seriesLabel + ": avg=" + avg + " min=" + min + " max=" + max +
                    " (from " + fmt.format(startTime) + " to " + fmt.format(endTime) + ")";
*/
        }
    }

    /** A tool tip generator for the alert indicator plot and the message rate plot. */
    private static class MessageRateToolTipGenerator implements XYToolTipGenerator {
        private static final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

        private final TimeTableXYDataset _messageCounts;

        public MessageRateToolTipGenerator(TimeTableXYDataset messageCounts) {
            _messageCounts = messageCounts;
        }

        public String generateToolTip(XYDataset dataset, int series, int item) {
            final TimePeriod period = ((TimeTableXYDataset) dataset).getTimePeriod(item);
            final Date startTime = period.getStart();
            final Date endTime = period.getEnd();
            final double numSec = (endTime.getTime() - startTime.getTime()) / 1000.;

            // The series being plotted contains message rates. But in the
            // tool tip we want to show the number of messages in the bin.
            // This cannot be computed from rate and period because the period
            // in the dataset covers the interval boundary, not the actual start
            // end time.
            final int numMsg = _messageCounts.getY(series, item).intValue();

            final String seriesLabel = dataset.getSeriesKey(series).toString();
            return seriesLabel + ": " + numMsg + " msg (in " + numSec + " sec from "
                   + fmt.format(startTime) + " to " + fmt.format(endTime) + ")";
        }
    }

    /** @param maxTimeRange maximum time range of data to keep around */
    public MetricsChartPanel(long maxTimeRange) {
        super(null);
        _maxTimeRange = maxTimeRange;

        // Creates the empty data structures.
        _frontendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_FRONTEND_RESPONSE);
        _backendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_BACKEND_RESPONSE);
        _responseTimes = new TimePeriodValuesWithHighLowCollection();
        _responseTimes.addSeries(_frontendResponseTimes);
        _responseTimes.addSeries(_backendResponseTimes);
        _messageRates = new TimeTableXYDataset();
        _messageCounts = new TimeTableXYDataset();

        //
        // Top plot for response time.
        //

        final NumberAxis rYAxis = new NumberAxis("Response Time (ms)");
        rYAxis.setAutoRange(true);
        final TimePeriodValueWithHighLowRenderer rRenderer = new TimePeriodValueWithHighLowRenderer();
        rRenderer.setDrawBarOutline(false);
        rRenderer.setSeriesStroke(0, RESPONSE_AVG_STROKE);
        rRenderer.setSeriesPaint(0, FRONTEND_RESPONSE_AVG_COLOR);
        rRenderer.setSeriesFillPaint(0, FRONTEND_RESPONSE_MINMAX_COLOR);
        rRenderer.setSeriesStroke(1, RESPONSE_AVG_STROKE);
        rRenderer.setSeriesPaint(1, BACKEND_RESPONSE_AVG_COLOR);
        rRenderer.setSeriesFillPaint(1, BACKEND_RESPONSE_MINMAX_COLOR);
        rRenderer.setBaseToolTipGenerator(new ResponseTimeToolTipGenerator());
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
        final MessageRateToolTipGenerator messageRateToolTipGenerator = new MessageRateToolTipGenerator(_messageCounts);
        iRenderer.setBaseToolTipGenerator(messageRateToolTipGenerator);
        final XYPlot iPlot = new XYPlot(_messageRates, null, iYAxis, iRenderer);
        iPlot.setBackgroundPaint(INDICATOR_PLOT_BACKCOLOR);
        iPlot.setRangeGridlinesVisible(false);
        iPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        //
        // Bottom plot for message rate.
        //

        final NumberAxis mYAxis = new NumberAxis("Message Rate (per sec)");
        mYAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        mYAxis.setAutoRange(true);
        final StackedXYBarRenderer mRenderer = new StackedXYBarRenderer() {
            public void drawItem(Graphics2D g2,
                                 XYItemRendererState state,
                                 Rectangle2D dataArea,
                                 PlotRenderingInfo info,
                                 XYPlot plot,
                                 ValueAxis domainAxis,
                                 ValueAxis rangeAxis,
                                 XYDataset dataset,
                                 int series,
                                 int item,
                                 CrosshairState crosshairState,
                                 int pass) {
                try {
                    super.drawItem(g2, state, dataArea, info, plot, domainAxis,
                                   rangeAxis, dataset, series, item, crosshairState, pass);
                } catch (IndexOutOfBoundsException e) {
                    // Probably the data item has just been deleted. Just skip rendering it.
                }
            }

        };
        mRenderer.setDrawBarOutline(false);
        mRenderer.setSeriesPaint(0, SUCCESS_COLOR);         // success message rate
        mRenderer.setSeriesPaint(1, POLICY_VIOLATION_COLOR);// policy violation message rate
        mRenderer.setSeriesPaint(2, ROUTING_FAILURE_COLOR); // routing failure message rate
        mRenderer.setBaseToolTipGenerator(messageRateToolTipGenerator);
        final XYPlot mPlot = new XYPlot(_messageRates, null, mYAxis, mRenderer);

        //
        // Now combine all plots to share the same time (x-)axis.
        //

        final DateAxis xAxis = new DateAxis(null);
        xAxis.setAutoRange(true);
        xAxis.setFixedAutoRange(_maxTimeRange);
        final CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(xAxis);
        combinedPlot.setGap(0.);
        combinedPlot.add(rPlot, 35);    // These weights are tweaked to match
        combinedPlot.add(iPlot, 5);     // the matching numeric display panel
        combinedPlot.add(mPlot, 60);    // next to the corresponding plots.
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
        setInitialDelay(100);           // Makes tool tip respond fast.
        setDismissDelay(Integer.MAX_VALUE); // Makes tool tip display indefinitely.
        setReshowDelay(100);
    }

    /**
     * Adds metric bins to the dataset and update the plots.
     *
     * @param metricsBins new data to be added
     */
    public void addData(List metricsBins) {
        // Temporarily disable notification so plots won't be redrawn when
        // datasets are changing. Note that this does not entirely prevent
        // the datasets from being accessed.
        _chart.setNotify(false);

        // Adds new data from the MetricsBin's to our JFreeChart data structures.
        Iterator itor = metricsBins.iterator();
        while (itor.hasNext()) {
            final MetricsBin bin = (MetricsBin) itor.next();

            // We are using the bin's boundary times instead of actual start and
            // end times, in order to avoid unsightly gaps in the bar charts.
            final SimpleTimePeriod period = new SimpleTimePeriod(bin.getPeriodStart(), bin.getPeriodStart() + bin.getInterval());

            _frontendResponseTimes.add(period,
                    bin.getAverageFrontendResponseTime(),
                    bin.getMaxFrontendResponseTime(),
                    bin.getMinFrontendResponseTime());
            _backendResponseTimes.add(period,
                    bin.getAverageBackendResponseTime(),
                    bin.getMaxBackendResponseTime(),
                    bin.getMinBackendResponseTime());

            final double successRate = bin.getCompletedRate();
            final double policyViolationRate = bin.getAttemptedRate() - bin.getAuthorizedRate();
            final double routingFailureRate = bin.getAuthorizedRate() - bin.getCompletedRate();
            _messageRates.add(period, new Double(successRate), SERIES_SUCCESS, false);
            _messageRates.add(period, new Double(policyViolationRate), SERIES_POLICY_VIOLATION, false);
            _messageRates.add(period, new Double(routingFailureRate), SERIES_ROUTING_FAILURE, false);

            final int numSuccess = bin.getNumCompletedRequest();
            final int numPolicyViolation = bin.getNumAttemptedRequest() - bin.getNumAuthorizedRequest();
            final int numRoutingFailure = bin.getNumAuthorizedRequest() - bin.getNumCompletedRequest();
            _messageCounts.add(period, new Integer(numSuccess), SERIES_SUCCESS, false);
            _messageCounts.add(period, new Integer(numPolicyViolation), SERIES_POLICY_VIOLATION, false);
            _messageCounts.add(period, new Integer(numRoutingFailure), SERIES_ROUTING_FAILURE, false);
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
                _messageCounts.remove(period, SERIES_SUCCESS, false);
                _messageCounts.remove(period, SERIES_POLICY_VIOLATION, false);
                _messageCounts.remove(period, SERIES_ROUTING_FAILURE, false);
            }
        }

        // Re-enable notification now that all dataset change is done.
        _chart.setNotify(true);

        // Now sends notifications to cause plots to update.
        try {
            _responseTimes.validateObject();
            _messageRates.validateObject();
            _messageCounts.validateObject();
        } catch (InvalidObjectException e) {
            // Should not get here.
        }
    }

    /** Clears all data and updates the plots. */
    public void clearData() {
        // Temporarily disable notification so plots won't be redrawn when
        // datasets are changing. Note that this does not entirely prevent
        // the datasets from being accessed.
        _chart.setNotify(false);

        _frontendResponseTimes.delete(0, _frontendResponseTimes.getItemCount() - 1);
        _backendResponseTimes.delete(0, _backendResponseTimes.getItemCount() - 1);

        for (int item = _messageRates.getItemCount(0) - 1; item >= 0; -- item) {
            final TimePeriod period = _messageRates.getTimePeriod(item);
            _messageRates.remove(period, SERIES_SUCCESS, false);
            _messageRates.remove(period, SERIES_POLICY_VIOLATION, false);
            _messageRates.remove(period, SERIES_ROUTING_FAILURE, false);
            _messageCounts.remove(period, SERIES_SUCCESS, false);
            _messageCounts.remove(period, SERIES_POLICY_VIOLATION, false);
            _messageCounts.remove(period, SERIES_ROUTING_FAILURE, false);
        }

        // Re-enable notification now that all dataset change is done.
        _chart.setNotify(true);

        // Now sends notifications to cause plots to update.
        try {
            _responseTimes.validateObject();
            _messageRates.validateObject();
            _messageCounts.validateObject();
        } catch (InvalidObjectException e) {
            // Should not get here.
        }
    }

    /** Creates a fake metrics bin. For testing only. */
    private static MetricsBin fakeMetricsBin(long startTime, long endTime, int binInterval) {
        final MetricsBin bin = new MetricsBin(startTime, binInterval, MetricsBin.RES_FINE, "SSG1", 123456);
        final double cosine = Math.cos(2. * Math.PI * startTime / (100. * 5 * 1000));
        final int numSuccess = (int) (1000. + 100. * cosine);
        final int numViolation = (int) (100. + 100. * cosine);
        final int numFailure = (int) (50. + 50. * cosine);
        final int numAttempted = numSuccess + numViolation + numFailure;
        final int numAuthorized = numSuccess + numFailure;
        final int numCompleted = numSuccess;
        final int frontendResponseTimeAvg = (int) (800. + 100. * cosine);
        final int frontendResponseTimeMin = (int) (700. + 100. * cosine);
        final int frontendResponseTimeMax = (int) (900. + 100. * cosine);
        final int backendResponseTimeAvg = (int) (600. - 100. * cosine);
        final int backendResponseTimeMin = (int) (500. - 100. * cosine);
        final int backendResponseTimeMax = (int) (700. - 100. * cosine);
        for (int i = 0; i < numAttempted; ++ i) {
            int responseTime = frontendResponseTimeAvg;
            if (numAttempted == 2) {
                if (i == 0) responseTime = frontendResponseTimeMin;
                if (i == 1) responseTime = frontendResponseTimeMax;
            } else { // numAttempted == 1 || numAttempted >= 3
                if (i == 1) responseTime = frontendResponseTimeMin;
                if (i == 2) responseTime = frontendResponseTimeMax;
            }
            bin.addAttemptedRequest(responseTime);
        }
        for (int i = 0; i < numAuthorized; ++ i) {
            bin.addAuthorizedRequest();
        }
        for (int i = 0; i < numCompleted; ++ i) {
            int responseTime = backendResponseTimeAvg;
            if (numAttempted == 2) {
                if (i == 0) responseTime = backendResponseTimeMin;
                if (i == 1) responseTime = backendResponseTimeMax;
            } else { // numAttempted == 1 || numAttempted >= 3
                if (i == 1) responseTime = backendResponseTimeMin;
                if (i == 2) responseTime = backendResponseTimeMax;
            }
            bin.addCompletedRequest(responseTime);
        }
        bin.setEndTime(endTime); // Closes the bin.
        return bin;
    }

    /** Tests the MetricsChartPanel using fake data. */
    public static void main(String[] args) {
        final int FINE_INTERVAL = 5 * 1000;
        final long MAX_TIME_RANGE = 15 * 60 * 1000;

        final MetricsChartPanel chartPanel = new MetricsChartPanel(MAX_TIME_RANGE);

        final JFrame mainFrame = new JFrame("MetricsChartPanel Test");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.add(chartPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);

        // Waits 3 seconds to simulate network delay to fetch data from SSG.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        // Fakes a bunch of historical fine bins.
        final long dataStart = ((System.currentTimeMillis() - MAX_TIME_RANGE) / FINE_INTERVAL) * FINE_INTERVAL;
        final long dataEnd = dataStart + MAX_TIME_RANGE;
        final List metricsBins = new ArrayList();
        for (long binStart = dataStart, binEnd = dataStart + FINE_INTERVAL;
             binStart < dataEnd;
             binStart += FINE_INTERVAL, binEnd += FINE_INTERVAL) {
            metricsBins.add(fakeMetricsBin(binStart, binEnd, FINE_INTERVAL));
        }

        chartPanel.addData(metricsBins);

        // Simulates new fine bins being added regularly.
        while (true) {
            try {
                Thread.sleep(FINE_INTERVAL);
            } catch (InterruptedException e) {
            }
            final long endTime = System.currentTimeMillis();
            final long startTime = endTime - FINE_INTERVAL;
            final MetricsBin bin = fakeMetricsBin(startTime, endTime, FINE_INTERVAL);
            final List bins = new ArrayList();
            bins.add(bin);
            chartPanel.addData(bins);
        }
    }
}
