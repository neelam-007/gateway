package com.l7tech.console.panels;

import com.l7tech.console.table.ContextListTableModel;
import com.l7tech.console.table.TableRowAction;
import com.l7tech.console.table.TableRowMenu;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.Preferences;
import com.l7tech.objectmodel.EntityHeader;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

/**
 * <CODE>ContainerListPanel</CODE> browses the contest of
 * the container/context. It does support basic object
 * navigaiton and object management.
 */
public class ContainerListPanel extends EntityEditorPanel {
    private static final Category log = Category.getInstance(ContainerListPanel.class.getName());

    private final JPanel listPane = new JPanel();
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private final JTable jTable = new JTable();
    private JScrollPane scrollPane = new JScrollPane(jTable);

    private ContextListTableModel tableModel = null;

    private JTree tree = null;
    private DefaultMutableTreeNode parentNode = null;
    // this panel GUI parent node (userObject in parentNode)
    private BasicTreeNode parentBasicTreeNode = null;

    // this panel receives events about
    private ObjectListener receiveListener = new ObjectListener();
    // track toolbar
    private ToolBarForTable toolBar;

    /**
     * default constructor
     */
    public ContainerListPanel() {
        layoutComponents();
        initializePropertiesListener();
    }

    /**
     * unimplemented for this panel
     */
    public void edit(Object dirObject) {
    }

    /**
     * set the parent node and <CODE>JTree</CODE> instance
     * for this context list.
     *
     * @param tree   the tree associated witht the context list
     * @param node   <CODE>DefaultMutableTreeNode</CODE> instance that
     *               is the parent node of this list
     */
    public void setParentNode(JTree tree, DefaultMutableTreeNode node) {
        parentNode = node;
        this.tree = tree;
        parentBasicTreeNode = (BasicTreeNode) node.getUserObject();

        if (tabbedPane.getTabCount() != 1) {
            throw new
                    IllegalStateException("expected tab count is 1, returned is " +
                    tabbedPane.getTabCount());
        }
        tabbedPane.setTitleAt(0, tabTitle(parentBasicTreeNode));
        if (toolBar != null) {
            listPane.remove(toolBar);
            toolBar = null;
        }
        toolBar = getToolBar(parentBasicTreeNode);
        if (toolBar != null) {
            listPane.add(toolBar, BorderLayout.NORTH);
        }
        // selection listener and properties
        jTable.getSelectionModel().addListSelectionListener(toolBar);
        jTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setTableModel();
    }

    /**
     * get the panel listener that receives and processes
     * events on this panel. It is used to keep this panel
     * in synch with the tree.
     *
     * @return the <CODE>PanelListenere</CODE> that receives
     *         object events.
     */
    public PanelListener getReceivePanelListener() {
        return receiveListener;
    }

    /**
     * layout components on this panel
     */
    private void layoutComponents() {
        addHierarchyListener(hierarchyListener);
        setLayout(new BorderLayout());

        listPane.setLayout(new BorderLayout());
        listPane.add(scrollPane, BorderLayout.CENTER);
        tabbedPane.addTab(null, listPane);
        tabbedPane.setFont(new Font("Dialog", Font.BOLD, 12));

        scrollPane.getViewport().setBackground(jTable.getBackground());

        JTableHeader header = jTable.getTableHeader();
        jTable.setTableHeader(header);
        jTable.setDefaultRenderer(Object.class, crenderer);
        jTable.setShowGrid(false);
        jTable.sizeColumnsToFit(0);

        jTable.
                addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            int row = jTable.getSelectedRow();
                            if (row == -1) return;
                            Object o = ((ContextListTableModel) jTable.getModel()).getValueAt(row);
                            if (o == null) return;
                            handleExploreRequest((BasicTreeNode) o);
                        }
                    }

                    public void mousePressed(MouseEvent e) {
                        jTablePopUpEventHandler(e);
                    }

                    public void mouseReleased(MouseEvent e) {
                        jTablePopUpEventHandler(e);
                    }
                });

        jTable.
                addKeyListener(new KeyAdapter() {
                    /** Invoked when a key has been pressed.*/
                    public void keyPressed(KeyEvent e) {
                        int row = jTable.getSelectedRow();
                        if (row == -1) return;
                        Object o = jTable.getValueAt(row, 0);
                        BasicTreeNode dobj = (BasicTreeNode) o;
                        int keyCode = e.getKeyCode();
                        if (keyCode == KeyEvent.VK_DELETE) {
                            handleDeleteRequest(dobj, row, true);
                        } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                            browseUpRequest(parentNode);
                        } else if (keyCode == KeyEvent.VK_ENTER) {
                            handleExploreRequest((BasicTreeNode) o);
                        }
                    }
                });

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * initialize properties listener
     */
    private void initializePropertiesListener() {
        // look and feel listener
        PropertyChangeListener l =
                new PropertyChangeListener() {
                    /** This method gets called when a property is changed.*/
                    public void propertyChange(final PropertyChangeEvent evt) {
                        if ("lookAndFeel".equals(evt.getPropertyName())) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    SwingUtilities.updateComponentTreeUI(ContainerListPanel.this);
                                    scrollPane.getViewport().setBackground(jTable.getBackground());
                                }
                            });
                        }
                    }
                };

        UIManager.addPropertyChangeListener(l);
        try {
            Preferences pref = Preferences.getPreferences();
            l = new PropertyChangeListener() {
                /** This method gets called when a property is changed.*/
                public void propertyChange(PropertyChangeEvent evt) {
                    log.debug("toolbar view changed to " + evt.getNewValue());
                }
            };

            // toolbars (icon, text etc)
            pref.
                    addPropertyChangeListener(Preferences.TOOLBARS_VIEW, l);

        } catch (IOException e) {
            log.warn("error instantiaitng preferences", e);
        }
    }

    /**
     * set the <CODE>TableModel</CODE> that is used by this
     * object browser instance.
     */
    private void setTableModel() {
        try {
            if (tableModel != null) {
                tableModel.stop();
            }

            tableModel =
                    new ContextListTableModel(parentBasicTreeNode.children(),
                            2,
                            new String[]{parentBasicTreeNode.getLabel(), getColHeader(parentBasicTreeNode)},
                            new ContextListTableModel.ObjectRowAdapter() {
                                public Object getValue(Object o, int col) {
                                    String text = "";
                                    if (o instanceof EntityHeader) {
                                        if (col == 1) {
                                            text = ((EntityHeader) o).getName();
                                        } else {
                                            return o;
                                        }
                                    } else if (o instanceof BasicTreeNode) {
                                        return o;
                                    } else {
                                        throw new IllegalArgumentException("Invalid argument type: "
                                                + "\nExpected: Entry"
                                                + "\nReceived: " + o == null ? "<null>" : o.getClass().getName());
                                    }
                                    if (text == null) {
                                        text = "";
                                    }
                                    return text;
                                }
                            });
            jTable.setModel(tableModel);
        } catch (Exception e) {
            log.error("loadChildren()", e);
        }
    }

    // could be usefull later..
    private String getColHeader(BasicTreeNode ptn) {
        if (ptn instanceof ProvidersFolderNode) return "Comments";
        return "Description";
    }

    /**
     * handle the explore request for the given object. If
     * object is a container it invokes new browser. If it is
     * a <CODE>Leaf</CODE> the properties panel is invoked.
     *
     * @param dobj   the <CODE>BasicTreeNode</CODE> to explore.
     */
    private void handleExploreRequest(BasicTreeNode dobj) {
        if (dobj.isLeaf()) {
            showEntryDialog(dobj);
        } else {

            final TreePath parentPath
                    = new TreePath(parentNode.getPath());
            if (!tree.isExpanded(parentPath)) {
                tree.expandPath(parentPath);
            }

            TreeNode n = TreeNodeAction.nodeByName(dobj.getName(), parentNode);
            if (n != null) {
                TreeNode[] nodes = ((DefaultMutableTreeNode) n).getPath();
                final TreePath path = new TreePath(nodes);
                SwingUtilities.
                        invokeLater(
                                new Runnable() {
                                    public void run() {
                                        if (!tree.isExpanded(path)) {
                                            tree.expandPath(path);
                                        }
                                        tree.setSelectionPath(path);
                                    }
                                });
            }
        }
    }

    /**
     * navigate up one level in the tree hierarchy
     *
     * @param node   node that describes the current tree position
     *               object browser is browsing
     */
    private void browseUpRequest(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode pNode =
                (DefaultMutableTreeNode) node.getParent();

        TreeNode[] nodes = pNode.getPath();
        final TreePath path = new TreePath(nodes);
        if (!tree.isExpanded(path)) {
            tree.expandPath(path);
        }
        tree.setSelectionPath(path);
    }

    /**
     * delete the given <code>BasicTreeNode</code>
     *
     * @param bn   node the node to delete
     * @param  row  the BasicTreeNode instnce row
     * @param askQuestion - the flag indicating if the "Are you sure..."
     *                      question should be asked
     */
    private void handleDeleteRequest(BasicTreeNode bn, int row, boolean askQuestion) {
        if (!TableRowAction.canDelete(bn)) {
            return;
        }
        if (TableRowAction.delete(bn, askQuestion)) {
            tableModel.removeRow(row);
            if (panelListener != null) {
                panelListener.onDelete(bn);
            }
        }
    }

    /**
     * open the new entity dialog the given <code>BasicTreeNode</code>
     */
    private void handleNewRequest() {
        JDialog dialog = getNewEntryDialog();
        if (dialog == null) {
            return;
        }
        dialog.show();
    }

    /**
     * Retruns new Entry dialog for a given node
     *
     * @return JDialog instance for a given node
     */
    private JDialog getNewEntryDialog() {
        JDialog dlg = null;
        JFrame f = (JFrame) SwingUtilities.windowForComponent(ContainerListPanel.this);
        if (parentBasicTreeNode instanceof AdminFolderNode) {
            AdminFolderNode adminFolder =
                    (AdminFolderNode) parentBasicTreeNode;
            NewAdminDialog dialog = new NewAdminDialog(f, adminFolder);
            dialog.setPanelListener(panelListener);
            dialog.setResizable(false);
            dlg = dialog;
        } else if (parentBasicTreeNode instanceof UserFolderNode) {
            UserFolderNode userFolder =
                    (UserFolderNode) parentBasicTreeNode;
            NewUserDialog dialog = new NewUserDialog(f);
            dialog.setPanelListener(panelListener);
            dialog.setResizable(false);
            dlg = dialog;
        } else if (parentBasicTreeNode instanceof ProvidersFolderNode) {
            NewProviderDialog dialog = new NewProviderDialog(f);
            dialog.setPanelListener(panelListener);
            dialog.setResizable(false);
            dlg = dialog;
        }
        return dlg;
    }

    /**
     * instantiate the dialog for given BasicTreeNode
     *
     * @param bn   the <CODE>BasicTreeNode</CODE> instance
     */
    private void showEntryDialog(BasicTreeNode bn) {
        if (!(bn instanceof EntityTreeNode)) return;

        JPanel panel = PanelFactory.getPanel((EntityTreeNode) bn, panelListener);

        if (panel == null) return;
        JFrame f = (JFrame) SwingUtilities.windowForComponent(ContainerListPanel.this);
        EditorDialog dialog = new EditorDialog(f, panel);

        dialog.pack();
        Utilities.centerOnScreen(dialog);
        dialog.show();
    }


    private final TableCellRenderer
            crenderer = new DefaultTableCellRenderer() {
                /* This is the only method defined by ListCellRenderer.  We just
                 * reconfigure the Jlabel each time we're called.
                 */
                public Component
                        getTableCellRendererComponent(JTable table,
                                                      Object value,
                                                      boolean iss,
                                                      boolean hasFocus,
                                                      int row, int column) {
                    if (iss) {
                        this.setBackground(table.getSelectionBackground());
                        this.setForeground(table.getSelectionForeground());
                    } else {
                        this.setBackground(table.getBackground());
                        this.setForeground(table.getForeground());
                    }
                    this.setFont(new Font("Dialog", Font.PLAIN, 12));

                    // based on value type and column, determine cell contents
                    setIcon(null);
                    if (value instanceof BasicTreeNode) {
                        BasicTreeNode bn = (BasicTreeNode) value;
                        if (column == 0) {
                            ImageIcon icon = IconManager.getIcon(bn);
                            if (icon == null) {
                                if (isFolder(bn)) {
                                    setIcon(UIManager.getIcon("Tree.closedIcon"));
                                } else {
                                    setIcon(UIManager.getIcon("Tree.leafIcon"));
                                }

                            }
                            setIcon(icon);
                            setText(bn.getName());
                        }
                    } else if (value instanceof String) {
                        setIcon(null);
                        setText((String) value);
                    } else {
                        if (column == 0) {
                            setText("Unknown type " + value.getClass());
                        }
                    }

                    return this;
                }

                /**
                 * is the object a folder?
                 *
                 * @param object the object to check
                 * @return true if object is any of the folders, false otherwise
                 */
                private boolean isFolder(Object object) {
                    Class clazz = object.getClass();
                    return
                            clazz.equals(ProvidersFolderNode.class) ||
                            clazz.equals(AdminFolderNode.class) ||
                            clazz.equals(UserFolderNode.class);
                }

            };

    /**
     * Determine if the parent tree node is tree root (used
     * mostly with logic related to navigation components).
     *
     * @return if the parent tree node property is the root of the
     *         <CODE>JTree</CODE> component, false otherwise.
     */
    private boolean isParentRoot() {
        return tree.getModel().getRoot() == parentNode;
    }


    /**
     * Handle the mouse click popup when the list item is right clicked. The context sensitive
     * menu is displayed if the right click was over an item.
     *
     * @param mouseEvent
     */
    private void jTablePopUpEventHandler(MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            int row = jTable.getSelectedRow();
            if (row == -1) return;
            Object o = ((ContextListTableModel) jTable.getModel()).getValueAt(row);
            if (o == null) return;
            JPopupMenu menu = getTableItemJPopupMenu(o, row);
            if (menu != null) {
                menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        }
        return;
    }

    /**
     * create and returns the tab title for the given
     * <CODE>BasicTreeNode</CODE> instance.
     *
     * @param bn     the <CODE>BasicTreeNode</CODE> instance
     * @return the title for the BasicTreeNode instance
     */
    private String tabTitle(BasicTreeNode bn) {
        if (bn instanceof BasicTreeNode) {
            int index = bn.getName().indexOf('.');
            String suffix = "";
            if (index != -1) {
                suffix = " / " + bn.getName().substring(index + 1);
            }
            return bn.getLabel() + suffix;
        } else if (bn instanceof EntityHeaderNode) {
            return bn.getName();
        } else {
            throw new IllegalArgumentException("don't know how to handle " + bn.getClass());
        }
    }

    /**
     * Return the TreeNodeJPopupMenu for a given object.
     *
     * @param object the object that the pop up menu is build for
     * @param row    the row in the table model taht corresponds to the object
     * @return JPopupMenu
     */
    private JPopupMenu getTableItemJPopupMenu(final Object object, final int row) {
        if (object == null
                || !(object instanceof BasicTreeNode)) {
            return null;
        }

        ActionListener listener = new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {

                // if it is not a tree node then just bail..
                if (!(object instanceof BasicTreeNode)) {
                    JOptionPane.showMessageDialog(null,
                            "Not yet implemented.",
                            "Information",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                BasicTreeNode bn = (BasicTreeNode) object;

                if (object instanceof EntityHeaderNode) {

                    // a node or expandable folder with properties
                    if (TableRowMenu.BROWSE.equals(e.getActionCommand())) {
                        handleExploreRequest((BasicTreeNode) object);
                    } else if (TableRowMenu.PROPERTIES.equals(e.getActionCommand())) {
                        showEntryDialog((BasicTreeNode) object);
                    } else if (TableRowMenu.DELETE.equals(e.getActionCommand())) {
                        handleDeleteRequest(bn, row, true);
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Not yet implemented.",
                                "Information",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    // might be a RealmFolder, or a CompanyFolder, or any Folder
                    // might be a report or a log too
                    if (TableRowMenu.BROWSE.equals(e.getActionCommand())) {
                        handleExploreRequest((BasicTreeNode) object);
                    } else if (TableRowMenu.PROPERTIES.equals(e.getActionCommand())) {
                        showEntryDialog((BasicTreeNode) object);
                    } else {
                        log.debug("action not implemented " + e.getActionCommand());
                    }
                }
            }
        };

        return TableRowMenu.forNode(new EntityTreeNode((BasicTreeNode) object), listener);
    }

    private class
            ObjectListener extends PanelListenerAdapter {
        /**
         * invoked after insert
         *
         * @param object an arbitrary object set by the Panel
         */
        public void onInsert(Object object) {
            BasicTreeNode row =
                    TreeNodeFactory.getTreeNode((EntityHeader) object);
            tableModel.addRow(row);

        }

        /**
         * invoked after update
         *
         * @param object an arbitrary object set by the Panel
         */
        public void onUpdate(Object object) {
            ;
        }

        /**
         * invoked on object delete
         *
         * @param object an arbitrary object set by the Panel
         */
        public void onDelete(Object object) {
            // Find the row that corresponds to the deleted object
            int row = tableModel.getRow(object);

            // If found
            if (-1 != row) {
                // Delete the object's row
                tableModel.removeRow(row);
            }
        }
    }

    /**
     * Return the ToolBarForTable instance for a given node or null.
     * @return ToolBarForTable
     */
    private ToolBarForTable getToolBar(BasicTreeNode bn) {
        ToolBarForTable tb = new ToolBarForTable();
        tb.setFloatable(false);
        return tb;
    }

    /**
     * the empty toolbar for table class. The users of the
     * populate the class with components (typically buttons).
     */
    final class ToolBarForTable
            extends JToolBar implements ListSelectionListener {
        JButton buttonUp;
        JButton buttonOpen;
        JButton buttonNew;
        JButton buttonEdit;
        JButton buttonDelete;

        public ToolBarForTable() {
            super();
            this.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
            initComponents();
        }

        private void initComponents() {
            buttonUp = new JButton();
            buttonUp.setIcon(IconManager.getUpOneLevelIcon());
            buttonUp.setFont(new Font("Dialog", 1, 10));
            buttonUp.setText("Up");
            buttonUp.setMargin(new Insets(0, 0, 0, 0));

            this.add(buttonUp);
            buttonUp.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {
                            browseUpRequest(parentNode);
                        }
                    });
            buttonUp.setEnabled(!isParentRoot());

            buttonOpen = new JButton();
            buttonOpen.setIcon(IconManager.getOpenFolderIcon());
            buttonOpen.setFont(new Font("Dialog", 1, 10));
            buttonOpen.setText("Open");
            buttonOpen.setMargin(new Insets(0, 0, 0, 0));

            this.add(buttonOpen);
            buttonOpen.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {
                            int row = jTable.getSelectedRow();
                            if (row == -1) return;
                            Object o = jTable.getModel().getValueAt(row, 0);
                            if (o == null) return;
                            handleExploreRequest((BasicTreeNode) o);
                        }
                    });

            buttonOpen.setEnabled(false);

            buttonNew = new JButton();
            buttonNew.setIcon(IconManager.getDefaultNewIcon());
            buttonNew.setFont(new Font("Dialog", 1, 10));
            buttonNew.setText("New");
            buttonNew.setMargin(new Insets(0, 0, 0, 0));
            this.add(buttonNew);
            buttonNew.setEnabled(TableRowAction.acceptNewChildren(parentBasicTreeNode));

            buttonNew.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {
                            handleNewRequest();
                        }
                    });


            buttonEdit = new JButton();
            buttonEdit.setIcon(IconManager.getDefaultEditIcon());
            buttonEdit.setFont(new Font("Dialog", 1, 10));
            buttonEdit.setText("Edit");
            buttonEdit.setMargin(new Insets(0, 0, 0, 0));
            this.add(buttonEdit);
            buttonEdit.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {
                            int row = jTable.getSelectedRow();
                            if (row == -1) return;
                            Object o = jTable.getModel().getValueAt(row, 0);
                            if (o == null) return;
                            showEntryDialog((BasicTreeNode) o);
                        }
                    });

            buttonEdit.setEnabled(false);

            buttonDelete = new JButton();
            buttonDelete.setIcon(IconManager.getDefaultDeleteIcon());
            buttonDelete.setFont(new Font("Dialog", 1, 10));
            buttonDelete.setText("Delete");
            buttonDelete.setMargin(new Insets(0, 0, 0, 0));
            this.add(buttonDelete);
            buttonDelete.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {
                            int row = jTable.getSelectedRow();
                            if (row == -1) return;
                            // return the basictreenode
                            Object o = jTable.getModel().getValueAt(row, 0);
                            if (o == null) return;
                            BasicTreeNode n = (BasicTreeNode) o;
                            handleDeleteRequest(n, row, true);
                        }
                    });
            buttonDelete.setEnabled(false);
            Utilities.
                    equalizeComponentSizes(
                            new JComponent[]{
                                buttonDelete,
                                buttonEdit,
                                buttonNew,
                                buttonOpen,
                                buttonUp
                            });
        }


        /**
         * Called whenever the value of the selection changes.
         * @param e the event that characterizes the change.
         */
        public void valueChanged(ListSelectionEvent e) {
            buttonUp.setEnabled(!isParentRoot());

            int row = jTable.getSelectedRow();
            if (row == -1) return;
            // return the basictreenode
            Object o = jTable.getModel().getValueAt(row, 0);
            if (o == null) return;
            BasicTreeNode n = (BasicTreeNode) o;

            buttonDelete.setEnabled(TableRowAction.canDelete(n));
            buttonEdit.setEnabled(TableRowAction.hasProperties(n));
            buttonOpen.setEnabled(TableRowAction.isBrowseable(n));

        }
    }

    // hierarchy listener
    private final
    HierarchyListener hierarchyListener =
            new HierarchyListener() {
                /** Called when the hierarchy has been changed.*/
                public void hierarchyChanged(HierarchyEvent e) {
                    long flags = e.getChangeFlags();
                    if ((flags & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                        if (ContainerListPanel.this.isShowing()) {
                            try {
                                tableModel.start();
                                SwingUtilities.
                                        invokeLater(new Runnable() {
                                            public void run() {
                                                if (tableModel.getRowCount() != 0) {
                                                    jTable.setRowSelectionInterval(0, 0);
                                                }
                                            }
                                        });
                            } catch (InterruptedException ex) {
                            }
                        } else {
                            if (tableModel != null) {
                                tableModel.stop();
                            }

                        }
                    }
                }
            };
}
