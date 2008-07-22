package com.l7tech.console.util.jfree;

import org.jfree.util.AbstractObjectList;

/**
 * A list of <code>Double</code> objects.
 * Similar to {@link org.jfree.util.BooleanList}.
 *
 * @author rmak
 */
public class DoubleList extends AbstractObjectList {
    private static final long serialVersionUID = 7391038530519195216L;

    /** Creates a new list. */
    public DoubleList() {
    }

    /**
     * Returns a {@link Double} from the list.
     *
     * @param index the index (zero-based)
     * @return a {@link Double} from the list
     */
    public Double getDouble(final int index) {
        return (Double) get(index);
    }

    /**
     * Sets the value for an item in the list.  The list is expanded if
     * necessary.
     *
     * @param index the index (zero-based)
     * @param b     the double
     */
    public void setDouble(final int index, final Double b) {
        set(index, b);
    }

    public boolean equals(final Object o) {
        if (o instanceof DoubleList) {
            return super.equals(o);
        }
        return false;
    }

    public int hashCode() {
        return super.hashCode();
    }
}
