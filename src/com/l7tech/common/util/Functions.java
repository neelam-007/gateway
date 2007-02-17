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

    /** A function that takes four arguments and returns a value. */
    public interface Quaternary<R, P1, P2, P3, P4> {
        R call(P1 p1, P2 p2, P3 p3, P4 p4);
    }

    /** A function that takes four arguments and returns void. */
    public interface QuaternaryVoid<P1, P2, P3, P4> {
        void call(P1 p1, P2 p2, P3 p3, P4 p4);
    }

    /** A funciton that takes five arguments and returns a value. */
    public interface Quinary<R, P1, P2, P3, P4, P5> {
        R call(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
    }

    /** A function that takes five arguments and returns void. */
    public interface QuinaryVoid<P1, P2, P3, P4, P5> {
        void call(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
    }

    /** A function that takes six arguments and returns a value. */
    public interface Sestary<R, P1, P2, P3, P4, P5, P6> {
        R call(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6);
    }

    /** A function that takes six arguments and returns void. */
    public interface SestaryVoid<P1, P2, P3, P4, P5, P6> {
        void call(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6);
    }

    // That's probably plenty ridiculous enough; past that, can switch to Variadic

    /** A function that takes any number of arguments and returns a value. */
    public interface Variadic<R, P> {
        R call(P ... args);
    }

    /** A function that takes any number of arguments and returns void. */
    public interface VariadicVoid<P> {
        void call(P ... args);
    }
}
