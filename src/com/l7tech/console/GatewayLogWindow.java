package com.l7tech.console;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.util.Locator;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ClusterStatusWorker;
import com.l7tech.console.util.ClusterInfoWorker;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.service.ServiceAdmin;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
public class GatewayLogWindow extends JFrame {
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static
            ResourceBundle resapplication =
            java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");
   // private final ClassLoader cl = getClass().getClassLoader();

    public GatewayLogWindow(final String title) {
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
        //todo: need to reorganize this -- remove it from LogPanel
        getLogPane().onConnect();
        getLogPane().refreshLogs();

        initAdminConnection();
        initCaches();

        // refresh the status
        refreshLogs();
    }

    /**
     * @param event ActionEvent
     * @see ActionEvent for details
     */
    private void exitMenuEventHandler(ActionEvent event) {

        //todo: stop the refresh timer here

        this.dispose();
    }

    private JPanel getJFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setPreferredSize(new Dimension(800, 600));
            frameContentPane.setLayout(new BorderLayout());
            //getJFrameContentPane().add(getToolBarPane(), "North");
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
        gatewayLogPane = new javax.swing.JPanel();
        gatewayLogTitle = new javax.swing.JLabel();

        mainPane.setLayout(new java.awt.BorderLayout());

        gatewayLogTitle.setFont(new java.awt.Font("Dialog", 1, 18));
        gatewayLogTitle.setText("  Log Browser");
        gatewayLogTitle.setMaximumSize(new java.awt.Dimension(136, 40));
        gatewayLogTitle.setMinimumSize(new java.awt.Dimension(136, 40));
        gatewayLogTitle.setPreferredSize(new java.awt.Dimension(136, 40));

        gatewayLogPane.setLayout(new java.awt.BorderLayout());
        gatewayLogPane.add(gatewayLogTitle);

        mainPane.add(gatewayLogPane, java.awt.BorderLayout.NORTH);
        mainPane.add(getLogPane(), java.awt.BorderLayout.CENTER);

        return mainPane;
    }


    public LogPanel getLogPane() {
        if (logPane != null) return logPane;

        logPane = new LogPanel();
        return logPane;

    }

    private javax.swing.Timer getLogRefreshTimer() {

        if (logRefreshTimer != null) return logRefreshTimer;

        // Create a refresh timer.
        logRefreshTimer = new javax.swing.Timer(GatewayStatus.STATUS_REFRESH_TIMER, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                refreshLogs();
            }
        });

        return logRefreshTimer;
    }

    private void refreshLogs() {
        getLogRefreshTimer().stop();

        // create a worker thread to retrieve the cluster info
        final ClusterInfoWorker infoWorker = new ClusterInfoWorker(clusterStatusAdmin, currentNodeList) {
            public void finished() {

                // Note: the get() operation is a blocking operation.
                if (this.get() != null) {

                    currentNodeList = getNewNodeList();
/*
                    updateClusterRequestCounterCache(this.getClusterRequestCount());

                    Vector cs = prepareClusterStatusData();

                    getClusterStatusTableModel().setData(cs);
                    getClusterStatusTableModel().getRealModel().setRowCount(cs.size());
                    getClusterStatusTableModel().fireTableDataChanged();
*/

                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy hh:mm:ss aaa");
                    getLogPane().setLastUpdateTime(("Last updated: " + sdf.format(Calendar.getInstance().getTime()) + "      "));
                    getLogRefreshTimer().start();
                }
                else{
                    if(isRemoteExceptionCaught()){
                        // the connection to the cluster is down
                        onDisconnect();
                    }
                }
            }
        };

        infoWorker.start();
    }

    public void dispose() {
        getLogRefreshTimer().stop();
        super.dispose();
    }

    public void onConnect() {
        initAdminConnection();
        initCaches();
//        getClusterConnectionStatusLabel().setText("");
        getLogRefreshTimer().start();
    }

    public void onDisconnect() {
//        getClusterConnectionStatusLabel().setText("      Error: Connection to the gateway cluster is down.");
//        getClusterConnectionStatusLabel().setForeground(Color.red);
        getLogRefreshTimer().stop();

        serviceManager = null;
        clusterStatusAdmin = null;
    }

     private void initAdminConnection() {
        clusterStatusAdmin = (ClusterStatusAdmin) Locator.getDefault().lookup(ClusterStatusAdmin.class);
        if (clusterStatusAdmin == null) throw new RuntimeException("Cannot obtain ClusterStatusAdmin remote reference");
    }

    private void initCaches() {
        currentNodeList = new Hashtable();
    }

    private javax.swing.JLabel gatewayLogTitle;
    private javax.swing.JPanel gatewayLogPane;
    private javax.swing.JPanel mainPane;

    private javax.swing.JMenuBar clusterWindowMenuBar;
    private javax.swing.JMenu fileMenu;

    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem helpTopicsMenuItem;
    private JPanel frameContentPane;
    private LogPanel logPane;
    private javax.swing.Timer logRefreshTimer;
    private ClusterStatusAdmin clusterStatusAdmin;
    private ServiceAdmin serviceManager;
    private Hashtable currentNodeList;

}
