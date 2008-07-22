/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.io.csv;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV reader deserializes CSV into records.
 *
 * @since SecureSpan 4.4
 * @author rmak
 * @see CSVPreference
 * @see CSVWriter
 */
public class CSVReader {
    protected final CSVPreference _preference;
    protected final Reader _reader;

    /**
     * Constucts a CSVReader for deserializing CSV from a character stream.
     *
     * @param reader        stream to read from; should be buffered
     * @param preference    specifies CSV control characters
     */
    public CSVReader(final Reader reader, final CSVPreference preference) {
        _reader = reader;
        _preference = preference;
    }

    /**
     * Deserializes one record from a string. This will stop reading from the
     * string as soon as end-of-record is found.
     *
     * @param preference    specifies CSV control characters
     * @param strictSyntax  true to enforce strict syntax mode parsing
     * @param csv           the CSV to decode
     * @return values in the record; null if <code>csv</code> is empty
     * @throws CSVException if <code>strictSyntax</code> is true and violated, or
     *                      if a malformed end-of-record is encountered
     * @throws IOException if I/O error when reading from the stream
     */
    public static List<String> decode(final CSVPreference preference,
                                      final boolean strictSyntax,
                                      final String csv)
            throws IOException {
        final CSVReader reader = new CSVReader(new StringReader(csv), preference);
        return reader.readRecord(strictSyntax);
    }

    private enum ParsingState {
        /** Expecting an unquoted value, quoted value, separator, or end-of-record. */
        BEFORE_VALUE,
        /** Just finished parsing a quoted value. */
        AFTER_VALUE,
        /** In midst of parsing a quoted value. */
        UNQUOTED_VALUE,
        /** In midst of parsing an unquoted value. */
        QUOTED_VALUE,
        /** In midst of parsing end-of-record string. */
        END_OF_RECORD,
    }

    /**
     * Read and parse for one record.
     *
     * @param strictSyntax  true to enforce strict syntax mode parsing
     * @return values in the record; null if already at end-of-stream
     * @throws CSVException if <code>strictSyntax</code> is true and violated, or
     *                      if a malformed end-of-record is encountered
     * @throws IOException if I/O error when reading from the stream
     */
    public List<String> readRecord(final boolean strictSyntax) throws IOException {
        final char quote = _preference.getQuote();
        final char separator = _preference.getSeparator();
        final String endOfRecord = _preference.getEndOfRecord();

        final List<String> result = new ArrayList<String>();
        final StringBuilder value = new StringBuilder();
        int endOfRecordCharsFound = 0;
        boolean wasQuote = false;   // true if a quote character was encountered when parsing a quoted value; and it waits to be determined if it is a closing quote or an escaped quote
        ParsingState state = ParsingState.BEFORE_VALUE;
        int charRead = -1;
        Character charPending = null;

        outer:
        while (true) {
            char c;
            if (charPending == null) {
                if ((charRead = _reader.read()) == -1) break;
                c = (char)charRead;
            } else {
                c = charPending;
                charPending = null;
            }

            switch (state) {
                case BEFORE_VALUE: {
                    if (c == ' ') {
                        // Ignore leading spaces.
                    } else if (c == separator) {
                        // It was an empty value.
                        result.add("");
                    } else if (c == quote) {
                        state = ParsingState.QUOTED_VALUE;
                    } else if (c == endOfRecord.charAt(0)) {
                        endOfRecordCharsFound = 1;
                        state = ParsingState.END_OF_RECORD;
                        result.add("");
                        if (endOfRecord.length() == 1) break outer;
                    } else {
                        state = ParsingState.UNQUOTED_VALUE;
                        value.append(c);
                    }
                    break;
                }

                case AFTER_VALUE: {
                    if (c == ' ') {
                        // Ignore trailing spaces.
                    } else if (c == separator) {
                        state = ParsingState.BEFORE_VALUE;
                    } else if (c == endOfRecord.charAt(0)) {
                        endOfRecordCharsFound = 1;
                        state = ParsingState.END_OF_RECORD;
                        if (endOfRecord.length() == 1) break outer;
                    } else {
                        if (strictSyntax) throw new CSVException("Unexpected character after a quoted value (expected: space/separator/end-of-record, actual: " + c + "(hex value " + Integer.toHexString(charRead).toUpperCase() + ")");
                        // else ignore
                    }
                    break;
                }

                case UNQUOTED_VALUE: {
                    if (c == separator) {
                        // Trim trailing spaces
                        for (int j = value.length() - 1; j >= 0 && value.charAt(j) == ' '; --j) {
                            value.deleteCharAt(j);
                        }
                        result.add(value.toString());
                        value.setLength(0);
                        state = ParsingState.BEFORE_VALUE;
                    } else if (c == endOfRecord.charAt(0)) {
                        // Trim trailing spaces
                        for (int j = value.length() - 1; j >= 0 && value.charAt(j) == ' '; --j) {
                            value.deleteCharAt(j);
                        }
                        result.add(value.toString());
                        value.setLength(0);
                        endOfRecordCharsFound = 1;
                        state = ParsingState.END_OF_RECORD;
                        if (endOfRecord.length() == 1) break outer;
                    } else {
                        value.append(c);
                    }
                    break;
                }

                case QUOTED_VALUE: {
                    if (wasQuote) {
                        if (c == quote) {
                            // It was an escaped quote.
                        } else {
                            // It was a closing quote.
                            value.deleteCharAt(value.length() - 1);
                            result.add(value.toString());
                            value.setLength(0);
                            state = ParsingState.AFTER_VALUE;

                            charPending = c;
                        }
                        wasQuote = false;
                    } else {
                        if (c == quote) {
                            wasQuote = true;
                        }
                        value.append(c);
                    }
                    break;
                }

                case END_OF_RECORD: {
                    if (c == endOfRecord.charAt(endOfRecordCharsFound)) {
                        ++endOfRecordCharsFound;
                        if (endOfRecordCharsFound == endOfRecord.length()) break outer;
                    } else {
                        throw new CSVException("Unexpected character when parsing character position " + endOfRecordCharsFound + " in end-of-record: " + c);
                    }
                    break;
                }

                default: throw new RuntimeException("INTERNAL ERROR: missing switch case");
            }
        }

        if (charRead == -1) { // End-of-stream reached before end-of-record.
            switch (state) {
                case BEFORE_VALUE: {
                    if (result.size() == 0) {
                        // Special case: no record data, will return null.
                    } else {
                        result.add("");
                    }
                    break;
                }

                case AFTER_VALUE: {
                    // Nothing to add.
                    break;
                }

                case UNQUOTED_VALUE: {
                    // Trim trailing spaces
                    for (int j = value.length() - 1; j >= 0 && value.charAt(j) == ' '; --j) {
                        value.deleteCharAt(j);
                    }

                    result.add(value.toString());
                    break;
                }

                case QUOTED_VALUE: {
                    if (wasQuote) {
                        // A closing quote.
                        value.deleteCharAt(value.length() - 1);
                        result.add(value.toString());
                    } else {
                        if (strictSyntax) throw new CSVException("Missing closing quote.");
                        result.add(value.toString());
                    }
                    break;
                }

                case END_OF_RECORD: {
                    if (strictSyntax) throw new CSVException("Incomplete end-of-record.");
                    break;
                }

                default: throw new RuntimeException("INTERNAL ERROR: missing switch case");
            }
        }

        if (result.size() == 0) {
            // Special case.
            return null;
        }
        return result;
    }
}
