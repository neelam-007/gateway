package com.l7tech.console.panels;

import com.l7tech.cluster.GatewayStatus;
import com.l7tech.cluster.ServiceUsage;
import com.l7tech.console.table.StatisticsTableSorter;
import com.l7tech.console.util.ArrowIcon;
import com.l7tech.console.util.ColumnHeaderTooltips;
import com.l7tech.logging.GenericLogAdmin;
import com.l7tech.logging.StatisticsRecord;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;


/*
 * This class creates a statistics panel.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class StatisticsPanel extends JPanel {

    GenericLogAdmin logService = null;
//    private ServiceAdmin serviceManager = null;
    private Vector statsList = new Vector();

    // IMPORTANT NOTE:
    // 1. need to make sure that NUMBER_OF_SAMPLE_PER_MINUTE has no fraction when REFRESH_INTERVAL is changed
    // 2. REFRESH_INTERVAL must be <= 60
    private static final int REFRESH_INTERVAL = 5;
    private static final int STAT_REFRESH_TIMER = 1000 * REFRESH_INTERVAL;
    private static final int NUMBER_OF_SAMPLE_PER_MINUTE = 60 / REFRESH_INTERVAL;

    private JTable statTable = null;
    private JTable statTotalTable = null;
    private JScrollPane statTablePane = null;
    private StatisticsTableSorter statTableSorter = null;
    private DefaultTableColumnModel columnModel = null;
    private DefaultTableColumnModel totalColumnModel = null;
    private DefaultTableModel statTotalTableModel = null;
    private long attemptedCountTotal = 0;
    private long authorizedCountTotal = 0;
    private long completedCountTotal = 0;
    private long lastMinuteCompletedCountTotal = 0;
    private HashMap lastMinuteCompletedCountsCache;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);
    static Logger logger = Logger.getLogger(StatisticsPanel.class.getName());

    public StatisticsPanel() {
        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getStatTablePane());
        add(getStatTotalTable());
        setMinimumSize(new java.awt.Dimension(400, 200));
        setPreferredSize(new java.awt.Dimension(400, 400));
        lastMinuteCompletedCountsCache = new HashMap();
    }


    private JScrollPane getStatTablePane() {
        if (statTablePane != null) return statTablePane;

        statTablePane = new JScrollPane();

        statTablePane.setViewportView(getStatTable());
        statTablePane.getViewport().setBackground(getStatTable().getBackground());
        statTablePane.getVerticalScrollBar().addComponentListener(new ComponentListener(){
            public void componentHidden(ComponentEvent e){
                getStatTotalColumnModel().getColumn(5).setMinWidth(0);
                getStatTotalColumnModel().getColumn(5).setMaxWidth(0);
                getStatTotalColumnModel().getColumn(5).setPreferredWidth(0);
            }
            public void componentResized(ComponentEvent e){
                // not used - the scroll bar width is fixed
            }
            public void componentMoved(ComponentEvent e){
                // not used
            }
            public void componentShown(ComponentEvent e){
                getStatTotalColumnModel().getColumn(5).setMinWidth(((JScrollBar) e.getSource()).getWidth() + 1);
                getStatTotalColumnModel().getColumn(5).setMaxWidth(((JScrollBar) e.getSource()).getWidth() + 1);
                getStatTotalColumnModel().getColumn(5).setPreferredWidth(((JScrollBar) e.getSource()).getWidth() + 1);
            }
        });


        ColumnHeaderTooltips htt = new ColumnHeaderTooltips();
        htt.setToolTip(getStatColumnModel().getColumn(0), "Service name. Updated every " + StatisticsPanel.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(getStatColumnModel().getColumn(1), "Total requests attempted since the cluster is up. " + "Updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(getStatColumnModel().getColumn(2), "Total requests authorized since the cluster is up. " + "Updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(getStatColumnModel().getColumn(3), "Total requests routed since the cluster is up. " + "Updated every " + StatisticsPanel.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(getStatColumnModel().getColumn(4), "Total requests routed in the past 60 seconds. " + "Updated every " + StatisticsPanel.REFRESH_INTERVAL + " seconds");

        getStatTable().getTableHeader().addMouseMotionListener(htt);

        return statTablePane;
    }

    private JTable getStatTotalTable(){
        if(statTotalTable != null) return statTotalTable;

        statTotalTable = new JTable(getStatTotalTableModel(), getStatTotalColumnModel());
        statTotalTable.setRowSelectionAllowed(false);

        return statTotalTable;
    }

    private JTable getStatTable() {
        if (statTable != null) return statTable;

        statTable = new JTable(getStatTableModel(), getStatColumnModel());

        addMouseListenerToHeaderInTable(statTable);

        statTable.setShowHorizontalLines(false);
        statTable.setShowVerticalLines(false);
        statTable.setRowSelectionAllowed(false);
        statTable.getTableHeader().setReorderingAllowed(false);

        return statTable;
    }

    private DefaultTableColumnModel getStatColumnModel() {
        if(columnModel != null) return columnModel;

        columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(0, 300));
        columnModel.addColumn(new TableColumn(1, 80));
        columnModel.addColumn(new TableColumn(2, 50));
        columnModel.addColumn(new TableColumn(3, 50));
        columnModel.addColumn(new TableColumn(4, 80));

        for(int i = 0; i < columnModel.getColumnCount(); i++){
            columnModel.getColumn(i).setHeaderRenderer(iconHeaderRenderer);
            columnModel.getColumn(i).setHeaderValue(getStatTableModel().getColumnName(i));
        }

        columnModel.addColumnModelListener(new TableColumnModelListener(){
              public void columnMarginChanged(ChangeEvent e){
                  updateStatTotalTableColumnModel(e);
              }
              public void columnMoved(TableColumnModelEvent e){
                  // not used
              }
              public void columnAdded(TableColumnModelEvent e){
                  // not used
              }
               public void columnRemoved(TableColumnModelEvent e){
                   // not used
               }
            public void columnSelectionChanged(ListSelectionEvent e){
                // not used
            }
        });
        return columnModel;
    }


    private DefaultTableColumnModel getStatTotalColumnModel(){
        if(totalColumnModel != null ) return totalColumnModel;

        totalColumnModel = new DefaultTableColumnModel();

        totalColumnModel.addColumn(new TableColumn(0, 300));
        totalColumnModel.addColumn(new TableColumn(1, 80));
        totalColumnModel.addColumn(new TableColumn(2, 50));
        totalColumnModel.addColumn(new TableColumn(3, 50));
//        totalColumnModel.addColumn(new TableColumn(4, 100));
        totalColumnModel.addColumn(new TableColumn(4, 80));
        totalColumnModel.addColumn(new TableColumn(5, 15));
        return totalColumnModel;
    }

    private StatisticsTableSorter getStatTableModel() {
        if (statTableSorter != null) {
            return statTableSorter;
        }

        String[] cols = {"Web Service Name", "Requests Attempted", "Authorized", "Routed", "Routed (last min.)"};
        String[][] rows = new String[][]{};

        DefaultTableModel tableModel = new DefaultTableModel(rows, cols) {
            public boolean isCellEditable(int row, int col) {
                // the table cells are not editable
                return false;
            }
        };

        statTableSorter = new StatisticsTableSorter(tableModel) {
            public Class getColumnClass(int columnIndex) {
                Class dataType = java.lang.String.class;
                // Only the value of the first column is a string, other columns contains numbers which should be aligned to the right
                if (columnIndex > 0) {
                    dataType = java.lang.Number.class;
                }
                return dataType;
            }
        };

        return statTableSorter;
    }

    DefaultTableModel getStatTotalTableModel() {
        if (statTotalTableModel != null) {
            return statTotalTableModel;
        }

        String[] cols = {"Total", "Attempted Total", "Authorized", "Routed", "Routed (last min.)", "dummy"};
        String[][] rows = new String[][]{
            {"TOTAL", null, null, null},
        };

        statTotalTableModel = new DefaultTableModel(rows, cols) {
            public Class getColumnClass(int columnIndex) {
                Class dataType = java.lang.String.class;
                // Only the value of the first column is a string, other columns contains numbers which should be aligned to the right
                if (columnIndex > 0) {
                    dataType = java.lang.Number.class;
                }
                return dataType;
            }

            public boolean isCellEditable(int row, int col) {
                // the table cells are not editable
                return false;
            }
        };
        return statTotalTableModel;
    }


    public void updateStatisticsTable(Vector rawStatsList) {

        statsList = new Vector();
        attemptedCountTotal = 0;
        authorizedCountTotal = 0;
        completedCountTotal = 0;
        lastMinuteCompletedCountTotal = 0;

        for (int i = 0; i < rawStatsList.size(); i++) {

            ServiceUsage stats = (ServiceUsage) rawStatsList.get(i);
            long completedCount = stats.getCompleted();

            updateStatCache(stats.getServiceName(), completedCount);
            long lastMinuteCompletedCount = getLastMinuteCompletedCount(stats.getServiceName());

            StatisticsRecord statsRec = new StatisticsRecord(stats.getServiceName(),
                    stats.getRequests(),
                    stats.getAuthorized(),
                    completedCount,
                    lastMinuteCompletedCount);

            statsList.add(statsRec);
            attemptedCountTotal += stats.getRequests();
            authorizedCountTotal += stats.getAuthorized();
            completedCountTotal += stats.getCompleted();
            lastMinuteCompletedCountTotal += lastMinuteCompletedCount;
        }

        getStatTableModel().setData(statsList);
        getStatTableModel().getRealModel().setRowCount(statsList.size());
        getStatTableModel().fireTableDataChanged();

        updateReqeustsTotal();

    }

    private void updateReqeustsTotal() {

       getStatTotalTable().setValueAt(new Long(attemptedCountTotal), 0, 1);
       getStatTotalTable().setValueAt(new Long(authorizedCountTotal), 0, 2);
       getStatTotalTable().setValueAt(new Long(completedCountTotal), 0, 3);
       getStatTotalTable().setValueAt(new Long(lastMinuteCompletedCountTotal), 0, 4);
       getStatTotalTableModel().fireTableDataChanged();
    }

    public void clearStatistics() {
        while (getStatTableModel().getRowCount() > 0) {
            getStatTableModel().getRealModel().removeRow(0);
        }
        getStatTableModel().fireTableDataChanged();

        // clear the totals
        getStatTotalTable().setValueAt(null, 0, 1);
        getStatTotalTable().setValueAt(null, 0, 2);
        getStatTotalTable().setValueAt(null, 0, 3);
        getStatTotalTable().setValueAt(null, 0, 4);
        getStatTotalTableModel().fireTableDataChanged();

    }

    public void updateStatTotalTableColumnModel(ChangeEvent e) {

        getStatTotalColumnModel().getColumn(0).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(0).getWidth());
        getStatTotalColumnModel().getColumn(1).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(1).getWidth());
        getStatTotalColumnModel().getColumn(2).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(2).getWidth());
        getStatTotalColumnModel().getColumn(3).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(3).getWidth());
        getStatTotalColumnModel().getColumn(4).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(4).getWidth());

        getStatTotalColumnModel().setColumnMargin(((DefaultTableColumnModel) e.getSource()).getColumnMargin());
    }

    public void updateStatCache(String serviceName, long completedCount) {

        Vector lastMinuteCounts = null;

        lastMinuteCounts = (Vector) lastMinuteCompletedCountsCache.get(serviceName);

        if (lastMinuteCounts != null) {
            if(lastMinuteCounts.size() <= NUMBER_OF_SAMPLE_PER_MINUTE){
                lastMinuteCounts.add(new Long(completedCount));
            }
            else{
               lastMinuteCounts.remove(0);
               lastMinuteCounts.add(new Long(completedCount));
            }

            if (lastMinuteCounts.size() > 2) {
                // check if the new count is smaller than the previous count
                if (((Long) lastMinuteCounts.get(lastMinuteCounts.size() - 1)).longValue()
                        - ((Long) lastMinuteCounts.get(lastMinuteCounts.size() - 2)).longValue() < 0) {

                    // this could happen when a node is removed from the cluster
                    logger.info("New total completed count is less than the previous count. Service Name: " + serviceName);

                    // should clean up the cache
                    lastMinuteCounts.removeAllElements();
                }
            }
        } else {
            Vector newList = new Vector();
            newList.add(new Long(completedCount));
            lastMinuteCompletedCountsCache.put(serviceName, newList);
        }
    }

    private long getLastMinuteCompletedCount(String serviceName){

        long lastMinuteCompletedCount = 0;
        Vector lastMinuteCounts = null;

        lastMinuteCounts = (Vector) lastMinuteCompletedCountsCache.get(serviceName);

        if (lastMinuteCounts != null) {
            int index = lastMinuteCounts.size() - 1;

            for(int i = 0; i < lastMinuteCounts.size() - 1 ; i++, index--){

                lastMinuteCompletedCount += ((Long) lastMinuteCounts.get(index)).longValue() - ((Long) lastMinuteCounts.get(index-1)).longValue();
            }
        }

        //for debug purpose
/*        if (lastMinuteCompletedCount < 0) {
            // dump the counter
            System.out.println("Service Name: " + serviceName +  ", count = " + lastMinuteCompletedCount);
            if (lastMinuteCounts != null) {

                for (int index = 0; index < lastMinuteCounts.size(); index++) {

                    System.out.println("couter " + index + ": " +  ((Long) lastMinuteCounts.get(index)).longValue());
                }
            }

        }*/

        return lastMinuteCompletedCount;
    }

    // This customized renderer can render objects of the type TextandIcon
    TableCellRenderer iconHeaderRenderer = new DefaultTableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            // Inherit the colors and font from the header component
            if (table != null) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    setForeground(header.getForeground());
                    setBackground(header.getBackground());
                    setFont(header.getFont());
                    setHorizontalTextPosition(SwingConstants.LEFT);
                }
            }

            setText((String) value);

            if (getStatTableModel().getSortedColumn() == column) {

                if (getStatTableModel().isAscending()) {
                    setIcon(upArrowIcon);
                } else {
                    setIcon(downArrowIcon);
                }
            }
            else{
                setIcon(null);
            }

            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    };

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

                    ((StatisticsTableSorter)tableView.getModel()).sortData(column, true);
                    ((StatisticsTableSorter)tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }
}
