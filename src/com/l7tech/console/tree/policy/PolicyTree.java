package com.l7tech.console.tree.policy;

import com.l7tech.console.action.DeleteAssertionAction;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityTreeCellRenderer;
import com.l7tech.console.tree.AssertionsTree;
import com.l7tech.console.util.PopUpMouseListener;
import com.l7tech.console.panels.PolicyEditorPanel;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

/**
 * Class PolicyTree is the extended <code>JTree</code> with addtional
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyTree extends JTree {
    static final Logger log = Logger.getLogger(PolicyTree.class.getName());
    /** component name */
    public final static String NAME = "policy.tree";
    private PolicyEditorPanel policyEditorPanel;

    /**
     * Create the new policy tree with the policy model.
     *
     * @param newModel
     */
    public PolicyTree(PolicyTreeModel newModel) {
        super(newModel);
        initialize();
    }

    /**
     * Create empty policy tree
     */
    public PolicyTree() {
        this(null);
    }

    /** initialize */
    private void initialize() {
        addKeyListener(new TreeKeyListener());
        addMouseListener(new TreeMouseListener());
        setCellRenderer(new EntityTreeCellRenderer());
        setDragEnabled(true);
        setTransferHandler(new PolicyTransferHandler());
    }

    public void setPolicyEditor(PolicyEditorPanel pe) {
        policyEditorPanel = pe;
    }

    /**
     * KeyAdapter for the policy trees
     */
    class TreeKeyListener extends KeyAdapter {

        /** Invoked when a key has been pressed.*/
        public void keyPressed(KeyEvent e) {
            JTree tree = (JTree)e.getSource();
            TreePath path = tree.getSelectionPath();
            if (path == null) return;
            AbstractTreeNode node =
              (AbstractTreeNode)path.getLastPathComponent();
            if (node == null) return;
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_DELETE) {
                if (!node.canDelete()) return;
                if (node instanceof AssertionTreeNode)
                    new DeleteAssertionAction((AssertionTreeNode)node).actionPerformed(null);
            } else if (keyCode == KeyEvent.VK_ENTER) {
                // default properties
            }
        }
    }

    class TreeMouseListener extends PopUpMouseListener {
        /**
         * Handle the mouse click popup when the Tree item is right clicked. The context sensitive
         * menu is displayed if the right click was over an item.
         *
         * @param mouseEvent
         */
        protected void popUpMenuHandler(MouseEvent mouseEvent) {
            JTree tree = (JTree)mouseEvent.getSource();

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
                    AbstractTreeNode node = (AbstractTreeNode)tree.getLastSelectedPathComponent();
                    Action[] actions = node.getActions();
                    if (policyEditorPanel !=null) {
                        policyEditorPanel.updateActions(actions);
                    }
                    JPopupMenu menu = getPopupMenu(actions);
                    if (menu != null) {
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        }
    }

    /**
     * Make a popup menu from actions.
     * The menu is constructed from the set of actions returned
     *
     * @return the popup menu
     */
    private JPopupMenu getPopupMenu(Action[] actions) {
        if (actions == null || actions.length == 0)
            return null;
        JPopupMenu pm = new JPopupMenu();
        for (int i = 0; i < actions.length; i++) {
            pm.add(actions[i]);
        }
        return pm;
    }

    /**
     * Assertion tree custom transfer handler
     */
    class PolicyTransferHandler extends TransferHandler {
        public int getSourceActions(JComponent c) {
            return NONE;
        }

        public Transferable createTransferable(JComponent c) {
            return null;
        }

        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            //if (!nodeCanImport()) return false;

            for (int i = 0; i < transferFlavors.length; i++) {
                DataFlavor transferFlavor = transferFlavors[i];
                if (transferFlavor.equals(AssertionsTree.ASSERTION_DATAFLAVOR)) return true;
            }
            return false;
        }

        public boolean importData(JComponent c, Transferable t) {
            if (canImport(c, t.getTransferDataFlavors())) {
                try {
                    AbstractTreeNode node
                      = (AbstractTreeNode)t.getTransferData(AssertionsTree.ASSERTION_DATAFLAVOR);

                    TreePath path = getSelectionPath();
                    if (path != null) {
                        AssertionTreeNode target = (AssertionTreeNode)path.getLastPathComponent();
                        if (target.accept(node)) {
                            target.receive(node);
                            return true;
                        }
                    }

                    return false;
                } catch (UnsupportedFlavorException ufe) {
                    log.log(Level.WARNING, ufe.getMessage(), ufe);
                } catch (IOException ioe) {
                    log.log(Level.WARNING, ioe.getMessage(), ioe);
                }
            }
            return false;
        }

        private boolean canImportAbstractTreeNode(AbstractTreeNode an) {
            TreePath path = getSelectionPath();
            if (path != null) {
                AssertionTreeNode node = (AssertionTreeNode)path.getLastPathComponent();
                if (node.isLeaf()) return false;

                return node.accept(an);
            }
            return false;
        }

        private boolean nodeCanImport() {
            log.info("nodeCanImport");
            TreePath path = getSelectionPath();
            if (path != null) {
                AssertionTreeNode node = (AssertionTreeNode)path.getLastPathComponent();
                return !node.isLeaf();
           }
           return false;
        }
    }

}
