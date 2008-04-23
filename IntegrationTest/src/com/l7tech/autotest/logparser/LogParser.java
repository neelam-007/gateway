package com.l7tech.autotest.logparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

/**
 * Scans the JUnit log files for the previous run and the current run and produces
 * two output files. The first output file is the list of new failures and the second
 * output file is the list of resolved failures.
 */
public class LogParser {
    /**
     * Creates a new LogParser object.
     */
    public LogParser() {
    }

    /**
     * Loads the list of expected failures. None of the expected failures will be
     * printed to the output files.
     */
    protected HashMap<String, String> loadExpectedFailures() {
        return loadFailuresFromFile(Main.getProperties().getProperty("expectedFailures.filename"), null);
    }

    /**
     * Loads the list of failures from the previous run of AutoTest.
     */
    protected HashMap<String, String> loadPreviousFailures() {
        return loadFailuresFromDirectory(Main.getProperties().getProperty("previousResults.path"));
    }

    /**
     * Loads the list of failures from the current run of AutoTest.
     */
    protected HashMap<String, String> loadCurrentFailures() {
        return loadFailuresFromDirectory(Main.getProperties().getProperty("currentResults.path"));
    }

    /**
     * Scans the specified directory looking for files named TEST-*. It loads all
     * failures from these files and returns a HashMap keyed by the fully qualified
     * method name and the values are the JUnit error messages.
     * @param directory The directory to scan
     * @return A HashMap of the failures that were found
     */
    private HashMap<String, String> loadFailuresFromDirectory(String directory) {
        File dir = new File(directory);
        HashMap<String, String> failures = new HashMap<String, String>();

        if(!dir.exists() || !dir.isDirectory()) {
            return failures;
        }

        final String patternString = Main.getProperties().getProperty("junit.logfile.pattern");
        File[] junitLogFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.matches(patternString);
            }
        });

        Pattern pattern = Pattern.compile("TEST-(.*)\\.txt");
        for(File file : junitLogFiles) {
            Matcher matcher = pattern.matcher(file.getName());
            String prefix = null;
            if(matcher.matches()) {
                prefix = matcher.group(1);
            }
            failures.putAll(loadFailuresFromFile(file.getAbsolutePath(), prefix));
        }

        return failures;
    }

    /**
     * Scans the specified file looking for failed test cases. Returns a HashMap
     * keyed by the fully qualified method name and the values are the JUnit error
     * messages.
     * @param filename The file to scan
     * @param prefix The fully qualified class name or null
     * @return A HashMap of the failures that were found
     */
    private HashMap<String, String> loadFailuresFromFile(String filename, String prefix) {
        Pattern pattern = Pattern.compile(Main.getProperties().getProperty("junit.logfile.testcase.pattern"));
        HashMap<String, String> failures = new HashMap<String, String>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));

            String testCase = null;
            String line = br.readLine();
            while(line != null) {
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    testCase = (prefix != null) ? prefix + "." + matcher.group(1) : matcher.group(1);
                } else if(testCase != null) {
                    if(failures.containsKey(testCase)) {
                        failures.put(testCase, failures.get(testCase) + "\n" + line);
                    } else {
                        failures.put(testCase, line);
                    }
                }

                line = br.readLine();
            }
        } catch(Exception e) {
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch(IOException e) {
                }
            }
        }

        return failures;
    }
}
