package com.l7tech.console.tree.policy;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityTreeCellRenderer;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Class PolicyTree is the extended <code>JTree</code> with addtional
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyTree extends JTree {
    /** component name */
    public final static String NAME = "policy.tree";
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
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_DELETE) {
                TreeNode node =
                  (TreeNode)path.getLastPathComponent();
                if (node == null) return;
            }
        }
    }

    class TreeMouseListener extends MouseAdapter {
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
}
