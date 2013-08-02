package com.l7tech.gui;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A SimpleTableModel which allows each row to be selected via a checkbox.
 * <p/>
 * Each row object of type T is wrapped in a SelectableObject.
 *
 * @param <T> the type of object to store in the model.
 */
public class CheckBoxSelectableTableModel<T> extends SimpleTableModel<SelectableObject<T>> {
    private final int checkColIndex;

    public CheckBoxSelectableTableModel(final int checkColIndex) {
        this.checkColIndex = checkColIndex;
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return columnIndex == checkColIndex ? Boolean.class : super.getColumnClass(columnIndex);
    }

    /**
     * Set the rows of the selectable model. Each T will be wrapped in a SelectableObject.
     *
     * @param rows the rows to add to the selectable model.
     */
    public void setSelectableObjects(@NotNull final List<T> rows) {
        final List<SelectableObject<T>> selectableObjects = new ArrayList<>(rows.size());
        for (final T row : rows) {
            selectableObjects.add(new SelectableObject<T>(row));
        }
        super.setRows(selectableObjects);
    }

    /**
     * Get the object of the given row index.
     *
     * @param index the index of the row.
     * @return the backing object stored in the selectable row or null if the given index is invalid.
     */
    public T getSelectableObject(final int index) {
        final SelectableObject<T> selectable = getRowObject(index);
        return selectable == null ? null : selectable.getSelectable();
    }

    /**
     * Select the selectable object of the given row index.
     *
     * @param index the row index to select.
     */
    public void select(final int index) {
        selectDeselect(index, true);
    }

    /**
     * Select all rows.
     */
    public void selectAll() {
        selectDeselectAll(true);
    }

    /**
     * Deselect all rows.
     */
    public void deselectAll() {
        selectDeselectAll(false);
    }

    /**
     * Deselect the selectable object of the given row index.
     *
     * @param index the row index to deselect.
     */
    public void deselect(final int index) {
        selectDeselect(index, false);
    }

    public void toggle(final int index) {
        final SelectableObject<T> selectable = getRowObject(index);
        if (selectable != null) {
            selectable.setSelected(!selectable.isSelected());
            fireTableRowsUpdated(index, index);
        }
    }

    public boolean isSelected(@NotNull final T selectable) {
        boolean selected = false;
        for (final SelectableObject<T> row : getRows()) {
            if (row.getSelectable().equals(selectable)) {
                selected = row.isSelected();
                break;
            }
        }
        return selected;
    }

    public List<T> getSelected() {
        final List<T> selected = new ArrayList<>();
        for (final SelectableObject<T> row : getRows()) {
            if (row.isSelected()) {
                selected.add(row.getSelectable());
            }
        }
        return selected;
    }

    private void selectDeselectAll(final boolean select) {
        for (final SelectableObject<T> row : getRows()) {
            row.setSelected(select);
        }
        fireTableDataChanged();
    }

    private void selectDeselect(final int index, final boolean select) {
        final SelectableObject<T> selectable = getRowObject(index);
        if (selectable != null) {
            selectable.setSelected(select);
        }
    }
}
