/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * An Iterator that iterates an Enumeration.
 */
public class EnumerationIterator implements Iterator, Enumeration {
    private final Enumeration enumeration;

    public EnumerationIterator(Enumeration enumeration) {
        if (enumeration == null) throw new NullPointerException();
        this.enumeration = enumeration;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return enumeration.hasMoreElements();
    }

    public Object next() {
        return enumeration.nextElement();
    }

    public boolean hasMoreElements() {
        return enumeration.hasMoreElements();
    }

    public Object nextElement() {
        return enumeration.nextElement();
    }
}
