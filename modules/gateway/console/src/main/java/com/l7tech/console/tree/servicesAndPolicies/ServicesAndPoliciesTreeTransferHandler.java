package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Enumeration;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 24-Jul-2008
 * Time: 10:47:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServicesAndPoliciesTreeTransferHandler extends TransferHandler {
    public ServicesAndPoliciesTreeTransferHandler() {
        super();
    }

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

            TreePath[] selectedPaths = ((ServicesAndPoliciesTree)c).getSelectionPaths();
            List<AbstractTreeNode> transferNodes = new ArrayList<AbstractTreeNode>(selectedPaths.length);

            HashSet<Object> nodesToTransfer = new HashSet<Object>(selectedPaths.length);
            for(TreePath path : selectedPaths) {
                nodesToTransfer.add(path.getLastPathComponent());
            }

            //Before showing what transferNodes were just cut, we need to uncut any transferNodes currently cut
            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)c;
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            tree.setAllChildrenUnCut();

            for(TreePath path : selectedPaths) {
                // Skip a node if an ancestor of its is selected
                boolean skip = false;
                for(int i = 0;i < path.getPathCount() - 1;i++) {
                    if(nodesToTransfer.contains(path.getPathComponent(i))) {
                        skip = true;
                        break;
                    }
                }
                if(skip) {
                    continue;
                }

                Object selectedNode = path.getLastPathComponent();
                if(selectedNode instanceof AbstractTreeNode) {
                    AbstractTreeNode item = (AbstractTreeNode)path.getLastPathComponent();
                    transferNodes.add(item);
                }else{
                    throw new RuntimeException("Node not a AbstractTreeNode: " + selectedNode);
                }

                if(selectedNode instanceof AbstractTreeNode){
                    AbstractTreeNode treeNode = (AbstractTreeNode) selectedNode;
                    treeNode.setCut(true);
                    treeNode.setChildrenCut(true);
                    model.nodeChanged(treeNode);
                }else{
                    throw new RuntimeException("Not an AbstractTreeNode: " + selectedNode);
                }
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
    public boolean canImport(TransferHandler.TransferSupport support) {
        if(support.getComponent() instanceof ServicesAndPoliciesTree && support.getDataFlavors().length == 1 &&
                (FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR.equals(support.getDataFlavors()[0]) ))
        {
            return true;
        } else {
            System.out.println("Cant import");
            return false;
        }
    }

    @Override
    public boolean canImport(JComponent c, DataFlavor[] transferFlavors) {
        if(c instanceof ServicesAndPoliciesTree && transferFlavors.length == 1 &&
                (FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR.equals(transferFlavors[0]) ))
        {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        ServicesAndPoliciesTree tree = null;
        try {
            if(support.getComponent() instanceof ServicesAndPoliciesTree) {
                tree = (ServicesAndPoliciesTree)support.getComponent();
                if(tree.getIgnoreCurrentclipboard()){
                    return false;
                }

                JTree.DropLocation dropLocation = tree.getDropLocation();
                TreePath path;
                if(dropLocation == null) { // Try using the selected path
                    if(tree.getSelectionCount() == 1) {
                        path = tree.getSelectionPath();
                    } else {
                        return false;
                    }
                } else {
                    path = dropLocation.getPath();
                }

                if(support.getDataFlavors().length > 0 && FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR.equals(support.getDataFlavors()[0])) {
                    if(!(path.getLastPathComponent() instanceof FolderNodeBase)) {
                        return false;
                    }

                    FolderNodeBase newParent = (FolderNodeBase)path.getLastPathComponent();
                    List<AbstractTreeNode> nodes = (List<AbstractTreeNode>)support.getTransferable().getTransferData(FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR);
                    for(AbstractTreeNode transferNode : nodes) {
                        if(transferNode instanceof FolderNode) {
                            FolderNode child = (FolderNode) transferNode;
                            Folder folder = new Folder(child.getName(), newParent.getOid());
                            folder.setOid(child.getOid());
                            //todo [Donal] get this to use the FolderManager
                            Registry.getDefault().getServiceManager().savePolicyFolder(folder);
                        } else if(transferNode instanceof ServiceNode) {
                            ServiceNode child = (ServiceNode) transferNode;
                            PublishedService service = child.getPublishedService();
                            System.out.println("New parent id: " + newParent.getOid());
                            service.setFolderOid(newParent.getOid());

                            Registry.getDefault().getServiceManager().savePublishedService(service);

                        } else if(transferNode instanceof PolicyEntityNode) {
                            PolicyEntityNode child = (PolicyEntityNode) transferNode;
                            Policy policy = child.getPolicy();
                            policy.setFolderOid(newParent.getOid());

                            Registry.getDefault().getPolicyAdmin().savePolicy(policy);
                        }

                        //FolderNodeBase identitifes nodes which represent folders
                        AbstractTreeNode parentNode = (AbstractTreeNode) newParent;
                        int insertPosition = parentNode.getInsertPosition(transferNode);
                        parentNode.insert(transferNode, insertPosition);
                    }
                }
                tree.refresh();
            }
        } catch (SaveException e) {
            if(tree != null){
                JOptionPane.showMessageDialog(tree, "Cannot save folder: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        } catch (UpdateException e){
            if(tree != null){
                JOptionPane.showMessageDialog(tree, "Cannot update folder: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        } catch(Exception e){
            return false;
        }

        return true;
    }

    @Override
    public boolean importData(JComponent c, Transferable t) {
        return importData(new TransferSupport(c, t));
    }
}
