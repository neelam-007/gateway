package com.l7tech.console;

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import com.incors.plaf.kunststoff.themes.KunststoffDesktopTheme;
import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Locator;
import com.l7tech.console.action.*;
import com.l7tech.console.event.ConnectionEvent;
import com.l7tech.console.event.ConnectionListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.PolicyToolBar;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.ProgressBar;
import com.l7tech.console.util.Registry;

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.EventListener;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The console main window <CODE>MainWindow</CODE> class.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class MainWindow extends JFrame {
    static Logger log = Logger.getLogger(MainWindow.class.getName());
    /** the resource path for the application */
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";

    /** the path to JavaHelp helpset file */
    public static final String HELP_PATH = "com/l7tech/console/resources/helpset/secure_span_gateway_console_help.hs";

    /** the resource bundle name */
    private static
    ResourceBundle resapplication =
      java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    private JMenuBar mainJMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu editMenu = null;
    private JMenu viewMenu = null;
    private JMenu helpMenu = null;


    private JMenuItem connectMenuItem = null;
    private JMenuItem disconnectMenuItem = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem menuItemPref = null;

    private JMenuItem helpTopicsMenuItem = null;

    private Action refreshAction = null;
    private Action serverLoadAction = null;
    private Action findAction = null;
    private Action prefsAction = null;
    private Action removeNodeAction = null;
    private Action connectAction = null;
    private Action disconnectAction = null;
    private Action toggleStatusBarAction = null;
    private Action toggleShowLogAction = null;
    private Action publishServiceAction = null;


    private JPanel frameContentPane = null;
    private JPanel mainPane = null;
    private JPanel statusBarPane = null;
    private JLabel statusMsgLeft = null;
    private JLabel statusMsgRight = null;
    private LogPanel logPane = null;


    private JToolBar toolBarPane = null;
    private PolicyToolBar policyToolBar = null;

    private JSplitPane mainJSplitPane = null;
    private JSplitPane mainJSplitPaneTop = null;
    private JPanel mainLeftJPanel = null;

    private JPanel mainSplitPaneRight = null;

    /* progress bar indicator */
    private ProgressBar progressBar = null;

    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();

    private JMenu gotoMenu;
    private JMenu serverLoadMenu;

    public static final String TITLE = "SSG Management Console";
    public static final String NAME = "main.window"; // registered
    private JTree policyTree;
    private EventListenerList listenerList = new WeakEventListenerList();
    // cached credential manager
    private ClientCredentialManager credentialManager;
    private Action newInernalUserAction;
    private Action newInernalGroupAction;
    private Action newProviderAction;

    /**
     * MainWindow constructor comment.
     */
    public MainWindow() throws IOException {
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
     * @return JMenuItem
     */
    private JMenuItem getConnectMenuItem() {
        if (connectMenuItem == null) {
            connectMenuItem = new JMenuItem(getConnectAction());
            connectMenuItem.setIcon(null);
            int mnemonic = connectMenuItem.getText().toCharArray()[0];
            connectMenuItem.setMnemonic(mnemonic);
            connectMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return connectMenuItem;
    }

    /**
     * Return the DisconnectMenuItem property value.
     * @return JMenuItem
     */
    private JMenuItem getDisconnectMenuItem() {
        if (disconnectMenuItem != null)
            return disconnectMenuItem;

        disconnectMenuItem = new JMenuItem(getDisconnectAction());
        disconnectMenuItem.setIcon(null);

        int mnemonic = disconnectMenuItem.getText().toCharArray()[0];
        disconnectMenuItem.setMnemonic(mnemonic);
        disconnectMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));

        return disconnectMenuItem;
    }


    /**
     * Return the menuItemPref property value.
     * @return JMenuItem
     */
    private JMenuItem getMenuItemPreferences() {
        if (menuItemPref == null) {
            menuItemPref = new JMenuItem(getPreferencesAction());
            menuItemPref.setIcon(null);

            int mnemonic = menuItemPref.getText().toCharArray()[0];
            menuItemPref.setMnemonic(mnemonic);
            menuItemPref.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return menuItemPref;
    }


    /**
     * Return the ExitMenuItem property value.
     * @return JMenuItem
     */
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


    /**
     * Return the helpTopicsMenuItem property value.
     * @return JMenuItem
     */
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


    /**
     * Return the fileMenu property value.
     * @return JMenu
     */
    private JMenu getFileMenu() {
        if (fileMenu == null) {
            fileMenu = new JMenu();
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
     * @return JMenu
     */
    private JMenu getEditMenu() {
        if (editMenu == null) {
            editMenu = new JMenu();
            editMenu.setText(resapplication.getString("Edit"));
            editMenu.add(getPublishServiceAction());
            editMenu.addSeparator();
            editMenu.add(getNewInternalUserAction());
            editMenu.add(getNewInternalGroupAction());
            editMenu.add(getNewProviderAction());
            int mnemonic = editMenu.getText().toCharArray()[0];
            editMenu.setMnemonic(mnemonic);
        }
        return editMenu;
    }


    /**
     * Return the viewMenu property value.
     * @return JMenu
     */
    private JMenu getViewMenu() {
        if (viewMenu != null) return viewMenu;

        viewMenu = new JMenu();
        viewMenu.setText(resapplication.getString("View"));
        // workaround to disable icon on the menu
        JMenuItem item = new JMenuItem(getRefreshAction());
        // item.setIcon(null);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        viewMenu.add(item);

        int mnemonic = viewMenu.getText().toCharArray()[0];
        viewMenu.setMnemonic(mnemonic);
        viewMenu.add(getGotoSubmenu());
        item = new JMenuItem(getServerLoadAction());
        mnemonic = item.getText().toCharArray()[0];
        item.setMnemonic(mnemonic);
        viewMenu.add(item);
        viewMenu.addSeparator();

        JCheckBoxMenuItem logItem = new JCheckBoxMenuItem(getShowLogToggleAction());
        viewMenu.add(logItem);
        logItem.setEnabled(false);

        final JCheckBoxMenuItem jcm =
          new JCheckBoxMenuItem(getToggleStatusBarToggleAction());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    jcm.setSelected(Preferences.getPreferences().isStatusBarBarVisible());
                    getToggleStatusBarToggleAction().actionPerformed(new ActionEvent(jcm, 0, null));
                } catch (IOException e) {
                    // ignore
                }
            }
        });

        viewMenu.add(jcm);
        return viewMenu;
    }

    /**
     * Return the newMenu property value.
     * @return JMenuItem
     */
    private JMenu getGotoSubmenu() {
        if (gotoMenu != null) return gotoMenu;

        gotoMenu = new JMenu("Go To");
        int mnemonic = gotoMenu.getText().toCharArray()[0];
        gotoMenu.setMnemonic(mnemonic);

        JMenuItem menuItem;

        menuItem = new JMenuItem(new HomeAction());
        gotoMenu.add(menuItem);
        return gotoMenu;
    }

    /**
     * Return the helpMenu property value.
     * @return JMenu
     */
    private JMenu getHelpMenu() {
        if (helpMenu != null) return helpMenu;

        helpMenu = new JMenu();
        helpMenu.setText(resapplication.getString("Help"));
        helpMenu.add(getHelpTopicsMenuItem());
        helpMenu.add(new ConsoleAction());
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
        String atext = resapplication.getString("ConnectMenuItem_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/connect2.gif"));
        connectAction =
          new AbstractAction(atext, icon) {
              /**
               * Invoked when an action occurs.
               *
               * @param event  the event that occured
               */
              public void actionPerformed(ActionEvent event) {
                  connectHandler(event);
              }
          };
        connectAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return connectAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the disconnect <CODE>Action</CODE> implementation
     */
    private Action getDisconnectAction() {
        if (disconnectAction != null) return disconnectAction;
        String atext = resapplication.getString("CloseButton_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/disconnect.gif"));
        disconnectAction =
          new AbstractAction(atext, icon) {
              /**
               * Invoked when an action occurs.
               *
               * @param event  the event that occured
               */
              public void actionPerformed(ActionEvent event) {
                  disconnectHandler(event);
              }
          };
        disconnectAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return disconnectAction;
    }

    /**
     * @return the <code>Action</code> for the publish service
     */
    private Action getPublishServiceAction() {
        if (publishServiceAction != null) {
            return publishServiceAction;
        }
        publishServiceAction = new PublishServiceAction() {
            /** specify the resource name for this action */
            protected String iconResource() {
                return "com/l7tech/console/resources/services16.png";
            }
        };
        publishServiceAction.setEnabled(false);
        this.addConnectionListener((PublishServiceAction)publishServiceAction);
        return publishServiceAction;
    }

    private Action getNewInternalUserAction() {
        if (newInernalUserAction != null) return newInernalUserAction;
        newInernalUserAction = new NewUserAction(null) {
            /** specify the resource name for this action */
            protected String iconResource() {
                return "com/l7tech/console/resources/user16.png";
            }

            ConnectionListener listener = new ConnectionListener() {
                public void onConnect(ConnectionEvent e) {
                    setEnabled(true);
                    final DefaultMutableTreeNode root =
                      (DefaultMutableTreeNode)getAssertionPaletteTree().getModel().getRoot();
                    AbstractTreeNode parent =
                      (AbstractTreeNode)TreeNodeActions.nodeByName(
                        UserFolderNode.INTERNAL_USERS_NAME,
                        root
                      );
                    node = parent;

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
            /** specify the resource name for this action */
            protected String iconResource() {
                return "com/l7tech/console/resources/group16.png";
            }

            ConnectionListener listener = new ConnectionListener() {
                public void onConnect(ConnectionEvent e) {
                    setEnabled(true);
                    final DefaultMutableTreeNode root =
                      (DefaultMutableTreeNode)getAssertionPaletteTree().getModel().getRoot();
                    AbstractTreeNode parent =
                      (AbstractTreeNode)TreeNodeActions.nodeByName(
                        GroupFolderNode.INTERNAL_GROUPS_NAME,
                        root
                      );
                    node = parent;
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


    private Action getNewProviderAction() {
          if (newProviderAction != null) return newProviderAction;
          newProviderAction = new NewProviderAction(null) {
              /** specify the resource name for this action */
              protected String iconResource() {
                  return "com/l7tech/console/resources/providers16.gif";
              }

              ConnectionListener listener = new ConnectionListener() {
                  public void onConnect(ConnectionEvent e) {
                      setEnabled(true);
                      final DefaultMutableTreeNode root =
                        (DefaultMutableTreeNode)getAssertionPaletteTree().getModel().getRoot();
                      AbstractTreeNode parent =
                        (AbstractTreeNode)TreeNodeActions.nodeByName(
                          ProvidersFolderNode.NAME,
                          root
                        );
                      node = parent;

                  }

                  public void onDisconnect(ConnectionEvent e) {
                      setEnabled(false);
                  }
              };

              {
                  MainWindow.this.addConnectionListener(listener);
              }
          };
          newProviderAction.setEnabled(false);
          return newProviderAction;
      }



    // Francis
    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that show/hide the Log window
     */
    private Action getShowLogToggleAction() {
        if (toggleShowLogAction != null) return toggleShowLogAction;

        //String atext = resapplication.getString("toggle.log.display");
        String atext = "Debug Log";

        toggleShowLogAction =
          new AbstractAction(atext) {
              /**
               * Invoked when an action occurs.
               *
               * @param event  the event that occured
               * @see Action#removePropertyChangeListener
               */
              public void actionPerformed(ActionEvent event) {
                  JCheckBoxMenuItem item = (JCheckBoxMenuItem)event.getSource();
                  //                Component[] comps = getMainLeftJPanel().getComponents();
                  //               for (int i = comps.length - 1; i >= 0; i--) {
                  //                 if (comps[i] instanceof JSplitPane) {
                  //
                  if (item.isSelected()) {
                      getLogPane().getLogs();
                  }
                  getLogPane().getPane().setVisible(item.isSelected());
                  mainJSplitPane.setDividerLocation(700);

                  validate();
                  repaint();
              }
              //               }
              //            }
          };
        toggleShowLogAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return toggleShowLogAction;
    }

    private Action getServerLoadAction() {
        if (serverLoadAction != null) return serverLoadAction;
        String atext = resapplication.getString("ServerLoad_MenuItem_text");
        serverLoadAction = new AbstractAction(atext) {
            /**
             * Invoked when an action occurs.
             *
             * @param event  the event that occured
             * @see Action#removePropertyChangeListener
             */
            public void actionPerformed(ActionEvent event) {
                ServerLoadDialog dialog = new ServerLoadDialog(MainWindow.this, true);
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                dialog.setResizable(false);
                dialog.setVisible(true);
            }
        };
        serverLoadAction.setEnabled(false);
        serverLoadAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return serverLoadAction;
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
              /**
               * Invoked when an action occurs.
               *
               * @param event  the event that occured
               * @see Action#removePropertyChangeListener
               */
              public void actionPerformed(ActionEvent event) {
                  JTree tree = getAssertionPaletteTree();
                  EntityTreeNode node =
                    (EntityTreeNode)tree.getLastSelectedPathComponent();

                  if (node != null) {
                      refreshNode(node);
                  }
              }
          };
        refreshAction.setEnabled(false);
        refreshAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return refreshAction;
    }


    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that toggles the shortcut bar
     */
    private Action getToggleStatusBarToggleAction() {
        if (toggleStatusBarAction != null) return toggleStatusBarAction;

        String atext = resapplication.getString("toggle.status.bar");

        toggleStatusBarAction =
          new AbstractAction(atext) {
              /**
               * Invoked when an action occurs.
               *
               * @param event  the event that occured
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
        toggleStatusBarAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return toggleStatusBarAction;
    }

    /**
     * create the Action (the component that is used by several
     * controls) that returns the 'find' action.
     *
     * @return the find <CODE>Action</CODE>
     */
    private Action getFindAction() {
        if (findAction != null) return findAction;
        findAction = new FindIdentityAction();
        return findAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that invokes the
     * preferences dialog
     */
    private Action getPreferencesAction() {
        if (prefsAction != null) return prefsAction;
        String atext = resapplication.getString("Preferences_MenuItem_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/preferences.gif"));
        prefsAction =
          new AbstractAction(atext, icon) {
              /** Invoked when an action occurs. */
              public void actionPerformed(ActionEvent event) {
                  PreferencesDialog dialog = new PreferencesDialog(MainWindow.this, true, isConnected());
                  dialog.pack();
                  Utilities.centerOnScreen(dialog);
                  dialog.setResizable(false);
                  dialog.setVisible(true);
              }
          };
        prefsAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return prefsAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that invokes the
     * remove node action
     */
    private Action getRemoveNodeAction() {
        if (removeNodeAction != null) return removeNodeAction;
        String atext = resapplication.getString("Delete_MenuItem_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Delete16.gif"));
        removeNodeAction =
          new AbstractAction(atext, icon) {
              /** Invoked when an action occurs.*/
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
        getShowLogToggleAction().setEnabled(connected);
        getServerLoadAction().setEnabled(connected);
    }


    /**
     * Return the JFrameContentPane property value.
     * @return JPanel
     */
    private JPanel getJFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setPreferredSize(new Dimension(700, 600));
            frameContentPane.setLayout(new BorderLayout());
            getJFrameContentPane().add(getToolBarPane(), "North");
            // getJFrameContentPane().add(getStatusBarPane(), "South");
            getJFrameContentPane().add(getMainPane(), "Center");
        }
        return frameContentPane;
    }


    /**
     * Return the jJPanelEditor property value.
     * @return JPanel
     */
    private JPanel getMainSplitPaneRight() {
        if (mainSplitPaneRight != null)
            return mainSplitPaneRight;

        mainSplitPaneRight = new JPanel();
        mainSplitPaneRight.setLayout(new GridBagLayout());
        return mainSplitPaneRight;
    }

    private WorkSpacePanel getWorkSpacePanel() {
        return ComponentRegistry.getInstance().getCurrentWorkspace();
    }

    /**
     * Return the palette tree view property value.
     * @return JTree
     */
    private JTree getAssertionPaletteTree() {
        JTree tree =
          (JTree)ComponentRegistry.getInstance().getComponent(AssertionsTree.NAME);
        if (tree != null)
            return tree;
        tree = new AssertionsTree();
        ComponentRegistry.getInstance().registerComponent(AssertionsTree.NAME, tree);
        return tree;
    }

    /**
     * Return the JTreeView property value.
     * @return JTree
     */
    private JTree getServicesTree() {
        JTree tree =
          (JTree)ComponentRegistry.getInstance().getComponent(ServicesTree.NAME);
        if (tree != null)
            return tree;

        tree = new ServicesTree();
        ComponentRegistry.getInstance().registerComponent(ServicesTree.NAME, tree);
        return tree;
    }

    /**
     * Initialize the workspace. Invoked on successfull login.
     */
    private void initalizeWorkspace() {
        DefaultTreeModel treeModel = new FilteredTreeModel(null);
        final AbstractTreeNode paletteRootNode =
          new AssertionsPaletteRootNode("Policy Assertions");
        treeModel.setRoot(paletteRootNode);
        getAssertionPaletteTree().setRootVisible(true);
        getAssertionPaletteTree().setModel(treeModel);
        TreePath path = new TreePath(paletteRootNode.getPath());
        getAssertionPaletteTree().setSelectionPath(path);


        String rootTitle = "Services @ ";
        try {
            rootTitle +=
              Preferences.getPreferences().getString(Preferences.SERVICE_URL);

        } catch (IOException e) {
            e.printStackTrace();
        }
        DefaultTreeModel servicesTreeModel = new FilteredTreeModel(null);
        final AbstractTreeNode servicesRootNode =
          new ServicesFolderNode(Registry.getDefault().getServiceManager(), rootTitle);
        servicesTreeModel.setRoot(servicesRootNode);

        getServicesTree().setRootVisible(true);
        getServicesTree().setModel(servicesTreeModel);
        TreePath initialPath = new TreePath(servicesRootNode.getPath());
        getServicesTree().setSelectionPath(initialPath);

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
     * @return JMenuBar
     */
    private JMenuBar getMainJMenuBar() {
        if (mainJMenuBar == null) {
            mainJMenuBar = new JMenuBar();
            mainJMenuBar.add(getFileMenu());
            mainJMenuBar.add(getEditMenu());
            mainJMenuBar.add(getViewMenu());
            mainJMenuBar.add(getHelpMenu());
        }
        return mainJMenuBar;
    }

    /**
     * Return the mainJSplitPaneTop property value.
     * @return JSplitPane
     */
    private JSplitPane getMainJSplitPaneTop() {
        if (mainJSplitPaneTop != null)
            return mainJSplitPaneTop;


        mainJSplitPaneTop =
          new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        mainJSplitPaneTop.add(getMainSplitPaneRight(), "right");
        mainJSplitPaneTop.add(getMainLeftJPanel(), "left");
        mainJSplitPaneTop.setDividerSize(2);
        addWindowListener(new WindowAdapter() {
            /** Invoked when a window has been opened. */
            public void windowOpened(WindowEvent e) {
                try {
                    Preferences prefs = Preferences.getPreferences();
                    String s = prefs.getString("main.split.divider.location");
                    if (s != null) {
                        int l = Integer.parseInt(s);
                        mainJSplitPaneTop.setDividerLocation(l);
                    }
                } catch (IOException e1) {

                } catch (NumberFormatException e1) {
                }
            }

            /** Invoked when a window has been closed. */
            public void windowClosed(WindowEvent e) {
                try {
                    Preferences prefs = Preferences.getPreferences();
                    int l = mainJSplitPaneTop.getDividerLocation();
                    prefs.putProperty("main.split.divider.location", Integer.toString(l));
                } catch (IOException e1) {
                } catch (NullPointerException e1) {
                }
            }
        });

        return mainJSplitPaneTop;
    }

    /**
     * Return the MainPane property value.
     * @return JPanel
     */
    private JPanel getMainPane() {
        if (mainPane == null) {
            mainPane = new JPanel();
            mainPane.setLayout(new BorderLayout());
            getMainPane().add(getMainJSplitPane(), "Center");
        }
        return mainPane;
    }

    // Francis
    private JSplitPane getMainJSplitPane() {
        if (mainJSplitPane == null) {
            mainJSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            mainJSplitPane.setResizeWeight(0.75);
            mainJSplitPane.setTopComponent(getMainJSplitPaneTop());
            mainJSplitPane.setBottomComponent(getLogPane().getPane());
            // mainJSplitPane.setDividerLocation(mainJSplitPane.getSize().getHeight() * 0.5);
            mainJSplitPane.setDividerLocation(700);
        }
        return mainJSplitPane;
    }

    private LogPanel getLogPane(){
        if(logPane == null) {
            logPane = new LogPanel();
        }
        return logPane;
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
              (ProgressBar)ComponentRegistry.getInstance().getComponent(ProgressBar.NAME);
            if (progressBar == null) {
                progressBar = new ProgressBar(0, 100, 20);
                ComponentRegistry.getInstance().registerComponent(ProgressBar.NAME, progressBar);
            }

            // a bit of a hack here , set the size to the size of "disconnected" label
            progressBar.setPreferredSize(getStatusMsgLeft().getPreferredSize());
            progressBar.setMaximumSize(getStatusMsgLeft().getMaximumSize());

            progressBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            rightPanel.add(progressBar, BorderLayout.EAST);
            getStatusBarPane().add(rightPanel, BorderLayout.EAST);
        }
        return statusBarPane;
    }

    /**
     * Return the StatusMsgLeft property value.
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
     * @return JToolBar
     */
    private JToolBar getToolBarPane() {
        if (toolBarPane != null) return toolBarPane;

        toolBarPane = new JToolBar();

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
        b.setHorizontalTextPosition(SwingConstants.RIGHT);

        b = toolBarPane.add(getFindAction());
        b.setFont(new Font("Dialog", 1, 10));
        b.setText((String)getFindAction().getValue(Action.NAME));
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
     * @return JToolBar
     */
    private JToolBar getPolicyToolBar() {
        if (policyToolBar != null) return policyToolBar;
        policyToolBar = new PolicyToolBar();
        policyToolBar.registerPaletteTree(getAssertionPaletteTree());
        policyTree = Registry.getDefault().getComponentRegistry().getPolicyTree();
        policyToolBar.registerPolicyTree(policyTree);
        return policyToolBar;
    }


    /**
     * Return the TreeJPanel property value.
     * @return JPanel
     */
    private JPanel getMainLeftJPanel() {
        if (mainLeftJPanel != null)
            return mainLeftJPanel;

        JTabbedPane treePanel = new JTabbedPane();
        treePanel.setTabPlacement(JTabbedPane.BOTTOM);
        //treePanel.setLayout(new BorderLayout());
        treePanel.addTab("Assertions", getAssertionPaletteTree());

        JScrollPane js = new JScrollPane(treePanel);
        int mInc = js.getVerticalScrollBar().getUnitIncrement();
        // some arbitrary text to set the unit increment to the
        // height of one line instead of default value
        int vInc = (int)getStatusMsgLeft().getPreferredSize().getHeight();
        js.getVerticalScrollBar().setUnitIncrement(Math.max(mInc, vInc));
        int hInc = (int)getStatusMsgLeft().getPreferredSize().getWidth();
        js.getHorizontalScrollBar().setUnitIncrement(Math.max(mInc, hInc));

        final JSplitPane sections = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        addWindowListener(new WindowAdapter() {
            /** Invoked when a window has been opened. */
            public void windowOpened(WindowEvent e) {
                try {
                    Preferences prefs = Preferences.getPreferences();
                    String s = prefs.getString("tree.split.divider.location");
                    if (s != null) {
                        int l = Integer.parseInt(s);
                        sections.setDividerLocation(l);
                    }
                } catch (IOException e1) {

                } catch (NumberFormatException e1) {
                }
            }

            /** Invoked when a window has been closed. */
            public void windowClosed(WindowEvent e) {
                try {
                    Preferences prefs = Preferences.getPreferences();
                    int l = sections.getDividerLocation();
                    prefs.putProperty("tree.split.divider.location", Integer.toString(l));
                } catch (IOException e1) {
                } catch (NullPointerException e1) {
                }
            }
        });


        sections.setTopComponent(js);

        treePanel = new JTabbedPane();
        // treePanel.setLayout(new BorderLayout());
        treePanel.addTab("Services", getServicesTree());
        treePanel.setTabPlacement(JTabbedPane.BOTTOM);


        js = new JScrollPane(treePanel);
        mInc = js.getVerticalScrollBar().getUnitIncrement();
        // some arbitrary text to set the unit increment to the
        // height of one line instead of default value
        vInc = (int)getStatusMsgLeft().getPreferredSize().getHeight();
        js.getVerticalScrollBar().setUnitIncrement(Math.max(mInc, vInc));
        hInc = (int)getStatusMsgLeft().getPreferredSize().getWidth();
        js.getHorizontalScrollBar().setUnitIncrement(Math.max(mInc, hInc));
        sections.setBottomComponent(js);
        sections.setDividerSize(10);


        mainLeftJPanel = new JPanel(new BorderLayout());
        mainLeftJPanel.add(sections, BorderLayout.CENTER);
        mainLeftJPanel.add(getPolicyToolBar(), BorderLayout.EAST);
        return mainLeftJPanel;
    }



    // --- Event listeners ---------------------------------------

    /**
     * The connect handler.
     * todo: rework with connect listeners
     * @param event  ActionEvent
     */
    private void connectHandler(ActionEvent event) {
        logon();
    }

    /**
     * The disconnect handler.
     *
     * @param event  ActionEvent
     */
    private void disconnectHandler(ActionEvent event) {
        fireDisconnected();
        getStatusMsgLeft().setText("Disconnected");
        getStatusMsgRight().setText("");

        getAssertionPaletteTree().setModel(null);
        getMainSplitPaneRight().removeAll();
        getMainSplitPaneRight().validate();
        getMainSplitPaneRight().repaint();
        getServicesTree().setModel(null);

        updateActions(null);
        // if inactivityTimer is running stop
        if (inactivityTimer.isRunning()) {
            inactivityTimer.stop();
        }

        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  System.gc(); // hack to help clearing references
              }
          });
    }

    /**
     * refresh the children under the node
     *
     * @param node   the node to refresh
     */
    private void refreshNode(EntityTreeNode node) {
        JTree tree = getAssertionPaletteTree();

        node.removeAllChildren();
        TreePath path = new TreePath(node.getPath());
        tree.expandPath(path);
        tree.setSelectionPath(path);
    }

    /**
     * update the actions, menus, buttons for the selected node.
     *
     * @param node   currently selected node
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
        this.dispose();
    }

    /**
     * Initializes listeners for the form
     */
    private void initListeners() {
        try {
            final Preferences prefs = Preferences.getPreferences();
            prefs.addPropertyChangeListener(
              new PropertyChangeListener() {
                  /**
                   * This method gets called when a property is changed.
                   * @param evt A PropertyChangeEvent object describing the
                   * event source and the property that has changed.
                   */
                  public void propertyChange(PropertyChangeEvent evt) {
                      MainWindow.log.info("preferences have been updated");
                      MainWindow.this.setLookAndFeel(prefs.getString(Preferences.LOOK_AND_FEEL));
                      MainWindow.this.setInactivitiyTimeout(prefs.getInactivityTimeout());
                  }
              });
        } catch (IOException e) {
            log.log(Level.WARNING, "cannot get preferences", e);
        }
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
                  showHelpTopics();
              }
          });
    }


    // --- End Event listeners ---------------------------------------

    /**
     * Initialize the class.
     */
    private void initialize() throws IOException {
        Preferences prefs = Preferences.getPreferences();
        initializePreferences();

        String lfName = prefs.getString(Preferences.LOOK_AND_FEEL);
        if (lfName == null) {
            //setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } else {
            setLookAndFeel(lfName);
        }

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("MainWindow");
        setJMenuBar(getMainJMenuBar());
        setTitle(resapplication.getString("SSG"));
        ImageIcon imageIcon =
          new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png"));
        setIconImage(imageIcon.getImage());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);
        getContentPane().add(getStatusBarPane(), BorderLayout.SOUTH);


        initListeners();


        toggleConnectedMenus(false);
        /* Pack frame on the screen */
        pack();
        /* restore window position */
        initializeWindowPosition();
        SwingUtilities.
          invokeLater(new Runnable() {
              public void run() {
                  int key =
                    connectMenuItem.getAccelerator().getKeyCode();
                  int modifiers =
                    connectMenuItem.getAccelerator().getModifiers();
                  dispatchEvent(new KeyEvent(MainWindow.this,
                    KeyEvent.KEY_PRESSED,
                    0,
                    modifiers,
                    key));
              }
          });
    }

    /**
     * Tweak any global preferences.
     */
    private void initializePreferences() {
        try {
            // see http://developer.java.sun.com/developer/bugParade/bugs/4155617.html
            // same problem exists for JWS
            UIManager.put("ClassLoader", cl);
            KunststoffLookAndFeel lf = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
            KunststoffLookAndFeel.setCurrentTheme(new KunststoffDesktopTheme());
            Preferences prefs = Preferences.getPreferences();
        } catch (IOException e) {
            log.log(Level.WARNING, "cannot get preferences", e);
        }
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

        if (!posWasSet)
            Utilities.centerOnScreen(this);
    }


    /**
     * The "Help Topics".
     * This procedure adds the JavaHelp to PMC application.
     */
    public void showHelpTopics() {
        HelpSet hs = null;
        URL url = null;
        HelpBroker javaHelpBroker = null;
        String helpsetName = "SSG Help";

        try {
            // Find the helpSet URL file.
            url = cl.getResource(HELP_PATH);
            hs = new HelpSet(cl, url);
            javaHelpBroker = hs.createHelpBroker();
            javaHelpBroker.setDisplayed(true);

        } catch (MissingResourceException e) {
            //Make sure the URL exists.
            if (url == null) {
                JOptionPane.showMessageDialog(null,
                  "Help URL is missing",
                  "Bad HelpSet Path ",
                  JOptionPane.WARNING_MESSAGE);
            }
        } catch (HelpSetException hex) {
            JOptionPane.showMessageDialog(null,
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

            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {

                long now = System.currentTimeMillis();
                double inactive = (now - lastStamp);
                if (Math.round(inactive / inactivityTimer.getDelay()) >= 1) { // match
                    inactivityTimer.stop(); // stop timer
                    MainWindow.this.getStatusMsgRight().
                      setText("inactivity timeout expired; disconnecting...");
                    // make sure it is invoked on event dispatching thread
                    SwingUtilities.invokeLater(
                      new Runnable() {
                          public void run() {
                              MainWindow.this.disconnectHandler(null);
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
     * @param lookAndFeel
     *               a string specifying the name of the class that implements
     *               the look and feel
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
                UIManager.setLookAndFeel(UIManager.
                  getCrossPlatformLookAndFeelClassName());
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
     *
     * @return true if the logon was succesfully validated, false
     *         otherwise
     */
    private boolean logon() {
        LogonDialog.logon(this, logonListenr);
        return true;
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

    private
    LogonDialog.LogonListener logonListenr =
      new LogonDialog.LogonListener() {
          /* invoked on authentication success */
          public void onAuthSuccess(String id) {
              getStatusMsgLeft().setText(id);
              /* load user preferences and merge them with system props */
              try {
                  Preferences prefs = Preferences.getPreferences();
                  if (prefs.rememberLoginId()) {
                      prefs.putProperty(Preferences.LAST_LOGIN_ID, id);
                      prefs.store();
                  }
              } catch (IOException e) {
                  log.log(Level.WARNING, "onAuthSuccess()", e);
              }
              initalizeWorkspace();
              int timeout = 0;
              try {
                  timeout = Preferences.getPreferences().getInactivityTimeout();
              } catch (IOException e) {
                  log.log(Level.WARNING, "unable to get preferences", e);
              }
              final int fTimeout = timeout;
              SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        MainWindow.this.
                          setInactivitiyTimeout(fTimeout);
                        MainWindow.this.fireConnected();
                    }
                });
              toggleConnectedMenus(true);
              new HomeAction().actionPerformed(null);
          }

          /* invoked on authentication failure */
          public void onAuthFailure() {
              ;
          }
      };
}
