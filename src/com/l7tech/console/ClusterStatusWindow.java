package com.l7tech.console;

import com.l7tech.console.util.*;
import com.l7tech.console.panels.StatisticsPanel;
import com.l7tech.console.table.ClusterStatusTableSorter;
import com.l7tech.console.table.LogTableModel;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.console.icons.ArrowIcon;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.util.Locator;
import com.l7tech.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;


/*
 * This class is a window extended from JFrame to show the cluster status.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ClusterStatusWindow extends JFrame {

    private static final int MAX = 100;
    private static final int MIN = 0;
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static ResourceBundle resapplication = java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");
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

        initAdminConnection();
        initCaches();

        // refresh the status
        refreshStatus();

        pack();
    }

    /**
     * @param event ActionEvent
     * @see ActionEvent for details
     */
    private void exitMenuEventHandler(ActionEvent event) {
        getStatusRefreshTimer().stop();
        this.dispose();
    }


    private JPanel getJFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setPreferredSize(new Dimension(800, 600));
            frameContentPane.setLayout(new BorderLayout());
            getJFrameContentPane().add(getMainPane(), "Center");
        }
        return frameContentPane;
    }

    private JMenuBar getClusterWindowMenuBar() {
        if (clusterWindowMenuBar == null) {
            clusterWindowMenuBar = new JMenuBar();
            clusterWindowMenuBar.add(getFileMenu());
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
        if (exitMenuItem != null) return exitMenuItem;

        exitMenuItem = new JMenuItem();
        exitMenuItem.setText(resapplication.getString("ExitMenuItem_text"));
        int mnemonic = 'X';
        exitMenuItem.setMnemonic(mnemonic);
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        exitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exitMenuEventHandler(e);
            }
        });

        return exitMenuItem;
    }


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
        mainSplitPane.setResizeWeight(0.5);
        clusterStatusPane.setLayout(new java.awt.BorderLayout());

        clusterStatusScrollPane.setMinimumSize(new java.awt.Dimension(400, 220));
        clusterStatusScrollPane.setViewportView(getClusterStatusTable());

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

    private JPanel getMessagePane(){

        if(messagePane != null) return messagePane;

        messagePane = new JPanel();


        messagePane.setLayout(new BorderLayout());
        messagePane.setPreferredSize(new java.awt.Dimension(136, 40));
        messagePane.setMinimumSize(new java.awt.Dimension(136, 40));
        messagePane.setMaximumSize(new java.awt.Dimension(136, 40));
        messagePane.add(getLastUpdateLabel(), java.awt.BorderLayout.EAST);

        return messagePane;
    }

    private JLabel getLastUpdateLabel(){
        if(updateTimeStamp != null) return updateTimeStamp;

        updateTimeStamp = new JLabel();
        updateTimeStamp.setText("");

        return updateTimeStamp;
    }

    private JTable getClusterStatusTable(){

        if(clusterStatusTable != null) return clusterStatusTable;

        clusterStatusTable = new javax.swing.JTable();
        clusterStatusTable.setModel(getClusterStatusTableModel());

        BarIndicator loadShareRenderer = new BarIndicator(MIN, MAX, Color.blue);
        loadShareRenderer.setStringPainted(true);
        loadShareRenderer.setBackground(clusterStatusTable.getBackground());

        // set limit value and fill color
        Hashtable limitColors1 = new Hashtable();
        limitColors1.put(new Integer(0), Color.green);

        BarIndicator requestRoutedRenderer = new BarIndicator(MIN, MAX, Color.magenta);
        requestRoutedRenderer.setStringPainted(true);
        requestRoutedRenderer.setBackground(clusterStatusTable.getBackground());

        clusterStatusTable.getColumnModel().getColumn(2).setCellRenderer(loadShareRenderer);
        clusterStatusTable.getColumnModel().getColumn(3).setCellRenderer(requestRoutedRenderer);

        clusterStatusTable.getColumnModel().getColumn(0).setMinWidth(0);
        clusterStatusTable.getColumnModel().getColumn(0).setMaxWidth(0);
        clusterStatusTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        clusterStatusTable.getColumnModel().getColumn(1).setCellRenderer(
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

                        Object s = clusterStatusTable.getValueAt(row, 0);

                        if (s.toString().equals("1")) {
                            this.setIcon(connectIcon);
                        } else if (s.toString().equals("0")) {
                            this.setIcon(disconnectIcon);
                        } else {

                        }
                        return this;
                    }
                });

        clusterStatusTable.getColumnModel().getColumn(5).setCellRenderer(
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

        for (int i = 0; i < clusterStatusTable.getColumnModel().getColumnCount();  i++) {
            clusterStatusTable.getColumnModel().getColumn(i).setHeaderRenderer(iconHeaderRenderer);
        }

        addMouseListenerToHeaderInTable(clusterStatusTable);
        clusterStatusTable.getTableHeader().setReorderingAllowed(false);

        return clusterStatusTable;
    }


    private JMenuItem getHelpTopicsMenuItem() {
        if (helpTopicsMenuItem != null) return helpTopicsMenuItem;

        helpTopicsMenuItem = new JMenuItem();
        helpTopicsMenuItem.setText(resapplication.getString("Help_TopicsMenuItem_text"));
        int mnemonic = helpTopicsMenuItem.getText().toCharArray()[0];
        helpTopicsMenuItem.setMnemonic(mnemonic);
        helpTopicsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        helpTopicsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Registry.getDefault().getComponentRegistry().getMainWindow().showHelpTopics();
            }
        });

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

        clusterStatusTableSorter = new ClusterStatusTableSorter(new LogTableModel(rows, cols)) {};

        return clusterStatusTableSorter;

    }

    private void initAdminConnection() {
        serviceManager = (ServiceAdmin) Locator.getDefault().lookup(ServiceAdmin.class);
        if (serviceManager == null) throw new IllegalStateException("Cannot obtain ServiceManager remote reference");

        clusterStatusAdmin = (ClusterStatusAdmin) Locator.getDefault().lookup(ClusterStatusAdmin.class);
        if (clusterStatusAdmin == null) throw new RuntimeException("Cannot obtain ClusterStatusAdmin remote reference");

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

        if (uptime == 0) return new String("0 mins");

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

            if(su.getLastUpdateTimeStamp() == su.getSecondLastUpdateTimeStamp()){
                su.incrementTimeStampUpdateFailureCount();
            }
            // the second last update time stamp is -1 the very first time when the node status is retrieved
            if (su.getSecondLastUpdateTimeStamp() == -1 ||
                    su.getLastUpdateTimeStamp() != su.getSecondLastUpdateTimeStamp()) {
                su.setStatus(1);
                su.resetTimeStampUpdateFailureCount();
            } else {
                if(su.getTimeStampUpdateFailureCount() >= GatewayStatus.MAX_UPDATE_FAILURE_COUNT){
                    su.setStatus(0);
                    su.resetTimeStampUpdateFailureCount();
                }
                else{
                    if(su.getLastUpdateTimeStamp() != su.getSecondLastUpdateTimeStamp()){
                        su.setStatus(1);
                    }
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

                SimpleDateFormat sdf = new SimpleDateFormat( "MMM d yyyy HH:mm:ss aaa" );;
                getLastUpdateLabel().setText("Last updated: " + sdf.format( Calendar.getInstance().getTime() ) + "      ");
                getStatusRefreshTimer().start();
            }
        };

        statsWorker.start();
    }

    public void dispose() {
        getStatusRefreshTimer().stop();
        super.dispose();
    }

    public void onConnect() {
        initAdminConnection();
        initCaches();

        getStatusRefreshTimer().start();
    }

    public void onDisconnect() {
        getStatusRefreshTimer().stop();
        serviceManager = null;
        clusterStatusAdmin = null;
    }

    private void initCaches() {
        currentNodeList = new Hashtable();
        clusterRequestCounterCache = new Vector();
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

    private javax.swing.JLabel serviceStatTitle;
    private javax.swing.JLabel clusterStatusTitle;
    private javax.swing.JLabel updateTimeStamp;
    private javax.swing.JPanel messagePane;
    private javax.swing.JPanel frameContentPane;
    private javax.swing.JPanel mainPane;
    private javax.swing.JPanel serviceStatPane;
    private javax.swing.JPanel clusterStatusPane;
    private javax.swing.JSplitPane mainSplitPane;
    private javax.swing.JScrollPane clusterStatusScrollPane;
    private javax.swing.JTable clusterStatusTable;
    private javax.swing.JMenuBar clusterWindowMenuBar;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem helpTopicsMenuItem;

    private ClusterStatusTableSorter clusterStatusTableSorter = null;
    private StatisticsPanel statisticsPane;
    private Vector clusterRequestCounterCache = new Vector();
    private javax.swing.Timer statusRefreshTimer;
    private ClusterStatusAdmin clusterStatusAdmin;
    private ServiceAdmin serviceManager;
    private Hashtable currentNodeList;


}

