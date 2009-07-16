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
import java.util.*;
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

            final Syntax.SyntaxErrorHandler handler = new DefaultSyntaxErrorHandler(audit);

            if(newVals == null || newVals.length == 0) {
                return null;
            } else if(newVals.length == 1) {
                return syntax.format(new Object[] {newVals[0]}, Syntax.DEFAULT_FORMATTER, handler, strict);
            } else {
                final Object[] retVal = new Object[newVals.length];
                final Object[] dummyObject = new Object[1];
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

    static interface Selector<T> {
        static final Selection NOT_PRESENT = new Selection(null, null);
        static class Selection {
            private final Object value;
            private final String remainingName;

            public Selection(Object value) {
                this(value, null);
            }

            public Selection(Object value, String remainingName) {
                this.value = value;
                this.remainingName = remainingName;
            }

            public Object getSelectedValue() {
                return value;
            }

            public String getRemainingName() {
                return remainingName;
            }
        }

        Selection select(T context, String name, Syntax.SyntaxErrorHandler handler, boolean strict);

        Class<T> getContextObjectClass();
    }

    private static final String[] selectors = {
        "com.l7tech.server.policy.variable.MessageSelector",
        "com.l7tech.server.policy.variable.X509CertificateSelector",
        "com.l7tech.server.policy.variable.UserSelector",
    };

    private static final Map<Class, Selector<Object>> selectorMap = Collections.unmodifiableMap(new HashMap<Class, Selector<Object>>() {{
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < selectors.length; i++) {
            String selectorClassname = selectors[i];
            try {
                Class clazz = Class.forName(selectorClassname);
                @SuppressWarnings({"unchecked"}) Selector<Object> sel = (Selector<Object>) clazz.newInstance();
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

        Object contextValue = vars.get(matchingName);
        final Syntax.SyntaxErrorHandler handler = new DefaultSyntaxErrorHandler(audit);

        Selector.Selection selection;
        if (!matchingName.toLowerCase().equals(syntax.remainingName.toLowerCase().trim())) {
            // Get name suffix, it will be used to select a sub-value from the found object
            assert(syntax.remainingName.toLowerCase().startsWith(matchingName));
            final int len = matchingName.length();
            assert(syntax.remainingName.substring(len, len +1).equals("."));
            selection = selectify(contextValue, syntax.remainingName.substring(len+1), handler, strict);
        } else {
            selection = new Selector.Selection(contextValue);
        }

        // else the name already matches a known variable

        if (selection == null || selection == Selector.NOT_PRESENT) {
            String msg = handler.handleBadVariable(syntax.remainingName);
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        } else {
            String name = selection.getRemainingName();
            if (name != null && name.length() > 0) {
                String msg = handler.handleBadVariable(name);
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            } else {
                contextValue = selection.getSelectedValue(); 
            }
        }

        final Object[] vals;
        if (contextValue instanceof Object[]) {
            vals = (Object[]) contextValue;
        } else {
            vals = new Object[] {contextValue};
        }

        return syntax.filter(vals, handler, strict);
    }

    private static Selector.Selection selectify(final Object contextObject, final String name, final Syntax.SyntaxErrorHandler handler, final boolean strict) {
        Object contextValue = contextObject;
        String remainingName = name;

        while (remainingName != null && remainingName.length() > 0) {
            // Try to find a Selector for values of this type
            Selector<Object> selector = null;
            for (Map.Entry<Class,Selector<Object>> entry : selectorMap.entrySet()) {
                Class clazz = entry.getKey();
                Selector<Object> sel = entry.getValue();
                if (clazz.isAssignableFrom(contextValue.getClass())) {
                    selector = sel;
                    break;
                }
            }

            if (selector == null) {
                // No selector for values of this type; just return it and hope the caller can cope
                return new Selector.Selection(contextValue, remainingName);
            }

            final Selector.Selection selection = selector.select(contextValue, remainingName, handler, strict);
            if (selection == null) {
                // This name is unknown to the selector
                String msg = handler.handleBadVariable(MessageFormat.format("{0} on {1}", remainingName, contextObject.getClass().getName()));
                if (strict) throw new IllegalArgumentException(msg);
                return Selector.NOT_PRESENT;
            }

            final String tempRemainder = selection.getRemainingName();
            if (tempRemainder == null || tempRemainder.length() == 0) {
                // Done: this selector has fully resolved the remaining name (note that the value may be legitimately null)
                return selection;
            }

            if (tempRemainder.length() > 0) {
                // The selector has selected a sub-object; loop with new remaining name and context object
                contextValue = selection.getSelectedValue();
                remainingName = tempRemainder;
                continue;
            }

            throw new IllegalStateException("Selector for " + remainingName + " returned " + selection);
        }
        throw new IllegalStateException("Unable to select " + name + " from " + contextObject.getClass().getName());
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

            replacement = Matcher.quoteReplacement(replacement); // bugzilla 3022 and 6813

            // 5.0 Audit Request Id enhancement imposes a limit to the length of each ctx variable replacement
            if (varLengthLimit != null && replacement.length() > varLengthLimit)
                matcher.appendReplacement(sb, replacement.substring(0, varLengthLimit));
            else
                matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Expands variables found in the input String similar to the process() methods,
     * but does not format the resolved values to String.
     *
     * @return a list of Objects containing String parts from the input that do not reference variables
     *         and the resolved variable values
     * @see #process(String, java.util.Map, com.l7tech.gateway.common.audit.Audit, boolean)
     */
    public static List<Object> processNoFormat(String s, Map vars, Audit audit, boolean strict) {
        if (s == null) throw new IllegalArgumentException();

        Matcher matcher = Syntax.regexPattern.matcher(s);
        List<Object> result = new ArrayList<Object>();

        int previousMatchEndIndex = 0;
        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: "+matchingCount);
            }
            result.add(s.substring(previousMatchEndIndex, matcher.start()));
            Collections.addAll(result, getAndFilter(vars, Syntax.parse(matcher.group(1), defaultDelimiter()), audit, strict));
            previousMatchEndIndex = matcher.end();
        }
        return result;
    }

    private ExpandVariables() {
    }

}