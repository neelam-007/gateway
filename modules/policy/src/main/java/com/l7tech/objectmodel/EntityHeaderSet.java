/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;
import java.io.Serializable;

/**
 * A Set&lt;EntityHeader&gt; that includes an optional {@link #exceededMax} property, used to indicate that this set
 * is the result of a search in which the maximum number of results was exceeded.
 *
 * @param <HT> the type of element
 *
 * @author alex
 */
@XmlJavaTypeAdapter(com.l7tech.objectmodel.EntityHeaderSetType.EntityHeaderSetTypeAdapter.class)
public class EntityHeaderSet<HT extends EntityHeader> implements Set<HT>, Serializable {
    private volatile Long exceededMax;
    private final Set<HT> delegate;

    private static final long serialVersionUID = 1L;

    public static <HT_ extends EntityHeader> EntityHeaderSet<HT_> empty() {
        return new EntityHeaderSet<HT_>(Collections.<HT_>emptySet());
    }

    public EntityHeaderSet(HT... items) {
        final Set<HT> delegate = new HashSet<HT>();
        delegate.addAll(Arrays.asList(items));
        this.delegate = delegate;
    }

    public EntityHeaderSet() {
        this.delegate = new HashSet<HT>();
    }

    public EntityHeaderSet(Set<HT> delegate) {
        this.delegate = delegate;
    }

    /**
     * @return true if this Set resulted from a search in which the number of results was capped
     */
    public boolean isMaxExceeded() {
        return exceededMax != null;
    }

    /**
     * @return the maximum number of results that the search was permitted to return.  Not necessarily equal to
     * {@link #size}.
     */
    public Long getExceededMax() {
        return exceededMax;
    }

    /**
     * Indicates that the search was capped at a maximum number of results, and provides the number that was used.  Not
     * guaranteed to be equal to {@link #size}.
     *
     * @param max the maximum number of results permitted from the search
     */
    public void setMaxExceeded(long max) {
        this.exceededMax = max;
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    public Iterator<HT> iterator() {
        return delegate.iterator();
    }

    public Object[] toArray() {
        return delegate.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    public boolean add(HT ht) {
        return delegate.add(ht);
    }

    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    public boolean addAll(Collection<? extends HT> c) {
        return delegate.addAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityHeaderSet that = (EntityHeaderSet)o;

        if (delegate != null ? !delegate.equals(that.delegate) : that.delegate != null) return false;
        if (exceededMax != null ? !exceededMax.equals(that.exceededMax) : that.exceededMax != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = exceededMax != null ? exceededMax.hashCode() : 0;
        result = 31 * result + (delegate != null ? delegate.hashCode() : 0);
        return result;
    }
}
