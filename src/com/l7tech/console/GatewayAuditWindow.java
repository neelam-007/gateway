/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 03, 2005<br/>
 */
package com.l7tech.console;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.console.action.Actions;
import com.l7tech.console.action.DeleteAuditEventsAction;
import com.l7tech.console.action.DownloadAuditEventsAction;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import java.util.Date;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * To display audit records.
 *
 * @author flascelles@layer7-tech.com, $Author$
 * @version $Revision$
 */
public class GatewayAuditWindow extends JFrame implements LogonListener {

    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private JLabel gatewayLogTitle = null;
    private JPanel gatewayLogPane = null;
    private JPanel mainPane = null;
    private JMenuBar clusterWindowMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu viewMenu = null;
    private JMenu helpMenu = null;
    private JMenuItem saveMenuItem = null;
    private JMenuItem exitMenuItem = null;
    private JCheckBoxMenuItem viewControlsMenuItem = null;
    private JCheckBoxMenuItem viewDetailsMenuItem = null;
    private JMenuItem viewRefreshMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;
    private JPanel frameContentPane = null;
    private LogPanel logPane = null;
    private boolean startConnected;

    private static
    ResourceBundle resapplication =
      java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    /**
     * Constructor
     */
    public GatewayAuditWindow() {
        this(true);
    }

    /**
     * Constructor
     */
    public GatewayAuditWindow(boolean connected) {
        super("SecureSpan Manager - Gateway Audit Events");
        startConnected = connected;
        ImageIcon imageIcon =
          new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png"));
        setIconImage(imageIcon.getImage());
        setBounds(0, 0, 850, 600);
        setJMenuBar(getClusterWindowMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);

        Actions.setEscKeyStrokeDisposes(this);

        // saveMenuItem listener
        getSaveMenuItem().
                addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  saveMenuEventHandler();
              }
          });

        // exitMenuItem listener
        getExitMenuItem().
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  exitMenuEventHandler();
              }
          });

        // view menu
        getViewMenu().
           addMenuListener(new MenuListener(){
              public void menuCanceled(MenuEvent e) {}
              public void menuDeselected(MenuEvent e) {}

              public void menuSelected(MenuEvent e) {
                  getViewControlsMenuItem().setSelected(getLogPane().getControlsExpanded());
                  getViewDetailsMenuItem().setSelected(getLogPane().getDetailsExpanded());
              }
          });

        // view controls
        getViewControlsMenuItem().
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  viewControlsMenuEventHandler(getViewControlsMenuItem().isSelected());
              }
          });

        // view details
        getViewDetailsMenuItem().
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  viewDetailsMenuEventHandler(getViewDetailsMenuItem().isSelected());
              }
          });

        // view refresh
        getViewRefreshMenuItem().
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  viewRefreshMenuEventHandler();
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

        if(!startConnected) getLogPane().onDisconnect();
    }

    /**
     * Display the given audits.
     *
     * @param auditFile the audit records to display.
     * @throws IOException on error
     */
    public boolean displayAudits(File auditFile) throws IOException {
        return getLogPane().importView(auditFile);
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
            clusterWindowMenuBar.add(getViewMenu());
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
            final DeleteAuditEventsAction deleteAuditEventsAction = new DeleteAuditEventsAction();

            deleteAuditEventsAction.setChainAction(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    getLogPane().refreshView();
                }
            });

            // Note that if you alter the menu item order you may need to
            // change the onLogon/onLogoff functions that use menu item
            // indexes to enable and disable items.
            //
            if(startConnected) {
                fileMenu.add(new JMenuItem(new DownloadAuditEventsAction()));
                fileMenu.add(new JMenuItem(deleteAuditEventsAction));
                fileMenu.addSeparator();
            }
            fileMenu.add(getSaveMenuItem());
            fileMenu.addSeparator();
            fileMenu.add(getExitMenuItem());
            int mnemonic = fileMenu.getText().toCharArray()[0];
            fileMenu.setMnemonic(mnemonic);
        }
        return fileMenu;
    }

    /**
     * Return viewMenu property value
     *
     * @return JMenu
     */
    private JMenu getViewMenu() {
        if (viewMenu != null) return viewMenu;

        viewMenu = new JMenu();
        viewMenu.setText(resapplication.getString("View"));
        viewMenu.add(getViewControlsMenuItem());
        viewMenu.add(getViewDetailsMenuItem());

        // Note that if you alter the menu item order you may need to
        // change the onLogon/onLogoff functions that use menu item
        // indexes to enable and disable items.
        //
        if(startConnected) {
            viewMenu.addSeparator();
            viewMenu.add(getViewRefreshMenuItem());
        }
        int mnemonic = viewMenu.getText().toCharArray()[0];
        viewMenu.setMnemonic(mnemonic);

        return viewMenu;
    }

    private JCheckBoxMenuItem getViewControlsMenuItem() {
        if (viewControlsMenuItem == null) {
            viewControlsMenuItem = new JCheckBoxMenuItem();
            viewControlsMenuItem.setText(resapplication.getString("View_ControlsMenuItem_text_name"));
            int mnemonic = viewControlsMenuItem.getText().toCharArray()[0];
            viewControlsMenuItem.setMnemonic(mnemonic);
            viewControlsMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return viewControlsMenuItem;
    }

    private JCheckBoxMenuItem getViewDetailsMenuItem() {
        if (viewDetailsMenuItem == null) {
            viewDetailsMenuItem = new JCheckBoxMenuItem();
            viewDetailsMenuItem.setText(resapplication.getString("View_DetailsMenuItem_text_name"));
            int mnemonic = viewDetailsMenuItem.getText().toCharArray()[0];
            viewDetailsMenuItem.setMnemonic(mnemonic);
            viewDetailsMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return viewDetailsMenuItem;
    }

    private JMenuItem getViewRefreshMenuItem() {
        if (viewRefreshMenuItem == null) {
            viewRefreshMenuItem = new JMenuItem();
            viewRefreshMenuItem.setText(resapplication.getString("Refresh_MenuItem_text"));
            Icon icon = new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/Refresh16.gif"));
            viewRefreshMenuItem.setIcon(icon);
            viewRefreshMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        }
        return viewRefreshMenuItem;
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
     * Get the save as menu item.
     *
     * @return JMenuItem for saving as
     */
    private JMenuItem getSaveMenuItem() {
        if (saveMenuItem == null) {
            saveMenuItem = new JMenuItem();
            saveMenuItem.setText(resapplication.getString("SaveAsMenuItem.name"));
            int mnemonic = saveMenuItem.getText().toCharArray()[0];
            saveMenuItem.setMnemonic(mnemonic);
            saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return saveMenuItem;
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
        gatewayLogTitle.setText(" Audit Events");
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

        logPane = new LogPanel();
        return logPane;
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(visible) {
            if(startConnected) {
                getLogPane().onConnect();
            }
        }
    }

    /**
     * Intialization when the connection to the server is established.
     */
    public void onLogon(LogonEvent e) {
        getFileMenu().getItem(0).setEnabled(true);
        getFileMenu().getItem(1).setEnabled(true);
        getViewMenu().getItem(3).setEnabled(true);
        getLogPane().onConnect();
    }

    /**
     * Clean up the resources when the connection to the server went down.
     */
    public void onLogoff(LogonEvent e) {
        getFileMenu().getItem(0).setEnabled(false);
        getFileMenu().getItem(1).setEnabled(false);
        getViewMenu().getItem(3).setEnabled(false);
        getLogPane().onDisconnect();
    }

    public void dispose() {
        getLogPane().getLogsRefreshTimer().stop();
        super.dispose();
    }

    private void viewControlsMenuEventHandler(boolean visible) {
        getLogPane().setControlsExpanded(visible);
    }

    private void viewDetailsMenuEventHandler(boolean visible) {
        getLogPane().setDetailsExpanded(visible);
    }

    private void viewRefreshMenuEventHandler() {
        getLogPane().refreshView();
    }

    /**
     * Save displayed audit records to file
     */
    private void saveMenuEventHandler() {
        // File requestor
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final JFileChooser fc = Utilities.createJFileChooser();
        fc.setDialogTitle("Save audit data as ...");
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File f) {
                return  f.isDirectory() || f.getName().toLowerCase().endsWith(".ssga");
            }
            public String getDescription() {
                return "(*.ssga) SecureSpan Gateway Audit data file.";
            }
        };
        final String suggestedName = "SecureSpanGateway_Audit_" +sdf.format(new Date()) + ".ssga";
        fc.setSelectedFile(new File(suggestedName));
        fc.addChoosableFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        fc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(e.getActionCommand())) {
                    fc.setSelectedFile(new File(suggestedName));
                }
            }
        });
        int r = fc.showDialog(GatewayAuditWindow.this, "Save");
        if(r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if(file!=null) {
                if((!file.exists() && file.getParentFile()!=null && file.getParentFile().canWrite()) ||
                   (file.isFile() && file.canWrite())) {
                    try {
                        logPane.exportView(file);
                    }
                    catch(IOException ioe) {
                        file.delete(); // attempt to clean up
                        ExceptionDialog d = ExceptionDialog.createExceptionDialog(
                                GatewayAuditWindow.this,
                                "SecureSpan Manager - Error",
                                "Error writing to file:\n'"+file.getAbsolutePath()+"'.",
                                ioe,
                                Level.WARNING);
                        d.pack();
                        Utilities.centerOnScreen(d);
                        d.setVisible(true);
                    }
                }
                else {
                    ExceptionDialog d = ExceptionDialog.createExceptionDialog(
                            GatewayAuditWindow.this,
                            "SecureSpan Manager - Error",
                            "Cannot write to file:\n'"+file.getAbsolutePath()+"'.",
                            null,
                            Level.INFO);
                    d.pack();
                    Utilities.centerOnScreen(d);
                    d.setVisible(true);
                }
            }
        }
    }
}
