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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Chart panel containing plots of metrics bins data. The chart contains 3 plots
 * stacked vertically and shares the same time (x-)axis.
 * <p>
 * Top plot shows frontend and backend response times.
 * Middle plot shows indicators of routing failure and policy violation.
 * Bottom plot shows message rates.
 *
 * @author rmak
 */
public class MetricsChartPanel extends ChartPanel {
    /** Color for success messages stack bars and indicators. */
    public static final Color SUCCESS_COLOR = new Color(0, 120, 0);

    /** Color for policy violation messages stack bars and indicators. */
    public static final Color POLICY_VIOLATION_COLOR = new Color(255, 255, 0);

    /** Color for routing failure messages stack bars and indicators. */
    public static final Color ROUTING_FAILURE_COLOR = new Color(255, 0, 0);

    /**
     * Background color of the indicator plot. Chosen to enhance visibility of
     * the indicator shape colors for policy violation and routing failure.
     */
    private static final Color INDICATOR_PLOT_BACKCOLOR = new Color(128, 128, 128);

    /** Stroke for the horizontal line representing average response times. */
    public static final BasicStroke RESPONSE_AVG_STROKE = new BasicStroke(2.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    /** Color for the horizontal line representing frontend response times. */
    public static final Color FRONTEND_RESPONSE_AVG_COLOR = new Color(95, 83, 173);

    /** Color for the high-low bars representing frontend response times. */
    public static final Color FRONTEND_RESPONSE_MINMAX_COLOR = new Color(186, 189, 255, 128);

    /** Color for the horizontal line representing backend response times. */
    public static final Color BACKEND_RESPONSE_AVG_COLOR = new Color(49, 53, 17);

    /** Color for the high-low bars representing backend response times. */
    public static final Color BACKEND_RESPONSE_MINMAX_COLOR = new Color(217, 225, 78, 128);

    /** Shape for routing failure and policy violation indicators. */
    private static final Rectangle2D.Double INDICATOR_SHAPE = new Rectangle2D.Double(-3., -3., 6., 6.);

    /**
     * Name of the series in {@link #_messageRates} that contains success messages.
     * Will appear in tooltips.
     */
    private static final String SERIES_NAME_SUCCESS = "Success";

    /**
     * Name of the series in {@link #_messageRates} that contains policy violation messages.
     * Will appear in tooltips.
     */
    private static final String SERIES_NAME_POLICY_VIOLATION = "Policy Violation";

    /**
     * Name of the series in {@link #_messageRates} that contains routing failure messages.
     * Will appear in tooltips.
     */
    private static final String SERIES_NAME_ROUTING_FAILURE = "Routing Failure";

    /**
     * Name of the series {@link #_frontendResponseTimes} that contains frontend response times.
     * Will appear in tooltips.
     */
    private static final String SERIES_NAME_FRONTEND_RESPONSE = "Frontend";

    /**
     * Name of the series {@link #_backendResponseTimes} that contains backend response times.
     * Will appear in tooltips.
     */
    private static final String SERIES_NAME_BACKEND_RESPONSE = "Backend";

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
     * The indicator plot and the stack bar plot rely on the series order:
     * Series 0 must be success.
     * Series 1 must be policy violation.
     * Series 2 must be routing failure.
     */
    private final TimeTableXYDataset _messageRates;

    /** Chart containing all plots with shared time axis. */
    private JFreeChart _chart;

    /** A tool tip generator for the response time plot. */
    private static class ResponseTimeToolTipGenerator implements XYToolTipGenerator {
        private static final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

        public String generateToolTip(XYDataset dataset, int series, int item) {
            TimePeriodValuesWithHighLowCollection dataset_ = (TimePeriodValuesWithHighLowCollection) dataset;
            TimePeriod period = dataset_.getSeries(series).getTimePeriod(item);
            Date startTime = period.getStart();
            Date endTime = period.getEnd();
            int avg = dataset_.getY(series, item).intValue();
            int min = dataset_.getStartY(series, item).intValue();
            int max = dataset_.getEndY(series, item).intValue();
            String seriesLabel = dataset_.getSeriesKey(series).toString();
            return seriesLabel + ": avg=" + avg + " min=" + min + " max=" + max +
                    " (from " + fmt.format(startTime) + " to " + fmt.format(endTime) + ")";
        }
    }

    /** A tool tip generator for the indicator plot and the message rate plot. */
    private static class MessageRateToolTipGenerator implements XYToolTipGenerator {
        private static final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

        public String generateToolTip(XYDataset dataset, int series, int item) {
            TimePeriod period = ((TimeTableXYDataset) dataset).getTimePeriod(item);
            Date startTime = period.getStart();
            Date endTime = period.getEnd();
            double numSec = (endTime.getTime() - startTime.getTime()) / 1000.;
            double msgPerSec = dataset.getY(series, item).doubleValue();
            long numMsg = Math.round(msgPerSec * numSec);
            String seriesLabel = dataset.getSeriesKey(series).toString();
            return seriesLabel + ": " + numMsg + " msg (in " + numSec + " sec from "
                   + fmt.format(startTime) + " to " + fmt.format(endTime) + ")";
        }
    }

    /** @param maxTimeRange maximum time range of data to keep around */
    public MetricsChartPanel(long maxTimeRange) {
        super(null);
        _maxTimeRange = maxTimeRange;

        // Creates the empty data structures.
        _frontendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_NAME_FRONTEND_RESPONSE);
        _backendResponseTimes = new TimePeriodValuesWithHighLow(SERIES_NAME_BACKEND_RESPONSE);
        _responseTimes = new TimePeriodValuesWithHighLowCollection();
        _responseTimes.addSeries(_frontendResponseTimes);
        _responseTimes.addSeries(_backendResponseTimes);
        _messageRates = new TimeTableXYDataset();

        //
        // Top plot for response time.
        //

        NumberAxis rYAxis = new NumberAxis("Response Time (ms)");
        rYAxis.setAutoRange(true);
        TimePeriodValueWithHighLowRenderer rRenderer = new TimePeriodValueWithHighLowRenderer();
        rRenderer.setDrawBarOutline(false);
        rRenderer.setSeriesStroke(0, RESPONSE_AVG_STROKE);
        rRenderer.setSeriesPaint(0, FRONTEND_RESPONSE_AVG_COLOR);
        rRenderer.setSeriesFillPaint(0, FRONTEND_RESPONSE_MINMAX_COLOR);
        rRenderer.setSeriesStroke(1, RESPONSE_AVG_STROKE);
        rRenderer.setSeriesPaint(1, BACKEND_RESPONSE_AVG_COLOR);
        rRenderer.setSeriesFillPaint(1, BACKEND_RESPONSE_MINMAX_COLOR);
        rRenderer.setBaseToolTipGenerator(new ResponseTimeToolTipGenerator());
        XYPlot rPlot = new XYPlot(_responseTimes, null, rYAxis, rRenderer);
        rPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        //
        // Middle plot for indicators of policy violations and routing failures.
        // This augments the message rate plot below since small values there
        // may shows up too small to see.
        //

        NumberAxis iYAxis = new NumberAxis() {
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
                                    // indicator shapes evenly.
        iYAxis.setTickLabelsVisible(false);
        iYAxis.setTickMarksVisible(false);
        ReplaceYShapeRenderer iRenderer = new ReplaceYShapeRenderer();
        iRenderer.setSeriesVisible(0, Boolean.FALSE);   // success messages
        iRenderer.setSeriesYValue(1, 3.);               // policy violation messages
        iRenderer.setSeriesYValue(2, 7.);               // routing failure messages
        iRenderer.setSeriesPaint(1, POLICY_VIOLATION_COLOR);
        iRenderer.setSeriesPaint(2, ROUTING_FAILURE_COLOR);
        iRenderer.setSeriesShape(1, INDICATOR_SHAPE);
        iRenderer.setSeriesShape(2, INDICATOR_SHAPE);
        MessageRateToolTipGenerator messageRateToolTipGenerator = new MessageRateToolTipGenerator();
        iRenderer.setBaseToolTipGenerator(messageRateToolTipGenerator);
        XYPlot iPlot = new XYPlot(_messageRates, null, iYAxis, iRenderer);
        iPlot.setBackgroundPaint(INDICATOR_PLOT_BACKCOLOR);
        iPlot.setRangeGridlinesVisible(false);
        iPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        //
        // Bottom plot for message rate.
        //

        NumberAxis mYAxis = new NumberAxis("Message Rate (per sec)");
        mYAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        mYAxis.setAutoRange(true);
        StackedXYBarRenderer mRenderer = new StackedXYBarRenderer() {
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
                // Enclose parent method in try-catch block to prevent
                // IndexOutOfBoundsException from bubbling up. This exception may arise
                // when plot is updating while the underlying data is being modified.
                try {
                    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
                } catch (IndexOutOfBoundsException e) {
                    // Can be ignored. Simply skip rendering of this data item.
                }
            }

        };
        mRenderer.setDrawBarOutline(false);
        mRenderer.setSeriesPaint(0, SUCCESS_COLOR);
        mRenderer.setSeriesPaint(1, POLICY_VIOLATION_COLOR);
        mRenderer.setSeriesPaint(2, ROUTING_FAILURE_COLOR);
        mRenderer.setBaseToolTipGenerator(messageRateToolTipGenerator);
        XYPlot mPlot = new XYPlot(_messageRates, null, mYAxis, mRenderer);

        //
        // Now combine all plots to share the same time (x-)axis.
        //

        DateAxis xAxis = new DateAxis(null);
        xAxis.setAutoRange(true);
        xAxis.setFixedAutoRange(_maxTimeRange);
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(xAxis);
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
        // datasets are changing, otherwise IndexOutOfBoundsException could happen.
        _chart.setNotify(false);

        // Adds new data from the MetricsBin to our JFreeChart data structures.
        Iterator itor = metricsBins.iterator();
        while (itor.hasNext()) {
            MetricsBin bin = (MetricsBin) itor.next();
            SimpleTimePeriod period = new SimpleTimePeriod(bin.getStartTime(), bin.getEndTime());

            _frontendResponseTimes.add(period,
                    bin.getAverageFrontendResponseTime(),
                    bin.getMaxFrontendResponseTime(),
                    bin.getMinFrontendResponseTime());
            _backendResponseTimes.add(period,
                    bin.getAverageBackendResponseTime(),
                    bin.getMaxBackendResponseTime(),
                    bin.getMinBackendResponseTime());

            double successRate = bin.getCompletedRate();
            double policyViolationRate = bin.getAttemptedRate() - bin.getAuthorizedRate();
            double routingFailureRate = bin.getAuthorizedRate() - bin.getCompletedRate();
            _messageRates.add(period, new Double(successRate), SERIES_NAME_SUCCESS, false);
            _messageRates.add(period, new Double(policyViolationRate), SERIES_NAME_POLICY_VIOLATION, false);
            _messageRates.add(period, new Double(routingFailureRate), SERIES_NAME_ROUTING_FAILURE, false);
        }

        // Remove data older than our maximum allowed time range.
        long newLowerBound = (long) _responseTimes.getDomainUpperBound(true) - _maxTimeRange;
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

        // Re-enable notification now that all dataset change is done.
        _chart.setNotify(true);

        // Now sends notifications to cause plots to update.
        try {
            _responseTimes.validateObject();
            _messageRates.validateObject();
        } catch (InvalidObjectException e) {
            // Should not get here.
        }
    }

    /** Clears all data and updates the plots. */
    public void clearData() {
        // Temporarily disable notification so plots won't be redrawn when
        // datasets are changing, otherwise IndexOutOfBoundsException could happen.
        _chart.setNotify(false);

        _frontendResponseTimes.delete(0, _frontendResponseTimes.getItemCount() - 1);
        _backendResponseTimes.delete(0, _backendResponseTimes.getItemCount() - 1);

        for (int i = _messageRates.getItemCount(0) - 1; i >= 0; -- i) {
            _messageRates.remove(_messageRates.getTimePeriod(i), SERIES_NAME_SUCCESS, false);
            _messageRates.remove(_messageRates.getTimePeriod(i), SERIES_NAME_POLICY_VIOLATION, false);
            _messageRates.remove(_messageRates.getTimePeriod(i), SERIES_NAME_ROUTING_FAILURE, false);
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
    }

    /** Creates a fake metrics bin. For testing only. */
    private static MetricsBin fakeMetricsBin(long startTime, long endTime, int binInterval) {
        MetricsBin bin = new MetricsBin(startTime, binInterval, MetricsBin.RES_FINE, "SSG1", 123456);
        double cosine = Math.cos(2. * Math.PI * startTime / (100. * 5 * 1000));
        int numSuccess = (int) (1000. + 100. * cosine);
        int numViolation = (int) (100. + 100. * cosine);
        int numFailure = (int) (50. + 50. * cosine);
        int numAttempted = numSuccess + numViolation + numFailure;
        int numAuthorized = numSuccess + numFailure;
        int numCompleted = numSuccess;
        int frontendResponseTimeAvg = (int) (800. + 100. * cosine);
        int frontendResponseTimeMin = (int) (700. + 100. * cosine);
        int frontendResponseTimeMax = (int) (900. + 100. * cosine);
        int backendResponseTimeAvg = (int) (600. - 100. * cosine);
        int backendResponseTimeMin = (int) (500. - 100. * cosine);
        int backendResponseTimeMax = (int) (700. - 100. * cosine);
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

        MetricsChartPanel chartPanel = new MetricsChartPanel(MAX_TIME_RANGE);

        JFrame mainFrame = new JFrame("MetricsChartPanel Test");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.add(chartPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);

        // Waits 3 seconds to simulate network delay to fetch data from SSG.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        // Fakes metric bins.
        long dataStart = ((System.currentTimeMillis() - MAX_TIME_RANGE) / FINE_INTERVAL) * FINE_INTERVAL;
        long dataEnd = dataStart + MAX_TIME_RANGE;
        List metricsBins = new ArrayList();
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
            long endTime = System.currentTimeMillis();
            long startTime = endTime - FINE_INTERVAL;
            MetricsBin bin = fakeMetricsBin(startTime, endTime, FINE_INTERVAL);
            List bins = new ArrayList();
            bins.add(bin);
            chartPanel.addData(bins);
        }
    }
}
