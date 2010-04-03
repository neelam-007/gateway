/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author alex
*/
public abstract class Syntax {
    public static final String DEFAULT_MV_DELIMITER = ", ";

    public static final String SYNTAX_PREFIX = "${";
    public static final String SYNTAX_SUFFIX = "}";
    public static final String REGEX_PREFIX = "(?:\\$\\{)";
    public static final String REGEX_SUFFIX = "(?:\\})";
    public static final Pattern regexPattern = Pattern.compile(REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX);
    public static final Pattern oneVarPattern = Pattern.compile("^" + REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX + "$");

    private final String variableName;
    private final String variableSelector;
    private final boolean array;

    private Syntax( final String variableName,
                    final String variableSelector,
                    final boolean array )  {
        this.variableName = variableName;
        this.variableSelector = variableSelector;
        this.array = array;
    }

    /**
     * Indicates that an array subscript was seen for the variable.
     *
     * @return true if the variable name is followed by an array subscript.
     */
    public boolean isArray() {
        return array;
    }

    /**
     * Get the name of the variable matched by this syntax.
     *
     * @return The variable name.
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * The full selector to use for this syntax.
     *
     * @return The variable selector.
     */
    public String getVariableSelector() {
        return variableSelector;
    }

    /**
     * Finds the longest period-delimited subname of the provided name that
     * matches at least one of the known names in the provided Set, or null if
     * no match can be found.
     *
     * <p>Note that the name must be a variable name after Syntax processing,
     * i.e. it should not have an array suffix or delimiter specification.</p>
     *
     * @param name the full name to search for (all leading and trailing spaces will be trimmed)
     * @param names the set of names to search within
     * @return the longest subset of the provided Name that matches one of the Set members, or null if no match is found
     */
    public static String getMatchingName(String name, Set names) {
        return getMatchingName(name, names, false, true);
    }

    /**
     * Finds the longest period-delimited subname of the provided name that
     * matches at least one of the known names in the provided Set, or null if
     * no match can be found.
     *
     * <p>Note that the name must be a variable name after Syntax processing,
     * i.e. it should not have an array suffix or delimiter specification.</p>
     *
     * @param name the full name to search for
     * @param names the set of names to search within
     * @param preserveCase whether to preserve the case of the provided variable name
     * @param trim whether to trim out trailing and leading spaces
     * @return the longest subset of the provided Name that matches one of the Set members, or null if no match is found
     */
    public static String getMatchingName( final String name,
                                          final Set names,
                                          final boolean preserveCase,
                                          final boolean trim) {
        String mutateName = preserveCase ? name : name.toLowerCase();
        mutateName = trim ? mutateName.trim() : mutateName;
        final String lname = mutateName;
        if (names.contains(lname)) return lname;

        int pos = lname.length();
        do {
            String tryname = lname.substring(0, pos);
            if (names.contains(tryname)) return tryname;
            pos = lname.lastIndexOf(".", pos-1);
        } while (pos > 0);

        return null;
    }

    private static interface Formatter {
        String format(Syntax syntax, Object o, SyntaxErrorHandler handler, boolean strict);
    }

    private static final Set<Class<?>> usefulToStringClasses = Collections.unmodifiableSet(new HashSet<Class<?>>() {{
        add(String.class);
        add(Number.class);
        add(Long.TYPE);
        add(Integer.TYPE);
        add(Boolean.class);
        add(Boolean.TYPE);
    }});

    /**
     * This will return the entire string contained within each set of ${...} in the String s. The variables names
     * may not exist and variables referenced like ${variablename.mainpart} will be returned as variablename.mainpart.
     * <p/>
     * There is no special behaviour for variables referenced which use selectors
     *
     * @param s String to find out what variables are referenced using our syntax of ${}
     * @return all the variable names which are contained within our variable reference syntax of ${...} in String s
     */
    public static String[] getReferencedNames(final String s) {
        if (s == null) throw new IllegalArgumentException();

        final List<String> vars = new ArrayList<String>();
        final Matcher matcher = regexPattern.matcher(s);
        while (matcher.find()) {
            final int count = matcher.groupCount();
            if (count != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: " + count);
            }
            String var = matcher.group(1);
            if (var != null) {
                var = var.trim();
                vars.add(Syntax.parse(var, DEFAULT_MV_DELIMITER).getVariableName());
            }
        }
        return vars.toArray(new String[vars.size()]);
    }

    /**
     * Get all variables referenced from String s with the difference that if the variable is indexed, it
     * will be omitted in the returned array.
     * e.g. if String s contains the string ${IDS[1]} the returned array will not contain IDS
     * @param s String to find out what non indexed variables are referenced from it
     * @return the list of all non indexed variables
     * @throws VariableNameSyntaxException
     */
    public static String[] getReferencedNamesIndexedVarsOmitted(String s) throws VariableNameSyntaxException {
        return getReferencedNamesWithIndexedVars(s, true);
    }

    /**
     * Get all variables referenced from String s, no matter if context variable is indexed or not.
     * @param s: A string to find out what variables are referenced from it
     * @return a list of all variables
     * @throws VariableNameSyntaxException
     */
    public static String[] getReferencedNamesIndexedVarsNotOmitted(String s) throws VariableNameSyntaxException {
        return getReferencedNamesWithIndexedVars(s, false);
    }

    /**
     * Get should-be-processed variables referenced from the string s.  Any variable being able to be processed depends on
     * the flag "omitted", which indicates whether indexed variables will be omitted or not.
     * @param s: A string to find out what variables are referenced from it
     * @param omitted: A boolean flag to determine whether indexed variables will be omitted or not.
     * @return a list of all should-be-processed variables.
     * @throws VariableNameSyntaxException
     */
    private static String[] getReferencedNamesWithIndexedVars(String s, boolean omitted) throws VariableNameSyntaxException {
        if (s == null) throw new IllegalArgumentException();

        List<String> vars = new ArrayList<String>();
        Matcher matcher = regexPattern.matcher(s);
        while (matcher.find()) {
            int count = matcher.groupCount();
            if (count != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+count);
            }
            String var = matcher.group(1);
            if (var != null) {
                var = var.trim();

                final Syntax varSyntax = Syntax.parse(var, DEFAULT_MV_DELIMITER);

                if (omitted && varSyntax.isArray())  continue;

                vars.add(omitted? varSyntax.getVariableName() : var);
            }
        }
        return vars.toArray(new String[vars.size()]);
    }

    // TODO find out how to move this into Syntax subclasses
    public static Syntax parse(String rawName, final String delimiter) {
        final int ppos = rawName.indexOf("|");
        if (ppos == 0) throw new VariableNameSyntaxException("Variable names must not start with '|'");
        if (ppos > 0) {
            return new MultivalueDelimiterSyntax(rawName.substring(0,ppos),null,false, rawName.substring(ppos+1), true);
        } else {
            final int lbpos = rawName.indexOf("[");
            if (lbpos >= 0) {
                validateArraySubscripts( rawName );
                return new MultivalueDelimiterSyntax(rawName.substring( 0, lbpos ), rawName, true, delimiter, false);
            } else {
                return new MultivalueDelimiterSyntax(rawName, null, false, delimiter, false);
            }
        }
    }

    private static void validateArraySubscripts( final String rawName ) {
        int leftBracketIndex = rawName.indexOf("[");
        while ( leftBracketIndex >= 0 ) {
            if (leftBracketIndex == 0) {
                throw new VariableNameSyntaxException("Variable names must not start with '['");
            }

            final int rightBracketIndex = rawName.indexOf("]", leftBracketIndex+1);
            if ( rightBracketIndex == leftBracketIndex + 1 ) throw new VariableNameSyntaxException("Array subscript must not be empty");
            if ( rightBracketIndex > 0 ) {
                final String subscriptText = rawName.substring(leftBracketIndex+1, rightBracketIndex);
                final int subscript;
                try {
                    subscript = Integer.parseInt(subscriptText);
                } catch (NumberFormatException e) {
                    throw new VariableNameSyntaxException("Array subscript not an integer", e);
                }
                if ( subscript < 0 ) {
                    throw new VariableNameSyntaxException("Array subscript must be positive");
                }
            } else {
                throw new VariableNameSyntaxException("']' expected but not found");
            }

            leftBracketIndex = rawName.indexOf("[", rightBracketIndex + 1);
        }
    }

    public static Formatter getFormatter( final Object object ) {
        if ( object instanceof Element ) {
            return ELEMENT_FORMATTER;
        }
        return DEFAULT_FORMATTER;
    }

    private static final Formatter DEFAULT_FORMATTER = new Formatter() {
        @Override
        public String format(Syntax syntax, Object o, SyntaxErrorHandler handler, boolean strict) {
            if (o == null) return "";

            boolean ok = false;
            //noinspection ForLoopReplaceableByForEach
            for (Iterator i = usefulToStringClasses.iterator(); i.hasNext();) {
                Class clazz = (Class) i.next();
                if (clazz.isAssignableFrom(o.getClass())) {
                    ok = true;
                    break;
                }
            }

            if (!ok) {
                String message = handler.handleSuspiciousToString( syntax.getVariableSelector(), o.getClass().getName() );
                if (strict) throw new VariableNameSyntaxException( message );
            }
            return o.toString();
        }
    };

    private static final Formatter ELEMENT_FORMATTER = new Formatter() {
        @Override
        public String format( final Syntax syntax, final Object o, final SyntaxErrorHandler handler, final boolean strict ) {
            if ( !(o instanceof Element) ) return "";

            try {
                OutputFormat format = new OutputFormat();
                format.setOmitXMLDeclaration(true);
                XMLSerializer serializer = new XMLSerializer(format);
                StringWriter writer = new StringWriter();
                serializer.setOutputCharStream(writer);
                serializer.serialize((Element)o);
                return writer.toString();
            } catch(IOException e) {
                return "";
            }
        }
    };

    public interface SyntaxErrorHandler {
        String handleSuspiciousToString( String remainingName, String className );
        String handleSubscriptOutOfRange( int subscript, String remainingName, int length );
        String handleBadVariable(String s);
        String handleBadVariable(String s, Throwable t);
    }

    public abstract String format(Object[] values, Formatter formatter, SyntaxErrorHandler handler, boolean strict);

    private static class MultivalueDelimiterSyntax extends Syntax {
        private final String delimiter;
        private final boolean delimiterSpecified;
        private MultivalueDelimiterSyntax(String name, String selector, boolean array, String delimiter, boolean delimiterSpecified) {
            super(name,selector==null ? name : selector,array);
            this.delimiter = delimiter;
            this.delimiterSpecified = delimiterSpecified;
        }

        @Override
        public String format(final Object[] values, final Formatter formatter, final SyntaxErrorHandler handler, final boolean strict) {
            if (values == null || values.length == 0) return "";

            // DOM Elements do not have a delimiter unless one was explicitly specified
            final String valueDelimiter;
            if (values[0] instanceof Element && !delimiterSpecified) {
                valueDelimiter = "";
            } else {
                valueDelimiter = delimiter;
            }

            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                String formatted = value == null ? null : formatter.format(this, value, handler, strict);
                if (formatted != null) {
                    if (valueDelimiter.length() == 1 && formatted.contains(valueDelimiter)) {
                        formatted = formatted.replace("\\", "\\\\");
                        formatted = formatted.replace(valueDelimiter, "\\" + valueDelimiter);
                    }
                    sb.append(formatted);
                }
                if (i < values.length-1) sb.append(valueDelimiter);
            }
            return sb.toString();
        }
    }
}
