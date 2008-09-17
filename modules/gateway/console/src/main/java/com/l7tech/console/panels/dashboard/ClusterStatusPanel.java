/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.dashboard;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.console.GatewayLogWindow;
import com.l7tech.console.MainWindow;
import com.l7tech.console.action.BaseAction;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.panels.EditGatewayNameDialog;
import com.l7tech.console.panels.StatisticsPanel;
import com.l7tech.console.table.ClusterStatusTableSorter;
import com.l7tech.console.util.*;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.gateway.common.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

/**
 * For display of cluster status and statistics.
 *
 * <p>This was refactored out of ClusterStatusWindow.
 * Up to SecureSpan 4.1, cluster status was displayed in its own window
 * (ClusterStatusWindow). From SecureSpan 4.2 on, it is displayed in a tab
 * inside {@link DashboardWindow}.
 *
 * @since SecureSpan 4.2
 * @author rmak
 */
public class ClusterStatusPanel extends JPanel {

    private JPanel mainPanel;
    private JTable clusterStatusTable;
    private StatisticsPanel statisticsPanel;
    private JLabel updateTimeStamp;
    private JTextField serviceNameFilterField;
    private JButton sinceNowButton;
    private JTextField countingSinceField;
    private JButton sinceStartupButton;
    private JButton clearServiceNameFilterButton;

    private static final int MIN = 0;
    private static final int MAX = 100;

    public static final int STATUS_TABLE_NODE_STATUS_COLUMN_INDEX = 0;
    public static final int STATUS_TABLE_NODE_NAME_COLUMN_INDEX = 1;
    public static final int STATUS_TABLE_LOAD_SHARING_COLUMN_INDEX = 2;
    public static final int STATUS_TABLE_REQUEST_ROUTED_COLUMN_INDEX = 3;
    public static final int STATUS_TABLE_LOAD_AVERAGE_COLUMN_INDEX = 4;
    public static final int STATUS_TABLE_SERVER_UPTIME_COLUMN_INDEX = 5;
    public static final int STATUS_TABLE_IP_ADDDRESS_COLUMN_INDEX = 6;
    public static final int STATUS_TABLE_NODE_ID_COLUMN_INDEX = 7;

    private static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static final ResourceBundle resapplication = ResourceBundle.getBundle("com.l7tech.console.resources.console");
    private static final ResourceBundle dsnDialogResources = ResourceBundle.getBundle("com.l7tech.console.resources.DeleteStaleNodeDialog");
    private static final String CLUSTER_INSTALL = "cluster install";

    private static final Icon UP_ARROW_ICON = new ArrowIcon(ArrowIcon.UP);
    private static final Icon DOWN_ARROW_ICON = new ArrowIcon(ArrowIcon.DOWN);

    private static final Logger logger = Logger.getLogger(ClusterStatusPanel.class.getName());

    private static final SimpleDateFormat COUNTER_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    private static final SimpleDateFormat UPDATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** Name of bound property for changes in cluster status. */
    public static final String CLUSTER_STATUS_CHANGE_PROPERTY = "clusterStatus";

    private JFrame parentFrame;
    private JMenuItem nodeLogViewMenuItem;
    private JMenuItem nodeDeleteMenuItem;
    private JMenuItem nodeRenameMenuItem;
    private ClusterStatusTableSorter clusterStatusTableSorter;
    private Timer statusRefreshTimer;
    private ClusterStatusAdmin clusterStatusAdmin;
    private ServiceAdmin serviceManager;
    private Hashtable<String, GatewayStatus> currentNodeList;
    private Vector<Long> clusterRequestCounterCache;
    private Vector<GatewayLogWindow> logWindows = new Vector<GatewayLogWindow>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * @param parent    the parent JFrame containing this panel
     */
    public ClusterStatusPanel(JFrame parent) {
        parentFrame = parent;

        initClusterStatusTable();

        serviceNameFilterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent evt) {
                applyServiceNameFilter();
            }
            public void removeUpdate(DocumentEvent e) {
                applyServiceNameFilter();
            }
            public void changedUpdate(DocumentEvent e) {
                applyServiceNameFilter();
            }
        });

        clearServiceNameFilterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                serviceNameFilterField.setText("");
            }
        });

        countingSinceField.setText(CLUSTER_INSTALL);

        sinceNowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Creates a worker thread to retrieve a snapshot of the Service statistics.
                final ClusterStatusWorker statsWorker = new ClusterStatusWorker(serviceManager, clusterStatusAdmin, currentNodeList, cancelled) {
                    public void finished() {
                        statisticsPanel.setCounterStart(getStatisticsList());
                        countingSinceField.setText(COUNTER_TIME_FORMAT.format(getCurrentClusterSystemTime()));
                    }
                };

                statsWorker.start();
            }
        });

        sinceStartupButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Creates a worker thread to retrieve a snapshot of the Service statistics.
                final ClusterStatusWorker statsWorker = new ClusterStatusWorker(serviceManager, clusterStatusAdmin, currentNodeList, cancelled) {
                    public void finished() {
                        statisticsPanel.clearCounterStart();
                        // For a snappier UI experience, instead of waiting for the next timer
                        // round to refresh the display, we manually invoke the update.
                        statisticsPanel.updateStatisticsTable(getStatisticsList());
                        countingSinceField.setText(CLUSTER_INSTALL);
                    }
                };

                statsWorker.start();
            }
        });

        updateTimeStamp.setText(" ");

        initAdminConnection();
        initCaches();

        refreshStatus();
    }

    private void initClusterStatusTable() {
        clusterStatusTable.setModel(getClusterStatusTableModel());
        clusterStatusTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        BarIndicator loadShareRenderer = new BarIndicator(MIN, MAX, Color.blue);
        loadShareRenderer.setStringPainted(true);
        loadShareRenderer.setBackground(clusterStatusTable.getBackground());

        BarIndicator requestRoutedRenderer = new BarIndicator(MIN, MAX, Color.magenta);
        requestRoutedRenderer.setStringPainted(true);
        requestRoutedRenderer.setBackground(clusterStatusTable.getBackground());

        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_LOAD_SHARING_COLUMN_INDEX).setCellRenderer(loadShareRenderer);
        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_REQUEST_ROUTED_COLUMN_INDEX).setCellRenderer(requestRoutedRenderer);

        // don't show the following node status and id columns
        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_NODE_STATUS_COLUMN_INDEX).setMinWidth(0);
        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_NODE_STATUS_COLUMN_INDEX).setMaxWidth(0);
        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_NODE_STATUS_COLUMN_INDEX).setPreferredWidth(0);
        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_NODE_ID_COLUMN_INDEX).setMinWidth(0);
        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_NODE_ID_COLUMN_INDEX).setMaxWidth(0);
        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_NODE_ID_COLUMN_INDEX).setPreferredWidth(0);

        ColumnHeaderTooltips htt = new ColumnHeaderTooltips();
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_NODE_NAME_COLUMN_INDEX),
                "Name of the SecureSpan Gateway");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_LOAD_SHARING_COLUMN_INDEX),
                "% of load calculated with data collected in the past 60 seconds, updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_REQUEST_ROUTED_COLUMN_INDEX),
                "% of routed requests calculated with data collected in the past 60 seconds, updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_IP_ADDDRESS_COLUMN_INDEX),
                "IP address of the SecureSpan Gateway");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_LOAD_AVERAGE_COLUMN_INDEX),
                "1 minute load average, updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_SERVER_UPTIME_COLUMN_INDEX),
                "Duration of the SecureSpan Gateway uptime");
        clusterStatusTable.getTableHeader().addMouseMotionListener(htt);

        clusterStatusTable.addMouseListener(new PopUpMouseListener() {
            protected void popUpMenuHandler(MouseEvent mouseEvent) {
                JPopupMenu menu = new JPopupMenu();

                final int rowAtPoint = clusterStatusTable.rowAtPoint(mouseEvent.getPoint());
                boolean canDelete = false;
                if (rowAtPoint >= 0) {
                    String nodeId = (String) clusterStatusTable.getValueAt(rowAtPoint, STATUS_TABLE_NODE_ID_COLUMN_INDEX);
                    boolean logsBeingDisplayed = displayingLogsForNode(nodeId);
                    Object o = clusterStatusTable.getValueAt(rowAtPoint, STATUS_TABLE_NODE_STATUS_COLUMN_INDEX);
                    if (o instanceof Integer) {
                        Integer nodeStatus = (Integer) o;
                        if (nodeStatus == 0) {
                            canDelete = true;
                        }
                    }

                    List<BaseAction> actions = new ArrayList<BaseAction>();
                    actions.add(new ViewLogsAction(rowAtPoint, !logsBeingDisplayed));
                    SecureAction sa = new DeleteNodeEntryAction(rowAtPoint, canDelete && !logsBeingDisplayed);
                    if (sa.isAuthorized()) {
                        actions.add(sa);
                    }
                    sa = new RenameNodeAction(rowAtPoint);
                    if (sa.isAuthorized()) {
                        actions.add(sa);
                    }
                    if (actions.isEmpty()) {
                        return;
                    }
                    for (Iterator iterator = actions.iterator(); iterator.hasNext();)
                    {
                        Action action = (Action) iterator.next();
                        menu.add(action);
                    }
                    Utilities.removeToolTipsFromMenuItems(menu);
                    menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });

        clusterStatusTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int row = clusterStatusTable.getSelectedRow();
                boolean canDelete = false;
                if (row >= 0 && Registry.getDefault().isAdminContextPresent()) {
                    String nodeId = (String) clusterStatusTable.getValueAt(row, STATUS_TABLE_NODE_ID_COLUMN_INDEX);
                    boolean logsBeingDisplayed = displayingLogsForNode(nodeId);

                    getNodeLogViewMenuItem().setEnabled(!logsBeingDisplayed);

                    Object o = clusterStatusTable.getValueAt(row, STATUS_TABLE_NODE_STATUS_COLUMN_INDEX);
                    if (o instanceof Integer) {
                        Integer nodeStatus = (Integer) o;
                        canDelete = (nodeStatus == 0);
                    }
                    SecureAction sa1 = new DeleteNodeEntryAction(row, canDelete);
                    getNodeDeleteMenuItem().setEnabled(sa1.isAuthorized() && canDelete && !logsBeingDisplayed);

                    SecureAction sa2 = new RenameNodeAction(row);
                    getNodeRenameMenuItem().setEnabled(sa2.isAuthorized());
                } else {
                    getNodeLogViewMenuItem().setEnabled(false);
                    getNodeDeleteMenuItem().setEnabled(false);
                    getNodeRenameMenuItem().setEnabled(false);
                }
            }
        });

        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_NODE_NAME_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            private Icon connectIcon =
                    new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/connect2.gif"));
            private Icon disconnectIcon =
                    new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/disconnect.gif"));
            private Icon unknownStatusIcon =
                    new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/unknownstatus.gif"));

            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setIcon(null);

                Object s = clusterStatusTable.getValueAt(row, 0);

                if (s.toString().equals("1")) {
                    this.setIcon(connectIcon);
                } else if (s.toString().equals("0")) {
                    this.setIcon(disconnectIcon);
                } else {
                    this.setIcon(unknownStatusIcon);
                }
                return this;
            }
        });

        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_SERVER_UPTIME_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                this.setHorizontalAlignment(SwingConstants.TRAILING);
                if (value instanceof Long) {
                    this.setText(convertUptimeToString((Long) value));
                }

                return this;
            }
        });

        for (int i = 0; i < clusterStatusTable.getColumnModel().getColumnCount(); i++)
        {
            clusterStatusTable.getColumnModel().getColumn(i).setHeaderRenderer(iconHeaderRenderer);
        }

        addMouseListenerToHeaderInTable(clusterStatusTable);
        clusterStatusTable.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Return nodeLogViewMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getNodeLogViewMenuItem() {
        if (nodeLogViewMenuItem != null) return nodeLogViewMenuItem;

        nodeLogViewMenuItem = buildMenuItem("Node_ViewLogsMenuItem_text_name", 'L', null,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    new ViewLogsAction(-1, true).performAction();
                }
        });
        nodeLogViewMenuItem.setEnabled(false);

        return nodeLogViewMenuItem;
    }

    /**
     * Return nodeDeleteMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getNodeDeleteMenuItem() {
        if(nodeDeleteMenuItem != null) return nodeDeleteMenuItem;

        nodeDeleteMenuItem = buildMenuItem("Node_DeleteMenuItem_text_name", 'D', null,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    new DeleteNodeEntryAction(-1, true).performAction();
                }
        });
        nodeDeleteMenuItem.setEnabled(false);

        return nodeDeleteMenuItem;
    }

    /**
     * Return nodeRenameMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getNodeRenameMenuItem() {
        if(nodeRenameMenuItem != null) return nodeRenameMenuItem;

        nodeRenameMenuItem = buildMenuItem("Node_RenameMenuItem_text_name", 'R', null,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    new RenameNodeAction(-1).performAction();
                }
        });
        nodeRenameMenuItem.setEnabled(false);

        return nodeRenameMenuItem;
    }

    private JMenuItem buildMenuItem(String resKey, char mnemonic, KeyStroke accelKeyStroke, ActionListener actionListener) {
        JMenuItem jMenuItem = new JMenuItem();
        jMenuItem.setText(resapplication.getString(resKey));
        if(mnemonic==0) mnemonic = jMenuItem.getText().toCharArray()[0];
        if(mnemonic>0) jMenuItem.setMnemonic(mnemonic);
        if(accelKeyStroke!=null) jMenuItem.setAccelerator(accelKeyStroke);
        else jMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, KeyEvent.ALT_MASK));
        jMenuItem.addActionListener(actionListener);

        return jMenuItem;
    }

    /**
     * Return clusterStatusTableSorter property value
     *
     * @return ClusterStatusTableSorter
     */
    private ClusterStatusTableSorter getClusterStatusTableModel() {

        if (clusterStatusTableSorter != null) {
            return clusterStatusTableSorter;
        }

        Object[][] rows = new Object[][]{};

        String[] cols = new String[]{
            "Status", "Gateway Node", "Load Sharing %", "Request Routed %", "Load Avg", "Uptime", "IP Address", "Node Id"
        };

        clusterStatusTableSorter = new ClusterStatusTableSorter(new DefaultTableModel(rows, cols) {
            public boolean isCellEditable(int row, int col) {
                // the table cells are not editable
                return false;
            }
        });

        return clusterStatusTableSorter;

    }

    /**
     * Initialize the object references of the remote services
     */
    private void initAdminConnection() {
        serviceManager = Registry.getDefault().getServiceManager();
        clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
    }

    /**
     * Add a mouse listener to the Table to trigger a table sort
     * when a column heading is clicked in the JTable.
     */
    public void addMouseListenerToHeaderInTable(JTable table) {

        final JTable tableView = table;
        tableView.setColumnSelectionAllowed(false);
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {

                    ((ClusterStatusTableSorter)tableView.getModel()).sortData(column, true);
                    ((ClusterStatusTableSorter)tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    /**
     * Convert the server uptime from milliseconds to String
     *
     * @param uptime Server uptime in milliseconds
     * @return String  The string representation of the uptime
     */
    private String convertUptimeToString(long uptime) {

        if (uptime == 0) return "0 mins";

        long uptime_ms = (/*System.currentTimeMillis() - */uptime) / 1000;

        long serverUpTimeMinutues = uptime_ms / 60;
        long minutes_remain = serverUpTimeMinutues % 60;
        long hrs_total = serverUpTimeMinutues / 60;
        long hrs_remain = hrs_total % 24;
        long days = hrs_total / 24;
        String uptimeString = "";


        // show days only if days > 0
        if (days > 0) {
            uptimeString += Long.toString(days) + " days ";
        }

        // show hrs only if hrs_remain > 0
        if (hrs_remain > 0 || (days > 0 && hrs_remain == 0)) {
            uptimeString += Long.toString(hrs_remain) + " hrs ";
        }

        uptimeString += Long.toString(minutes_remain) + " mins ";

        return uptimeString;
    }

    /**
     * Prepare the cluster status data for displaying on the cluster status window
     *
     * @return Vector  The list of node status of every gateways in the cluster.
     */
    private Vector<GatewayStatus> prepareClusterStatusData() {

        Vector<GatewayStatus> cs = new Vector<GatewayStatus>();

        if (currentNodeList == null) return cs;

        for (GatewayStatus su : currentNodeList.values()) {
            if (su.getLastUpdateTimeStamp() == su.getSecondLastUpdateTimeStamp()) {
                su.incrementTimeStampUpdateFailureCount();
            }
            // the second last update time stamp is -1 the very first time when the node status is retrieved
            // the node status is unknown in this case
            if (su.getSecondLastUpdateTimeStamp() == -1) {
                su.setStatus(GatewayStatus.NODE_STATUS_UNKNOWN);
            } else if (su.getLastUpdateTimeStamp() != su.getSecondLastUpdateTimeStamp()) {
                su.setStatus(GatewayStatus.NODE_STATUS_ACTIVE);
                su.resetTimeStampUpdateFailureCount();
            } else {
                if (su.getTimeStampUpdateFailureCount() >= GatewayStatus.MAX_UPDATE_FAILURE_COUNT) {
                    su.setStatus(GatewayStatus.NODE_STATUS_INACTIVE);
                    //su.resetTimeStampUpdateFailureCount();
                } else {
                    // has not hit the limit, the node status does not change
                    su.setStatus(su.getLastState());
                }
            }

            if (getClusterRequestCount() != 0) {
                su.setLoadSharing((new Long(su.getRequestCount() * 100 / getClusterRequestCount())).intValue());
            } else {
                su.setLoadSharing(0);
            }
            cs.add(su);
        }

        return cs;
    }

    /**
     * Update the total request count for the cluster
     *
     * @param newCount The new value of the total request count
     */
    private void updateClusterRequestCounterCache(long newCount) {

        if (clusterRequestCounterCache.size() <= GatewayStatus.NUMBER_OF_SAMPLE_PER_MINUTE) {
            clusterRequestCounterCache.add(newCount);
        } else {
            clusterRequestCounterCache.remove(0);
            clusterRequestCounterCache.add(newCount);
        }
    }

    /**
     * Return the total request count of the cluster
     *
     * @return long  The total request count.
     */
    private long getClusterRequestCount() {

        long totalCount = 0;

        int index = clusterRequestCounterCache.size() - 1;

        for (int i = 0; i < clusterRequestCounterCache.size() - 1; i++, index--) {

            totalCount += clusterRequestCounterCache.get(index) - clusterRequestCounterCache.get(index - 1);
        }

        return totalCount;
    }

    /**
     * Create a refresh timer for retrieving the cluster status periodically.
     *
     * @return Timer  The refresh timer
     */
    private Timer getStatusRefreshTimer() {

        if (statusRefreshTimer != null) return statusRefreshTimer;

        // Create a refresh timer.
        statusRefreshTimer = new Timer(GatewayStatus.STATUS_REFRESH_TIMER, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                refreshStatus();
            }
        });

        return statusRefreshTimer;
    }

    /**
     * This function creates a worker thread to perform the status retrieval from the cluster
     */
    public void refreshStatus() {

        getStatusRefreshTimer().stop();

        // get the selected row index
        int selectedRowIndexOld = clusterStatusTable.getSelectedRow();
        final String nodeIdSelected;

        // save the number of selected message
        if (selectedRowIndexOld >= 0) {
            nodeIdSelected = (String) clusterStatusTable.getValueAt(selectedRowIndexOld, STATUS_TABLE_NODE_NAME_COLUMN_INDEX);
        } else {
            nodeIdSelected = null;
        }

        // create a worker thread to retrieve the Service statistics
        final ClusterStatusWorker statsWorker = new ClusterStatusWorker(serviceManager, clusterStatusAdmin, currentNodeList, cancelled) {
            public void finished() {

                if (isCanceled()) {
                    logger.info("Cluster status retrieval is canceled.");
                    getStatusRefreshTimer().stop();
                } else {
                    // Note: the get() operation is a blocking operation.
                    if (this.get() != null) {
                        //updateServerMetricsFields(getMetrics());
                        statisticsPanel.updateStatisticsTable(this.getStatisticsList());

                        currentNodeList = getNewNodeList();
                        updateClusterRequestCounterCache(this.getClusterRequestCount());

                        Vector<GatewayStatus> cs = prepareClusterStatusData();

                        getClusterStatusTableModel().setData(cs);
                        getClusterStatusTableModel().getRealModel().setRowCount(cs.size());
                        getClusterStatusTableModel().fireTableDataChanged();

                        setSelectedRow(nodeIdSelected);

                        firePropertyChange(CLUSTER_STATUS_CHANGE_PROPERTY, null, cs);

                        Calendar cal = Calendar.getInstance();
                        cal.setTime(getCurrentClusterSystemTime());
                        updateTimeStamp.setText("Last Updated: " + UPDATE_TIME_FORMAT.format(cal.getTime()) + " (Gateway time)   ");
                        getStatusRefreshTimer().start();
                    }
                }
            }
        };

        statsWorker.start();
    }

    /**
     * Clean up the resources of the cluster status window when the window is closed.
     */
    public void dispose() {
        synchronized(logWindows) {
            for (GatewayLogWindow window : logWindows) {
                try {
                    window.dispose();
                } catch (Exception ex) {
                }
            }
            logWindows.removeAllElements();
        }

        getStatusRefreshTimer().stop();
        cancelled.set(true);
    }

    /**
     * Initialize the resources when the connection to the cluster is established.
     */
    public void onLogon(LogonEvent e) {
        initAdminConnection();
        initCaches();
        getStatusRefreshTimer().start();
        cancelled.set(false);

        synchronized(logWindows) {
            for (GatewayLogWindow window : logWindows) {
                try {
                    window.onLogon(e);
                } catch (Exception ex) {
                }
            }
        }
    }


    /**
     * Clean up the resources when the connection to the cluster went down.
     */
    public void onLogoff(LogonEvent e) {
        cleanUp();

        synchronized(logWindows) {
            for (GatewayLogWindow window : logWindows) {
                try {
                    window.onLogoff(e);
                } catch (Exception ex) {
                }
            }
        }
    }

    private void cleanUp() {
        getStatusRefreshTimer().stop();
        if (!updateTimeStamp.getText().trim().endsWith("[Disconnected]")) {
            updateTimeStamp.setText(updateTimeStamp.getText().trim() + " [Disconnected]   ");
        }

        setNodeStatusUnknown();
        serviceManager = null;
        clusterStatusAdmin = null;
        cancelled.set(true);
    }


    /**
     * Return the flag indicating whether the job has been cancelled or not.
     *
     * @return true if the job is cancelled, false otherwise.
     */
    public boolean isCanceled() {
        return cancelled.get();
    }

    /**
     * Initialize the caches.
     */
    private void initCaches() {
        currentNodeList = new Hashtable<String, GatewayStatus>();
        clusterRequestCounterCache = new Vector<Long>();
    }

    /**
     * Set node status to unknown.
     */
    public void setNodeStatusUnknown() {
        Vector<GatewayStatus> cs = new Vector<GatewayStatus>();

        for (GatewayStatus su : currentNodeList.values()) {
            su.setStatus(GatewayStatus.NODE_STATUS_UNKNOWN);
            cs.add(su);
        }

        getClusterStatusTableModel().setData(cs);
        getClusterStatusTableModel().getRealModel().setRowCount(cs.size());
        getClusterStatusTableModel().fireTableDataChanged();
    }

    /**
     * Set the row of the node status table which is currenlty selected by the user. This function
     * is called after an update on the table is done.
     *
     * @param nodeId The node Id of the row being selected.
     */
    public void setSelectedRow(String nodeId) {
        if (nodeId != null) {
            // keep the current row selection
            int rowCount = clusterStatusTable.getRowCount();

            for (int i = 0; i < rowCount; i++) {
                if (clusterStatusTable.getValueAt(i, STATUS_TABLE_NODE_NAME_COLUMN_INDEX).equals(nodeId)) {
                    clusterStatusTable.setRowSelectionInterval(i, i);

                    break;
                }
            }
        }
    }

    private boolean displayingLogsForNode(String nodeId) {
        boolean displayed = false;

        synchronized(logWindows) {
            for (GatewayLogWindow window : logWindows) {
                if (nodeId.equals(window.getNodeId())) {
                    displayed = true;
                }
            }
        }

        return displayed;
    }

    private void applyServiceNameFilter() {
        try {
            statisticsPanel.setServiceFilter(serviceNameFilterField.getText());
            serviceNameFilterField.setBackground((Color)UIManager.get("TextField.background"));
        } catch (PatternSyntaxException e) {
            serviceNameFilterField.setBackground(Color.YELLOW);
        }
    }

    /**
     * This customized renderer can render objects of the type TextandIcon
     */
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

            setText((String)value);

            if (getClusterStatusTableModel().getSortedColumn() == column) {

                if (getClusterStatusTableModel().isAscending()) {
                    setIcon(UP_ARROW_ICON);
                } else {
                    setIcon(DOWN_ARROW_ICON);
                }
            } else {
                setIcon(null);
            }

            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    };

    private class DeleteNodeEntryAction extends SecureAction {
        private final int tableRow;

        public DeleteNodeEntryAction(int row, boolean buttonEnabled) {
            super(null);
            setEnabled(buttonEnabled);
            tableRow = row;
        }

        /**
         * @return the aciton description
         */
        public String getDescription() {
            return "Delete the node entry in the database";
        }

        /**
         * @return the action name
         */
        public String getName() {
            return "Delete Node";
        }

        /**
         * subclasses override this method specifying the resource name
         */
        protected String iconResource() {
            return RESOURCE_PATH + "/delete.gif";
        }

        /**
         * Invoked when an action occurs.
         */
        public void performAction() {

            // get the selected row index
            int selectedRowIndex = tableRow;
            if(selectedRowIndex < 0) {
                selectedRowIndex = clusterStatusTable.getSelectedRow(); // when called from node menu
            }
            final int selectedRowIndexOld = selectedRowIndex;

            // save the number of selected message
            if (selectedRowIndexOld >= 0) {
                final String nodeNameSelected = (String) clusterStatusTable.getValueAt(selectedRowIndexOld, STATUS_TABLE_NODE_NAME_COLUMN_INDEX);

                // ask user to confirm                 // Make sure
                DialogDisplayer.showConfirmDialog(parentFrame,
                  "Are you sure you want to delete " +
                  nodeNameSelected + "?",
                  "Delete Stale Node",
                  JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {

                                    if (clusterStatusAdmin == null) {
                                        logger.warning("ClusterStatusAdmin service is not available. Cannot delete the node: " + nodeNameSelected);

                                        JOptionPane.
                                          showMessageDialog(parentFrame,
                                            dsnDialogResources.getString("delete.stale.node.error.connection.lost"),
                                            dsnDialogResources.getString("delete.stale.node.error.title"),
                                            JOptionPane.ERROR_MESSAGE);
                                        return;
                                    }

                                    try {
                                        clusterStatusAdmin.removeStaleNode((String) clusterStatusTable.getValueAt(selectedRowIndexOld, STATUS_TABLE_NODE_ID_COLUMN_INDEX));

                                    } catch (DeleteException e) {
                                        logger.warning("Cannot delete the node: " + nodeNameSelected);
                                        JOptionPane.
                                          showMessageDialog(parentFrame,
                                            dsnDialogResources.getString("delete.stale.node.error.delete"),
                                            dsnDialogResources.getString("delete.stale.node.error.title"),
                                            JOptionPane.ERROR_MESSAGE);

                                    }
                                }
                            });

                        }

                    }
                });
            }
        }
    }

    private class RenameNodeAction extends SecureAction {
        private final int tableRow;

        public RenameNodeAction(int row) {
            super(null);
            tableRow = row;
        }

        /**
         * @return the aciton description
         */
        public String getDescription() {
            return "Change the node name";  //To change body of implemented methods use File | Settings | File Templates.
        }

        /**
         * @return the action name
         */
        public String getName() {
            return "Rename Node";
        }

        /**
         * subclasses override this method specifying the resource name
         */
        protected String iconResource() {
            return RESOURCE_PATH + "/Edit16.gif";
        }


        @Override
        public boolean isAuthorized() {
            for (Permission perm : getSecurityProvider().getUserPermissions()) {
                if (perm.getEntityType() == EntityType.ANY || perm.getEntityType() == EntityType.CLUSTER_INFO) {
                    if (perm.getOperation() == OperationType.UPDATE) {
                        if (perm.getScope().isEmpty()) return true;
                    }
                }
            }
            return false;
        }

        /**
         * Invoked when an action occurs.
         */
        public void performAction() {
            // get the selected row index
            int selectedRow = tableRow;
            if(selectedRow < 0) {
                selectedRow = clusterStatusTable.getSelectedRow(); // when called from node menu
            }

            final String nodeName;
            final String nodeId;

            // save the number of selected message
            if (selectedRow >= 0) {
                nodeName = (String) clusterStatusTable.getValueAt(selectedRow, STATUS_TABLE_NODE_NAME_COLUMN_INDEX);
                nodeId = (String) clusterStatusTable.getValueAt(selectedRow, STATUS_TABLE_NODE_ID_COLUMN_INDEX);

                EditGatewayNameDialog dialog = new EditGatewayNameDialog(parentFrame, clusterStatusAdmin, nodeId, nodeName);                dialog.setVisible(true);
            }
        }
    }

    private class ViewLogsAction extends BaseAction {
        private final int tableRow;

        public ViewLogsAction(int row, boolean buttonEnabled) {
            setEnabled(buttonEnabled);
            tableRow = row;
        }

        public String getName() {
            return "View Log";
        }

        protected String iconResource() {
            return RESOURCE_PATH + "/AnalyzeGatewayLog16x16.gif";
        }

        protected void performAction() {
            // get the selected row index
            int selectedRow = tableRow;
            if(selectedRow < 0) {
                selectedRow = clusterStatusTable.getSelectedRow(); // when called from node menu
            }

            // save the number of selected message
            if (selectedRow >= 0) {
                final String nodeName = (String) clusterStatusTable.getValueAt(selectedRow, STATUS_TABLE_NODE_NAME_COLUMN_INDEX);
                final String nodeId = (String) clusterStatusTable.getValueAt(selectedRow, STATUS_TABLE_NODE_ID_COLUMN_INDEX);

                final GatewayLogWindow window = new GatewayLogWindow(nodeName, nodeId);
                window.pack();

                window.addWindowListener(new WindowAdapter() {
                    boolean disposed = false;

                    public void windowClosing(final WindowEvent e) {
                        if (!disposed) {
                            logWindows.remove(window);
                            window.dispose();
                        }
                        disposed = true;

                    }

                    public void windowClosed(final WindowEvent e) {
                        if (!disposed) {
                            logWindows.remove(window);
                            window.dispose();
                        }
                        disposed = true;
                    }
                });

                logWindows.add(window);
                window.setVisible(true);
            }
        }
    }
}
