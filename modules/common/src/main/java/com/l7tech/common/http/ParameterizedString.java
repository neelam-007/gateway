package com.l7tech.common.http;

import com.l7tech.util.ConfigFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for working with query strings and form post data.
 *
 * <p>The aim is for compatibility with the Servlet API, not strictly
 * "correct" behaviour.</p>
 *
 * <p>For example, the W3C recommends allowing ';' in place of '&', this
 * class does not support this.</p>
 *
 * <p>Note that this class does not behave in a way that is 100% compatible
 * with the Servlet API (e.g. this class will pass back the full undecoded
 * text when url decoding fails)</p>
 */
public class ParameterizedString {
    private static final int MAX_FIELD_LENGTH = ConfigFactory.getIntProperty( "com.l7tech.http.maxParameterLength", 1000000 );
    private static final boolean SEMICOLON_AS_SEPARATOR = ConfigFactory.getBooleanProperty("com.l7tech.http.useSemicolonAsSeparator", false);
    //- PUBLIC

    /**
     * Create a new parameterized string from the given text.
     *
     * @param paramStr the raw string.
     * @throws IllegalArgumentException if the string is illegal
     */
    public ParameterizedString(String paramStr) {
        this(paramStr, false);
    }

    /**
     * Create a new parameterized string from the given text.
     *
     * <p>When using a strict parse invalid characters are disallowed as
     * well as invalid url encoding.</p>
     *
     * @param paramStr the raw string.
     * @param strict true for a stricter interpretation of a legal paramStr
     * @throws IllegalArgumentException if the string is illegal
     */
    public ParameterizedString(String paramStr, boolean strict) {
        parameters = new LinkedHashMap<String,String[]>();
        parseParameterString(parameters, paramStr, strict);
    }

    /**
     * Get the names of the parameters.
     *
     * @return the parameter names in order of appearance.
     */
    public Set getParameterNames() {
        return Collections.unmodifiableSet(parameters.keySet());
    }

    /**
     * Check if the given parameter exists.
     *
     * @param name the parameter
     * @return true if exists
     */
    public boolean parameterExists(String name) {
        return parameters.containsKey(name);
    }

    /**
     * Check if the given parameter has only one value.
     *
     * @param name the parameter
     * @return true if exists and has only one value
     */
    public boolean parameterHasSingleValue(String name) {
        return getParameterValues(name).length==1;
    }

    /**
     * Get the values for the given parameter.
     *
     * @param name the parameter name
     * @return the value list (never null)
     */
    public String[] getParameterValues(String name) {
        String[] values = parameters.get(name);
        if(values==null) {
            values = new String[0];
        }
        return values;
    }

    /**
     * Get the first value for the given parameter.
     *
     * @param name the parameter name
     * @return the value (may be null)
     */
    public String getParameterValue(String name) {
        String value = null;
        String[] values = parameters.get(name);
        if(values!=null && values.length>0) {
            value = values[0];
        }
        return value;
    }

    /**
     * Get the map of parameter names to value arrays.
     *
     * @return the parameter map.
     */
    public Map getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Like the one from the Servlet API (HttpUtils).
     *
     * @param queryString the string to parse
     * @return the parameter map.
     * @throws IllegalArgumentException if the string is illegal
     */
    public static Map<String,String[]> parseQueryString(String queryString) {
        return parseQueryString(queryString, false);
    }

    /**
     * Like the one from the Servlet API (HttpUtils).
     *
     * @param queryString the string to parse
     * @param strict true for a strict parse
     * @return the parameter map.
     * @throws IllegalArgumentException if the string is illegal
     */
    public static Map<String,String[]> parseQueryString(String queryString, boolean strict) {
        Map<String,String[]> paramMap = new LinkedHashMap<String,String[]>();
        parseParameterString(paramMap, queryString, strict);
        return paramMap;
    }

    /**
     * Parse the given parameter string into the given map.
     *
     * @param holder the Map to put the parameters into
     * @param paramStr the string to parse
     * @param strict true for a strict parse
     * @throws IllegalArgumentException if the string is illegal
     */
    public static void parseParameterString(Map<String, String[]> holder, String paramStr, boolean strict) {
        if(holder==null) throw new IllegalArgumentException("holder must not be null.");
        if(paramStr!=null) {
            doParse(holder, paramStr, SEMICOLON_AS_SEPARATOR ?"&;":"&", strict);
        }
    }

    /**
     * Create a string representation of this parameter string.
     *
     * @return the string
     */
    public String toString() {
        return toString(parameters);
    }

    //- PRIVATE

    /**
     * It is debatable whether to allow "/", "\", ":", "@", "|", "{", "}", "[", "]", "=", ";", "?", "#", """, "`", "^", "<", ">" and "~" in the qs names.
     */
    private static Pattern validcomponent = Pattern.compile("[a-zA-Z0-9$\\-_.+!*'(),%/\\\\:@|{}\\[\\]=;?#\"`^<>~]*");

    /**
     * Map of parsed parameters name (string) -> values (string array)
     */
    private Map<String,String[]> parameters;

    /**
     * Check that the given parameter string nvp does not contain invalid characters.
     */
    private static void checkContents(String text, String fulltext) {
        if (text.length() > MAX_FIELD_LENGTH)
            throw new IllegalArgumentException("Field exceeds configured maximum field length of " + MAX_FIELD_LENGTH + " characters");
        Matcher matcher = validcomponent.matcher(text);
        if(!matcher.matches()) {
            throw new IllegalArgumentException("Invalid character in '"+text+"'; '"+fulltext+"'");
        }
    }

    /**
     * URL decode the given string
     */
    private static String decode(String text, boolean strict) {
        String decoded = text;
        try {
            decoded = URLDecoder.decode(text, HttpConstants.ENCODING_UTF8);
        }
        catch(IllegalArgumentException iae) {
            if(strict) throw new IllegalArgumentException("Error during URL decoding of '"+text+"': " + iae.getMessage());
        }
        catch(UnsupportedEncodingException uee) {
            throw new IllegalStateException("Encoding not available: " + HttpConstants.ENCODING_UTF8);
        }
        return decoded;
    }

    /**
     * Do the work ...
     */
    private static void doParse(Map<String, String[]> holder, String paramStr, String separators, boolean strict) {
        StringTokenizer strtok = new StringTokenizer(paramStr, separators);
        while(strtok.hasMoreTokens()) {
            String nvp = strtok.nextToken();

            int splitIndex = nvp.indexOf('=');
            if(splitIndex<0) {
                continue; // ignore parameter with no value
            }

            String name = nvp.substring(0, splitIndex);
            String value = nvp.substring(splitIndex+1);

            if(strict) {
                checkContents(name, paramStr);
                checkContents(value, paramStr);
            }

            name = decode(name, strict);
            value = decode(value, strict);

            String[] values = holder.get(name);
            if(values==null) {
                values = new String[1];
                values[0] = value;
            }
            else {
                String[] newValues = new String[values.length+1];
                System.arraycopy(values, 0, newValues, 0, values.length);
                newValues[values.length] = value;
                values = newValues;
            }

            holder.put(name, values);
        }

    }

    /**
     * Create a string representation of the given map.
     */
    private static String toString(Map<String,String[]> map) {
        StringBuffer sb = new StringBuffer(128);

        sb.append("ParameterizedString()[");
        for(Iterator<Map.Entry<String,String[]>> ei=map.entrySet().iterator(); ei.hasNext();) {
            Map.Entry<String,String[]> entry = ei.next();
            sb.append("'");
            sb.append(entry.getKey());
            sb.append("'=");
            String[] values = entry.getValue();
            for(int v=0; v<values.length; v++) {
                if(v!=0) sb.append(",");
                sb.append("'");
                sb.append(values[v]);
                sb.append("'");
            }
            if(ei.hasNext()) sb.append("; ");
        }
        sb.append("]");

        return sb.toString();
    }
}
