package com.l7tech.policy.variable;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.DateUtils;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public final String remainingName;
    public static final String SYNTAX_PREFIX = "${";
    public static final String SYNTAX_SUFFIX = "}";
    public static final String REGEX_PREFIX = "(?:\\$\\{)";
    public static final String REGEX_SUFFIX = "(?:\\})";
    public static final Pattern regexPattern = Pattern.compile(REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX);
    public static final Pattern oneVarPattern = Pattern.compile("^" + REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX + "$");

    private Syntax(String name) {
        this.remainingName = name;
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
    public static String getMatchingName(String name, Set<String> names) {
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
                                          final Set<String> names,
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
        add(Goid.class);
    }});

    /**
     * This will return the entire string contained within each set of ${...} in the given strings. The variables names
     * may not exist and variables referenced like ${variablename.mainpart} will be returned as variablename.mainpart.
     * <p/>
     * There is no special behaviour for variables referenced which use selectors.
     *
     * @param values the Strings to find out what variables are referenced using our syntax of ${}. Must not be null. Null elements are ignored.
     * @return all the (distinct) variable names which are contained within our variable reference syntax of ${...} in String s
     * @throws VariableNameSyntaxException If an error occurs
     */
    @NotNull
    public static String[] getReferencedNames( final String... values ) {
        final Set<String> variables = new LinkedHashSet<String>();

        for ( final String value : values ) {
            if ( value != null ) {
                variables.addAll( Arrays.asList( getReferencedNames( value ) ) );
            }
        }

        return variables.toArray( new String[variables.size()] );
    }

    /**
     * This will return the entire string contained within each set of ${...} in the String s. The variables names
     * may not exist and variables referenced like ${variablename.mainpart} will be returned as variablename.mainpart.
     * <p/>
     * There is no special behaviour for variables referenced which use selectors
     *
     * @param s String to find out what variables are referenced using our syntax of ${}. May be null.
     * @return all the variable names which are contained within our variable reference syntax of ${...} in String s.  May be empty but never null.
     * @throws VariableNameSyntaxException If an error occurs e.g. s contains an invalid variable reference e.g. ${var[}
     */
    @NotNull
    public static String[] getReferencedNames(final @Nullable String s) {
        if (s == null) return new String[0];
        return getReferencedNames( s, true );
    }


    /**
     * This will return the entire string contained within each set of ${...} in the String s. The variables names
     * may not exist and variables referenced like ${variablename.mainpart} will be returned as variablename.mainpart.
     * <p/>
     * There is no special behaviour for variables referenced which use selectors
     *
     * @param s String to find out what variables are referenced using our syntax of ${}. May be null.
     * @param strict when strict processing is enabled runtime exceptions will be thrown on error.
     * @return all the variable names which are contained within our variable reference syntax of ${...} in String s
     * @throws VariableNameSyntaxException If strict and an error occurs
     */
    @NotNull
    public static String[] getReferencedNames(final @Nullable String s, final boolean strict) {
        final List<String> vars = new ArrayList<String>();
        if ( s != null ) {
            final Matcher matcher = regexPattern.matcher(s);
            while (matcher.find()) {
                final int count = matcher.groupCount();
                if (count != 1) {
                    throw new IllegalStateException("Expecting 1 matching group, received: " + count);
                }
                String var = matcher.group(1);
                if (var != null) {
                    var = var.trim();
                    try {
                        vars.add(Syntax.parse(var, DEFAULT_MV_DELIMITER).remainingName);
                    } catch ( VariableNameSyntaxException e ) {
                        if ( strict ) throw e;
                    }
                }
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
     * Get an expression that references the given variable.
     *
     * @param name The name (may be null)
     * @return The expression or null if the name was null
     */
    public static String getVariableExpression( final @Nullable String name ) {
        return name == null ? null : SYNTAX_PREFIX + name + SYNTAX_SUFFIX;        
    }

    /**
     * Validate that a string value contains only context variable references.
     * <p/>
     * This method will return false if any variable reference is invalid.
     * <p/>
     * " \t\n\r\f," characters are ignored and allowed. No spaces between variable references is also supported.
     *
     * Note: This method never throws VariableNameSyntaxException
     * @param toValidate String which should only reference variables. Cannot be null
     * @return true if only variables are referenced.
     */
    public static boolean validateStringOnlyReferencesVariables(final String toValidate) {
        try {
            final String[] refNames = Syntax.getReferencedNamesIndexedVarsNotOmitted(toValidate);

            final StringBuilder syntaxString = new StringBuilder();
            for (String refName : refNames) {
                final String error = VariableMetadata.validateName(refName, true);
                if (error != null) {
                    return false;
                }
                syntaxString.append(Syntax.getVariableExpression(refName));
            }

            final StringBuilder userString = new StringBuilder();
            final StringTokenizer st = new StringTokenizer(toValidate, " \t\n\r\f,");
            while (st.hasMoreTokens()) {
                userString.append(st.nextToken());
            }
            return syntaxString.toString().equals(userString.toString());
        } catch (VariableNameSyntaxException e) {
            return false;
        }
    }

    /**
     * Check if an expression contains in invalid variable reference.
     *
     * @param expression String to validate
     * @return true if any refernces are valid, false otherwise.
     */
    public static boolean validateAnyVariableReferences(final @NotNull String expression) {
        try {
            final String[] refNames = Syntax.getReferencedNamesIndexedVarsNotOmitted(expression);
            for (String refName : refNames) {
                final String error = VariableMetadata.validateName(refName, true);
                if (error != null) {
                    return false;
                }
            }

            return true;
        } catch (VariableNameSyntaxException e) {
            return false;
        }
    }

    /**
     * Validate a user supplied string only contains a single variable and nothing else. Supports
     * array syntax.
     *
     * @param value user input to validate for a single reference.
     * @return true if parameter only contains a single single reference to a variable
     * @throws VariableNameSyntaxException if an invalid variable reference is contained within value.
     */
    public static boolean isOnlyASingleVariableReferenced(final @NotNull String value) {
        final String[] referencedNames = Syntax.getReferencedNamesIndexedVarsNotOmitted(value);
        try {
            return referencedNames.length == 1 && value.equals(Syntax.getVariableExpression(referencedNames[0]));
        } catch (VariableNameSyntaxException e) {
            return false;
        }
    }

    /**
     * Validate that a user supplied string consists only of a single variable (not even syntax or a template), and return the string.
     * <P/>
     * The value must contain only the characters "${" as the very first characters followed by a valid context variable
     * name (optionally followed by valid syntax) followed by "}" as the very last character.
     *
     * @param value the value to examine.
     * @return the name of the valid single variable reference, or null if the string did not meet the requirements
     *         (of "${" followed by a valid variable name followed by "}").
     */
    public static String getSingleVariableReferenced(final @NotNull String value) {
        final Matcher matcher = oneVarPattern.matcher(value);
        if (!matcher.matches())
            return null;

        String[] values = getReferencedNames(value, false);
        if (values.length != 1)
            return null;

        return values[0];
    }

    /**
     * Check if an expression references any variables. This method will never throw a VariableNameSyntaxException
     *
     * @param value expression value to check
     * @return true if any variable is referenced, false otherwise. No distinction is made for invalid references, a
     * variable is only referenced if the reference is valid.
     */
    public static boolean isAnyVariableReferenced(final @NotNull String value) {
        boolean varIsReferenced = false;
        try {
            final String[] referencedNames = Syntax.getReferencedNames(value);
            varIsReferenced =  referencedNames.length >0;
        } catch (VariableNameSyntaxException e) {
            // do nothing
        }
        return varIsReferenced;
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
        if (s == null) return new String[0];

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

                if (omitted && varSyntax instanceof MultivalueArraySubscriptSyntax)  continue;

                vars.add(omitted? varSyntax.remainingName : var);
            }
        }
        return vars.toArray(new String[vars.size()]);
    }

    // TODO find out how to move this into Syntax subclasses
    public static Syntax parse(String rawName, final String delimiter) {
        int ppos = rawName.indexOf("|");
        if (ppos == 0) throw new VariableNameSyntaxException("Variable names must not start with '|'");
        if (ppos > 0) {
            return new MultivalueDelimiterSyntax(rawName.substring(0,ppos), rawName.substring(ppos+1), true);
        } else {
            // Can't combine concatenation with subscript (yet -- 2D arrays?)
            int lbpos = rawName.indexOf("[");
            if (lbpos == 0) throw new VariableNameSyntaxException("Variable names must not start with '['");
            if (lbpos > 0) {
                int rbpos = rawName.indexOf("]", lbpos+1);
                if (rbpos == 0) throw new VariableNameSyntaxException("Array subscript must not be empty");
                if (rbpos > 0) {
                    String ssub = rawName.substring(lbpos+1, rbpos);
                    int subscript;
                    try {
                        subscript = Integer.parseInt(ssub);
                    } catch (NumberFormatException e) {
                        throw new VariableNameSyntaxException("Array subscript not an integer", e);
                    }
                    if (subscript < 0) throw new VariableNameSyntaxException("Array subscript must be positive");
                    return new MultivalueArraySubscriptSyntax(rawName.substring(0, lbpos), subscript);
                } else throw new VariableNameSyntaxException("']' expected but not found");
            } else {
                return new MultivalueDelimiterSyntax(rawName, delimiter, false);
            }
        }
    }

    public static Formatter getFormatter( final Object object ) {
        if ( object instanceof Element ) {
            return ELEMENT_FORMATTER;
        } else if (object instanceof Date) {
            return DATE_FORMATTER;
        }
        return DEFAULT_FORMATTER;
    }

    private static final Formatter DATE_FORMATTER = new Formatter() {
        @Override
        public String format(Syntax syntax, Object o, SyntaxErrorHandler handler, boolean strict) {
            if (o == null || !(o instanceof Date)) {
                return "";
            }

            return DateUtils.getZuluFormattedString((Date) o);
        }
    };

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
                String message = handler.handleSuspiciousToString( syntax.remainingName, o.getClass().getName() );
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

    public abstract Object[] filter(Object[] values, SyntaxErrorHandler handler, boolean strict);
    public abstract String format(Object[] values, Formatter formatter, SyntaxErrorHandler handler, boolean strict);

    private static class MultivalueDelimiterSyntax extends Syntax {
        private final String delimiter;
        private final boolean delimiterSpecified;
        private MultivalueDelimiterSyntax(String name, String delimiter, boolean delimiterSpecified) {
            super(name);
            this.delimiter = delimiter;
            this.delimiterSpecified = delimiterSpecified;
        }

        @Override
        public Object[] filter(Object[] values, SyntaxErrorHandler handler, boolean strict) {
            return values;
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

    private static class MultivalueArraySubscriptSyntax extends Syntax {
        private final int subscript;
        private MultivalueArraySubscriptSyntax(String name, int subscript) {
            super(name);
            this.subscript = subscript;
        }

        @Override
        public Object[] filter(Object[] values, SyntaxErrorHandler handler, boolean strict) {
            if (subscript > values.length-1) {
                String message = handler.handleSubscriptOutOfRange( subscript, remainingName, values.length );
                if (strict)
                    throw new VariableNameSyntaxException(message);
                else
                    return null;
            }
            return new Object[] { values[subscript] };
        }

        @Override
        public String format(Object[] values, Formatter formatter, SyntaxErrorHandler handler, boolean strict) {
            if (values == null || values.length != 1) return "";

            return formatter.format(this, values[0], handler, strict);
        }
    }

}
