/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * An Enumeration that enumerates an Iterator
 */
public class IteratorEnumeration implements Enumeration, Iterator {
    private final Iterator iterator;

    public IteratorEnumeration(Iterator iterator) {
        if (iterator == null) throw new NullPointerException();
        this.iterator = iterator;
    }

    public boolean hasMoreElements() {
        return iterator.hasNext();
    }

    public Object nextElement() {
        return iterator.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public Object next() {
        return iterator.next();
    }
}
