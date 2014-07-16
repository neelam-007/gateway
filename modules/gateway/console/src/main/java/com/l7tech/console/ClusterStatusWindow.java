/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.SheetHolder;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.panels.EditGatewayNameDialog;
import com.l7tech.console.panels.StatisticsPanel;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.table.ClusterStatusTableSorter;
import com.l7tech.console.util.*;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.service.ServiceAdmin;

import javax.swing.*;
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

/**
 * This class is a window extended from JFrame to show the cluster status.
 *
 * @deprecated Replaced by {@link com.l7tech.console.panels.dashboard.ClusterStatusPanel} since SecureSpan 4.2.
 */
@Deprecated
public class ClusterStatusWindow extends JFrame implements LogonListener, SheetHolder {

    public static final int STATUS_TABLE_NODE_STATUS_COLUMN_INDEX = 0;
    public static final int STATUS_TABLE_NODE_NAME_COLUMN_INDEX = 1;
    public static final int STATUS_TABLE_LOAD_SHARING_COLUMN_INDEX = 2;
    public static final int STATUS_TABLE_REQUEST_ROUTED_COLUMN_INDEX = 3;
    public static final int STATUS_TABLE_LOAD_AVERAGE_COLUMN_INDEX = 4;
    public static final int STATUS_TABLE_SERVER_UPTIME_COLUMN_INDEX = 5;
    public static final int STATUS_TABLE_IP_ADDDRESS_COLUMN_INDEX = 6;
    public static final int STATUS_TABLE_NODE_ID_COLUMN_INDEX = 7;
    private javax.swing.JLabel serviceStatTitle = null;
    private javax.swing.JLabel clusterStatusTitle = null;
    private javax.swing.JLabel updateTimeStamp = null;
    private javax.swing.JPanel messagePane = null;
    private javax.swing.JPanel frameContentPane = null;
    private javax.swing.JPanel mainPane = null;
    private javax.swing.JPanel serviceStatPane = null;
    private javax.swing.JPanel clusterStatusPane = null;
    private javax.swing.JSplitPane mainSplitPane = null;
    private javax.swing.JScrollPane clusterStatusScrollPane = null;
    private javax.swing.JTable clusterStatusTable = null;
    private javax.swing.JMenuBar clusterWindowMenuBar = null;
    private javax.swing.JMenu fileMenu = null;
    private javax.swing.JMenu nodeMenu = null;
    private javax.swing.JMenu helpMenu = null;
    private javax.swing.JMenuItem exitMenuItem = null;
    private javax.swing.JMenuItem nodeLogViewMenuItem = null;
    private javax.swing.JMenuItem nodeDeleteMenuItem = null;
    private javax.swing.JMenuItem nodeRenameMenuItem = null;
    private javax.swing.JMenuItem helpTopicsMenuItem = null;
    private ClusterStatusTableSorter clusterStatusTableSorter = null;
    private StatisticsPanel statisticsPane = null;
    private javax.swing.Timer statusRefreshTimer = null;
    private ClusterStatusAdmin clusterStatusAdmin = null;
    private ServiceAdmin serviceManager = null;
    private Hashtable currentNodeList = null;
    private Vector clusterRequestCounterCache = null;

    private static final int MAX = 100;
    private static final int MIN = 0;
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static ResourceBundle resapplication = java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");
    static Logger logger = Logger.getLogger(ClusterStatusWindow.class.getName());
    private final ClassLoader cl = ClusterStatusWindow.class.getClassLoader();
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Resource bundle with default locale
     */
    private ResourceBundle dsnDialogResources = null;

    /**
     * Constructor
     *
     * @param title The window title
     */
    public ClusterStatusWindow(final String title) {
        super(title);
        ImageIcon imageIcon = new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/CA_Logo_Black_16x16.png"));
        setIconImage(imageIcon.getImage());
        setBounds(0, 0, 850, 600);
        setJMenuBar(getClusterWindowMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);
        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initResources();
        initAdminConnection();
        initCaches();

        // refresh the status
        refreshStatus();

        pack();
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        dsnDialogResources = ResourceBundle.getBundle("com.l7tech.console.resources.DeleteStaleNodeDialog", locale);
    }

    /**
     * Clean up the resources of the winodw when the user exits the window
     */
    private void exitMenuEventHandler() {
        getStatusRefreshTimer().stop();
        this.dispose();
    }

    /**
     * Return the frameContentPane property value
     *
     * @return JPanel
     */
    private JPanel getJFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setPreferredSize(new Dimension(800, 600));
            frameContentPane.setLayout(new BorderLayout());
            getJFrameContentPane().add(getMainPane(), "Center");
        }
        return frameContentPane;
    }

    /**
     * Return the clusterWindowMenuBar property value
     *
     * @return JMenubar
     */
    private JMenuBar getClusterWindowMenuBar() {
        if (clusterWindowMenuBar == null) {
            clusterWindowMenuBar = new JMenuBar();
            clusterWindowMenuBar.add(getFileMenu());
            clusterWindowMenuBar.add(getNodeMenu());
            clusterWindowMenuBar.add(getHelpMenu());
        }
        return clusterWindowMenuBar;
    }

    /**
     * Return fileMenu property value
     *
     * @return JMenu
     */
    private JMenu getFileMenu() {
        if (fileMenu == null) {
            fileMenu = new JMenu();
            fileMenu.setText(resapplication.getString("File"));
            fileMenu.add(getExitMenuItem());
            int mnemonic = fileMenu.getText().toCharArray()[0];
            fileMenu.setMnemonic(mnemonic);
        }
        return fileMenu;
    }

    /**
     * Return nodeMenu propery value
     *
     * @return JMenu
     */
    private JMenu getNodeMenu() {
        if (nodeMenu != null) return nodeMenu;

        nodeMenu = new JMenu();
        nodeMenu.setText(resapplication.getString("Node"));
        nodeMenu.add(getNodeDeleteMenuItem());
        nodeMenu.add(getNodeRenameMenuItem());
        int mnemonic = nodeMenu.getText().toCharArray()[0];
        nodeMenu.setMnemonic(mnemonic);

        return nodeMenu;
    }

    /**
     * Return helpMenu propery value
     *
     * @return JMenu
     */
    private JMenu getHelpMenu() {
        if (helpMenu != null) return helpMenu;

        helpMenu = new JMenu();
        helpMenu.setText(resapplication.getString("Help"));
        helpMenu.add(getHelpTopicsMenuItem());
        int mnemonic = helpMenu.getText().toCharArray()[0];
        helpMenu.setMnemonic(mnemonic);

        return helpMenu;
    }

    /**
     * Return mainPane property value
     *
     * @return JPanel
     */
    private JPanel getMainPane() {
        if (mainPane != null) return mainPane;

        mainPane = new javax.swing.JPanel();
        mainPane.setLayout(new BorderLayout());
        mainSplitPane = new javax.swing.JSplitPane();

        clusterStatusScrollPane = new javax.swing.JScrollPane();
        clusterStatusPane = new javax.swing.JPanel();
        clusterStatusTitle = new javax.swing.JLabel();

        serviceStatPane = new javax.swing.JPanel();
        serviceStatTitle = new javax.swing.JLabel();

        mainSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.3);
        clusterStatusPane.setLayout(new java.awt.BorderLayout());

        clusterStatusScrollPane.setMinimumSize(new java.awt.Dimension(400, 120));
        clusterStatusScrollPane.setPreferredSize(new java.awt.Dimension(400, 180));
        clusterStatusScrollPane.setViewportView(getClusterStatusTable());
        clusterStatusScrollPane.getViewport().setBackground(getClusterStatusTable().getBackground());

        getClusterStatusTable().addMouseListener(new PopUpMouseListener() {
            @Override
            protected void popUpMenuHandler(MouseEvent mouseEvent) {
                JPopupMenu menu = new JPopupMenu();

                final int rowAtPoint = getClusterStatusTable().rowAtPoint(mouseEvent.getPoint());
                boolean canDelete = false;
                if (rowAtPoint >= 0) {
                    String nodeId = (String)getClusterStatusTable().getValueAt(rowAtPoint, STATUS_TABLE_NODE_ID_COLUMN_INDEX);
                    Object o = getClusterStatusTable().getValueAt(rowAtPoint, STATUS_TABLE_NODE_STATUS_COLUMN_INDEX);
                    if (o instanceof Integer) {
                        Integer nodeStatus = (Integer)o;
                        if (nodeStatus.intValue() == 0) {
                            canDelete = true;
                        }
                    }

                    List actions = new ArrayList();
                    SecureAction sa = new DeleteNodeEntryAction(rowAtPoint, canDelete );
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
                    for (Iterator iterator = actions.iterator(); iterator.hasNext();) {
                        Action action = (Action)iterator.next();
                        menu.add(action);
                    }
                    Utilities.removeToolTipsFromMenuItems(menu);
                    menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        getClusterStatusTable().getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = getClusterStatusTable().getSelectedRow();
                boolean canDelete = false;
                if(row>=0) {
                    Object o = getClusterStatusTable().getValueAt(row, STATUS_TABLE_NODE_STATUS_COLUMN_INDEX);
                    if (o instanceof Integer) {
                        Integer nodeStatus = (Integer) o;
                        canDelete = (nodeStatus.intValue() == 0);
                    }
                    SecureAction sa1 = new DeleteNodeEntryAction(row, canDelete);
                    getNodeDeleteMenuItem().setEnabled(sa1.isAuthorized() && canDelete );

                    SecureAction sa2 = new RenameNodeAction(row);
                    getNodeRenameMenuItem().setEnabled(sa2.isAuthorized());
                }
                else {
                    getNodeDeleteMenuItem().setEnabled(false);
                    getNodeRenameMenuItem().setEnabled(false);
                }
            }
        });

        clusterStatusPane.add(clusterStatusScrollPane, java.awt.BorderLayout.CENTER);

        clusterStatusTitle.setFont(new java.awt.Font("Dialog", 1, 18));
        clusterStatusTitle.setText("Gateway Status");
        clusterStatusTitle.setMaximumSize(new java.awt.Dimension(136, 40));
        clusterStatusTitle.setMinimumSize(new java.awt.Dimension(136, 40));
        clusterStatusTitle.setPreferredSize(new java.awt.Dimension(136, 40));
        clusterStatusPane.add(clusterStatusTitle, java.awt.BorderLayout.NORTH);

        mainSplitPane.setTopComponent(clusterStatusPane);

        serviceStatPane.setLayout(new java.awt.BorderLayout());

        serviceStatTitle.setFont(new java.awt.Font("Dialog", 1, 18));
        serviceStatTitle.setText(" Service Statistics");
        serviceStatTitle.setMaximumSize(new java.awt.Dimension(136, 40));
        serviceStatTitle.setMinimumSize(new java.awt.Dimension(136, 40));
        serviceStatTitle.setPreferredSize(new java.awt.Dimension(136, 40));
        serviceStatPane.add(serviceStatTitle, java.awt.BorderLayout.NORTH);

        serviceStatPane.add(getStatisticsPane(), java.awt.BorderLayout.CENTER);
        mainSplitPane.setBottomComponent(serviceStatPane);

        mainPane.add(mainSplitPane, java.awt.BorderLayout.CENTER);
        mainPane.add(getMessagePane(), java.awt.BorderLayout.SOUTH);

        return mainPane;
    }

    /**
     * Return messagePane property value
     *
     * @return JPanel
     */
    private JPanel getMessagePane() {

        if (messagePane != null) return messagePane;

        messagePane = new JPanel();


        messagePane.setLayout(new BorderLayout());
        messagePane.setPreferredSize(new java.awt.Dimension(136, 40));
        messagePane.setMinimumSize(new java.awt.Dimension(136, 40));
        messagePane.setMaximumSize(new java.awt.Dimension(136, 40));
        messagePane.add(getLastUpdateLabel(), java.awt.BorderLayout.EAST);

        return messagePane;
    }

    /**
     * Return updateTimeStamp property value
     *
     * @return JLabel
     */
    private JLabel getLastUpdateLabel() {
        if (updateTimeStamp != null) return updateTimeStamp;

        updateTimeStamp = new JLabel();
        updateTimeStamp.setFont(new java.awt.Font("Dialog", 0, 12));
        updateTimeStamp.setText("");

        return updateTimeStamp;
    }

    /**
     * Return clusterStatusTable property value
     *
     * @return JTable
     */
    private JTable getClusterStatusTable() {

        if (clusterStatusTable != null) return clusterStatusTable;

        clusterStatusTable = new javax.swing.JTable();
        clusterStatusTable.setModel(getClusterStatusTableModel());
        clusterStatusTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        BarIndicator loadShareRenderer = new BarIndicator(MIN, MAX, Color.blue);
        loadShareRenderer.setStringPainted(true);
        loadShareRenderer.setBackground(clusterStatusTable.getBackground());

        // set limit value and fill color
        Hashtable limitColors1 = new Hashtable();
        limitColors1.put(new Integer(0), Color.green);

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
          "Name of the Gateway. Updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_LOAD_SHARING_COLUMN_INDEX),
          "% of load calculated with data collected in the past 60 seconds. " +
          "Updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_REQUEST_ROUTED_COLUMN_INDEX),
          "% of routed requests calculated with data collected in the past 60 seconds. " +
          "Updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_IP_ADDDRESS_COLUMN_INDEX),
          "IP address of the Gateway. Updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_LOAD_AVERAGE_COLUMN_INDEX),
          "1 minute load average. Updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        htt.setToolTip(clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_SERVER_UPTIME_COLUMN_INDEX),
          "Duration of the Gateway uptime. Updated every " + GatewayStatus.REFRESH_INTERVAL + " seconds");
        clusterStatusTable.getTableHeader().addMouseMotionListener(htt);

        clusterStatusTable.getColumnModel().getColumn(STATUS_TABLE_NODE_NAME_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            private Icon connectIcon =
              new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/connect2.gif"));
            private Icon disconnectIcon =
              new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/disconnect.gif"));
            private Icon unknownStatusIcon =
              new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/unknownstatus.gif"));

            @Override
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

            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                this.setHorizontalAlignment(SwingConstants.TRAILING);
                if (value instanceof Long) {
                    this.setText(convertUptimeToString(((Long)value).longValue()));
                }

                return this;
            }
        });

        for (int i = 0; i < clusterStatusTable.getColumnModel().getColumnCount(); i++) {
            clusterStatusTable.getColumnModel().getColumn(i).setHeaderRenderer(iconHeaderRenderer);
        }

        addMouseListenerToHeaderInTable(clusterStatusTable);
        clusterStatusTable.getTableHeader().setReorderingAllowed(false);

        return clusterStatusTable;
    }

    /**
     * Return exitMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getExitMenuItem() {
        if (exitMenuItem != null) return exitMenuItem;

        exitMenuItem = buildMenuItem("ExitMenuItem.name",
                                     'X',
                                     null,
            new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitMenuEventHandler();
            }
        });

        return exitMenuItem;
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
                @Override
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
                @Override
                public void actionPerformed(ActionEvent e) {
                    new RenameNodeAction(-1).performAction();
                }
        });
        nodeRenameMenuItem.setEnabled(false);

        return nodeRenameMenuItem;
    }

    /**
     * Return helpTopicsMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getHelpTopicsMenuItem() {
        if (helpTopicsMenuItem != null) return helpTopicsMenuItem;

        helpTopicsMenuItem = buildMenuItem("Help_TopicsMenuItem_text_name",
                                          (char)0,
                                          KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    TopComponents.getInstance().showHelpTopics();
                }
        });

        return helpTopicsMenuItem;
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
     * Return statisticsPane property value
     *
     * @return StatisticsPanel
     */
    private StatisticsPanel getStatisticsPane() {
        if (statisticsPane != null) return statisticsPane;

        statisticsPane = new StatisticsPanel();
        return statisticsPane;
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
            @Override
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
            @Override
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

        if (uptime == 0) return new String("0 mins");

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
    private Vector prepareClusterStatusData() {

        Vector cs = new Vector();

        if (currentNodeList == null) return cs;

        for (Iterator i = currentNodeList.keySet().iterator(); i.hasNext();) {
            GatewayStatus su = (GatewayStatus)currentNodeList.get(i.next());

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
            clusterRequestCounterCache.add(new Long(newCount));
        } else {
            clusterRequestCounterCache.remove(0);
            clusterRequestCounterCache.add(new Long(newCount));
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

            totalCount += ((Long)clusterRequestCounterCache.get(index)).longValue() - ((Long)clusterRequestCounterCache.get(index - 1)).longValue();
        }

        return totalCount;
    }

    /**
     * Create a refresh timer for retrieving the cluster status periodically.
     *
     * @return Timer  The refresh timer
     */
    private javax.swing.Timer getStatusRefreshTimer() {

        if (statusRefreshTimer != null) return statusRefreshTimer;

        // Create a refresh timer.
        statusRefreshTimer = new javax.swing.Timer(GatewayStatus.STATUS_REFRESH_TIMER, new ActionListener() {
            @Override
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
        int selectedRowIndexOld = getClusterStatusTable().getSelectedRow();
        final String nodeIdSelected;

        // save the number of selected message
        if (selectedRowIndexOld >= 0) {
            nodeIdSelected = (String)getClusterStatusTable().getValueAt(selectedRowIndexOld, STATUS_TABLE_NODE_NAME_COLUMN_INDEX);
        } else {
            nodeIdSelected = null;
        }

        // create a worker thread to retrieve the Service statistics
        final ClusterStatusWorker statsWorker = new ClusterStatusWorker(serviceManager, clusterStatusAdmin, currentNodeList, cancelled) {
            @Override
            public void finished() {

                if (isCanceled()) {
                    logger.info("Cluster status retrieval is canceled.");
                    getStatusRefreshTimer().stop();
                } else {
                    // Note: the get() operation is a blocking operation.
                    if (this.get() != null) {
                        //updateServerMetricsFields(getMetrics());
                        statisticsPane.updateStatisticsTable(this.getStatisticsList());

                        currentNodeList = getNewNodeList();
                        updateClusterRequestCounterCache(this.getClusterRequestCount());

                        Vector cs = prepareClusterStatusData();

                        getClusterStatusTableModel().setData(cs);
                        getClusterStatusTableModel().getRealModel().setRowCount(cs.size());
                        getClusterStatusTableModel().fireTableDataChanged();

                        setSelectedRow(nodeIdSelected);

                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy hh:mm:ss aaa");
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(getCurrentClusterSystemTime());
                        getLastUpdateLabel().setText("Last Updated: " + sdf.format(cal.getTime()) + "   ");
                        getStatusRefreshTimer().start();
                    }
                }
            }
        };

        statsWorker.start();
    }

    @Override
    public void setVisible(boolean vis) {
        if (!vis) {
            // Let inactivity timeout start counting after this window is closed.
            TopComponents.getInstance().updateLastActivityTime();
        }
        super.setVisible( vis );
    }

    /**
     * Clean up the resources of the cluster status window when the window is closed.
     */
    @Override
    public void dispose() {
        getStatusRefreshTimer().stop();
        super.dispose();
    }

    /**
     * Initialize the resources when the connection to the cluster is established.
     */
    @Override
    public void onLogon(LogonEvent e) {
        initAdminConnection();
        initCaches();
        getStatusRefreshTimer().start();
        cancelled.set(false);
    }


    /**
     * Clean up the resources when the connection to the cluster went down.
     */
    @Override
    public void onLogoff(LogonEvent e) {
        cleanUp();
    }

    private void cleanUp() {
        getStatusRefreshTimer().stop();
        if (!getLastUpdateLabel().getText().trim().endsWith("[Disconnected]")) {
            getLastUpdateLabel().setText(getLastUpdateLabel().getText().trim() + " [Disconnected]   ");
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
        currentNodeList = new Hashtable();
        clusterRequestCounterCache = new Vector();
    }

    /**
     * Set node status to unknown.
     */
    public void setNodeStatusUnknown() {
        Vector cs = new Vector();

        for (Iterator i = currentNodeList.keySet().iterator(); i.hasNext();) {
            GatewayStatus su = (GatewayStatus)currentNodeList.get(i.next());
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
            int rowCount = getClusterStatusTable().getRowCount();

            for (int i = 0; i < rowCount; i++) {
                if (getClusterStatusTable().getValueAt(i, STATUS_TABLE_NODE_NAME_COLUMN_INDEX).equals(nodeId)) {
                    getClusterStatusTable().setRowSelectionInterval(i, i);

                    break;
                }
            }
        }
    }

    /**
     * This customized renderer can render objects of the type TextandIcon
     */
    TableCellRenderer iconHeaderRenderer = new DefaultTableCellRenderer() {
        @Override
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
                    setIcon(upArrowIcon);
                } else {
                    setIcon(downArrowIcon);
                }
            } else {
                setIcon(null);
            }

            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    };

    @Override
    public void showSheet(JInternalFrame sheet) {
        DialogDisplayer.showSheet(this, sheet);
    }

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
        @Override
        public String getDescription() {
            return "Delete the node entry in the database";
        }

        /**
         * @return the action name
         */
        @Override
        public String getName() {
            return "Delete Node";
        }

        /**
         * subclasses override this method specifying the resource name
         */
        @Override
        protected String iconResource() {
            return RESOURCE_PATH + "/delete.gif";
        }

        /**
         * Invoked when an action occurs.
         */
        @Override
        public void performAction() {

            // get the selected row index
            int selectedRowIndex = tableRow;
            if(selectedRowIndex < 0) {
                selectedRowIndex = getClusterStatusTable().getSelectedRow(); // when called from node menu
            }
            final int selectedRowIndexOld = selectedRowIndex;

            // save the number of selected message
            if (selectedRowIndexOld >= 0) {
                final String nodeNameSelected = (String)getClusterStatusTable().getValueAt(selectedRowIndexOld, STATUS_TABLE_NODE_NAME_COLUMN_INDEX);

                // ask user to confirm                 // Make sure
                DialogDisplayer.showConfirmDialog(ClusterStatusWindow.this,
                  "Are you sure you want to delete " +
                  nodeNameSelected + "?",
                  "Delete Stale Node",
                  JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {

                                    if (clusterStatusAdmin == null) {
                                        logger.warning("ClusterStatusAdmin service is not available. Cannot delete the node: " + nodeNameSelected);

                                        JOptionPane.
                                          showMessageDialog(ClusterStatusWindow.this,
                                            dsnDialogResources.getString("delete.stale.node.error.connection.lost"),
                                            dsnDialogResources.getString("delete.stale.node.error.title"),
                                            JOptionPane.ERROR_MESSAGE);
                                        return;
                                    }

                                    try {
                                        clusterStatusAdmin.removeStaleNode((String)getClusterStatusTable().getValueAt(selectedRowIndexOld, STATUS_TABLE_NODE_ID_COLUMN_INDEX));

                                    } catch (DeleteException e) {
                                        logger.warning("Cannot delete the node: " + nodeNameSelected);
                                        JOptionPane.
                                          showMessageDialog(ClusterStatusWindow.this,
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
        @Override
        public String getDescription() {
            return "Change the node name";  //To change body of implemented methods use File | Settings | File Templates.
        }

        /**
         * @return the action name
         */
        @Override
        public String getName() {
            return "Rename Node";
        }

        /**
         * subclasses override this method specifying the resource name
         */
        @Override
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
        @Override
        public void performAction() {
            // get the selected row index
            int selectedRow = tableRow;
            if(selectedRow < 0) {
                selectedRow = getClusterStatusTable().getSelectedRow(); // when called from node menu
            }

            final String nodeName;
            final String nodeId;

            // save the number of selected message
            if (selectedRow >= 0) {
                nodeName = (String)getClusterStatusTable().getValueAt(selectedRow, STATUS_TABLE_NODE_NAME_COLUMN_INDEX);
                nodeId = (String)getClusterStatusTable().getValueAt(selectedRow, STATUS_TABLE_NODE_ID_COLUMN_INDEX);

                EditGatewayNameDialog dialog = new EditGatewayNameDialog(ClusterStatusWindow.this, clusterStatusAdmin, nodeId, nodeName);
                dialog.setVisible(true);
            }
        }
    }
}

