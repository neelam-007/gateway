package com.l7tech.common.util;

/**
 * Plumbing for building simple callbacks that don't deserve to have a special interface created just for them.
 * <p/>
 * Requires Java 5.
 */
public interface Functions {
    /** A function that takes no arguments and returns a value. */
    public interface Nullary<R> {
        R call();
    }

    /**
     * A function that takes no arguments and returns no value.
     * Provided for symmetry; instead of this, you might consider using the equivalent but more idiomatic Runnable.
     */
    public interface NullaryVoid {
        void call();
    }

    /** A function that takes one argument and returns a value. */
    public interface Unary<R, P1> {
        R call(P1 p1);
    }

    /** A function that takes one argument and returns void. */
    public interface UnaryVoid<P1> {
        void call(P1 p1);
    }

    /** A function that takes two arguments and returns a value. */
    public interface Binary<R, P1, P2> {
        R call(P1 p1, P2 p2);
    }

    /** A function that takes two arguments and returns void. */
    public interface BinaryVoid<P1, P2> {
        void call(P1 p1, P2 p2);
    }

    /** A function that takes three arguments and returns a value. */
    public interface Ternary<R, P1, P2, P3> {
        R call(P1 p1, P2 p2, P3 p3);
    }

    /** A function that takes three arguments and returns void. */
    public interface TernaryVoid<P1, P2, P3> {
        void call(P1 p1, P2 p2, P3 p3);
    }
}
