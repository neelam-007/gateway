package com.l7tech.console;

import com.l7tech.console.util.BarIndicator;
import com.l7tech.console.util.Registry;
import com.l7tech.console.panels.StatisticsPanel;
import com.l7tech.console.table.ClusterStatusTableSorter;
import com.l7tech.console.table.LogTableModel;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.cluster.ClusterInfo;
import com.l7tech.console.icons.ArrowIcon;
import com.l7tech.common.gui.util.ImageCache;

import javax.swing.*;
import javax.swing.table.*;
import java.util.ResourceBundle;
import java.util.Hashtable;
import java.util.Vector;
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


        //todo: remove this test data
        Vector dummyData = getClusterStatusDummyData();
        getClusterStatusTableModel().setData(dummyData);
        getClusterStatusTableModel().getRealModel().setRowCount(dummyData.size());
        getClusterStatusTableModel().fireTableDataChanged();

        pack();

        //todo: need to reorganize this
        getStatisticsPane().onConnect();
        getStatisticsPane().refreshStatistics();

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
        //jTable1 = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        //jScrollPane1 = new javax.swing.JScrollPane();

        msgTable3 = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();

        jLabel2 = new javax.swing.JLabel();

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.5);
        jPanel2.setLayout(new java.awt.BorderLayout());

        jScrollPane2.setMinimumSize(new java.awt.Dimension(400, 220));
        jTable2.setModel(getClusterStatusTableModel());

        BarIndicator loadShareRenderer = new BarIndicator(MIN,MAX, Color.green);
         loadShareRenderer.setStringPainted(true);
         loadShareRenderer.setBackground(jTable2.getBackground());

         // set limit value and fill color
         Hashtable limitColors1 = new Hashtable();
         limitColors1.put(new Integer( 0), Color.green);
        // limitColors.put(new Integer(60), Color.yellow);
        // limitColors.put(new Integer(80), Color.red);

         //loadShareRenderer.setLimits(limitColors1);

         BarIndicator requestFailureRenderer = new BarIndicator(MIN,MAX, Color.red);
         requestFailureRenderer.setStringPainted(true);
         requestFailureRenderer.setBackground(jTable2.getBackground());

         Hashtable limitColors2 = new Hashtable();
         limitColors2.put(new Integer( 0), Color.red);
        //requestFailureRenderer.setLimits(limitColors2);

         jTable2.getColumnModel().getColumn(2).setCellRenderer(loadShareRenderer);
         jTable2.getColumnModel().getColumn(3).setCellRenderer(requestFailureRenderer);

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
                  if(value instanceof Long) {
                      this.setText(convertUptimeToString(((Long) value).longValue()));
                  }

                  return this;
              }
          });

        for(int i = 0; i <= 6; i++){
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

    private ClusterStatusTableSorter getClusterStatusTableModel(){

        if (clusterStatusTableSorter != null) {
            return clusterStatusTableSorter;
        }

         Object[][] rows = new Object [][]{
/*                    {new Integer(1), "SSG1", new Integer(20), new Integer(5), new Double(1.5), "1 days 2 hrs 16 mins", "192.128.1.100"},
                    {new Integer(1), "SSG2", new Integer(23), new Integer(10), new Double(1.8), "2 days 10 hrs 3 mins", "192.128.1.101"},
                    {new Integer(0), "SSG3", new Integer(0), new Integer(0), new Double(0), "0", "192.128.1.102"},
                    {new Integer(1), "SSG4", new Integer(17), new Integer(3), new Double(1.1), "2 hrs 10 mins", "192.128.2.10"},
                    {new Integer(1), "SSG5", new Integer(18), new Integer(8), new Double(2.1), "6 days 8 hrs 55 mins", "192.128.2.11"},
                    {new Integer(1), "SSG6", new Integer(22), new Integer(5), new Double(0.8), "7 hrs 23 mins", "192.128.3.1"},
                    {new Integer(0), "SSG7", new Integer(0), new Integer(0), new Double(0), "0", "192.128.3.2"},
                    {new Integer(0), "SSG8", new Integer(0), new Integer(0), new Double(0), "0", "192.128.3.3"}*/
            };

        String[] cols = new String[]{
            "Status", "Gateway", "Load Sharing %", "Request Failure%", "Load Avg", "Uptime", "IP Address"
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

                    ((ClusterStatusTableSorter)tableView.getModel()).sortData(column, true);
                    ((ClusterStatusTableSorter)tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    String convertUptimeToString(long uptime){

        System.out.println("Current time is : " + System.currentTimeMillis());
        long uptime_ms = System.currentTimeMillis()/1000 - uptime;

        long serverUpTimeMinutues = uptime_ms/60;
        long minutes_remain = serverUpTimeMinutues%60;
        long hrs_total = serverUpTimeMinutues/60;
        long hrs_remain = hrs_total%24;
        long days = hrs_total/24;
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

//        serverUpTime.setText(END_SPACE + SERVER_UP_TIME_PREFIX + uptimeString);
 //       lastMinuteServerLoad.setText(LAST_MINUTE_SERVER_LOAD_PREFIX + Double.toString(metrics.getLoad1()) + END_SPACE);

        return uptimeString;
    }

    Vector getClusterStatusDummyData(){

     Vector dummyData = new Vector();

        ClusterInfo  c1 = new ClusterInfo();
        c1.setName("SSG1"); c1.setAddress("192.128.1.100"); c1.setAvgLoad(1.5); c1.setUptime(1072746384);
        GatewayStatus node1 = new GatewayStatus(c1, 1,  20, 5);

        ClusterInfo  c2 = new ClusterInfo();
        c2.setName("SSG2"); c2.setAddress("192.128.1.101"); c2.setAvgLoad(1.8); c2.setUptime(1072656394);
        GatewayStatus node2 = new GatewayStatus(c2, 1,  23, 10);

        ClusterInfo  c3 = new ClusterInfo();
        c3.setName("SSG3"); c3.setAddress("192.128.1.102"); c3.setAvgLoad(0); c3.setUptime(1072746404);
        GatewayStatus node3 = new GatewayStatus(c3, 0,  0, 0);

        ClusterInfo  c4 = new ClusterInfo();
        c4.setName("SSG4"); c4.setAddress("192.128.2.10"); c4.setAvgLoad(1.1); c4.setUptime(1072776414);
        GatewayStatus node4 = new GatewayStatus(c4, 1,  17, 3);

        ClusterInfo  c5 = new ClusterInfo();
        c5.setName("SSG5"); c5.setAddress("192.128.2.11"); c5.setAvgLoad(2.1); c5.setUptime(1072746484);
        GatewayStatus node5 = new GatewayStatus(c5, 1,  18, 8);

        ClusterInfo  c6 = new ClusterInfo();
        c6.setName("SSG6"); c6.setAddress("192.128.3.1"); c6.setAvgLoad(0.8); c6.setUptime(1072736464);
        GatewayStatus node6 = new GatewayStatus(c6, 1,  22, 5);

        ClusterInfo  c7 = new ClusterInfo();
        c7.setName("SSG7"); c7.setAddress("192.128.3.2"); c7.setAvgLoad(0); c7.setUptime(1072808010);
        GatewayStatus node7 = new GatewayStatus(c7, 0,  0, 0);

        ClusterInfo  c8 = new ClusterInfo();
        c8.setName("SSG8"); c8.setAddress("192.128.3.3"); c8.setAvgLoad(0); c8.setUptime(1072808325);
        GatewayStatus node8 = new GatewayStatus(c8, 0,  0, 0);

        dummyData.add(node1);
        dummyData.add(node2);
        dummyData.add(node3);
        dummyData.add(node4);
        dummyData.add(node5);
        dummyData.add(node6);
        dummyData.add(node7);
        dummyData.add(node8);

        return dummyData;
    }

    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;

    private javax.swing.JPanel mainPane;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel8;
//    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
//    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable msgTable3;

    private javax.swing.JMenuBar clusterWindowMenuBar;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem helpTopicsMenuItem;
    private ClusterStatusTableSorter clusterStatusTableSorter = null;
    private StatisticsPanel statisticsPane;

    private JPanel frameContentPane;

}

