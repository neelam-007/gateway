package com.l7tech.console;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Locator;
import com.l7tech.console.action.*;
import com.l7tech.console.event.ConnectionEvent;
import com.l7tech.console.event.ConnectionListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.event.ConnectionAdapter;
import com.l7tech.console.panels.*;
import com.l7tech.console.security.ClientCredentialManager;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.identity.IdentitiesRootNode;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.tree.policy.PolicyToolBar;
import com.l7tech.console.util.*;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.cluster.ClusterStatusAdmin;

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.DefaultHelpBroker;
import javax.swing.*;
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
import java.util.ArrayList;
import java.util.EventListener;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;


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
    ResourceBundle resapplication =
      java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

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
    private JMenuItem statMenuItem = null;
    private JMenuItem manageJmsEndpointsMenuItem = null;
    private JMenuItem manageCertificatesMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;

    private Action refreshAction = null;
    private Action findAction = null;
    private Action prefsAction = null;
    private Action removeNodeAction = null;
    private Action connectAction = null;
    private Action disconnectAction = null;
    private Action toggleStatusBarAction = null;
    private Action togglePolicyMessageArea = null;
    private Action publishServiceAction = null;
    private Action createServiceAction = null;
    private Action toggleGatewayLogWindowAction = null;
    private Action toggleClusterStatusWindowAction = null;
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

    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();

    public static final String TITLE = "SSG Management Console";
    public static final String NAME = "main.window"; // registered
    private EventListenerList listenerList = new WeakEventListenerList();
    // cached credential manager
    private ClientCredentialManager credentialManager;
    private Action newInernalUserAction;
    private Action newInernalGroupAction;
    private Action newLDAPProviderAction;
    private Action newPKIProviderAction;
    private HomeAction homeAction = new HomeAction();
    Action monitorAction = null;
    private ClusterStatusWindow clusterStatusWindow = null;
    private GatewayLogWindow gatewayLogWindow = null;
    private Action manageJmsEndpointsAction = null;
    private Action manageCertificatesAction = null;
    private String connectionContext = "";
    private FocusAdapter actionsFocusListener;
    private ServicesTree servicesTree;
    private IdentityProvidersTree identityProvidersTree;

    /**
     * MainWindow constructor comment.
     */
    public MainWindow() {
        super(TITLE);
        initialize();
    }

    /**
     * add the ConnectionListener
     * 
     * @param listener the ConnectionListener
     */
    public void addConnectionListener(ConnectionListener listener) {
        listenerList.add(ConnectionListener.class, listener);
    }

    /**
     * remove the the ConnectionListener
     * 
     * @param listener the ConnectionListener
     */
    public void removeConnectionListener(ConnectionListener listener) {
        listenerList.remove(ConnectionListener.class, listener);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type (connection event).
     */
    private void fireConnected() {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTED);
        EventListener[] listeners = listenerList.getListeners(ConnectionListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((ConnectionListener)listeners[i]).onConnect(event);
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type (connection event).
     */
    private void fireDisconnected() {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.DISCONNECTED);
        EventListener[] listeners = listenerList.getListeners(ConnectionListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((ConnectionListener)listeners[i]).onDisconnect(event);
        }
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
            fileMenu.setText(resapplication.getString("Session"));
            fileMenu.add(getConnectMenuItem());
            fileMenu.add(getDisconnectMenuItem());
            fileMenu.add(getMenuItemPreferences());

            fileMenu.add(getExitMenuItem());
            int mnemonic = fileMenu.getText().toCharArray()[0];
            fileMenu.setMnemonic(mnemonic);
        }
        return fileMenu;
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
            editMenu.addSeparator();

            editMenu.add(getManageJmsEndpointsMenuItem());
            editMenu.add(getManageCertificatesMenuItem());
            int mnemonic = editMenu.getText().toCharArray()[0];
            editMenu.setMnemonic(mnemonic);

            editMenu.addSeparator();
            editMenu.add(getStatMenuItem());
            editMenu.add(getLogMenuItem());
        }
        return editMenu;
    }

    private JMenu getNewProviderSubMenu() {
        if (newProviderSubMenu == null) {
            newProviderSubMenu = new JMenu();
            newProviderSubMenu.setFocusable(false);

            newProviderSubMenu.setText("Create Identity Provider");
            newProviderSubMenu.setIcon(new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/providers16.gif")));

            newProviderSubMenu.add(getNewProviderAction());
            newProviderSubMenu.add(getNewPKIProviderAction());
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
    private Action getPublishServiceAction() {
        if (publishServiceAction != null) {
            return publishServiceAction;
        }
        publishServiceAction = new PublishServiceAction();
        publishServiceAction.setEnabled(false);
        this.addConnectionListener((PublishServiceAction)publishServiceAction);
        return publishServiceAction;
    }


    /**
     * @return the <code>Action</code> for the create service
     */
    private Action getCreateServiceAction() {
        if (createServiceAction != null) {
            return createServiceAction;
        }
        createServiceAction = new CreateServiceWsdlAction();
        createServiceAction.setEnabled(false);
        this.addConnectionListener((CreateServiceWsdlAction)createServiceAction);
        return createServiceAction;
    }


    private Action getNewInternalUserAction() {
        if (newInernalUserAction != null) return newInernalUserAction;
        newInernalUserAction = new NewUserAction(null) {
            /**
             * specify the resource name for this action
             */
            protected String iconResource() {
                return "com/l7tech/console/resources/user16.png";
            }

            ConnectionListener listener = new ConnectionListener() {
                public void onConnect(ConnectionEvent e) {
                    setEnabled(true);
                }

                public void onDisconnect(ConnectionEvent e) {
                    setEnabled(false);
                }
            };

            {
                MainWindow.this.addConnectionListener(listener);
            }
        };
        newInernalUserAction.setEnabled(false);
        return newInernalUserAction;
    }

    private Action getNewInternalGroupAction() {
        if (newInernalGroupAction != null) return newInernalGroupAction;
        newInernalGroupAction = new NewGroupAction(null) {
            /**
             * specify the resource name for this action
             */
            protected String iconResource() {
                return "com/l7tech/console/resources/group16.png";
            }

            ConnectionListener listener = new ConnectionListener() {
                public void onConnect(ConnectionEvent e) {
                    setEnabled(true);
                }

                public void onDisconnect(ConnectionEvent e) {
                    setEnabled(false);
                }
            };

            {
                MainWindow.this.addConnectionListener(listener);
            }
        };
        newInernalGroupAction.setEnabled(false);
        return newInernalGroupAction;
    }

    private Action getNewPKIProviderAction() {
         if (newPKIProviderAction != null) return newPKIProviderAction;
         newPKIProviderAction = new NewFederatedIdentityProviderAction(null) {
             /**
              * specify the resource name for this action
              */
             protected String iconResource() {
                 return "com/l7tech/console/resources/providers16.gif";
             }

             ConnectionListener listener = new ConnectionListener() {
                 public void onConnect(ConnectionEvent e) {
                     setEnabled(true);
                     final DefaultMutableTreeNode root =
                       (DefaultMutableTreeNode)getIdentitiesTree().getModel().getRoot();
                     node = (AbstractTreeNode)root;
                 }

                 public void onDisconnect(ConnectionEvent e) {
                     setEnabled(false);
                 }
             };

             {
                 MainWindow.this.addConnectionListener(listener);
             }
         };
         newPKIProviderAction.setEnabled(false);
         return newPKIProviderAction;
     }

    private Action getNewProviderAction() {
        if (newLDAPProviderAction != null) return newLDAPProviderAction;
        newLDAPProviderAction = new NewProviderAction(null) {
            /**
             * specify the resource name for this action
             */
            protected String iconResource() {
                return "com/l7tech/console/resources/providers16.gif";
            }

            ConnectionListener listener = new ConnectionListener() {
                public void onConnect(ConnectionEvent e) {
                    setEnabled(true);
                    final DefaultMutableTreeNode root =
                      (DefaultMutableTreeNode)getIdentitiesTree().getModel().getRoot();
                    node = (AbstractTreeNode)root;
                }

                public void onDisconnect(ConnectionEvent e) {
                    setEnabled(false);
                }
            };

            {
                MainWindow.this.addConnectionListener(listener);
            }
        };
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
              ConnectionListener listener = new ConnectionAdapter() {
                  public void onDisconnect(ConnectionEvent e) {
                      setEnabled(false);
                  }
              };

              {
                  MainWindow.this.addConnectionListener(listener);
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
                  final KeyboardFocusManager kbm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                  final Component c = kbm.getFocusOwner();
                  log.finest("the focus owner is "+c.getClass());
                  if (c instanceof Refreshable) {

                      try {
                          Refreshable r = (Refreshable)c;
                          if (r.canRefresh()) {
                              log.finest("Invoke refresh on "+c.getClass());
                              r.refresh();
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
                    log.finest("focusLost, no component in focus setting refreshAction to "+false);
                    refreshAction.setEnabled(false);
                    return;
                }

                boolean enable = c instanceof Refreshable && ((Refreshable)c).canRefresh();
                log.finest("focusLost "+ c.getClass() +" setting refreshAction to "+enable);
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

        final AbstractTreeNode identitiesRootNode = new IdentitiesRootNode("Identity Providers");
        treeModel = new FilteredTreeModel(null);
        treeModel.setRoot(identitiesRootNode);

        final JTree identitiesTree = getIdentitiesTree();
        identitiesTree.setRootVisible(true);
        identitiesTree.setModel(treeModel);

        String rootTitle = "Web Services @ ";
        rootTitle +=
          Preferences.getPreferences().getString(Preferences.SERVICE_URL);
        DefaultTreeModel servicesTreeModel = new FilteredTreeModel(null);
        final AbstractTreeNode servicesRootNode =
          new ServicesFolderNode(Registry.getDefault().getServiceManager(), rootTitle);
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


    private ClusterStatusWindow getClusterStatusWindow() {
        if (clusterStatusWindow != null) return clusterStatusWindow;

        clusterStatusWindow = new ClusterStatusWindow(resapplication.getString("SSG") + " - " + resapplication.getString("ClusterStatusWindowTitle"));
        clusterStatusWindow.addWindowListener(new WindowAdapter() {
            public void windowClosed(final WindowEvent e) {
                statMenuItem.setSelected(false);

                clusterStatusWindow = null;
            }

            public void windowClosing(final WindowEvent e) {
                statMenuItem.setSelected(false);

                clusterStatusWindow.dispose();
                clusterStatusWindow = null;
            }
        });
        addConnectionListener(clusterStatusWindow);
        Utilities.centerOnScreen(clusterStatusWindow);
        return clusterStatusWindow;
    }

    private GatewayLogWindow getGatewayLogWindow() {
        if (gatewayLogWindow != null) return gatewayLogWindow;

        gatewayLogWindow = new GatewayLogWindow(resapplication.getString("SSG") + " - " + resapplication.getString("LogBrowserWindowTitle"));
        gatewayLogWindow.addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                destroyGatewayLogWindow();
            }

            public void windowClosed(final WindowEvent e) {
                destroyGatewayLogWindow();
            }

        });
        addConnectionListener(gatewayLogWindow);
        Utilities.centerOnScreen(gatewayLogWindow);
        return gatewayLogWindow;
    }

    private void destroyGatewayLogWindow() {
        if (gatewayLogWindow == null)
            return;
        getLogMenuItem().setSelected(false);
        gatewayLogWindow.dispose();
        removeConnectionListener(gatewayLogWindow);
        gatewayLogWindow = null;
    }

    private Action getManageJmsEndpointsAction() {
        if (manageJmsEndpointsAction != null)
            return manageJmsEndpointsAction;

        final String atext = resapplication.getString("jms.monitored.endpoints.display.action.name");
        final String aDesc = resapplication.getString("jms.monitored.endpoints.display.action.desc");

        manageJmsEndpointsAction = new BaseAction() {
            public String getName() {
                return atext;
            }

            public String getDescription() {
                return atext;
            }

            protected String iconResource() {
                return "com/l7tech/console/resources/enableService.gif";
            }

            public void performAction() {
                JmsQueuesWindow jqw = JmsQueuesWindow.createInstance(MainWindow.this);
                Utilities.centerOnScreen(jqw);
                jqw.show();
                jqw.dispose();
            }
        };
        manageJmsEndpointsAction.putValue(Action.NAME, atext);
        manageJmsEndpointsAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        manageJmsEndpointsAction.setEnabled(false);
        enableActionWhileConnected(manageJmsEndpointsAction);
        return manageJmsEndpointsAction;
    }

    private Action getManageCertificatesAction() {
        if (manageCertificatesAction != null)
            return manageCertificatesAction;

        final String atext = resapplication.getString("manage.cert.action.name");
        final String aDesc = resapplication.getString("manage.cert.action.desc");

        manageCertificatesAction = new BaseAction() {
            public String getName() {
                return atext;
            }

            public String getDescription() {
                return atext;
            }

            protected String iconResource() {
                return "com/l7tech/console/resources/cert16.gif";
            }

            public void performAction() {
                CertManagerWindow cmw = CertManagerWindow.getInstance(MainWindow.this);
                Utilities.centerOnScreen(cmw);
                cmw.show();
                cmw.dispose();
            }
        };
        manageCertificatesAction.putValue(Action.NAME, atext);
        manageCertificatesAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        manageCertificatesAction.setEnabled(false);
        enableActionWhileConnected(manageCertificatesAction);
        return manageCertificatesAction;
    }

    private Action getGatewayLogWindowAction() {
        if (toggleGatewayLogWindowAction != null) return toggleGatewayLogWindowAction;

        String actionName = resapplication.getString("toggle.gateway.log.display.action.name");
        String actionDesc = resapplication.getString("toggle.gateway.log.display.action.desc");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/ServerLogs.gif"));

        toggleGatewayLogWindowAction = new AbstractAction(actionName, icon) {
            public void actionPerformed(ActionEvent event) {
                getGatewayLogWindow().show();
                getGatewayLogWindow().setState(Frame.NORMAL);
            }
        };
        toggleGatewayLogWindowAction.putValue(Action.SHORT_DESCRIPTION, actionDesc);
        toggleGatewayLogWindowAction.setEnabled(false);
        enableActionWhileConnected(toggleGatewayLogWindowAction);
        return toggleGatewayLogWindowAction;
    }

    /**
     * Configure the specified item to be enabled only while we are connected to a gateway.
     */
    private ArrayList weakListenerListWorkAround = new ArrayList();

    private void enableActionWhileConnected(final Action item) {
        ConnectionListener myListener = new ConnectionListener() {
            public void onConnect(ConnectionEvent e) {
                item.setEnabled(true);
            }

            public void onDisconnect(ConnectionEvent e) {
                item.setEnabled(false);
            }
        };
        weakListenerListWorkAround.add(myListener); // bind lifecycle to lifespan of MainWindow instance
        addConnectionListener(myListener);
    }

    private Action getClusterStatusWindowAction() {
        if (toggleClusterStatusWindowAction != null) return toggleClusterStatusWindowAction;

        String actionName = resapplication.getString("toggle.cluster.status.display.action.name");
        String actionDesc = resapplication.getString("toggle.cluster.status.display.action.desc");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/ClusterServers.gif"));

        toggleClusterStatusWindowAction = new AbstractAction(actionName, icon) {
            /**
             * Invoked when an action occurs.
             *
             * @param event the event that occured
             * @see Action#removePropertyChangeListener
             */
            public void actionPerformed(ActionEvent event) {
                getClusterStatusWindow().show();
                getClusterStatusWindow().setState(Frame.NORMAL);
            }
        };
        toggleClusterStatusWindowAction.putValue(Action.SHORT_DESCRIPTION, actionDesc);
        toggleClusterStatusWindowAction.setEnabled(false);
        enableActionWhileConnected(toggleClusterStatusWindowAction);
        return toggleClusterStatusWindowAction;
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
        addConnectionListener(policyToolBar);
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

        if (clusterStatusWindow != null) clusterStatusWindow.dispose();

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
                MainWindow.log.info("preferences have been updated");
                MainWindow.this.setLookAndFeel(prefs.getString(Preferences.LOOK_AND_FEEL));
                MainWindow.this.setInactivitiyTimeout(prefs.getInactivityTimeout());
            }
        });
        credentialManager =
          (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        if (credentialManager == null) {
            log.log(Level.WARNING, "Cannot obtain current credential manager");
        } else {
            log.log(Level.FINEST, "Registering the connection listener " + credentialManager.getClass());
            addConnectionListener(credentialManager);
        }


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
                      // MainWindow.this.log.debug("listener invoked");
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
        statMenuItem = new JMenuItem(getClusterStatusWindowAction());

        return statMenuItem;
    }

    public void updateNodeNameInStatusMessage(String oldName, String newName) {
        // extract the node name from the status message

        int startIndex = getStatusMsgLeft().getText().indexOf(CONNECTION_PREFIX);
        if (startIndex > 0) {
            String nodeName = getStatusMsgLeft().getText().substring(startIndex+CONNECTION_PREFIX.length(), getStatusMsgLeft().getText().length() - 1);

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
          public void onAuthSuccess(String id) {

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

              if(nodeName == null) {
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
}
