/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.io.csv;

/**
 * For specifying control characters used in CSV.
 *
 * <h3>CSV Format Specification</h3>
 * There is no authoritative CSV format.
 * This is our specification.
 * It is based on Excel's import and export behaviour.
 * <ul>
 *   <li>CSV data contains zero or more <i>record</i>s.
 *   <li>Each record contains one or more <i>value</i>s.
 *   <li>Records are separated by an <i>end-of-record</i> string.
 *   <li>Values are separated by a <i>separator</i>.
 *   <li>Spaces at the beginning and end of record, and before and after a
 *       separator are ignored.
 *   <li>If a value contains the separator character, any end-of-record characters,
 *       leading or trailing spaces, it must be quoted using the <i>quote character</i>.
 *   <li>If a value contains the quote character, the value must be quoted and
 *       the quote character represented by 2 consecutive quote characters.
 *   <li>Any value may always be quoted even if not neccessary.
 *   <li>The separator character and the quote character must be different.
 *   <li>End-of-record must not contain the separator character or the quote
 *       character.
 *   <li>End-of-record must not be empty string or contain the space character.
 *   <li>Special cases:
 *     <ul>
 *       <li>The end-of-record is always required if a record contains an empty
 *           value only.
 *       <li>If a file ends with an end-of-record, it is not interpreted to have
 *           a trailing record with an empty value only.
 *     </ul>
 *     Consequently,
 *     <ul>
 *       <li>An empty file is deserialized into zero record.
 *       <li>An end-of-record at the beginning of file or immediately after
 *           another end-of-record is deserialized into a record with an empty
 *           value only.
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Strict Syntax Mode Parsing</h3>
 * Strict syntax rule requires:
 * <ol>
 *   <li>A quoted value must be followed by optional spaces and then a separator or end-of-record.</li>
 *   <li>A quoted value must have a closing quote before end-of-record
 *   <li>If end-of-record is multi-character, it must be used in its entirety.
 * </ol>
 * When strict mode parsing is turned off, the above rules are relaxed:
 * <ol>
 *   <li>In (1) above, other characters are ignored.</li>
 *   <li>In (2) above, the missing closing quote is assumed.
 *   <li>In (3) above, truncated end-of-record is allowed.
 * </ol>
 *
 * <h3>Excel Compatibility</h3>
 * For compatibility with Excel:
 * <ul>
 *   <li>the separator is preferrably comma (,), but can be a tab or any printable character.
 *   <li>the quote character is either double quote (") or single quote (')
 *   <li>the end-of-record is linefeed (0x0A)
 * </ul>
 * Beware that Excel have trouble importing values containing character code 0x09 to 0x1F.
 * All other character values appears OK including Unicode characters.
 *
 * <h3>Unicode Compatibility</h3>
 * If values contain Unicode characters, you should use UTF-8 encoded character streams.
 *
 * <h3>Multi-dimensional Data</h3>
 * Although in its basic form, CSV are used to encode 2-dimensional data,
 * higher dimensional data can still be serialized using recursive encoding as
 * long as different separator and quote character are used in each dimension.
 *
 * <p>For example, we have a record where the third value is an array of 2 subvalues:
 * <blockquote>
 * A B [x y]
 * </blockquote>
 * This is how we may serialize it:
 * <blockquote><pre>
 * final CSVPreference PREF_0 = new CSVPreference('"' , ',', "\n");
 * final CSVPreference PREF_1 = new CSVPreference('\'', ':', "\n");
 * final String A = "A", B = "B";
 * final String[] subvalues = {"x", "y"};
 * final List&lt;Object> record = new ArrayList();
 * record.add(A);
 * record.add(B);
 * record.add(CVSWriter.encode(PREF_1, subvalues);
 * final CSVWriter w = new CSVWriter(outputWriter, PREF_0);
 * w.writeRecord(record);
 * </pre></blockquote>
 * This is how we may deserialize it:
 * <blockquote><pre>
 * final CSVReader r = new CSVReader(inputReader, PREF_0);
 * final List&lt;String> record = r.readRecord(true);
 * final String A = record.get(0);
 * final String B = record.get(1);
 * final String[] subvalues = CSVReader.decode(PREF_1, true, record.get(2)).toArray(new String[2]);
 * </pre></blockquote>
 * The serialized CSV can be imported to Excel using 2 rounds of <i>Text to Columns</i> conversion.
 *
 * @since SecureSpan 4.4
 * @author rmak
 * @see CSVReader
 * @see CSVWriter
 */
public class CSVPreference {
    protected final char _separator;
    protected final char _quote;
    protected final String _endOfRecord;

    /**
     * Specifies control characters.
     *
     * @param quote         the quote character
     * @param separator     the separator character
     * @param endOfRecord   the end-of-record string
     * @throws IllegalArgumentException if specification rules are violated
     */
    public CSVPreference(final char quote,
                         final char separator,
                         final String endOfRecord) {
        if (quote == separator) throw new IllegalArgumentException("Quote (" + quote + ") character cannot be the same as separator (" + separator + ")");
        for (int i = 0; i < endOfRecord.length(); ++i) {
            if (endOfRecord.charAt(i) == quote) throw new IllegalArgumentException("End-of-record cannot contain the quote character: " + quote);
            if (endOfRecord.charAt(i) == separator) throw new IllegalArgumentException("End-of-record cannot contain the separator character: " + separator);
        }
        if (endOfRecord.length() == 0) throw new IllegalArgumentException("End-of-record cannot be empty string.");
        if (endOfRecord.contains(" ")) throw new IllegalArgumentException("End-of-record cannot be contain the space character.");

        _separator = separator;
        _quote = quote;
        _endOfRecord = endOfRecord;
    }

    public String getEndOfRecord() {
        return _endOfRecord;
    }

    public char getQuote() {
        return _quote;
    }

    public char getSeparator() {
        return _separator;
    }
}
