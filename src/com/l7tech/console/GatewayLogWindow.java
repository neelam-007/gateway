package com.l7tech.console;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.util.Locator;
import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.security.LogonEvent;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class GatewayLogWindow extends JFrame implements LogonListener {

    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private javax.swing.JLabel gatewayLogTitle = null;
    private javax.swing.JPanel gatewayLogPane = null;
    private javax.swing.JPanel mainPane = null;
    private javax.swing.JMenuBar clusterWindowMenuBar = null;
    private javax.swing.JMenu fileMenu = null;
    private javax.swing.JMenu helpMenu = null;
    private javax.swing.JMenuItem exitMenuItem = null;
    private javax.swing.JMenuItem helpTopicsMenuItem = null;
    private JPanel frameContentPane = null;
    private LogPanel logPane = null;
    private final Strategy strategy;

    private static
    ResourceBundle resapplication =
      java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    public interface Strategy {
        Locator getLogAdminLocator();
        String getWindowTitle();
        String getPanelTitle();
        Collection getExtraFileMenuActions();
    }

    /**
     * Constructor
     * @param strategy the {@link Strategy} for this window
     */
    public GatewayLogWindow(final Strategy strategy) {
        super(strategy.getWindowTitle());
        this.strategy = strategy;
        ImageIcon imageIcon =
          new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png"));
        setIconImage(imageIcon.getImage());
        setBounds(0, 0, 850, 600);
        setJMenuBar(getClusterWindowMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);

        Actions.setEscKeyStrokeDisposes(this);

        // exitMenuItem listener
        getExitMenuItem().
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  exitMenuEventHandler();
              }
          });

        // HelpTopics listener
        getHelpTopicsMenuItem().
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  TopComponents.getInstance().getMainWindow().showHelpTopics(e);
              }
          });

        pack();

        getLogPane().onConnect();
        getLogPane().refreshLogs();
    }

    /**
     * Clean up the resources of the window when the user exits the window.
     */
    private void exitMenuEventHandler() {
        dispose();
    }

    /**
     * Return frameContentPane property value
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
     * Return clusterWindowMenuBar property value
     *
     * @return JMenuBar
     */
    private JMenuBar getClusterWindowMenuBar() {
        if (clusterWindowMenuBar == null) {
            clusterWindowMenuBar = new JMenuBar();
            clusterWindowMenuBar.add(getFileMenu());
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
            Collection extraActions = strategy.getExtraFileMenuActions();
            if (extraActions != null) {
                for (Iterator i = extraActions.iterator(); i.hasNext();) {
                    Action action = (Action)i.next();
                    fileMenu.add(new JMenuItem(action));
                }
            }
            fileMenu.add(getExitMenuItem());
            int mnemonic = fileMenu.getText().toCharArray()[0];
            fileMenu.setMnemonic(mnemonic);
        }
        return fileMenu;
    }

    /**
     * Return helpMenu property value
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
     * Return helpTopicsMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getHelpTopicsMenuItem() {
        if (helpTopicsMenuItem == null) {
            helpTopicsMenuItem = new JMenuItem();
            helpTopicsMenuItem.setText(resapplication.getString("Help_TopicsMenuItem_text_name"));
            int mnemonic = helpTopicsMenuItem.getText().toCharArray()[0];
            helpTopicsMenuItem.setMnemonic(mnemonic);
            helpTopicsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        }
        return helpTopicsMenuItem;
    }

    /**
     * Return exitMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getExitMenuItem() {
        if (exitMenuItem == null) {
            exitMenuItem = new JMenuItem();
            exitMenuItem.setText(resapplication.getString("ExitMenuItem.name"));
            int mnemonic = 'X';
            exitMenuItem.setMnemonic(mnemonic);
            exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return exitMenuItem;
    }

    /**
     * Return mainPane property value
     *
     * @return JPanel
     */
    private JPanel getMainPane() {
        if (mainPane != null) return mainPane;

        mainPane = new javax.swing.JPanel();
        gatewayLogPane = new javax.swing.JPanel();
        gatewayLogTitle = new javax.swing.JLabel();

        mainPane.setLayout(new java.awt.BorderLayout());

        gatewayLogTitle.setFont(new java.awt.Font("Dialog", 1, 18));
        gatewayLogTitle.setText("  " + strategy.getPanelTitle());
        gatewayLogTitle.setMaximumSize(new java.awt.Dimension(136, 40));
        gatewayLogTitle.setMinimumSize(new java.awt.Dimension(136, 40));
        gatewayLogTitle.setPreferredSize(new java.awt.Dimension(136, 40));

        gatewayLogPane.setLayout(new java.awt.BorderLayout());
        gatewayLogPane.add(gatewayLogTitle);

        mainPane.add(gatewayLogPane, java.awt.BorderLayout.NORTH);
        mainPane.add(getLogPane(), java.awt.BorderLayout.CENTER);

        return mainPane;
    }

    /**
     * Return logPane property value
     *
     * @return LogPanel
     */
    public LogPanel getLogPane() {
        if (logPane != null) return logPane;

        logPane = new LogPanel(strategy.getLogAdminLocator());
        return logPane;
    }

    public void flushCachedLogs() {
        getLogPane().clearMsgTable();
    }

    /**
     * Intialization when the connection to the server is established.
     */
    public void onLogon(LogonEvent e) {
        getLogPane().onConnect();
    }

    /**
     * Clean up the resources when the connection to the server went down.
     */
    public void onLogoff(LogonEvent e) {
        getLogPane().onDisconnect();
    }

    public void dispose() {
        getLogPane().getLogsRefreshTimer().stop();
        super.dispose();
    }

}
