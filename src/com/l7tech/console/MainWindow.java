package com.l7tech.console;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Locator;
import com.l7tech.console.action.*;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.panels.FindIdentitiesDialog;
import com.l7tech.console.panels.LogonDialog;
import com.l7tech.console.panels.PreferencesDialog;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.security.LogonEvent;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.identity.IdentitiesRootNode;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.tree.policy.PolicyToolBar;
import com.l7tech.console.util.*;

import javax.help.DefaultHelpBroker;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The console main window <CODE>MainWindow</CODE> class.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class MainWindow extends JFrame {
    static Logger log = Logger.getLogger(MainWindow.class.getName());
    /**
     * the resource path for the application
     */
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";

    /**
     * the path to JavaHelp helpset file
     */
    public static final String HELP_PATH = "com/l7tech/console/resources/helpset/SecureSpan_Manager_Help_System.hs";

    public static final int MAIN_SPLIT_PANE_DIVIDER_SIZE = 10;
    public static final String CONNECTION_PREFIX = " [connected to node: ";
    /**
     * the resource bundle name
     */
    private static
    ResourceBundle resapplication = ResourceBundle.getBundle("com.l7tech.console.resources.console");

    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();

    private JMenuBar mainJMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu editMenu = null;
    private JMenu viewMenu = null;
    private JMenu helpMenu = null;
    private JMenu newProviderSubMenu = null;

    private JMenuItem connectMenuItem = null;
    private JMenuItem disconnectMenuItem = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem menuItemPref = null;
    private JMenuItem logMenuItem = null;
    private JMenuItem auditMenuItem = null;
    private JMenuItem statMenuItem = null;
    private JMenuItem manageJmsEndpointsMenuItem = null;
    private JMenuItem manageCertificatesMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;

    // actions
    private Action refreshAction = null;
    private Action findAction = null;
    private Action prefsAction = null;
    private Action removeNodeAction = null;
    private Action connectAction = null;
    private Action disconnectAction = null;
    private Action toggleStatusBarAction = null;
    private Action togglePolicyMessageArea = null;
    private PublishServiceAction publishServiceAction = null;
    private PublishNonSoapServiceAction publishNonSoapServiceAction = null;
    private CreateServiceWsdlAction createServiceAction = null;
    private ViewGatewayLogsAction viewGatewayLogWindowAction = null;
    private ViewClusterStatusAction viewClusterStatusAction = null;
    private ValidatePolicyAction validatePolicyAction;
    private ExportPolicyToFileAction exportPolicyAction;
    private ImportPolicyFromFileAction importPolicyAction;
    private SavePolicyAction savePolicyAction;
    private ViewGatewayAuditsAction viewGatewayAuditsWindowAction;
    private ManageJmsEndpointsAction manageJmsEndpointsAction = null;
    private HomeAction homeAction = new HomeAction();
    Action monitorAction = null;
    private NewGroupAction newInernalGroupAction;
    private NewLdapProviderAction newLDAPProviderAction;
    private NewFederatedIdentityProviderAction newPKIProviderAction;
    private ManageCertificatesAction manageCertificatesAction = null;
    private NewInternalUserAction newInernalUserAction;


    private JPanel frameContentPane = null;
    private JPanel mainPane = null;
    private JPanel statusBarPane = null;
    private JLabel statusMsgLeft = null;
    private JLabel statusMsgRight = null;


    private JToolBar toolBarPane = null;
    private PolicyToolBar policyToolBar = null;
    private JSplitPane mainSplitPane = null;
    private JPanel mainLeftPanel = null;
    private JPanel mainSplitPaneRight = null;

    /* progress bar indicator */
    private ProgressBar progressBar = null;

    public static final String TITLE = "SSG Management Console";
    public static final String NAME = "main.window"; // registered
    private EventListenerList listenerList = new WeakEventListenerList();
    // cached credential manager
    private String connectionContext = "";
    private FocusAdapter actionsFocusListener;
    private ServicesTree servicesTree;
    private IdentityProvidersTree identityProvidersTree;
    private JMenuItem validateMenuItem;
    private JMenuItem importMenuItem;
    private JMenuItem exportMenuItem;
    private JMenuItem saveMenuItem;
    private boolean disconnected = false;
    private String ssgURL;
    private SsmApplication ssmApplication;
    private IdentitiesRootNode identitiesRootNode;
    private ServicesFolderNode servicesRootNode;

    /**
     * MainWindow constructor comment.
     */
    public MainWindow(SsmApplication app) {
        super(TITLE);
        ssmApplication = app;
        initialize();
    }

    /**
     * add the ConnectionListener
     *
     * @param listener the ConnectionListener
     */
    public void addLogonListener(LogonListener listener) {
        listenerList.add(LogonListener.class, listener);
    }

    /**
     * remove the the ConnectionListener
     *
     * @param listener the ConnectionListener
     */
    public void removeLogonListener(LogonListener listener) {
        listenerList.remove(LogonListener.class, listener);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type (connection event).
     */
    private void fireConnected() {
        LogonEvent event = new LogonEvent(this, LogonEvent.LOGON);
        EventListener[] listeners = listenerList.getListeners(LogonListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((LogonListener)listeners[i]).onLogon(event);
        }
        disconnected = false;
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type (connection event).
     */
    private void fireDisconnected() {
        LogonEvent event = new LogonEvent(this, LogonEvent.LOGOFF);
        EventListener[] listeners = listenerList.getListeners(LogonListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((LogonListener)listeners[i]).onLogoff(event);
        }
        disconnected = true;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    /**
     * The SSG URL this ssm is connected to (excluding the /ssg suffix).
     *
     * @return
     */
    public String ssgURL() {
        return ssgURL;
    }

    /**
     * Return the ConnectMenuItem property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getConnectMenuItem() {
        if (connectMenuItem == null) {
            connectMenuItem = new JMenuItem(getConnectAction());
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/connect2.gif"));
            connectMenuItem.setIcon(icon);
            int mnemonic = connectMenuItem.getText().toCharArray()[0];
            connectMenuItem.setMnemonic(mnemonic);
            connectMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return connectMenuItem;
    }

    /**
     * Return the DisconnectMenuItem property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getDisconnectMenuItem() {
        if (disconnectMenuItem != null)
            return disconnectMenuItem;

        disconnectMenuItem = new JMenuItem(getDisconnectAction());
        disconnectMenuItem.setFocusable(false);
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/disconnect.gif"));
        disconnectMenuItem.setIcon(icon);

        int mnemonic = disconnectMenuItem.getText().toCharArray()[0];
        disconnectMenuItem.setMnemonic(mnemonic);
        disconnectMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));

        return disconnectMenuItem;
    }


    /**
     * Return the menuItemPref property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getMenuItemPreferences() {
        if (menuItemPref == null) {
            menuItemPref = new JMenuItem(getPreferencesAction());
            menuItemPref.setFocusable(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/preferences.gif"));
            menuItemPref.setIcon(icon);
            int mnemonic = menuItemPref.getText().toCharArray()[0];
            menuItemPref.setMnemonic(mnemonic);
            menuItemPref.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return menuItemPref;
    }


    /**
     * Return the ExitMenuItem property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getExitMenuItem() {
        if (exitMenuItem == null) {
            exitMenuItem = new JMenuItem();
            exitMenuItem.setFocusable(false);
            exitMenuItem.setText(resapplication.getString("ExitMenuItem.name"));
            exitMenuItem.setToolTipText(resapplication.getString("ExitMenuItem.desc"));
            int mnemonic = 'X';
            exitMenuItem.setMnemonic(mnemonic);
            exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return exitMenuItem;
    }


    /**
     * Return the helpTopicsMenuItem property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getHelpTopicsMenuItem() {
        if (helpTopicsMenuItem == null) {
            helpTopicsMenuItem = new JMenuItem();
            helpTopicsMenuItem.setFocusable(false);
            helpTopicsMenuItem.setText(resapplication.getString("Help_TopicsMenuItem_text_name"));
            helpTopicsMenuItem.setToolTipText(resapplication.getString("Help_TopicsMenuItem_text_description"));
            int mnemonic = helpTopicsMenuItem.getText().toCharArray()[0];
            helpTopicsMenuItem.setMnemonic(mnemonic);
            helpTopicsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        }
        return helpTopicsMenuItem;
    }


    /**
     * Return the fileMenu property value.
     *
     * @return JMenu
     */
    private JMenu getFileMenu() {
        if (fileMenu == null) {
            fileMenu = new JMenu();
            fileMenu.setFocusable(false);
            fileMenu.setText(resapplication.getString("File"));

            fileMenu.add(getSaveMenuItem());
            fileMenu.add(getExportMenuItem());
            fileMenu.add(getImportMenuItem());
            fileMenu.add(getValidateMenuItem());

            fileMenu.addSeparator();

            fileMenu.add(getConnectMenuItem());
            fileMenu.add(getDisconnectMenuItem());
            fileMenu.add(getMenuItemPreferences());
            fileMenu.add(getExitMenuItem());
            int mnemonic = fileMenu.getText().toCharArray()[0];
            fileMenu.setMnemonic(mnemonic);
        }
        return fileMenu;
    }

    private JMenuItem getValidateMenuItem() {
        if (validateMenuItem == null) {
            validateMenuItem = new JMenuItem(getValidateAction());
            int mnemonic = validateMenuItem.getText().toCharArray()[0];
            validateMenuItem.setMnemonic(mnemonic);
            validateMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return validateMenuItem;
    }

    private BaseAction getValidateAction() {
        if (validatePolicyAction == null) {
            validatePolicyAction = new ValidatePolicyAction();
            validatePolicyAction.setEnabled(false);
            MainWindow.this.addLogonListener(validatePolicyAction);
        }
        return validatePolicyAction;
    }

    private JMenuItem getImportMenuItem() {
        if (importMenuItem == null) {
            importMenuItem = new JMenuItem(getImportPolicyAction());
            importMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/saveTemplate.gif"));
            importMenuItem.setIcon(icon);
            int mnemonic = importMenuItem.getText().toCharArray()[0];
            importMenuItem.setMnemonic(mnemonic);
            importMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return importMenuItem;
    }

    private JMenuItem getExportMenuItem() {
        if (exportMenuItem == null) {
            exportMenuItem = new JMenuItem(getExportPolicyAction());
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/saveTemplate.gif"));
            exportMenuItem.setIcon(icon);
            int mnemonic = exportMenuItem.getText().toCharArray()[0];
            exportMenuItem.setMnemonic(mnemonic);
            exportMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return exportMenuItem;
    }

    private BaseAction getExportPolicyAction() {
        if (exportPolicyAction == null) {
            exportPolicyAction = new ExportPolicyToFileAction();
            exportPolicyAction.setEnabled(false);
        }
        return exportPolicyAction;
    }

    private BaseAction getImportPolicyAction() {
        if (importPolicyAction == null) {
            importPolicyAction = new ImportPolicyFromFileAction();
            importPolicyAction.setEnabled(false);
        }
        return exportPolicyAction;
    }

    private JMenuItem getSaveMenuItem() {
        if (saveMenuItem == null) {
            saveMenuItem = new JMenuItem(getSaveAction());
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Save16.gif"));
            saveMenuItem.setIcon(icon);
            int mnemonic = saveMenuItem.getText().toCharArray()[0];
            saveMenuItem.setMnemonic(mnemonic);
            saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return saveMenuItem;
    }

    private BaseAction getSaveAction() {
        if (savePolicyAction == null) {
            savePolicyAction = new SavePolicyAction();
            savePolicyAction.setEnabled(false);
        }
        return savePolicyAction;
    }

    /**
     * Return the editMenu property value.
     *
     * @return JMenu
     */
    private JMenu getEditMenu() {
        if (editMenu == null) {
            editMenu = new JMenu();
            editMenu.setFocusable(false);
            editMenu.setText(resapplication.getString("Edit"));

            editMenu.add(getNewProviderSubMenu());
            editMenu.add(getNewInternalGroupAction());
            editMenu.add(getNewInternalUserAction());
            editMenu.add(getFindAction());

            editMenu.addSeparator();

            editMenu.add(getPublishServiceAction());
            editMenu.add(getCreateServiceAction());
            editMenu.add(getPublishNonSoapServiceAction());
            editMenu.addSeparator();

            editMenu.add(getManageCertificatesMenuItem());
            editMenu.add(getManageJmsEndpointsMenuItem());

            int mnemonic = editMenu.getText().toCharArray()[0];
            editMenu.setMnemonic(mnemonic);

            editMenu.addSeparator();
            editMenu.add(getStatMenuItem());
            editMenu.add(getLogMenuItem());
            editMenu.add(getAuditMenuItem());
        }
        return editMenu;
    }

    private JMenu getNewProviderSubMenu() {
        if (newProviderSubMenu == null) {
            newProviderSubMenu = new JMenu();
            newProviderSubMenu.setFocusable(false);

            newProviderSubMenu.setText("Create Identity Provider");
            newProviderSubMenu.setIcon(new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/CreateIdentityProvider16x16.gif")));

            newProviderSubMenu.add(getNewProviderAction());
            newProviderSubMenu.add(getNewFederatedIdentityProviderAction());
        }
        return newProviderSubMenu;
    }

    /**
     * Return the viewMenu property value.
     *
     * @return JMenu
     */
    private JMenu getViewMenu() {
        if (viewMenu != null) return viewMenu;

        viewMenu = new JMenu();
        viewMenu.setFocusable(false);
        viewMenu.setText(resapplication.getString("View"));
        JCheckBoxMenuItem jcm = new JCheckBoxMenuItem(getPolicyMessageAreaToggle());
        final Preferences preferences = Preferences.getPreferences();

        boolean policyMessageAreaVisible = preferences.isPolicyMessageAreaVisible();
        jcm.setSelected(policyMessageAreaVisible);
        viewMenu.add(jcm);

        jcm = new JCheckBoxMenuItem(getToggleStatusBarToggleAction());
        jcm.setSelected(preferences.isStatusBarBarVisible());
        viewMenu.add(jcm);

        viewMenu.addSeparator();

        JMenuItem item = new JMenuItem(getRefreshAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        viewMenu.add(item);

        int mnemonic = viewMenu.getText().toCharArray()[0];
        viewMenu.setMnemonic(mnemonic);


        return viewMenu;
    }

    /**
     * Return the helpMenu property value.
     *
     * @return JMenu
     */
    private JMenu getHelpMenu() {
        if (helpMenu != null) return helpMenu;

        helpMenu = new JMenu();
        helpMenu.setFocusable(false);
        helpMenu.setText(resapplication.getString("Help"));
        helpMenu.add(getHelpTopicsMenuItem());
        helpMenu.add(new AboutAction());
        int mnemonic = helpMenu.getText().toCharArray()[0];
        helpMenu.setMnemonic(mnemonic);

        return helpMenu;
    }


    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the connect <CODE>Action</CODE> implementation
     */
    private Action getConnectAction() {
        if (connectAction != null) return connectAction;
        String atext = resapplication.getString("ConnectMenuItem.name");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/connect2.gif"));
        String aDesc = resapplication.getString("ConnectMenuItem.desc");
        connectAction =
          new AbstractAction(atext, icon) {
              /**
               * Invoked when an action occurs.
               *
               * @param event the event that occured
               */
              public void actionPerformed(ActionEvent event) {
                  activateLogonDialog();
              }
          };
        connectAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return connectAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the disconnect <CODE>Action</CODE> implementation
     */
    private Action getDisconnectAction() {
        if (disconnectAction != null) return disconnectAction;
        String atext = resapplication.getString("DisconnectMenuItem.name");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/disconnect.gif"));

        String aDesc = resapplication.getString("DisconnectMenuItem.desc");
        disconnectAction =
          new AbstractAction(atext, icon) {
              /**
               * Invoked when an action occurs.
               *
               * @param event the event that occured
               */
              public void actionPerformed(ActionEvent event) {
                  try {
                      disconnectHandler(event);
                  } catch (ActionVetoException e) {
                      // action vetoed
                  }
              }
          };
        disconnectAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return disconnectAction;
    }

    /**
     * @return the <code>Action</code> for the publish service
     */
    private PublishServiceAction getPublishServiceAction() {
        if (publishServiceAction != null) {
            return publishServiceAction;
        }
        publishServiceAction = new PublishServiceAction();
        publishServiceAction.setEnabled(false);
        this.addLogonListener(publishServiceAction);
        return publishServiceAction;
    }

    private PublishNonSoapServiceAction getPublishNonSoapServiceAction() {
        if (publishNonSoapServiceAction != null) {
            return publishNonSoapServiceAction;
        }
        publishNonSoapServiceAction = new PublishNonSoapServiceAction();
        publishNonSoapServiceAction.setEnabled(false);
        this.addLogonListener(publishNonSoapServiceAction);
        return publishNonSoapServiceAction;
    }


    /**
     * @return the <code>Action</code> for the create service
     */
    private CreateServiceWsdlAction getCreateServiceAction() {
        if (createServiceAction != null) {
            return createServiceAction;
        }
        createServiceAction = new CreateServiceWsdlAction();
        createServiceAction.setEnabled(false);
        this.addLogonListener((CreateServiceWsdlAction)createServiceAction);
        return createServiceAction;
    }


    private NewInternalUserAction getNewInternalUserAction() {
        if (newInernalUserAction != null) return newInernalUserAction;
        newInernalUserAction = new NewInternalUserAction(null);
        MainWindow.this.addLogonListener(newInernalUserAction);
        newInernalUserAction.setEnabled(false);
        return newInernalUserAction;
    }

    private NewGroupAction getNewInternalGroupAction() {
        if (newInernalGroupAction != null) return newInernalGroupAction;
        newInernalGroupAction = new NewGroupAction(null);
        MainWindow.this.addLogonListener(newInernalGroupAction);
        newInernalGroupAction.setEnabled(false);
        return newInernalGroupAction;
    }

    private NewFederatedIdentityProviderAction getNewFederatedIdentityProviderAction() {
        if (newPKIProviderAction != null) return newPKIProviderAction;
        newPKIProviderAction = new NewFederatedIdentityProviderAction(null) {
            public void onLogon(LogonEvent e) {
                super.onLogon(e);
                final DefaultMutableTreeNode root =
                  (DefaultMutableTreeNode)getIdentitiesTree().getModel().getRoot();
                node = (AbstractTreeNode)root;
            }

        };

        MainWindow.this.addLogonListener(newPKIProviderAction);
        newPKIProviderAction.setEnabled(false);
        return newPKIProviderAction;
    }

    private NewLdapProviderAction getNewProviderAction() {
        if (newLDAPProviderAction != null) return newLDAPProviderAction;
        newLDAPProviderAction = new NewLdapProviderAction(null) {
            public void onLogon(LogonEvent e) {
                super.onLogon(e);
                final DefaultMutableTreeNode root =
                  (DefaultMutableTreeNode)getIdentitiesTree().getModel().getRoot();
                node = (AbstractTreeNode)root;
            }
        };
        MainWindow.this.addLogonListener(newLDAPProviderAction);
        newLDAPProviderAction.setEnabled(false);
        return newLDAPProviderAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that refreshes the tree
     */
    private Action getRefreshAction() {
        if (refreshAction != null) return refreshAction;
        String atext = resapplication.getString("Refresh_MenuItem_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Refresh16.gif"));

        refreshAction =
          new AbstractAction(atext, icon) {
              LogonListener listener = new LogonListener() {
                  public void onLogon(LogonEvent e) {
                      setEnabled(false);
                  }

                  /**
                   * Invoked on logoff
                   *
                   * @param e describing the logoff event
                   */
                  public void onLogoff(LogonEvent e) {
                      setEnabled(false);
                  }

              };

              {
                  MainWindow.this.addLogonListener(listener);
                  FocusListener fl = getActionsFocusListener();
                  getIdentitiesTree().addFocusListener(fl);
                  getServicesTree().addFocusListener(fl);
              }

              /**
               * Invoked when an action occurs.
               *
               * @param event the event that occured
               * @see Action#removePropertyChangeListener
               */
              public void actionPerformed(ActionEvent event) {
                  Collection alreadyRefreshed = new ArrayList();
                  // no matter what, if id provider tree exists, always refresh it
                  if (identityProvidersTree != null) {
                      identityProvidersTree.refresh(identitiesRootNode);
                      alreadyRefreshed.add(identityProvidersTree);
                  }
                  // no matter what, if service tree exists, always refresh it
                  if (servicesTree != null) {
                      servicesTree.refresh(servicesRootNode);
                      alreadyRefreshed.add(servicesTree);
                  }
                  // no matter what, always refresh the policy editor panel if it is showing
                  final WorkSpacePanel cw = TopComponents.getInstance().getCurrentWorkspace();
                  final JComponent jc = cw.getComponent();
                  if (jc != null && jc instanceof PolicyEditorPanel) {
                      PolicyEditorPanel pep = (PolicyEditorPanel)jc;
                      if (pep.getPolicyTree() != null) {
                          alreadyRefreshed.add(pep.getPolicyTree());
                          pep.getPolicyTree().refresh();
                          alreadyRefreshed.add(pep.getPolicyTree());
                      }
                  }
                  final KeyboardFocusManager kbm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                  final Component c = kbm.getFocusOwner();
                  log.finest("the focus owner is " + c.getClass());
                  if (c instanceof Refreshable) {

                      try {
                          Refreshable r = (Refreshable)c;
                          if (!alreadyRefreshed.contains(r)) {
                              if (r.canRefresh()) {
                                  log.finest("Invoke refresh on " + c.getClass());
                                  r.refresh();
                              }
                          }
                      } finally {
                          SwingUtilities.invokeLater(new Runnable() {
                              public void run() {
                                  if (kbm.getFocusOwner() != c) {
                                      c.requestFocusInWindow();
                                  }
                              }
                          });
                      }
                  }
              }
          };
        refreshAction.setEnabled(false);
        refreshAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return refreshAction;
    }

    /**
     * The focus listener that manages the enable/disable state of the
     * actions based on the currently focused component.
     *
     * @return the focuslistener
     */
    public FocusListener getActionsFocusListener() {
        if (actionsFocusListener != null) {
            return actionsFocusListener;
        }

        actionsFocusListener = new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                Object c = e.getSource();

                boolean enable = c instanceof Refreshable && ((Refreshable)c).canRefresh();
                refreshAction.setEnabled(enable);
                log.finest("focusGained " + c.getClass() + " setting refreshAction to " + enable);
            }

            public void focusLost(FocusEvent e) {
                Component c = e.getOppositeComponent();
                if (c == null) {
                    log.finest("focusLost, no component in focus setting refreshAction to " + false);
                    refreshAction.setEnabled(false);
                    return;
                }

                boolean enable = c instanceof Refreshable && ((Refreshable)c).canRefresh();
                log.finest("focusLost " + c.getClass() + " setting refreshAction to " + enable);
                refreshAction.setEnabled(enable);
            }
        };

        return actionsFocusListener;
    }


    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that toggles the status bar
     */
    private Action getToggleStatusBarToggleAction() {
        if (toggleStatusBarAction != null) return toggleStatusBarAction;

        String atext = resapplication.getString("toggle.status.bar.action.name");
        String aDesc = resapplication.getString("toggle.status.bar.action.desc");

        toggleStatusBarAction =
          new AbstractAction(atext) {
              /**
               * Invoked when an action occurs.
               *
               * @param event the event that occured
               * @see Action#removePropertyChangeListener
               */
              public void actionPerformed(ActionEvent event) {
                  JCheckBoxMenuItem item = (JCheckBoxMenuItem)event.getSource();
                  final boolean selected = item.isSelected();
                  getStatusBarPane().setVisible(selected);
                  try {
                      Preferences p = Preferences.getPreferences();
                      p.seStatusBarVisible(selected);
                      p.store();
                  } catch (IOException e) {
                      // ignore
                  }
              }
          };
        toggleStatusBarAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return toggleStatusBarAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that toggles the  policy message area
     */
    private Action getPolicyMessageAreaToggle() {
        if (togglePolicyMessageArea != null) return togglePolicyMessageArea;

        String atext = resapplication.getString("toggle.policy.msg.action.name");
        String aDesc = resapplication.getString("toggle.policy.msg.action.desc");

        togglePolicyMessageArea =
          new AbstractAction(atext) {
              /**
               * Invoked when an action occurs.
               *
               * @param event the event that occured
               * @see Action#removePropertyChangeListener
               */
              public void actionPerformed(ActionEvent event) {
                  JCheckBoxMenuItem item = (JCheckBoxMenuItem)event.getSource();
                  final boolean selected = item.isSelected();
                  final WorkSpacePanel cw = TopComponents.getInstance().getCurrentWorkspace();
                  final JComponent c = cw.getComponent();
                  if (c != null && c instanceof PolicyEditorPanel) {
                      PolicyEditorPanel pe = (PolicyEditorPanel)c;
                      pe.setMessageAreaVisible(selected);
                  }
                  try {
                      Preferences p = Preferences.getPreferences();
                      p.setPolicyMessageAreaVisible(selected);
                      p.store();
                  } catch (IOException e) {
                      // ignore
                  }
              }
          };
        togglePolicyMessageArea.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return togglePolicyMessageArea;
    }


    /**
     * create the Action (the component that is used by several
     * controls) that returns the 'find' action.
     *
     * @return the find <CODE>Action</CODE>
     */
    private Action getFindAction() {
        if (findAction != null) return findAction;

        FindIdentitiesDialog.Options options = new FindIdentitiesDialog.Options();
        options.enableDeleteAction();
        options.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        findAction = new FindIdentityAction(options);
        String aDesc = resapplication.getString("Find_MenuItem_text_description");
        findAction.putValue(Action.SHORT_DESCRIPTION, aDesc);

        return findAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that invokes the
     *         preferences dialog
     */
    private Action getPreferencesAction() {
        if (prefsAction != null) return prefsAction;
        String atext = resapplication.getString("PreferencesMenuItem.name");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/preferences.gif"));
        String aDesc = resapplication.getString("PreferencesMenuItem.desc");
        prefsAction =
          new AbstractAction(atext, icon) {
              /**
               * Invoked when an action occurs.
               */
              public void actionPerformed(ActionEvent event) {
                  PreferencesDialog dialog = new PreferencesDialog(MainWindow.this, false, isConnected());
                  dialog.pack();
                  Utilities.centerOnScreen(dialog);
                  dialog.setResizable(false);
                  dialog.setVisible(true);
              }
          };
        prefsAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return prefsAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that invokes the
     *         remove node action
     */
    private Action getRemoveNodeAction() {
        if (removeNodeAction != null) return removeNodeAction;
        String atext = resapplication.getString("Delete_MenuItem_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Delete16.gif"));
        removeNodeAction =
          new AbstractAction(atext, icon) {
              /**
               * Invoked when an action occurs.
               */
              public void actionPerformed(ActionEvent event) {
              }
          };
        removeNodeAction.putValue(Action.SHORT_DESCRIPTION, atext);
        removeNodeAction.setEnabled(false);
        return removeNodeAction;
    }

    /**
     * enable or disable menus and buttons, depending on the
     * connection status
     *
     * @param connected true if connected, false otherwise
     */
    private void toggleConnectedMenus(boolean connected) {
        getFindAction().setEnabled(connected);
        getDisconnectAction().setEnabled(connected);
        getConnectAction().setEnabled(!connected);
        homeAction.setEnabled(connected);

    }


    /**
     * Return the JFrameContentPane property value.
     *
     * @return JPanel
     */
    private JPanel getFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setBorder(null);
            frameContentPane.setPreferredSize(new Dimension(700, 600));
            frameContentPane.setLayout(new BorderLayout());
            frameContentPane.add(getToolBarPane(), "North");
            frameContentPane.add(getMainPane(), "Center");
        }
        return frameContentPane;
    }


    /**
     * Return the jJPanelEditor property value.
     *
     * @return JPanel
     */
    private JPanel getMainSplitPaneRight() {
        if (mainSplitPaneRight != null)
            return mainSplitPaneRight;

        mainSplitPaneRight = new JPanel();
        mainSplitPaneRight.setLayout(new GridBagLayout());
        mainSplitPaneRight.setBorder(null);
        return mainSplitPaneRight;
    }

    private WorkSpacePanel getWorkSpacePanel() {
        return TopComponents.getInstance().getCurrentWorkspace();
    }

    /**
     * Return the palette tree view property value.
     *
     * @return JTree
     */
    private JTree getAssertionPaletteTree() {
        JTree tree = (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        if (tree != null)
            return tree;
        tree = new AssertionsTree();
        tree.setShowsRootHandles(true);
        tree.setBorder(null);
        TopComponents.getInstance().registerComponent(AssertionsTree.NAME, tree);
        return tree;
    }

    /**
     * Return the identities tree.
     *
     * @return JTree
     */
    private JTree getIdentitiesTree() {
        JTree tree = (JTree)TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
        if (tree != null) return tree;
        identityProvidersTree = new IdentityProvidersTree();
        identityProvidersTree.setShowsRootHandles(true);
        identityProvidersTree.setBorder(null);
        TopComponents.getInstance().registerComponent(IdentityProvidersTree.NAME, identityProvidersTree);
        return identityProvidersTree;
    }

    /**
     * Return the JTreeView property value.
     *
     * @return JTree
     */
    private JTree getServicesTree() {
        JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
        if (tree != null)
            return tree;

        servicesTree = new ServicesTree();
        servicesTree.setShowsRootHandles(true);
        TopComponents.getInstance().registerComponent(ServicesTree.NAME, servicesTree);
        return servicesTree;
    }

    /**
     * Initialize the workspace. Invoked on successfull login.
     */
    private void initalizeWorkspace() {
        DefaultTreeModel treeModel = new FilteredTreeModel(null);
        final AbstractTreeNode paletteRootNode = new AssertionsPaletteRootNode("Policy Assertions");
        treeModel.setRoot(paletteRootNode);
        final JTree assertionPaletteTree = getAssertionPaletteTree();
        assertionPaletteTree.setRootVisible(true);
        assertionPaletteTree.setModel(treeModel);
        TreePath path = new TreePath(paletteRootNode.getPath());
        assertionPaletteTree.setSelectionPath(path);

        identitiesRootNode = new IdentitiesRootNode("Identity Providers");
        treeModel = new FilteredTreeModel(null);
        treeModel.setRoot(identitiesRootNode);

        final JTree identitiesTree = getIdentitiesTree();
        identitiesTree.setRootVisible(true);
        identitiesTree.setModel(treeModel);

        String rootTitle = "Web Services @ ";
        rootTitle +=
          Preferences.getPreferences().getString(Preferences.SERVICE_URL);
        DefaultTreeModel servicesTreeModel = new FilteredTreeModel(null);
        servicesRootNode = new ServicesFolderNode(Registry.getDefault().getServiceManager(), rootTitle);
        servicesTreeModel.setRoot(servicesRootNode);

        getServicesTree().setRootVisible(true);
        getServicesTree().setModel(servicesTreeModel);

        TreeSelectionListener treeSelectionListener =
          new TreeSelectionListener() {
              private final JTree assertionPalette =
                assertionPaletteTree;
              private final JTree services = getServicesTree();

              public void valueChanged(TreeSelectionEvent e) {
                  Object o = e.getSource();
                  if (o == assertionPalette) {
                      if (!isRemovePath(e)) {
                          log.finer("Clearing selection services");
                          services.clearSelection();
                      }
                  } else if (o == services) {
                      if (!isRemovePath(e)) {
                          log.finer("Clearing selection assertions palette");
                          assertionPalette.clearSelection();
                      }
                  } else {
                      log.warning("Received unexpected selection path from " + o.getClass());
                  }
              }

              private boolean isRemovePath(TreeSelectionEvent e) {
                  TreePath[] paths = e.getPaths();
                  for (int i = 0; i < paths.length; i++) {
                      if (!e.isAddedPath(i)) {
                          return true;
                      }
                  }
                  return false;
              }

          };
        getServicesTree().addTreeSelectionListener(treeSelectionListener);
        assertionPaletteTree.addTreeSelectionListener(treeSelectionListener);

        getMainSplitPaneRight().removeAll();
        GridBagConstraints constraints
          = new GridBagConstraints(0, // gridx
            0, // gridy
            1, // widthx
            1, // widthy
            1.0, // weightx
            1.0, // weigthy
            GridBagConstraints.NORTH, // anchor
            GridBagConstraints.BOTH, //fill
            new Insets(0, 0, 0, 0), // inses
            0, // padx
            0); // pady
        getMainSplitPaneRight().add(getWorkSpacePanel(), constraints);
    }


    /**
     * Return the MainJMenuBar property value.
     *
     * @return JMenuBar
     */
    private JMenuBar getMainJMenuBar() {
        if (mainJMenuBar == null) {
            mainJMenuBar = new JMenuBar();
            mainJMenuBar.setFocusable(false);
            mainJMenuBar.add(getFileMenu());
            mainJMenuBar.add(getEditMenu());
            mainJMenuBar.add(getViewMenu());
            //           mainJMenuBar.add(getWindowMenu());
            mainJMenuBar.add(getHelpMenu());
            Utilities.removeToolTipsFromMenuItems(mainJMenuBar);
        }
        return mainJMenuBar;
    }


    private Action getManageJmsEndpointsAction() {
        if (manageJmsEndpointsAction != null)
            return manageJmsEndpointsAction;


        manageJmsEndpointsAction = new ManageJmsEndpointsAction();
        manageJmsEndpointsAction.setEnabled(false);
        this.addLogonListener(manageJmsEndpointsAction);
        return manageJmsEndpointsAction;
    }

    private Action getManageCertificatesAction() {
        if (manageCertificatesAction != null)
            return manageCertificatesAction;

        manageCertificatesAction = new ManageCertificatesAction();
        manageCertificatesAction.setEnabled(false);
        this.addLogonListener(manageCertificatesAction);
        return manageCertificatesAction;
    }

    private Action getGatewayLogWindowAction() {
        if (viewGatewayLogWindowAction != null) return viewGatewayLogWindowAction;
        viewGatewayLogWindowAction = new ViewGatewayLogsAction();
        viewGatewayLogWindowAction.setEnabled(false);
        this.addLogonListener(viewGatewayLogWindowAction);
        return viewGatewayLogWindowAction;
    }

    private Action getGatewayAuditWindowAction() {
        if (viewGatewayAuditsWindowAction != null) return viewGatewayAuditsWindowAction;
        viewGatewayAuditsWindowAction = new ViewGatewayAuditsAction();
        viewGatewayAuditsWindowAction.setEnabled(false);
        this.addLogonListener(viewGatewayAuditsWindowAction);
        return viewGatewayAuditsWindowAction;
    }


    private Action getClusterStatusAction() {
        if (viewClusterStatusAction != null) return viewClusterStatusAction;
        viewClusterStatusAction = new ViewClusterStatusAction();
        viewClusterStatusAction.setEnabled(false);
        this.addLogonListener(viewClusterStatusAction);
        return viewClusterStatusAction;
    }

    /**
     * Return the mainJSplitPaneTop property value.
     *
     * @return JSplitPane
     */
    private JSplitPane getMainSplitPane() {
        if (mainSplitPane != null)
            return mainSplitPane;


        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        mainSplitPane.add(getMainSplitPaneRight(), "right");
        mainSplitPane.add(getMainLeftPanel(), "left");
        mainSplitPane.setDividerSize(4);
        mainSplitPane.setBorder(null);
        addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window has been opened.
             */
            public void windowOpened(WindowEvent e) {
                try {
                    Preferences prefs = Preferences.getPreferences();
                    String s = prefs.getString("main.split.divider.location");
                    if (s != null) {
                        int l = Integer.parseInt(s);
                        mainSplitPane.setDividerLocation(l);
                    }
                } catch (NumberFormatException e1) {
                }
            }

            /**
             * Invoked when a window has been closed.
             */
            public void windowClosed(WindowEvent e) {
                try {
                    Preferences prefs = Preferences.getPreferences();
                    int l = mainSplitPane.getDividerLocation();
                    prefs.putProperty("main.split.divider.location", Integer.toString(l));
                } catch (NullPointerException e1) {
                }
            }
        });

        return mainSplitPane;
    }

    /**
     * Return the MainPane property value.
     *
     * @return JPanel
     */
    private JPanel getMainPane() {
        if (mainPane == null) {
            mainPane = new JPanel();
            mainPane.setBorder(null);
            mainPane.setLayout(new BorderLayout());
            getMainPane().add(getMainSplitPane(), "Center");
        }
        return mainPane;
    }

    /**
     * Return the StatusBarPane property value.
     *
     * @return JPanel
     */
    private JPanel getStatusBarPane() {
        if (statusBarPane == null) {
            statusBarPane = new JPanel();
            statusBarPane.setLayout(new BorderLayout());
            statusBarPane.setDoubleBuffered(true);
            Border border =
              BorderFactory.
              createCompoundBorder(getStatusMsgLeft().getBorder(),
                BorderFactory.createEmptyBorder(2, 2, 2, 2));

            getStatusMsgLeft().setBorder(border);

            getStatusBarPane().add(getStatusMsgLeft(), BorderLayout.WEST);
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BorderLayout());

            getStatusMsgRight().setBorder(border);
            rightPanel.add(getStatusMsgRight(), BorderLayout.WEST);
            progressBar =
              (ProgressBar)TopComponents.getInstance().getComponent(ProgressBar.NAME);
            if (progressBar == null) {
                progressBar = new ProgressBar(0, 100, 20);
                TopComponents.getInstance().registerComponent(ProgressBar.NAME, progressBar);
            }

            // a bit of a hack here , set the size to the size of "disconnected" label
            progressBar.setPreferredSize(getStatusMsgLeft().getPreferredSize());
            progressBar.setMaximumSize(getStatusMsgLeft().getMaximumSize());

            progressBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            // rightPanel.add(progressBar, BorderLayout.EAST);
            getStatusBarPane().add(rightPanel, BorderLayout.EAST);
        }
        return statusBarPane;
    }

    /**
     * Return the StatusMsgLeft property value.
     *
     * @return JLabel
     */
    private JLabel getStatusMsgLeft() {
        if (statusMsgLeft == null) {
            statusMsgLeft = new JLabel();
            statusMsgLeft.setText("Disconnected");
        }
        return statusMsgLeft;
    }

    /**
     * Return the StatusMsgRight property value.
     *
     * @return JLabel
     */
    private JLabel getStatusMsgRight() {
        if (statusMsgRight == null) {
            statusMsgRight = new JLabel();
        }
        return statusMsgRight;
    }

    /**
     * Return the ToolBarPane property value.
     *
     * @return JToolBar
     */
    private JToolBar getToolBarPane() {
        if (toolBarPane != null) return toolBarPane;

        toolBarPane = new JToolBar();
        toolBarPane.setFocusable(false);
        toolBarPane.putClientProperty("JToolBar.isRollover", Boolean.TRUE);

        JButton b = toolBarPane.add(getConnectAction());
        b.setFont(new Font("Dialog", 1, 10));
        b.setText((String)getConnectAction().getValue(Action.NAME));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setHorizontalTextPosition(SwingConstants.RIGHT);

        b = toolBarPane.add(getDisconnectAction());
        b.setFont(new Font("Dialog", 1, 10));
        b.setText((String)getDisconnectAction().getValue(Action.NAME));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setHorizontalTextPosition(SwingConstants.RIGHT);

        b = toolBarPane.add(getRefreshAction());
        b.setFont(new Font("Dialog", 1, 10));
        b.setText((String)getRefreshAction().getValue(Action.NAME));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusable(false);
        b.setHorizontalTextPosition(SwingConstants.RIGHT);

        b = toolBarPane.add(homeAction);
        b.setFont(new Font("Dialog", 1, 10));
        b.setText((String)homeAction.getValue(Action.NAME));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setHorizontalTextPosition(SwingConstants.RIGHT);

        b = toolBarPane.add(getPreferencesAction());
        b.setFont(new Font("Dialog", 1, 10));
        b.setText((String)getPreferencesAction().getValue(Action.NAME));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setHorizontalTextPosition(SwingConstants.RIGHT);

        toolBarPane.add(Box.createHorizontalGlue());

        return toolBarPane;
    }


    /**
     * Return the ToolBarPane property value.
     *
     * @return JToolBar
     */
    public PolicyToolBar getPolicyToolBar() {
        if (policyToolBar != null) return policyToolBar;
        policyToolBar = new PolicyToolBar();
        policyToolBar.registerPaletteTree(getAssertionPaletteTree());
        addLogonListener(policyToolBar);
        return policyToolBar;
    }


    /**
     * Return the TreeJPanel property value.
     *
     * @return JPanel
     */
    private JPanel getMainLeftPanel() {
        if (mainLeftPanel != null)
            return mainLeftPanel;

        JTabbedPane treePanel = new JTabbedPane();
        treePanel.setBorder(null);
        //treePanel.setLayout(new BorderLayout());
        treePanel.addTab("Assertions", getAssertionPaletteTree());
        treePanel.addTab("Identity Providers", getIdentitiesTree());

        JScrollPane js = new JScrollPane(treePanel);
        js.setBorder(null);
        int mInc = js.getVerticalScrollBar().getUnitIncrement();
        // some arbitrary text to set the unit increment to the
        // height of one line instead of default value
        int vInc = (int)getStatusMsgLeft().getPreferredSize().getHeight();
        js.getVerticalScrollBar().setUnitIncrement(Math.max(mInc, vInc));
        int hInc = (int)getStatusMsgLeft().getPreferredSize().getWidth();
        js.getHorizontalScrollBar().setUnitIncrement(Math.max(mInc, hInc));

        final JSplitPane sections = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window has been opened.
             */
            public void windowOpened(WindowEvent e) {
                try {
                    Preferences prefs = Preferences.getPreferences();
                    String s = prefs.getString("tree.split.divider.location");
                    if (s != null) {
                        int l = Integer.parseInt(s);
                        sections.setDividerLocation(l);
                    }
                } catch (NumberFormatException e1) {
                }
            }

            /**
             * Invoked when a window has been closed.
             */
            public void windowClosed(WindowEvent e) {
                try {
                    Preferences prefs = Preferences.getPreferences();
                    int l = sections.getDividerLocation();
                    prefs.putProperty("tree.split.divider.location", Integer.toString(l));
                } catch (NullPointerException e1) {
                }
            }
        });


        sections.setTopComponent(js);
        sections.setBorder(null);
        treePanel = new JTabbedPane();
        // treePanel.setLayout(new BorderLayout());
        treePanel.addTab("Web Services", getServicesTree());
        treePanel.setTabPlacement(JTabbedPane.TOP);
        treePanel.setBorder(null);


        js = new JScrollPane(treePanel);
        js.setBorder(null);
        mInc = js.getVerticalScrollBar().getUnitIncrement();
        // some arbitrary text to set the unit increment to the
        // height of one line instead of default value
        vInc = (int)getStatusMsgLeft().getPreferredSize().getHeight();
        js.getVerticalScrollBar().setUnitIncrement(Math.max(mInc, vInc));
        hInc = (int)getStatusMsgLeft().getPreferredSize().getWidth();
        js.getHorizontalScrollBar().setUnitIncrement(Math.max(mInc, hInc));
        sections.setBottomComponent(js);
        sections.setDividerSize(10);


        mainLeftPanel = new JPanel(new BorderLayout());
        mainLeftPanel.add(sections, BorderLayout.CENTER);
        mainLeftPanel.add(getPolicyToolBar(), BorderLayout.EAST);
        mainLeftPanel.setBorder(null);
        return mainLeftPanel;
    }


    // --- Event listeners ---------------------------------------

    /**
     * The disconnect handler.
     *
     * @param event ActionEvent
     */
    private void disconnectHandler(ActionEvent event) throws ActionVetoException {
        getWorkSpacePanel().clearWorkspace();
        getStatusMsgLeft().setText("Disconnected");
        getStatusMsgRight().setText("");
        getAssertionPaletteTree().setModel(null);
        getMainSplitPaneRight().removeAll();
        getMainSplitPaneRight().validate();
        getMainSplitPaneRight().repaint();
        getServicesTree().setModel(null);
        getIdentitiesTree().setModel(null);
        updateActions(null);
        fireDisconnected();

        // if inactivityTimer is running stop
        if (inactivityTimer.isRunning()) {
            inactivityTimer.stop();
        }
    }


    /**
     * update the actions, menus, buttons for the selected node.
     *
     * @param node currently selected node
     */
    private void updateActions(AbstractTreeNode node) {
        if (node == null) {
            toggleConnectedMenus(false);
            getRemoveNodeAction().setEnabled(false);
            getFindAction().setEnabled(false);
            return;
        }
        getRemoveNodeAction().setEnabled(node.canDelete());
        getFindAction().setEnabled(false);
        getRefreshAction().setEnabled(false);
    }

    /**
     * @param event ActionEvent
     * @see ActionEvent for details
     */
    private void exitMenuEventHandler(ActionEvent event) {
        if (isConnected()) {
            try {
                disconnectHandler(event);
            } catch (ActionVetoException e) {
                return;
            }
        }

        this.dispose();
    }

    /**
     * Initializes listeners for the form
     */
    private void initListeners() {
        final Preferences prefs = Preferences.getPreferences();
        prefs.addPropertyChangeListener(new PropertyChangeListener() {
            /**
             * This method gets called when a property is changed.
             *
             * @param evt A PropertyChangeEvent object describing the
             *            event source and the property that has changed.
             */
            public void propertyChange(PropertyChangeEvent evt) {
                MainWindow.log.finest("preferences have been updated");
                MainWindow.this.setLookAndFeel(prefs.getString(Preferences.LOOK_AND_FEEL));
                MainWindow.this.setInactivitiyTimeout(prefs.getInactivityTimeout());
            }
        });


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
                  showHelpTopics(e);
              }
          });

        // look and feel listener
        PropertyChangeListener l =
          new PropertyChangeListener() {
              /**
               * This method gets called when a property is changed.
               */
              public void propertyChange(final PropertyChangeEvent evt) {
                  if ("lookAndFeel".equals(evt.getPropertyName())) {
                      SwingUtilities.invokeLater(new Runnable() {
                          public void run() {
                              SwingUtilities.updateComponentTreeUI(MainWindow.this);
                          }
                      });
                  }
              }
          };
        UIManager.addPropertyChangeListener(l);
    }


    // --- End Event listeners ---------------------------------------

    /**
     * Initialize the class.
     */
    private void initialize() {

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // exit routine, do not remove
                MainWindow.this.exitMenuEventHandler(null);
            }
        });
        setName("MainWindow");
        setJMenuBar(getMainJMenuBar());
        setTitle(resapplication.getString("SSG"));
        Image icon = ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png");
        ImageIcon imageIcon = new ImageIcon(icon);
        setIconImage(imageIcon.getImage());
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(getFrameContentPane(), BorderLayout.CENTER);
        contentPane.add(getStatusBarPane(), BorderLayout.SOUTH);
        getRootPane().setFocusable(false);

        initListeners();


        toggleConnectedMenus(false);
        /* Pack frame on the screen */
        //pack();
        validate();
        /* restore window position */
        initializeWindowPosition();
    }

    /**
     * Restore any saved window position.  Declines to restore the position
     * if the screen resolution has decreased since then.
     */
    private void initializeWindowPosition() {
        boolean posWasSet = false;
        Dimension curScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        try {
            Preferences prefs = Preferences.getPreferences();
            Dimension lastScreenSize = prefs.getLastScreenSize();
            Dimension lastWindowSize = prefs.getLastWindowSize();
            Point lastWindowLocation = prefs.getLastWindowLocation();
            if (lastScreenSize != null &&
              lastScreenSize.width >= curScreenSize.width &&
              lastScreenSize.height >= curScreenSize.height) {
                if (lastWindowLocation != null) {
                    this.setLocation(lastWindowLocation);
                    posWasSet = true;
                }
                if (lastWindowSize != null) {
                    this.setSize(lastWindowSize);
                    posWasSet = true;
                }
            }

            prefs.setLastScreenSize(curScreenSize);
            prefs.store();
            prefs.updateSystemProperties();
        } catch (IOException e) {
            log.log(Level.WARNING, "unable to fetch or set window position prefs: ", e);
        }

        if (!posWasSet) {
            this.setSize(curScreenSize);
            Utilities.centerOnScreen(this);
        }
    }


    /**
     * The "Help Topics".
     * This procedure adds the JavaHelp to PMC application.
     */
    public void showHelpTopics(ActionEvent e) {
        HelpSet hs = null;
        URL url = null;
        HelpBroker javaHelpBroker = null;
        String helpsetName = "SSG Help";

        try {
            // Find the helpSet URL file.
            url = cl.getResource(HELP_PATH);
            hs = new HelpSet(cl, url);
            javaHelpBroker = hs.createHelpBroker();
            Object source = e.getSource();

            if (source instanceof Window) {
                ((DefaultHelpBroker)javaHelpBroker).setActivationWindow((Window)source);
            }
            javaHelpBroker.setDisplayed(true);

        } catch (MissingResourceException ex) {
            //Make sure the URL exists.
            if (url == null) {
                JOptionPane.showMessageDialog(MainWindow.this,
                  "Help URL is missing",
                  "Bad HelpSet Path ",
                  JOptionPane.WARNING_MESSAGE);
            }
        } catch (HelpSetException hex) {
            JOptionPane.showMessageDialog(MainWindow.this,
              helpsetName + " is not available",
              "Warning",
              JOptionPane.WARNING_MESSAGE);
            log.log(Level.SEVERE, helpsetName + " file was not found. " + hex.toString());
        }
    }

    // -------------- inactivitiy timeout (close your eyes) -------------------
    final Timer
      inactivityTimer =
      new Timer(60 * 1000 * 20,
        new ActionListener() {
            long lastStamp = System.currentTimeMillis();

            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e) {

                long now = System.currentTimeMillis();
                double inactive = (now - lastStamp);
                if (Math.round(inactive / inactivityTimer.getDelay()) >= 1) { // match
                    inactivityTimer.stop(); // stop timer
                    MainWindow.this.getStatusMsgRight().
                      setText("inactivity timeout expired; disconnecting...");
                    // make sure it is invoked on event dispatching thread
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                MainWindow.this.disconnectHandler(null);
                            } catch (ActionVetoException e1) {
                                // swallow, cannot happen from here
                            }
                        }
                    });
                }
            }

            // AWT event listener
            private final
            AWTEventListener listener =
              new AWTEventListener() {
                  public void eventDispatched(AWTEvent e) {
                      lastStamp = System.currentTimeMillis();
                  }
              };
            // all events we know about
            long mask =
              AWTEvent.COMPONENT_EVENT_MASK |
              AWTEvent.CONTAINER_EVENT_MASK |
              AWTEvent.FOCUS_EVENT_MASK |
              AWTEvent.KEY_EVENT_MASK |
              AWTEvent.MOUSE_EVENT_MASK |
              AWTEvent.MOUSE_MOTION_EVENT_MASK |
              AWTEvent.WINDOW_EVENT_MASK |
              AWTEvent.ACTION_EVENT_MASK |
              AWTEvent.ADJUSTMENT_EVENT_MASK |
              AWTEvent.ITEM_EVENT_MASK |
              AWTEvent.TEXT_EVENT_MASK |
              AWTEvent.INPUT_METHOD_EVENT_MASK |
              AWTEvent.PAINT_EVENT_MASK |
              AWTEvent.INVOCATION_EVENT_MASK |
              AWTEvent.HIERARCHY_EVENT_MASK |
              AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK;

            // dynamic initializer, register listener
            {
                MainWindow.this.getToolkit().
                  addAWTEventListener(listener, mask);
            }
        });

    // -------------- inactivitiy timeout end (open your eyes) -------------------


    /**
     * set the inactivity timeout value
     *
     * @param newTimeout new inactivity timeout
     */
    private void setInactivitiyTimeout(int newTimeout) {
        int inactivityTimeout = newTimeout * 60 * 1000;
        if (!isConnected()) return;

        if (inactivityTimeout == 0) {
            if (inactivityTimer.isRunning()) {
                inactivityTimer.stop();
                log.log(Level.WARNING, "inactivity timeout disabled (timeout = 0)");
            }
        } else if (inactivityTimeout > 0) {
            //  substract 1 secs (tollerance)
            inactivityTimer.setDelay(inactivityTimeout);
            inactivityTimer.setInitialDelay(inactivityTimeout);
            if (inactivityTimer.isRunning()) {
                inactivityTimer.stop();
            }
            inactivityTimer.start();
            log.log(Level.INFO, "inactivity timeout enabled (timeout = " + inactivityTimeout + ")");
        } else {
            log.log(Level.WARNING, "incorrect timeout value " + inactivityTimeout);
            setInactivitiyTimeout(0);
        }
    }


    /**
     * set the look and feel
     *
     * @param lookAndFeel a string specifying the name of the class that implements
     *                    the look and feel
     */
    private void setLookAndFeel(String lookAndFeel) {

        if (lookAndFeel == null) return;
        boolean lfSet = true;

        // if same look and feel quick exit
        if (lookAndFeel.
          equals(UIManager.getLookAndFeel().getClass().getName())) {
            return;
        }

        try {
            Object lafObject =
              Class.forName(lookAndFeel).newInstance();
            UIManager.setLookAndFeel((LookAndFeel)lafObject);
        } catch (Exception e) {
            lfSet = false;
        }
        // there was a problem setting l&f, try crossplatform one (best bet)
        if (!lfSet) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                return;
            }
        }
        // update panels with new l&f
        SwingUtilities.updateComponentTreeUI(MainWindow.this);
        MainWindow.this.validate();
    }

    /**
     * invoke logon dialog
     */
    void activateLogonDialog() {
        LogonDialog.logon(this, logonListenr);
    }

    /**
     * is this console connected?
     *
     * @return true if connected,false otherwise
     */
    private boolean isConnected() {
        // the menu item is enabled if connected
        return getDisconnectMenuItem().isEnabled();
    }


    public JMenuItem getLogMenuItem() {
        if (logMenuItem != null) return logMenuItem;
        logMenuItem = new JMenuItem(getGatewayLogWindowAction());

        return logMenuItem;
    }

    public JMenuItem getAuditMenuItem() {
        if (auditMenuItem != null) return auditMenuItem;
        auditMenuItem = new JMenuItem(getGatewayAuditWindowAction());

        return auditMenuItem;
    }

    public JMenuItem getManageJmsEndpointsMenuItem() {
        if (manageJmsEndpointsMenuItem != null)
            return manageJmsEndpointsMenuItem;
        manageJmsEndpointsMenuItem = new JMenuItem(getManageJmsEndpointsAction());

        return manageJmsEndpointsMenuItem;
    }

    public JMenuItem getManageCertificatesMenuItem() {
        if (manageCertificatesMenuItem != null)
            return manageCertificatesMenuItem;
        manageCertificatesMenuItem = new JMenuItem(getManageCertificatesAction());

        return manageCertificatesMenuItem;
    }

    public JMenuItem getStatMenuItem() {

        if (statMenuItem != null) return statMenuItem;
        statMenuItem = new JMenuItem(getClusterStatusAction());

        return statMenuItem;
    }

    public void updateNodeNameInStatusMessage(String oldName, String newName) {
        // extract the node name from the status message

        int startIndex = getStatusMsgLeft().getText().indexOf(CONNECTION_PREFIX);
        if (startIndex > 0) {
            String nodeName = getStatusMsgLeft().getText().substring(startIndex + CONNECTION_PREFIX.length(), getStatusMsgLeft().getText().length() - 1);

            if (nodeName.equals(oldName)) {
                // update the node name only when the nodeName mataches with the oldName
                getStatusMsgLeft().setText(connectionContext + getNodeNameMsg(newName));
            }
        } else {
            // this should never happen
            log.severe("Internal error: cannot update the node name on the status bar.");
        }
    }

    private String getNodeNameMsg(String nodeName) {

        String nodeNameMsg = "";
        if (nodeName != null) {
            nodeNameMsg = CONNECTION_PREFIX + nodeName + "]";
        }
        return nodeNameMsg;
    }

    private
    LogonDialog.LogonListener logonListenr =
      new LogonDialog.LogonListener() {
          /* invoked on authentication success */
          public void onAuthSuccess(String id, String serverURL) {
              ssgURL = serverURL;
              String statusMessage = id;
              connectionContext = "";

              /* set the preferences */
              try {
                  Preferences prefs = Preferences.getPreferences();
                  connectionContext = " @ " + prefs.getString(Preferences.SERVICE_URL);
                  if (prefs.rememberLoginId()) {
                      prefs.putProperty(Preferences.LAST_LOGIN_ID, id);
                      prefs.store();
                  }
              } catch (IOException e) {
                  log.log(Level.WARNING, "onAuthSuccess()", e);
              }

              ClusterStatusAdmin clusterStatusAdmin = (ClusterStatusAdmin)Locator.getDefault().lookup(ClusterStatusAdmin.class);
              if (clusterStatusAdmin == null) throw new RuntimeException("Cannot obtain ClusterStatusAdmin remote reference");

              String nodeName = "";
              try {
                  nodeName = clusterStatusAdmin.getSelfNodeName();
              } catch (RemoteException e) {
                  log.log(Level.WARNING, "Cannot get the node name", e);
              }

              if (nodeName == null) {
                  nodeName = "unknown";
              }

              statusMessage += connectionContext;
              statusMessage += getNodeNameMsg(nodeName);

              getStatusMsgLeft().setText(statusMessage);
              initalizeWorkspace();
              int timeout = 0;
              timeout = Preferences.getPreferences().getInactivityTimeout();

              final int fTimeout = timeout;
              SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                      MainWindow.this.
                        setInactivitiyTimeout(fTimeout);
                      MainWindow.this.fireConnected();
                  }
              });
              toggleConnectedMenus(true);
              homeAction.actionPerformed(null);
          }

          /* invoked on authentication failure */
          public void onAuthFailure() {
              ;
          }
      };

    /**
     * called when the policy currently edited gets deleted
     */
    public void firePolicyEditDeleted() {
        getValidateMenuItem().setEnabled(false);
        getSaveMenuItem().setEnabled(false);
        getExportMenuItem().setEnabled(false);
        getImportMenuItem().setEnabled(false);
    }

    public void firePolicyEdit(PolicyEditorPanel policyPanel) {
        // enable the items that make sense to show when a policy is being edited
        getValidateMenuItem().setAction(policyPanel.getValidateAction());
        getSaveMenuItem().setAction(policyPanel.getSaveAction());
        getExportMenuItem().setAction(policyPanel.getExportAction());
        getImportMenuItem().setAction(policyPanel.getImportAction());
    }
}
