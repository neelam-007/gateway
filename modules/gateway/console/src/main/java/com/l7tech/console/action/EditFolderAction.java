package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.PolicyFolderPropertiesDialog;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.some;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Enumeration;
import java.awt.*;


/**
 * Action to rename a folder.
 */
public class EditFolderAction extends SecureAction {
    static Logger log = Logger.getLogger(EditFolderAction.class.getName());

    private Folder folder;
    private FolderAdmin folderAdmin;
    private AbstractTreeNode folderToRename;
    private FolderHeader folderHeader;

    public EditFolderAction(Folder folder, FolderHeader folderHeader, AbstractTreeNode folderToRename, FolderAdmin folderAdmin) {
        super(new AttemptedUpdate(EntityType.FOLDER, folder));
        this.folder = folder;
        this.folderAdmin = folderAdmin;
        this.folderToRename = folderToRename;
        this.folderHeader = folderHeader;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Edit Folder";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Edit Folder";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        editFolder();
    }

    private void editFolder() {
        Frame f = TopComponents.getInstance().getTopParent();
        final PolicyFolderPropertiesDialog dialog = new PolicyFolderPropertiesDialog(f, folderHeader);
        dialog.setVisible(true);

        if(dialog.isConfirmed()) {
            String prevFolderName = folder.getName();
            try {
                folder.setName(dialog.getName());
                folder.setSecurityZone(dialog.getSelectedSecurityZone());
                folderAdmin.saveFolder(folder);
                folderHeader.setName(dialog.getName());
                folderHeader.setSecurityZoneOid(dialog.getSelectedSecurityZone() == null ? null : dialog.getSelectedSecurityZone().getOid());
                final JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.nodeChanged(folderToRename);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            refreshTree(folder, tree);
                        }
                    });
                }
            } catch(ConstraintViolationException e) {
                folder.setName(prevFolderName);
                DialogDisplayer.showMessageDialog(dialog,
                        "Folder '"+dialog.getName()+"' already exists.",
                        "Folder Already Exists",
                        JOptionPane.WARNING_MESSAGE, new Runnable() {
                    @Override
                    public void run() {
                        editFolder();
                    }
                });
            } catch(UpdateException e) {
                folder.setName(prevFolderName);
                DialogDisplayer.showMessageDialog(dialog,
                                                 e.getMessage(),
                                                 "Failed Folder Update",
                                                 JOptionPane.ERROR_MESSAGE, null);

                log.log(Level.WARNING, "Failed to update policy folder", ExceptionUtils.getMessage(e));
            } catch(SaveException e) {
                folder.setName(prevFolderName);
                DialogDisplayer.showMessageDialog(dialog,
                                                 e.getMessage(),
                                                 "Failed Folder Save",
                                                 JOptionPane.ERROR_MESSAGE, null);
                log.log(Level.WARNING, "Failed to save policy folder", ExceptionUtils.getMessage(e));
            }
        }
    }

    private void sortChildren(AbstractTreeNode node){
        if(!(node instanceof FolderNode)){
            return;
        }

        java.util.List<AbstractTreeNode> childNodes = new ArrayList<AbstractTreeNode>();
        for(int i = 0; i < node.getChildCount(); i++){
            AbstractTreeNode childNode = (AbstractTreeNode)node.getChildAt(i);
            childNodes.add(childNode);
            if(childNode instanceof FolderNode){
                sortChildren(childNode);
            }
        }

        //Detach all children
        node.removeAllChildren();
        for(AbstractTreeNode atn: childNodes){
            node.insert(atn, node.getInsertPosition(atn, RootNode.getComparator()));
        }
    }

    private void refreshTree(Folder folder, final JTree tree) {
        try {
            this.folder = folderAdmin.findByPrimaryKey(folder.getOid());

            TreePath rootPath = tree.getPathForRow(0);
            final TreePath selectedNodeTreePath = tree.getSelectionPath();
            final Enumeration pathEnum = tree.getExpandedDescendants(rootPath);

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            RootNode rootNode = (RootNode) model.getRoot();
            sortChildren(rootNode);
            model.nodeStructureChanged(rootNode);

            while (pathEnum.hasMoreElements()) {
                Object pathObj = pathEnum.nextElement();
                TreePath tp = (TreePath) pathObj;
                tree.expandPath(tp);
            }
            tree.setSelectionPath(selectedNodeTreePath);

        } catch (FindException fe) {
            logger.info("Cannot the new folder to update folder node.");
        }
    }
}

