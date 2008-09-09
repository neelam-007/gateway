package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.admin.AliasAdmin;
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
            if(!ServicesAndPoliciesTree.isUserAuthorizedToMoveFolders()) return null;

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
                            Registry.getDefault().getFolderAdmin().saveFolder(folder);
                        }else if(transferNode instanceof EntityWithPolicyNode){
                            EntityWithPolicyNode childTransferNode = (EntityWithPolicyNode) transferNode;
                            Object childObj = childTransferNode.getUserObject();
                            if(!(childObj instanceof OrganizationHeader)) return false;
                            OrganizationHeader oH = (OrganizationHeader) childObj;

                            //See if an node already representing this entity already exists in this folder location for this service
                            if(parentNode instanceof FolderNode){
                                FolderNode fn = (FolderNode) parentNode;
                                if(fn.isEntityAChildNode(oH.getOid())){
                                    if(tree != null){
                                        JOptionPane.showMessageDialog(tree, "Cannot move an entity with another entity (alias or original) in the same folder", "Move Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                    RootNode rootNode = (RootNode) model.getRoot();
                                    rootNode.setCut(false);
                                    rootNode.setChildrenCut(false);
                                    return false;
                                }
                            }

                            Entity entity = childTransferNode.getEntity();
                            if(!(entity instanceof HasFolder)) return false;

                            if(childTransferNode instanceof ServiceNodeAlias || childTransferNode instanceof PolicyEntityNodeAlias){
                                //With the entity oid and the old folder oid we can find the actual alias
                                //which we need to update
                                Object userObj = oldParent.getUserObject();
                                if(!(userObj instanceof FolderHeader)) return false;
                                FolderHeader fh = (FolderHeader) userObj;
                                AliasAdmin aliasAdmin = null;
                                if(childTransferNode instanceof ServiceNode){
                                    aliasAdmin = Registry.getDefault().getServiceManager();
                                }else if(childTransferNode instanceof PolicyEntityNode){
                                    aliasAdmin = Registry.getDefault().getPolicyAdmin();
                                }
                                Alias alias = aliasAdmin.findAliasByEntityAndFolder(Long.valueOf(entity.getId()), fh.getOid());
                                if(alias == null){
                                    DialogDisplayer.showMessageDialog(tree,
                                      "Cannot find alias",
                                      "Find Error",
                                      JOptionPane.ERROR_MESSAGE, null);
                                    return false;
                                }
                                //now update the alias
                                alias.setFolderOid(newParent.getOid());
                                aliasAdmin.saveAlias(alias);

                                //Update the OrganizationHeader representing the alias
                                //this is enough to update the entity correctly. Below when updateUserObject is called
                                //after it downloads the entity it will use the OrganizationHeader to update the aliases
                                //folder and alias properties
                                OrganizationHeader header = (OrganizationHeader) childTransferNode.getUserObject();
                                header.setFolderOid(newParent.getOid());
                            }else{
                                HasFolder o = (HasFolder) entity;
                                o.setFolderOid(newParent.getOid());
                                if(childTransferNode instanceof ServiceNode){
                                    Registry.getDefault().getServiceManager().savePublishedService((PublishedService) entity);
                                }else if(childTransferNode instanceof PolicyEntityNode){
                                    Registry.getDefault().getPolicyAdmin().savePolicy((Policy) entity);
                                }
                            }
                            childTransferNode.updateUserObject();
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
