/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

import com.l7tech.common.util.FileUtils;
import com.sun.japex.report.TestSuiteReport;
import org.apache.commons.lang.StringEscapeUtils;
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

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Trend report generator.
 *
 * <p>(I wrote this as a custom replacement of {@link com.sun.japex.report.TrendReport}.)
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
    private static final String RESULTS_HEADER_1_MARKER = "<!--{results header row 1}-->";
    private static final String RESULTS_HEADER_2_MARKER = "<!--{results header row 2}-->";
    private static final String RESULTS_ROW_MARKER = "<!--{next results row}-->";
    private static final String TESTCASE_IMG_MARKER = "<!--{next test case img}-->";
    private static final String NOTES_MARKER = "<!--{next note}-->";

    private static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd E hh:mm:ss a z");

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();
    {
        NUMBER_FORMAT.setMaximumFractionDigits(1);
        NUMBER_FORMAT.setMinimumFractionDigits(1);
        NUMBER_FORMAT.setGroupingUsed(false);   // because they confused JavaScript
    }

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

        // Populates reportsTable.
        int reportIndex = 0;
        for (TestSuiteReport report : reports.keySet()) {
            final String reportPath = reports.get(report);
            final File reportFile = new File(reportPath);
            final String reportRelativePath = FileUtils.getRelativePath(outputDir, reportFile).replace('\\', '/');
            PerformanceUtil.insertAtMarker(html, REPORTS_ROW_MARKER,
                    "<tr>" +
                    "<td class=\"centered\"><input class=\"hideShowCheckbox\" type=\"checkbox\" checked onClick=\"hideShowReport(" + reportIndex + ", this.checked)\"/></td>" +
                    "<td class=\"centered\"><input class=\"statsCheckbox\" type=\"checkbox\" checked onClick=\"calcStats()\"/></td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NAME)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(LONG_DATE_FORMAT.format(report.getDate().getTime())) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.SS_VERSION)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.HOST_NAME)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NUMBER_OF_CPUS)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.OS_NAME) + " " + PerformanceUtil.getParameter(report, Constants.OS_ARCHITECTURE)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.VM_INFO)) + "</td>" +
                    "<td>" + StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NOTES)) + "</td>" +
                    "<td><a href=\"" + StringEscapeUtils.escapeHtml(reportRelativePath) + "\">" + StringEscapeUtils.escapeHtml(reportFile.getName()) + "</a></td>" +
                    "</tr>\n"
            );
            ++ reportIndex;
        }

        // Populates threshold highlight options.
        final StringBuilder optionsHtml = new StringBuilder();
        reportIndex = 0;
        for (TestSuiteReport report : reports.keySet()) {
            optionsHtml.append("<option value=\"")
                       .append(reportIndex)
                       .append("\">")
                       .append(StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NAME)))
                       .append("</option>");
            ++ reportIndex;
        }
        PerformanceUtil.replaceMarkerAll(html, REPORTS_OPTION_MARKER, optionsHtml.toString());

        // Populates resultsTable.
        final StringBuilder header1Html = new StringBuilder();
        final StringBuilder header2Html = new StringBuilder();
        for (TestSuiteReport report : reports.keySet()) {
            header1Html.append("<th colspan=\"2\">")
                       .append(StringEscapeUtils.escapeHtml(PerformanceUtil.getParameter(report, Constants.NAME)))
                       .append("</th>");
            header2Html.append("<th>tr/sec</th><th>&Delta;</th>");
        }
        PerformanceUtil.replaceMarkerAll(html, RESULTS_HEADER_1_MARKER, header1Html.toString());
        PerformanceUtil.replaceMarkerAll(html, RESULTS_HEADER_2_MARKER, header2Html.toString());

        for (String testCaseName : testCaseNames) {
            final StringBuilder htmlRow = new StringBuilder("<tr><td><a href=\"#" + StringEscapeUtils.escapeHtml(testCaseName) + "\">" + StringEscapeUtils.escapeHtml(testCaseName) + "</a></td>");
            for (TestSuiteReport report : reports.keySet()) {
                final List<TestSuiteReport.Driver> drivers = report.getDrivers();
                if (drivers.size() == 0) {
                    htmlRow.append("<td></td><td></td>");
                } else {
                    if (drivers.size() > 1) {
                        if (!params.isNoWarning()) {
                            System.out.println("Warning: More than 1 driver found in report; ignoring all but the first: " + reports.get(report));
                        }
                    }
                    final TestSuiteReport.TestCase testCase = drivers.get(0).getTestCase(testCaseName);
                    if (testCase == null) {
                        htmlRow.append("<td></td><td></td>");
                    } else {
                        htmlRow.append("<td class=\"right\">").append(NUMBER_FORMAT.format(testCase.getResult())).append("</td>")
                               .append("<td class=\"right\"></td>");    // percentage difference
                    }
                }
            }
            htmlRow.append("<td class=\"right avg\"></td><td class=\"right stdev\"></td></tr>\n");
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
     *
     * @param testCaseName  the Japex test case to plot
     * @param reports       Japex test reports
     * @param destFile      location to save the generated chart file
     * @throws IOException  if I/O error occurs
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
}
