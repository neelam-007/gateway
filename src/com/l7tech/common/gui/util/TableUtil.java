package com.l7tech.common.gui.util;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

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
}

