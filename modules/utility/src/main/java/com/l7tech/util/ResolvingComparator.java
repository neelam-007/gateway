package com.l7tech.util;

import com.l7tech.util.Functions.Unary;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Comparator that uses a resolver to get the compared data for and object.
 *
 * <p>This can be useful for sorting Objects on a particular field, etc.</p>
 *
 * @author Steve Jones
 */
public class ResolvingComparator<KT,RT extends Comparable<RT>> implements Comparator<KT> {

    //- PUBLIC

    /**
     * Construct a resolving comparator from a Unary function.
     *
     * @param unary The function to call
     * @param reverse True to reverse the comparison (and hence the order)
     * @param <KT> The key type
     * @param <RT> The (comparable) value type
     * @return The new resolving comparator
     */
    @NotNull
    public static <KT,RT extends Comparable<RT>> ResolvingComparator<KT,RT> fromUnary(
            @NotNull final Unary<RT,KT> unary,
            final boolean reverse ) {
        return new ResolvingComparator<KT, RT>( new Resolver<KT, RT>(){
            @Override
            public RT resolve( final KT key ) {
                return unary.call( key );
            }
        }, reverse );
    }

    /**
     * Create a resolving comparator that uses the given resolver.
     *
     * @param resolver The resolver to use
     * @param reverse True to reverse the comparison (and hence the order)
     */
    public ResolvingComparator( @NotNull final Resolver<KT, RT> resolver,
                                final boolean reverse) {
        this.resolver = resolver;
        this.reverse = reverse;
    }

    /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.<p>
     */
    @Override
    public int compare( final KT o1, final KT o2) {
        if (o1 == null || o2 == null) throw new IllegalArgumentException("Cannot compare null!");

        RT c1 = resolver.resolve(o1);
        RT c2 = resolver.resolve(o2);

        return c1.compareTo(c2) * (reverse ? -1 : 1);
    }

    //- PRIVATE

    private final Resolver<KT, RT> resolver;
    private final boolean reverse;
}
