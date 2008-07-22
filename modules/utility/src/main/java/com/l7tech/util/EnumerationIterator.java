/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * An Iterator that iterates an Enumeration.
 */
public class EnumerationIterator<E> implements Iterator<E>, Enumeration<E> {
    private final Enumeration<E> enumeration;

    public EnumerationIterator(Enumeration<E> enumeration) {
        if (enumeration == null) throw new NullPointerException();
        this.enumeration = enumeration;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return enumeration.hasMoreElements();
    }

    public E next() {
        return enumeration.nextElement();
    }

    public boolean hasMoreElements() {
        return enumeration.hasMoreElements();
    }

    public E nextElement() {
        return enumeration.nextElement();
    }
}
