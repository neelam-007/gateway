package com.l7tech.policy.assertion;

/**
 * Tests whether two values are equal.  Variable names are supported using ${assertion.var} syntax.
 * @see com.l7tech.server.message.PolicyEnforcementContext#getVariable(String)
 * @see com.l7tech.server.message.PolicyEnforcementContext#getVariables()
 * @see com.l7tech.server.message.PolicyEnforcementContext#setVariable(String, Object)
 */
public class EqualityAssertion extends Assertion {
    private String expression1;
    private String expression2;

    public EqualityAssertion() {
    }

    public EqualityAssertion(String expr1, String expr2) {
        this.expression1 = expr1;
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
}
