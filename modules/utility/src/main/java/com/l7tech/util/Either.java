package com.l7tech.util;

import static com.l7tech.util.CollectionUtils.foreach;
import com.l7tech.util.Functions.BinaryVoid;
import static com.l7tech.util.Functions.partial;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for a disjoint unions.
 *
 * <p>If one of the values is for "success" then this should be the right value
 * with the "failure" value being the left value.</p>
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
    public static <A,B> Either<A,B> left( final A a ) {
        return new Either<A,B>( a, null );
    }

    /**
     * Create an either with the given right value.
     *
     * @param b The right value to use.
     * @param <A> The left type
     * @param <B> The right type
     * @return An either for the given right value
     */
    public static <A,B> Either<A,B> right( final B b ) {
        return new Either<A,B>( null, b );
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
    public A left() {
        return a;
    }

    /**
     * Get the right value for this either
     *
     * @return The right value
     */
    public B right() {
        return b;
    }

    /**
     * Invoke either the given left or right function.
     *
     * @param left The function to invoke if this either is the left value
     * @param right The function to invoke if this either is the right value
     * @param <R> The function return type
     * @return The result of invoking the left or right function
     */
    public <R> R either( final Functions.Unary<R,A> left,
                         final Functions.Unary<R,B> right ) {
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
    public <R,E extends Throwable> R either( final Functions.UnaryThrows<R,A,E> left,
                                             final Functions.UnaryThrows<R,B,E> right ) throws E {
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

    /**
     * Get the left values from the given list of eithers.
     *
     * @param eithers The list of eithers
     * @param <L> The left type
     * @param <R> The right type
     * @return The list of left values
     */
    @NotNull
    public static <L,R> List<L> lefts( @NotNull final List<Either<L,R>> eithers ) {
        return toList( eithers, new BinaryVoid<List<L>,Either<L, R>>() {
            @Override
            public void call( final List<L> list,
                              final Either<L, R> either ) {
                if ( either.isLeft() ) list.add( either.left() );
            }
        } );
    }

    /**
     * Get the right values from the given list of eithers.
     *
     * @param eithers The list of eithers
     * @param <L> The left type
     * @param <R> The right type
     * @return The list of right values
     */
    @NotNull
    public static <L,R> List<R> rights( @NotNull final List<Either<L,R>> eithers ) {
        return toList( eithers, new BinaryVoid<List<R>,Either<L, R>>() {
            @Override
            public void call( final List<R> list,
                              final Either<L, R> either ) {
                if ( either.isRight() ) list.add( either.right() );
            }
        } );
    }

    //- PRIVATE

    private final A a;
    private final B b;

    private Either( final A a, final B b ) {
        this.a = a;
        this.b = b;
    }

    private static <L,R,T> List<T> toList( final List<Either<L,R>> eithers,
                                           final BinaryVoid<List<T>,Either<L, R>> builder ) {
        final List<T> list = new ArrayList<T>();
        foreach( eithers, false, partial(builder,list));
        return list;
    }

}
