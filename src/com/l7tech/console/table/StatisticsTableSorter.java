package com.l7tech.console.table;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Oct 27, 2003
 * Time: 2:57:24 PM
 * To change this template use Options | File Templates.
 */

import com.l7tech.logging.StatisticsRecord;

import java.util.*;
import java.util.logging.Logger;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableModel;

public class StatisticsTableSorter extends FilteredDefaultTableModel {
    static Logger logger = Logger.getLogger(StatisticsTableSorter.class.getName());
    boolean ascending = true;
    int columnToSort = 0;
    int compares;
    private Vector data;
    private Object[] sortedData = null;

    public StatisticsTableSorter() {
    }

    public StatisticsTableSorter(DefaultTableModel model) {
        setModel(model);
    }

    public void setModel(DefaultTableModel model) {
        super.setRealModel(model);
    }

    public void setData(Vector data) {
        this.data = data;
        sortData();
    }

    private void sortData() {

        Object[] sorted = data.toArray();
        Arrays.sort(sorted, new ColumnSorter(columnToSort, ascending));

        sortedData = sorted;
    }

    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return ((StatisticsRecord) sortedData[row]).getServiceName();
            case 1:
                return new Long(((StatisticsRecord) sortedData[row]).getAttemptedCount());
            case 2:
                return new Long(((StatisticsRecord) sortedData[row]).getAuthorizedCount());
            case 3:
                return new Long(((StatisticsRecord) sortedData[row]).getCompletedCount());
            case 4:
                return new Long(((StatisticsRecord) sortedData[row]).getCompletedCountPerMinute());
            case 5:
                return new Long(((StatisticsRecord) sortedData[row]).getCompletedCountLastMinute());
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    // Add a mouse listener to the Table to trigger a table sort
    // when a column heading is clicked in the JTable.
    public void addMouseListenerToHeaderInTable(JTable table) {

        final JTable tableView = table;
        tableView.setColumnSelectionAllowed(false);
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {

                    //int shiftPressed = e.getModifiers()&InputEvent.SHIFT_MASK;
                    //ascending = (shiftPressed == 0);

                    // toggle the sorting order
                    ascending = ascending ? false : true;
                    columnToSort = column;


                    sortData();
                    fireTableDataChanged();

                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    public class ColumnSorter implements Comparator {
        private boolean ascending;
        private int column;

        ColumnSorter(int column, boolean ascending) {
            this.ascending = ascending;
            this.column = column;
        }

        public int compare(Object a, Object b) {

            String elementA = new String("");
            String elementB = new String("");

            switch (column) {
                case 0:
                    elementA = ((StatisticsRecord) a).getServiceName();
                    elementB = ((StatisticsRecord) b).getServiceName();
                    break;
                case 1:
                    elementA = Long.toString(((StatisticsRecord) a).getAttemptedCount());
                    elementB = Long.toString(((StatisticsRecord) b).getAttemptedCount());
                    break;
                case 2:
                    elementA = Long.toString(((StatisticsRecord) a).getAuthorizedCount());
                    elementB = Long.toString(((StatisticsRecord) b).getAuthorizedCount());
                    break;
                case 3:
                    elementA = Long.toString(((StatisticsRecord) a).getCompletedCount());
                    elementB = Long.toString(((StatisticsRecord) b).getCompletedCount());
                    break;
                case 4:
                    elementA = Long.toString(((StatisticsRecord) a).getCompletedCountPerMinute());
                    elementB = Long.toString(((StatisticsRecord) b).getCompletedCountPerMinute());
                    break;
                case 5:
                    elementA = Long.toString(((StatisticsRecord) a).getCompletedCountLastMinute());
                    elementB = Long.toString(((StatisticsRecord) b).getCompletedCountLastMinute());
                    break;
                default:
                    logger.warning("Bad Statistics Table Column: " + column);
                    break;
            }

            // Treat empty strains like nulls
            if (elementA instanceof String && (elementA).length() == 0) {
                elementA = null;
            }
            if (elementB instanceof String && (elementB).length() == 0) {
                elementB = null;
            }

            // Sort nulls so they appear last, regardless
            // of sort order
            if (elementA == null && elementB == null) {
                return 0;
            } else if (elementA == null) {
                return 1;
            } else if (elementB == null) {
                return -1;
            } else {
                if (ascending) {
                    return (elementA).compareTo(elementB);
                } else {
                    return (elementB).compareTo(elementA);
                }
            }
        }
    }

}

