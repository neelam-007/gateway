package com.l7tech.console;

import com.l7tech.console.util.*;
import com.l7tech.console.panels.StatisticsPanel;
import com.l7tech.console.table.ClusterStatusTableSorter;
import com.l7tech.console.table.LogTableModel;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.console.icons.ArrowIcon;
import com.l7tech.console.event.ConnectionListener;
import com.l7tech.console.event.ConnectionEvent;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.util.Locator;
import com.l7tech.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.table.*;
import java.util.ResourceBundle;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Iterator;
import java.awt.*;
import java.awt.event.*;


/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ClusterStatusWindow extends JFrame {

    private static final int MAX = 100;
    private static final int MIN = 0;
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static
            ResourceBundle resapplication =
            java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");
    //private final ClassLoader cl = getClass().getClassLoader();
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);

    public ClusterStatusWindow(final String title) {
        super(title);
        ImageIcon imageIcon =
                new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png"));
        setIconImage(imageIcon.getImage());
        setBounds(0, 0, 850, 600);
        setJMenuBar(getClusterWindowMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);
        // exitMenuItem listener
        getExitMenuItem().
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        exitMenuEventHandler(e);
                    }
                });

        // HelpTopics listener
        getHelpTopicsMenuItem().
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Registry.getDefault().getComponentRegistry().getMainWindow().showHelpTopics();
                    }
                });

        initAdminConnection();
        initCaches();

        // refresh the status
        refreshStatus();

        //todo: remove this test data
/*        Vector dummyData = getClusterStatusDummyData(false);
        dummyData = getClusterStatusDummyData(true);
        getClusterStatusTableModel().setData(dummyData);
        getClusterStatusTableModel().getRealModel().setRowCount(dummyData.size());
        getClusterStatusTableModel().fireTableDataChanged();*/

        pack();

        //todo: need to reorganize this
/*        getStatisticsPane().onConnect();
        getStatisticsPane().refreshStatistics();*/

    }

    /**
     * @param event ActionEvent
     * @see ActionEvent for details
     */
    private void exitMenuEventHandler(ActionEvent event) {
        Registry.getDefault().getComponentRegistry().getMainWindow().getStatMenuItem().setSelected(false);
        this.dispose();
    }


    private JPanel getJFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setPreferredSize(new Dimension(800, 600));
            frameContentPane.setLayout(new BorderLayout());
            //       getJFrameContentPane().add(getToolBarPane(), "North");
            getJFrameContentPane().add(getMainPane(), "Center");
        }
        return frameContentPane;
    }

    private JMenuBar getClusterWindowMenuBar() {
        if (clusterWindowMenuBar == null) {
            clusterWindowMenuBar = new JMenuBar();
            clusterWindowMenuBar.add(getFileMenu());
            // clusterWindowMenuBar.add(getViewMenu());
            clusterWindowMenuBar.add(getHelpMenu());
        }
        return clusterWindowMenuBar;
    }

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

    private JMenu getHelpMenu() {
        if (helpMenu != null) return helpMenu;

        helpMenu = new JMenu();
        helpMenu.setText(resapplication.getString("Help"));
        helpMenu.add(getHelpTopicsMenuItem());
        int mnemonic = helpMenu.getText().toCharArray()[0];
        helpMenu.setMnemonic(mnemonic);

        return helpMenu;
    }

    private JMenuItem getExitMenuItem() {
        if (exitMenuItem == null) {
            exitMenuItem = new JMenuItem();
            exitMenuItem.setText(resapplication.getString("ExitMenuItem_text"));
            int mnemonic = 'X';
            exitMenuItem.setMnemonic(mnemonic);
            exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return exitMenuItem;
    }


    private JPanel getMainPane() {
        if (mainPane != null) return mainPane;

        mainPane = new javax.swing.JPanel();

        mainPane = new javax.swing.JPanel();
        mainPane.setLayout(new BorderLayout());
        jSplitPane1 = new javax.swing.JSplitPane();

        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.5);
        jPanel2.setLayout(new java.awt.BorderLayout());

        jScrollPane2.setMinimumSize(new java.awt.Dimension(400, 220));
        jTable2.setModel(getClusterStatusTableModel());

        BarIndicator loadShareRenderer = new BarIndicator(MIN, MAX, Color.green);
        loadShareRenderer.setStringPainted(true);
        loadShareRenderer.setBackground(jTable2.getBackground());

        // set limit value and fill color
        Hashtable limitColors1 = new Hashtable();
        limitColors1.put(new Integer(0), Color.green);
        // limitColors.put(new Integer(60), Color.yellow);
        // limitColors.put(new Integer(80), Color.red);

        //loadShareRenderer.setLimits(limitColors1);

        BarIndicator requestRoutedRenderer = new BarIndicator(MIN, MAX, Color.blue);
        requestRoutedRenderer.setStringPainted(true);
        requestRoutedRenderer.setBackground(jTable2.getBackground());

        Hashtable limitColors2 = new Hashtable();
        limitColors2.put(new Integer(0), Color.red);
        //requestFailureRenderer.setLimits(limitColors2);

        jTable2.getColumnModel().getColumn(2).setCellRenderer(loadShareRenderer);
        jTable2.getColumnModel().getColumn(3).setCellRenderer(requestRoutedRenderer);

        jTable2.getColumnModel().getColumn(0).setMinWidth(0);
        jTable2.getColumnModel().getColumn(0).setMaxWidth(0);
        jTable2.getColumnModel().getColumn(0).setPreferredWidth(0);

        jTable2.getColumnModel().getColumn(1).setCellRenderer(
                new DefaultTableCellRenderer() {
                    private Icon connectIcon =
                            new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/connect2.gif"));
                    private Icon disconnectIcon =
                            new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/disconnect.gif"));

                    public Component getTableCellRendererComponent(JTable table,
                                                                   Object value,
                                                                   boolean isSelected,
                                                                   boolean hasFocus,
                                                                   int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(null);

                        Object s = jTable2.getValueAt(row, 0);

                        //String status = "";
                        // if(s instanceof String) status = (String) s;
                        // this.setText("");
                        if (s.toString().equals("1")) {
                            this.setIcon(connectIcon);
                        } else if (s.toString().equals("0")) {
                            this.setIcon(disconnectIcon);
                        } else {

                        }
                        return this;
                    }
                });

        jTable2.getColumnModel().getColumn(5).setCellRenderer(
                new DefaultTableCellRenderer() {

                    public Component getTableCellRendererComponent(JTable table,
                                                                   Object value,
                                                                   boolean isSelected,
                                                                   boolean hasFocus,
                                                                   int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        this.setHorizontalAlignment(SwingConstants.TRAILING);
                        if (value instanceof Long) {
                            this.setText(convertUptimeToString(((Long) value).longValue()));
                        }

                        return this;
                    }
                });

        for (int i = 0; i <= 6; i++) {
            jTable2.getColumnModel().getColumn(i).setHeaderRenderer(iconHeaderRenderer);
        }

        addMouseListenerToHeaderInTable(jTable2);
        jTable2.getTableHeader().setReorderingAllowed(false);

        jScrollPane2.setViewportView(jTable2);

        jPanel2.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jLabel4.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel4.setText("Gateway Status");
        jLabel4.setMaximumSize(new java.awt.Dimension(136, 40));
        jLabel4.setMinimumSize(new java.awt.Dimension(136, 40));
        jLabel4.setPreferredSize(new java.awt.Dimension(136, 40));
        jPanel2.add(jLabel4, java.awt.BorderLayout.NORTH);

        jSplitPane1.setLeftComponent(jPanel2);

        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel3.setPreferredSize(new java.awt.Dimension(400, 308));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel1.setText(" Service Statistics");
        jLabel1.setMaximumSize(new java.awt.Dimension(136, 40));
        jLabel1.setMinimumSize(new java.awt.Dimension(136, 40));
        jLabel1.setPreferredSize(new java.awt.Dimension(136, 40));
        jPanel1.add(jLabel1, java.awt.BorderLayout.NORTH);

        jPanel8.setLayout(new java.awt.BorderLayout());
        jPanel1.add(getStatisticsPane(), java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanel1);

        mainPane.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        return mainPane;
    }


    private JMenuItem getHelpTopicsMenuItem() {
        if (helpTopicsMenuItem == null) {
            helpTopicsMenuItem = new JMenuItem();
            helpTopicsMenuItem.setText(resapplication.getString("Help_TopicsMenuItem_text"));
            int mnemonic = helpTopicsMenuItem.getText().toCharArray()[0];
            helpTopicsMenuItem.setMnemonic(mnemonic);
            helpTopicsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        }
        return helpTopicsMenuItem;
    }

    private StatisticsPanel getStatisticsPane() {
        if (statisticsPane != null) return statisticsPane;

        statisticsPane = new StatisticsPanel();
        return statisticsPane;
    }

    private ClusterStatusTableSorter getClusterStatusTableModel() {

        if (clusterStatusTableSorter != null) {
            return clusterStatusTableSorter;
        }

        Object[][] rows = new Object[][]{};

        String[] cols = new String[]{
            "Status", "Gateway", "Load Sharing %", "Request Routed %", "Load Avg", "Uptime", "IP Address"
        };

        Class[] types = new Class[]{
            java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.String.class, java.lang.String.class
        };

        LogTableModel tableModel = new LogTableModel(rows, cols);

        clusterStatusTableSorter = new ClusterStatusTableSorter(tableModel) {
            /*public Class getColumnClass(int columnIndex) {
                return types[columnIndex];

            }*/
        };

        return clusterStatusTableSorter;

    }

    private void initAdminConnection(){

        System.out.println("Initializing Admin Service....");

        serviceManager = (ServiceAdmin) Locator.getDefault().lookup(ServiceAdmin.class);
         if (serviceManager == null) throw new IllegalStateException("Cannot obtain ServiceManager remote reference");

         clusterStatusAdmin = (ClusterStatusAdmin) Locator.getDefault().lookup(ClusterStatusAdmin.class);
         if (clusterStatusAdmin == null) throw new RuntimeException("Cannot obtain ClusterStatusAdmin remote reference");

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

                    ((ClusterStatusTableSorter) tableView.getModel()).sortData(column, true);
                    ((ClusterStatusTableSorter) tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    private String convertUptimeToString(long uptime) {

        if(uptime == 0) return new String("0 mins");

        long uptime_ms = (System.currentTimeMillis() - uptime) / 1000;

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

    private Vector prepareClusterStatusData() {

        Vector cs = new Vector();

        if (currentNodeList == null) return cs;

        for (Iterator i = currentNodeList.keySet().iterator(); i.hasNext();) {
            GatewayStatus su = (GatewayStatus) currentNodeList.get(i.next());

            // the second last update time stamp is -1 the very first time when the node status is retrieved
            if(su.getSecondLastUpdateTimeStamp() == -1 ||
                su.getLastUpdateTimeStamp() != su.getSecondLastUpdateTimeStamp()){
                su.setStatus(1);
            }
            else{
                su.setStatus(0);
            }

            if(getClusterRequestCount() !=0 ){
                su.setLoadSharing((new Long(su.getRequestCount() * 100 / getClusterRequestCount())).intValue());
            }
            else{
                su.setLoadSharing(0);
            }
            cs.add(su);
        }

/*        ServiceUsage[] serviceStat = new ServiceUsage[0];
        try {
            serviceStat = clusterStatusService.getServiceUsage();
        } catch (FindException e) {
            System.err.println("ERROR " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("ERROR " + e.getMessage());
        }
        if (serviceStat != null) {
            System.out.println("Number of service statistics is: " + serviceStat.length);
        }

        long totalClusterRequest = 0;
        for (int i = 0; i < serviceStat.length; i++) {
            if (addExtraCount) {
                totalClusterRequest += (serviceStat[i].getRequests() + 300);
            } else {
                totalClusterRequest += serviceStat[i].getRequests();
            }

        }
        updateClusterRequestCounterCache(totalClusterRequest);


        long totalRequest;
        long totalCompleted;

        for (int i = 0; i < dummyData.size(); i++) {
            GatewayStatus gatewayStatus = (GatewayStatus) dummyData.elementAt(i);

            totalRequest = 0;
            totalCompleted = 0;
            for (int j = 0; j < serviceStat.length; j++) {
                if (gatewayStatus.getName().equals(serviceStat[j].getNodeid())) {
                    if (addExtraCount) {
                        totalRequest += (serviceStat[j].getRequests() + 300);
                        totalCompleted += (serviceStat[j].getCompleted() + 300);
                    } else {
                        totalRequest += serviceStat[j].getRequests();
                        totalCompleted += serviceStat[j].getCompleted();
                    }

                }
            }

            gatewayStatus.updateRequestCounterCache(totalRequest);
            gatewayStatus.updateCompletedCounterCache(totalCompleted);
            if (getClusterRequestCount() > 0) {
                gatewayStatus.setLoadSharing((new Long(gatewayStatus.getRequestCount() * 100 / getClusterRequestCount())).intValue());
            } else {
                gatewayStatus.setLoadSharing(0);
            }
            gatewayStatus.setStatus(1);

        }*/

        return cs;
    }


    private void updateClusterRequestCounterCache(long newCount) {

        if (clusterRequestCounterCache.size() <= GatewayStatus.NUMBER_OF_SAMPLE_PER_MINUTE) {
            clusterRequestCounterCache.add(new Long(newCount));
        } else {
            clusterRequestCounterCache.remove(0);
            clusterRequestCounterCache.add(new Long(newCount));
        }
    }

    private long getClusterRequestCount() {

        long totalCount = 0;

        int index = clusterRequestCounterCache.size() - 1;

        for (int i = 0; i < clusterRequestCounterCache.size() - 1; i++, index--) {

            totalCount += ((Long) clusterRequestCounterCache.get(index)).longValue() - ((Long) clusterRequestCounterCache.get(index - 1)).longValue();
        }

        return totalCount;
    }


    private javax.swing.Timer getStatusRefreshTimer() {

        if (statusRefreshTimer != null) return statusRefreshTimer;

        // Create a refresh timer.
        statusRefreshTimer = new javax.swing.Timer(GatewayStatus.STATUS_REFRESH_TIMER, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                refreshStatus();
            }
        });

        return statusRefreshTimer;
    }

    public void refreshStatus() {

        getStatusRefreshTimer().stop();

        // create a worker thread to retrieve the Service statistics
        final ClusterStatusWorker statsWorker = new ClusterStatusWorker(serviceManager, clusterStatusAdmin, currentNodeList) {
            public void finished() {
                //updateServerMetricsFields(getMetrics());
                statisticsPane.updateStatisticsTable(this.getStatisticsList());

                currentNodeList = getNewNodeList();
                updateClusterRequestCounterCache(this.getClusterRequestCount());

                Vector cs = prepareClusterStatusData();

                getClusterStatusTableModel().setData(cs);
                getClusterStatusTableModel().getRealModel().setRowCount(cs.size());
                getClusterStatusTableModel().fireTableDataChanged();
                getStatusRefreshTimer().start();
            }
        };

        statsWorker.start();
    }

    public void dispose(){
        getStatusRefreshTimer().stop();
        super.dispose();
    }

    public void onConnect() {

        System.out.println("Connection is UP.....");
        initAdminConnection();
        initCaches();

        getStatusRefreshTimer().start();
    }

    public void onDisconnect() {

        System.out.println("Connection is DOWN");
        getStatusRefreshTimer().stop();
        serviceManager = null;
        clusterStatusAdmin = null;
    }

    private void initCaches(){
        currentNodeList = new Hashtable();
        clusterRequestCounterCache = new Vector();
    }

    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;

    private javax.swing.JPanel mainPane;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable jTable2;

    private javax.swing.JMenuBar clusterWindowMenuBar;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem helpTopicsMenuItem;
    private ClusterStatusTableSorter clusterStatusTableSorter = null;
    private StatisticsPanel statisticsPane;
    private Vector clusterRequestCounterCache = new Vector();
    private JPanel frameContentPane;
    private javax.swing.Timer statusRefreshTimer;
    private ClusterStatusAdmin clusterStatusAdmin;
    private ServiceAdmin serviceManager;
    private Hashtable currentNodeList;

}

