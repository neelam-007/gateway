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

                AbstractTreeNode treeNode = (AbstractTreeNode) selectedNode;
                treeNode.setCut(true);
                treeNode.setChildrenCut(true);
                model.nodeChanged(treeNode);
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

                if(transferable.getTransferDataFlavors().length > 0 && FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR.equals(transferable.getTransferDataFlavors()[0])) {
                    if(!(path.getLastPathComponent() instanceof FolderNodeBase)) {
                        return false;
                    }

                    FolderNodeBase newParent = (FolderNodeBase)path.getLastPathComponent();
                    List<AbstractTreeNode> nodes = (List<AbstractTreeNode>)transferable.getTransferData(FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR);
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
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
                            child.updateUserObject();
                        } else if(transferNode instanceof PolicyEntityNode) {
                            PolicyEntityNode child = (PolicyEntityNode) transferNode;
                            Policy policy = child.getPolicy();
                            policy.setFolderOid(newParent.getOid());
                            Registry.getDefault().getPolicyAdmin().savePolicy(policy);
                            child.updateUserObject();
                        }

                        AbstractTreeNode oldParent = (AbstractTreeNode)transferNode.getParent();
                        int transferNodeIndex = oldParent.getIndex(transferNode);

                        //FolderNodeBase identitifes nodes which represent folders
                        AbstractTreeNode parentNode = (AbstractTreeNode) newParent;

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
        }
        return true;
    }
}
