/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.common.audit.Audit;
import com.l7tech.common.audit.CommonMessages;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class replaces the variables placeholders in the string that is passed to the
 * {@link ExpandVariables#process} method.
 * The variables placeholders are by default of format <code>${var.name}</code> where
 * <code>var.name</code> is the variable name.
 * The variables are passed in the <code>Map</code> of string key-value pairs. The default
 * variables are passed in constructor and optional overriding variables can be passed in
 * {@link ExpandVariables#process(String, Map, Audit)} method.
 */
public final class ExpandVariables {
    public static final String SYNTAX_PREFIX = "${";
    public static final String SYNTAX_SUFFIX = "}";
    
    private static final String REGEX_PREFIX = "(?:\\$\\{)";
    private static final String REGEX_SUFFIX = "(?:\\})";
    private static final Pattern regexPattern = Pattern.compile(REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX);
    private static final Pattern oneVarPattern = Pattern.compile("^" + REGEX_PREFIX +"(.+?)"+REGEX_SUFFIX + "$");
    private static final String DEFAULT_DELIMITER = ", ";

    private static interface Formatter {
        String format(VariableNameSyntax syntax, Object o, Audit audit);
    }

    private static final Formatter DEFAULT_FORMATTER = new Formatter() {
        public String format(VariableNameSyntax syntax, Object o, Audit audit) {
            if (!(Number.class.isAssignableFrom(o.getClass()) || CharSequence.class.isAssignableFrom(o.getClass())))
                audit.logAndAudit(CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING, syntax.remainingName, o.getClass().getName());

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

    public static Object processSingleVariableAsObject(final String expr, final Map vars, final Audit audit) {
        if (expr == null) {
            throw new IllegalArgumentException();
        }

        Matcher matcher = oneVarPattern.matcher(expr);
        if (matcher.matches()) {
            final String rawName = matcher.group(1);
            // TODO allow recursive syntax someday (i.e. ${foo[0]|DELIM} if foo is multi-dimensional)
            final VariableNameSyntax syntax = parseNameSyntax(rawName);
            final Object[] newVals = getAndFilter(vars, syntax, audit);
            if (newVals == null || newVals.length == 0) return null;
            // TODO is it OK to return both an array and a single value for the same variable?
            if (newVals.length == 1) return newVals[0];
            return newVals;
        } else {
            return process(expr, vars, audit);
        }
    }

    private static Object[] getAndFilter(Map vars, VariableNameSyntax syntax, Audit audit) {
        final Object o = vars.get(syntax.remainingName);

        final Object[] vals;
        if (o instanceof Object[]) {
            vals = (Object[]) o;
        } else {
            vals = new Object[] {o};
        }

        return syntax.filter(vals, audit);
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

        protected abstract Object[] filter(Object[] values, Audit audit);
        protected abstract String format(Object[] values, Formatter formatter, Audit audit);
    }

    private static class MultivalueDelimiterSyntax extends VariableNameSyntax {
        private final String delimiter;
        private MultivalueDelimiterSyntax(String name, String delimiter) {
            super(name);
            this.delimiter = delimiter;
        }

        protected Object[] filter(Object[] values, Audit audit) {
            return values;
        }

        protected String format(final Object[] values, final Formatter formatter, final Audit audit) {
            if (values == null || values.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (value != null) sb.append(formatter.format(this, value, audit));
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

        protected Object[] filter(Object[] values, Audit audit) {
            if (subscript > values.length-1) {
                audit.logAndAudit(CommonMessages.TEMPLATE_SUBSCRIPT_OUTOFRANGE, Integer.toString(subscript), remainingName, Integer.toString(values.length));
                return null;
            }
            return new Object[] { values[subscript] };
        }

        protected String format(Object[] values, Formatter formatter, Audit audit) {
            if (values == null || values.length != 1) return "";
            return formatter.format(this, values[0], audit);
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
    public static String process(String s, Map vars, Audit audit) {
        if (s == null) throw new IllegalArgumentException();

        Matcher matcher = regexPattern.matcher(s);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+matchingCount);
            }

            final VariableNameSyntax syntax = parseNameSyntax(matcher.group(1));
            Object[] newVals = getAndFilter(vars, syntax, audit);
            String replacement;
            if (newVals == null || newVals.length == 0) {
                replacement = "";
            } else {
                // TODO support formatters for other data types!
                replacement = syntax.format(newVals, DEFAULT_FORMATTER, audit);
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
}