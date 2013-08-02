package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.PublishInternalServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.logging.Level;

/**
 * User: megery
 */
public class PublishInternalServiceAction extends SecureAction{
    @NotNull private final Option<Folder> folder;
    @NotNull private final Option<AbstractTreeNode> abstractTreeNode;

    public PublishInternalServiceAction() {
        this( Option.<Folder>none(), Option.<AbstractTreeNode>none() );
    }

    public PublishInternalServiceAction( @NotNull final Folder folder,
                                         @NotNull final AbstractTreeNode abstractTreeNode ) {
        this( Option.some( folder ), Option.some( abstractTreeNode ) );
    }

    public PublishInternalServiceAction( @NotNull final Option<Folder> folder,
                                         @NotNull final Option<AbstractTreeNode> abstractTreeNode ) {
        super(new AttemptedCreate(EntityType.SERVICE), UI_PUBLISH_SERVICE_WIZARD);
        this.folder = folder;
        this.abstractTreeNode = abstractTreeNode;
    }
    
    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Publish Internal Service";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Publish Internal Service";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/services16.png";
    }

    /**
     */
    @Override
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        PublishInternalServiceWizard dialog = PublishInternalServiceWizard.getInstance(f);
        dialog.addEntityListener(listener);
        if( folder.isSome() ) dialog.setFolder( folder.some() );
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
            EntityHeader eh = (EntityHeader)ev.getEntity();
            final JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            if (tree != null) {
                AbstractTreeNode root = TopComponents.getInstance().getServicesFolderNode();
                AbstractTreeNode parent = abstractTreeNode.orSome( root );

                //Remove any filter before insert
                TopComponents.getInstance().clearFilter();

                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(eh, null);
                model.insertNodeInto(sn, parent, parent.getInsertPosition(sn, RootNode.getComparator()));
                RootNode rootNode = (RootNode) model.getRoot();
                rootNode.addEntity(eh.getGoid(), sn);

                tree.setSelectionPath(new TreePath(sn.getPath()));
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new EditPolicyAction((ServiceNode)sn).invoke();

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
