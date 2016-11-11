package com.l7tech.gui;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A SimpleTableModel which allows each row to be selected.
 *
 * @param <T> the type of object to store in the model.
 */
public class SelectableTableModel<T> extends SimpleTableModel<T> {
    private final List<T> selected;

    private final boolean allowMultipleSelect;
    private final int selectionColumnIndex;

    public SelectableTableModel(final boolean allowMultiSelect, final int selectionColumnIndex) {
        this.allowMultipleSelect = allowMultiSelect;
        this.selectionColumnIndex = selectionColumnIndex;

        selected = new ArrayList<>();
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return columnIndex == selectionColumnIndex ? Boolean.class : super.getColumnClass(columnIndex);
    }

    /**
     * Select the object of the given row index.
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
     * Deselect the object of the given row index.
     *
     * @param index the row index to deselect.
     */
    public void deselect(final int index) {
        selectDeselect(index, false);
    }

    /**
     * Toggle the selection of the given row index.
     *
     * @param index the row index to toggle.
     */
    public void toggle(final int index) {
        final T selectable = getRowObject(index);
        if (selectable != null) {
            if (selected.contains(selectable)) {
                selected.remove(selectable);
            } else {
                selected.add(selectable);
            }
            fireTableRowsUpdated(index, index);
        }
    }

    /**
     * @param selectable the object which may be selected.
     * @return true if the given object is selected, false otherwise.
     */
    public boolean isSelected(@NotNull final T selectable) {
        return selected.contains(selectable);
    }

    /**
     * @return a collection of selected objects.
     */
    public List<T> getSelected() {
        return selected;
    }

    public boolean isAllowMultipleSelect() {
        return allowMultipleSelect;
    }

    private void selectDeselectAll(final boolean select) {
        //Clearing previously selected Roles in the List, before using SELECT ALL Operation(in PM UI)
        selected.clear();
        if (select) {
            selected.addAll(getRows());
        } else {
            selected.clear();
        }
        fireTableDataChanged();
    }

    private void selectDeselect(final int index, final boolean select) {
        final T selectable = getRowObject(index);
        if (selectable != null) {
            if (select) {
                if (allowMultipleSelect) {
                    selected.clear();
                }
                selected.add(selectable);
            } else {
                selected.remove(selectable);
            }

            fireTableRowsUpdated(index, index);
        }
    }

    @Override
    public void setRows(List<T> rows) {
        deselectAll();
        super.setRows(rows);
    }
}
