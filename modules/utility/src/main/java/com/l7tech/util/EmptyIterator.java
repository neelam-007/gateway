package com.l7tech.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that produces no elements. 
 */
public class EmptyIterator<E> implements Iterator<E> {
    public boolean hasNext() {
        return false;
    }

    public E next() {
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new IllegalStateException();
    }
}
