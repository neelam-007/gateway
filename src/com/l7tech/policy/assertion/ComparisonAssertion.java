package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.ExpandVariables;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Compares two values.  Variable names are supported using ${assertion.var} syntax.
 * @see com.l7tech.server.message.PolicyEnforcementContext#getVariable(String)
 * @see com.l7tech.server.message.PolicyEnforcementContext#setVariable(String, Object)
 */
public class ComparisonAssertion extends Assertion implements UsesVariables {
    private String expression1;
    private String expression2;
    private Operator operator = Operator.EQ;
    private boolean negate = false;
    private boolean caseSensitive = true;

    public String[] getVariablesUsed() {
        return ExpandVariables.getReferencedNames(expression1 + expression2);
    }

    public static interface Comparer {
        boolean matches(Object val1, Object val2, ComparisonAssertion assertion);
    }

    public static final class Operator implements Serializable {
        private static int n = 0;
        public static final Operator LT = new Operator(n++, "LT", "less than", false, -1, -1);
        public static final Operator LE = new Operator(n++, "LE", "less than or equal to", false, -1, 0);
        public static final Operator EQ = new Operator(n++, "EQ", "equal to", false, 0, 0);
        public static final Operator GT = new Operator(n++, "GT", "greater than", false, 1, 1);
        public static final Operator GE = new Operator(n++, "GE", "greater than or equal to", false, 1, 0);
        public static final Operator EMPTY = new Operator(n++, "EMPTY", "empty", true, new Comparer() {
            public boolean matches(Object val1, Object val2, ComparisonAssertion assertion) {
                return val1 == null || val1.toString().length() == 0;
            }
        });
        public static final Operator CONTAINS = new Operator(n++, "CONTAINS", "contains", false, new Comparer() {
            public boolean matches(Object val1, Object val2, ComparisonAssertion assertion) {
                if (val1 instanceof String && val2 instanceof String) {
                    String s1 = (String)val1;
                    String s2 = (String)val2;

                    if (!assertion.isCaseSensitive()) {
                        s1 = s1.toLowerCase();
                        s2 = s2.toLowerCase();
                    }
                    return s1.contains(s2);
                } else {
                    throw new IllegalArgumentException("The CONTAINS operator is only supported with String values");
                }
            }
        });

        private static Operator[] VALUES = { LT, LE, EQ, GT, GE, EMPTY, CONTAINS };
        private static final Map byShortName = new HashMap();

        static {
            for (int i = 0; i < VALUES.length; i++) {
                Operator operator = VALUES[i];
                byShortName.put(operator.getShortName(), operator);
            }
        }

        private Operator(int num, String shortName, String name, boolean unary, int compareVal1, int compareVal2) {
            this.num = num;
            this.shortName = shortName;
            this.name = name;
            this.unary = unary;
            this.comparer = null;
            this.compareVal1 = compareVal1;
            this.compareVal2 = compareVal2;
        }

        public Operator(int num, String shortName, String name, boolean unary, Comparer comparer) {
            this.num = num;
            this.shortName = shortName;
            this.name = name;
            this.unary = unary;
            this.comparer = comparer;
            this.compareVal1 = Integer.MAX_VALUE;
            this.compareVal2 = Integer.MAX_VALUE;
        }

        public static Operator[] getValues() {
            Operator[] clone = new Operator[VALUES.length];
            System.arraycopy(VALUES, 0, clone, 0, VALUES.length);
            return clone;
        }

        public static Operator getByShortName(String shortName) {
            return (Operator)byShortName.get(shortName);
        }

        public int getNum() {
            return num;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return name;
        }

        public boolean isUnary() {
            return unary;
        }

        public int getCompareVal1() {
            return compareVal1;
        }

        public int getCompareVal2() {
            return compareVal2;
        }

        protected Object readResolve() {
            return VALUES[num];
        }

        public String getShortName() {
            return shortName;
        }

        public Comparer getComparer() {
            return comparer;
        }

        private final int num;
        private final String shortName;
        private final String name;
        private final boolean unary;
        private final int compareVal1;
        private final int compareVal2;
        private final Comparer comparer;
    }

    public ComparisonAssertion() {
    }

    public ComparisonAssertion(boolean negate, String expr1, Operator operator, String expr2) {
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

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
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
