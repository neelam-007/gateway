package com.l7tech.gui;

import org.jetbrains.annotations.NotNull;

/**
 * Wrapper for an Object to allow it to be selectable.
 *
 * @param <T> the object to make selectable.
 */
public class SelectableObject<T> {
    boolean selected;
    final T selectable;

    /**
     * @param selectable the object to make selectable.
     */
    public SelectableObject(@NotNull final T selectable) {
        this.selectable = selectable;
    }

    /**
     * @param selected true to select the object, false otherwise.
     */
    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    /**
     * @return true if the object is selected, false otherwise.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @return the object backed by this SelectableObject.
     */
    @NotNull
    public T getSelectable() {
        return selectable;
    }
}
