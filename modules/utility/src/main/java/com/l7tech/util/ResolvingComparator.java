package com.l7tech.util;

import java.util.Comparator;

/**
 * Comparator that uses a resolver to get the compared data for and object.
 *
 * <p>This can be useful for sorting Objects on a particular field, etc.</p>
 *
 * @author Steve Jones
 */
public class ResolvingComparator<RT extends Comparable> implements Comparator {

    //- PUBLIC

    public ResolvingComparator(Resolver<Object, RT> resolver, boolean reverse) {
        this.resolver = resolver;
        this.reverse = reverse;
    }

    /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.<p>
     */
    public int compare(Object o1, Object o2) {
        if (o1 == null || o2 == null) throw new IllegalArgumentException("Cannot compare null!");

        Comparable c1 = resolver.resolve(o1);
        Comparable c2 = resolver.resolve(o2);

        return c1.compareTo(c2) * (reverse ? -1 : 1);
    }

    //- PRIVATE

    private final Resolver<Object, RT> resolver;
    private final boolean reverse;
}
