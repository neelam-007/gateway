package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.IdentityProviderDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.ProviderNode;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.util.Enumeration;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.awt.*;

/**
 * The <code>IdentityProviderPropertiesAction</code> edits the
 * identity provider.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityProviderPropertiesAction extends NodeAction {

    public IdentityProviderPropertiesAction(ProviderNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Provider properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit provider properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
            public void run() {
                JFrame f = Registry.getDefault().getComponentRegistry().getMainWindow();
                IdentityProviderDialog d =
                  new IdentityProviderDialog(f, ((EntityHeaderNode)node).getEntityHeader());
                d.addEntityListener(entityListener);
                d.setResizable(false);
                d.show();
            }
        });
    }

    EntityListener entityListener = new EntityListenerAdapter() {
        public void entityUpdated(EntityEvent ev) {
            if (tree == null) {
                log.warning("Internal: tree has not been set.");
                return;
            }

            try {
                tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                Enumeration enumeration = node.children();

                while (enumeration.hasMoreElements()) {
                    AbstractTreeNode n = (AbstractTreeNode)enumeration.nextElement();
                    n.setHasLoadedChildren(false);
                    model.nodeStructureChanged(n);
                }
            } finally {
                tree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    };
}
