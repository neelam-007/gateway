/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

import com.l7tech.common.util.FileUtils;
import com.sun.japex.report.TestSuiteReport;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.apache.commons.lang.StringEscapeUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Trend report generator.
 *
 * <p>This is our custom replacement of {@link com.sun.japex.report.TrendReport}.
 *
 * @see TrendReportParams
 * @see com.l7tech.test.performance
 * @author rmak
 */
public class TrendReport {

    /** HTML template. */
    private static final String HTML_TEMPLATE_PATH = "resources/TrendReport.html";

    private static final String CSS_TEMPLATE_PATH = "resources/TrendReport.css";

    // Index page HTML template markers.
    private static final String TITLE_MARKER = "<!--{title}-->";
    private static final String CREATION_TIME_MARKER = "<!--{creation time}-->";
    private static final String REPORTS_ROW_MARKER = "<!--{next report row}-->";
    private static final String REPORTS_OPTION_MARKER = "<!--{next report option}-->";
    private static final String RESULTS_HEADER_MARKER = "<!--{results header}-->";
    private static final String RESULTS_ROW_MARKER = "<!--{next results row}-->";
    private static final String TESTCASE_IMG_MARKER = "<!--{next test case img}-->";
    private static final String NOTES_MARKER = "<!--{next note}-->";

    private static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd E hh:mm:ss a z");

    private static final int CHART_WIDTH  = 800;
    private static final int CHART_HEIGHT = 200;

    public static void main(String[] args) throws Exception {
        TrendReportParams params = new TrendReportParams(args);
        new TrendReport().run(params);
        System.exit(0);
    }

    public void run(TrendReportParams params) throws  IOException, ParserConfigurationException {
        final SortedMap<TestSuiteReport, String> reports = ReportSpec.getReports(
                params.getReportSpecs(), params.isNoWarning() ? null : System.out);
        if (params.isVerbose()) {
            System.out.println("Found " + reports.size() + " Japex report.");
        }

        // Gathers and sorts all the test case names.
        final SortedSet<String> testCaseNames = new TreeSet<String>();
        for (TestSuiteReport report : reports.keySet()) {
            for (TestSuiteReport.Driver driver : report.getDrivers()) {
                for (TestSuiteReport.TestCase testCase : driver.getTestCases()) {
                    testCaseNames.add(testCase.getName());
                }
            }
        }

        // Creates output directory if not exists.
        final File outputDir = new File(params.getOutputDir());
        if (! outputDir.exists()) {
            if (! outputDir.mkdirs()) {
                throw new IOException("Cannot create output directory: " + outputDir.getAbsolutePath());
            }
        }

        // Composes the HTML index page by editing the markers in the template.
        final StringBuilder html = PerformanceUtil.readResource(getClass(), HTML_TEMPLATE_PATH);
        PerformanceUtil.replaceMarkerAll(html, TITLE_MARKER, params.getTitle());
        PerformanceUtil.replaceMarkerAll(html, CREATION_TIME_MARKER, LONG_DATE_FORMAT.format(new Date()));

        int column = 0;
        for (TestSuiteReport report : reports.keySet()) {
            ++ column;
            final String reportPath = reports.get(report);
            final File reportFile = new File(reportPath);
            final String reportRelativePath = FileUtils.getRelativePath(outputDir, reportFile).replace('\\', '/');
            // TODO class="outlier"
            PerformanceUtil.insertAtMarker(html, REPORTS_ROW_MARKER,
                    "<tr>" +
                    "<td><input class=\"hideShowCheckbox\" type=\"checkbox\" checked onClick=\"hideShowColumn(" + column + ", this.checked)\"></td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NAME)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(LONG_DATE_FORMAT.format(report.getDate().getTime())) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.SS_VERSION)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.HOST_NAME)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NUMBER_OF_CPUS)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.OS_NAME) + " " + PerformanceUtil.getParameter(report, Constants.OS_ARCHITECTURE)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.VM_INFO)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NOTES)) + "</td>" +
                    "<td><a href=\"" + StringEscapeUtils.escapeHtml(reportRelativePath) + "\">" + StringEscapeUtils.escapeHtml(reportFile.getName()) + "</td>" +
                    "</tr>\n"
            );
        }

        final StringBuilder optionsHtml = new StringBuilder();
        column = 0;
        for (TestSuiteReport report : reports.keySet()) {
            ++ column;
            optionsHtml.append("<option value=\"" + column + "\">" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NAME)) + "</option>");
        }
        PerformanceUtil.replaceMarkerAll(html, REPORTS_OPTION_MARKER, optionsHtml.toString());
        
        final StringBuilder headerHtml = new StringBuilder("<tr><th></th>");
        for (TestSuiteReport report : reports.keySet()) {
            headerHtml.append("<th>");
            headerHtml.append(StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NAME)));
            headerHtml.append("</th>");
        }
        headerHtml.append("<th class=\"avg\">Average</th><th class=\"stdev\">Std Dev</th></tr>");
        PerformanceUtil.replaceMarkerAll(html, RESULTS_HEADER_MARKER, headerHtml.toString());

        for (String testCaseName : testCaseNames) {
            final StringBuilder htmlRow = new StringBuilder("<tr><td><a href=\"#" + StringEscapeUtils.escapeHtml(testCaseName) + "\">" + StringEscapeUtils.escapeHtml(testCaseName) + "</a></td>");
            for (TestSuiteReport report : reports.keySet()) {
                final List<TestSuiteReport.Driver> drivers = report.getDrivers();
                if (drivers.size() == 0) {
                    htmlRow.append("<td></td>");
                } else {
                    if (drivers.size() > 1) {
                        if (!params.isNoWarning()) {
                            System.out.println("Warning: More than 1 driver found in report; ignoring all but the first: " + reports.get(report));
                        }
                    }
                    final TestSuiteReport.TestCase testCase = drivers.get(0).getTestCase(testCaseName);
                    if (testCase == null) {
                        htmlRow.append("<td></td>");
                    } else {
                        htmlRow.append("<td>");
                        htmlRow.append(testCase.getResult());
                        htmlRow.append("</td>");
                    }
                }
            }
            htmlRow.append("<td class=\"avg\"></td><td class=\"stdev\"></td></tr>\n");
            PerformanceUtil.insertAtMarker(html, RESULTS_ROW_MARKER, htmlRow.toString());
        }

        for (String testCaseName : testCaseNames) {
            final String chartFileName = testCaseName.replaceAll("[\\/:*?\"<>|]", "_") + ".png";
            final File chartFilePath = new File(outputDir, chartFileName);
            createTrendChart(testCaseName, reports.keySet(), chartFilePath);
            PerformanceUtil.insertAtMarker(html, TESTCASE_IMG_MARKER,
                    "<img class=\"plot\" id=\"" + StringEscapeUtils.escapeHtml(testCaseName) + "\" src=\"" + StringEscapeUtils.escapeHtml(chartFileName) + "\"><br>\n"
            );
        }

        for (String note : params.getNotes()) {
            PerformanceUtil.insertAtMarker(html, NOTES_MARKER, note + "<br/>");
        }

        // Saves HTML to file.
        FileWriter htmlWriter = null;
        try {
            final File htmlFile = new File(outputDir, "index.html");
            htmlWriter = new FileWriter(htmlFile);
            htmlWriter.write(html.toString());
            if (params.isVerbose()) {
                System.out.println("Trend report saved to " + htmlFile);
            }
        } finally {
            htmlWriter.close();
        }

        // Copy/Create CSS file.
        PerformanceUtil.copyResource(getClass(), CSS_TEMPLATE_PATH,
                new File(outputDir, new File(CSS_TEMPLATE_PATH).getName()).getPath());
    }

    /**
     * Creates a chart for a test case across all reports.
     */
    private void createTrendChart(final String testCaseName, final Collection<TestSuiteReport> reports, final File destFile) throws IOException {
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (TestSuiteReport report : reports) {
            for (TestSuiteReport.Driver driver : report.getDrivers()) {
                final TestSuiteReport.TestCase testCase = driver.getTestCase(testCaseName);
                if (testCase != null) {
                    final double value = testCase.getResult();
                    if (! Double.isNaN(value)) {
                        dataset.addValue(value, driver.getName(), PerformanceUtil.getParameter(report, Constants.NAME));
                    }
                }
            }
        }

        final JFreeChart chart = ChartFactory.createLineChart(
                testCaseName,
                "",
                "transactions/sec",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                false,
                false);
        chart.setAntiAlias(true);

        final CategoryPlot plot = chart.getCategoryPlot();
        final DrawingSupplier supplier = new DefaultDrawingSupplier(
            DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
            // Draw a small diamond
            new Shape[] { new Polygon(new int[] {3, 0, -3, 0},
                                      new int[] {0, 3, 0, -3}, 4) }
        );
        plot.setDomainGridlinePaint(Color.black);
        plot.setRangeGridlinePaint(Color.black);
        plot.setDrawingSupplier(supplier);

        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setShapesVisible(true);
        renderer.setStroke(new BasicStroke(2.0f));

        final CategoryAxis axis = plot.getDomainAxis();
        axis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        axis.setMaximumCategoryLabelWidthRatio(Float.MAX_VALUE);
        axis.setMaximumCategoryLabelLines(Integer.MAX_VALUE);

        ChartUtilities.saveChartAsPNG(destFile, chart, CHART_WIDTH, CHART_HEIGHT);
    }

    private void copyResource(final String resourceName, final String filePath) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            final InputStream in = getClass().getResourceAsStream(resourceName);
            if (in == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            is = new BufferedInputStream(in);
            os = new BufferedOutputStream(new FileOutputStream(filePath));
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
