/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.io.csv;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link CSVReader}.
 * Round trip of {@link CSVWriter} + {@link CSVReader} is done in {@link CSVWriterTest}.
 */
public class CSVReaderTest extends TestCase {

    public static Test suite() {
        return new TestSuite(CSVReaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSingleRecord() throws IOException {
        // Canonical CSV is already tested in round trip test of CSVWriterTest.testSingleRecord.
        // Here we'll test non-canonical CSV.

        final CSVPreference p = new CSVPreference('"', ',', "\n");
        doSingleRecord(p, "ignore leading space", " A", "A");
        doSingleRecord(p, "ignore leading space", " A\n", "A");
        doSingleRecord(p, "ignore leading space", " A,", "A", "");
        doSingleRecord(p, "ignore leading space", " A, B", "A", "B");
        doSingleRecord(p, "ignore leading space", " \"A\"", "A");
        doSingleRecord(p, "ignore leading space", " \"A\", \"B\"", "A", "B");
        doSingleRecord(p, "ignore trailing space", "A ", "A");
        doSingleRecord(p, "ignore trailing space", "A \n", "A");
        doSingleRecord(p, "ignore trailing space", "A ,", "A", "");
        doSingleRecord(p, "ignore trailing space", "A ,B ", "A", "B");
        doSingleRecord(p, "ignore trailing space", "\"A\" ", "A");
        doSingleRecord(p, "ignore trailing space", "\"A\" ,\"B\" ", "A", "B");
    }

    private static void doSingleRecord(final CSVPreference p,
                                       final String message,
                                       final String record,
                                       String... expectedValues)
            throws IOException {
        List<String> expectedList = null;
        if (expectedValues.length != 0) {
            expectedList = Arrays.asList(expectedValues);
        }

        // Tests CSVReader.decode.
        assertEquals(message + " [decode]", expectedList, CSVReader.decode(p, true, record));

        // Tests CSVReader.readRecord.
        final StringReader sr = new StringReader(record);
        final CSVReader r = new CSVReader(sr, p);
        assertEquals(message + " [readRecord]", expectedList, r.readRecord(true));
    }

    public void testSpecialCase() throws IOException {
        final CSVPreference p = new CSVPreference('"', ',', "\n");
        assertEquals("no record", null, CSVReader.decode(p, true, ""));
        assertEquals("1 empty string", Arrays.asList(""), CSVReader.decode(p, true, "\n"));
    }

    /**
     * Tests the strict syntax option using sloppy csv.
     */
    public void testStrictSyntax() throws IOException {
        final CSVPreference p = new CSVPreference('"', ',', "\n");
        doStrictSyntax(p, "unexpected character after a quoted value", "\"A\"x", "A");
        doStrictSyntax(p, "unexpected character after a quoted value", "\"A\" x", "A");
        doStrictSyntax(p, "unexpected character after a quoted value", "\"A\" x ", "A");
        doStrictSyntax(p, "unexpected character after a quoted value", "\"A\"x,B", "A", "B");
        doStrictSyntax(p, "unexpected character after a quoted value", "\"A\" x,B", "A", "B");
        doStrictSyntax(p, "unexpected character after a quoted value", "\"A\" x ,B", "A", "B");
        doStrictSyntax(p, "missing closing quote", "\"A", "A");

        final CSVPreference p2 = new CSVPreference('"', ',', "\r\n");
        doStrictSyntax(p2, "incomplete end-of-record", "\r", "");
        doStrictSyntax(p2, "incomplete end-of-record", " \r", "");
        doStrictSyntax(p2, "incomplete end-of-record", "A\r", "A");
        doStrictSyntax(p2, "incomplete end-of-record", "\"A\"\r", "A");
        doStrictSyntax(p2, "incomplete end-of-record", "A,B\r", "A", "B");
    }

    /**
     * Asserts that parsing a sloppy CSV causes exception in strict syntax mode
     * but succeeds in relaxed mode.
     *
     * @param p                 CSV preference
     * @param message           description text
     * @param csv               sloppy csv with non-fatal syntax mistakes
     * @param expectedValues    expected result from parsing
     */
    private static void doStrictSyntax(final CSVPreference p,
                                       final String message,
                                       final String csv,
                                       String... expectedValues) throws IOException {
        List<String> expectedList = null;
        if (expectedValues.length != 0) {
            expectedList = Arrays.asList(expectedValues);
        }

        try {
            assertEquals(message + " [strict syntax]", expectedList, CSVReader.decode(p, true, csv));
            fail("CSVException expected due to strict syntax mode parsing failure.");
        } catch (CSVException e) {
            // expected
        }

        try {
            assertEquals(message + " [relaxed syntax]", expectedList, CSVReader.decode(p, false, csv));
        } catch (CSVException e) {
            fail("CSVException not expected in relaxed syntax mode parsing.");
        }
    }
}
