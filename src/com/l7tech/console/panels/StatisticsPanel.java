package com.l7tech.console.panels;

import com.l7tech.adminws.logging.Log;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.console.table.LogTableModel;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.service.ServiceStatistics;

import javax.swing.*;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Vector;


/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 26, 2003
 * Time: 12:51:48 PM
 * To change this template use Options | File Templates.
 */
public class StatisticsPanel extends JPanel {

    private static final String SERVER_UP_TIME_PREFIX = "Server Uptime: ";
    private static final String LAST_MINUTE_SERVER_LOAD_PREFIX = "Avg load (1 min): ";

    private JTextArea data = new JTextArea("Hello");
    private com.l7tech.adminws.service.ServiceManager serviceManager = null;
    private JTable statTable = null;
    private JScrollPane statTablePane = null;
    private DefaultTableModel statTableModel = null;
    private JPanel selectPane = null;
    private JPanel controlPane = null;
    private JCheckBox autoRefresh = null;
    private JPanel serverLoadPane = null;
    private javax.swing.Timer statRefreshTimer = null;
    private JLabel serverUpTime = null;
    private JLabel lastMinuteServerLoad = null;
    private Log logstub = (Log) Locator.getDefault().lookup(Log.class);

    public StatisticsPanel() {
        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getStatTablePane());
        add(getSelectPane());

        setVisible(false);

    }

    private JScrollPane getStatTablePane() {
        if (statTablePane == null) {
            statTablePane = new JScrollPane();
        }
        statTablePane.setViewportView(getStatTable());
        statTablePane.getViewport().setBackground(getStatTable().getBackground());

        return statTablePane;
    }

    private JTable getStatTable() {
        if (statTable == null) {
            statTable = new JTable(getStatTableModel(), getStatColumnModel());
        }

        statTable.setShowHorizontalLines(false);
        statTable.setShowVerticalLines(false);
        statTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        statTable.setRowSelectionAllowed(true);

        return statTable;
    }

    private DefaultTableColumnModel getStatColumnModel() {
        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(0, 400));
        columnModel.addColumn(new TableColumn(1, 100));
        columnModel.addColumn(new TableColumn(2, 100));
        columnModel.addColumn(new TableColumn(3, 100));
        columnModel.getColumn(0).setHeaderValue(getStatTableModel().getColumnName(0));
        columnModel.getColumn(1).setHeaderValue(getStatTableModel().getColumnName(1));
        columnModel.getColumn(2).setHeaderValue(getStatTableModel().getColumnName(2));
        columnModel.getColumn(3).setHeaderValue(getStatTableModel().getColumnName(3));

        return columnModel;
    }

    private DefaultTableModel getStatTableModel() {
        if (statTableModel != null) {
            return statTableModel;
        }

        String[] cols = {"Service Name", "Attempted Requests", "Authorized Requests", "Completed Requests"};
        String[][] rows = new String[][]{};

        statTableModel = new LogTableModel(rows, cols) {
            public Class getColumnClass(int columnIndex){
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

    private JPanel getSelectPane() {
        if (selectPane == null) {
            selectPane = new JPanel();
        }

        selectPane.setMinimumSize(new Dimension((int) selectPane.getSize().getWidth(), 35));
        // selectPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        selectPane.setLayout(new BorderLayout());
        selectPane.add(getServerLoadPane(), BorderLayout.EAST);
        selectPane.add(getControlPane(), BorderLayout.WEST);

        return selectPane;
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
        if (controlPane == null) {
            controlPane = new JPanel();
        }

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
        statRefreshTimer = new javax.swing.Timer(3000, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                refreshStatistics();
            }
        });

        return statRefreshTimer;
    }

    public void refreshStatistics() {

        getStatRefreshTimer().stop();

        boolean cleanUp = true;

        if (serviceManager == null) {
            serviceManager = (com.l7tech.adminws.service.ServiceManager) Locator.getDefault().lookup(com.l7tech.adminws.service.ServiceManager.class);
            if (serviceManager == null) throw new RuntimeException("Cannot instantiate the ServiceManager");
        }

        com.l7tech.objectmodel.EntityHeader[] entityHeaders = null;

        try {
            entityHeaders = serviceManager.findAllPublishedServices();

            System.out.println("Number of published services: " + entityHeaders.length);
            EntityHeader header = null;
            for (int i = 0; i < entityHeaders.length; i++) {

                // remove old table data
                if (cleanUp) {
                    while (getStatTableModel().getRowCount() > 0) {
                        getStatTableModel().removeRow(0);
                    }
                    cleanUp = false;
                }

                header = entityHeaders[i];
                System.out.println("Name of the published service: " + header.getName());
                System.out.println("Type of the published service: " + header.getType());

                if ( header.getType().toString() == com.l7tech.objectmodel.EntityType.SERVICE.toString()) {
                    Vector newRow = new Vector();

                    newRow.add(header.getName());
                    ServiceStatistics stats = null;

                    try {
                        //System.out.println("Service Oid: " + header.getOid());
                        stats = serviceManager.getStatistics(header.getOid());

                        if (stats != null) {
                            //System.out.println("Attempted Request Count: " + stats.getAttemptedRequestCount());
                            //System.out.println("Authorized Request Count: " + stats.getAuthorizedRequestCount());
                            //System.out.println("Completeded Request Count: " + stats.getCompletedRequestCount());

                            newRow.add(new Integer(stats.getAttemptedRequestCount()));
                            newRow.add(new Integer(stats.getAuthorizedRequestCount()));
                            newRow.add(new Integer(stats.getCompletedRequestCount()));

                            getStatTableModel().addRow(newRow);
                        }

                    } catch (Throwable t) {
                       t.printStackTrace();
                    }

                }

                getStatTableModel().fireTableDataChanged();

                // also update the server load statistics
                loadValues();
            }
        } catch (RemoteException e) {
        }

        getStatRefreshTimer().start();
    }


    private void loadValues() {
        UptimeMetrics metrics = null;
        try {
            metrics = logstub.getUptime();
        } catch (RemoteException e) {
            metrics = null;
        }
        if (metrics == null) {
             serverUpTime.setText(SERVER_UP_TIME_PREFIX + "unavailable" + "        ");
             lastMinuteServerLoad.setText(LAST_MINUTE_SERVER_LOAD_PREFIX + "unavailable" + "    ");

            //fiveminuteLoadText.setText("unavailable");
            //fifteenminuteLoadText.setText("unavailable");

        } else {
            int days = metrics.getDays();
            int hrs = metrics.getHours();
            int minutes = metrics.getMinutes();
            String uptime = "";
            if (days > 0) uptime += Integer.toString(days) + " days ";
            if (hrs > 0) uptime += Integer.toString(hrs) + " hrs ";
            if (minutes > 0) uptime += Integer.toString(minutes) + " minutes " + "        ";
            serverUpTime.setText(SERVER_UP_TIME_PREFIX + uptime);
            lastMinuteServerLoad.setText(LAST_MINUTE_SERVER_LOAD_PREFIX + Double.toString(metrics.getLoad1()) + "    ");

            //fiveminuteLoadText.setText(Double.toString(metrics.getLoad2()));
            //fifteenminuteLoadText.setText(Double.toString(metrics.getLoad3()));
        }
    }

    public void stopRefreshTimer() {
        getStatRefreshTimer().stop();
    }

}
