package com.l7tech.config.client.options;

import com.l7tech.util.InetAddressUtil;

import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Type for a configuration option.
 *
 * <p>A type defines a validation (default) rule and visibility for an option.</p>
 *
 * @author steve
 */
public enum OptionType {
    
    //- PUBLIC

    /**
     * A host name
     */
    HOSTNAME("^[\\[a-zA-Z0-9][a-zA-Z0-9\\-\\_.:\\]]{0,254}"),

    /**
     * A IPv4 or IPv6 address
     */
    IP_ADDRESS("^(?:(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|\\*|localhost)$", false, new WildcardIpFormat(), String.class) {
        @Override
        protected boolean nonRegexMatch(String input) {
            // IPv4 regex failed, expect an IPv6 literal; prevent InetAddress.getByName from doing a lookup
            return InetAddressUtil.isValidIpv6Address(input);
        }},

    /**
     * A password (should not be displayed in UI or recorded in logs)
     */
    PASSWORD("^[\\p{Graph}\\p{Blank}]{0,255}$", true, null, String.class),

    /**
     * A file path
     */
    PATH("^[\\p{Graph}\\p{Blank}]{0,1024}$"),

    /**
     * A port number
     */
    PORT("^(?:6(?:[1-4]\\d{3}|(?:5(?:[0-4]\\d{2}|5(?:[0-2]\\d|3[0-5]))))|[1-5]\\d{4}|(?!0)\\d{2,4}|[1-9])$", false, new DecimalFormat("#0"), Integer.class),   // 1 - 65535

    /**
     * A positive integer
     */
    INTEGER("^(?:[12]\\d{9}|[1-9]\\d{0,8}|0)$", false, new DecimalFormat("#0"), Integer.class),   // 0 - MAX

    /**
     * Any text
     */
    TEXT("^[\\p{Graph}\\p{Blank}]{0,10240}$"),

    /**
     * A username / login
     */
    USERNAME("^[\\p{Graph}\\p{Blank}]{1,255}$"),

    /**
     * A true / false value
     */
    BOOLEAN("^(?:[YyNn]|[Yy][Ee][Ss]|[Nn][Oo]|[TtFf]|[Tt][Rr][Uu][Ee]|[Ff][Aa][Ll][Ss][Ee])$", false, new BooleanFormat(), Boolean.class),

    /**
     * A date / time value (the regex could do with some tightening up)
     */
    TIMESTAMP("^[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]$", false, new DateTimeFormat(), Date.class);

    /**
     * Get the Format associated with the type.
     *
     * @return The format or null if none.
     */
    public Format getFormat() {
        return format;    
    }

    /**
     * Get the class associated with the type.
     *
     * @return The class.
     */
    public Class getTypeClass() {
        return typeClass;
    }

    /**
     * True if this option should be hidden in the GUI.
     *
     * @return true if hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @return true if the supplied input matches the expected format, false otherwise
     */
    public boolean matches(String input) {
        boolean regexMatch = pattern != null && pattern.matcher(input).matches();
        return regexMatch || nonRegexMatch(input);
    }

    // - PROTECTED

    /**
     * Should be overriden by types for applying an alternate (non-regex) logic for input matching,
     * if the type was not configured with a regular expression or if the regular expression didn't match.
     *
     * @return true if the input matched the additional logic, false otherwise
     */
    protected boolean nonRegexMatch(String input) {
        return false;
    }

    //- PRIVATE
        
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private Pattern pattern;
    private Format format;
    private Class typeClass;
    private boolean hidden;
    
    private OptionType( String pattern ) {
        this( pattern, false, null, String.class );
    }
    
    private OptionType( String patternString, boolean hidden, Format format, Class typeClass ) {
        if (patternString != null)
            pattern = Pattern.compile(patternString);
        this.hidden = hidden;
        this.format = format;
        this.typeClass = typeClass;
    }

    private static final class BooleanFormat extends Format {
        @Override
        public StringBuffer format( final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
            return toAppendTo.append(((Boolean)obj).booleanValue() ? "Yes" : "No");
        }

        @Override
        public Object parseObject( final String source, final ParsePosition pos) {
            Boolean result = Boolean.FALSE;

            if ( source != null) {
                pos.setIndex(source.length());

                if ( source.toLowerCase().startsWith("y") || source.toLowerCase().startsWith("t") ) {
                    result = Boolean.TRUE;
                }
            }

            return result;
        }
    }

    private static final class DateTimeFormat extends SimpleDateFormat {
        public DateTimeFormat() {
            super(DATE_FORMAT);
        }
    }

    private static final class WildcardIpFormat extends Format {
        @Override
        public StringBuffer format( final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
            if ( InetAddressUtil.isAnyHostAddress(String.valueOf(obj)) ) {
                toAppendTo.append( "*" );
            } else if ( InetAddressUtil.isLoopbackAddress(String.valueOf(obj)) ) {
                toAppendTo.append( "localhost" );            
            } else {
                toAppendTo.append( obj );
            }
            
            return toAppendTo;
        }

        @Override
        public Object parseObject( final String source, final ParsePosition pos) {
            String result = source;

            if ( source != null ) {
                pos.setIndex(source.length());

                if ( "*".equals(source) ) {
                   result = InetAddressUtil.getAnyHostAddress();
                } else if ( "localhost".equals(source) ) {
                    result = InetAddressUtil.getLocalHostAddress();
                }
            }

            return result;
        }
    }
}
