/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class replaces the variables placeholders in the string that is passed to the
 * {@link ExpandVariables#process} method.
 * The variables placeholders are by default of format <code>${var.name}</code> where
 * <code>var.name</code> is the variable name.
 * The variables are passed in the <code>Map</code> of string key-value pairs. The default
 * variables are passed in constructor and optional overriding variables can be passed in
 * {@link ExpandVariables#process(String, java.util.Map)} method.
 */
public final class ExpandVariables {
    private static final Logger logger = Logger.getLogger(ExpandVariables.class.getName());

    public static final String SYNTAX_PREFIX = "${";
    public static final String SYNTAX_SUFFIX = "}";
    
    private static final String REGEX_PREFIX = "(?:\\$\\{)";
    private static final String REGEX_SUFFIX = "(?:\\})";
    private static final Pattern regexPattern = Pattern.compile(REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX);
    private static final Pattern oneVarPattern = Pattern.compile("^" + REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX + "$");
    private static final String DEFAULT_DELIMITER = ", ";

    private static interface Formatter {
        String format(VariableNameSyntax syntax, Object o);
    }

    private static final Formatter DEFAULT_FORMATTER = new Formatter() {
        public String format(VariableNameSyntax syntax, Object o) {
            if (!(Number.class.isAssignableFrom(o.getClass()) || CharSequence.class.isAssignableFrom(o.getClass())))
                logger.warning("Variable '" + syntax.remainingName + "' is a " + o.getClass().getName() + ", not a String or Number; using .toString() instead");

            return o.toString();
        }
    };

    public static String[] getReferencedNames(String s) {
        if (s == null) {
            throw new IllegalArgumentException();
        }
        ArrayList vars = new ArrayList();
        Matcher matcher = regexPattern.matcher(s);
        while (matcher.find()) {
            int count = matcher.groupCount();
            if (count != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+count);
            }
            String var = matcher.group(1);
            vars.add(parseNameSyntax(var).remainingName);
        }
        return (String[]) vars.toArray(new String[0]);
    }

    public static Object processSingleVariableAsObject(String expr, Map vars) {
        if (expr == null) {
            throw new IllegalArgumentException();
        }

        Matcher matcher = oneVarPattern.matcher(expr);
        if (matcher.matches()) {
            final String rawName = matcher.group(1);
            final VariableNameSyntax syntax = parseNameSyntax(rawName);
            final Object[] newVals = getAndFilter(vars, syntax);
            if (newVals == null || newVals.length == 0) return null;
            // TODO is it OK to return both an array and a single value for the same variable?
            if (newVals.length == 1) return newVals[0];
            return newVals;
        } else {
            return process(expr, vars);
        }
    }

    private static Object[] getAndFilter(Map vars, VariableNameSyntax syntax) {
        final Object o = vars.get(syntax.remainingName);

        final Object[] vals;
        if (o instanceof Object[]) {
            vals = (Object[]) o;
        } else {
            vals = new Object[] {o};
        }

        return syntax.filter(vals);
    }

    private static VariableNameSyntax parseNameSyntax(String rawName) {
        int ppos = rawName.indexOf("|");
        if (ppos == 0) throw new IllegalArgumentException("Variable names must not start with '|'");
        if (ppos > 0) {
            return new MultivalueDelimiterSyntax(rawName.substring(0,ppos), rawName.substring(ppos+1));
        } else {
            // Can't combine concatenation with subscript (yet -- 2D arrays?)
            int lbpos = rawName.indexOf("[");
            if (lbpos == 0) throw new IllegalArgumentException("Variable names must not start with '['");
            if (lbpos > 0) {
                int rbpos = rawName.indexOf("]", lbpos+1);
                if (rbpos == 0) throw new IllegalArgumentException("Array subscript must not be empty");
                if (rbpos > 0) {
                    String ssub = rawName.substring(lbpos+1, rbpos);
                    int subscript;
                    try {
                        subscript = Integer.parseInt(ssub);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Array subscript not an integer", e);
                    }
                    if (subscript < 0) throw new IllegalArgumentException("Array subscript must be positive");
                    return new MultivalueArraySubscriptSyntax(rawName.substring(0, lbpos), subscript);
                } else throw new IllegalArgumentException("']' expected but not found");
            } else {
                return new MultivalueDelimiterSyntax(rawName, DEFAULT_DELIMITER);
            }
        }
    }

    private static abstract class VariableNameSyntax {
        protected final String remainingName;

        private VariableNameSyntax(String name) {
            this.remainingName = name;
        }

        protected abstract Object[] filter(Object[] values);
        protected abstract String format(Object[] values, Formatter formatter);
    }

    private static class MultivalueDelimiterSyntax extends VariableNameSyntax {
        private final String delimiter;
        private MultivalueDelimiterSyntax(String name, String delimiter) {
            super(name);
            this.delimiter = delimiter;
        }

        protected Object[] filter(Object[] values) {
            return values;
        }

        protected String format(final Object[] values, final Formatter formatter) {
            if (values == null || values.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (value != null) sb.append(formatter.format(this, value));
                if (i < values.length-1) sb.append(delimiter);
            }
            return sb.toString();
        }
    }

    private static class MultivalueArraySubscriptSyntax extends VariableNameSyntax {
        private final int subscript;
        private MultivalueArraySubscriptSyntax(String name, int subscript) {
            super(name);
            this.subscript = subscript;
        }

        protected Object[] filter(Object[] values) {
            if (subscript > values.length-1) {
                logger.log(Level.WARNING, "Array subscript ({0}) in {1} out of range ({2} values); returning no values", new Object[] { Integer.valueOf(subscript), remainingName, Integer.valueOf(values.length) });
                return null;
            }
            return new Object[] { values[subscript] };
        }

        protected String format(Object[] values, Formatter formatter) {
            if (values == null || values.length != 1) return "";
            return formatter.format(this, values[0]);
        }
    }

    /**
     * Process the input string and expand the variables using the supplied
     * user variables map. If the varaible is not found in variables map
     * then the default variables map is consulted.
     *
     * @param s the input message as a message
     * @param vars the caller supplied varialbes map that is consulted first
     * @return the message with expanded/resolved varialbes
     */
    public static String process(String s, Map vars) {
        if (s == null) {
            throw new IllegalArgumentException();
        }
        Matcher matcher = regexPattern.matcher(s);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+matchingCount);
            }

            final VariableNameSyntax syntax = parseNameSyntax(matcher.group(1));
            Object[] newVals = getAndFilter(vars, syntax);
            String replacement;
            if (newVals == null || newVals.length == 0) {
                replacement = "";
            } else {
                replacement = syntax.format(newVals, DEFAULT_FORMATTER); // TODO support formatters for other data types!
            }

            replacement = makeDollarExplicit(replacement); // bugzilla 3022
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String makeDollarExplicit(String in) {
        if (in == null) return null;
        if (in.indexOf('$') < 0) return in;
        return in.replace("$", "\\$");
    }

    private ExpandVariables() {
    }

    public static void validateName(String name) {
        char c1 = name.charAt(1);
        if ("$".indexOf(c1) >= 0 || !Character.isJavaIdentifierStart(c1)) // Java allows '$', we don't
            throw new IllegalArgumentException("variable names must not start with '" + c1 + "'");

        for (int i = 0; i < name.toCharArray().length; i++) {
            char c = name.toCharArray()[i];
            if (c == '.') continue; // We allow '.', Java doesn't
            if (!Character.isJavaIdentifierPart(c))
                throw new IllegalArgumentException("variable names must not contain '" + c + "'");
        }
    }
}