package com.l7tech.console;

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import com.l7tech.console.panels.*;
import com.l7tech.console.sbar.JOutlookBar;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.Preferences;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import org.apache.log4j.Category;

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * The console main window <CODE>MainWindow</CODE> class.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class MainWindow extends JFrame {
    static final Category log = Category.getInstance(MainWindow.class.getName());
    /** the resource path for the application */
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";

    /** the path to JavaHelp helpset file */
    public static final String HELP_PATH = "com/l7tech/console/resources/helpset/console.hs";

    /** the resource bundle name */
    private static
            ResourceBundle resapplication =
            java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    private JMenuBar mainJMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu editMenu = null;
    private JMenu viewMenu = null;
    private JMenu helpMenu = null;
    private JMenu tBarsOptionMenu = null;

    private JMenuItem connectMenuItem = null;
    private JMenuItem disconnectMenuItem = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem menuItemPref = null;

    private JMenu newMenu = null;
//  private JMenuItem editMenuItem = null;
    private JMenuItem deleteMenuItem = null;

    private JMenuItem aboutBoxMenuItem = null;
    private JMenuItem booksOnlineMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;

    private Action refreshAction = null;
    private Action findAction = null;
    private Action prefsAction = null;
    private Action removeNodeAction = null;
    private Action connectAction = null;
    private Action disconnectAction = null;

    private JPanel frameContentPane = null;
    private JPanel mainPane = null;
    private JPanel statusBarPane = null;
    private JLabel statusMsgLeft = null;
    private JLabel statusMsgRight = null;

    private JToolBar toolBarPane = null;
    private JTree treeDirectoryView = null;
    private JSplitPane mainJSplitPane = null;
    private JPanel mainLeftJPanel = null;

    private JPanel objectBrowserPane = null;

    /* progress bar indicator */
    private JProgressBar progressBar = null;

    // panel that lists container
    private final
            ContainerListPanel cListPanel = new ContainerListPanel();

    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();

    /** the panel listener broker */
    private final
            PanelListenerBroker listenerBroker = new PanelListenerBroker();
    private DirectoryTreeNode editingNode = null;

    /**
     * MainWindow constructor comment.
     * @param title java.lang.String
     */
    public MainWindow(String title) throws IOException {
        super(title);
        initialize();
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
        if (disconnectMenuItem == null) {
            disconnectMenuItem = new JMenuItem(getDisconnectAction());
            disconnectMenuItem.setIcon(null);

            int mnemonic = disconnectMenuItem.getText().toCharArray()[0];
            disconnectMenuItem.setMnemonic(mnemonic);
            disconnectMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return disconnectMenuItem;
    }


    /**
     * Return the menuItemPref property value.
     * @return JMenuItem
     */
    private JMenuItem getJMenuItemPref() {
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
     * Return the newMenu property value.
     * @return JMenuItem
     */
    private JMenu getNewMenu() {
        if (newMenu == null) {
            newMenu = new JMenu();
            newMenu.setText(resapplication.getString("New_MenuItem_text"));
        }
        return newMenu;
    }


    /**
     * Return the deleteMenuItem property value.
     * @return JMenuItem
     */
    private JMenuItem getDeleteMenuItem() {
        if (deleteMenuItem == null) {
            deleteMenuItem = new JMenuItem(getRemoveNodeAction());
            deleteMenuItem.setIcon(null);
            int mnemonic = 'X';
            deleteMenuItem.setMnemonic(mnemonic);
            deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.CTRL_MASK));
        }
        return deleteMenuItem;
    }

    /**
     * Return the aboutBoxMenuItem property value.
     * @return JMenuItem
     */
    private JMenuItem getAboutBoxMenuItem() {
        if (aboutBoxMenuItem == null) {
            aboutBoxMenuItem = new JMenuItem();
            aboutBoxMenuItem.setText(resapplication.getString("About_BoxMenuItem_text"));
            int mnemonic = aboutBoxMenuItem.getText().toCharArray()[0];
            aboutBoxMenuItem.setMnemonic(mnemonic);
            aboutBoxMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return aboutBoxMenuItem;
    }

    /**
     * Return the BooksOnlineMenuItem property value.
     * @return JMenuItem
     */
    private JMenuItem getBooksOnlineMenuItem() {
        if (booksOnlineMenuItem == null) {
            booksOnlineMenuItem = new JMenuItem();
            booksOnlineMenuItem.setText(resapplication.getString("Books_OnlineMenuItem_text"));
            int mnemonic = booksOnlineMenuItem.getText().toCharArray()[0];
            booksOnlineMenuItem.setMnemonic(mnemonic);
            booksOnlineMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return booksOnlineMenuItem;
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
            fileMenu.add(getJMenuItemPref());
            fileMenu.addSeparator();
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
            editMenu.add(getNewMenu());

            editMenu.add(getDeleteMenuItem());
/*      editMenu.add(getCopyMenuItem());
      editMenu.add(getPasteMenuItem());*/
            //editMenu.addSeparator();
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
        if (viewMenu == null) {
            viewMenu = new JMenu();
            viewMenu.setText(resapplication.getString("View"));
            // workaround to disable icon on the menu
            viewMenu.add(getToolbarsSubmenu());
            JMenuItem item = new JMenuItem(getRefreshAction());
            item.setIcon(null);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
            viewMenu.add(item);
            int mnemonic = viewMenu.getText().toCharArray()[0];
            viewMenu.setMnemonic(mnemonic);
        }
        return viewMenu;
    }

    /**
     * Return the newMenu property value.
     * @return JMenuItem
     */
    private JMenu getToolbarsSubmenu() {
        if (tBarsOptionMenu == null) {
            tBarsOptionMenu = new JMenu("Toolbars");
            int mnemonic = tBarsOptionMenu.getText().toCharArray()[0];
            tBarsOptionMenu.setMnemonic(mnemonic);

            ButtonGroup group = new ButtonGroup();
            JRadioButtonMenuItem rbMenuItem;

            rbMenuItem = new JRadioButtonMenuItem("Icons and labels");
            group.add(rbMenuItem);
            tBarsOptionMenu.add(rbMenuItem);
            tBarsOptionMenu.addActionListener(new ActionListener() {
                /** Invoked when an action occurs.*/
                public void actionPerformed(ActionEvent e) {
                }
            });

            rbMenuItem = new JRadioButtonMenuItem("Text labels");
            group.add(rbMenuItem);
            tBarsOptionMenu.add(rbMenuItem);
            tBarsOptionMenu.addActionListener(new ActionListener() {
                /** Invoked when an action occurs.*/
                public void actionPerformed(ActionEvent e) {
                }
            });

            rbMenuItem = new JRadioButtonMenuItem("Icons");
            group.add(rbMenuItem);
            tBarsOptionMenu.add(rbMenuItem);
            tBarsOptionMenu.addActionListener(new ActionListener() {
                /** Invoked when an action occurs.*/
                public void actionPerformed(ActionEvent e) {
                }
            });
        }
        return tBarsOptionMenu;
    }

    /**
     * Return the helpMenu property value.
     * @return JMenu
     */
    private JMenu getHelpMenu() {
        if (helpMenu == null) {
            helpMenu = new JMenu();
            helpMenu.setText(resapplication.getString("Help"));
            helpMenu.add(getHelpTopicsMenuItem());
            //helpMenu.add(getBooksOnlineMenuItem());
            helpMenu.add(getAboutBoxMenuItem());
            int mnemonic = helpMenu.getText().toCharArray()[0];
            helpMenu.setMnemonic(mnemonic);
        }
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
                        JTree tree = getJTreeDirectoryView();
                        DirectoryTreeNode node =
                                (DirectoryTreeNode)tree.getLastSelectedPathComponent();

                        if (node != null) {
                            refreshNode(node);
                        }
                    }
                };
        refreshAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return refreshAction;
    }

    /**
     * create the Action (the component that is used by several
     * controls) that returns the 'find' action.
     *
     * @return the find <CODE>Action</CODE>
     */
    private Action getFindAction() {
        if (findAction != null) return findAction;
        String atext = resapplication.getString("Find_MenuItem_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Find16.gif"));

        findAction =
                new AbstractAction(atext, icon) {
                    /**
                     * Invoked when an action occurs.
                     *
                     * @param event  the event that occured
                     * @see Action#removePropertyChangeListener
                     */
                    public void actionPerformed(ActionEvent event) {

                        DirectoryTreeNode context =
                                (DirectoryTreeNode)getJTreeDirectoryView().getModel().getRoot();
                        JDialog d = new FindDialog(MainWindow.this, true, context, listenerBroker);
                        d.setLocation(MainWindow.this.getLocationOnScreen());
                        d.show();
                    }
                };
        refreshAction.putValue(Action.SHORT_DESCRIPTION, atext);
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
                        JTree tree = getJTreeDirectoryView();
                        DirectoryTreeNode node =
                                (DirectoryTreeNode)tree.getLastSelectedPathComponent();

                        if (node != null) {
                            removeNode(node);
                        }
                    }
                };
        removeNodeAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return removeNodeAction;
    }

    /**
     * enable or disable menus and buttons, depending on the
     * connection status
     *
     * @param connected true if connected, false otherwise
     */
    private void toggleConnectedMenus(boolean connected) {
        getDisconnectAction().setEnabled(connected);
        getConnectAction().setEnabled(!connected);
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
    private JPanel getObjectBrowserPane() {
        if (objectBrowserPane == null) {
            objectBrowserPane = new JPanel();
            objectBrowserPane.setLayout(new GridBagLayout());
        }
        return objectBrowserPane;
    }

    /**
     * Return the JTreeDirectoryView property value.
     * @return JTree
     */
    private JTree getJTreeDirectoryView() {
        if (treeDirectoryView == null) {
            treeDirectoryView = new JTree();
            treeDirectoryView.setShowsRootHandles(true);
            treeDirectoryView.setLargeModel(true);
            treeDirectoryView.setCellRenderer(new EntityTreeCellRenderer());
            treeDirectoryView.putClientProperty("JTree.lineStyle", "Angled");

            TreeNode node = new DefaultMutableTreeNode("Disconnected");
            treeDirectoryView.setUI(CustomTreeUI.getTreeUI());

            DefaultTreeModel treeModel =
                    new DefaultTreeModel(node);
            getJTreeDirectoryView().setModel(treeModel);
            TreePath path = new TreePath(node);
            treeDirectoryView.setSelectionPath(path);
            updateActions(null);
        }
        return treeDirectoryView;
    }

    /**
     * Create the directory root object, and instanitate the
     * model.
     */
    private void setJtreeRootNode() {
        final DirectoryTreeNode node =
                new DirectoryTreeNode(new RootNode());
        TreeModel treeModel = new FilteredTreeModel(node);
        getJTreeDirectoryView().setRootVisible(false);
        getJTreeDirectoryView().setModel(treeModel);
        TreePath path = new TreePath(node.getPath());
        getJTreeDirectoryView().setSelectionPath(path);
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
     * Return the mainJSplitPane property value.
     * @return JSplitPane
     */
    private JSplitPane getMainJSplitPane() {
        if (mainJSplitPane == null) {
            mainJSplitPane =
                    new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            mainJSplitPane.setDividerLocation(200);
            getMainJSplitPane().add(getObjectBrowserPane(), "right");

            getMainJSplitPane().add(getMainLeftJPanel(), "left");
        }
        return mainJSplitPane;
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

            // a bit of a hack here , set the size to the size of "disconnected" label
//      progressBar.setPreferredSize(getStatusMsgLeft().getPreferredSize());
//      progressBar.setMaximumSize(getStatusMsgLeft().getMaximumSize());
//
//      progressBar.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
//      progressBar.setDoubleBuffered(true);
//      rightPanel.add(progressBar, BorderLayout.EAST);
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
        if (toolBarPane == null) {
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
        }
        return toolBarPane;
    }

    /**
     * Return the TreeJPanel property value.
     * @return JPanel
     */
    private JPanel getMainLeftJPanel() {

        if (mainLeftJPanel == null) {

            JPanel treePanel = new JPanel();
            treePanel.setLayout(new BorderLayout());
            treePanel.add(getJTreeDirectoryView(), "Center");

            JScrollPane js = new JScrollPane(treePanel);
            int mInc = js.getVerticalScrollBar().getUnitIncrement();
            // some arbitrary text to set the unit increment to the
            // height of one line instead of default value
            int vInc = (int)getStatusMsgLeft().getPreferredSize().getHeight();
            js.getVerticalScrollBar().setUnitIncrement(Math.max(mInc, vInc));

            int hInc = (int)getStatusMsgLeft().getPreferredSize().getWidth();
            js.getHorizontalScrollBar().setUnitIncrement(Math.max(mInc, hInc));
            ActionListener l = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                }
            };

            JOutlookBar outlook = new JOutlookBar();

            // here is the sequece of commands adding buttons to specific folders
            // Folder "Internet"
            outlook.addIcon("Shortcut Bar", "Services", RESOURCE_PATH + "/services32.png", l);
            outlook.addIcon("Shortcut Bar", "Policies", RESOURCE_PATH + "/policy32.gif", l);
            outlook.addIcon("Shortcut Bar", "Providers", RESOURCE_PATH + "/providers32.gif", l);
            outlook.addIcon("Shortcut Bar", "Users", RESOURCE_PATH + "/user32.png", l);
            outlook.addIcon("Shortcut Bar", "Groups", RESOURCE_PATH + "/group32.png", l);

            JSplitPane sections = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, outlook, js);
            sections.setDividerLocation(150);
            sections.setOneTouchExpandable(true);
            mainLeftJPanel = new JPanel(new BorderLayout());
            mainLeftJPanel.add(sections, "Center");
        }
        return mainLeftJPanel;
    }

    /**
     * Return the TreeNodeJPopupMenu property value.
     * @return JPopupMenu
     */
    private JPopupMenu getTreeNodeJPopupMenu(final TreeNode node) {
        if (node == null || !(node instanceof DirectoryTreeNode)) {
            return null;
        }
        ActionListener listener = new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                JPanel panel = null;
                DirectoryTreeNode dNode = (DirectoryTreeNode)node;
                Object object = dNode.getUserObject();
                DefaultMutableTreeNode parent =
                        (DefaultMutableTreeNode)node.getParent();

                if (TreeNodeMenu.DELETE.equals(e.getActionCommand())) {
                    removeNode(dNode);
                } else if (TreeNodeMenu.NEW_ADMINISTRATOR.equals(e.getActionCommand())) {
                    AdminFolderNode adminFolder = (AdminFolderNode)object;
                    NewAdminDialog dialog = new NewAdminDialog(MainWindow.this, adminFolder);
                    dialog.setResizable(false);
                    dialog.setPanelListener(listenerBroker);
                    dialog.show();
                } else if (TreeNodeMenu.NEW_GROUP.equals(e.getActionCommand())) {
                    NewGroupDialog dialog =
                            new NewGroupDialog(MainWindow.this, (EntityHeader)parent.getUserObject());
                    dialog.setResizable(false);
                    dialog.setPanelListener(listenerBroker);
                    dialog.show();
                } else if (TreeNodeMenu.NEW_USER.equals(e.getActionCommand())) {
                    NewUserDialog dialog =
                            new NewUserDialog(MainWindow.this, (EntityHeader)parent.getUserObject());
                    dialog.setResizable(false);
                    dialog.setPanelListener(listenerBroker);
                    dialog.show();
                } else if (TreeNodeMenu.NEW_PROVIDER.equals(e.getActionCommand())) {
                    NewProviderDialog dialog = new NewProviderDialog(MainWindow.this);
                    dialog.setResizable(false);
                    dialog.setPanelListener(listenerBroker);
                    dialog.show();
                } else if (TreeNodeMenu.PROPERTIES.equals(e.getActionCommand())) {
                    panel = PanelFactory.getPanel(dNode, true, listenerBroker);
                } else if (TreeNodeMenu.BROWSE.equals(e.getActionCommand())) {
                    getJTreeDirectoryView().expandPath(getJTreeDirectoryView().getSelectionPath());
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Not yet implemented.",
                            "Information",
                            JOptionPane.INFORMATION_MESSAGE);
                }

                // only if something is returned
                if (panel != null) {
                    EditorDialog dialog = new EditorDialog(MainWindow.this, panel);

                    dialog.pack();
                    Utilities.centerOnScreen(dialog);
                    dialog.show();
                }
            }
        };
        return TreeNodeMenu.forNode((DirectoryTreeNode)node, listener);
    }


    // --- Event listeners ---------------------------------------

    /**
     * The about box handler.
     *
     * @param event  ActionEvent
     */
    private void aboutBoxHandler(ActionEvent event) {
        this.showAboutBox();
    }

    /**
     * The connect handler.
     *
     * @param event  ActionEvent
     */
    private void connectHandler(ActionEvent event) {
        if (logon()) {
            toggleConnectedMenus(true);
        }
    }

    /**
     * The disconnect handler.
     *
     * @param event  ActionEvent
     */
    private void disconnectHandler(ActionEvent event) {
        LogonDialog.logoff();
        DefaultTreeModel treeModel =
                new DefaultTreeModel(new DefaultMutableTreeNode("Disconnected"));
        getStatusMsgLeft().setText("Disconnected");
        getStatusMsgRight().setText("");

        getJTreeDirectoryView().setModel(treeModel);
        getObjectBrowserPane().removeAll();
        getObjectBrowserPane().validate();
        getObjectBrowserPane().repaint();

        updateActions(null);
        // if inactivityTimer is running stop
        if (inactivityTimer.isRunning()) {
            inactivityTimer.stop();
        }

    }

    // the PanelListener that handles the object events
    // and performs the corresponding tree action
    private PanelListener
            treeObjectListener = new PanelListenerAdapter() {
                /**
                 * invoked after insert
                 *
                 * @param object an arbitrary object set by the Panel
                 */
                public void onInsert(Object object) {
                    BasicTreeNode newNode =
                            TreeNodeFactory.getTreeNode((EntityHeader)object);
                    if (newNode.isLeaf()) return;

                    JTree tree = getJTreeDirectoryView();
                    TreePath path = tree.getSelectionPath();
                    // do not add if never expanded
                    if (!tree.hasBeenExpanded(path)) return;

                    DirectoryTreeNode pNode =
                            (DirectoryTreeNode)tree.getLastSelectedPathComponent();

                    DirectoryTreeNode node = new DirectoryTreeNode(newNode);
                    pNode.add(node);
                    pNode.sortChildren(DirectoryTreeNode.DEFAULT_COMPARATOR);
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.nodeStructureChanged(pNode);
                }


                /**
                 * invoked after update
                 *
                 * @param object an arbitrary object set by the Panel
                 */
                public void onUpdate(Object object) {
                    BasicTreeNode newNode =
                            TreeNodeFactory.getTreeNode((EntityHeader)object);

                    if (newNode.isLeaf() ||
                            !(newNode instanceof EntityHeader))
                        return;
                    EntityHeader en = (EntityHeader)newNode;

                    JTree tree = getJTreeDirectoryView();
                    TreePath path = tree.getSelectionPath();
                    // do not update if never expanded
                    if (!tree.hasBeenExpanded(path)) return;


                    DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode)TreeNodeAction.
                            nodeByName(en.getName(), (DefaultMutableTreeNode)path.getLastPathComponent());

                    if (node == null) {
                        throw new
                                IllegalStateException("Update of node that isn't in tree ( " + en.getName() + " )");
                    }
                    node.setUserObject(newNode);
                }

                /**
                 * invoked after object delete
                 *
                 * @param object an arbitrary object set by the Panel
                 */
                public void onDelete(Object object) {
                    BasicTreeNode newNode =
                            TreeNodeFactory.getTreeNode((EntityHeader)object);

                    if (newNode.isLeaf() ||
                            !(newNode instanceof EntityHeader))
                        return;

                    EntityHeader en = (EntityHeader)newNode;

                    JTree tree = getJTreeDirectoryView();
                    TreePath path = tree.getSelectionPath().getParentPath();

                    if (tree.hasBeenExpanded(path)) {
                        DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode)TreeNodeAction.
                                nodeByName(en.getName(), (DefaultMutableTreeNode)path.getLastPathComponent());

                        if (node == null) {
                            throw new
                                    IllegalStateException("Update of node that isn't in tree ( " + en.getName() + " )");
                        }
                        ((DefaultTreeModel)tree.getModel()).removeNodeFromParent(node);
                    }
                }
            };

    /**
     * Updates the right panel to edit the given node.
     */
    private void activateBrowserPanel(DirectoryTreeNode node) {
        editingNode = node;

        JPanel panel;
        // check if node/context supports listing
        panel = getContextListPanel(node);

        // only if something is returned
        if (panel != null) {
            if (panel instanceof EditorPanel) {
                ((EditorPanel)panel).setPanelListener(listenerBroker);
            }
            getObjectBrowserPane().removeAll();
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
            getObjectBrowserPane().add(panel, constraints);
            getObjectBrowserPane().validate();
            getObjectBrowserPane().repaint();
        }
    }

    /**
     * return the context list panel for the node, or null if node
     * is not a 'listable' container.
     *
     * @param node   DirectoryTreeNode representing the context
     * @return The <CODE>JPanel</CODE> that lists the context, null if
     *         the context does not support listing.
     */
    private JPanel getContextListPanel(DirectoryTreeNode node) {

        if (!node.isLeaf()) {
            cListPanel.setParentNode(getJTreeDirectoryView(), node);
            return cListPanel;
        }
        return null;
    }

    /**
     * Invoked on node selection change, update the right panel
     *
     * @param event
     * @see TreeSelectionEvent for details
     */
    private void treeSelectionEventHandler(TreeSelectionEvent event) {
        // get the node and call panel factory
        Object object = getJTreeDirectoryView().getLastSelectedPathComponent();
        // if not DirectoryTreeNode silently return
        if (!(object instanceof DirectoryTreeNode)) {
            return;
        }
        DirectoryTreeNode node = (DirectoryTreeNode)object;
        // update actions for the node
        updateActions(node);
        activateBrowserPanel(node);
    }

    /**
     * refresh the children under the node
     *
     * @param node   the node to refresh
     */
    private void refreshNode(DirectoryTreeNode node) {
        JTree tree = getJTreeDirectoryView();
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

        node.removeAllChildren();
        TreePath path = new TreePath(node.getPath());
        tree.expandPath(path);
        tree.setSelectionPath(path);
    }

    /**
     * remove the node from the MW and from the tree
     *
     * @param node   the node to remove
     */
    private void removeNode(DirectoryTreeNode node) {
        // store the parent node to use as a panel for later
        DirectoryTreeNode parentNode = (DirectoryTreeNode)node.getParent();
        if (!TreeNodeAction.deleteNode(node)) return;
        treeObjectListener.onDelete(node.getUserObject());

        // node deleted now change selection to parent
        TreePath tPath = new TreePath(parentNode.getPath());
        getJTreeDirectoryView().getSelectionModel().setSelectionPath(tPath);
    }

    /**
     * update the actions, menus, buttons for the selected node.
     *
     * @param node   currently selected node
     */
    private void updateActions(DirectoryTreeNode node) {
        JMenu nMenu = getNewMenu();
        nMenu.removeAll();
        nMenu.setEnabled(false);

        if (node == null) {
            toggleConnectedMenus(false);
            getRemoveNodeAction().setEnabled(false);
            getFindAction().setEnabled(false);
            return;
        }

        Container cont = getTreeNodeJPopupMenu(node);

        if (cont != null) {
            Component[] components = cont.getComponents();
            Utilities.equalizeComponentSizes(components);
            for (int i = 0; components != null && i < components.length; i++) {
                if (components[i] instanceof JMenu &&
                        ((JMenu)components[i]).getText().equals(TreeNodeMenu.NEW)) {
                    JMenu menu = (JMenu)components[i];
                    Component[] nItems = menu.getMenuComponents();
                    Utilities.equalizeComponentSizes(nItems);
                    for (int j = 0; nItems != null && j < nItems.length; j++) {
                        nMenu.add(nItems[j]);
                    }
                    nMenu.setEnabled(true);
                    break;
                }
            }
        }
        getRemoveNodeAction().setEnabled(TreeNodeAction.canDelete(node));
        getFindAction().setEnabled(true);
    }

    /**
     * @param event ActionEvent
     * @see ActionEvent for details
     */
    private void exitMenuEventHandler(ActionEvent event) {
        this.dispose();
    }

    /**
     * Handle the mouse click popup when the Tree item is right clicked. The context sensitive
     * menu is displayed if the right click was over an item.
     *
     * @param mouseEvent
     */
    public void jTreeDirectoryViewPopUpEventHandler(MouseEvent mouseEvent) {
        JTree tree = getJTreeDirectoryView();

        if (mouseEvent.isPopupTrigger()) {
            int closestRow = tree.getClosestRowForLocation(mouseEvent.getX(), mouseEvent.getY());

            if (closestRow != -1
                    && tree.getRowBounds(closestRow).contains(mouseEvent.getX(), mouseEvent.getY())) {
                int[] rows = tree.getSelectionRows();
                boolean found = false;

                for (int i = 0; rows != null && i < rows.length; i++) {
                    if (rows[i] == closestRow) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    tree.setSelectionRow(closestRow);
                }
                TreeNode node = (TreeNode)tree.getLastSelectedPathComponent();

                JPopupMenu menu = getTreeNodeJPopupMenu(node);
                if (menu != null) {
                    menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        }
        return;
    }

    /**
     * Handle the <code>Jtree</code> will expand event
     *
     * @param event
     */
    public void jTreeDirectoryViewWillExpandEventHandler(TreeExpansionEvent event)
            throws ExpandVetoException {
        ;
    }

    /**
     * Handle the <code>Jtree</code> expanded event
     *
     * @param event
     */
    public void jTreeDirectoryViewTreeExpandedEventHandler(TreeExpansionEvent event) {
        ;
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
                            MainWindow.this.log.debug("preferences have been updated");
                            MainWindow.this.setLookAndFeel(prefs.getString(prefs.LOOK_AND_FEEL));
                            MainWindow.this.setInactivitiyTimeout(prefs.getInactivityTimeout());
                        }
                    });
        } catch (IOException e) {
            log.warn("cannot get preferences", e);
        }

        // exitMenuItem listener
        getExitMenuItem().
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        exitMenuEventHandler(e);
                    }
                });


        getAboutBoxMenuItem().
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        aboutBoxHandler(e);
                    }
                });

        // HelpTopics listener
        getHelpTopicsMenuItem().
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showHelpTopics();
                    }
                });

        final JTree tree = getJTreeDirectoryView();
        // JTree listeners
        tree.
                addTreeSelectionListener(
                        new TreeSelectionListener() {
                            public void valueChanged(TreeSelectionEvent e) {
                                if (TreeWorker.active()) {
                                    TreeWorker.stopWorker();
                                }
                                try {
                                    getContentPane().
                                            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                    treeSelectionEventHandler(e);
                                } catch (Exception ex) {
                                    log.error("main()", ex);
                                } finally {
                                    getContentPane().
                                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                }
                            }
                        });

        tree.
                addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        ;
                    }

                    public void mouseEntered(MouseEvent e) {
                        ;
                    }

                    public void mouseExited(MouseEvent e) {
                        ;
                    }

                    public void mousePressed(MouseEvent e) {
                        jTreeDirectoryViewPopUpEventHandler(e);
                    }

                    public void mouseReleased(MouseEvent e) {
                        jTreeDirectoryViewPopUpEventHandler(e);
                    }
                });

        tree.
                addTreeWillExpandListener(
                        new TreeWillExpandListener() {
                            /**
                             * Invoked whenever a node in the tree is about to be collapsed.
                             */
                            public void treeWillCollapse(TreeExpansionEvent event)
                                    throws ExpandVetoException {
                                ;
                            }

                            /**
                             * Invoked whenever a node in the tree is about to be expanded.
                             */
                            public void treeWillExpand(TreeExpansionEvent event)
                                    throws ExpandVetoException {
                                jTreeDirectoryViewWillExpandEventHandler(event);
                            }
                        });


        tree.
                addTreeExpansionListener(new TreeExpansionListener() {
                    /**
                     * Called whenever an item in the tree has been expanded.
                     */
                    public void treeExpanded(TreeExpansionEvent event) {
                        jTreeDirectoryViewTreeExpandedEventHandler(event);
                    }

                    /**
                     * Called whenever an item in the tree has been collapsed.
                     */
                    public void treeCollapsed(TreeExpansionEvent event) {
                        TreeWorker.stopWorker();
                    }
                });


        tree.
                addKeyListener(new KeyAdapter() {
                    /** Invoked when a key has been pressed.*/
                    public void keyPressed(KeyEvent e) {
                        TreePath path = tree.getSelectionPath();
                        if (path == null) return;

                        DirectoryTreeNode node =
                                (DirectoryTreeNode)path.getLastPathComponent();
                        if (node == null) return;
                        int keyCode = e.getKeyCode();
                        if (keyCode == KeyEvent.VK_DELETE) {
                            removeNode(node);
                        } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                            DefaultMutableTreeNode parent =
                                    (DefaultMutableTreeNode)node.getParent();
                            if (parent == null) return;

                            TreeNode[] nodes = parent.getPath();
                            final TreePath nPath = new TreePath(nodes);
                            if (!tree.isExpanded(nPath)) {
                                tree.expandPath(nPath);
                            }
                            tree.setSelectionPath(nPath);
                        }
                    }
                });

        listenerBroker.addPanelListener(treeObjectListener);
        listenerBroker.addPanelListener(cListPanel.getReceivePanelListener());
    }


    // --- End Event listeners ---------------------------------------

    /**
     * Initialize the class.
     */
    private void initialize() throws IOException {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("MainWindow");
        setJMenuBar(getMainJMenuBar());
        setTitle(resapplication.getString("SSG"));
        ImageIcon imageIcon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/rooster.gif"));
        setIconImage(imageIcon.getImage());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getJFrameContentPane(), BorderLayout.CENTER);
        getContentPane().add(getStatusBarPane(), BorderLayout.SOUTH);
//    setContentPane(getJFrameContentPane());
        initializePreferences();
        initListeners();
        // new IconManager().loadCommonImages();
        Preferences prefs = Preferences.getPreferences();

        String lfName = prefs.getString(Preferences.LOOK_AND_FEEL);
        if (lfName == null) {
            setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } else {
            setLookAndFeel(lfName);
        }

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
            //lf.setCurrentTheme(new KunststoffDesktopTheme());

            Preferences prefs = Preferences.getPreferences();
        } catch (IOException e) {
            log.warn("cannot get preferences", e);
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
            log.debug("unable to fetch or set window position prefs: ", e);
        }

        if (!posWasSet)
            Utilities.centerOnScreen(this);
    }


    /**
     * the "About Box"
     */
    public void showAboutBox() {
        AboutBox.showDialog(this);
    }

    /**
     * The "Help Topics".
     * This procedure adds the JavaHelp to PMC application.
     */
    public void showHelpTopics() {
        HelpSet hs = null;
        URL url = null;
        HelpBroker javaHelpBroker = null;
        String helpsetName = "PMC Help";

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
            log.error(helpsetName + " file was not found. " + hex.toString());
        }
    }


    /** Hide or show the statusbar */
    public void viewStatusBar() {
        getStatusBarPane().setVisible(!(getStatusBarPane().isVisible()));
    }

    /** Hide or show the toolbar */
    public void viewToolBar() {
        getToolBarPane().setVisible(!(getToolBarPane().isVisible()));
    }

    /**
     * start the progress bar indicator.
     * Note that the progress bar updates can happen only
     * on the Swing dispatching thread.
     */
    public void startProgressIndicator() {
        //
    }

    /**
     * stop the progress bar indicator.
     * Note that the progress bar updates can happen only
     * on the Swing dispatching thread.
     */
    public void stopProgressIndicator() {
        progressBar.setValue(0);

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
                log.debug("inactivity timeout disabled (timeout = 0)");
            }
        } else if (inactivityTimeout > 0) {
            //  substract 1 secs (tollerance)
            inactivityTimer.setDelay(inactivityTimeout);
            inactivityTimer.setInitialDelay(inactivityTimeout);
            if (inactivityTimer.isRunning()) {
                inactivityTimer.stop();
            }
            inactivityTimer.start();
            log.debug("inactivity timeout enabled (timeout = " + inactivityTimeout + ")");
        } else {
            log.error("incorrect timeout value " + inactivityTimeout);
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
        //PanelFactory.clearCachedPanels();
        treeDirectoryView.setUI(CustomTreeUI.getTreeUI());
    }

    /**
     * invoke logon dialog
     *
     * @return true if the logon was succesfully validated, false
     *         otherwise
     */
    private boolean logon() {
        LogonDialog.logon(this, logonListenr);
        setJtreeRootNode();
        int timeout = 0;
        try {
            timeout = Preferences.getPreferences().getInactivityTimeout();
        } catch (IOException e) {
            log.warn("unable to get preferences", e);
        }
        final int fTimeout = timeout;
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        MainWindow.this.
                                setInactivitiyTimeout(fTimeout);
                    }
                });
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
                        log.debug("onAuthSuccess()", e);
                    }
                }

                /* invoked on authentication failure */
                public void onAuthFailure() {
                    ;
                }
            };
}
