package com.l7tech.gui.util;

import com.l7tech.gui.CheckBoxSelectableTableModel;
import com.l7tech.gui.SelectableObject;
import com.l7tech.gui.SimpleColumn;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.util.Functions;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;

/**
 * Handy utility routines for dealing with tables.
 */
public final class TableUtil {

    /** this class cannot be instantiated */
    private TableUtil() {}

    /**
     * Finds the width in pixels of the longest table row in column n.
     */
    public static int getColumnWidth(JTable table, int col) {
        int maxWidth = 0;
        TableColumn tc = table.getColumnModel().getColumn(col);
        Object header  = tc.getHeaderValue();
        if (header != null) {
            TableCellRenderer headerRenderer = tc.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            Component hc = headerRenderer.getTableCellRendererComponent(table, header, false, false, 0, col);
            int ins = 0;
            if (hc instanceof JComponent) {
                JComponent jc = (JComponent)hc;
                Insets insets = jc.getInsets();
                ins = insets.left + insets.right;
            }
            maxWidth = hc.getPreferredSize().width+ins;
        }

        for (int row = 0; row < table.getRowCount(); ++row) {
            int width = getCellPreferredSize(table, row, col).width;
            if (width > maxWidth)
                maxWidth = width;
        }
        return maxWidth;
    }

    /**
     * Figures out the preferred size in pixels of the given cell.
     */
    public static Dimension getCellPreferredSize(JTable table, int row, int col) {
        Object val = table.getModel().getValueAt(row, col);
        return getCellPreferredSize(table, row, col, val);
    }

    /**
     * Figures out the preferred width in pixels of the given object if rendered
     * in the given column.
     */
    public static Dimension getCellPreferredSize(JTable table,
                                                 int row,
                                                 int col,
                                                 Object val) {
        TableCellRenderer rend = table.getCellRenderer(row, col);
        Component c = rend.getTableCellRendererComponent(table, val, false, false, row, col);
        Dimension d = c.getPreferredSize();
        return new Dimension(d.width + 2, d.height + 3); // XXX hardcoded inset adjustment
    }

    /**
     * Figures out how high the given row wants to be.
     */
    public static int getRowHeight(JTable table, int row) {
        int maxHeight = 0;
        for (int col = 0; col < table.getColumnCount(); ++col) {
            int height = getCellPreferredSize(table, row, col).height;
            if (height > maxHeight)
                maxHeight = height;
        }
        return maxHeight;
    }


    /**
     * Sets the min and max width of the given column to the width of its largest cell.
     */
    public static void adjustColumnWidth(JTable table, int col, int minimum) {
        int width = getColumnWidth(table, col);
        if (width < minimum)
            width = minimum;
        setColumnWidth(table, col, width);
    }

    /**
     * Sets the preferred and max width of the given column to the width of its
     * largest cell.
     */
    public static void adjustColumnWidth(JTable table, int col) {
        adjustColumnWidth(table, col, 0);
    }

    /**
     * Sets the preferred and max width of the given column to the preferred width
     * that the given value would render with.
     */
    public static void adjustColumnWidth(JTable table, int col, Object val) {
        setColumnWidth(table, col, getCellPreferredSize(table, 0, col, val).width);
    }

    /**
     * Sets both the preferred and max width of a column to the same value.
     */
    public static void setColumnWidth(JTable table, int col, int width) {
        table.getColumnModel().getColumn(col).setPreferredWidth(width);
        table.getColumnModel().getColumn(col).setMaxWidth(width);
    }

    /**
     * Create an action that will cause the currently-selected row(s) in the specified table, if any, to swap positions
     * with the previous row (if possible).

     * @param table table to alter.  Must use a SimpleTableModel.
     * @param tableModel the SimpleTableModel the table is attached to.
     * @return an ActionListener that will, when invoked, move the selected table row up one spot.
     */
    public static ActionListener createMoveUpAction(@NotNull final JTable table, @NotNull final SimpleTableModel<?> tableModel) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                swapSelectedRowsWithOffset(table, tableModel, -1);
            }
        };
    }

    /**
     * Create an action that will cause the currently-selected row(s) in the specified table, if any, to swap positions
     * with the next row (if possible).

     * @param table table to alter.  Must use a SimpleTableModel.
     * @param tableModel the SimpleTableModel the table is attached to.
     * @return an ActionListener that will, when invoked, move the selected table row down one spot.
     */
    public static ActionListener createMoveDownAction(@NotNull final JTable table, @NotNull final SimpleTableModel<?> tableModel) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                swapSelectedRowsWithOffset(table, tableModel, 1);
            }
        };
    }

    private static void swapSelectedRowsWithOffset(JTable table, SimpleTableModel<?> tableModel, int offset) {
        final int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            if (offset > 0) {
                // moving rows downwards
                // process in reverse order so they do not collide
                ArrayUtils.reverse(selectedRows);
            }
            table.clearSelection();
            for (int i = 0; i < selectedRows.length; i++) {
                int row = selectedRows[i];
                final int r2 = row + offset;
                if (tableModel.swapRows(row, r2))
                    table.getSelectionModel().addSelectionInterval(r2, r2);
            }
        }
    }

    /**
     * Augments {@link com.l7tech.gui.SimpleColumn} with sizing hints to produce a column
     * descriptor suitable for use with {@link TableUtil#configureTable}.
     */
    public static class Col<RT> extends SimpleColumn<RT> {
        final int minWidth;
        final int preferredWidth;
        final int maxWidth;

        public Col(String name, int minWidth, int preferredWidth, int maxWidth, Functions.Unary<?,RT> valueGetter) {
            super(name, valueGetter);
            this.minWidth = minWidth;
            this.preferredWidth = preferredWidth;
            this.maxWidth = maxWidth;
        }
    }

    /**
     * Create a column descriptor to pass to {@link #configureTable}.
     *
     * @param name        the name of the column, to use for the default table header.  Required.
     * @param minWidth    the minimum width of the column, per {@link TableColumn#setMinWidth(int)}
     * @param preferredWidth  the preferred width of the column, per {@link TableColumn#setPreferredWidth(int)}
     * @param maxWidth        the maximum width of the column, per {@link TableColumn#setMaxWidth(int)}
     * @param valueGetter a transform to use to map instances of {@link RT} to cell values.
     *                    See {@link Functions#getterTransform(java.lang.reflect.Method)}
     *                    and {@link Functions#propertyTransform(Class, String)} for easy ways to create a transform.
     * @return the newly-created column descriptor.
     */
    public static <RT> Col<RT> column(String name, int minWidth, int preferredWidth, int maxWidth, Functions.Unary<?,RT> valueGetter) {
        return new Col<RT>(name, minWidth, preferredWidth, maxWidth, valueGetter);
    }

    /**
     * Create a column descriptor to pass to {@link #configureTable}.
     *
     * @param name        the name of the column, to use for the default table header.  Required.
     * @param minWidth    the minimum width of the column, per {@link TableColumn#setMinWidth(int)}
     * @param preferredWidth  the preferred width of the column, per {@link TableColumn#setPreferredWidth(int)}
     * @param maxWidth        the maximum width of the column, per {@link TableColumn#setMaxWidth(int)}
     * @param valueGetter a transform to use to map instances of {@link RT} to cell values.
     *                    See {@link Functions#getterTransform(java.lang.reflect.Method)}
     *                    and {@link Functions#propertyTransform(Class, String)} for easy ways to create a transform.
     * @param columnClass The class for the column
     * @return the newly-created column descriptor.
     */
    public static <T,RT> Col<RT> column(String name, int minWidth, int preferredWidth, int maxWidth, Functions.Unary<T,RT> valueGetter, Class<T> columnClass) {
        Col<RT> col = column( name, minWidth, preferredWidth, maxWidth, valueGetter );
        col.setColumnClass( columnClass );
        return col;
    }

    /**
     * Configure a table using the {@link SimpleTableModel} and the specified column descriptions, using a generic
     * row backing type.
     * <p/>
     * This creates and assigns a new {@link SimpleTableModel}, and sets the column widths based on
     * the column descriptors passed in.
     *
     * @param table    the JTable to reconfigure.  Any existing table model will be discarded.
     * @param columns  one or more column descriptors.  Use {@link #column} to create one.
     * @return the SimpleTableModel that was created and assigned.
     */
    @SafeVarargs
    public static <RT> SimpleTableModel<RT> configureTable(JTable table, Col<RT>... columns) {
        SimpleTableModel<RT> model = new SimpleTableModel<RT>();
        model.setColumns(Arrays.<SimpleColumn<RT>>asList(columns));
        configureTable(table, model, columns);
        return model;
    }

    /**
     * Configure a table using the {@link CheckBoxSelectableTableModel} and the specified column descriptions, using a generic row backing type.
     *
     * This creates and assigns a new {@link SimpleTableModel}, and sets the column widths based on the column descriptors passed in.
     *
     * @param table             the JTable to reconfigure.  Any existing table model will be discarded.
     * @param selectColIndex    the index of the column which contains the check box.
     * @param columns           one or more column descriptors.  Use {@link #column} to create one.
     * @param <RT>              the type of object which is selectable.
     * @return                  the CheckBoxSelectableTableModel that was created and assigned.
     */
    public static <RT> CheckBoxSelectableTableModel<RT> configureSelectableTable(@NotNull final JTable table, final int selectColIndex, @NotNull final Col<RT>... columns) {
        final CheckBoxSelectableTableModel<RT> model = new CheckBoxSelectableTableModel(selectColIndex);
        final List<SimpleColumn<SelectableObject<RT>>> selectableColumns = new ArrayList<>();
        for (final Col<RT> column : columns) {
            final Col<SelectableObject<RT>> col = new Col<>(column.getName(), column.minWidth, column.preferredWidth, column.maxWidth, new Functions.Unary<Object, SelectableObject<RT>>() {
                @Override
                public Object call(final SelectableObject<RT> selectable) {
                    return column.getValueGetter().call(selectable.getSelectable());
                }
            });
            selectableColumns.add(col);
        }
        model.setColumns(selectableColumns);
        configureTable(table, (SimpleTableModel)model, columns);

        // mouse listener which toggles the selection depending on which row was clicked
        table.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                final int rowIndex = table.rowAtPoint(e.getPoint());
                if (rowIndex >= 0) {
                    final int modelIndex = table.convertRowIndexToModel(rowIndex);
                    if (modelIndex >= 0) {
                        model.toggle(modelIndex);
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        return model;
    }

    private static <RT> void configureTable(@NotNull final JTable table, @NotNull final SimpleTableModel<RT> model, @NotNull final Col<RT>[] columns) {
        table.setModel(model);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumnModel cols = table.getColumnModel();
        int numCols = model.getColumnCount();
        for (int i = 0; i < numCols; ++i) {
            final TableColumn col = cols.getColumn(i);
            col.setMinWidth(columns[i].minWidth);
            col.setPreferredWidth(columns[i].preferredWidth);
            col.setMaxWidth(columns[i].maxWidth);
        }
    }
}

