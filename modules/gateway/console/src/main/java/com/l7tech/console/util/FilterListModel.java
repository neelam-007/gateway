package com.l7tech.console.util;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;

/**
 * FilterListModel extends AbstractListModel and provides simple
 * filtering over a ListModel.
 * In 'patterns' language the class is a variation of decorator
 * (transparent closure) over the ListModel passed to the constructor.
 *
 * The instance of the class register itself as a listener for the
 * <code>ListModel</code> it decorates, and propagates the events
 * to its own listeners.
 *
 * The class is not thread safe.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class FilterListModel<E> extends AbstractListModel {
    /**
     * Creates a new instance of FilteredTreeModel encapsulating
     * the root of this model.
     *
     * @param model   the model
     */
    public FilterListModel(ListModel model) {
        this.model = model;
        loadFilteredIndices();

        // add listener to the data
        model.addListDataListener(
                new ListDataListener() {
                    /**
                     * Sent after the indices in the index0,index1
                     * interval have been inserted in the data model.
                     * The new interval includes both index0 and index1.
                     *
                     * @param e  a ListDataEvent encapuslating the event information
                     */
                    public void intervalAdded(ListDataEvent e) {
                        loadFilteredIndices();
                        fireContentsChanged(FilterListModel.this, 0, getSize());
                    }

                    /**
                     * Sent after the indices in the index0,index1 interval
                     * have been removed from the data model.  The interval
                     * includes both index0 and index1.
                     *
                     * @param e  a ListDataEvent encapuslating the event information
                     */
                    public void intervalRemoved(ListDataEvent e) {
                        loadFilteredIndices();
                        fireContentsChanged(FilterListModel.this, 0, getSize());
                    }

                    /**
                     * Sent when the contents of the list has changed in a way
                     * that's too complex to characterize with the previous
                     * methods.  Index0 and index1 bracket the change.
                     *
                     * @param e  a ListDataEvent encapuslating the event information
                     */
                    public void contentsChanged(ListDataEvent e) {
                        loadFilteredIndices();
                        fireContentsChanged(FilterListModel.this, 0, getSize());
                    }
                });
    }

    /**
     * get the underlying <CODE>ListModel</CODE> that is filtered
     * by FilteredListModel.
     *
     * @return the <code>ListModel</CODE> that is filtered
     */
    public ListModel getModel() {
        return model;
    }

    /**
     * call this method after the filter has been updated.  The method
     * notifies listeners about the change.
     */
    public void refresh() {
        loadFilteredIndices();
        fireContentsChanged(this, 0, getSize());
    }

    /**
     * call this method after the filter has been updated.  The method
     * notifies listeners about the change.
     */
    public void filterUpdated() {
        fireContentsChanged(this, 0, getSize());
    }

    /**
     * associate the filter with this <CODE>FilterListModel</CODE>
     *
     * @param filter new Filter
     */
    public void setFilter(Filter<E> filter) {
        this.filter = filter;
        loadFilteredIndices();
        fireContentsChanged(this, 0, getSize());
    }

    /**
     * @return the filter associated with this FilterListModel.
     *         <B>null</B> is returned if filter not set.
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * clear the filter associated with this <CODE>FilterListModel</CODE>
     */
    public void clearFilter() {
        filter = null;
        filteredIndices.clear();
        fireContentsChanged(this, 0, getSize());
    }

    /**
     * Returns the value at the specified index. The method performs
     * filtering over the <CODE>ListModel</CODE> if Filter has been
     * specified.
     *
     * @param index  the index of the elemnt
     * @return the value at the specified index.
     */
    public E getElementAt(int index) {
        if (filter == null) {
            return (E) model.getElementAt(index);
        } else {
            return (E) model.getElementAt(((Integer)filteredIndices.get(index)).intValue());
        }
    }

    /**
     * Gets the specified element index
     * @param element
     * @return
     */
    public int getElementIndex(E element) {
        if (filter == null) {
            return getElementIndex(model, element);
        } else {
            return getElementIndex(this, element);
        }
    }

    private int getElementIndex(ListModel<E> model, E element) {
        if (element != null) {
            for (int index = 0; index < model.getSize(); index++) {
                if (model.getElementAt(index).equals(element)) {
                    return index;
                }
            }
        }

        return -1;
    }

    /**
     * Returns the number of elements in the list.
     * The method uses the filter if specified (not null)
     *
     * @return  the number of elements in the list.
     */
    public int getSize() {
        if (filter == null) {
            return model.getSize();
        } else {
            return filteredIndices.size();
        }
    }

    /** Loads valid (filter accepted) indices from the underlying model */
    private void loadFilteredIndices() {
        if (null != filter) {
            // NOTE: due to the nature of populating the filteredIndices array,
            // the array can be considered sorted
            int size = model.getSize();
            filteredIndices = new ArrayList<Integer>(size);
            for (int i = 0, index = 0; index < size; index++) {
                if (filter.accept((E) model.getElementAt(index))) {
                    filteredIndices.add(new Integer(index));
                }
            }
        }
    }

    /**
     * Returns the filtered index of the specified index.
     *
     * @param index  the original index of the element
     * @return the index after the filter is applied
     */
    public int getFilteredIndex(int index){
         if (filter == null) {
            return index;
        } else {
            return filteredIndices.indexOf(index);
        }
    }

    /**
     * Returns the original index of the specified index.
     *
     * @param index  the filtered item index
     * @return the original index of the specified filtered item index
     */
    public int getOriginalIndex(int index){
        if (filter == null) {
            return index;
        } else {
            return filteredIndices.get(index);
        }
    }

    private Filter<E> filter;
    private ListModel model;
    private List<Integer> filteredIndices; // sorted list of filtered indices
}

