package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.logging.Level;

/**
 * Parent class for all actions which publish a service.
 */
public abstract class AbstractPublishServiceAction extends SecureAction {
    private final Option<Folder> folder;
    private final Option<AbstractTreeNode> parentNode;

    public AbstractPublishServiceAction(@NotNull final AttemptedOperation attemptedOperation, @NotNull final Option<Folder> folder,
                                        @NotNull final Option<AbstractTreeNode> parentNode) {
        super(attemptedOperation);
        this.folder = folder;
        this.parentNode = parentNode;
    }

    public abstract AbstractPublishServiceWizard createWizard();

    @Override
    protected void performAction() {
        final AbstractPublishServiceWizard dialog = createWizard();
        dialog.addEntityListener(listener);
        if (folder.isSome()) {
            dialog.setFolder(folder.some());
        }
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog);
    }

    private EntityListener listener = new EntityListenerAdapter() {
        /**
         * Fired when an new entity is added.
         *
         * @param ev event describing the action
         */
        @Override
        public void entityAdded(final EntityEvent ev) {
            EntityHeader eh = (EntityHeader) ev.getEntity();
            final JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            if (tree != null) {
                AbstractTreeNode root = TopComponents.getInstance().getServicesFolderNode();
                AbstractTreeNode parentNode = AbstractPublishServiceAction.this.parentNode.orSome(root);

                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                //Remove any filter before insert
                TopComponents.getInstance().clearFilter();

                final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(eh, RootNode.getComparator());
                model.insertNodeInto(sn, parentNode, parentNode.getInsertPosition(sn, RootNode.getComparator()));
                RootNode rootNode = (RootNode) model.getRoot();
                rootNode.addEntity(eh.getGoid(), sn);
                tree.setSelectionPath(new TreePath(sn.getPath()));
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new EditPolicyAction((ServiceNode) sn).invoke();

                        //reset filter to ALL
                        ((ServicesAndPoliciesTree) tree).filterTreeToDefault();
                    }
                });

            } else {
                log.log(Level.WARNING, "Service tree unreachable.");
            }
        }
    };
}
