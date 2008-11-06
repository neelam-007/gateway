/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents a strategy for unary or binary comparisons.
 */
public class ComparisonOperator implements Serializable {
    private static int n = 0;
    public static final ComparisonOperator LT = new ComparisonOperator(n++, "LT", "less than", false, -1, -1);
    public static final ComparisonOperator LE = new ComparisonOperator(n++, "LE", "less than or equal to", false, -1, 0);
    public static final ComparisonOperator EQ = new EqualsOperator(n++, "EQ", "equal to");
    public static final ComparisonOperator GT = new ComparisonOperator(n++, "GT", "greater than", false, 1, 1);
    public static final ComparisonOperator GE = new ComparisonOperator(n++, "GE", "greater than or equal to", false, 1, 0);
    public static final ComparisonOperator EMPTY = new EmptyOperator(n++, "EMPTY", "empty");
    public static final ComparisonOperator CONTAINS = new ContainsOperator(n++, "CONTAINS", "contain");
    public static final ComparisonOperator NE = new NotEqualsOperator(n++, "NE", "not equal to");

    private static ComparisonOperator[] VALUES = { LT, LE, EQ, GT, GE, EMPTY, CONTAINS }; // NE not included
    private static final Map byShortName = new HashMap();

    static {
        for (int i = 0; i < VALUES.length; i++) {
            ComparisonOperator operator = VALUES[i];
            byShortName.put(operator.getShortName(), operator);
        }
    }

    private ComparisonOperator(int num, String shortName, String name, boolean unary, int compareVal1, int compareVal2) {
        this.num = num;
        this.shortName = shortName;
        this.name = name;
        this.unary = unary;
        this.compareVal1 = compareVal1;
        this.compareVal2 = compareVal2;
    }

    public ComparisonOperator(int num, String shortName, String name, boolean unary) {
        this.num = num;
        this.shortName = shortName;
        this.name = name;
        this.unary = unary;
        this.compareVal1 = Integer.MAX_VALUE;
        this.compareVal2 = Integer.MAX_VALUE;
    }

    public static ComparisonOperator[] getValues() {
        ComparisonOperator[] clone = new ComparisonOperator[VALUES.length];
        System.arraycopy(VALUES, 0, clone, 0, VALUES.length);
        return clone;
    }

    public static ComparisonOperator getByShortName(String shortName) {
        return (ComparisonOperator)byShortName.get(shortName);
    }

    // This method is invoked reflectively by WspEnumTypeMapping
    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public String objectToString(Object target) {
                ComparisonOperator op = (ComparisonOperator)target;
                return op.getShortName();
            }

            public Object stringToObject(String value) throws IllegalArgumentException {
                ComparisonOperator op = ComparisonOperator.getByShortName(value);
                if (op == null) throw new IllegalArgumentException("Unknown Operator short name: '" + value + "'");
                return op;
            }
        };
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

    /**
     * Default comparison strategy: compare two Comparables using this operator.
     *
     * @param left   the left side of the comparison.  MUST NOT be null.
     * @param right  the right side of the comparison.  MAY be null.
     * @param ignoreCase  Ignored by operators other than CONTAINS and EQ.
     *                    If true, either side that is a String will be forced to lowercase before comparing
     */
    public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
        if (left == null) throw new NullPointerException();
        if (right == null) return false;
        boolean match;
        int comp = left.compareTo(right);
        if (comp > 0) comp = 1;
        if (comp < 0) comp = -1;
        match = comp == getCompareVal1() || comp == getCompareVal2();
        return match;
    }

    private final int num;
    private final String shortName;
    private final String name;
    private final boolean unary;
    private final int compareVal1;
    private final int compareVal2;

    /** Compares two Comparables for equality. */
    private static class EqualsOperator extends ComparisonOperator {
        public EqualsOperator(int num, String shortName, String name) {
            super(num, shortName, name, false, 0, 0);
        }

        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
            if (ignoreCase) {
                if (left instanceof String) {
                    String s = (String)left;
                    left = s.toLowerCase();
                }
                if (right instanceof String) {
                    String s = (String)right;
                    right = s.toLowerCase();
                }
            }
            return super.compare(left, right, ignoreCase);
        }
    }

    /** Negated EQ. */
    private static class NotEqualsOperator extends EqualsOperator {
        public NotEqualsOperator(int num, String shortName, String name) {
            super(num, shortName, name);
        }

        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
            return !super.compare(left, right, ignoreCase);
        }
    }

    /** Unary operator that returns true if Comparable is null or its toString() returns empty string. */
    private static class EmptyOperator extends ComparisonOperator {
        public EmptyOperator(int num, String shortName, String name) {
            super(num, shortName, name, true);
        }

        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
            return left == null || left.toString().length() == 0;
        }
    }

    /** Binary operator that returns true if right String is contained within left String. */
    private static class ContainsOperator extends ComparisonOperator {
        public ContainsOperator(int num, String shortName, String name) {
            super(num, shortName, name, false);
        }

        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
            String s1 = left.toString();
            String s2 = right.toString();

            if (ignoreCase) {
                s1 = s1.toLowerCase();
                s2 = s2.toLowerCase();
            }
            return s1.contains(s2);
        }
    }
}
