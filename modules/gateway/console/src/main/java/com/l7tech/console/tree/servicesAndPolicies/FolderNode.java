package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.action.*;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;

/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with services.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class FolderNode extends AbstractTreeNode implements FolderNodeBase {
    private final FolderHeader folderHeader;
    private final Folder folder;
    private final Action[] allActions;

    public FolderNode(FolderHeader folderHeader, Folder parentFolder) {
        super(folderHeader, RootNode.getComparator());
        this.folderHeader = folderHeader;

        final FolderAdmin folderAdmin = Registry.getDefault().getFolderAdmin();

        if (parentFolder == null && folderHeader.getParentFolderOid() != null) {
            try {
                parentFolder = folderAdmin.findByPrimaryKey(folderHeader.getParentFolderOid());
            } catch (FindException e) {
                throw new RuntimeException("Couldn't find parent folder", e);
            }
        }

        folder = new Folder(folderHeader.getName(), parentFolder);
        folder.setOid(folderHeader.getOid());

        allActions = new Action[]{
            new EditFolderAction(folder, folderHeader, this, folderAdmin),
            new CreateFolderAction(folder, this, folderAdmin),
            new DeleteFolderAction(folderHeader.getOid(), this, folderAdmin),
            new PasteAsAliasAction(this),
            new RefreshTreeNodeAction(this)
        };
    }

    public String getName() {
        return folderHeader.getName();
    }

    public long getOid() {
        return folderHeader.getOid();
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/folder.gif";
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    public void addChild(AbstractTreeNode child) {
        insert(child, getInsertPosition(child, RootNode.getComparator()));
    }

    public AbstractTreeNode addEntityNode(EntityHeader entityHeader) {
        AbstractTreeNode child = TreeNodeFactory.asTreeNode(entityHeader, null);
        insert(child, getInsertPosition(child, RootNode.getComparator()));
        return child;
    }

    @Override
    public Action[] getActions() {
        // Filter unlicensed actions
        List<Action> actions = new ArrayList<Action>();
        for (Action action : allActions) {
            if(action instanceof PasteAsAliasAction){
                if(!RootNode.isAliasSet()) continue;
            }
            if (action.isEnabled())
                actions.add(action);
        }
        Action secureCut = ServicesAndPoliciesTree.getSecuredAction(EntityType.FOLDER,
                                                                OperationType.UPDATE,
                                                                ServicesAndPoliciesTree.ClipboardActionType.CUT);
        if(secureCut != null) actions.add(secureCut);
        Action securePaste = ServicesAndPoliciesTree.getSecuredAction(EntityType.FOLDER,
                                                                OperationType.UPDATE,
                                                                ServicesAndPoliciesTree.ClipboardActionType.PASTE);
        if(securePaste != null) actions.add(securePaste);

        return actions.toArray(new Action[]{});
    }

    /**
     * Non recursive method to determine if the entity represented by the supplied oid is a direct child
     * of this folder node. Will also return true if an alias representing this entity is in this folder
     * @param oid the oid to search for
     * @return true if oid's entity is a direct child, false otherwise
     */
    public boolean isEntityAChildNode(long oid){
        for(int i = 0; i < getChildCount(); i++){
            AbstractTreeNode atn = (AbstractTreeNode) getChildAt(i);
            Object userObj = atn.getUserObject();
            if(atn instanceof EntityWithPolicyNode){
                EntityHeader aH = (EntityHeader) userObj;
                if(aH.getOid() == oid){
                    return true;
                }
            }
        }
        return false;
    }

    public Folder getFolder() {
        return folder;
    }
}
