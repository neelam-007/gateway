package com.l7tech.console.action;

import com.l7tech.console.panels.PublishNonSoapServiceWizard;
import com.l7tech.console.MainWindow;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.util.TopComponents;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;

/**
 * SSM action to publish a non-soap xml service.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 14, 2004<br/>
 * $Id$<br/>
 */
public class PublishNonSoapServiceAction extends SecureAction {
    public String getName() {
        return "Publish XML Application";
    }

    public String getDescription() {
        return "Publish a non-soap XML application";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/services16.png";
    }

    protected void performAction() {
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        PublishNonSoapServiceWizard wiz = PublishNonSoapServiceWizard.getInstance(mw);
        wiz.pack();
        wiz.setSize(800, 480);
        Utilities.centerOnScreen(wiz);
        wiz.addEntityListener(listener);
        wiz.setModal(true);
        wiz.setVisible(true);
    }

    private EntityListener listener = new EntityListenerAdapter() {
        /**
         * Fired when an new entity is added.
         *
         * @param ev event describing the action
         */
        public void entityAdded(final EntityEvent ev) {
            EntityHeader eh = (EntityHeader)ev.getEntity();
            JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
            if (tree != null) {
                AbstractTreeNode root = (AbstractTreeNode)tree.getModel().getRoot();
                TreeNode[] nodes = root.getPath();
                TreePath nPath = new TreePath(nodes);
                if (tree.hasBeenExpanded(nPath)) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(eh);
                    model.insertNodeInto(sn, root, root.getInsertPosition(sn));

                    tree.setSelectionPath(new TreePath(sn.getPath()));
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            new EditServicePolicyAction((ServiceNode)sn).invoke();
                        }
                    });
                }
            } else {
                log.log(Level.WARNING, "Service tree unreachable.");
            }
        }
    };
}
