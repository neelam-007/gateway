package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.PolicyPropertiesPanel;
import com.l7tech.console.panels.ServicePropertiesDialog;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.admin.AliasAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAll;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import static com.l7tech.util.Option.optional;
import static com.l7tech.util.TextUtils.isNotEmpty;

/**
 * TransferHandler for the Services and Policies tree.
 */
public class ServicesAndPoliciesTreeTransferHandler extends TransferHandler {

    @Override
    public int getSourceActions(JComponent c) {
        if(c instanceof ServicesAndPoliciesTree) {
            return  TransferHandler.COPY_OR_MOVE;
        } else {
            return TransferHandler.NONE;
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if(c instanceof ServicesAndPoliciesTree) {
            final ServicesAndPoliciesTree servicesAndPoliciesTree = (ServicesAndPoliciesTree) c;

            //get selections with permission check
            final List<AbstractTreeNode> transferNodes =
                    servicesAndPoliciesTree.getSmartSelectedNodesForClipboard();

            //don't let the rootnode be dragged or appear to be draggable
            for( final AbstractTreeNode atn : transferNodes ){
                if( atn instanceof RootNode ){
                    return null;
                }
            }

            //Before showing what transferNodes were just cut, we need to uncut any transferNodes currently cut
            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)c;
            tree.setAllChildrenUnCut();

            for(AbstractTreeNode atn: transferNodes){
                atn.setCut(true);
                atn.setChildrenCut(true);
            }

            if(transferNodes.isEmpty()) {
                return null;
            } else {
                tree.setIgnoreCurrentClipboard(false);
                return new FolderAndNodeTransferable(transferNodes);
            }
        } else {
            return null;
        }
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {

        if (action == TransferHandler.COPY)  {
            List<AbstractTreeNode> nodes;
            try {
                nodes = (List<AbstractTreeNode>)data.getTransferData(FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR);
            } catch (UnsupportedFlavorException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for(AbstractTreeNode atn: nodes){
                atn.setCut(false);
                atn.setChildrenCut(false);
            }
        }
    }

    @Override
    public boolean canImport(JComponent c, DataFlavor[] transferFlavors) {
        return c instanceof ServicesAndPoliciesTree && transferFlavors.length == 1 &&
                (FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR.equals(transferFlavors[0]));
    }

    @Override
    public boolean canImport(TransferSupport support) {
        //determine the drop location
        JTree.DropLocation dropZone = (JTree.DropLocation) support.getDropLocation();
        if ( dropZone != null ) {
            //determine that the drop location is a folder node base.
            //NOTE: RootNode is also an instance of FolderNodeBase
            TreePath path = dropZone.getPath();
            return path != null && path.getLastPathComponent() instanceof FolderNodeBase;
        } else {
            return false;
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean importData(TransferSupport support) {
       return support.getComponent() instanceof JComponent &&
              doImportData((JComponent)support.getComponent(), support.getTransferable(), support.isDrop(),support.getDropAction());
    }
    @Override
    public boolean importData(JComponent comp, Transferable t) {
        return doImportData(comp,t,false, -1);
    }
    public boolean doImportData(JComponent component, Transferable transferable, boolean isDrop, int dropAction) {
        if(component instanceof ServicesAndPoliciesTree) {
            final ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) component;
            try {
                if(tree.getIgnoreCurrentclipboard()){
                    return false;
                }
                TreePath path;
                if(tree.getSelectionCount() == 1) {
                    path = tree.getSelectionPath();
                } else {
                    return false;
                }

                if(transferable.getTransferDataFlavors().length > 0 && FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR.equals(transferable.getTransferDataFlavors()[0])) {
                    if(!(path.getLastPathComponent() instanceof FolderNodeBase)) {
                        return false;
                    }

                    FolderNode newParent = (FolderNode)path.getLastPathComponent();
                    List<AbstractTreeNode> nodes = (List<AbstractTreeNode>)transferable.getTransferData(FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR);

                    for(AbstractTreeNode transferNode : nodes) {
                        if(transferNode.isCut() || (isDrop && dropAction == COPY)){
                            boolean success = moveNode(transferNode, newParent,tree );
                            if(!success)return success;
                        }
                        else{
                            // copy node
                            boolean success = copyNode(transferNode, newParent, tree);
                            if(!success)return success;
                        }
                    }
                }
            } catch (UpdateException e){
                if(tree != null){
                    if (ExceptionUtils.causedBy(e, StaleUpdateException.class)) {
                        DialogDisplayer.showMessageDialog(tree, e.getMessage(),
                                "Update Error", JOptionPane.ERROR_MESSAGE, null);
                    } else {
                        DialogDisplayer.showMessageDialog(tree,"Cannot update folder: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE, null);
                    }
                }
                return false;
            } catch (UserCancelledException e) {
                // copy cancelled as user declined selection of a policy revision
                return false;
            } catch(Exception e){
                if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)){
                    if(tree != null){
                        DialogDisplayer.showMessageDialog(tree,"Cannot save as entity is a duplicate", "Save Error", JOptionPane.ERROR_MESSAGE, null);
                   }
                }
                return false;
            } finally {
                TreePath rootPath = tree.getPathForRow(0);
                final Enumeration pathEnum = tree.getExpandedDescendants(rootPath);

                RootNode rootNode = (RootNode) tree.getModel().getRoot();
                ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(rootNode);
                //tree.setSelectionPath(new TreePath(.getPath()));
                while (pathEnum.hasMoreElements()) {
                    Object pathObj = pathEnum.nextElement();
                    TreePath tp = (TreePath) pathObj;
                    tree.expandPath(tp);
                }
            }
        }
        return true;
    }

    private boolean copyNode( final AbstractTreeNode transferNode,
                              final FolderNode newParent,
                              final ServicesAndPoliciesTree tree)
            throws FindException, ConstraintViolationException, UpdateException, VersionException, PolicyAssertionException, SaveException, UserCancelledException {


        EntityWithPolicyNode childTransferNode = (EntityWithPolicyNode) transferNode;
        Entity entity = childTransferNode.getEntity();
        if (!(entity instanceof HasFolderId || entity instanceof HasFolder)) return false;

        Object childObj = childTransferNode.getUserObject();
        if(!(childObj instanceof OrganizationHeader)) return false;

        final Folder newParentFolder = newParent.getFolder();
        final OrganizationHeader oH = (OrganizationHeader) childObj;
        if (entity instanceof PublishedService){
            if ( oH.isAlias() ) {
                return copyAlias(newParent, tree, (PublishedService)entity);
            } else {
                return copyService(newParent, tree, newParentFolder, (PublishedService) entity);
            }
        } else if(entity instanceof Policy){
            if ( oH.isAlias() ) {
                return copyAlias(newParent, tree, (Policy)entity);
            } else {
                return copyPolicy( newParent, tree, newParentFolder, (Policy) entity );
            }
        }
        return false;
    }

    private <FE extends NamedEntityImp & HasFolder> boolean copyAlias( final FolderNode newParent,
                                                                       final ServicesAndPoliciesTree tree,
                                                                       final FE entity) {

        //Make sure the folder in which they are being created is not the same the folder they original entities are in
        //this constraint is also enforced in MarkEntityToAliasAction and also in db
        //entity.getFolder()
        final Folder entityFolder = entity.getFolder();
        final Folder parentFolder = newParent.getFolder();
        if( entityFolder != null && Goid.equals(entityFolder.getGoid(), parentFolder.getGoid()) ){
            DialogDisplayer.showMessageDialog(tree, "Cannot create alias in the same folder as original", "Create Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        final DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        final RootNode rootNode = (RootNode) model.getRoot();
        final Goid parentFolderGoid = parentFolder.getGoid();
        //Create an alias of each node selected

        final OrganizationHeader header;
        Goid aliasGoid;
        if (entity instanceof PublishedService) {
            PublishedService ps = (PublishedService) entity;
            //check if an alias already exists here
            PublishedServiceAlias checkAlias;
            try {
                checkAlias = Registry.getDefault().getServiceManager().findAliasByEntityAndFolder(ps.getGoid(), parentFolderGoid);
                if(checkAlias != null){
                    DialogDisplayer.showMessageDialog(tree,"Alias of service " + ps.displayName() + " already exists in folder " + parentFolder.getName(), "Create Error", JOptionPane.ERROR_MESSAGE, null);
                    return false;
                }
            } catch (FindException e1) {
                throw new RuntimeException("Unable to check for existing alias", e1);
            }

            try {
                header = new ServiceHeader(ps);
                PublishedServiceAlias psa = new PublishedServiceAlias(ps, parentFolder);
                aliasGoid = Registry.getDefault().getServiceManager().saveAlias(psa);
            } catch (ObjectModelException ome) {
                throw new RuntimeException("Unable to save alias", ome);
            } catch (VersionException ve) {
                throw new RuntimeException("Unable to save alias", ve);
            }
        } else if (entity instanceof Policy) {
            Policy policy = (Policy) entity;
            //check if an alias already exists here
            PolicyAlias checkAlias;
            try {
                checkAlias = Registry.getDefault().getPolicyAdmin().findAliasByEntityAndFolder(policy.getGoid(), parentFolderGoid);
                if(checkAlias != null){
                    DialogDisplayer.showMessageDialog(tree,"Alias of policy " + policy.getName() + " already exists in folder " + parentFolder.getName(), "Create Error", JOptionPane.ERROR_MESSAGE, null);
                    return false;
                }
            } catch (FindException e1) {
                throw new RuntimeException("Unable to check for existing alias", e1);
            }

            try {
                header = new PolicyHeader(policy);
                PolicyAlias pa = new PolicyAlias(policy, parentFolder);
                aliasGoid = Registry.getDefault().getPolicyAdmin().saveAlias(pa);
            } catch (SaveException e1) {
                throw new RuntimeException("Unable to save alias", e1);
            }
        } else {
            throw new IllegalStateException("Referent was neither a Policy nor a Service");
        }

        header.setAliasGoid(aliasGoid);
        header.setFolderId(parentFolderGoid);
        EntityWithPolicyNode childNode = (EntityWithPolicyNode) TreeNodeFactory.asTreeNode(header, RootNode.getComparator());

        int insertPosition = newParent.getInsertPosition(childNode, RootNode.getComparator());
        newParent.insert(childNode, insertPosition);
        model.nodesWereInserted(newParent, new int[]{insertPosition});
        rootNode.addAlias(header.getGoid(), childNode);
        tree.setSelectionPath(new TreePath(childNode.getPath()));
        model.nodeChanged(childNode);
        return true;
    }

    private boolean copyPolicy( final AbstractTreeNode parentNode,
                                final ServicesAndPoliciesTree tree,
                                final Folder newParentFolder,
                                final Policy policy ) throws UserCancelledException {
        // Only policy include fragments can be copied
        if ( policy.getType() != PolicyType.INCLUDE_FRAGMENT ) {
            return false;
        }

        Policy newPolicy = new Policy(policy);
        EntityUtils.updateCopy( newPolicy );
        newPolicy.setGuid( null );
        newPolicy.setFolder( newParentFolder );
        ensureActivePolicy( policy, newPolicy );

        editAndSavePolicy( parentNode, tree, newPolicy );
        return true;
    }

    private void ensureActivePolicy( final Policy originalPolicy,
                                     final Policy copiedPolicy ) throws UserCancelledException {
        if ( originalPolicy.isDisabled() ) {
            String policyXml = WspWriter.getPolicyXml(new AllAssertion());
            final Option<PolicyVersion> version;
            try {
                version = PolicyRevisionUtils.selectRevision( originalPolicy.getGoid(), "copy" );
                if ( version.isSome() ) {
                    final PolicyVersion policyVersion = version.some();
                    final Option<PolicyVersion> fullVersion = optional( Registry.getDefault().getPolicyAdmin().
                            findPolicyVersionByPrimaryKey( policyVersion.getPolicyGoid(), policyVersion.getGoid() ) );
                    policyXml = fullVersion.map( new Unary<String,PolicyVersion>(){
                        @Override
                        public String call( final PolicyVersion policyVersion ) {
                            return policyVersion.getXml();
                        }
                    } ).filter( isNotEmpty() ).orSome( policyXml );
                }
            } catch (FindException e) {
                String msg = "Unable to retrieve versions for disabled policy goid " + originalPolicy.getGoid() + ": " + ExceptionUtils.getMessage(e);
                JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                              msg, "Unable to Retrieve Revisions", JOptionPane.ERROR_MESSAGE);

            }
            copiedPolicy.setXml( policyXml );
        }
    }

    private void editAndSavePolicy( final AbstractTreeNode parentNode,
                                    final ServicesAndPoliciesTree tree,
                                    final Policy newPolicy ) {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final OkCancelDialog<Policy> dlg = PolicyPropertiesPanel.makeDialog( mw, newPolicy, true );
        dlg.pack();
        Utilities.centerOnParentWindow( dlg );

        final Runnable editAndSaveRunnable = new Runnable(){
            @Override
            public void run() {
                editAndSavePolicy( parentNode, tree, newPolicy );
            }
        };

        DialogDisplayer.display( dlg, new Runnable() {
            @Override
            public void run() {
                if ( dlg.wasOKed() ) {
                    Policy returnedPolicy = dlg.getValue();
                    try {
                        final Pair<Goid, String> goidAndGuid = Registry.getDefault().getPolicyAdmin().savePolicy( returnedPolicy );
                        returnedPolicy.setGoid( goidAndGuid.left );
                        returnedPolicy.setGuid( goidAndGuid.right );
                        final AbstractTreeNode policyNode = TreeNodeFactory.asTreeNode( new PolicyHeader( returnedPolicy ), RootNode.getComparator() );
                        final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                        model.insertNodeInto( policyNode, parentNode, parentNode.getInsertPosition( policyNode, RootNode.getComparator() ) );
                        final RootNode rootNode = (RootNode) model.getRoot();
                        rootNode.addEntity( goidAndGuid.left, policyNode );
                        tree.setSelectionPath( new TreePath( policyNode.getPath() ) );
                        model.nodeChanged( policyNode );
                    } catch ( DuplicateObjectException doe ) {
                        String message = "Unable to save the policy '" + returnedPolicy.getName() + "'.\n";
                        if ( returnedPolicy.getType() == PolicyType.GLOBAL_FRAGMENT ) {
                            message += "The policy name is already in use or there is an existing\n" +
                                    "Global Policy Fragment with the '" + returnedPolicy.getInternalTag() + "' tag.";
                        } else if ( returnedPolicy.getType() == PolicyType.INTERNAL && PolicyType.getAuditMessageFilterTags().contains( returnedPolicy.getInternalTag() ) ) {
                            message += "The policy name is already in use or there is an existing\n" +
                                    "Internal Policy with the '" + returnedPolicy.getInternalTag() + "' tag.";
                        } else {
                            message += "The policy name is already used, please choose a different\n name and try again.";

                        }
                        DialogDisplayer.showMessageDialog( mw, "Duplicate policy", message, null, editAndSaveRunnable );
                    } catch ( SaveException e ) {
                        final String msg = "Error creating policy:" + ExceptionUtils.getMessage(e);
                        DialogDisplayer.showMessageDialog( mw, null, msg, null, editAndSaveRunnable );
                    } catch ( PolicyAssertionException e ) {
                        final String msg = "Error creating policy:" + ExceptionUtils.getMessage(e);
                        DialogDisplayer.showMessageDialog( mw, null, msg, null, editAndSaveRunnable );
                    }
                    tree.filterTreeToDefault();
                }
            }
        } );
    }

    private boolean copyService(final AbstractTreeNode parentNode, final ServicesAndPoliciesTree tree, Folder newParentFolder, PublishedService service) throws UserCancelledException {

        final PublishedService newService = new PublishedService(service);
        EntityUtils.updateCopy( newService );
        EntityUtils.updateCopy( newService.getPolicy() );
        newService.getPolicy().setGuid( null );
        newService.setFolder(newParentFolder);
        ensureActivePolicy( service.getPolicy(), newService.getPolicy() );

        try {
            final Registry registry = Registry.getDefault();
            final boolean hasTracePermission = registry.getSecurityProvider().hasPermission(new AttemptedUpdateAll(EntityType.SERVICE));
            final Frame mw = TopComponents.getInstance().getTopParent();
            final Collection<ServiceDocument> documents = registry.getServiceManager().findServiceDocumentsByServiceID( service.getId() );
            if ( documents != null ) {
                for ( final ServiceDocument document : documents ) {
                    EntityUtils.resetIdentity( document );
                }
            }
            final ServicePropertiesDialog dlg = new ServicePropertiesDialog(mw, newService, documents, true, hasTracePermission);
            dlg.pack();
            Utilities.centerOnParentWindow(dlg);
            dlg.selectNameField();
            DialogDisplayer.display(dlg, new Runnable() {
                @Override
                public void run() {
                    if (dlg.wasOKed()) {
                        Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                        PublishedService savedService = dlg.getService();
                        final AbstractTreeNode serviceNode = TreeNodeFactory.asTreeNode(new ServiceHeader(savedService), RootNode.getComparator());
                        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                        model.insertNodeInto(serviceNode, parentNode, parentNode.getInsertPosition(serviceNode, RootNode.getComparator()));
                        RootNode rootNode = (RootNode) model.getRoot();
                        rootNode.addEntity(savedService.getGoid(), serviceNode);
                        tree.setSelectionPath(new TreePath(serviceNode.getPath()));
                        model.nodeChanged(serviceNode);
                        tree.filterTreeToDefault();
                    }
                }
            });
            return true;
        } catch ( FindException e ) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error accessing service for copy." );
            return false;
        }
    }


    private boolean moveNode(AbstractTreeNode transferNode, FolderNode newParent, final ServicesAndPoliciesTree tree) throws FindException, ConstraintViolationException, UpdateException {
        FolderNode updatedFolderNode = null;
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            AbstractTreeNode oldParent = (AbstractTreeNode)transferNode.getParent();
        final Folder newParentFolder = newParent.getFolder();
        if(transferNode instanceof FolderNode) {
            FolderNode child = (FolderNode) transferNode;
            Folder movedFolder = child.getFolder();
            try {
                Registry.getDefault().getFolderAdmin().moveEntityToFolder( newParentFolder, movedFolder );

                //need to update with new folder version from the database
                Folder updatedFolder = Registry.getDefault().getFolderAdmin().findByPrimaryKey(movedFolder.getGoid());
                updatedFolderNode = new FolderNode(new FolderHeader(updatedFolder), updatedFolder.getFolder());
            } catch(ConstraintViolationException e) {
                DialogDisplayer.showMessageDialog(tree,
                                 "Folder '"+movedFolder.getName()+"' already exists.",
                                 "Folder Already Exists",
                                 JOptionPane.WARNING_MESSAGE, null);
                return false;
            } catch (FindException fe) {
                //should not happen
                DialogDisplayer.showMessageDialog(tree,
                                 "Cannot find folder '"+movedFolder.getName()+"'.",
                                 "Folder cannot be found",
                                 JOptionPane.ERROR_MESSAGE, null);
                return false;
            }
        }else if(transferNode instanceof EntityWithPolicyNode){
            EntityWithPolicyNode childTransferNode = (EntityWithPolicyNode) transferNode;
            Object childObj = childTransferNode.getUserObject();
            if(!(childObj instanceof OrganizationHeader)) return false;
            OrganizationHeader oH = (OrganizationHeader) childObj;

            //See if an node already representing this entity already exists in this folder location for this service
            if (newParent instanceof FolderNode) {
                FolderNode fn = (FolderNode) newParent;
                if (fn.isEntityAChildNode(oH.getGoid())) {
                    if (tree != null) {
                        DialogDisplayer.showMessageDialog(tree,
                                                         "Cannot move an entity with another entity (alias or original) in the same folder",
                                                         "Move Error",
                                                         JOptionPane.ERROR_MESSAGE, null);


                    }
                    RootNode rootNode = (RootNode) model.getRoot();
                    rootNode.setCut(false);
                    rootNode.setChildrenCut(false);
                    return false;
                }
            }

            Entity entity = childTransferNode.getEntity();
            if (!(entity instanceof HasFolderId || entity instanceof HasFolder)) return false;

            if (childTransferNode instanceof ServiceNodeAlias || childTransferNode instanceof PolicyEntityNodeAlias) {
                //With the entity oid and the old folder oid we can find the actual alias
                //which we need to update
                Object userObj = oldParent.getUserObject();
                if(!(userObj instanceof FolderHeader)) return false;
                FolderHeader fh = (FolderHeader) userObj;
                AliasAdmin aliasAdmin = null;
                if(childTransferNode instanceof ServiceNode) {
                    aliasAdmin = Registry.getDefault().getServiceManager();
                } else if(childTransferNode instanceof PolicyEntityNode) {
                    aliasAdmin = Registry.getDefault().getPolicyAdmin();
                }
                Alias alias = aliasAdmin.findAliasByEntityAndFolder(Goid.parseGoid(entity.getId()), fh.getGoid());
                if(alias == null) {
                    DialogDisplayer.showMessageDialog(tree, "Cannot find alias", "Find Error", JOptionPane.ERROR_MESSAGE, null);
                    return false;
                }

                // now update the alias parent folder
                Registry.getDefault().getFolderAdmin().moveEntityToFolder( newParentFolder, alias );

                //Update the OrganizationHeader representing the alias
                //this is enough to update the entity correctly. Below when updateUserObject is called
                //after it downloads the entity it will use the OrganizationHeader to update the aliases
                //folder and alias properties
                OrganizationHeader header = (OrganizationHeader) childTransferNode.getUserObject();
                header.setFolderId(newParent.getGoid());
            } else if ( entity instanceof PersistentEntity) {
                Registry.getDefault().getFolderAdmin().moveEntityToFolder( newParentFolder, (PersistentEntity)entity );

            }
            childTransferNode.updateUserObject();
        }

        transferNode.setCut(false);
        transferNode.setChildrenCut(false);

        int transferNodeIndex = oldParent.getIndex(transferNode);
        int insertPosition = newParent.getInsertPosition(transferNode);

        //we need to update the tree with the correct folder version if we are modifying the folder node
        if (transferNode instanceof FolderNode && updatedFolderNode != null) {

            //move all children of the changed node to the updated node
            Enumeration theChildren = transferNode.children();
            Vector<AbstractTreeNode> collectedChildren = new Vector<AbstractTreeNode>();

            //collect the children
            while (theChildren.hasMoreElements()) {
                collectedChildren.add((AbstractTreeNode) theChildren.nextElement());
            }

            //add them to the updated folder node
            for (AbstractTreeNode node : collectedChildren) {
                updatedFolderNode.insert(node, updatedFolderNode.getInsertPosition(node, RootNode.getComparator()));
            }

            oldParent.remove(transferNodeIndex);    //remove the older version
            newParent.insert(updatedFolderNode, insertPosition);   //add the updated version

            //order is important. Update the old parent first
            model.nodesWereRemoved(oldParent, new int[]{transferNodeIndex}, new Object[]{transferNode});
            model.nodesWereInserted(newParent, new int[]{insertPosition});
            tree.setSelectionPath(new TreePath(updatedFolderNode.getPath()));   //set to expand the folder for new locaiton
            model.nodeChanged(updatedFolderNode);

        } else {
            newParent.insert(transferNode, insertPosition);

            //order is important. Update the old parent first
            model.nodesWereRemoved(oldParent, new int[]{transferNodeIndex}, new Object[]{transferNode});
            model.nodesWereInserted(newParent, new int[]{insertPosition});
            tree.setSelectionPath(new TreePath(transferNode.getPath()));
            model.nodeChanged(transferNode);
        }
        return true;
    }
}
