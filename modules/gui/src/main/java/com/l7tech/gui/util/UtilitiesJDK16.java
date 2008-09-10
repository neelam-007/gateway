package com.l7tech.gui.util;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableModel;
import javax.swing.table.TableStringConverter;
import java.util.ArrayList;
import java.util.Comparator;

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
                              final Comparator [] comparators) {
        if(cols != null && comparators != null){
            if(cols.length != comparators.length) throw new IllegalArgumentException("Length of comparators must match " +
                "length of cols array.");
        }
        if(comparators != null && cols == null){
            throw new IllegalArgumentException("Cannot supply comparators if cols have not also been supplied");
        }
        TableRowSorter sorter = new TableRowSorter(model);

        if ( cols != null ) {
            java.util.List <RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
            for ( int i=0; i< cols.length; i++ ) {
                sortKeys.add(new RowSorter.SortKey(cols[i], order[i]?SortOrder.ASCENDING:SortOrder.DESCENDING));
            }
            sorter.setSortKeys(sortKeys);
        }

        if( comparators != null){
            for(int i = 0; i < comparators.length; i++){
                if(comparators[i] != null){
                    sorter.setComparator(i, comparators[i]);
                }
            }
        }

        table.setRowSorter(sorter);
        sorter.sort();
    }
}
