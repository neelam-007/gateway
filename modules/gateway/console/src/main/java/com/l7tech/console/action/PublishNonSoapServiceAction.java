package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.PublishNonSoapServiceWizard;
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
 * SSM action to publish a non-soap xml service.
 */
public class PublishNonSoapServiceAction extends SecureAction {
    @NotNull private final Option<Folder> folder;
    @NotNull private final Option<AbstractTreeNode> abstractTreeNode;

    public PublishNonSoapServiceAction() {
        this( Option.<Folder>none(), Option.<AbstractTreeNode>none() );
    }

    public PublishNonSoapServiceAction( @NotNull final Folder folder,
                                        @NotNull final AbstractTreeNode abstractTreeNode ) {
        this( Option.some( folder ), Option.some( abstractTreeNode ) );
    }

    public PublishNonSoapServiceAction( @NotNull final Option<Folder> folder,
                                        @NotNull final Option<AbstractTreeNode> abstractTreeNode ) {
        super(new AttemptedCreate(EntityType.SERVICE), UI_PUBLISH_XML_WIZARD);
        this.folder = folder;
        this.abstractTreeNode = abstractTreeNode;
    }

    @Override
    public String getName() {
        return "Publish Web API";
    }

    @Override
    public String getDescription() {
        return "Publish an entry point for a generic service not based on a WSDL (REST, Web API, FTP)";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/xmlObject16.gif";
    }

    @Override
    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        PublishNonSoapServiceWizard wiz = PublishNonSoapServiceWizard.getInstance(mw);
        wiz.pack();
        Utilities.centerOnScreen(wiz);
        wiz.addEntityListener(listener);
        if (folder.isSome()) wiz.setFolder(folder.some());
        wiz.setModal(true);
        DialogDisplayer.display(wiz);
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
