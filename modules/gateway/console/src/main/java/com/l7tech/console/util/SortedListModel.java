package com.l7tech.console.util;

import javax.swing.*;
import java.util.*;

/**
 * The <CODE>SortedListModel</CODE> is te implementation
 * if the <CODE>ListModel</CODE>, that sorts the entries
 * according to the natural ordering of its elements
 * (see Comparable), or in the order provided by the instance
 * of <CODE>Comparator</CODE> to the constructor.
 *
 * Besides implementing the methods of <CODE>ListModel</CODE>,
 * the <CODE>SortedListModel</CODE> class implements
 * <code>MutableListMode</code> that provides several
 * methods to access and alter the data model.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SortedListModel<E> extends AbstractListModel {

    /** SortedSet that backs the list */
    SortedSet<E> model;

    /** default constructor */
    public SortedListModel() {
        model = new TreeSet<E>();
    }

    /**
     * the constructor accepting the <CODE>Comparator</CODE>
     * instance. The ListModel ordering is imposed by the c.
     *
     * @param c      Comparator, that governs the ordering
     *               of ListModel
     */
    public SortedListModel(Comparator<? super E> c) {
        model = new TreeSet<E>(c);
    }

    /** @return the length of the list. */
    public int getSize() {
        return model.size();
    }

    /**
     * @param index  integer the index
     * @return the value at the specified index.
     */
    public E getElementAt(int index) {
        return (E) model.toArray()[index];
    }

    /**
     * add an object to this ListModel
     *
     * @param element Object the element to add
     */
    public void add(E element) {
        if (model.add(element)) {
            fireContentsChanged(this, 0, getSize());
        }
    }

    /**
     * add the Collection to the ListModel
     *
     * @param c Collection of elements
     */
    public void addAll(Collection<E> c) {
        model.addAll(c);
        fireContentsChanged(this, 0, getSize());
    }

    /**
     * add the array of object elements to the ListModel
     *
     * @param elements Object array of elements
     */
    public void addAll(E[] elements) {
        Collection<E> c = Arrays.asList(elements);
        model.addAll(c);
        fireContentsChanged(this, 0, getSize());
    }

    /**
     * clear the ListModel
     */
    public void clear() {
        model.clear();
        fireContentsChanged(this, 0, getSize());
    }

    /**
     * Searches the JList for a specified Object.
     *
     * @param element Object to search the ListModel for
     * @return true if it contains, false otherwise
     */
    public boolean contains(E element) {
        return model.contains(element);
    }

    /** @return the first element in the ListModel */
    public E firstElement() {
        return model.first();
    }

    /**
     *
     * @return the <CODE>Iterator</CODE> over the elements
     *         in this ListModel
     */
    public Iterator iterator() {
        return model.iterator();
    }

    /** @return the last element in the ListModel */
    public E lastElement() {
        return model.last();
    }

    /**
     *
     * @return an array containing all of the elements in
     * this  list in the correct order.
     */
    public E[] toArray() {
        return (E[]) model.toArray();
    }

    /** @return a List containing all of the elements of the list in the correct order. */
    public List<E> toList() {
        return new ArrayList<E>(model);
    }

    /**
     * remove the element from the JList
     *
     * @param element the elment to remove
     * @return true of the object has been remvoed, false otherwise
     */
    public boolean removeElement(E element) {
        boolean removed = model.remove(element);
        if (removed) {
            fireContentsChanged(this, 0, getSize());
        }
        return removed;
    }
}
