package com.l7tech.gui.util;

import com.l7tech.gui.SimpleColumn;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Arrays;

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
     * Attempt to make the given table cell visible.
     * From http://www.codeguru.com/java/articles/161.shtml
     */
    public static void setCellVisible(JTable table, int row, int col) {
        Container p = table.getParent();
        if (p instanceof JViewport) {
            Container gp = p.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane)gp;
                // Make sure the table is the main viewport
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != table) {
                    return;
                }

                Rectangle cellrect = table.getCellRect(row, col, true);
                Rectangle viewrect = viewport.getViewRect();
                if (viewrect.contains(cellrect))
                    return;
                Rectangle union = viewrect.union(cellrect);
                int x = (int)(union.getX() + union.getWidth() - viewrect.getWidth());
                int y = (int)(union.getY() + union.getHeight() - viewrect.getHeight());
                viewport.setViewPosition(new Point(x, y));
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

        public Col(String name, int minWidth, int preferredWidth, int maxWidth, Functions.Unary<Object,RT> valueGetter) {
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
    public static <RT> Col<RT> column(String name, int minWidth, int preferredWidth, int maxWidth, Functions.Unary<Object,RT> valueGetter) {
        return new Col<RT>(name, minWidth, preferredWidth, maxWidth, valueGetter);
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
    public static <RT> SimpleTableModel<RT> configureTable(JTable table, Col<RT>... columns) {
        SimpleTableModel<RT> model = new SimpleTableModel<RT>();
        model.setColumns(Arrays.<SimpleColumn<RT>>asList(columns));

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

        return model;
    }
}

