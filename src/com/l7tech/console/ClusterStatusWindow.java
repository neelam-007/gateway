package com.l7tech.console;

import com.l7tech.console.action.AboutAction;
import com.l7tech.console.action.ActionVetoException;
import com.l7tech.console.util.BarIndicator;
import com.l7tech.console.util.Registry;
import com.l7tech.console.panels.StatisticsPanel;
import com.l7tech.common.gui.util.ImageCache;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.ResourceBundle;
import java.util.Hashtable;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;

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
        helpMenu.add(new AboutAction());
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
        jTable2.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{
                    {"Active", "SSG1", new Integer(20), new Integer(5), new Double(1.5), "1 days 2 hrs 16 mins", "192.128.1.100"},
                    {"Active", "SSG2", new Integer(23), new Integer(10), new Double(1.8), "2 days 10 hrs 3 mins", "192.128.1.101"},
                    {"Inactive", "SSG3", new Integer(0), new Integer(0), new Double(0), "0", "192.128.1.102"},
                    {"Active", "SSG4", new Integer(17), new Integer(3), new Double(1.1), "2 hrs 10 mins", "192.128.2.10"},
                    {"Active", "SSG5", new Integer(18), new Integer(8), new Double(2.1), "6 days 8 hrs 55 mins", "192.128.2.11"},
                    {"Active", "SSG6", new Integer(22), new Integer(5), new Double(0.8), "7 hrs 23 mins", "192.128.3.1"},
                    {"Inactive", "SSG7", new Integer(0), new Integer(0), new Double(0), "0", "192.128.3.2"},
                    {"Inactive", "SSG8", new Integer(0), new Integer(0), new Double(0), "0", "192.128.3.3"}
                },
                new String[]{
                    "Status", "Gateway", "Load Sharing %", "Request Failure%", "Load Avg", "Uptime", "IP Address"
                }
        ) {
            Class[] types = new Class[]{
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });

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

                  String status = "";
                  if(s instanceof String) status = (String) s;
                     // this.setText("");
                      if (status.startsWith("Active")) {
                          this.setIcon(connectIcon);
                      } else if (status.startsWith("Inactive")) {
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


                  return this;
              }
          });

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

    private StatisticsPanel statisticsPane;

    private JPanel frameContentPane;

}

