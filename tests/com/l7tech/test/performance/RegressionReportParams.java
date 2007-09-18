/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

import java.util.ArrayList;
import java.util.List;

/**
 * For parsing command line interface of {@link RegressionReport}.
 *
 * @see com.l7tech.test.performance
 * @author rmak
 */
public class RegressionReportParams {

    /** Comparison types. */
    public enum Comparison {
        above("above (or more)"),
        below("below (or more)"),
        outside("outside"),
        within("within");

        public final String htmlText;
        Comparison(String htmlText) { this.htmlText = htmlText; }
    };

    /** Simple average. */
    public static final String AVG = "avg";

    /** Population standard deviation. */
    public static final String STDEV = "stdev";

    private boolean _alwaysEMail;
    private String _benchmark;
    private String _ccAddresses;
    private Comparison _comparison;
    private String _httpDir;
    private final List<String> _notes = new ArrayList<String>();
    private boolean _noWarning;
    private String _outputDir;
    private double _percent;
    private List<ReportSpec> _reportSpecs = new ArrayList<ReportSpec>();
    private String _title;
    private String _fromAddress;
    private String _toAddresses;
    private boolean _verbose;

    private void displayUsageAndExit() {
        System.out.println(
                "Purpose: Runs regression on Japex test reports, with the lastest as the\n" +
                "         target and the rest as benchmark; plus sends e-mail notification if a\n" +
                "         threshold has been crossed.\n" +
                "Usage: java RegressionReport [option...] title outputDir sender toList percent comparison\n" +
                "           benchmark (reportFile|reportDir[?date?offset])...\n" +
                "       E-mail server connection parameters are configured through Java Mail API system properties.\n" +
                "Options:\n" +
                "       -alwaysEMail    send mail even if threshold has not be reached\n" +
                "       -cc=list        CC list (comma-separated e-mail addresses)\n" +
                "       -httpDir=dir    the subdirectory portion of the output URL\n" +
                "       -note=text      an HTML note to be added to the report\n" +
                "       -noWarning      suppress warnings\n" +
                "       -verbose        prints progress\n" +
                "Required arguments:\n" +
                "       title           report title\n" +
                "       outputDir       directory to save regression report\n" +
                "       sender          e-mail address of sender\n" + 
                "       toList          comma-separated e-mail addresses\n" +
                "       percent         percentage difference between benchmark and target\n" +
                "       comparison      type of comparison: \"ge\" or \"outside\"\n" +
                "                           (ge means >=, outside means > || <)\n" +
                "       benchmark       benchmark to compare against: a report name, \"" + AVG + "\" or \"" + STDEV + "\"\n" +
                "                           (the latest data is excluded when calculating " + AVG + " and " + STDEV + ")\n" +
                "       reportFile      a Japex test report file" +
                "       reportDir       a directory containing Japex test report files\n" +
                "       date            starting/ending date to include; in the format YYYY-MM-DD or \"today\"\n" +
                "       offset          positive or negative offset from 'date' in the format '-?[0-9]+(D|W|M|Y)'\n" +
                "                       where D=days, W=weeks, M=months and Y=years (default -1Y)\n" +
                "Example: To send e-mail to Lisa and Marge, CC Bart and Homer, if the lastest\n" +
                "         result is 5% >= average based on reports from 4.0 and the last 10 days:\n" +
                "           java -Dmail.smtp.host=layer7-mx0\n" +
                "               " + RegressionReport.class.getName() + "\n" +
                "               -cc=bart@layer7tech.com,homer@layer7tech.com\n" +
                "               -httpDir=testperf/regression\n" +
                "               -note=This is <b>so cool</b>!\n" +
                "               \"Performance Regression Report\"\n" +
                "               C:\\Inetpub\\wwwroot\\testperf\\regression\n" +
                "               lisa@layer7tech.com,marge@layer7tech.com\n" +
                "               5 ge avg\n" +
                "               C:\\Inetpub\\wwwroot\\testperf\\reports\\4.0.xml\n" +
                "               C:\\Inetpub\\wwwroot\\testperf\\reports?today?-10D\n"
        );
        System.exit(1);
    }

    public RegressionReportParams(final String[] args) {
        // Parses command line options.
        final List<String> tokens = new ArrayList<String>();
        for (int i = 0; i < args.length; ++ i) {
            if (args[i].startsWith("-")) {
                if (args[i].equals("-help") || args[i].equals("-h") || args[i].equals("-?")) {
                    displayUsageAndExit();
                } else if (args[i].equals("-alwaysEMail")) {
                    _alwaysEMail = true;
                } else if (args[i].startsWith("-cc=")) {
                    _ccAddresses = args[i].substring(4);
                } else if (args[i].startsWith("-httpDir=")) {
                    _httpDir = args[i].substring(9);
                } else if (args[i].startsWith("-note=")) {
                    _notes.add(args[i].substring(6));
                } else if (args[i].equals("-noWarning")) {
                    _noWarning = true;
                } else if (args[i].equals("-verbose")) {
                    _verbose = true;
                } else {
                    System.out.println("Error: Unknown command line option: " + args[i]);
                    displayUsageAndExit();
                }
            } else {
                tokens.add(args[i]);
            }
        }

        // Parses command line required arguments.
        if (tokens.size() < 8) {
            displayUsageAndExit();
        }

        try {
            _title = tokens.get(0);
            _outputDir = tokens.get(1);
            _fromAddress = tokens.get(2);
            _toAddresses = tokens.get(3);
            _percent = Double.parseDouble(tokens.get(4));
            _comparison = Comparison.valueOf(tokens.get(5));
            _benchmark = tokens.get(6);
            _reportSpecs = ReportSpec.parse(tokens.subList(7, tokens.size()));
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    public String getBenchmark() {
        return _benchmark;
    }

    /**
     * @return comma-separated list of CC addresses to e-mail; can be null
     */
    public String getCcAddresses() {
        return _ccAddresses;
    }

    public Comparison getComparison() {
        return _comparison;
    }

    /**
     * @return the subdirectory portion of the output URL; can be null
     */
    public String getHttpDir() {
        return _httpDir;
    }

    public List<String> getNotes() {
        return _notes;
    }

    public String getOutputDir() {
        return _outputDir;
    }

    public double getPercent() {
        return _percent;
    }

    public List<ReportSpec> getReportSpecs() {
        return _reportSpecs;
    }

    public String getTitle() {
        return _title;
    }

    public String getFromAddress() {
        return _fromAddress;
    }

    public String getToAddresses() {
        return _toAddresses;
    }

    public boolean isAlwaysEMail() {
        return _alwaysEMail;
    }

    public boolean isNoWarning() {
        return _noWarning;
    }

    public boolean isVerbose() {
        return _verbose;
    }
}
