package com.l7tech.util;

import static com.l7tech.util.CollectionUtils.foreach;
import com.l7tech.util.Functions.BinaryVoid;
import static com.l7tech.util.Functions.partial;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for working with Eithers.
 */
public class Eithers {

    //- PUBLIC

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
        return toList( eithers, new BinaryVoid<List<L>, Either<L, R>>() {
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
        return toList( eithers, new BinaryVoid<List<R>, Either<L, R>>() {
            @Override
            public void call( final List<R> list,
                              final Either<L, R> either ) {
                if ( either.isRight() ) list.add( either.right() );
            }
        } );
    }

    /**
     * Extract a throwable/result value from an either.
     *
     * @param either The either to process
     * @param <R> The result (or return) type
     * @param <T> The throwable type
     * @return The result (right) value (never null)
     * @throws T If the either left valued
     */
    @NotNull
    public static <R,T extends Throwable> R extract( @NotNull final Either<T,R> either ) throws T {
        if ( either.isLeft() ) {
            throw either.left();
        } else {
            return either.right();
        }
    }

    /**
     * Extract a throwable/result value from a nested either.
     *
     * <p>For use with void methods, return an empty option of type Void. Be
     * sure to call "isSome()" on the result to ensure any nested either is
     * extracted.</p>
     *
     * @param either The either to process
     * @param <R> The result (or return) type
     * @param <T1> The first throwable type
     * @param <T2> The second throwable type
     * @return The result (right) value (never null)
     * @throws T1 If the corresponding either is left valued
     * @throws T2 If the corresponding either is left valued
     */
    @NotNull
    public static <R, T1 extends Throwable, T2 extends Throwable>R extract2( @NotNull final Either<T1,Either<T2,R>> either ) throws T1, T2 {
        return extract( extract( either ) );
    }

    /**
     * Extract a throwable/result value from a nested either.
     *
     * <p>For use with void methods, return an empty option of type Void. Be
     * sure to call "isSome()" on the result to ensure any nested either is
     * extracted.</p>
     *
     * @param either The either to process
     * @param <R> The result (or return) type
     * @param <T1> The first throwable type
     * @param <T2> The second throwable type
     * @param <T3> The third throwable type
     * @return The result (right) value (never null)
     * @throws T1 If the corresponding either is left valued
     * @throws T2 If the corresponding either is left valued
     * @throws T3 If the corresponding either is left valued
     */
    @NotNull
    public static <R, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable>R extract3( @NotNull final Either<T1,Either<T2,Either<T3,R>>> either ) throws T1, T2, T3 {
        return extract( extract2( either ) );
    }

    /**
     * Extract a throwable/result value from a nested either.
     *
     * <p>For use with void methods, return an empty option of type Void. Be
     * sure to call "isSome()" on the result to ensure any nested either is
     * extracted.</p>
     *
     * @param either The either to process
     * @param <R> The result (or return) type
     * @param <T1> The first throwable type
     * @param <T2> The second throwable type
     * @param <T3> The third throwable type
     * @param <T4> The fourth throwable type
     * @return The result (right) value (never null)
     * @throws T1 If the corresponding either is left valued
     * @throws T2 If the corresponding either is left valued
     * @throws T3 If the corresponding either is left valued
     * @throws T4 If the corresponding either is left valued
     */
    @NotNull
    public static <R, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, T4 extends Throwable>R extract4( @NotNull final Either<T1,Either<T2,Either<T3,Either<T4,R>>>> either ) throws T1, T2, T3, T4 {
        return extract( extract3( either ) );
    }

    /**
     * Extract a throwable/result value from a nested either.
     *
     * <p>For use with void methods, return an empty option of type Void. Be
     * sure to call "isSome()" on the result to ensure any nested either is
     * extracted.</p>
     *
     * @param either The either to process
     * @param <R> The result (or return) type
     * @param <T1> The first throwable type
     * @param <T2> The second throwable type
     * @param <T3> The third throwable type
     * @param <T4> The fourth throwable type
     * @param <T5> The fifth throwable type
     * @return The result (right) value (never null)
     * @throws T1 If the corresponding either is left valued
     * @throws T2 If the corresponding either is left valued
     * @throws T3 If the corresponding either is left valued
     * @throws T4 If the corresponding either is left valued
     * @throws T5 If the corresponding either is left valued
     */
    @NotNull
    public static <R, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, T4 extends Throwable, T5 extends Throwable>R extract5( @NotNull final Either<T1,Either<T2,Either<T3,Either<T4,Either<T5,R>>>>> either ) throws T1, T2,  T3, T4, T5 {
        return extract( extract4( either ) );
    }

    /**
     * Extract a throwable/result value from a nested either.
     *
     * <p>For use with void methods, return an empty option of type Void. Be
     * sure to call "isSome()" on the result to ensure any nested either is
     * extracted.</p>
     *
     * @param either The either to process
     * @param <R> The result (or return) type
     * @param <T1> The first throwable type
     * @param <T2> The second throwable type
     * @param <T3> The third throwable type
     * @param <T4> The fourth throwable type
     * @param <T5> The fifth throwable type
     * @param <T6> The sixth throwable type
     * @return The result (right) value (never null)
     * @throws T1 If the corresponding either is left valued
     * @throws T2 If the corresponding either is left valued
     * @throws T3 If the corresponding either is left valued
     * @throws T4 If the corresponding either is left valued
     * @throws T5 If the corresponding either is left valued
     * @throws T6 If the corresponding either is left valued
     */
    @NotNull
    public static <R, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, T4 extends Throwable, T5 extends Throwable, T6 extends Throwable>R extract6( @NotNull final Either<T1,Either<T2,Either<T3,Either<T4,Either<T5,Either<T6,R>>>>>> either ) throws T1, T2, T3, T4, T5, T6 {
        return extract( extract5( either ) );
    }

    /**
     * Extract a throwable/result value from a nested either.
     *
     * <p>For use with void methods, return an empty option of type Void. Be
     * sure to call "isSome()" on the result to ensure any nested either is
     * extracted.</p>
     *
     * @param either The either to process
     * @param <R> The result (or return) type
     * @param <T1> The first throwable type
     * @param <T2> The second throwable type
     * @param <T3> The third throwable type
     * @param <T4> The fourth throwable type
     * @param <T5> The fifth throwable type
     * @param <T6> The sixth throwable type
     * @param <T7> The seventh throwable type
     * @return The result (right) value (never null)
     * @throws T1 If the corresponding either is left valued
     * @throws T2 If the corresponding either is left valued
     * @throws T3 If the corresponding either is left valued
     * @throws T4 If the corresponding either is left valued
     * @throws T5 If the corresponding either is left valued
     * @throws T6 If the corresponding either is left valued
     * @throws T7 If the corresponding either is left valued
     */
    @NotNull
    public static <R, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, T4 extends Throwable, T5 extends Throwable, T6 extends Throwable, T7 extends Throwable>R extract7( @NotNull final Either<T1,Either<T2,Either<T3,Either<T4,Either<T5,Either<T6,Either<T7,R>>>>>>> either ) throws T1, T2, T3, T4, T5, T6, T7 {
        return extract( extract6( either ) );
    }

    /**
     * Construct a nested either with the first left value.
     * 
     * @param left1 The value to use
     * @param <R> The right type
     * @param <L1> The first left type
     * @param <L2> The second left type
     * @return The nested either
     */
    @NotNull
    public static <L1, L2, R> E2<L1,L2,R> left2_1( @NotNull final L1 left1 ) {
        return new E2<L1,L2,R>( left1, null, null );
    }

    /**
     * Construct a nested either with the second left value.
     *
     * @param left2 The value to use
     * @param <R> The right type
     * @param <L1> The first left type
     * @param <L2> The second left type
     * @return The nested either
     */
    @NotNull
    public static <L1, L2, R> E2<L1,L2,R> left2_2( @NotNull final L2 left2 ) {
        return new E2<L1,L2,R>( null, left2, null );
    }

    /**
     * Construct a nested either with the right value.
     *
     * @param right The value to use
     * @param <R> The right type
     * @param <L1> The first left type
     * @param <L2> The second left type
     * @return The nested either
     */
    @NotNull
    public static <L1, L2, R> E2<L1,L2,R> right2( @NotNull final R right ) {
        return new E2<L1,L2,R>( null, null, right );
    }

    @NotNull
    public static <L1, L2, L3, R> E3<L1,L2,L3,R> left3_1( @NotNull final L1 left1 ) {
        return new E3<L1,L2,L3,R>( left1, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, R> E3<L1,L2,L3,R> left3_2( @NotNull final L2 left2 ) {
        return new E3<L1,L2,L3,R>( null, left2, null, null );
    }

    @NotNull
    public static <L1, L2, L3, R> E3<L1,L2,L3,R> left3_3( @NotNull final L3 left3 ) {
        return new E3<L1,L2,L3,R>( null, null, left3, null );
    }

    @NotNull
    public static <L1, L2, L3, R> E3<L1,L2,L3,R> right3( @NotNull final R right ) {
        return new E3<L1,L2,L3,R>( null, null, null, right );
    }

    @NotNull
    public static <L1, L2, L3, L4, R> E4<L1,L2,L3,L4,R> left4_1( @NotNull final L1 left1 ) {
        return new E4<L1,L2,L3,L4,R>( left1, null,null,  null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, R> E4<L1,L2,L3,L4,R> left4_2( @NotNull final L2 left2 ) {
        return new E4<L1,L2,L3,L4,R>( null, left2, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, R> E4<L1,L2,L3,L4,R> left4_3( @NotNull final L3 left3 ) {
        return new E4<L1,L2,L3,L4,R>( null, null, left3, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, R> E4<L1,L2,L3,L4,R> left4_4( @NotNull final L4 left4 ) {
        return new E4<L1,L2,L3,L4,R>( null, null, null, left4, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, R> E4<L1,L2,L3,L4,R> right4( @NotNull final R right ) {
        return new E4<L1,L2,L3,L4,R>( null, null, null, null, right );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, R> E5<L1,L2,L3,L4,L5,R> left5_1( @NotNull final L1 left1 ) {
        return new E5<L1,L2,L3,L4,L5,R>( left1, null,null,  null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, R> E5<L1,L2,L3,L4,L5,R> left5_2( @NotNull final L2 left2 ) {
        return new E5<L1,L2,L3,L4,L5,R>( null, left2, null, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, R> E5<L1,L2,L3,L4,L5,R> left5_3( @NotNull final L3 left3 ) {
        return new E5<L1,L2,L3,L4,L5,R>( null, null, left3, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, R> E5<L1,L2,L3,L4,L5,R> left5_4( @NotNull final L4 left4 ) {
        return new E5<L1,L2,L3,L4,L5,R>( null, null, null, left4, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, R> E5<L1,L2,L3,L4,L5,R> left5_5( @NotNull final L5 left5 ) {
        return new E5<L1,L2,L3,L4,L5,R>( null, null, null, null, left5, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, R> E5<L1,L2,L3,L4,L5,R> right5( @NotNull final R right ) {
        return new E5<L1,L2,L3,L4,L5,R>( null, null, null, null, null, right );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, R> E6<L1,L2,L3,L4,L5,L6,R> left6_1( @NotNull final L1 left1 ) {
        return new E6<L1,L2,L3,L4,L5,L6,R>( left1, null,null,  null, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, R> E6<L1,L2,L3,L4,L5,L6,R> left6_2( @NotNull final L2 left2 ) {
        return new E6<L1,L2,L3,L4,L5,L6,R>( null, left2, null, null, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, R> E6<L1,L2,L3,L4,L5,L6,R> left6_3( @NotNull final L3 left3 ) {
        return new E6<L1,L2,L3,L4,L5,L6,R>( null, null, left3, null, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, R> E6<L1,L2,L3,L4,L5,L6,R> left6_4( @NotNull final L4 left4 ) {
        return new E6<L1,L2,L3,L4,L5,L6,R>( null, null, null, left4, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, R> E6<L1,L2,L3,L4,L5,L6,R> left6_5( @NotNull final L5 left5 ) {
        return new E6<L1,L2,L3,L4,L5,L6,R>( null, null, null, null, left5, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, R> E6<L1,L2,L3,L4,L5,L6,R> left6_6( @NotNull final L6 left6 ) {
        return new E6<L1,L2,L3,L4,L5,L6,R>( null, null, null, null, null, left6, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, R> E6<L1,L2,L3,L4,L5,L6,R> right6( @NotNull final R right ) {
        return new E6<L1,L2,L3,L4,L5,L6,R>( null, null, null, null, null, null, right );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, L7, R> E7<L1,L2,L3,L4,L5,L6,L7,R> left7_1( @NotNull final L1 left1 ) {
        return new E7<L1,L2,L3,L4,L5,L6,L7,R>( left1, null,null,  null, null, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, L7, R> E7<L1,L2,L3,L4,L5,L6,L7,R> left7_2( @NotNull final L2 left2 ) {
        return new E7<L1,L2,L3,L4,L5,L6,L7,R>( null, left2, null, null, null, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, L7, R> E7<L1,L2,L3,L4,L5,L6,L7,R> left7_3( @NotNull final L3 left3 ) {
        return new E7<L1,L2,L3,L4,L5,L6,L7,R>( null, null, left3, null, null, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, L7, R> E7<L1,L2,L3,L4,L5,L6,L7,R> left7_4( @NotNull final L4 left4 ) {
        return new E7<L1,L2,L3,L4,L5,L6,L7,R>( null, null, null, left4, null, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, L7, R> E7<L1,L2,L3,L4,L5,L6,L7,R> left7_5( @NotNull final L5 left5 ) {
        return new E7<L1,L2,L3,L4,L5,L6,L7,R>( null, null, null, null, left5, null, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, L7, R> E7<L1,L2,L3,L4,L5,L6,L7,R> left7_6( @NotNull final L6 left6 ) {
        return new E7<L1,L2,L3,L4,L5,L6,L7,R>( null, null, null, null, null, left6, null, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, L7, R> E7<L1,L2,L3,L4,L5,L6,L7,R> left7_7( @NotNull final L7 left7 ) {
        return new E7<L1,L2,L3,L4,L5,L6,L7,R>( null, null, null, null, null, null, left7, null );
    }

    @NotNull
    public static <L1, L2, L3, L4, L5, L6, L7, R> E7<L1,L2,L3,L4,L5,L6,L7,R> right7( @NotNull final R right ) {
        return new E7<L1,L2,L3,L4,L5,L6,L7,R>( null, null, null, null, null, null, null, right );
    }

    /**
     * Convenience class for declaration of a nested Either.
     *
     * @param <L1> The first left type
     * @param <L2> The second left type
     * @param <R> The right type
     */
    public static class E2<L1, L2, R>
            extends Either<L1,Either<L2,R>> {
        private E2( final L1 left1, final L2 left2, final R right ) {
            super( left1, new Either<L2,R>( left2, right ) );
        }
    }

    /**
     * Convenience class for declaration of a nested Either.
     *
     * @param <L1> The first left type
     * @param <L2> The second left type
     * @param <L3> The third left type
     * @param <R> The right type
     */
    public static class E3<L1, L2, L3, R>
            extends E2<L1,L2,Either<L3,R>> {
        private E3( final L1 left1, final L2 left2, final L3 left3, final R right ) {
            super( left1, left2, new Either<L3,R>( left3, right ) );
        }
    }

    public static class E4<L1, L2, L3, L4,R>
            extends E3<L1,L2,L3,Either<L4,R>> {
        private E4( final L1 left1, final L2 left2, final L3 left3, final L4 left4, final R right ) {
            super( left1, left2, left3, new Either<L4,R>( left4, right ) );
        }
    }

    public static class E5<L1, L2, L3, L4, L5,R>
            extends E4<L1,L2,L3,L4,Either<L5,R>> {
        private E5( final L1 left1, final L2 left2, final L3 left3, final L4 left4, final L5 left5, final R right ) {
            super( left1, left2, left3, left4, new Either<L5,R>( left5, right ) );
        }
    }

    public static class E6<L1, L2, L3, L4, L5, L6,R>
            extends E5<L1,L2,L3,L4,L5,Either<L6,R>> {
        private E6( final L1 left1, final L2 left2, final L3 left3, final L4 left4, final L5 left5, final L6 left6, final R right ) {
            super( left1, left2, left3, left4, left5, new Either<L6,R>( left6, right ) );
        }
    }

    public static class E7<L1, L2, L3, L4, L5, L6, L7,R>
            extends E6<L1,L2,L3,L4,L5,L6,Either<L7,R>> {
        private E7( final L1 left1, final L2 left2, final L3 left3, final L4 left4, final L5 left5, final L6 left6, final L7 left7, final R right ) {
            super( left1, left2, left3, left4, left5, left6, new Either<L7,R>( left7, right ) );
        }
    }

    //- PRIVATE

    private static <L,R,T> List<T> toList( final List<Either<L,R>> eithers,
                                           final BinaryVoid<List<T>,Either<L, R>> builder ) {
        final List<T> list = new ArrayList<T>();
        foreach( eithers, false, partial(builder,list));
        return list;
    }

}
