package com.l7tech.console.panels;

import com.l7tech.adminws.logging.Log;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.console.table.LogTableModel;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.service.ServiceStatistics;

import javax.swing.*;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.HashMap;


/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 26, 2003
 * Time: 12:51:48 PM
 * To change this template use Options | File Templates.
 */
public class StatisticsPanel extends JPanel {

    private static final String STATISTICS_UNAVAILABLE = "unavailable";
    private static final String SERVER_UP_TIME_PREFIX = "Server Uptime: ";
    private static final String LAST_MINUTE_SERVER_LOAD_PREFIX = "Avg load (1 min): ";
    private static final String MIDDLE_SPACE = "     ";
    private static final String END_SPACE    = "   ";
    private static final int STAT_REFRESH_TIMER = 5000;
    private static final int NUMBER_OF_SAMPLE_PER_MINUTE = 12;
    private com.l7tech.adminws.service.ServiceManager serviceManager = null;
    private JTable statTable = null;
    private JTable statTotalTable = null;
    private JScrollPane statTablePane = null;
    private DefaultTableModel statTableModel = null;
    private DefaultTableColumnModel columnModel = null;
    private DefaultTableColumnModel totalColumnModel = null;
    private DefaultTableModel statTotalTableModel = null;
    private JPanel selectPane = null;
    private JPanel controlPane = null;
    private JCheckBox autoRefresh = null;
    private JPanel serverLoadPane = null;
    private javax.swing.Timer statRefreshTimer = null;
    private JLabel serverUpTime = null;
    private JLabel lastMinuteServerLoad = null;
    private Log logstub = (Log) Locator.getDefault().lookup(Log.class);
    private JPanel selectPaneRight = null;
    private JPanel selectPaneLeft = null;
    private long attemptedCountTotal = 0;
    private long authorizedCountTotal = 0;
    private long completedCountTotal = 0;
    private long serverUpTimeMinutues = 0;
    private long completedCountPerMinuteTotal = 0;
    private long lastMinuteCompletedCountTotal = 0;
    private HashMap lastMinuteCompletedCountsCache = new HashMap();

    public StatisticsPanel() {
        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getStatTablePane());
        add(getStatTotalTable());
        add(getSelectPane());

        setVisible(false);

    }

    private JScrollPane getStatTablePane() {
        if (statTablePane != null) return statTablePane;

        statTablePane = new JScrollPane();

        statTablePane.setViewportView(getStatTable());
        statTablePane.getViewport().setBackground(getStatTable().getBackground());

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

        statTable.setShowHorizontalLines(false);
        statTable.setShowVerticalLines(false);
        statTable.setRowSelectionAllowed(false);

        return statTable;
    }

    private DefaultTableColumnModel getStatColumnModel() {
        if(columnModel != null) return columnModel;

        columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(0, 300));
        columnModel.addColumn(new TableColumn(1, 80));
        columnModel.addColumn(new TableColumn(2, 50));
        columnModel.addColumn(new TableColumn(3, 50));
        columnModel.addColumn(new TableColumn(4, 100));
        columnModel.addColumn(new TableColumn(5, 80));
        columnModel.getColumn(0).setHeaderValue(getStatTableModel().getColumnName(0));
        columnModel.getColumn(1).setHeaderValue(getStatTableModel().getColumnName(1));
        columnModel.getColumn(2).setHeaderValue(getStatTableModel().getColumnName(2));
        columnModel.getColumn(3).setHeaderValue(getStatTableModel().getColumnName(3));
        columnModel.getColumn(4).setHeaderValue(getStatTableModel().getColumnName(4));
        columnModel.getColumn(5).setHeaderValue(getStatTableModel().getColumnName(5));

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
        totalColumnModel.addColumn(new TableColumn(4, 100));
        totalColumnModel.addColumn(new TableColumn(5, 80));

        return totalColumnModel;
    }

    private DefaultTableModel getStatTableModel() {
        if (statTableModel != null) {
            return statTableModel;
        }

        String[] cols = {"Service Name", "Attempted Requests", "Authorized", "Completed", "Completed (requests/min)", "Completed (last min.)"};
        String[][] rows = new String[][]{};

        statTableModel = new LogTableModel(rows, cols) {
            public Class getColumnClass(int columnIndex) {
                Class dataType = java.lang.String.class;
                // Only the value of the first column is a string, other columns contains numbers which should be aligned to the right
                if (columnIndex > 0) {
                    dataType = java.lang.Number.class;
                }
                return dataType;
            }
        };

        return statTableModel;
    }

    DefaultTableModel getStatTotalTableModel() {
        if (statTotalTableModel != null) {
            return statTotalTableModel;
        }

        String[] cols = {"Total", "Attempted Total", "Authorized", "Completed", "Completed requests/min", "Completed (last min.)"};
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
        selectPane.add(getSelectPaneLeft(), BorderLayout.WEST);
        selectPane.add(getSelectPaneRight(), BorderLayout.EAST);

        return selectPane;
    }

    private JPanel getSelectPaneRight(){
        if(selectPaneRight != null) return selectPaneRight;

        selectPaneRight = new JPanel();
        selectPaneRight.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 10));
        selectPaneRight.add(getServerLoadPane());
        return selectPaneRight;
    }

    private JPanel getSelectPaneLeft(){
        if( selectPaneLeft != null) return selectPaneLeft;

        selectPaneLeft = new JPanel();
        selectPaneLeft.setLayout(new FlowLayout(FlowLayout.LEFT));
        selectPaneLeft.add(getControlPane());

        return selectPaneLeft;
    }
    private JPanel getServerLoadPane() {
        if (serverLoadPane != null) return serverLoadPane;

        serverLoadPane = new JPanel();
        serverLoadPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

        if (serverUpTime == null) serverUpTime = new JLabel();
        if (lastMinuteServerLoad == null) lastMinuteServerLoad = new JLabel();
        serverLoadPane.add(serverUpTime);
        serverLoadPane.add(lastMinuteServerLoad);

        return serverLoadPane;
    }

    private JPanel getControlPane() {
        if (controlPane != null) return controlPane;

        controlPane = new JPanel();
        controlPane.setLayout(new FlowLayout(FlowLayout.LEFT));

        if (autoRefresh == null) {
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


        return controlPane;
    }

    private javax.swing.Timer getStatRefreshTimer() {

        if (statRefreshTimer != null) return statRefreshTimer;

        // Create a refresh timer.
        statRefreshTimer = new javax.swing.Timer(STAT_REFRESH_TIMER, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                refreshStatistics();
            }
        });

        return statRefreshTimer;
    }

    public void refreshStatistics() {

        getStatRefreshTimer().stop();

        // also update the server load statistics
        loadServerMetricsValues();

        boolean cleanUp = true;

        if (serviceManager == null) {
            serviceManager = (com.l7tech.adminws.service.ServiceManager) Locator.getDefault().lookup(com.l7tech.adminws.service.ServiceManager.class);
            if (serviceManager == null) throw new RuntimeException("Cannot instantiate the ServiceManager");
        }

        com.l7tech.objectmodel.EntityHeader[] entityHeaders = null;

        try {
            entityHeaders = serviceManager.findAllPublishedServices();

            EntityHeader header = null;
            for (int i = 0; i < entityHeaders.length; i++) {

                // remove old table data
                if (cleanUp) {
                    while (getStatTableModel().getRowCount() > 0) {
                        getStatTableModel().removeRow(0);
                    }
                    attemptedCountTotal = 0;
                    authorizedCountTotal = 0;
                    completedCountTotal = 0;
                    completedCountPerMinuteTotal = 0;
                    lastMinuteCompletedCountTotal = 0;
                    cleanUp = false;
                }

                header = entityHeaders[i];
                if (header.getType().toString() == com.l7tech.objectmodel.EntityType.SERVICE.toString()) {
                    Vector newRow = new Vector();

                    newRow.add(header.getName());
                    ServiceStatistics stats = null;

                    try {
                        stats = serviceManager.getStatistics(header.getOid());

                        if (stats != null) {
                            long completedCounts = stats.getCompletedRequestCount();

                            newRow.add(new Integer(stats.getAttemptedRequestCount()));
                            newRow.add(new Integer(stats.getAuthorizedRequestCount()));
                            newRow.add(new Long(completedCounts));

                            if(serverUpTimeMinutues > 0){
                                newRow.add(new Long(completedCounts/serverUpTimeMinutues));
                                completedCountPerMinuteTotal += completedCounts/serverUpTimeMinutues;
                            }
                            else{
                                newRow.add(new Long(0));
                            }

                            updateStatCache(header.getName(), completedCounts);

                            long lastMinuteCompletedCount = getLastMinuteCompletedCount(header.getName());
                            newRow.add(new Long(lastMinuteCompletedCount));

                            getStatTableModel().addRow(newRow);
                            attemptedCountTotal += stats.getAttemptedRequestCount();
                            authorizedCountTotal += stats.getAuthorizedRequestCount();
                            completedCountTotal += stats.getCompletedRequestCount();
                            lastMinuteCompletedCountTotal += lastMinuteCompletedCount;
                        }

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                }

            }
        } catch (RemoteException e) {
        }

        getStatTableModel().fireTableDataChanged();
        updateReqeustsTotal();

        getStatRefreshTimer().start();
    }

    private void updateReqeustsTotal(){

       getStatTotalTable().setValueAt(new Long(attemptedCountTotal), 0, 1);
       getStatTotalTable().setValueAt(new Long(authorizedCountTotal), 0, 2);
       getStatTotalTable().setValueAt(new Long(completedCountTotal), 0, 3);
       getStatTotalTable().setValueAt(new Long(completedCountPerMinuteTotal), 0, 4);
        getStatTotalTable().setValueAt(new Long(lastMinuteCompletedCountTotal), 0, 5);
       getStatTotalTableModel().fireTableDataChanged();
    }

    private void loadServerMetricsValues() {
        UptimeMetrics metrics = null;
        try {
            metrics = logstub.getUptime();
        } catch (RemoteException e) {
            metrics = null;
        }
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
    }

    public void stopRefreshTimer() {
        getStatRefreshTimer().stop();
    }

    public void clearStatiistics() {
        while (getStatTableModel().getRowCount() > 0) {
            getStatTableModel().removeRow(0);
        }
        getStatTableModel().fireTableDataChanged();
        serverUpTime.setText(END_SPACE + SERVER_UP_TIME_PREFIX + STATISTICS_UNAVAILABLE + MIDDLE_SPACE);
        lastMinuteServerLoad.setText(LAST_MINUTE_SERVER_LOAD_PREFIX + STATISTICS_UNAVAILABLE + END_SPACE);
    }

    public void updateStatTotalTableColumnModel(ChangeEvent e) {

        getStatTotalColumnModel().getColumn(0).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(0).getWidth());
        getStatTotalColumnModel().getColumn(1).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(1).getWidth());
        getStatTotalColumnModel().getColumn(2).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(2).getWidth());
        getStatTotalColumnModel().getColumn(3).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(3).getWidth());
        getStatTotalColumnModel().getColumn(4).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(4).getWidth());
        getStatTotalColumnModel().getColumn(5).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(5).getWidth());

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
}
