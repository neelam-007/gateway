package com.l7tech.console.tree.identity;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.*;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.Refreshable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

/**
 * Class <code>IdentityProvidersTree</code> is the specialized <code>JTree</code>
 * that contains identity providers, SAML providers.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class IdentityProvidersTree extends JTree implements DragGestureListener, Refreshable {
    static final Logger log = Logger.getLogger(IdentityProvidersTree.class.getName());
    /**
     * assertion data flavor for DnD
     */

    public static final DataFlavor ASSERTION_DATAFLAVOR;

    static {
        DataFlavor df;
        try {
            df = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + AbstractTreeNode.class.getName());
        } catch (ClassNotFoundException e) {
            df = null;
        }
        ASSERTION_DATAFLAVOR = df;
    }

    /**
     * component name
     */
    public final static String NAME = "identities.tree";

    /**
     * Create the new policy tree with the policy model.
     *
     * @param newModel
     */
    public IdentityProvidersTree(DefaultTreeModel newModel) {
        super(newModel);
        initialize();
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    /**
     * Create empty policy tree
     */
    public IdentityProvidersTree() {
        this(null);
    }

    /**
     * initialize
     */
    private void initialize() {
        addKeyListener(new TreeKeyListener());
        addMouseListener(new TreeMouseListener());
        setCellRenderer(new EntityTreeCellRenderer());

        // Make this JTree a drag source
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, this);

        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        addTreeWillExpandListener(new AssertionsTreeWillExpandListener());
        addTreeExpansionListener(new AssertionsTreeExpansionListener());

        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void dragGestureRecognized(DragGestureEvent dge) {
        Point ptDragOrigin = dge.getDragOrigin();
        TreePath path = getPathForLocation(ptDragOrigin.x, ptDragOrigin.y);
        if (path == null)
            return;
        Transferable ta = createTransferable(path);
        if (ta == null) {
            return;
        }
        setSelectionPath(path);	// Select this path in the tree
        dge.startDrag(null, ta);
    }

    private Transferable createTransferable(TreePath path) {
        if (path != null) {
            AbstractTreeNode node = (AbstractTreeNode)path.getLastPathComponent();
            if (!node.isLeaf()) return null;
            if (node instanceof ProviderNode) return null;
            return new AssertionTransferable(node);
        }
        return null;
    }

    public void refresh(AbstractTreeNode n) {
        if (n == null) {
            TreePath path = getSelectionPath();
            if (path != null) {
                n = (AbstractTreeNode)path.getLastPathComponent();
            }
        }
        if (n != null) {
            final Action[] actions = n.getActions(RefreshAction.class);
            if (actions.length == 0) {
                log.warning("No refresh action found");
            } else {
                ((NodeAction)actions[0]).setTree(this);
                ActionManager.getInstance().invokeAction(actions[0]);
            }
        }
    }

    public void refresh() {
        refresh(null);
    }

    /**
     * This tree can always be refreshed
     * <p/>
     * May consider introducing the logic arround disabling this while the tree is
     * refreshing if needed
     *
     * @return always true
     */
    public boolean canRefresh() {
        return true;
    }

    /**
     * KeyAdapter for the policy trees
     */
    class TreeKeyListener extends KeyAdapter {

        /**
         * Invoked when a key has been pressed.
         */
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
                if (node instanceof EntityHeaderNode) {
                    final EntityHeaderNode en = (EntityHeaderNode)node;
                    new DeleteEntityAction(en, en.getProviderConfig()).actionPerformed(null);
                } else if (node instanceof PolicyTemplateNode)
                    new DeletePolicyTemplateAction((PolicyTemplateNode)node).actionPerformed(null);
            } else if (keyCode == KeyEvent.VK_ENTER) {
                Action a = node.getPreferredAction();
                if (a != null) {
                    ActionManager.getInstance().invokeAction(a);
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
            if (node == null || !node.isLeaf()) return;

            Action a = node.getPreferredAction();
            if (a != null) {
                ActionManager.getInstance().invokeAction(a);
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

                    JPopupMenu menu = node.getPopupMenu(IdentityProvidersTree.this);
                    if (menu != null) {
                        Utilities.removeToolTipsFromMenuItems(menu);
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        }
    }

    class AssertionTransferable implements Transferable {
        private AbstractTreeNode node;

        public AssertionTransferable(AbstractTreeNode an) {
            this.node = an;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{ASSERTION_DATAFLAVOR};
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

    private class AssertionsTreeWillExpandListener implements TreeWillExpandListener {
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        }
    }

    private class AssertionsTreeExpansionListener implements TreeExpansionListener {
        public void treeExpanded(TreeExpansionEvent event) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        public void treeCollapsed(TreeExpansionEvent event) {
        }
    }

}
