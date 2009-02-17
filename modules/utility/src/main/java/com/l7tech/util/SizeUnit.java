package com.l7tech.util;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.text.NumberFormat;

/**
 *
 */
public class SizeUnit {
    private static int next = 0;
    public static final SizeUnit BYTES = new SizeUnit(next++, "bytes", "B", 1);
    public static final SizeUnit KIBIBYTES = new SizeUnit(next++, "kilobytes", "KB", 1024);
    public static final SizeUnit MEBIBYTES = new SizeUnit(next++, "megabytes", "MB", 1024*1024);
    public static final SizeUnit GIBIBYTES = new SizeUnit(next++, "gigabytes", "GB", 1024*1024*1024);

    public static final SizeUnit[] ALL = new SizeUnit[] { BYTES, KIBIBYTES, MEBIBYTES, GIBIBYTES };

    private static final Pattern numberPattern = Pattern.compile("(\\d*\\.?\\d*)(\\p{Lower}{0,2})");

    private final int num;
    private final String name;
    private final String abbreviationid;
    private final String abbreviation;
    private final int multiplier;

    private static final Map<String,SizeUnit> valuesByAbbrev = new HashMap<String,SizeUnit>();
    static {
        for (SizeUnit timeUnit : ALL) {
            valuesByAbbrev.put(timeUnit.abbreviationid, timeUnit);
        }
    }

    private SizeUnit( final int num, final String name, final String abbrev, final int multiplier ) {
        this.num = num;
        this.name = name;
        this.abbreviationid = abbrev.toLowerCase();
        this.abbreviation = abbrev;
        this.multiplier = multiplier;
    }

    public String getName() {
        return name;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public long toMillis( final long value ) {
        return value * multiplier;
    }

    public String toString() {
        return name;
    }

    public int compareTo( final Object o ) {
        SizeUnit that = (SizeUnit)o;
        return this.multiplier - that.multiplier;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    protected Object readResolve() throws ObjectStreamException {
        return ALL[num];
    }

    public static SizeUnit fromAbbreviation( final String value ) {
        return valuesByAbbrev.get(value);
    }

    /**
     * Parses a size expressed as a number (which may contain periods or commas) followed by a one- or
     * three-letter, case-insensitive unit abbreviation, e.g. "10KiB" is 10 kibibytes, resulting in 10240.
     * 
     * @param value The string to be parsed
     * @param unsuffixedUnit The SizeUnit to assume is in use when no unit suffix is present
     * @return the equivalent size in bytes
     */
    public static long parse( final String value, final SizeUnit unsuffixedUnit ) throws NumberFormatException {
        if (value == null) throw new NullPointerException();
        if (value.length() == 0) throw new NumberFormatException("Empty strings are not supported");
        if (value.length() > 20) throw new NumberFormatException("Strings with more than 20 characters are not supported");

        try {
            Matcher mat = numberPattern.matcher(value.toLowerCase().replace(",", "").replace(" ", ""));
            if (!mat.matches() && mat.groupCount() != 2)
                throw new NumberFormatException("Value doesn't match expected format");

            final String snum = mat.group(1);

            final SizeUnit unit;
            String maybeUnit = mat.group(2);
            SizeUnit su = valuesByAbbrev.get(maybeUnit);
            if (su == null) su = unsuffixedUnit;
            unit = su;

            BigDecimal bd = new BigDecimal(snum); // OK to throw NFE
            return (long) (bd.doubleValue() * unit.multiplier);
        } catch (IllegalStateException ise) {
            throw new NumberFormatException(ise.getMessage());
        } catch (IndexOutOfBoundsException ioobe) {
            throw new NumberFormatException(ioobe.getMessage());
        }
    }

    /**
     * Parses a size expressed as a number (which may contain periods or commas) followed by a one- or
     * three-letter, case-insensitive unit abbreviation, e.g. "10KiB" is 10 kibibytes, resulting in 10240.
     *
     * <p>If there is no suffix, then the value is assumed to be in bytes.</b>
     *
     * @param value The string to be parsed
     * @return the equivalent size in bytes
     */
    public static long parse( final String value ) {
        return parse(value, BYTES);
    }

    /**
     * Format the given value using an "appropriate" unit.
     *
     * TODO display decimal place for values under 10?
     *
     * @param value The value to format
     * @return The formatted string
     */
    public static String format( final long value ) {
        long displayValue;
        String displayUnit;
        if ( value < KIBIBYTES.multiplier ) {
            displayValue = value / BYTES.multiplier;
            displayUnit = BYTES.abbreviation;
        } else if ( value < MEBIBYTES.multiplier ) {
            displayValue = value / KIBIBYTES.multiplier;
            if ( value % KIBIBYTES.multiplier != 0 ) displayValue++; // round up
            displayUnit = KIBIBYTES.abbreviation;
        } else if ( value < GIBIBYTES.multiplier ) {
            displayValue = value / MEBIBYTES.multiplier;
            if ( value % MEBIBYTES.multiplier != 0 ) displayValue++; // round up
            displayUnit = MEBIBYTES.abbreviation;
        } else {
            displayValue = value / GIBIBYTES.multiplier;
            if ( value % GIBIBYTES.multiplier != 0 ) displayValue++; // round up
            displayUnit = GIBIBYTES.abbreviation;
        }

        return NumberFormat.getIntegerInstance().format( displayValue ) + displayUnit;
    }
}
