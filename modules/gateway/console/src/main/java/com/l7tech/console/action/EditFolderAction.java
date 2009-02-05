package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.RefreshTreeNodeAction;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.PolicyFolderPropertiesDialog;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.*;


/**
 * Action to rename a folder.
 */
public class EditFolderAction extends SecureAction {
    static Logger log = Logger.getLogger(CreateFolderAction.class.getName());

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
        return "Rename Folder";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Rename Folder";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /**
     */
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        PolicyFolderPropertiesDialog dialog = new PolicyFolderPropertiesDialog(f, folder.getName());
        dialog.setModal(true);
        dialog.setVisible(true);

        if(dialog.isConfirmed()) {
            String prevFolderName = folder.getName();
            try {
                folder.setName(dialog.getName());
                folderAdmin.saveFolder(folder);
                folderHeader.setName(dialog.getName());
                final JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.nodeChanged(folderToRename);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            RefreshTreeNodeAction refresh = new RefreshTreeNodeAction((RootNode) tree.getModel().getRoot());
                            refresh.setTree(tree);
                            refresh.invoke();
                        }
                    });
                }
            } catch(ConstraintViolationException e) {
                folder.setName(prevFolderName);
                DialogDisplayer.showMessageDialog(dialog,
                                                 "Folder '"+dialog.getName()+"' already exists.",
                                                 "Folder Already Exists",
                                                 JOptionPane.WARNING_MESSAGE, null);
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
}

