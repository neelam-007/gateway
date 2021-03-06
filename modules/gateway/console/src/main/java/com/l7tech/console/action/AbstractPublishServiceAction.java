package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Parent class for all actions which publish a service.
 */
public abstract class AbstractPublishServiceAction extends SecureAction {
    @NotNull private Option<Folder> folder = Option.none();
    @NotNull private Option<AbstractTreeNode> parentNode = Option.none();

    public AbstractPublishServiceAction(@NotNull final AttemptedOperation attemptedOperation,
                                        @NotNull final Option<Folder> folder,
                                        @NotNull final Option<AbstractTreeNode> parentNode) {
        super(attemptedOperation);
        this.folder = folder;
        this.parentNode = parentNode;
    }

    public AbstractPublishServiceAction(@NotNull final AttemptedOperation attemptedOperation,
                                        @NotNull final Option<Folder> folder,
                                        @NotNull final Option<AbstractTreeNode> parentNode,
                                        @Nullable String requiredFeaturesetLicense) {
        super(attemptedOperation, requiredFeaturesetLicense);
        this.folder = folder;
        this.parentNode = parentNode;
    }

    public AbstractPublishServiceAction() {
        this(new AttemptedCreate(EntityType.SERVICE), Option.<Folder>none(), Option.<AbstractTreeNode>none());
    }

    protected abstract AbstractPublishServiceWizard createWizard();

    @Override
    protected void performAction() {
        final AbstractPublishServiceWizard dialog = createWizard();
        dialog.addEntityListener(listener);
        if (folder.isSome()) {
            dialog.setFolder(folder.some());
        }
        dialog.pack();
        dialog.setModal(true);
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog);
    }

    @NotNull
    protected Option<Folder> getFolder() {
        return folder;
    }

    protected void setFolder(@NotNull Option<Folder> folder) {
        this.folder = folder;
    }

    @NotNull
    protected Option<AbstractTreeNode> getParentNode() {
        return parentNode;
    }

    protected void setParentNode(@NotNull Option<AbstractTreeNode> parentNode) {
        this.parentNode = parentNode;
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

            final ServicesAndPoliciesTree tree =
                    (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

            if (tree != null) {
                //Remove any filter before reloading
                TopComponents.getInstance().clearFilter();

                reloadParentNode(tree);

                // set selection path to newly published service
                final AbstractTreeNode serviceNode = tree.getRootNode().getNodeForEntity(eh.getGoid());
                tree.setSelectionPath(new TreePath(serviceNode.getPath()));

                // open published service for editing
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new EditPolicyAction((ServiceNode) serviceNode).invoke();

                        //reset filter to ALL
                        tree.filterTreeToDefault();
                    }
                });
            } else {
                log.log(Level.WARNING, "Service tree unreachable.");
            }
        }

        private void reloadParentNode(final ServicesAndPoliciesTree tree) {
            Set<Goid> opened = findExpandedFolderIds(tree);

            RootNode root = tree.getRootNode();

            root.removeAllChildren();
            root.reloadChildren();

            ((DefaultTreeModel) (tree.getModel())).nodeStructureChanged(root);

            restoreExpandedFolders(tree, opened);

            tree.validate();
            tree.repaint();
        }

        private Set<Goid> findExpandedFolderIds(JTree tree) {
            Set<Goid> ret = new HashSet<>();

            int rowCount = tree.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                TreePath path = tree.getPathForRow(i);
                Object obj = path.getLastPathComponent();
                if (obj instanceof FolderNode) {
                    FolderNode node = (FolderNode) obj;
                    if (tree.isExpanded(path))
                        ret.add(node.getGoid());
                }
            }

            return ret;
        }

        private void restoreExpandedFolders(JTree tree, Set<Goid> folderIdsToExpand) {
            Set<Goid> idsToExpand = new HashSet<>(folderIdsToExpand);

            boolean expandedSomething;

            do {
                expandedSomething = false;

                for (int i = 0; i < tree.getRowCount(); i++) {
                    TreePath path = tree.getPathForRow(i);
                    Object obj = path.getLastPathComponent();
                    if (obj instanceof FolderNode) {
                        FolderNode node = (FolderNode) obj;
                        final Goid folderId = node.getGoid();
                        if (idsToExpand.contains(folderId)) {
                            idsToExpand.remove(folderId);
                            expandedSomething = true;
                            tree.expandPath(path);
                            break;
                        }
                    }
                }
            } while (expandedSomething);
        }
    };
}
