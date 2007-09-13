/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

import java.util.ArrayList;
import java.util.List;

/**
 * For parsing command line interface of {@link TrendReport}.
 *
 * @see com.l7tech.test.performance
 * @author rmak
 */
public class TrendReportParams {

    private final List<String> _notes = new ArrayList<String>();
    private boolean _noWarning;
    private String _outputDir;
    private List<ReportSpec> _reportSpecs = new ArrayList<ReportSpec>();
    private String _title;
    private boolean _verbose;

    private void displayUsageAndExit() {
        System.out.println(
                "Purpose: Generates a trend report from Japex test reports.\n" +
                "Usage: java TrendReport [option...] title outputDir (reportFile|reportDir[?date?offset])...\n" +
                "Options:\n" +
                "       -note=text      an HTML note to be added to the report\n" +
                "       -noWarning      suppress warnings\n" +
                "       -verbose        prints progress\n" +
                "Required arguments:\n" +
                "       title           title to be used for this report\n" +
                "       outputDir       directory to save trend report\n" +
                "       reportFile      a Japex report file" +
                "       reportDir       a directory containing Japex report files\n" +
                "       date            starting/ending date to include; in the format YYYY-MM-DD or \"today\"\n" +
                "       offset          positive or negative offset from 'date' in the format '-?[0-9]+(D|W|M|Y)'\n" +
                "                       where D=days, W=weeks, M=months and Y=years (default -1Y)\n" +
                "Example: To generate from 4.0 release plus the last 10 days:\n" +
                "           java " + TrendReport.class.getName() + "\n" +
                "               -note=This is <b>so cool</b>!\n" +
                "               \"Performance Trend Report\"\n" +
                "               C:\\Inetpub\\wwwroot\\testperf\\trend\n" +
                "               C:\\Inetpub\\wwwroot\\testperf\\reports\\4.0.xml\n" +
                "               C:\\Inetpub\\wwwroot\\testperf\\reports?today?-10D\n"
        );
        System.exit(1);
    }

    public TrendReportParams(final String[] args) {
        // Parses command line options.
        final List<String> tokens = new ArrayList<String>();
        for (int i = 0; i < args.length; ++ i) {
            if (args[i].startsWith("-")) {
                if (args[i].equals("-help") || args[i].equals("-h") || args[i].equals("-?")) {
                    displayUsageAndExit();
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
        if (tokens.size() < 3) {
            displayUsageAndExit();
        }

        try {
            _title = tokens.get(0);
            _outputDir = tokens.get(1);
            _reportSpecs = ReportSpec.parse(tokens.subList(2, tokens.size()));
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    public List<String> getNotes() {
        return _notes;
    }

    public String getOutputDir() {
        return _outputDir;
    }

    public List<ReportSpec> getReportSpecs() {
        return _reportSpecs;
    }

    public String getTitle() {
        return _title;
    }
    public boolean isNoWarning() {
        return _noWarning;
    }

    public boolean isVerbose() {
        return _verbose;
    }
}
