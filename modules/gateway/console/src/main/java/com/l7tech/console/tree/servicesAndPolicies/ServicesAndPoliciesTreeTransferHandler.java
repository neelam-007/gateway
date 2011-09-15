package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.action.EditPolicyProperties;
import com.l7tech.console.action.EditServiceProperties;
import com.l7tech.console.action.PasteAsAliasAction;
import com.l7tech.console.panels.PolicyPropertiesPanel;
import com.l7tech.console.panels.ServicePropertiesDialog;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.AliasAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAll;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.mutable.*;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

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
            //Can only drag and drop if the user is admin or has 'Manage Webservices' role
            if(!ServicesAndPoliciesTree.isUserAuthorizedToUpdateFolders()) return null;

            ServicesAndPoliciesTree servicesAndPoliciesTree = (ServicesAndPoliciesTree) c;            
            List<AbstractTreeNode> transferNodes = servicesAndPoliciesTree.getSmartSelectedNodes();
            //don't let the rootnode be dragged or appear to be draggable
            for(AbstractTreeNode atn : transferNodes){
                if(atn instanceof RootNode){
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
                            return success;
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

    private boolean copyNode(AbstractTreeNode transferNode, final FolderNode newParent, final ServicesAndPoliciesTree tree)
            throws FindException, ConstraintViolationException, UpdateException, VersionException, PolicyAssertionException, SaveException {


        EntityWithPolicyNode childTransferNode = (EntityWithPolicyNode) transferNode;
        Entity entity = childTransferNode.getEntity();
        if (!(entity instanceof HasFolderOid || entity instanceof HasFolder)) return false;

        Object childObj = childTransferNode.getUserObject();
        if(!(childObj instanceof OrganizationHeader)) return false;

        final Folder newParentFolder = newParent.getFolder();
        final OrganizationHeader oH = (OrganizationHeader) childObj;
        final FolderNode oldParent = (FolderNode)transferNode.getParent();
        if(oH.isAlias()){
            return copyAlias(newParent, tree, entity, oldParent);
        }else if(entity instanceof PublishedService){
            return copyService(newParent, tree, newParentFolder, (PublishedService) entity);
        }else if(entity instanceof Policy){
            return copyPolicy(newParent, tree, newParentFolder, (Policy) entity);
        }
        return false;
    }

    private boolean copyAlias(FolderNode newParent, ServicesAndPoliciesTree tree, Entity entity, FolderNode oldParent) {

        //Make sure the folder in which they are being created is not the same the folder they original entities are in
        //this constraint is also enforced in MarkEntityToAliasAction and also in db

        FolderHeader oldParentHeader = (FolderHeader) oldParent.getUserObject();
        FolderHeader newParentHeader = (FolderHeader) newParent.getUserObject();
        if(oldParentHeader.getOid() == newParentHeader.getOid()){
            DialogDisplayer.showMessageDialog(tree, "Cannot create alias in the same folder as original", "Create Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        RootNode rootNode = (RootNode) model.getRoot();
        long parentFolderOid = newParentHeader.getOid();
        Folder parentFolder = ((FolderNode) newParent).getFolder();
        //Create an alias of each node selected

        final OrganizationHeader header;
        Long aliasOid;
        if (entity instanceof PublishedService) {
            PublishedService ps = (PublishedService) entity;
            //check if an alias already exists here
            PublishedServiceAlias checkAlias;
            try {
                checkAlias = Registry.getDefault().getServiceManager().findAliasByEntityAndFolder(ps.getOid(), parentFolderOid);
                if(checkAlias != null){
                    DialogDisplayer.showMessageDialog(tree,"Alias of service " + ps.displayName() + " already exists in folder " + oldParentHeader.getName(), "Create Error", JOptionPane.ERROR_MESSAGE, null);
                    return false;
                }
            } catch (FindException e1) {
                throw new RuntimeException("Unable to check for existing alias", e1);
            }

            try {
                header = new ServiceHeader(ps);
                PublishedServiceAlias psa = new PublishedServiceAlias(ps, parentFolder);
                aliasOid = Registry.getDefault().getServiceManager().saveAlias(psa);
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
                checkAlias = Registry.getDefault().getPolicyAdmin().findAliasByEntityAndFolder(policy.getOid(), parentFolderOid);
                if(checkAlias != null){
                    DialogDisplayer.showMessageDialog(tree,"Alias of policy " + policy.getName() + " already exists in folder " + oldParentHeader.getName(), "Create Error", JOptionPane.ERROR_MESSAGE, null);
                    return false;
                }
            } catch (FindException e1) {
                throw new RuntimeException("Unable to check for existing alias", e1);
            }

            try {
                header = new PolicyHeader(policy);
                PolicyAlias pa = new PolicyAlias(policy, parentFolder);
                aliasOid = Registry.getDefault().getPolicyAdmin().saveAlias(pa);
            } catch (SaveException e1) {
                throw new RuntimeException("Unable to save alias", e1);
            }
        } else {
            throw new IllegalStateException("Referent was neither a Policy nor a Service");
        }

        header.setAliasOid(aliasOid);
        header.setFolderOid(parentFolderOid);
        EntityWithPolicyNode childNode = (EntityWithPolicyNode) TreeNodeFactory.asTreeNode(header, RootNode.getComparator());

        int insertPosition = newParent.getInsertPosition(childNode, RootNode.getComparator());
        newParent.insert(childNode, insertPosition);
        model.nodesWereInserted(newParent, new int[]{insertPosition});
        rootNode.addAlias(header.getOid(), childNode);
        tree.setSelectionPath(new TreePath(childNode.getPath()));
        model.nodeChanged(childNode);
        return true;
    }

    private boolean copyPolicy(final AbstractTreeNode parentNode, final ServicesAndPoliciesTree tree, final Folder newParentFolder, Policy policy) {
        Policy newPolicy = new Policy(policy);
        newPolicy.setGuid(null);
        newPolicy.setOid(Policy.DEFAULT_OID);
        newPolicy.setName("Clone of "+policy.getName());
        newPolicy.setFolder(newParentFolder);

        final Frame mw = TopComponents.getInstance().getTopParent();
        final OkCancelDialog<Policy> dlg = PolicyPropertiesPanel.makeDialog(mw, newPolicy, true);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);

        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    Policy returnedPolicy = dlg.getValue();
                    long oid = 0;
                    try {
                        oid = Registry.getDefault().getPolicyAdmin().savePolicy(returnedPolicy);
                        final AbstractTreeNode policyNode = TreeNodeFactory.asTreeNode(new PolicyHeader(returnedPolicy), RootNode.getComparator());
                        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                        model.insertNodeInto(policyNode, parentNode, parentNode.getInsertPosition(policyNode, RootNode.getComparator()));
                        RootNode rootNode = (RootNode) model.getRoot();
                        rootNode.addEntity(oid, policyNode);
                        tree.setSelectionPath(new TreePath(policyNode.getPath()));
                        model.nodeChanged(policyNode);
                    } catch (SaveException e) {
                        String msg = "Error creating policy:" + e.getMessage();
                        DialogDisplayer.showMessageDialog(mw, null, msg, null);
                        copyPolicy(  parentNode,   tree,  newParentFolder,returnedPolicy);
                    } catch (PolicyAssertionException e) {
                        String msg = "Error while changing policy properties.";
                        DialogDisplayer.showMessageDialog(mw, null, msg, null);
                        copyPolicy(  parentNode,   tree,  newParentFolder,returnedPolicy);
                    }
                    tree.filterTreeToDefault();
                }
            }
        });
        return true;
    }

    private boolean copyService(final AbstractTreeNode parentNode, final ServicesAndPoliciesTree tree, Folder newParentFolder, PublishedService service) {

        final PublishedService newService = new PublishedService(service);

        newService.setOid(PublishedService.DEFAULT_OID);
        newService.getPolicy().setGuid(null);
        newService.getPolicy().setOid(Policy.DEFAULT_OID);
        newService.setName("Clone of " + service.getName());
        newService.setFolder(newParentFolder);

        boolean hasTracePermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdateAll(EntityType.SERVICE));
        final Frame mw = TopComponents.getInstance().getTopParent();
        final ServicePropertiesDialog dlg = new ServicePropertiesDialog(mw, newService, true, hasTracePermission);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    final AbstractTreeNode serviceNode = TreeNodeFactory.asTreeNode(new ServiceHeader(newService), RootNode.getComparator());
                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                    model.insertNodeInto(serviceNode, parentNode, parentNode.getInsertPosition(serviceNode, RootNode.getComparator()));
                    RootNode rootNode = (RootNode) model.getRoot();
                    rootNode.addEntity(newService.getOid(), serviceNode);
                    tree.setSelectionPath(new TreePath(serviceNode.getPath()));
                    model.nodeChanged(serviceNode);

                    tree.filterTreeToDefault();
                }
            }
        });
        return dlg.wasOKed();
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
                Folder updatedFolder = Registry.getDefault().getFolderAdmin().findByPrimaryKey(movedFolder.getOid());
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
                if (fn.isEntityAChildNode(oH.getOid())) {
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
            if (!(entity instanceof HasFolderOid || entity instanceof HasFolder)) return false;

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
                Alias alias = aliasAdmin.findAliasByEntityAndFolder(Long.valueOf(entity.getId()), fh.getOid());
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
                header.setFolderOid(newParent.getOid());
            } else if ( entity instanceof PersistentEntity ) {
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
