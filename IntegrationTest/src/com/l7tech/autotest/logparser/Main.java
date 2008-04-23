package com.l7tech.autotest.logparser;

import java.util.HashMap;
import java.util.Properties;
import java.util.Map;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 18-Dec-2007
 * Time: 10:27:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    private static Properties properties = new Properties();

    /**
     * Returns the properties for this application.
     *
     * @return The properties
     */
    public static Properties getProperties() {
        return properties;
    }

    /**
     * Writes out the output files.
     */
    private static void writeOutResults(HashMap<String, String> expectedFailures,
                                        HashMap<String, String> currentFailures,
                                        HashMap<String, String> previousFailures)
    {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(properties.getProperty("newFailures.filename"));

            pw.print("------------------------------------------\n");
            pw.print("             New Failures\n");
            pw.print("------------------------------------------\n");

            for(Map.Entry<String, String> entry : currentFailures.entrySet()) {
                if(!expectedFailures.containsKey(entry.getKey()) && !previousFailures.containsKey(entry.getKey())) {
                    pw.format("%s()\n%s\n", entry.getKey(), entry.getValue());
                }
            }

            pw.print("\n------------------------------------------\n");
            pw.print("             All Failures\n");
            pw.print("------------------------------------------\n");

            for(Map.Entry<String, String> entry : currentFailures.entrySet()) {
                if(!expectedFailures.containsKey(entry.getKey())) {
                    pw.format("%s()\n%s\n", entry.getKey(), entry.getValue());
                }
            }
        } catch(Exception e) {
        } finally {
            if(pw != null) {
                pw.close();
            }
        }

        pw = null;
        try {
            pw = new PrintWriter(properties.getProperty("resolvedFailures.filename"));

            pw.print("------------------------------------------\n");
            pw.print("           Resolved Failures\n");
            pw.print("------------------------------------------\n");

            for(Map.Entry<String, String> entry : previousFailures.entrySet()) {
                if(!expectedFailures.containsKey(entry.getKey()) && !currentFailures.containsKey(entry.getKey())) {
                    pw.format("%s()\n%s\n", entry.getKey(), entry.getValue());
                }
            }

            pw.print("\n------------------------------------------\n");
            pw.print("         All Previous Failures\n");
            pw.print("------------------------------------------\n");

            for(Map.Entry<String, String> entry : previousFailures.entrySet()) {
                if(!expectedFailures.containsKey(entry.getKey())) {
                    pw.format("%s()\n%s\n", entry.getKey(), entry.getValue());
                }
            }
        } catch(Exception e) {
        } finally {
            if(pw != null) {
                pw.close();
            }
        }
    }

    /**
     * Logs into the SSG, and revokes the certificates.
     * @param args
     */
    public static void main(String[] args) {
        try {
            properties.load(new FileReader("src/autotest_log_parser.properties"));
        } catch(IOException e) {
            System.err.println("Failed to load properties file.");
            System.exit(-1);
        }

        LogParser logParser = new LogParser();
        HashMap<String, String> expectedFailures = logParser.loadExpectedFailures();
        HashMap<String, String> previousFailures = logParser.loadPreviousFailures();
        HashMap<String, String> currentFailures = logParser.loadCurrentFailures();

        MailParser mailParser = new MailParser();
        mailParser.parseMessages();
        mailParser.clearInbox();
        currentFailures.putAll(mailParser.getUnmatchedExpectedMessages());
        currentFailures.putAll(mailParser.getUnexpectedMessages());

        writeOutResults(expectedFailures, currentFailures, previousFailures);
    }
}
