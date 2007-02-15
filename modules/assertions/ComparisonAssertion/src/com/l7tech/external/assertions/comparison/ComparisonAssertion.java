package com.l7tech.external.assertions.comparison;

import com.l7tech.common.logic.BinaryPredicate;
import com.l7tech.common.logic.Predicate;
import com.l7tech.common.util.ComparisonOperator;
import com.l7tech.common.util.Functions;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.ExpandVariables;

/**
 * Processes the value resulting from the evaluation of an expression through any number of {@link Predicate}s.
 *
 * Variable names are supported using ${assertion.var} syntax.
 * @see com.l7tech.server.message.PolicyEnforcementContext#getVariable(String)
 * @see com.l7tech.server.message.PolicyEnforcementContext#setVariable(String, Object)
 */
public class ComparisonAssertion extends Assertion implements UsesVariables {
    private String leftValue;
    private Predicate[] predicates = new Predicate[0];

    /**
     * Returns the variables referenced in {@link #leftValue}, as well as any referenced in the
     * {@link BinaryPredicate#rightValue} values of our {@link #predicates}. (Only {@link BinaryPredicate} supports the
     * use of variables on the right-hand side of the equation)
     */
    public String[] getVariablesUsed() {
        StringBuilder sb = new StringBuilder(leftValue == null ? "" : leftValue);
        for (Predicate predicate : predicates) {
            if (predicate instanceof BinaryPredicate) {
                BinaryPredicate bp = (BinaryPredicate) predicate;
                String rv = bp.getRightValue();
                if (rv != null) sb.append(rv);
            }
        }
        return ExpandVariables.getReferencedNames(sb.toString());
    }

    public ComparisonAssertion() {
    }

    public Predicate[] getPredicates() {
        return predicates;
    }

    public void setPredicates(Predicate... predicates) {
        this.predicates = predicates;
    }

    public String getExpression1() {
        return leftValue;
    }

    public void setExpression1(String expression1) {
        this.leftValue = expression1;
    }

    private boolean check() {
        if (predicates == null || predicates.length == 0) {
            predicates = new Predicate[] { new BinaryPredicate() };
            return true;
        } else
            return predicates.length == 1 && predicates[0] instanceof BinaryPredicate;
    }

    private BinaryPredicate compat() {
        return ((BinaryPredicate)predicates[0]);
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public String getExpression2() {
        if (check()) return compat().getRightValue();
        return null;
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public void setExpression2(String expression2) {
        if (check()) compat().setRightValue(expression2);
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public ComparisonOperator getOperator() {
        if (check()) return compat().getOperator();
        return null;
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public void setOperator(ComparisonOperator operator) {
        if (check()) compat().setOperator(operator);
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public boolean isNegate() {
        return check() && compat().isNegated();
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public void setNegate(boolean negate) {
        if (check()) compat().setNegated(negate);
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public boolean isCaseSensitive() {
        return check() && compat().isCaseSensitive();
    }

    /** @deprecated -- use a {@link BinaryPredicate} in {@link #predicates}*/
    public void setCaseSensitive(boolean caseSensitive) {
        if (check()) compat().setCaseSensitive(caseSensitive);
    }

    public AssertionMetadata meta() {
        clearCachedMetadata(getClass().getName());
        DefaultAssertionMetadata meta = super.defaultMeta();
        meta.put(AssertionMetadata.LONG_NAME, "Evaluate an expression that may include variables against various rules");

        // Request to appear in "misc" ("Service Availability") palette folder
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(AssertionMetadata.WSP_EXTERNAL_NAME, "ComparisonAssertion");

        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.comparison.server.ServerComparisonAssertion");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.comparison.console.ComparisonPropertiesDialog");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Comparison" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/check16.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME, new Functions.Unary<String, ComparisonAssertion>() {
            public String call(ComparisonAssertion ass) {
                StringBuffer name = new StringBuffer("Proceed if ");
                name.append(ass.getExpression1());
                if (ass.getPredicates().length == 1 && ass.getPredicates()[0] instanceof BinaryPredicate) {
                    if (ass.getOperator() == ComparisonOperator.CONTAINS) {
                        if (ass.isNegate()) {
                            name.append(" does not contain ");
                        } else {
                            name.append(" contains ");
                        }
                    } else {
                        name.append(" is ");
                        if (ass.isNegate()) name.append("not ");
                        name.append(ass.getOperator().toString());
                    }
                    if (!ass.getOperator().isUnary()) {
                        name.append(" ").append(ass.getExpression2());
                    }
                } else {
                    name.append(" matches rules (TODO Describe them!)"); // TODO
                }
                return name.toString();
            }
        });
        return meta;
    }

}
