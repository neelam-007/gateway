/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import javax.xml.bind.annotation.XmlEnum;

/**
 * Class that represents a strategy for unary or binary comparisons.
 */
@XmlEnum(String.class)
public enum ComparisonOperator {
    /** Less than. */
    LT("less than", -1, -1),

    /** Less than or equal to. */
    LE("less than or equal to", -1, 0),

    /** Equal to. */
    EQ("equal to", new EqualsStrategy()),

    /** Greater than. */
    GT("greater than", 1, 1),

    /** Greater than or equal to. */
    GE("greater than or equal to", 1, 0),

    /** Is empty. */
    EMPTY("empty", new EmptyStrategy()),

    /** Contains. */
    CONTAINS("contain", new ContainsStrategy()),

    /** Not equal to. */
    NE("not equal to", new NotEqualsStrategy());

    private ComparisonOperator(String name, ComparisonStrategy strategy) {
        this.name = name;
        this.strategy = strategy;
    }

    private ComparisonOperator(String name, int compareVal1, int compareVal2) {
        this(name, new DefaultComparisonStrategy(compareVal1, compareVal2));
    }

    public String toString() {
        return name;
    }

    public boolean isUnary() {
        return strategy.isUnary();
    }

    public String getName() {
        return name;
    }

    private final String name;
    private final ComparisonStrategy strategy;

    public <T extends Comparable> boolean compare(T left, T right, boolean ignoreCase) {
        return strategy.compare(left, right, ignoreCase);
    }

    /** Compares two Comparables for equality. */
    private static class EqualsStrategy extends DefaultComparisonStrategy {
        private EqualsStrategy() {
            super(0, 0);
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
    private static class NotEqualsStrategy extends EqualsStrategy {
        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
            return !super.compare(left, right, ignoreCase);
        }
    }

    /** Unary operator that returns true if Comparable is null or its toString() returns empty string. */
    private static class EmptyStrategy extends ComparisonStrategy {
        protected EmptyStrategy() {
            super(true);
        }

        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
            return left == null || left.toString().length() == 0;
        }
    }

    /** Binary operator that returns true if right String is contained within left String. */
    private static class ContainsStrategy extends ComparisonStrategy {
        protected ContainsStrategy() {
            super(false);
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

    private static abstract class ComparisonStrategy {
        private final boolean unary;

        protected ComparisonStrategy(boolean unary) {
            this.unary = unary;
        }

        public abstract boolean compare(Comparable left, Comparable right, boolean ignoreCase);
        public boolean isUnary() { return unary; }
    }

    /**
     * Default comparison strategy: compare two Comparables using this operator.
     *
     */
    private static class DefaultComparisonStrategy extends ComparisonStrategy {
        private final int compareVal1;
        private final int compareVal2;

        private DefaultComparisonStrategy(int compareVal1, int compareVal2) {
            super(false);
            this.compareVal1 = compareVal1;
            this.compareVal2 = compareVal2;
        }

        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
            if (left == null) throw new NullPointerException();
            if (right == null) return false;
            boolean match;
            @SuppressWarnings({"unchecked"})
            int comp = left.compareTo(right);
            if (comp > 0) comp = 1;
            if (comp < 0) comp = -1;
            match = comp == compareVal1 || comp == compareVal2;
            return match;
        }
    }
}
