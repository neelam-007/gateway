package com.l7tech.util;

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

    public ResolvingComparator(Resolver<KT, RT> resolver, boolean reverse) {
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
