package com.l7tech.gui.util;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableModel;
import javax.swing.table.TableStringConverter;
import java.util.ArrayList;

/**
 * GUI Utilities that must be guarded due to use of JDK 1.6 functionality.
 */
class UtilitiesJDK16 extends Utilities.Utils {

    public int convertRowIndexToModel(JTable table, int row) {
        return table.convertRowIndexToModel( row );
    }

    @SuppressWarnings({"unchecked"})
    public void setRowSorter( final JTable table,
                              final TableModel model,
                              final int[] cols,
                              final boolean[] order,
                              final Utilities.TableStringConverter converter) {
        TableRowSorter sorter = new TableRowSorter(model);

        if ( cols != null ) {
            java.util.List <RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
            for ( int i=0; i< cols.length; i++ ) {
                sortKeys.add(new RowSorter.SortKey(cols[i], order[i] ? SortOrder.ASCENDING : SortOrder.DESCENDING ));
            }
            sorter.setSortKeys(sortKeys);
        }

        if ( converter != null ) {
            sorter.setStringConverter( new TableStringConverter() {
                public String toString(TableModel model, int row, int column) {
                    return converter.toString( model, row, column );
                }
            });
        }

        table.setRowSorter(sorter);
    }
}
