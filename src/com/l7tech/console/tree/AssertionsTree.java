package com.l7tech.console.tree;

import com.l7tech.console.action.Actions;
import com.l7tech.console.action.DeleteEntityAction;
import com.l7tech.console.action.DeletePolicyTemplateAction;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

/**
 * Class ServiceTree is the speciaqliced <code>JTree</code> that
 * handles service
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AssertionsTree extends JTree {
    static final Logger log = Logger.getLogger(AssertionsTree.class.getName());
    /** assertion data flavor for DnD*/

    public static final DataFlavor ASSERTION_DATAFLAVOR;
    static {
        DataFlavor df;
        try {
             df = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class="+AbstractTreeNode.class.getName());
        } catch (ClassNotFoundException e) {
            df = null;
        }
        ASSERTION_DATAFLAVOR = df;
    }

    /** component name */
    public final static String NAME = "assertion.palette";

    /**
     * Create the new policy tree with the policy model.
     *
     * @param newModel
     */
    public AssertionsTree(DefaultTreeModel newModel) {
        super(newModel);
        initialize();
    }

    /**
     * Create empty policy tree
     */
    public AssertionsTree() {
        this(null);
    }

    /** initialize */
    private void initialize() {
        addKeyListener(new TreeKeyListener());
        addMouseListener(new TreeMouseListener());
        setCellRenderer(new EntityTreeCellRenderer());
        setDragEnabled(true);
        setTransferHandler(new AssertionTransferHandler());
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
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
                if (node instanceof EntityHeaderNode)
                    new DeleteEntityAction((EntityHeaderNode)node).performAction();
                else if (node instanceof PolicyTemplateNode)
                    new DeletePolicyTemplateAction((PolicyTemplateNode)node).performAction();
            } else if (keyCode == KeyEvent.VK_ENTER) {
                Action a = node.getPreferredAction();
                if (a != null) {
                    Actions.invokeAction(a);
                }
            }
        }
    }

    class TreeMouseListener extends MouseAdapter {
        /**
         * Invoked when the mouse has been clicked on a component.
         */
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2) return;
            JTree tree = (JTree)e.getSource();
            TreePath path = tree.getSelectionPath();
            if (path == null) return;
            AbstractTreeNode node =
              (AbstractTreeNode)path.getLastPathComponent();
            if (node == null) return;

            Action a = node.getPreferredAction();
            if (a != null) {
                Actions.invokeAction(a);
            }
        }

        public void mousePressed(MouseEvent e) {
            popUpMenuHandler(e);
        }

        public void mouseReleased(MouseEvent e) {
            popUpMenuHandler(e);
        }

        /**
         * Handle the mouse click popup when the Tree item is right clicked. The context sensitive
         * menu is displayed if the right click was over an item.
         *
         * @param mouseEvent
         */
        private void popUpMenuHandler(MouseEvent mouseEvent) {
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

                    JPopupMenu menu = node.getPopupMenu();
                    if (menu != null) {
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        }
    }

    /**
     * Assertion tree custom transfer handler
     */
    class AssertionTransferHandler extends TransferHandler {
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        public Transferable createTransferable(JComponent c) {
            TreePath path = getSelectionPath();
            if (path != null) {
                AbstractTreeNode node = (AbstractTreeNode)path.getLastPathComponent();
                if (!node.isLeaf()) return null;
                if (node instanceof ProviderNode) return null;
                return new AssertionTransferable(node);
            }

            return null;
        }

        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            return false;
        }
    }

    class AssertionTransferable implements Transferable {
        private AbstractTreeNode node;

        public AssertionTransferable(AbstractTreeNode an) {
            this.node= an;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{ ASSERTION_DATAFLAVOR};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(ASSERTION_DATAFLAVOR);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (isDataFlavorSupported(flavor)) {
                return node;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
