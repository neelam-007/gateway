package com.l7tech.util;

import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Utility class for a disjoint unions.
 *
 * <p>If one of the values is for "success" then this should be the right value
 * with the "failure" value being the left value.</p>
 *
 * <p>Either does not permit null values. To use nulls wrap the optional value
 * in an <code>Option</code>, or use one of the provided constructors that does
 * this.</p>
 *
 * @see Eithers
 * @see Option
 */
public class Either<A,B> implements Serializable {

    //- PUBLIC

    /**
     * Create an either with the given left value.
     *
     * @param a The left value to use.
     * @param <A> The left type
     * @param <B> The right type
     * @return An either for the given left value
     */
    @NotNull
    public static <A,B> Either<A,B> left( @NotNull final A a ) {
        return new Either<A,B>( a, null );
    }

    /**
     * Create an either with the given optional left value.
     *
     * @param a The left value to use.
     * @param <A> The left type
     * @param <B> The right type
     * @return An either for the given left value
     */
    @NotNull
    public static <A,B> Either<Option<A>,B> leftOption( @Nullable final A a ) {
        return new Either<Option<A>,B>( optional(a), null );
    }

    /**
     * Create an either with the given right value.
     *
     * @param b The right value to use.
     * @param <A> The left type
     * @param <B> The right type
     * @return An either for the given right value
     */
    @NotNull
    public static <A,B> Either<A,B> right( @NotNull final B b ) {
        return new Either<A,B>( null, b );
    }

    /**
     * Create an either with the given optional right value.
     *
     * @param b The right value to use.
     * @param <A> The left type
     * @param <B> The right type
     * @return An either for the given right value
     */
    @NotNull
    public static <A,B> Either<A,Option<B>> rightOption( @Nullable final B b ) {
        return new Either<A,Option<B>>( null, optional( b ) );
    }

    /**
     * True if this either is the left value.
     *
     * @return True if left
     */
    public boolean isLeft() {
        return a != null;
    }

    /**
     * True if this either is the right value.
     *
     * @return True if right
     */
    public boolean isRight() {
        return b != null;
    }

    /**
     * Get the left value for this either
     *
     * @return The left value
     */
    @NotNull
    public A left() {
        if (!isLeft()) throw new IllegalStateException( "Right valued either" );
        return a;
    }

    /**
     * Get the right value for this either
     *
     * @return The right value
     */
    @NotNull
    public B right() {
        if (!isRight()) throw new IllegalStateException( "Left valued either" );
        return b;
    }

    /**
     * Convert the left side of this either to an option.
     *
     * @return The optional left value.
     */
    @NotNull
    public Option<A> toLeftOption() {
        return isLeft() ? some( left() ) : Option.<A>none() ;
    }

    /**
     * Convert the right side of this either to an option.
     *
     * @return The optional right value.
     */
    @NotNull
    public Option<B> toRightOption() {
        return isRight() ? some( right() ) : Option.<B>none() ;
    }

    /**
     * Create a swapped version of this either.
     *
     * @return The either with left and right values swapped
     */
    public Either<B,A> swap() {
        return new Either<B, A>( b, a );
    }

    /**
     * Invoke either the given left or right function.
     *
     * @param left The function to invoke if this either is the left value
     * @param right The function to invoke if this either is the right value
     * @param <R> The function return type
     * @return The result of invoking the left or right function
     */
    @Nullable
    public <R> R either( @NotNull final Functions.Unary<R,A> left,
                         @NotNull final Functions.Unary<R,B> right ) {
        if ( isLeft() ) {
            return left.call( a );
        } else {
            return right.call( b );
        }
    }

    /**
     * Invoke either the given left or right function with an exception.
     *
     * @param left The function to invoke if this either is the left value
     * @param right The function to invoke if this either is the right value
     * @param <R> The function return type
     * @param <E> The function exception type
     * @return The result of invoking the left or right function
     * @throws E if the left or right function throws E
     */
    @Nullable
    public <R,E extends Throwable> R either( @NotNull final Functions.UnaryThrows<R,A,E> left,
                                             @NotNull final Functions.UnaryThrows<R,B,E> right ) throws E {
        if ( isLeft() ) {
            return left.call( a );
        } else {
            return right.call( b );
        }
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        final Either either = (Either) o;

        if ( a != null ? !a.equals( either.a ) : either.a != null ) return false;
        if ( b != null ? !b.equals( either.b ) : either.b != null ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        return result;
    }

    //- PACKAGE

    Either( final A a, final B b ) {
        this.a = a;
        this.b = b;
    }

    //- PRIVATE

    private final A a;
    private final B b;

}
