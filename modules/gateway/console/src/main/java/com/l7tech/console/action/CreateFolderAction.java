package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.PolicyFolderPropertiesDialog;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ConstraintViolationException;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.*;

/**
 * Action for creating a new service/policy folder.
 */
public class CreateFolderAction extends SecureAction {
    private static Logger log = Logger.getLogger(CreateFolderAction.class.getName());

    private final AbstractTreeNode parentNode;
    private final FolderAdmin folderAdmin;
    private final Folder parentFolder;

    public CreateFolderAction(Folder parentFolder, AbstractTreeNode parentNode, FolderAdmin folderAdmin) {
        super(new AttemptedCreate(EntityType.FOLDER));
        this.parentFolder = parentFolder;
        this.parentNode = parentNode;
        this.folderAdmin = folderAdmin;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Create New Folder";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Create New Folder";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }

    @Override
    protected void performAction() {
        createFolder("");
    }

    private void createFolder( final String name ) {
        final Frame f = TopComponents.getInstance().getTopParent();
        final Folder newFolder = new Folder(name, parentFolder);
        final PolicyFolderPropertiesDialog dialog = new PolicyFolderPropertiesDialog(f, new FolderHeader(newFolder), false);
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                if(dialog.isConfirmed()) {
                    //final Folder folder = new Folder(dialog.getName(), parentFolder);
                    newFolder.setName(dialog.getName());
                    newFolder.setSecurityZone(dialog.getSelectedSecurityZone());
                    try {
                        newFolder.setGoid(folderAdmin.saveFolder(newFolder));

                        final JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                        if (tree != null) {
                            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                            FolderHeader header = new FolderHeader(newFolder);
                            final AbstractTreeNode sn = new FolderNode(header, parentFolder);
                            model.insertNodeInto(sn, parentNode, parentNode.getInsertPosition(sn, RootNode.getComparator()));

                            tree.setSelectionPath(new TreePath(sn.getPath()));
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    //reset filter
                                    ((ServicesAndPoliciesTree) tree).filterTreeToDefault();
                                }
                            });
                        }
                    } catch(ConstraintViolationException e) {
                        DialogDisplayer.showMessageDialog(dialog,
                                "Folder '"+dialog.getName()+"' already exists.",
                                "Folder Already Exists",
                                JOptionPane.WARNING_MESSAGE, new Runnable() {
                            @Override
                            public void run() {
                                createFolder(newFolder.getName());
                            }
                        });
                    } catch(UpdateException e) {
                        log.log(Level.WARNING, "Failed to create policy folder", e);
                    } catch(SaveException e) {
                        JOptionPane.showMessageDialog(f, "Cannot create folder: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                        log.log(Level.WARNING, "Failed to create policy folder", e);
                    }
                }

            }
        });

    }
}
