package com.l7tech.gui.widgets;

/**
 * Represents an interface with common JCheckBoxListModel aware methods.
 */
public interface JCheckBoxListModelAware {

    /**
     * Swaps the entries at the specified indices
     * @param index1
     * @param index2
     */
    void swapEntries(int index1, int index2);

    /**
     * Set the "armed" state for the checkbox at the specified index.
     * @param index
     */
    void arm(int index);

    /**
     * Clear the "armed" state from any checkbox that was armed by a call to {@link #arm}.
     */
    void disarm();

    /**
     * Toggle the checkbox at the specified index.
     * @param index
     */
    void toggle(int index);

}
