/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.io.csv;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * CSV writer serializes records in CSV.
 *
 * @since SecureSpan 4.4
 * @author rmak
 * @see CSVPreference
 * @see CSVReader
 */
public class CSVWriter {
    protected final CSVPreference _preference;
    protected final Writer _writer;

    /** Position (zero-offset) of value to be written next. */
    protected int _valuesPosition;

    /** Number of full records already written. */
    protected int _recordCount;

    /** True if the first value has been written but end-of-record has not. */
    protected boolean _pendingEndOfRecord;

    /**
     * Constructs CSVWriter for serializing CSV to a character stream.
     *
     * @param writer        stream to write from
     * @param preference    specifies CSV control characters
     */
    public CSVWriter(final Writer writer, final CSVPreference preference) {
        _writer = writer;
        _preference = preference;
    }

    /**
     * Encode a list of values into CSV format. No end-of-record string is appended.
     *
     * @param preference    specifies CSV control characters
     * @param values        values to encode using their toString() method
     * @return CSV
     */
    public static String encode(final CSVPreference preference, final List<Object> values) {
        return encode(preference, values.toArray());
    }

    /**
     * Encode a list of values into CSV format. No end-of-record string is appended.
     *
     * @param preference    specifies CSV control characters
     * @param values        values to encode using their toString() method
     * @return CSV
     */
    public static String encode(final CSVPreference preference, final Object... values) {
        final String[] strings = new String[values.length];
        for (int i = 0; i < values.length; ++i) {
            strings[i] = values[i].toString();
        }
        return encode(preference, strings);
    }

    /**
     * Encode a list of strings into CSV format. No end-of-record string is appended.
     *
     * @param preference    specifies CSV control characters
     * @param strings       strings to encode
     * @return CSV
     */
    public static String encode(final CSVPreference preference, final String... strings) {
        if (strings.length == 0) {
            throw new IllegalArgumentException("Must have at least one value.");
        }

        final StringBuilder result = new StringBuilder();
        result.append(quote(preference, strings[0]));
        for (int i = 1; i < strings.length; ++i) {
            result.append(preference.getSeparator())
                  .append(quote(preference, strings[i]));
        }
        return result.toString();
    }

    /**
     * Serializes a single value, as part of the current record, to the output stream.
     *
     * @param value     the value to serialize using its toString() method
     * @throws IOException if I/O error when writing to the character stream
     */
    public void writeValue(final Object value) throws IOException {
        writeValue(value.toString());
    }

    /**
     * Serializes a single value, as part of the current record, to the output stream.
     *
     * @param value     the value to serialize
     * @throws IOException if I/O error when writing to the character stream
     */
    public void writeValue(final String value) throws IOException {
        if (_valuesPosition > 0) {
            _writer.write(_preference.getSeparator());
        }
        _writer.write(quote(_preference, value.toString()));
        ++_valuesPosition;
        _pendingEndOfRecord = true;
    }

    /**
     * Ends the current record being written.
     *
     * @throws IOException if I/O error when writing to the character stream
     * @throws IllegalStateException if no value has been written for the current record
     */
    public void writeEndOfRecord() throws IOException {
        if (_valuesPosition == 0) {
            throw new IllegalStateException("A record must have at least one value.");
        }

        if (_pendingEndOfRecord) {
            _writer.write(_preference.getEndOfRecord());
            ++_recordCount;
            _pendingEndOfRecord = false;
            _valuesPosition = 0;
        }
    }

    /**
     * Serializes one record to the character stream. An end-of-record is always
     * appended.
     *
     * @param values    values of the record
     * @throws IOException if I/O error when writing to the character stream
     * @throws IllegalStateException
     */
    public void writeRecord(final List<Object> values) throws IOException {
        writeRecord(values.toArray());
    }

    /**
     * Serializes one record to the character stream. An end-of-record is always
     * appended.
     *
     * @param values    values of the record
     * @throws IOException if I/O error when writing to the character stream
     * @throws IllegalStateException
     */
    public void writeRecord(final Object... values) throws IOException {
        final String[] strings = new String[values.length];
        for (int i = 0; i < values.length; ++i) {
            strings[i] = values[i].toString();
        }
        writeRecord(strings);
    }

    /**
     * Serializes one record to the character stream. An end-of-record is always
     * appended.
     *
     * @param strings   values of the record
     * @throws IOException if I/O error when writing to the character stream
     * @throws IllegalStateException
     */
    public void writeRecord(final String... strings) throws IOException {
        if (strings.length == 0) {
            throw new IllegalArgumentException("A record must have at least one value.");
        }

        for (String s : strings) {
            writeValue(s);
        }
        writeEndOfRecord();
    }

    /**
     * @return the current records count
     */
    public int getRecordCount() {
        return _recordCount;
    }

    /**
     * Sets the current records count.
     *
     * @param n     the new records count
     */
    public void initRecordCount(final int n) {
        _recordCount = n;
    }

    /**
     * Applies quotes and escapes to a value if neccessary.
     *
     * @param preference    specifies CSV control characters
     * @param s             the value
     * @return quoted and/or escaped value; or original string if no treatment neccessary
     */
    protected static String quote(final CSVPreference preference, final String s) {
        if (s.length() == 0) return s;

        final char quote = preference.getQuote();
        final char separator = preference.getSeparator();
        final String endOfRecord = preference.getEndOfRecord();

        boolean needsQuote = false;
        boolean hasQuote = false;

        for (int i = 0; i < s.length(); ++i) {
            final char c = s.charAt(i);
            if (c == quote) {
                needsQuote = hasQuote = true;
                break;
            } else if (c == separator) {
                needsQuote = true;
            } else {
                for (int j = 0; j < endOfRecord.length(); ++j) {
                    if (c == endOfRecord.charAt(j)) {
                        needsQuote = true;
                    }
                }
            }
        }

        if (s.charAt(0) == ' ' || s.charAt(s.length() - 1) == ' ') {
            needsQuote = true;
        }

        if (hasQuote) {
            final StringBuilder result = new StringBuilder(s.length());
            result.append(quote);
            for (int i = 0; i < s.length(); ++i) {
                final char c = s.charAt(i);
                if (c == quote) {
                    result.append(quote);
                }
                result.append(c);
            }
            result.append(quote);
            return result.toString();
        } else if (needsQuote) {
            return quote + s + quote;
        } else {
            return s;
        }
    }
}
