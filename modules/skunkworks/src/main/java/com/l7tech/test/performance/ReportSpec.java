/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

import com.sun.japex.report.TestSuiteReport;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Japex test report path specification.
 *
 * <p>Used for parsing specifications of Japex test report paths entered through
 * command line.
 *
 * <p>To specify a single Japex test report, simply list its file path:
 * <blockquote><code>
 * C:\Inetpub\wwwroot\testperf\reports\4.0.xml
 * </code></blockquote>
 *
 * <p>To sepcify all Japex test reports in a directory, simply list its directory path:
 * <blockquote>
 * <code>C:\Inetpub\wwwroot\testperf\reports</code>
 * </blockquote>
 *
 * <p>To specify all Japex test reports in a directory within a date range:
 * <blockquote>
 * <code>C:\Inetpub\wwwroot\testperf\reports?</code><i>start</i><code>?</code><i>offset</i>
 * </blockquote>
 * where <i>start</i> is either "<code>today</code>" or a date in the form yyyy-mm-dd,
 * and offset is in the form [+-]number[DWMY], e.g.,
 * <blockquote><code>
 * C:\Inetpub\wwwroot\testperf\reports?today?-10D
 * </code></blockquote> for the last 10 days, or
 * <blockquote><code>
 * C:\Inetpub\wwwroot\testperf\reports?t2007-01-01?4W
 * </code></blockquote> for the 4 weeks since Jan 1, 2007.
 */
public class ReportSpec {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /** Directory or file path. */
    public String path;

    /** Minimum time (inclusive) if {@link #path} is a directory; <code>null</code> otherwise */
    public Calendar from;

    /** Maximum time (inclusive) if {@link #path} is a directory; <code>null</code> otherwise */
    public Calendar to;

    /**
     * Parses the given command line arguments into report path specifications.
     *
     * @param args  command line arguments
     * @return list of report path specifications
     * @throws IllegalArgumentException if the command line arguments are malformed
     */
    public static List<ReportSpec> parse(List<String> args) {
        final List<ReportSpec> results = new ArrayList<ReportSpec>();
        String arg = null;
        try {
            for (int i = 0; i < args.size(); ++ i) {
                arg = args.get(i);
                final String[] parts = arg.split("\\?", 3);
                if (parts.length == 1 || parts.length == 3) {
                    final ReportSpec reportSpec = new ReportSpec();
                    reportSpec.path = parts[0];

                    if (parts.length == 3) {
                        parseTimeRange(parts[1], parts[2], reportSpec);
                    }

                    results.add(reportSpec);
                } else {
                    throw new IllegalArgumentException("Invalid report specification: " + arg);
                }
            }

            return results;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date/offset: " + arg);
        }
    }

    private static void parseTimeRange(String ymd, String offset, ReportSpec reportSpec) throws ParseException {
        final Date date = ymd.equals("today") ? new Date() : DATE_FORMAT.parse(ymd);

        final int n = Integer.parseInt(offset.substring(0, offset.length() - 1));
        final char c = offset.toUpperCase().charAt(offset.length() - 1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        switch (c) {
            case'D':
                cal.add(Calendar.DATE, n);
                break;
            case'W':
                cal.add(Calendar.WEEK_OF_YEAR, n);
                break;
            case'M':
                cal.add(Calendar.MONTH, n);
                break;
            case'Y':
                cal.add(Calendar.YEAR, n);
        }

        Calendar from = null;
        Calendar to = null;
        if (n > 0) {
            from = Calendar.getInstance();
            from.setTime(date);
            to = cal;
        } else {
            from = cal;
            to = Calendar.getInstance();
            to.setTime(date);
            to.add(Calendar.DATE, 1);      // needed?
        }

        reportSpec.from = from;
        reportSpec.to = to;
    }

    /**
     * Parses test suite reports given a list of report path specifications.
     *
     * @param reportSpecs   a list of report path specifications
     * @param out           if not null, warnings are written to this PrintStream
     * @return a map of test suite reports; map keys are reports sorted by report
     *         times; map values are the file path where the report was read from
     * @throws ParserConfigurationException if there is a serious configuration error with the XML parser
     */
    public static SortedMap<TestSuiteReport, String> getReports(final List<ReportSpec> reportSpecs, final PrintStream out)
            throws ParserConfigurationException {
        final SortedMap<TestSuiteReport, String> reports =
                new TreeMap<TestSuiteReport, String>(new Comparator<TestSuiteReport>() {
                    public int compare(TestSuiteReport r1, TestSuiteReport r2) {
                        return r1.getDate().compareTo(r2.getDate());
                    }
                });

        String errPath = null;   // for exception message
        try {
            for (ReportSpec reportSpec : reportSpecs) {
                final File f = new File(reportSpec.path);
                if (f.exists()) {
                    if (f.isFile()) {
                        errPath = reportSpec.path;
                        if (reportSpec.from != null) {
                            if (out != null) {
                                out.println("Warning: Ignoring date/offset specification for report file: " + errPath);
                            }
                        }
                        reports.put(new TestSuiteReport(f), f.getAbsolutePath());
                    } else if (f.isDirectory()) {
                        for (File child : f.listFiles()) {
                            if (child.isFile() && child.getName().toLowerCase().endsWith(".xml")) {
                                errPath = child.getPath();
                                if (PerformanceUtil.isJapexReport(child)) {
                                    final TestSuiteReport report = new TestSuiteReport(child);
                                    if (reportSpec.from == null ||
                                        (report.getDate().compareTo(reportSpec.from) >= 0 &&
                                         report.getDate().compareTo(reportSpec.to) <= 0)) {
                                        reports.put(report, child.getAbsolutePath());
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (out != null) {
                        out.println("Warning: Ignoring non-existing file or directory path: " + reportSpec.path);
                    }
                }
            }
        } catch (SAXException e) {
            if (out != null) {
                out.println("Warning: Skipping malformed Japex test suite report \"" + errPath + "\": " + e.toString());
            }
        } catch (IOException e) {
            if (out != null) {
                out.println("Warning: Skipping unreadable Japex test suite report \"" + errPath + "\": " + e.toString());
            }
        }

        return reports;
    }
}
