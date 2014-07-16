/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.dashboard;

import com.l7tech.console.action.PrintAction;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.SheetHolder;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.SsmPreferences;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Monitoring dashboard.
 *
 * <p>Up to SecureSpan 4.1, DashboardWindow displayed just service metrics.
 * From SecureSpan 4.2 on, it contains one tab for service metrics, another for cluster status.
 *
 * @author alex
 * @author rmak
 */
public class DashboardWindow extends JFrame implements LogonListener, SheetHolder {

    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    private ServiceMetricsPanel serviceMetricsPanel;
    private ClusterStatusPanel clusterStatusPanel;
    private JPanel serviceMetricsTabPanel;
    private JPanel clusterStatusTabPanel;
    private SsmPreferences preferences = TopComponents.getInstance().getPreferences();

    private final int clusterStatusTabIndex;

    private static final ResourceBundle _commonResources = ResourceBundle.getBundle("com.l7tech.console.resources.console");
    private static final ResourceBundle _windowResources = ResourceBundle.getBundle("com.l7tech.console.panels.dashboard.resources.DashboardWindow");
    private static final Logger logger = Logger.getLogger(DashboardWindow.class.getName());

    private final ImageIcon ALERT_ICON = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/Alert16x16.gif"));

    public DashboardWindow() throws HeadlessException {
        super(_windowResources.getString("window.title"));

        ImageIcon imageIcon = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/CA_Logo_Black_16x16.png"));
        setIconImage(imageIcon.getImage());

        initMenuBar();

        clusterStatusTabIndex = tabbedPane.indexOfComponent(clusterStatusTabPanel);

        clusterStatusPanel.addPropertyChangeListener(ClusterStatusPanel.CLUSTER_STATUS_CHANGE_PROPERTY, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (ClusterStatusPanel.CLUSTER_STATUS_CHANGE_PROPERTY.equals(evt.getPropertyName())) {
                    // Sets alert icon on cluster status tab if any node is down. Clears if none.
                    boolean anyInactive = false;
                    //noinspection unchecked
                    final Vector<GatewayStatus> clusterStatus = (Vector<GatewayStatus>)evt.getNewValue();
                    for (GatewayStatus gs : clusterStatus) {
                        if (gs.getStatus() == GatewayStatus.NODE_STATUS_INACTIVE) {
                            anyInactive = true;
                            break;
                        }
                    }

                    setTabAlert(clusterStatusTabIndex, anyInactive);
                }
            }
        });

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                // Disables service metrics refresh if tab is not in front in
                // order to save network bandwidth and CPU time.
                serviceMetricsPanel.setRefreshEnabled(tabbedPane.getSelectedComponent() == serviceMetricsTabPanel);
                // Cannot do the same for cluster status because we want
                // node-down-alert whether tab is in front or not.
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        getContentPane().add(mainPanel);
        pack();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exitMenuEventHandler();
            }
        });

        // Load the last window status (size and location).
        Utilities.restoreWindowStatus(this, preferences.asProperties(), getWidth(), getHeight());
    }

    /**
     * Clean up the resources of the window when the user exits the window.
     */
    private void exitMenuEventHandler() {
        try {
            Properties prop = Utilities.getWindowStatus(this);
            preferences.updateFromProperties(prop, true);
            preferences.store();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to save divider location.", e);
        }

        dispose();
    }

    private void initMenuBar() {
        final JMenuItem exitMenuItem = new JMenuItem(_commonResources.getString("ExitMenuItem.name"));
        exitMenuItem.setMnemonic('X');
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke('X', KeyEvent.ALT_DOWN_MASK));
        exitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exitMenuEventHandler();
            }
        });

        final JMenuItem printItem = new JMenuItem(_commonResources.getString("Print"));
        printItem.setMnemonic(printItem.getText().toCharArray()[0]);
        printItem.setAccelerator(KeyStroke.getKeyStroke('P', KeyEvent.ALT_DOWN_MASK));
        printItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PageFormat pageFormat = new PageFormat();
                pageFormat.setOrientation(PageFormat.LANDSCAPE);
                new PrintAction(mainPanel, pageFormat).actionPerformed(e);
            }
        });

        final JMenu fileMenu = new JMenu(_commonResources.getString("File"));
        fileMenu.setMnemonic(fileMenu.getText().toCharArray()[0]);
        fileMenu.add(exitMenuItem);
        fileMenu.add(printItem);

        final JMenuItem helpTopicsMenuItem = new JMenuItem(_commonResources.getString("Help_TopicsMenuItem_text_name"));
        helpTopicsMenuItem.setMnemonic('H');
        helpTopicsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        helpTopicsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TopComponents.getInstance().showHelpTopics();
            }
        });

        final JMenu helpMenu = new JMenu(_commonResources.getString("Help"));
        helpMenu.setMnemonic(helpMenu.getText().toCharArray()[0]);
        helpMenu.add(helpTopicsMenuItem);

        final JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
    }

    public void setTabAlert(int index, boolean alert) {
        if (alert) {
            tabbedPane.setForegroundAt(index, Color.RED);
            tabbedPane.setIconAt(index, ALERT_ICON);
        } else {
            tabbedPane.setForegroundAt(index, (Color)UIManager.get("TabbedPane.foreground"));
            tabbedPane.setIconAt(index, null);
        }
    }

    public void onLogon(LogonEvent e) {
        serviceMetricsPanel.onLogon(e);
        clusterStatusPanel.onLogon(e);
    }

    public void onLogoff(LogonEvent e) {
        serviceMetricsPanel.onLogoff(e);
        clusterStatusPanel.onLogoff(e);
        dispose();
    }

    public void setVisible(boolean vis) {
        serviceMetricsPanel.setRefreshEnabled(vis);
        if (!vis) {
            // Let inactivity timeout start counting after this window is closed.
            TopComponents.getInstance().updateLastActivityTime();
        }
        super.setVisible(vis);
    }

    public void dispose() {
        if (serviceMetricsPanel != null) serviceMetricsPanel.dispose();
        if (clusterStatusPanel != null) clusterStatusPanel.dispose();
        super.dispose();
    }

    public void showSheet(JInternalFrame sheet) {
        DialogDisplayer.showSheet(this, sheet);
    }

    /**
     * Instantiates IDEA form components marked as "Custom Create"; such as
     * those requiring constructor parameters.
     */
    private void createUIComponents() {
        clusterStatusPanel = new ClusterStatusPanel(this);
    }
}
