package com.l7tech.console.panels;

import com.l7tech.common.util.Locator;
import com.l7tech.console.table.LogTableModel;
import com.l7tech.console.table.StatisticsTableSorter;
import com.l7tech.console.icons.ArrowIcon;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.logging.LogAdmin;
import com.l7tech.logging.StatisticsRecord;
import com.l7tech.cluster.ServiceUsage;

import javax.swing.*;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.HashMap;


/*
 * This class creates a statistics panel.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class StatisticsPanel extends JPanel {

    private static final String STATISTICS_UNAVAILABLE = "unavailable";
    private static final String SERVER_UP_TIME_PREFIX = "Server Uptime: ";
    private static final String LAST_MINUTE_SERVER_LOAD_PREFIX = "Avg load (1 min): ";
    private static final String MIDDLE_SPACE = "     ";
    private static final String END_SPACE    = "   ";

    LogAdmin logService = null;
    private ServiceAdmin serviceManager = null;
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
    private JPanel selectPane = null;
    private JPanel controlPane = null;
    private JCheckBox autoRefresh = null;
    private JPanel serverLoadPane = null;
    private javax.swing.Timer statRefreshTimer = null;
//    private JLabel serverUpTime = null;
//    private JLabel lastMinuteServerLoad = null;
//    private JPanel selectPaneRight = null;
    private JPanel selectPaneLeft = null;
    private long attemptedCountTotal = 0;
    private long authorizedCountTotal = 0;
    private long completedCountTotal = 0;
//    private long serverUpTimeMinutues = 0;
//    private long completedCountPerMinuteTotal = 0;
    private long lastMinuteCompletedCountTotal = 0;
    private HashMap lastMinuteCompletedCountsCache;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);

    public StatisticsPanel() {
        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getStatTablePane());
        add(getStatTotalTable());
        add(getSelectPane());

        lastMinuteCompletedCountsCache = new HashMap();

   //     setVisible(false);

    }

    /**
     * This function is called when a connection to the server is established.
     */
    public void onConnect(){

        logService = (LogAdmin) Locator.getDefault().lookup(LogAdmin.class);
        if (logService == null) throw new IllegalStateException("Cannot obtain LogAdmin remote reference");

        serviceManager = (ServiceAdmin) Locator.getDefault().lookup(ServiceAdmin.class);
        if (serviceManager == null) throw new RuntimeException("Cannot obtain ServiceManager remote reference");

        // intantiate the cache
        lastMinuteCompletedCountsCache = new HashMap();

        statsList = new Vector();
    }

    public void onDisconnect(){

        logService = null;
        serviceManager = null;
        lastMinuteCompletedCountsCache = null;

    //    stopRefreshTimer();
        clearStatiistics();
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

        for(int i = 0; i <= 4; i++){
             columnModel.getColumn(i).setHeaderRenderer(iconHeaderRenderer);
        }

        columnModel.getColumn(0).setHeaderValue(getStatTableModel().getColumnName(0));
        columnModel.getColumn(1).setHeaderValue(getStatTableModel().getColumnName(1));
        columnModel.getColumn(2).setHeaderValue(getStatTableModel().getColumnName(2));
        columnModel.getColumn(3).setHeaderValue(getStatTableModel().getColumnName(3));
        columnModel.getColumn(4).setHeaderValue(getStatTableModel().getColumnName(4));


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

        String[] cols = {"Service Name", "Requests Attempted", "Authorized", "Completed", "Completed (last min.)"};
        String[][] rows = new String[][]{};

        LogTableModel tableModel = new LogTableModel(rows, cols);

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

        String[] cols = {"Total", "Attempted Total", "Authorized", "Completed", "Completed (last min.)", "dummy"};
        String[][] rows = new String[][]{
            {"TOTAL", null, null, null},
        };

        statTotalTableModel = new LogTableModel(rows, cols){
           public Class getColumnClass(int columnIndex) {
                Class dataType = java.lang.String.class;
                // Only the value of the first column is a string, other columns contains numbers which should be aligned to the right
                if (columnIndex > 0) {
                    dataType = java.lang.Number.class;
                }
                return dataType;
            }
        };
        return statTotalTableModel;
    }

    private JPanel getSelectPane() {
        if (selectPane != null) return selectPane;

        selectPane = new JPanel();
        selectPane.setMinimumSize(new Dimension((int) selectPane.getSize().getWidth(), 35));
        selectPane.setLayout(new BorderLayout());
        selectPane.add(getSelectPaneLeft(), BorderLayout.EAST);
//        selectPane.add(getSelectPaneRight(), BorderLayout.EAST);

        return selectPane;
    }

/*    private JPanel getSelectPaneRight(){
        if(selectPaneRight != null) return selectPaneRight;

        selectPaneRight = new JPanel();
        selectPaneRight.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 10));
        selectPaneRight.add(getServerLoadPane());
        return selectPaneRight;
    }*/

    private JPanel getSelectPaneLeft(){
        if( selectPaneLeft != null) return selectPaneLeft;

        selectPaneLeft = new JPanel();
        selectPaneLeft.setLayout(new FlowLayout(FlowLayout.LEFT));
        selectPaneLeft.add(getControlPane());

        return selectPaneLeft;
    }

/*    private JPanel getServerLoadPane() {
        if (serverLoadPane != null) return serverLoadPane;

        serverLoadPane = new JPanel();
        serverLoadPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

        if (serverUpTime == null) serverUpTime = new JLabel();
        if (lastMinuteServerLoad == null) lastMinuteServerLoad = new JLabel();
        serverLoadPane.add(serverUpTime);
        serverLoadPane.add(lastMinuteServerLoad);

        return serverLoadPane;
    }*/

    private JPanel getControlPane() {
        if (controlPane != null) return controlPane;

        controlPane = new JPanel();
        controlPane.setLayout(new FlowLayout(FlowLayout.LEFT));

 /*       if (autoRefresh == null) {
            autoRefresh = new JCheckBox();
        }
        autoRefresh.setFont(new java.awt.Font("Dialog", 0, 11));
        autoRefresh.setText("Auto-refresh");
        autoRefresh.setSelected(true);
        autoRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (autoRefresh.isSelected()) {
                    getStatRefreshTimer().start();
                } else {
                    getStatRefreshTimer().stop();
                }
            }
        });
        controlPane.add(autoRefresh);
*/

        return controlPane;
    }

/*    private javax.swing.Timer getStatRefreshTimer() {

        if (statRefreshTimer != null) return statRefreshTimer;

        // Create a refresh timer.
        statRefreshTimer = new javax.swing.Timer(STAT_REFRESH_TIMER, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                refreshStatistics();
            }
        });

        return statRefreshTimer;
    }*/

/*    public void refreshStatistics() {

        getStatRefreshTimer().stop();

        // create a worker thread to retrieve the Service statistics
        final StatisticsWorker statsWorker = new StatisticsWorker(serviceManager, logService) {
            public void finished(){
                //updateServerMetricsFields(getMetrics());
                updateStatisticsTable(getStatsList());
            }
        };

        statsWorker.start();
    }*/

    public void updateStatisticsTable(Vector rawStatsList) {

        statsList = new Vector();
        attemptedCountTotal = 0;
        authorizedCountTotal = 0;
        completedCountTotal = 0;
    //    completedCountPerMinuteTotal = 0;
        lastMinuteCompletedCountTotal = 0;

        for (int i = 0; i < rawStatsList.size(); i++) {

            ServiceUsage stats = (ServiceUsage) rawStatsList.get(i);
            long completedCount = stats.getCompleted();
            long completedCountPerMinute = 0;
   /*         if (serverUpTimeMinutues > 0) {
                completedCountPerMinute = completedCount / serverUpTimeMinutues;
                completedCountPerMinuteTotal += completedCount / serverUpTimeMinutues;
            }*/

            updateStatCache(stats.getServiceName(), completedCount);
            long lastMinuteCompletedCount = getLastMinuteCompletedCount(stats.getServiceName());

            StatisticsRecord statsRec = new StatisticsRecord(stats.getServiceName(),
                    (long) stats.getRequests(),
                    (long) stats.getAuthorized(),
                    completedCount,
//                    completedCountPerMinute,
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

//        getStatRefreshTimer().start();

    }

    private void updateReqeustsTotal() {

       getStatTotalTable().setValueAt(new Long(attemptedCountTotal), 0, 1);
       getStatTotalTable().setValueAt(new Long(authorizedCountTotal), 0, 2);
       getStatTotalTable().setValueAt(new Long(completedCountTotal), 0, 3);
       getStatTotalTable().setValueAt(new Long(lastMinuteCompletedCountTotal), 0, 4);
       getStatTotalTableModel().fireTableDataChanged();
    }

/*    private void updateServerMetricsFields(UptimeMetrics metrics) {

        if (metrics == null) {
            serverUpTime.setText(END_SPACE + SERVER_UP_TIME_PREFIX + STATISTICS_UNAVAILABLE + MIDDLE_SPACE);
            lastMinuteServerLoad.setText(LAST_MINUTE_SERVER_LOAD_PREFIX + STATISTICS_UNAVAILABLE + END_SPACE);

        } else {

            long uptime_ms = (System.currentTimeMillis() - metrics.getServerBootTime()) / 1000;

            serverUpTimeMinutues = uptime_ms/60;
            long minutes_remain = serverUpTimeMinutues%60;
            long hrs_total = serverUpTimeMinutues/60;
            long hrs_remain = hrs_total%24;
            long days = hrs_total/24;
            String uptimeString = "";

            if(days == 1){
                uptimeString += Long.toString(days) + " day ";
            }
            else{
                uptimeString += Long.toString(days) + " days ";
            }
            if(hrs_remain == 1){
                uptimeString += Long.toString(hrs_remain) + " hour ";
            }
            else{
                uptimeString += Long.toString(hrs_remain) + " hours ";
            }
            if(minutes_remain == 1){
                uptimeString += Long.toString(minutes_remain) + " minute " + MIDDLE_SPACE;
            }
            else{
                uptimeString += Long.toString(minutes_remain) + " minutes " + MIDDLE_SPACE;
            }
            serverUpTime.setText(END_SPACE + SERVER_UP_TIME_PREFIX + uptimeString);
            lastMinuteServerLoad.setText(LAST_MINUTE_SERVER_LOAD_PREFIX + Double.toString(metrics.getLoad1()) + END_SPACE);

        }
    }*/

/*    public void stopRefreshTimer() {
        getStatRefreshTimer().stop();
    }*/

    public void clearStatiistics() {
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
