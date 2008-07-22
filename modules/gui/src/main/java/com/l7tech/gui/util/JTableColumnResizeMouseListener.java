package com.l7tech.gui.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.JTableHeader;
import javax.swing.*;

/**
 * MouseListener to resize JTable column widths when you double-click.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class JTableColumnResizeMouseListener extends MouseAdapter {

    //- PUBLIC

    /**
     * Create a resize listener for the given table with the given default widths.
     *
     * <p>Note that the widths are for the TableModel column indexes.<p>
     *
     * @param table         The JTable to resize
     * @param defaultWidths The default width by column index.
     */
    public JTableColumnResizeMouseListener(JTable table, int[] defaultWidths) {
        this.table = table;
        this.defaultWidths = defaultWidths;
    }

    /**
     * Handles the mouse click. A double-click triggers column resize.
     *
     * @param e The mouse event.
     */
    public void mouseClicked(MouseEvent e) {
        JTableHeader tableHeader = table.getTableHeader();
        int cursorType = tableHeader.getCursor().getType();
        // only resize if the resize cursor is being shown.
        if (e.getClickCount() == 2 &&
            (cursorType== Cursor.E_RESIZE_CURSOR || cursorType==Cursor.W_RESIZE_CURSOR)) {

            TableModel tm = table.getModel();
            TableColumnModel columnModel = table.getColumnModel();
            int tableViewColumn = table.columnAtPoint(e.getPoint());
            int tableModelColum = table.convertColumnIndexToModel(tableViewColumn);
            TableColumn tc = columnModel.getColumn(tableViewColumn);

            int defWidth = defaultWidths[tableModelColum];
            int prefWidth = table.getRowCount() > 0 ? tc.getMinWidth() : defaultWidths[tableModelColum];
            if (prefWidth < 50) prefWidth = 50;

            // find the largest preferred width for the column
            for(int r=0; r<table.getRowCount(); r++) {
                TableCellRenderer tcr = table.getCellRenderer(r, tableViewColumn);
                int width = (int) tcr.getTableCellRendererComponent(table, tm.getValueAt(r,tableModelColum), false, false, r, tableViewColumn).getPreferredSize().getWidth();
                if(width > prefWidth) prefWidth = width;
            }

            int newWidth = 0;
            int resizeAmount = 0;
            if (tc.getPreferredWidth() != prefWidth) {
                newWidth = prefWidth;
                resizeAmount = tc.getPreferredWidth() - prefWidth;
            }
            else if (tc.getPreferredWidth() != defWidth){
                newWidth = defWidth;
                resizeAmount = tc.getPreferredWidth() - defWidth;
            }

            if(resizeAmount!=0) {
                // resize the column
                tc.setPreferredWidth(newWidth);

                int ignoreCols = 1; // ignore column being resized and any Zero length columns
                for(int t=0; t<columnModel.getColumnCount(); t++) {
                    if(t!=tableViewColumn && columnModel.getColumn(t).getPreferredWidth()==0) ignoreCols++;
                }
                if(table.getAutoResizeMode()!=JTable.AUTO_RESIZE_OFF && (columnModel.getColumnCount()-ignoreCols)>0) {
                    // adjust the sizes of all the other columns to make/fill room
                    int adjustment = resizeAmount / (columnModel.getColumnCount()-ignoreCols);

                    for(int t=0; t<columnModel.getColumnCount(); t++) {
                        if(t!=tableViewColumn) {
                            TableColumn column = columnModel.getColumn(t);
                            int width = column.getPreferredWidth()+adjustment;
                            if(column.getPreferredWidth()>0) {
                                column.setPreferredWidth(width);
                                resizeAmount -= adjustment;
                            }
                        }
                    }

                    // deal with the remainder
                    if (resizeAmount != 0) {
                        int col = columnModel.getColumnCount()-1;
                        if(tableModelColum == col) col--;
                        TableColumn column = columnModel.getColumn(col);
                        int width = column.getPreferredWidth()+resizeAmount;
                        column.setPreferredWidth(width);
                    }
                }

                // refresh
                tableHeader.resizeAndRepaint();
            }
        }
    }

    //- PRIVATE

    private final int[] defaultWidths;
    private final JTable table;

}
