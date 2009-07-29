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
     * Finds the longest period-delimited subname of the provided name that matches at least one of the known names
     * in the provided Set, or null if no match can be found.
     * @param name the full name to search for (all leading and trailing spaces will be trimmed)
     * @param names the set of names to search within
     * @return the longest subset of the provided Name that matches one of the Set members, or null if no match is found
     */
    public static String getMatchingName(String name, Set names) {
        return getMatchingName(name, names, false, true);
    }

    /**
     * Finds the longest period-delimited subname of the provided name that matches at least one of the known names
     * in the provided Set, or null if no match can be found.
     * @param name the full name to search for
     * @param names the set of names to search within
     * @param preserveCase whether to preserve the case of the provided variable name
     * @param trim whether to trim out trailing and leading spaces
     * @return the longest subset of the provided Name that matches one of the Set members, or null if no match is found
     */
    public static String getMatchingName(String name, Set names, boolean preserveCase, boolean trim) {
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

    private static final Set usefulToStringClasses = Collections.unmodifiableSet(new HashSet() {{
        add(String.class);
        add(Number.class);
        add(Long.TYPE);
        add(Integer.TYPE);
        add(Boolean.class);
        add(Boolean.TYPE);
    }});

    public static String[] getReferencedNames(String s) throws VariableNameSyntaxException {
        if (s == null) throw new IllegalArgumentException();

        ArrayList vars = new ArrayList();
        Matcher matcher = regexPattern.matcher(s);
        while (matcher.find()) {
            int count = matcher.groupCount();
            if (count != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+count);
            }
            String var = matcher.group(1);
            if (var != null) var = var.trim();
            vars.add(Syntax.parse(var, DEFAULT_MV_DELIMITER).remainingName);
        }
        return (String[]) vars.toArray(new String[0]);
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

    public static final Formatter DEFAULT_FORMATTER = new Formatter() {
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
            if (values[0] instanceof Element) {
                StringBuilder sb = new StringBuilder();
                OutputFormat format = new OutputFormat();
                format.setOmitXMLDeclaration(true);
                XMLSerializer serializer = new XMLSerializer(format);
                for (int i = 0; i < values.length; i++) {
                    Element value = (Element)values[i];
                    if (value != null) {
                        try {
                            StringWriter writer = new StringWriter();
                            serializer.setOutputCharStream(writer);
                            serializer.serialize(value);
                            sb.append(writer.toString());

                            if (delimiterSpecified && i < values.length - 1) {
                                sb.append(delimiter);
                            }
                        } catch(IOException e) {
                            // Skip to the next Element
                        }
                    }
                }
                return sb.toString();
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < values.length; i++) {
                    Object value = values[i];
                    String formatted = value == null ? null : formatter.format(this, value, handler, strict);
                    if (formatted != null) {
                        if (delimiter.length() == 1 && formatted.contains(delimiter)) {
                            formatted = formatted.replace("\\", "\\\\");
                            formatted = formatted.replace(delimiter, "\\" + delimiter);
                        }
                        sb.append(formatted);
                    }
                    if (i < values.length-1) sb.append(delimiter);
                }
                return sb.toString();

            }
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

            if (values[0] instanceof Element) {
                try {
                    OutputFormat format = new OutputFormat();
                    format.setOmitXMLDeclaration(true);
                    XMLSerializer serializer = new XMLSerializer(format);
                    StringWriter writer = new StringWriter();
                    serializer.setOutputCharStream(writer);
                    serializer.serialize((Element)values[0]);
                    return writer.toString();
                } catch(IOException e) {
                    return "";
                }
            } else {
                return formatter.format(this, values[0], handler, strict);
            }
        }
    }

}
