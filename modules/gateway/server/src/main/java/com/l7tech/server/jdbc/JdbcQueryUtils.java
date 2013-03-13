package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils for creating prepared query statements
 */
public class JdbcQueryUtils {

    static public Object performJdbcQuery(JdbcQueryingManager jdbcQueryingManager, String connectionName, String query, List<String> resolveAsObjectList, AuditSinkPolicyEnforcementContext context, Audit audit) throws Exception {
        String[] vars = Syntax.getReferencedNames(query);
        try {
            final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, context, vars, true, resolveAsObjectList, audit);
            String plainQuery = context == null ? query : pair.left;
            // todo add support for schema name
            return jdbcQueryingManager.performJdbcQuery(connectionName, plainQuery, null, 1, pair.right);
        } catch (VariableNameSyntaxException e) {
            return e.getMessage();
        }
    }

    /**
     * Get the SQL query with usages of context variables replaced with ? characters.
     * <p/>
     * e.g.
     * <p>
     * select * from employees where id=${var} and department = ${var1}
     * </p>
     * <p/>
     * will return
     * <p>
     * select * from employees where id=? and department = ?
     * </p>
     *
     * @param query            string query which references context variables
     * @param context          The Policy Enforcement Context with all available variables
     * @param varsWithoutIndex get the variables used by the query with no indexes
     * @param convertVariablesToStrings true if variables should be converted into strings
     * @param audit            Audit for audits related to looking up variables.
     * @return pair of the String query with variables replaced with '?' and the list of parameters. The right side
     *         of parameters may be empty but never null.
     */
    @NotNull
    static public Pair<String, List<Object>> getQueryStatementWithoutContextVariables(String query,
                                                                                      final PolicyEnforcementContext context,
                                                                                      final String[] varsWithoutIndex,
                                                                                      final boolean convertVariablesToStrings,
                                                                                      final List<String> resolveAsObjectList,
                                                                                      final Audit audit) {
        final List<Object> paramValues = new ArrayList<Object>();
        if (Syntax.getReferencedNames(query).length == 0) {
            // There are no context variables in the query text. Just return it immediately.
            return new Pair<String, List<Object>>(query, paramValues);
        }

        // Get values of these context variables and then store these values into the parameter list, paramValues.
        // paramValues will be used in the PreparedStatement to set up values.
        final String[] varsWithIndex = Syntax.getReferencedNamesIndexedVarsNotOmitted(query);
        final Map<String, Pattern> varPatternMap = new HashMap<String, Pattern>();
        for (final String varWithIndex : varsWithIndex) {
            if (!convertVariablesToStrings) {
                List<Object> varValues = ExpandVariables.processNoFormat("${" + varWithIndex + "}", context.getVariableMap(varsWithoutIndex, audit), audit, true);
                //when parameters are multi-value, make each value as a separate parameter of the parametrized query separated by comma.
                StringBuilder sb = new StringBuilder();
                Iterator<Object> iter = varValues.iterator();
                while (iter.hasNext()) {
                    Object value = iter.next();
                    paramValues.add(value);
                    sb.append("?").append(iter.hasNext() ? ", " : "");
                }

                final Pattern searchPattern = getSearchPattern(varWithIndex, varPatternMap);
                Matcher matcher = searchPattern.matcher(query);
                query = matcher.replaceFirst(sb.toString());
            } else {
                Object value;
                if (resolveAsObjectList.contains(varWithIndex))
                    value = ExpandVariables.processSingleVariableAsObject("${" + varWithIndex + "}", context.getVariableMap(varsWithoutIndex, audit), audit);
                else
                    value = ExpandVariables.process("${" + varWithIndex + "}", context.getVariableMap(varsWithoutIndex, audit), audit);
                paramValues.add(value);
            }
        }

        if (convertVariablesToStrings) {
            // Replace all context variables with a question mark, ?
            Matcher matcher = Syntax.regexPattern.matcher(query);
            query = matcher.replaceAll("?");
        }

        return new Pair<String, List<Object>>(query, paramValues);
    }

    static public Pair<String, List<Object>> getQueryStatementWithoutContextVariables(final String query,
                                                                                      final PolicyEnforcementContext context,
                                                                                      final String[] varsWithoutIndex,
                                                                                      final boolean convertVariablesToStrings,
                                                                                      final Audit audit) {
        return getQueryStatementWithoutContextVariables(query, context, varsWithoutIndex, convertVariablesToStrings, Collections.<String>emptyList(), audit);
    }

     static private Pattern getSearchPattern(String varWithIndex, Map<String, Pattern> varPatternMap) {
        if (varPatternMap.containsKey(varWithIndex)) {
            return varPatternMap.get(varWithIndex);
        }

        final Pattern searchPattern = Pattern.compile("${" + varWithIndex + "}", Pattern.LITERAL);
        varPatternMap.put(varWithIndex, searchPattern);

        return searchPattern;
    }
}
