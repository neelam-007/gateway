/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.io.csv;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link CSVWriter} and round trip of {@link CSVWriter} + {@link CSVReader}.
 */
public class CSVWriterTest extends TestCase {

    private static final String ALL_CHARS;
    static {
        final StringBuilder tmp = new StringBuilder();
        for (int i = 0; i <= 255; ++i) tmp.append((char)i);
        tmp.append("\u4E2D\u6587");  // Unicode for "Chinese".
        ALL_CHARS = tmp.toString();
    }

    public static Test suite() {
        return new TestSuite(CSVWriterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSingleRecord() throws IOException {
        for (String eor : new String[]{"\n", "\r\n"}) {
            final CSVPreference p = new CSVPreference('"', ',', eor);
            doSingleRecord(p, "1 string", "A", "A");
            doSingleRecord(p, "1 space", "\" \"", " ");
            doSingleRecord(p, "leading space", "\" A\"", " A");
            doSingleRecord(p, "trailing space", "\"A \"", "A ");
            doSingleRecord(p, "embedded space", "A A", "A A");
            doSingleRecord(p, "1 empty string", "", "");
            doSingleRecord(p, "separator character", "\",\"", ",");
            doSingleRecord(p, "quote character", "\"\"\"\"", "\"");
            doSingleRecord(p, "2 strings", "a,b", "a", "b");
            doSingleRecord(p, "2 empty strings", ",", "", "");
            doSingleRecord(p, "empty string and string", ",A", "", "A");
            doSingleRecord(p, "string and empty string", "A,", "A", "");

            doSingleRecord(p, "end-of-record string", "\"" + eor + "\"", eor);
            for (int i = 0; i < eor.length(); ++i) {
                final String c = Character.toString(eor.charAt(i));
                doSingleRecord(p, "end-of-record character", "\"" + c + "\"", c);
            }

            final String everything = " " + ALL_CHARS + eor + " ";
            final String csv = '"' + everything.replace("\"", "\"\"") + '"';
            doSingleRecord(p, "1 string with everything", csv, everything);
        }
    }

    private static void doSingleRecord(final CSVPreference p,
                                       final String message,
                                       final String expectedEncodedCSV,
                                       final String... values)
            throws IOException {
        // Tests CSVWriter.encode serialization to expected CSV.
        assertEquals(message + " [encode]", expectedEncodedCSV, CSVWriter.encode(p, values));

        // Tests CSVWriter.writeRecord serialization to expected CSV.
        final StringWriter sw = new StringWriter();
        final CSVWriter w = new CSVWriter(sw, p);
        w.writeRecord(values);
        final String expectedStreamCSV = expectedEncodedCSV + p.getEndOfRecord();
        assertEquals(message + " [writeRecord]", expectedStreamCSV, sw.toString());

        // Tests CSVReader.readRecord deserialization to original. (i.e., round trip test)
        final StringReader sr = new StringReader(sw.toString());
        final CSVReader r = new CSVReader(sr, p);
        final List<String> output = r.readRecord(true);
        final List<String> input = Arrays.asList(values);
        assertEquals(message + " [readRecord]", input, output);
    }

    public void testMultiRecord() throws IOException {
        for (String eor : new String[]{"\n", "\r\n"}) {
            final CSVPreference p = new CSVPreference('"', ',', eor);
            doMultiRecord(p, "2 rows, 1 string", "A" + eor + "B" + eor, new String[]{"A"}, new String[]{"B"});
            doMultiRecord(p, "2 rows, 1 space", "\" \"" + eor + "\" \"" + eor, new String[]{" "}, new String[]{" "});
            doMultiRecord(p, "2 rows, leading space", "\" A\"" + eor + "\" B\"" + eor, new String[]{" A"}, new String[]{" B"});
            doMultiRecord(p, "2 rows, trailing space", "\"A \"" + eor + "\"B \"" + eor, new String[]{"A "}, new String[]{"B "});
            doMultiRecord(p, "2 rows, embedded space", "A A" + eor + "B B" + eor, new String[]{"A A"}, new String[]{"B B"});
            doMultiRecord(p, "2 rows, 1 empty string", eor + eor, new String[]{""}, new String[]{""});
            doMultiRecord(p, "2 rows, separator character", "\",\"" + eor + "\",\"" + eor, new String[]{","}, new String[]{","});
            doMultiRecord(p, "2 rows, quote character", "\"\"\"\"" + eor + "\"\"\"\"" + eor, new String[]{"\""}, new String[]{"\""});
            doMultiRecord(p, "2 rows, 2 strings", "A,a" + eor + "B,b" + eor, new String[]{"A", "a"}, new String[]{"B", "b"});
            doMultiRecord(p, "2 rows, 2 empty strings", "," + eor + "," + eor, new String[]{"", ""}, new String[]{"", ""});
            doMultiRecord(p, "2 rows, empty string and string", ",A" + eor + ",B" + eor, new String[]{"", "A"}, new String[]{"", "B"});
            doMultiRecord(p, "2 rows, string and empty string", "A," + eor + "B," + eor, new String[]{"A", ""}, new String[]{"B", ""});

            doMultiRecord(p, "2 rows, end-of-record string", "\"" + eor + "\"" + eor + "\"" + eor + "\"" + eor, new String[]{eor}, new String[]{eor});
            for (int i = 0; i < eor.length(); ++i) {
                final String c = Character.toString(eor.charAt(i));
                doMultiRecord(p, "2 rows, end-of-record character", "\"" + c + "\"" + eor + "\"" + c + "\"" + eor, new String[]{c}, new String[]{c});
            }

            // Tests values with every character.
            final String everything = " " + ALL_CHARS + eor + " ";
            final String csv = '"' + everything.replace("\"", "\"\"") + '"';
            doMultiRecord(p, "2 rows, 2 strings with everything", csv + ',' + csv + eor + csv + ',' + csv + eor, new String[]{everything, everything}, new String[]{everything, everything});
        }
    }

    public static void doMultiRecord(final CSVPreference p,
                                     final String message,
                                     final String expectedFileCSV,
                                     final String[]... records)
            throws IOException {
        // Tests serialization to expected CSV.
        final StringWriter sw = new StringWriter();
        final CSVWriter w = new CSVWriter(sw, p);
        for (String[] record : records) {
            w.writeRecord(record);
        }
        assertEquals(message + " [writeRecord]", expectedFileCSV, sw.toString());

        // Tests deserialization to original. (i.e., round trip test)
        final StringReader sr = new StringReader(sw.toString());
        final CSVReader r = new CSVReader(sr, p);
        List<String> output;
        int i = 0;
        while ((output = r.readRecord(true)) != null) {
            final List<String> input = Arrays.asList(records[i]);
            assertEquals(message + " [readRecord]", input, output);
            ++i;
        }
        assertEquals(message + " [no. of readRecord]", records.length, i);
    }

    /**
     * Tests 4-dimensional data.
     */
    public void test4D() throws IOException {
        final CSVPreference PREF[] = new CSVPreference[]{
            new CSVPreference('"' , ',', "\n"),     // 1st level CSV control characters
            new CSVPreference('\'', ':', "\n"),     // 2nd level CSV control characters
            new CSVPreference('^' , '|', "\n"),     // 3rd level CSV control characters
        };

        final List<Object> inputSubSubRecord = new ArrayList<Object>();
        inputSubSubRecord.add("");
        inputSubSubRecord.add(" ");
        inputSubSubRecord.add("Bart");
        inputSubSubRecord.add("Lisa \",':^|");

        final List<Object> inputSubRecord = new ArrayList<Object>();
        inputSubRecord.add("");
        inputSubRecord.add(" ");
        inputSubRecord.add("Homer");
        inputSubRecord.add("Marge \",':^|");
        inputSubRecord.add(inputSubSubRecord);

        final List<Object> inputRecord = new ArrayList<Object>();
        inputRecord.add("");
        inputRecord.add(" ");
        inputRecord.add("Abraham");
        inputRecord.add("Mona \",':^|");
        inputRecord.add(inputSubRecord);

        final List<List<Object>> input = new ArrayList<List<Object>>();
        input.add(inputRecord);
        input.add(inputRecord);

        // Serializes.
        final List<Object> inputSubRecord_ = new ArrayList<Object>(inputSubRecord);
        inputSubRecord_.set(4, CSVWriter.encode(PREF[2], inputSubSubRecord));   // Change List into encoded CSV.
        final List<Object> inputRecord_ = new ArrayList<Object>(inputRecord);
        inputRecord_.set(4, CSVWriter.encode(PREF[1], inputSubRecord_));    // Change List into encoded CSV.
        final StringWriter sw = new StringWriter();
        final CSVWriter w = new CSVWriter(sw, PREF[0]);
        for (int i = 0; i < input.size(); ++i) {
            w.writeRecord(inputRecord_);
        }

        // Deserializes.
        final List<List<Object>> output = new ArrayList<List<Object>>();
        final StringReader sr = new StringReader(sw.toString());
        final CSVReader r = new CSVReader(sr, PREF[0]);
        List<String> outputRecord_ = null;
        while ((outputRecord_ = r.readRecord(true)) != null) {
            final List<String> outputSubRecord_ = CSVReader.decode(PREF[1], true, outputRecord_.get(4));
            final List<String> outputSubSubRecord_ = CSVReader.decode(PREF[2], true, outputSubRecord_.get(4));

            final List<Object> outputRecord = new ArrayList<Object>(outputRecord_);
            final List<Object> outputSubRecord = new ArrayList<Object>(outputSubRecord_);
            outputRecord.set(4, outputSubRecord);
            final List<Object> outputSubSubRecord = new ArrayList<Object>(outputSubSubRecord_);
            outputSubRecord.set(4, outputSubSubRecord);
            output.add(outputRecord);
        }

        assertEquals("4-dimensional", input, output);
    }
}
