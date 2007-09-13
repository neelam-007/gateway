/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

import com.sun.japex.report.TestSuiteReport;
import com.l7tech.common.util.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

import javax.xml.parsers.ParserConfigurationException;
import javax.mail.Session;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Regression report generator.
 *
 * <p>This is our custom replacement of {@link com.sun.japex.RegressionTracker}.
 *
 * @see RegressionReportParams
 * @see com.l7tech.test.performance
 * @author rmak
 */
public class RegressionReport {

    /** HTML template. */
    private static final String HTML_TEMPLATE_PATH = "resources/RegressionReport.html";

    // Index page HTML template markers.
    private static final String BASE_HREF_MARKER = "<!--{base href}-->";
    private static final String TITLE_MARKER = "<!--{title}-->";
    private static final String CREATION_TIME_MARKER = "<!--{creation time}-->";
    private static final String THRESHOLD_MARKER = "<!--{threshold}-->";
    private static final String BENCHMARK_HEADER_1_MARKER = "<!--{benchmark header 1}-->";
    private static final String BENCHMARK_HEADER_2_MARKER = "<!--{benchmark header 2}-->";
    private static final String TARGET_HEADER_MARKER = "<!--{target header}-->";
    private static final String RESULTS_ROW_MARKER = "<!--{next results row}-->";
    private static final String TARGET_FILE_MARKER = "<!--{target report}-->";
    private static final String BENCHMARK_FILES_MARKER = "<!--{next benchmark report}-->";
    private static final String NOTES_MARKER = "<!--{next note}-->";

    private static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd E hh:mm:ss a z");

    public static void main(String[] args) throws Exception {
        RegressionReportParams params = new RegressionReportParams(args);
        new RegressionReport().run(params);
        System.exit(0);
    }

    public void run(RegressionReportParams params) throws IOException, ParserConfigurationException {

        final SortedMap<TestSuiteReport, String> reports = ReportSpec.getReports(
                params.getReportSpecs(), params.isNoWarning() ? null : System.out);
        if (reports.size() < 2) {
            System.out.println("Error: Insufficent number of reports to perform regression (found: " + reports.size() + ", expected: at least 2)");
            System.exit(1);
        }
        if (params.isVerbose()) {
            System.out.println("Found " + reports.size() + " Japex test reports.");
        }

        // Regression target is the latest report.
        TestSuiteReport targetReport = null;
        for (TestSuiteReport testSuiteReport : reports.keySet()) {
            targetReport = testSuiteReport;
        }
        final String targetReportPath = reports.get(targetReport);
        final File targetReportFile = new File(targetReportPath);
        final String targetReportName = PerformanceUtil.getParameter(targetReport, Constants.NAME);

        // Removes the target report from those used for benchmarking.
        reports.remove(targetReport);

        final String benchmark = params.getBenchmark();
        final boolean benchMarkIsSingleReport = ! (benchmark.equals(RegressionReportParams.AVG) || benchmark.equals(RegressionReportParams.STDEV));
        if (benchMarkIsSingleReport) {
            if (benchmark.equals(targetReportName)) {
                System.out.println("Error: target report is same as benchmark report: " + benchmark);
                System.exit(1);
            }
        }

        // Sorts the test case names in the target report.
        if (targetReport.getDrivers().size() > 1) {
            if (!params.isNoWarning()) {
                System.out.println("Warning: More than 1 driver found in target report; ignoring all but the first: " + targetReportPath);
            }
        }
        final TestSuiteReport.Driver targetDriver = targetReport.getDrivers().get(0);
        final SortedSet<String> testCaseNames = new TreeSet<String>();
        for (TestSuiteReport.TestCase testCase : targetDriver.getTestCases()) {
            testCaseNames.add(testCase.getName());
        }

        // Creates output directory if not exists.
        final File outputDir = new File(params.getOutputDir());
        if (! outputDir.exists()) {
            if (! outputDir.mkdirs()) {
                throw new IOException("Cannot create output directory: " + outputDir.getAbsolutePath());
            }
        }

        // Begin composing the HTML page by editing the markers in the template.
        final StringBuilder html = PerformanceUtil.readResource(getClass(), HTML_TEMPLATE_PATH);
        PerformanceUtil.replaceMarkerAll(html, TITLE_MARKER, StringEscapeUtils.escapeHtml(params.getTitle()));
        PerformanceUtil.replaceMarkerAll(html, CREATION_TIME_MARKER, StringEscapeUtils.escapeHtml(LONG_DATE_FORMAT.format(new Date())));

        final StringBuilder thresholdHtml = new StringBuilder();
        thresholdHtml.append(params.getPercent());
        thresholdHtml.append("% ");
        thresholdHtml.append(params.getComparison().htmlText);
        thresholdHtml.append(" ");
        thresholdHtml.append(StringEscapeUtils.escapeHtml(params.getBenchmark()));
        PerformanceUtil.replaceMarkerAll(html, THRESHOLD_MARKER, thresholdHtml.toString());

        if (benchMarkIsSingleReport) {
            PerformanceUtil.replaceMarkerAll(html, BENCHMARK_HEADER_1_MARKER, "<th>Benchmark</th>");
            PerformanceUtil.replaceMarkerAll(html, BENCHMARK_HEADER_1_MARKER, "<th>" + StringEscapeUtils.escapeHtml(benchmark) + "</th>");
        } else {
            PerformanceUtil.replaceMarkerAll(html, BENCHMARK_HEADER_1_MARKER, "<th colspan=\"2\">Benchmark</th>");
            PerformanceUtil.replaceMarkerAll(html, BENCHMARK_HEADER_2_MARKER, "<th>Averge</th><th>Std Dev</th>");
        }

        PerformanceUtil.replaceMarkerAll(html, TARGET_HEADER_MARKER, "<th>" + StringEscapeUtils.escapeHtml(targetReportName) + "</th>");

        final String targetRelativePath = FileUtils.getRelativePath(outputDir, targetReportFile).replace('\\', '/');
        PerformanceUtil.replaceMarkerAll(html, TARGET_FILE_MARKER, "<a href=\"" + StringEscapeUtils.escapeHtml(targetRelativePath) + "\">" + StringEscapeUtils.escapeHtml(targetReportFile.getName()) + "</a>");

        for (TestSuiteReport report : reports.keySet()) {
            final File reportFile = new File(reports.get(report));
            final String reportRelativePath = FileUtils.getRelativePath(outputDir, reportFile).replace('\\', '/');
            PerformanceUtil.insertAtMarker(html, BENCHMARK_FILES_MARKER, "<a href=\"" + StringEscapeUtils.escapeHtml(reportRelativePath) + "\">" + StringEscapeUtils.escapeHtml(reportFile.getName()) + "</a><br/>");
        }

        for (String note : params.getNotes()) {
            PerformanceUtil.insertAtMarker(html, NOTES_MARKER, note + "<br/>");
        }

        // Runs regression against each test case in the target report.
        boolean anyFail = false;    // true if any test cases fail regression threshold
        for (String testCaseName : testCaseNames) {

            int n = 0;          // number of results found to benchmark against
            double sum = 0.;    // sum of results
            double sum2 = 0.;   // sum of squares of results

            // These are used if benchmarking against a single report.
            double single;

            // Looks for reports with matching test case name.
            for (TestSuiteReport report : reports.keySet()) {
                final String reportName = PerformanceUtil.getParameter(report, Constants.NAME);
                final List<TestSuiteReport.Driver> drivers = report.getDrivers();
                if (drivers.size() > 0) {
                    if (drivers.size() > 1 && ! params.isNoWarning()) {
                        System.out.println("Warning: More than 1 driver found in Japex test report; ignoring all but the first: " + reports.get(report));
                    }

                    final TestSuiteReport.TestCase testCase = drivers.get(0).getTestCase(testCaseName);
                    if (testCase != null) {
                        final double testResult = testCase.getResult();

                        if (benchMarkIsSingleReport) {
                            if (reportName.equals(benchmark)) {
                                n = 1;
                                sum = testResult;
                                break;  // stop looking; break out of for-loop
                            }
                        } else {
                            ++ n;
                            sum += testResult;
                            sum2 += testResult * testResult;
                        }
                    }
                }
            }

            // Do threshold comparison.
            final double targetResult = targetDriver.getTestCase(testCaseName).getResult();
            if (n > 0) {
                final double avg = sum / n;
                double stdev = 0.;
                if (n >= 2) {
                    stdev = Math.sqrt((sum2 - n * avg * avg) / (n - 1));
                }

                double delta;   // percentage difference
                if (benchmark.equals(RegressionReportParams.STDEV)) {
                    delta = (targetResult - stdev) / stdev * 100.;
                } else {
                    delta = (targetResult - avg) / avg * 100.;
                }

                final RegressionReportParams.Comparison comparison = params.getComparison();
                final double percent = params.getPercent();
                boolean fail;
                if (comparison == RegressionReportParams.Comparison.ge) {
                    fail = delta >= percent;
                } else if (comparison == RegressionReportParams.Comparison.outside) {
                    fail = Math.abs(delta) >= percent;
                } else {
                    throw new RuntimeException("Missing code to handle comparison type: " + comparison);
                }
                anyFail |= fail;

                final StringBuilder tr = new StringBuilder("<tr><td>" + StringEscapeUtils.escapeHtml(testCaseName) + "</td>");
                tr.append("<td>");
                tr.append(avg);
                tr.append("</td>");
                if (! benchMarkIsSingleReport) {
                    tr.append("<td>");
                    tr.append(stdev);
                    tr.append("</td>");
                }
                tr.append("<td>");
                tr.append(targetResult);
                tr.append("</td><td");
                if (fail) tr.append(" class=\"warn\"");
                tr.append(">");
                if (delta > 0.) tr.append("+");
                tr.append(delta);
                tr.append("%</td></tr>\n");
                PerformanceUtil.insertAtMarker(html, RESULTS_ROW_MARKER, tr.toString());
            } else {
                // No report found to benchmark against this test case name.
                PerformanceUtil.insertAtMarker(html, RESULTS_ROW_MARKER,
                        "<tr>" +
                        "<td>" + StringEscapeUtils.escapeHtml(testCaseName) + "</td>" +
                        "<td></td>" +
                        "<td></td>" +
                        "<td>" + targetResult + "</td>" +
                        "<td></td>" +
                        "</tr>\n"
                );
            }
        }

        // Saves HTML to file.
        FileWriter htmlWriter = null;
        try {
            final File htmlFile = new File(params.getOutputDir(), "index.html");
            htmlWriter = new FileWriter(htmlFile);
            htmlWriter.write(html.toString());
            if (params.isVerbose()) {
                System.out.println("Regression report saved to " + htmlFile);
            }
        } finally {
            htmlWriter.close();
        }

        // Send HTML as e-mail.
        if (anyFail || params.isAlwaysEMail()) {
            // Hyperlinks in e-mail need a base href.
            if (params.getHttpDir() != null) {
                PerformanceUtil.replaceMarkerAll(html, BASE_HREF_MARKER,
                        "<base href=\"http://" + InetAddress.getLocalHost().getHostName() + "/" + params.getHttpDir() + "/index.html\"/>");
            }

            try {
                final Properties props = System.getProperties();
                final Session session = Session.getDefaultInstance(props);
                final Message msg = new MimeMessage(session);
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(params.getToAddresses(), false));
                if (params.getCcAddresses() != null) {
                    msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(params.getCcAddresses(), false));
                }
                msg.setSubject(params.getTitle());
                msg.setText(html.toString());
                msg.setHeader("Content-Type", "text/html");
                msg.setSentDate(new Date());
                Transport.send(msg);
                if (params.isVerbose()) {
                    System.out.println("Sent e-mail to " + params.getToAddresses() +
                            (params.getCcAddresses() == null ? "" : ", CC " + params.getCcAddresses()));
                }
            } catch (MessagingException e) {
                System.out.println("Error: Failed to send e-mail: " + e.toString());
            }
        }
    }
}
