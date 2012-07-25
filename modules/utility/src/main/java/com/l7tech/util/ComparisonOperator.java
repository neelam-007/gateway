package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    NE("not equal to", new NotEqualsStrategy()),

    /** Always true. */
    TRUE("always true", new FixedStrategy(true)),

    /** Always false. */
    FALSE("always false", new FixedStrategy(false));

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

    /**
     * @param left value
     * @param right value
     * @param ignoreCase ignore case
     * @param <T> Although both left and right will be comparable, that does not mean that they are, they still must
     * be the same type or comparable types.
     * @return true if comparison strategy is successful
     * @throws com.l7tech.util.ComparisonOperator.NotComparableException if the types are not comparable
     */
    public <T extends Comparable> boolean compare(T left, @Nullable T right, boolean ignoreCase) throws NotComparableException, RightValueIsNullException {
        return strategy.compare(left, right, ignoreCase);
    }

    public static class NotComparableException extends Exception{
        public NotComparableException(String message) {
            super(message);
        }
    }

    public static class RightValueIsNullException extends Exception{
    }

    /**
     * Compares two Comparables for equality.
     */
    private static class EqualsStrategy extends DefaultComparisonStrategy {
        private EqualsStrategy() {
            super(0, 0);
        }

        @Override
        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) throws NotComparableException {
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

            if (left instanceof CharSequence && right instanceof CharSequence) {
                // Use constant-time comparison when comparing strings
                final CharSequence lc = (CharSequence) left;
                final CharSequence rc = (CharSequence) right;
                final int len = lc.length();
                if (len != rc.length())
                    return false;
                int val = 0;
                for (int i = 0; i < len; ++i) {
                    val |= lc.charAt(i) ^ rc.charAt(i);
                }
                return 0 == val;
            }

            return super.compare(left, right, ignoreCase);
        }

    }

    /**
     * Negated EQ.
     */
    private static class NotEqualsStrategy extends EqualsStrategy {
        @Override
        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) throws NotComparableException {
            return !super.compare(left, right, ignoreCase);
        }
    }

    /**
     * Unary operator that returns true if Comparable is null or its toString() returns empty string.
     */
    private static class EmptyStrategy extends ComparisonStrategy {
        protected EmptyStrategy() {
            super(true);
        }

        @Override
        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
            return left == null || left.toString().length() == 0;
        }
    }

    /**
     * Binary operator that returns true if right String is contained within left String.
     */
    private static class ContainsStrategy extends ComparisonStrategy {
        protected ContainsStrategy() {
            super(false);
        }

        @Override
        public boolean compare(Comparable left, @Nullable Comparable right, boolean ignoreCase) throws RightValueIsNullException {
            if (right == null) {
                throw new RightValueIsNullException();
            }
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

        public abstract boolean compare(Comparable left, Comparable right, boolean ignoreCase) throws NotComparableException, RightValueIsNullException;
        public boolean isUnary() { return unary; }
    }

    /**
     * Default comparison strategy: compare two Comparables using this operator.
     */
    private static class DefaultComparisonStrategy extends ComparisonStrategy {
        private final int compareVal1;
        private final int compareVal2;

        private DefaultComparisonStrategy(int compareVal1, int compareVal2) {
            super(false);
            this.compareVal1 = compareVal1;
            this.compareVal2 = compareVal2;
        }

        @Override
        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) throws NotComparableException {
            if (left == null) throw new NullPointerException();
            if (right == null) return false;
            boolean match;
            int comp;
            try {
                //noinspection unchecked
                comp = left.compareTo(right);
            } catch ( ClassCastException cce ) {
                throw new NotComparableException("Cannot compare type '" + left.getClass() + "' with type '" + right.getClass() + "'");
            }
            if (comp > 0) comp = 1;
            if (comp < 0) comp = -1;
            match = comp == compareVal1 || comp == compareVal2;
            return match;
        }
    }

    /**
     * Always returns the same boolean value, no matter what operands are submitted
     */
    private static class FixedStrategy extends ComparisonStrategy {
        private final boolean what;

        public FixedStrategy(boolean b) {
            super(true);
            this.what = b;
        }

        @Override
        public boolean compare(Comparable left, Comparable right, boolean ignoreCase) {
            return what;
        }
    }
}
