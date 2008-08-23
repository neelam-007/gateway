package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.policy.Policy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.util.List;

/**
 * TransferHandler for the Services and Policies tree.
 */
public class ServicesAndPoliciesTreeTransferHandler extends TransferHandler {

    @Override
    public int getSourceActions(JComponent c) {
        if(c instanceof ServicesAndPoliciesTree) {
            return TransferHandler.MOVE;
        } else {
            return TransferHandler.NONE;
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if(c instanceof ServicesAndPoliciesTree) {
            //Can only drag and drop if the user is admin or has 'Manage Webservices' role
            //use an AttemptedUpdate, which represents an Update attempty on a Policy_Folder to determine
            //whether drag and drop is enabled for this user
            //todo [Donal] create utility method for this check
            final Folder folder = new Folder("TestFolder", null);
            AttemptedUpdate attemptedUpdate = new AttemptedUpdate(EntityType.FOLDER, folder);
            if (Registry.getDefault().isAdminContextPresent()){
                SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
                if(!securityProvider.hasPermission(attemptedUpdate)){
                    return null;
                }
            }

            ServicesAndPoliciesTree servicesAndPoliciesTree = (ServicesAndPoliciesTree) c;            
            List<AbstractTreeNode> transferNodes = servicesAndPoliciesTree.getSmartSelectedNodes();
            //don't let the rootnode be dragged or appear to be draggable
            for(AbstractTreeNode atn: transferNodes){
                if(atn instanceof RootNode){
                    return null;
                }
            }

            //Before showing what transferNodes were just cut, we need to uncut any transferNodes currently cut
            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)c;
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            tree.setAllChildrenUnCut();

            for(AbstractTreeNode atn: transferNodes){
                atn.setCut(true);
                atn.setChildrenCut(true);
                model.nodeChanged(atn);
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
    }

    @Override
    public boolean canImport(JComponent c, DataFlavor[] transferFlavors) {
        return c instanceof ServicesAndPoliciesTree && transferFlavors.length == 1 &&
                (FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR.equals(transferFlavors[0]));
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean importData(JComponent component, Transferable transferable) {
        if(component instanceof ServicesAndPoliciesTree) {
            final ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) component;
            try {
                if(tree.getIgnoreCurrentclipboard()){
                    return false;
                }
// TODO [Donal] ensure this is ok
//                JTree.DropLocation dropLocation = tree.getDropLocation();
                  TreePath path;
//                if(dropLocation == null) { // Try using the selected path
                    if(tree.getSelectionCount() == 1) {
                        path = tree.getSelectionPath();
                    } else {
                        return false;
                    }
//                } else {
//                    path = dropLocation.getPath();
//                }
                //TODO [Donal] When ever going to paste need to make sure that the service / policy has not been changed
                //TODO [Donal] by another admin user via a different SSM, load the service and check it's in the same folder
                if(transferable.getTransferDataFlavors().length > 0 && FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR.equals(transferable.getTransferDataFlavors()[0])) {
                    if(!(path.getLastPathComponent() instanceof FolderNodeBase)) {
                        return false;
                    }

                    FolderNodeBase newParent = (FolderNodeBase)path.getLastPathComponent();
                    AbstractTreeNode parentNode = (AbstractTreeNode) newParent;
                    List<AbstractTreeNode> nodes = (List<AbstractTreeNode>)transferable.getTransferData(FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR);
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

                    for(AbstractTreeNode transferNode : nodes) {
                        AbstractTreeNode oldParent = (AbstractTreeNode)transferNode.getParent();
                        
                        if(transferNode instanceof FolderNode) {
                            FolderNode child = (FolderNode) transferNode;
                            Folder folder = new Folder(child.getName(), newParent.getOid());
                            folder.setOid(child.getOid());
                            //todo [Donal] get this to use the FolderManager
                            Registry.getDefault().getServiceManager().saveFolder(folder);
                        } else if(transferNode instanceof ServiceNode) {
                            ServiceNode child = (ServiceNode) transferNode;
                            PublishedService service = child.getPublishedService();
                            Object parentObj = parentNode.getUserObject();
                            if(!(parentObj instanceof FolderHeader)) return false;

                            //See if an node already representing this entity already exists in this folder location for this service
                            if(parentNode instanceof FolderNode){
                                FolderNode fn = (FolderNode) parentNode;
                                if(fn.isEntityAChildNode(service.getOid())){
                                    if(tree != null){
                                        JOptionPane.showMessageDialog(tree, "Cannot move an entity with another entity (alias or original) in the same folder", "Move Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                    return false;
                                }
                            }

                            if(child.isAlias()){
                                ServiceAdmin sAdmin = Registry.getDefault().getServiceManager();
                                //With it's service oid and old folder oid we can find the actual alias
                                //which we need to update
                                Object userObj = oldParent.getUserObject();
                                if(!(userObj instanceof FolderHeader)) return false;
                                FolderHeader fh = (FolderHeader) userObj;
                                PublishedServiceAlias psa = sAdmin.findAliasByServiceAndFolder(service.getOid(), fh.getOid());
                                if(psa == null){
                                    DialogDisplayer.showMessageDialog(tree,
                                      "Cannot find alias",
                                      "Find Error",
                                      JOptionPane.ERROR_MESSAGE, null);
                                    return false;
                                }
                                //now update the alias
                                psa.setFolderOid(newParent.getOid());
                                sAdmin.savePublishedServiceAlias(psa);
                                //Update the ServiceHeader representing the alias
                                ServiceHeader sH = (ServiceHeader) child.getUserObject();
                                sH.setFolderOid(newParent.getOid());

                            }else{
                                service.setFolderOid(newParent.getOid());
                                Registry.getDefault().getServiceManager().savePublishedService(service);
                            }
                            child.updateUserObject();
                            
                        } else if(transferNode instanceof PolicyEntityNode) {
                            PolicyEntityNode child = (PolicyEntityNode) transferNode;
                            Policy policy = child.getPolicy();
                            policy.setFolderOid(newParent.getOid());
                            Registry.getDefault().getPolicyAdmin().savePolicy(policy);
                            child.updateUserObject();
                        }
                        int transferNodeIndex = oldParent.getIndex(transferNode);
                        int insertPosition = parentNode.getInsertPosition(transferNode, RootNode.getComparator());
                        parentNode.insert(transferNode, insertPosition);

                        //order is important. Update the old parent first
                        model.nodesWereRemoved(oldParent, new int[]{transferNodeIndex}, new Object[]{transferNode});
                        model.nodesWereInserted(parentNode, new int[]{insertPosition});

                        transferNode.setCut(false);
                        transferNode.setChildrenCut(false);
                    }
                }

            } catch (SaveException e) {
                if(tree != null){
                    DialogDisplayer.showMessageDialog(tree,"Cannot save folder: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE, null);
                }
                return false;
            } catch (UpdateException e){
                if(tree != null){
                    DialogDisplayer.showMessageDialog(tree,"Cannot update folder: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE, null);
                }
                return false;
            } catch(Exception e){
                if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)){
                    if(tree != null){
                        DialogDisplayer.showMessageDialog(tree,"Cannot save as entity is a duplicate", "Save Error", JOptionPane.ERROR_MESSAGE, null);
                   }
                }
                return false;
            }
        }
        return true;
    }
}
