package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.DefaultSyntaxErrorHandler;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public static Object processSingleVariableAsObject(final String expr, final Map<String,?> vars, final Audit audit) {
        return processSingleVariableAsObject(expr, vars, audit, strict());
    }

    @Nullable
    public static Object processSingleVariableAsDisplayableObject(final String expr, final Map<String,?> vars, final Audit audit) {
        return processSingleVariableAsDisplayableObject(expr, vars, audit, strict());
    }

    private static boolean strict() {
        return ConfigFactory.getBooleanProperty( ServerConfigParams.PARAM_TEMPLATE_STRICTMODE, false );
    }

    public static Object processSingleVariableAsObject(final String expr, final Map<String,?> vars, final Audit audit, final boolean strict) {
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

    /**
     * This is a convenience method which delegates to {@link #process(String, java.util.Map, com.l7tech.gateway.common.audit.Audit, boolean)}
     * when the expression does not reference a single value.
     * <p>
     * When a single variable is referenced, then this method provides convenient support for multi valued variables,
     * by returning an Object [] of formatted Strings.
     * <p>
     * This provides the ability to process the formatted value of individual values independently of each other.
     * This is different to {@link #process(String, java.util.Map, com.l7tech.gateway.common.audit.Audit, boolean)}
     * which would concatenate the joined values when the variable is multi valued.
     *
     * @param expr String expression to evaluate.
     * @param vars the caller supplied variables map that is consulted first
     * @param audit an audit instance to catch warnings
     * @param strict true if failures to resolve variables should throw exceptions rather than log warnings
     * @return if the expression references a single reference, then either null (does not exist - depends on strict = false)
     * or a String or an Object [] of formatted Strings. If an Object [] it is never null or empty, otherwise see
     * {@link #process(String, java.util.Map, com.l7tech.gateway.common.audit.Audit, boolean)}
     */
    @Nullable
    public static Object processSingleVariableAsDisplayableObject(final String expr, final Map<String,?> vars, final Audit audit, final boolean strict) {
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
                return syntax.format(new Object[] {newVals[0]}, Syntax.getFormatter(newVals[0]), handler, strict);
            } else {
                final Object[] retVal = new Object[newVals.length];
                final Object[] dummyObject = new Object[1];
                for(int i = 0;i < newVals.length;i++) {
                    dummyObject[0] = newVals[i];
                    retVal[i] = syntax.format(dummyObject, Syntax.getFormatter(newVals[i]), handler, strict);
                }

                return retVal;
            }
        } else {
            return process(expr, vars, audit, strict);
        }
    }

    public static String defaultDelimiter() {
        String delim = ConfigFactory.getProperty( ServerConfigParams.PARAM_TEMPLATE_MULTIVALUE_DELIMITER, null );
        if (delim != null) return delim;
        return Syntax.DEFAULT_MV_DELIMITER;
    }

    /**
     * Call when a non existent variable is referenced by a string value in a policy.
     * This method will log and audit the non existent variable and optionally throw an unchecked exception.
     *
     * @param nonExistentVariable the non existent variable referenced in a policy
     * @param strict              if true, a VariableNameSyntaxException will be thrown
     * @param audit               the Audit to log and audit to
     */
    public static void badVariable(String nonExistentVariable, boolean strict, Audit audit) {
        audit.logAndAudit(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE, nonExistentVariable);
        if (strict) throw new VariableNameSyntaxException(nonExistentVariable);
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

        Selection select(String contextName, T context, String name, Syntax.SyntaxErrorHandler handler, boolean strict);

        Class<T> getContextObjectClass();
    }

    private static final String[] selectorClassnames = {
        "com.l7tech.server.policy.variable.MessageSelector",
        "com.l7tech.server.policy.variable.X509CertificateSelector",
        "com.l7tech.server.policy.variable.UserSelector",
        "com.l7tech.server.policy.variable.AuditSelector",
        "com.l7tech.server.policy.variable.AuditRecordSelector",
        "com.l7tech.server.policy.variable.AuditDetailSelector",
        "com.l7tech.server.policy.variable.AuditSearchCriteriaSelector",
        "com.l7tech.server.policy.variable.PolicyEnforcementContextSelector",
        "com.l7tech.server.policy.variable.PartInfoSelector",
        "com.l7tech.server.policy.variable.ArraySelector",
        "com.l7tech.server.policy.variable.DebugTraceVariableContextSelector",
        "com.l7tech.server.policy.variable.SecurePasswordLocatorContextSelector",
        "com.l7tech.server.policy.variable.SecurePasswordSelector",
        "com.l7tech.server.policy.variable.SecureConversationSessionSelector",
        "com.l7tech.server.policy.variable.BuildVersionContext$BuildVersionContextSelector",
        "com.l7tech.server.policy.variable.KerberosAuthorizationDataSelector"
    };

    private static final List<Selector<?>> selectors = Collections.unmodifiableList(new ArrayList<Selector<?>>() {{
        for ( final String selectorClassname : selectorClassnames ) {
            try {
                final Class clazz = Class.forName( selectorClassname );
                final Selector<?> sel = (Selector<?>) clazz.newInstance();
                add( sel );
            } catch ( InstantiationException e ) {
                throw new RuntimeException( e ); // Can't happen
            } catch ( IllegalAccessException e ) {
                throw new RuntimeException( e ); // Can't happen
            } catch ( ClassNotFoundException e ) {
                throw new RuntimeException( e ); // Can't happen
            }
        }
    }});

    /**
     * Determine if any variable in the expression does not exist.
     *
     * Note: this method will not cause WARNING logging associated with a non existent variable.
     *
     * @param expression String expression to check
     * @param vars all available variables
     * @param audit Auditor to audit to
     * @return true if any referenced variable does not exist, false otherwise.
     */
    public static boolean isVariableReferencedNotFound(@Nullable final String expression,
                                                       @NotNull final Map<String, Object> vars,
                                                       @NotNull Audit audit) {
        final String[] referencedNames = Syntax.getReferencedNames(expression);
        for (String referencedName : referencedNames) {
            try {
                ExpandVariables.process(Syntax.getVariableExpression(referencedName), vars, audit, true);
            } catch (VariableNameSyntaxException e) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private static Object[] getAndFilter(Map<String,?> vars, Syntax syntax, Audit audit, boolean strict) {
        String matchingName = Syntax.getMatchingName(syntax.remainingName.toLowerCase(), vars.keySet());
        if (matchingName == null) {
            badVariable(syntax.remainingName, strict, audit);
            return null;
        }

        Object contextValue = vars.get(matchingName);
        final Syntax.SyntaxErrorHandler handler = new DefaultSyntaxErrorHandler(audit);

        Selector.Selection selection;
        if (!matchingName.toLowerCase().equals(syntax.remainingName.toLowerCase().trim())) {
            if (contextValue == null) {
                String msg = handler.handleBadVariable(syntax.remainingName);
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }

            // Get name suffix, it will be used to select a sub-value from the found object
            assert(syntax.remainingName.toLowerCase().startsWith(matchingName));
            final int len = matchingName.length();
            assert(syntax.remainingName.substring(len, len +1).equals("."));
            selection = selectify(matchingName, contextValue, syntax.remainingName.substring(len+1), handler, strict);
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
        } else if( contextValue instanceof List){
            final List<?> tempList = (List) contextValue;
            vals = tempList.toArray(new Object[tempList.size()]);
        } else {
            vals = new Object[] {contextValue};
        }

        return syntax.filter(vals, handler, strict);
    }

    private static Selector.Selection selectify(final String contextObjectName,
                                                final Object contextObject,
                                                final String name,
                                                final Syntax.SyntaxErrorHandler handler,
                                                final boolean strict) {
        String contextName =  contextObjectName;
        Object contextValue = contextObject;
        String remainingName = name;

        while (remainingName != null && remainingName.length() > 0) {
            // Try to find a Selector for values of this type
            Selector selector = null;
            for ( Selector<?> sel : selectors ) {
                if (sel.getContextObjectClass().isAssignableFrom( contextValue.getClass() )) {
                    selector = sel;
                    break;
                }
            }

            if (selector == null) {
                // No selector for values of this type; just return it and hope the caller can cope
                return new Selector.Selection(contextValue, remainingName);
            }

            @SuppressWarnings({ "unchecked" })
            final Selector.Selection selection = selector.select(contextName, contextValue, remainingName, handler, strict);
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
                String extraContextName = remainingName.substring( 0, remainingName.length() - selection.getRemainingName().length());
                if ( extraContextName.endsWith( "." )) {
                    extraContextName = extraContextName.substring( 0, extraContextName.length() - 1 );
                }
                if ( !contextName.endsWith( "." ) ) {
                    extraContextName = "." + extraContextName;    
                }
                contextName += extraContextName;
                contextValue = selection.getSelectedValue();
                remainingName = tempRemainder;
                continue;
            }

            throw new IllegalStateException("Selector for " + remainingName + " returned " + selection);
        }
        throw new IllegalStateException("Unable to select " + name + " from " + contextObject.getClass().getName());
    }

    @NotNull
    public static String process(String s, Map<String,?> vars, Audit audit) {
        return process(s, vars, audit, strict(), null);
    }

    public static String process(String s, Map<String,?> vars, Audit audit, Functions.Unary<String,String> valueFilter) {
        return process(s, vars, audit, strict(), valueFilter);
    }

    /**
     * Process the input string and expand the variables using the supplied
     * user variables map. If the variable is not found in variables map
     * then the default variables map is consulted.
     *
     * @param s the input message as a message
     * @param vars the caller supplied variables map that is consulted first
     * @param audit an audit instance to catch warnings
     * @param strict true if failures to resolve variables should throw exceptions rather than log warnings
     * @return the message with expanded/resolved variables
     */
    public static String process(String s, Map<String,?> vars, Audit audit, boolean strict) {
        return process(s, vars, audit, strict, null);
    }

    /**
     * Process the input string and expand the variables using the supplied
     * user variables map. If the variable is not found in variables map
     * then the default variables map is consulted.
     *
     * @param s              the input message as a message
     * @param vars           the caller supplied variables map that is consulted first
     * @param audit          an audit instance to catch warnings
     * @param strict         true if failures to resolve variables should throw exceptions rather than log warnings. If a
     *                       variable referenced by s does not exist and strict is true then an unchecked
     *                       VariableNameSyntaxException will be throw
     * @param varLengthLimit the length limit of each replacement context variable value.
     * @return the message with expanded/resolved variables
     */
    public static String process(String s, Map<String,?> vars, Audit audit, boolean strict, final int varLengthLimit) {
        return process( s, vars, audit, strict, new Functions.Unary<String,String>(){
            @Override
            public String call( final String replacement ) {
                if ( replacement.length() > varLengthLimit )
                    return replacement.substring(0, varLengthLimit);
                else
                    return replacement;
            }
        } );
    }

    /**
     * Process the input string and expand the variables using the supplied
     * user variables map. If the variable is not found in variables map
     * then the default variables map is consulted.
     *
     * @param s              the input message as a message
     * @param vars           the caller supplied variables map that is consulted first
     * @param audit          an audit instance to catch warnings
     * @param strict         true if failures to resolve variables should throw exceptions rather than log warnings. If a
     *                       variable referenced by s does not exist and strict is true then an unchecked
     *                       VariableNameSyntaxException will be throw
     * @param valueFilter    A filter to call on each substituted value (or null for no filtering)
     * @return the message with expanded/resolved variables
     */
    @NotNull
    public static String process(String s, Map<String,?> vars, Audit audit, boolean strict, @Nullable Functions.Unary<String,String> valueFilter) {
        if (s == null) throw new IllegalArgumentException();

        Matcher matcher = Syntax.regexPattern.matcher(s);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: " + matchingCount);
            }

            final Syntax syntax = Syntax.parse(matcher.group(1), defaultDelimiter());
            Object[] newVals = getAndFilter(vars, syntax, audit, strict);
            String replacement;
            if (newVals == null || newVals.length == 0) {
                replacement = "";
            } else {
                // TODO support formatters for other data types!
                Syntax.SyntaxErrorHandler handler = new DefaultSyntaxErrorHandler(audit);
                replacement = syntax.format(newVals, Syntax.getFormatter( newVals[0] ), handler, strict);
            }

            replacement = valueFilter != null ? valueFilter.call(replacement) : replacement;
            replacement = Matcher.quoteReplacement(replacement); // bugzilla 3022 and 6813
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Expands variables found in the input String similar to the process() methods,
     * but does not format the resolved values to String.
     * <p/>
     * Multi valued variables are not returned as a single value, but the returned list will contain an element
     * for every element in each multi valued variable found
     *
     * @param s The string to process.
     * @param vars The available variables.
     * @param audit The Auditor to audit to.
     * @param strict Thrown VariableNameSyntaxException if a referenced variable does not exist.
     * @return a list of Objects containing String parts from the input that do not reference variables
     *         and the resolved variable values. Variables which have a null value will be included as null.
     * @see #process(String, java.util.Map, com.l7tech.gateway.common.audit.Audit, boolean)
     * @throws com.l7tech.policy.variable.VariableNameSyntaxException throw if strict and an unknown variable is referenced.
     */
    @NotNull("Values of the list may be null")
    public static List<Object> processNoFormat(@NotNull final String s,
                                               @NotNull final Map<String,?> vars,
                                               @NotNull final Audit audit,
                                               final boolean strict)
            throws VariableNameSyntaxException{

        Matcher matcher = Syntax.regexPattern.matcher(s);
        List<Object> result = new ArrayList<Object>();

        int previousMatchEndIndex = 0;
        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: " + matchingCount);
            }
            final String preceedingText = s.substring(previousMatchEndIndex, matcher.start());
            //note if there is actually an empty space, we will preserve it, so no .trim() before .isEmpty()
            if (!preceedingText.isEmpty()) result.add(s.substring(previousMatchEndIndex, matcher.start()));

            final Object[] newVals = getAndFilter(vars, Syntax.parse(matcher.group(1), defaultDelimiter()), audit, strict);
            if (newVals != null) {
                Collections.addAll(result, newVals);
            }

            previousMatchEndIndex = matcher.end();
        }
        if (previousMatchEndIndex < s.length())
            result.add(s.substring(previousMatchEndIndex, s.length()));
        return result;
    }

    @NotNull
    public static List<Object> processNoFormat(@NotNull final String s,
                                               @NotNull final Map<String,?> vars,
                                               @NotNull final Audit audit) {
        return processNoFormat(s, vars, audit, strict());
    }

    private ExpandVariables() {
    }

}
