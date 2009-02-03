/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.DefaultSyntaxErrorHandler;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfig;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

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
    public static Object processSingleVariableAsObject(final String expr, final Map vars, final Audit audit) {
        return processSingleVariableAsObject(expr, vars, audit, strict());
    }

    public static Object processSingleVariableAsDisplayableObject(final String expr, final Map vars, final Audit audit) {
        return processSingleVariableAsDisplayableObject(expr, vars, audit, strict());
    }

    private static boolean strict() {
        return "true".equals(ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_TEMPLATE_STRICTMODE));
    }

    public static Object processSingleVariableAsObject(final String expr, final Map vars, final Audit audit, final boolean strict) {
        if (expr == null) throw new IllegalArgumentException();

        Matcher matcher = Syntax.oneVarPattern.matcher(expr);
        if (matcher.matches()) {
            final String rawName = matcher.group(1);
            // TODO allow recursive syntax someday (i.e. ${foo[0]|DELIM} if foo is multi-dimensional)
            final Syntax syntax = Syntax.parse(rawName, defaultDelimiter());
            final Object[] newVals = getAndFilter(vars, syntax, audit, strict);
            if (newVals == null || newVals.length == 0) return null;
            // TODO is it OK to return both an array and a single value for the same variable?
            if (newVals.length == 1) return newVals[0];
            return newVals;
        } else {
            return process(expr, vars, audit, strict);
        }
    }

    public static Object processSingleVariableAsDisplayableObject(final String expr, final Map vars, final Audit audit, final boolean strict) {
        if (expr == null) throw new IllegalArgumentException();

        Matcher matcher = Syntax.oneVarPattern.matcher(expr);
        if (matcher.matches()) {
            final String rawName = matcher.group(1);
            // TODO allow recursive syntax someday (i.e. ${foo[0]|DELIM} if foo is multi-dimensional)
            final Syntax syntax = Syntax.parse(rawName, defaultDelimiter());
            final Object[] newVals = getAndFilter(vars, syntax, audit, strict);

            Syntax.SyntaxErrorHandler handler = new DefaultSyntaxErrorHandler(audit);

            if(newVals == null || newVals.length == 0) {
                return null;
            } else if(newVals.length == 1) {
                return syntax.format(new Object[] {newVals[0]}, Syntax.DEFAULT_FORMATTER, handler, strict);
            } else {
                Object[] retVal = new Object[newVals.length];
                Object[] dummyObject = new Object[1];
                for(int i = 0;i < newVals.length;i++) {
                    dummyObject[0] = newVals[i];
                    retVal[i] = syntax.format(dummyObject, Syntax.DEFAULT_FORMATTER, handler, strict);
                }

                return retVal;
            }
        } else {
            return process(expr, vars, audit, strict);
        }
    }

    public static String defaultDelimiter() {
        String delim = ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_TEMPLATE_MULTIVALUE_DELIMITER);
        if (delim != null) return delim;
        return Syntax.DEFAULT_MV_DELIMITER;
    }

    public static void badVariable(String msg, boolean strict, Audit audit) {
        audit.logAndAudit( CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE, msg);
        if (strict) throw new IllegalArgumentException(msg);
    }

    static interface Selector {
        Object select(Object context, String name, Audit audit, boolean strict);

        Class getContextObjectClass();
    }

    private static final String[] selectors = {
        "com.l7tech.server.policy.variable.MessageSelector"
    };

    private static final Map selectorMap = Collections.unmodifiableMap(new HashMap() {{
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < selectors.length; i++) {
            String selectorClassname = selectors[i];
            try {
                Class clazz = Class.forName(selectorClassname);
                Selector sel = (Selector) clazz.newInstance();
                put(sel.getContextObjectClass(), sel);
            } catch (InstantiationException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
    }});

    private static Object[] getAndFilter(Map vars, Syntax syntax, Audit audit, boolean strict) {
        String matchingName = Syntax.getMatchingName(syntax.remainingName.toLowerCase(), vars.keySet());
        if (matchingName == null) {
            badVariable(syntax.remainingName, strict, audit);
            return null;
        }

        Object got = vars.get(matchingName);

        if (!matchingName.toLowerCase().equals(syntax.remainingName.toLowerCase())) {
            // Get name suffix, it will be used to select a sub-value from the found object
            assert(syntax.remainingName.toLowerCase().startsWith(matchingName.toLowerCase()));
            assert(syntax.remainingName.substring(matchingName.length(), matchingName.length()+1).equals("."));

            Selector selector = (Selector) selectorMap.get(got.getClass());
            if (selector != null) {
                String remainder = syntax.remainingName.substring(matchingName.length()+1);
                if (remainder.length() > 0) {
                    Object newval = selector.select(got, remainder, audit, strict);
                    if (newval == null) {
                        badVariable(MessageFormat.format("{0} on {1}", remainder, got.getClass().getName()), strict, audit);
                    }
                    got = newval;
                }
            }
        } // else the name is good as-is

        final Object[] vals;
        if (got instanceof Object[]) {
            vals = (Object[]) got;
        } else {
            vals = new Object[] {got};
        }

        Syntax.SyntaxErrorHandler handler = new DefaultSyntaxErrorHandler(audit);
        return syntax.filter(vals, handler, strict);
    }

    public static String process(String s, Map vars, Audit audit) {
        return process(s, vars, audit, strict(), null);
    }

    /**
     * Process the input string and expand the variables using the supplied
     * user variables map. If the varaible is not found in variables map
     * then the default variables map is consulted.
     *
     * @param s the input message as a message
     * @param vars the caller supplied varialbes map that is consulted first
     * @param audit an audit instance to catch warnings
     * @param strict true if failures to resolve variables should throw exceptions rather than log warnings
     * @return the message with expanded/resolved varialbes
     */
    public static String process(String s, Map vars, Audit audit, boolean strict) {
        return process(s, vars, audit, strict, null);
    }

    /**
     * Process the input string and expand the variables using the supplied
     * user variables map. If the varaible is not found in variables map
     * then the default variables map is consulted.
     *
     * @param s the input message as a message
     * @param vars the caller supplied varialbes map that is consulted first
     * @param audit an audit instance to catch warnings
     * @param strict true if failures to resolve variables should throw exceptions rather than log warnings
     * @param varLengthLimit the length limit of each replacement context variable value, use null if no limit is applied
     * @return the message with expanded/resolved varialbes
     */
    public static String process(String s, Map vars, Audit audit, boolean strict, Integer varLengthLimit) {
        if (s == null) throw new IllegalArgumentException();

        Matcher matcher = Syntax.regexPattern.matcher(s);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+matchingCount);
            }

            final Syntax syntax = Syntax.parse(matcher.group(1), defaultDelimiter());
            Object[] newVals = getAndFilter(vars, syntax, audit, strict);
            String replacement;
            if (newVals == null || newVals.length == 0) {
                replacement = "";
            } else {
                // TODO support formatters for other data types!
                Syntax.SyntaxErrorHandler handler = new DefaultSyntaxErrorHandler(audit);
                replacement = syntax.format(newVals, Syntax.DEFAULT_FORMATTER, handler, strict);
            }

            replacement = makeDollarExplicit(replacement); // bugzilla 3022

            // 5.0 Audit Request Id enhancement imposes a limit to the length of each ctx variable replacement
            if (varLengthLimit != null && replacement.length() > varLengthLimit)
                matcher.appendReplacement(sb, replacement.substring(0, varLengthLimit));
            else
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