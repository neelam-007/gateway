package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.common.util.ComparisonOperator;

/**
 * Compares two values.  Variable names are supported using ${assertion.var} syntax.
 * @see com.l7tech.server.message.PolicyEnforcementContext#getVariable(String)
 * @see com.l7tech.server.message.PolicyEnforcementContext#setVariable(String, Object)
 */
public class ComparisonAssertion extends Assertion implements UsesVariables {
    private String expression1;
    private String expression2;
    private ComparisonOperator operator = ComparisonOperator.EQ;
    private boolean negate = false;
    private boolean caseSensitive = true;

    public String[] getVariablesUsed() {
        return ExpandVariables.getReferencedNames(expression1 + expression2);
    }

    public ComparisonAssertion() {
    }

    public ComparisonAssertion(boolean negate, String expr1, ComparisonOperator operator, String expr2) {
        this.negate = negate;
        this.expression1 = expr1;
        this.operator = operator;
        this.expression2 = expr2;
    }

    public String getExpression1() {
        return expression1;
    }

    public void setExpression1(String expression1) {
        this.expression1 = expression1;
    }

    public String getExpression2() {
        return expression2;
    }

    public void setExpression2(String expression2) {
        this.expression2 = expression2;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public void setOperator(ComparisonOperator operator) {
        this.operator = operator;
    }

    public boolean isNegate() {
        return negate;
    }

    public void setNegate(boolean negate) {
        this.negate = negate;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
}
