/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 03, 2005<br/>
 */
package com.l7tech.console;

import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gui.util.*;
import com.l7tech.console.action.DeleteAuditEventsAction;
import com.l7tech.console.action.DownloadAuditEventsAction;
import com.l7tech.console.action.StartAuditArchiverAction;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.AuditMessage;
import com.l7tech.console.util.AbstractAuditMessage;
import com.l7tech.console.util.AuditHeaderMessage;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * To display audit records.
 *
 * @author flascelles@layer7-tech.com, $Author$
 * @version $Revision$
 */
public class GatewayAuditWindow extends JFrame implements LogonListener, SheetHolder {

    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static final String WINDOW_TITLE = "Policy Manager - Gateway Audit Events";
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
    private JMenuItem downloadAuditMenuItem = null;
    private JMenuItem deleteAuditEventsMenuItem = null;
    private JMenuItem startAuditArchiverMenuItem = null;
    private JPanel frameContentPane = null;
    private LogPanel logPane = null;
    private boolean startConnected;
    private SsmPreferences preferences = TopComponents.getInstance().getPreferences();

    private static final Logger logger = Logger.getLogger(GatewayAuditWindow.class.getName());
    private static final ResourceBundle resapplication = ResourceBundle.getBundle("com.l7tech.console.resources.console");

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
        super(WINDOW_TITLE);
        startConnected = connected;
        ImageIcon smallIcon =
            new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/CA_Logo_Black_16x16.png"));
        ImageIcon largeIcon =
            new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/CA_Logo_Black_32x32.png"));
        setIconImages(Arrays.asList(smallIcon.getImage(), largeIcon.getImage()));
        setJMenuBar(getClusterWindowMenuBar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);

        Utilities.setEscKeyStrokeDisposes(this);

        // saveMenuItem listener
        getSaveMenuItem().
                addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                  saveMenuEventHandler();
              }
          });

        // exitMenuItem listener
        getExitMenuItem().
          addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                  exitMenuEventHandler();
              }
          });

        // view menu
        getViewMenu().
           addMenuListener(new MenuListener(){
              @Override
              public void menuCanceled(MenuEvent e) {}
              @Override
              public void menuDeselected(MenuEvent e) {}

              @Override
              public void menuSelected(MenuEvent e) {
                  getViewControlsMenuItem().setSelected(getLogPane().getControlsExpanded());
                  getViewDetailsMenuItem().setSelected(getLogPane().getDetailsExpanded());
              }
          });

        // view controls
        getViewControlsMenuItem().
          addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                  viewControlsMenuEventHandler(getViewControlsMenuItem().isSelected());
              }
          });

        // view details
        getViewDetailsMenuItem().
          addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                  viewDetailsMenuEventHandler(getViewDetailsMenuItem().isSelected());
              }
          });

        // view refresh
        getViewRefreshMenuItem().
          addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                  viewRefreshMenuEventHandler();
              }
          });

        // HelpTopics listener
        getHelpTopicsMenuItem().
          addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                  TopComponents.getInstance().showHelpTopics();
              }
          });

        // listen for source via lookup button value change
        getLogPane().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("source_vialookup")){
                    updateFileActions((Boolean)evt.getNewValue());
                }
            }
        });

        pack();

        if (startConnected) {
            getLogPane().setControlsExpanded(true);
        } else {
            getLogPane().onDisconnect();
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitMenuEventHandler();
            }
        });

        // Load the last window status (size and location).
        Utilities.restoreWindowStatus(this, preferences.asProperties(), 1050, 750);
    }

    private void updateFileActions(boolean isViaLookupSelected) {
        getDownloadAuditEventsMenuItem().setEnabled(!isViaLookupSelected);
        getDeleteAuditEventsMenuItem().setEnabled(!isViaLookupSelected);
        getStartAuditArchiverMenuItem().setEnabled(!isViaLookupSelected);
    }

    /**
     * Display the given audits.
     *
     * @param auditFile the audit records to display.
     * @throws IOException on error
     */
    public boolean displayAudits(File auditFile) throws IOException {
        setTitle(WINDOW_TITLE + " (" + auditFile.getName() + ")");
        getViewControlsMenuItem().setEnabled(false);
        return getLogPane().importView(auditFile);
    }

    /**
     * Display audit events around the given date.
     *
     * <p>This will attempt to display audits relevant to the requested time.</p>
     *
     * @param date the date for records to display.
     */
    public void displayAudits(Date date) {
        LogPanel logPanel = getLogPane();
        logPanel.setSelectionDetails(date, -6);
    }

    /**
     * Displays the given audit records. Old display is cleared first.
     *
     * @param auditRecords  audit records to display
     */
    public void displayAudits(Collection<AuditRecord> auditRecords) {
        displayAudits( auditRecords, new Functions.Unary<AbstractAuditMessage,AuditRecord>(){
            @Override
            public AbstractAuditMessage call(final AuditRecord auditRecord) {
                return new AuditMessage( auditRecord );
            }
        } );
    }

    /**
     * Displays the given audit records. Old display is cleared first.
     *
     * @param auditRecordHeaders  audit records to display
     */
    public void displayAuditHeaders(Collection<AuditRecordHeader> auditRecordHeaders) {
        displayAudits( auditRecordHeaders, new Functions.Unary<AbstractAuditMessage,AuditRecordHeader>(){
            @Override
            public AbstractAuditMessage call(final AuditRecordHeader auditRecordHeader) {
                return new AuditHeaderMessage( auditRecordHeader );
            }
        } );
    }

    /**
     * Displays the given audit info. Old display is cleared first.
     */
    private <T> void displayAudits( final Collection<T> auditDataCollection,
                                    final Functions.Unary<AbstractAuditMessage, T> logMessageBuilder ) {
        // Constructs mapping from gateway node ID to name.
        final Map<String, String> nodeIdNames = new HashMap<String, String>();
        final Registry registry = Registry.getDefault();
        if (registry.isAdminContextPresent()) {
            try {
                final ClusterNodeInfo[] nodeInfos = registry.getClusterStatusAdmin().getClusterStatus();
                for (ClusterNodeInfo nodeInfo : nodeInfos) {
                    nodeIdNames.put(nodeInfo.getNodeIdentifier(), nodeInfo.getName());
                }
            } catch (FindException e) {
                // Leave the map empty.
            }
        }
        // else Leave the map empty.

        // Converts flat collection to Map.
        final Map<Goid, AbstractAuditMessage> logs = new HashMap<Goid, AbstractAuditMessage>();
        for ( T auditData : auditDataCollection ) {
            final AbstractAuditMessage logMessage = logMessageBuilder.call( auditData );
            final String nodeId = logMessage.getNodeId();
            // Needs to set gateway node name manually:
            String nodeName = nodeIdNames.get(nodeId);
            if (nodeName != null) {     // Node has been removed from cluster. To be consistent w/the full blown audit viewer do not show
                logMessage.setNodeName(nodeName);
            }
            logs.put(logMessage.getMsgNumber(), logMessage);
        }
        getLogPane().setLogs(logs);
        getViewControlsMenuItem().setEnabled(false);
    }

    /**
     * Clean up the resources of the window when the user exits the window.
     */
    private void exitMenuEventHandler() {
        try {
            Properties prop = Utilities.getWindowStatus(this);
            preferences.updateFromProperties(prop, true);
            preferences.store();
            getLogPane().onDisconnect();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to save divider location.", e);
        }

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
            frameContentPane.setPreferredSize(new Dimension(950, 600));//TODO this is where you change the size!!!
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
                @Override
                public void actionPerformed(ActionEvent e) {
                    getLogPane().refreshView();
                }
            });

            // Note that if you alter the menu item order you may need to
            // change the onLogon/onLogoff functions that use menu item
            // indexes to enable and disable items.
            //
            if(startConnected) {
                fileMenu.add(getDownloadAuditEventsMenuItem());
                fileMenu.add(getDeleteAuditEventsMenuItem());
                fileMenu.add(getStartAuditArchiverMenuItem());
                fileMenu.addSeparator();
            }
            fileMenu.add(getSaveMenuItem());
            fileMenu.addSeparator();
            fileMenu.add(getExitMenuItem());
            int mnemonic = fileMenu.getText().toCharArray()[0];
            fileMenu.setMnemonic(mnemonic);

            updateFileActions(getLogPane().getViaAuditLookupPolicyRadioButton().isSelected());
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

    private JMenuItem getDownloadAuditEventsMenuItem() {
        if(downloadAuditMenuItem == null){
            downloadAuditMenuItem =  new JMenuItem(new DownloadAuditEventsAction());
        }
        return downloadAuditMenuItem;
    }
    private JMenuItem getDeleteAuditEventsMenuItem() {
        if(deleteAuditEventsMenuItem == null){
            deleteAuditEventsMenuItem =  new JMenuItem(new DeleteAuditEventsAction());
        }
        return deleteAuditEventsMenuItem;
    }
    private JMenuItem getStartAuditArchiverMenuItem() {
        if(startAuditArchiverMenuItem == null){
            startAuditArchiverMenuItem =  new JMenuItem(new StartAuditArchiverAction());
        }
        return startAuditArchiverMenuItem;
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
        JPanel gatewayLogPane = new JPanel();
        mainPane.setLayout(new java.awt.BorderLayout());
        gatewayLogPane.setLayout(new java.awt.BorderLayout());
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

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(visible) {
            if(startConnected) {
                getLogPane().onConnect();
            }
        } else {
            // Let inactivity timeout start counting after this window is closed.
            TopComponents.getInstance().updateLastActivityTime();
        }
    }

    /**
     * Intialization when the connection to the server is established.
     */
    @Override
    public void onLogon(LogonEvent e) {
        getFileMenu().getItem(0).setEnabled(true);
        getFileMenu().getItem(1).setEnabled(true);
        getViewMenu().getItem(3).setEnabled(true);
        getLogPane().onConnect();
    }

    /**
     * Clean up the resources when the connection to the server went down.
     */
    @Override
    public void onLogoff(LogonEvent e) {
        getFileMenu().getItem(0).setEnabled(false);
        getFileMenu().getItem(1).setEnabled(false);
        getViewMenu().getItem(3).setEnabled(false);
        getLogPane().onDisconnect();
    }

    @Override
    public void dispose() {
        if (logPane != null) {
            logPane.getLogsRefreshTimer().stop();
        }
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
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                int numRecords = logPane.getMsgTable().getRowCount();
                if (numRecords > 1000) {
                    DialogDisplayer.showMessageDialog(GatewayAuditWindow.this, null,
                            "Audit Viewer cannot save more than 1000 records.  Please narrow your search.", null);
                } else {
                    doSave(fc);
                }
            }
        });
    }

    private void doSave(final JFileChooser fc) {
        fc.setDialogTitle("Save audit data as ...");
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return  f.isDirectory() || f.getName().toLowerCase().endsWith(".ssga");
            }
            @Override
            public String getDescription() {
                return "(*.ssga) Gateway Audit data file.";
            }
        };
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final String suggestedName = "Gateway_Audit_" + sdf.format(new Date()) + ".ssga";
        fc.setSelectedFile(new File(suggestedName));
        fc.addChoosableFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        fc.addActionListener(new ActionListener() {
            @Override
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

                //if user did not append .ssga extension, we'll append to it
                if (!file.getName().endsWith(".ssga")){
                    file = new File(file.toString() + ".ssga");
                }

                //
                // Can't check parent is writable due to JDK bug (see bug 2349 for info)
                //
                if((!file.exists() && file.getParentFile()!=null /*&& file.getParentFile().canWrite()*/) ||
                   (file.isFile() && file.canWrite())) {
                    try {
                        logPane.exportView(file);
                    }
                    catch(IOException ioe) {
                        file.delete(); // attempt to clean up
                        DialogDisplayer.showMessageDialog(GatewayAuditWindow.this, null,
                                "Error writing to file:\n'" + file.getAbsolutePath() + "'.", null);
                    }
                }
                else {
                    DialogDisplayer.showMessageDialog(GatewayAuditWindow.this, null,
                            "Cannot write to file:\n'" + file.getAbsolutePath() + "'.", null);
                }
            }
        }
    }

    @Override
    public void showSheet(JInternalFrame sheet) {
        DialogDisplayer.showSheet(this, sheet);
    }
}
