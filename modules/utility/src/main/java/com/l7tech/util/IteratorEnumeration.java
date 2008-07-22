/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * An Enumeration that enumerates an Iterator
 */
public class IteratorEnumeration<E> implements Enumeration<E>, Iterator<E> {
    private final Iterator<E> iterator;

    public IteratorEnumeration(Iterator<E> iterator) {
        if (iterator == null) throw new NullPointerException();
        this.iterator = iterator;
    }

    public boolean hasMoreElements() {
        return iterator.hasNext();
    }

    public E nextElement() {
        return iterator.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public E next() {
        return iterator.next();
    }
}
