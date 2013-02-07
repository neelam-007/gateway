package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils for creating prepared query statements
 */
public class JdbcQueryUtils {

    static public Object performJdbcQuery(JdbcQueryingManager jdbcQueryingManager,String connectionName, String query,List<String> resolveAsObjectList , AuditSinkPolicyEnforcementContext context, Audit audit) throws Exception {
        String[] vars = Syntax.getReferencedNames(query);
        List<Object> preparedStmtParams = new ArrayList<Object>();
        try {
            String plainQuery = context == null? query : JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, preparedStmtParams, context, vars, false ,resolveAsObjectList,audit);
            // todo add support for schema name
            return jdbcQueryingManager.performJdbcQuery(connectionName, plainQuery, null, 1, preparedStmtParams);
        } catch (VariableNameSyntaxException e) {
            return e.getMessage();
        }
    }

    /**
     *
     * @param query
     * @param params
     * @param context
     * @param varsWithoutIndex
     * @param allowMultiValued
     * @param audit
     * @return
     */
    static public String getQueryStatementWithoutContextVariables(String query, List<Object> params, PolicyEnforcementContext context,String[] varsWithoutIndex, boolean allowMultiValued, List<String> resolveAsObjectList , Audit audit) {
        if (Syntax.getReferencedNames(query).length == 0) {
            // There are no context variables in the query text. Just return it immediately.
            return query;
        }
        // Get values of these context variables and then store these values into the parameter list, params.
        // params will be used in the PreparedStatement to set up values.
        final String[] varsWithIndex = Syntax.getReferencedNamesIndexedVarsNotOmitted(query);
        final Map<String, Pattern> varPatternMap = new HashMap<String, Pattern>();
        for (final String varWithIndex : varsWithIndex) {
            if (allowMultiValued) {
                List<Object> varValues = ExpandVariables.processNoFormat("${" + varWithIndex + "}", context.getVariableMap(varsWithoutIndex, audit), audit, true);
                //when parameters are multi-value, make each value as a separate parameter of the parameterized query separated by comma.
                StringBuilder sb = new StringBuilder();
                Iterator<Object> iter = varValues.iterator();
                while (iter.hasNext()) {
                    Object value = iter.next();
                    params.add(value);
                    sb.append("?").append(iter.hasNext() ? ", " : "");
                }

                final Pattern searchPattern = getSearchPattern(varWithIndex, varPatternMap);
                Matcher matcher = searchPattern.matcher(query);
                query = matcher.replaceFirst(sb.toString());
            } else {
                Object value;
                if(resolveAsObjectList.contains(varWithIndex))
                    value= ExpandVariables.processSingleVariableAsObject("${" + varWithIndex + "}", context.getVariableMap(varsWithoutIndex, audit), audit);
                else
                    value = ExpandVariables.process("${" + varWithIndex + "}", context.getVariableMap(varsWithoutIndex, audit), audit);
                params.add(value);
            }
        }

        if (!allowMultiValued) {
            // Replace all context variables with a question mark, ?
            Matcher matcher = Syntax.regexPattern.matcher(query);
            query = matcher.replaceAll("?");
        }

        return query;
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
