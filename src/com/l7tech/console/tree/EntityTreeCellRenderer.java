package com.l7tech.console.tree;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;

import com.l7tech.console.util.IconManager;

/**
 * Console custom <CODE>TreeCellRenderer</CODE>.
 * Labels, and the image icon for tree nodes.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class EntityTreeCellRenderer
        extends DefaultTreeCellRenderer {

    /**
     * default constructor
     */
    public EntityTreeCellRenderer() {
    }

    /**
     * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent
     */
    public Component
            getTreeCellRendererComponent(JTree tree, Object value,
                                         boolean sel, boolean expanded,
                                         boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value,
                sel, expanded,
                leaf, row, hasFocus);

        Object object = ((DefaultMutableTreeNode)value).getUserObject();
        this.setBackgroundNonSelectionColor(tree.getBackground());
        if (object instanceof BasicTreeNode) {
            BasicTreeNode node = (BasicTreeNode)object;
            setText(node.getLabel());
            Icon icon = IconManager.getInstance().getIcon(node);

            if (icon == null) {
                if (isFolder(node)) {
                    setIcon(UIManager.getIcon("Tree.closedIcon"));
                } else {
                    icon = UIManager.getIcon("Tree.leafIcon");
                }
            }
            setIcon(icon);

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
                clazz.equals(PoliciesFolderNode.class) ||
                clazz.equals(ServicesFolderNode.class) ||
                clazz.equals(AdminFolderNode.class) ||
                clazz.equals(GroupFolderNode.class) ||
                clazz.equals(UserFolderNode.class);
    }

}
