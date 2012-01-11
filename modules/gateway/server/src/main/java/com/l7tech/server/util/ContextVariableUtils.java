package com.l7tech.server.util;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.Functions.flatmap;

/**
 * Utility class for common processing of context variables values.
 *
 */
public class ContextVariableUtils {

    /**
     * Get all Strings referenced from the expression. This expression may be made up of variable and non variable
     * string references.
     *
     * The splitPattern is applied to both the expression string and also any resolved variables (single or multivalued)
     * which contain strings. In the case of multi valued the splitPattern is applied to each item.
     *
     * @param expression The expression to extract strings from. This should be delimited as expected by the splitPattern
     * and may contain strings and variable references.
     * @param serverVariables Map of available variables.
     * @param auditor The auditor to audit to.
     * @param splitPattern The Pattern to apply to all resolved variable values (including multi valued individual items)
     * in order to obtain the entire list of runtime values for the expression.
     * @param callback called if a value resolved from expression is not a string.
     * @return The list of Strings extracted.
     */
    public static List<String> getAllResolvedStrings(@NotNull final String expression,
                                                     @NotNull final Map<String, Object> serverVariables,
                                                     @NotNull final Audit auditor,
                                                     @NotNull final Pattern splitPattern,
                                                     @Nullable final Functions.UnaryVoid<Object> callback) {

        return flatmap(list(splitPattern.split(expression)), new Functions.UnaryThrows<Iterable<String>, String, RuntimeException>() {
            @Override
            public Iterable<String> call(String token) throws RuntimeException {
                List<Object> listToProcess = (!Syntax.validateStringOnlyReferencesVariables(token)) ?
                        // if the expression is not a single variable reference, then it's an expression
                        new ArrayList<Object>(Arrays.asList(ExpandVariables.process(token, serverVariables, auditor))):
                        // otherwise it's a single variable reference
                        ExpandVariables.processNoFormat(token, serverVariables, auditor);

                return ContextVariableUtils.getStringsFromList(
                        listToProcess,
                        splitPattern,
                        callback);
            }
        });
    }

    /**
     * Get all Strings from the List of possible objects. Expected to be the output of
     * {@link com.l7tech.server.policy.variable.ExpandVariables#processNoFormat(String, java.util.Map, com.l7tech.gateway.common.audit.Audit, boolean)}
     *
     * Each String found in the list will have the split pattern applied to it if not null.
     *
     * @param objectList list to extract strings from
     * @param splitPattern pattern to split found strings on. If null no splitting of stings is done.
     * @param notStringCallback if not null, callback will be invoked with any non string value found
     * @return list of all found strings
     */
    public static List<String> getStringsFromList(@NotNull final List<Object> objectList,
                                                  @Nullable final Pattern splitPattern,
                                                  @Nullable final Functions.UnaryVoid<Object> notStringCallback) {
        return flatmap(objectList, new Functions.UnaryThrows<Iterable<String>, Object, RuntimeException>() {
            @Override
            public Iterable<String> call(Object val) throws RuntimeException {
                if (val instanceof String) {
                    String customVal = (String) val;
                    if (splitPattern != null) {
                        final String[] authMethods = splitPattern.split(customVal);
                        return Functions.grep(Arrays.asList(authMethods), TextUtils.isNotEmpty());
                    } else {
                        return Functions.grep(Arrays.asList(customVal), TextUtils.isNotEmpty());
                    }
                } else {
                    if (notStringCallback != null) {
                        notStringCallback.call(val);
                    }
                    return Collections.emptyList();
                }
            }
        });
    }
}
