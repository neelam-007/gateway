/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class represents Policy nodes in the lower-left policy CRUD tree
 */
public abstract class EntityWithPolicyNode<ET extends Entity, HT extends EntityHeader> extends EntityHeaderNode<HT> {
    static final Logger log = Logger.getLogger(EntityWithPolicyNode.class.getName());

    /**
     * construct the <CODE>PolicyEntityNode</CODE> instance for
     * a given entity header.
     *
     * @param e the EntityHeader instance, must represent published service
     * @throws IllegalArgumentException thrown if unexpected type
     */
    public EntityWithPolicyNode(HT e)
      throws IllegalArgumentException {
        this(e, null);
    }

    public EntityWithPolicyNode(HT e, Comparator c){
        super(e, c);
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    @Override
    public boolean canDelete() {
        return true;
    }

    /** @return the object ID of the entity for this node (either a PublishedService or a Policy). */
    public Goid getEntityGoid() {
        return getEntityHeader().getGoid();
    }

    /**
     * Get the entity.  As this may trigger a lazy download you should prefer to use
     * the entity header instead whenever possible.  If you only need the entity OID,
     * use {@link #getEntityGoid()}.
     *
     * @return the Entity behind this Service Node
     * @throws FindException if the Entity cannot be loaded, or has been deleted
     */
    public abstract ET getEntity() throws FindException;
    
    protected abstract String getEntityName();
    public abstract void clearCachedEntities();

    public abstract Policy getPolicy() throws FindException;

    public abstract void updateUserObject() throws FindException;

    protected void orphanMe() throws FindException {
        ensureOrphan();
        throw new FindException(getEntityName() + " does not exist any more.");
    }

    private void ensureOrphan() {
        TopComponents creg = TopComponents.getInstance();
        JTree tree = (JTree)creg.getComponent(ServicesAndPoliciesTree.NAME);
        if (tree == null)
            return;
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        if (model == null)
            return;
        final TreeNode parent = this.getParent();
        if (parent != null) {
            Enumeration kids = parent.children();
            while (kids.hasMoreElements()) {
                TreeNode node = (TreeNode) kids.nextElement();
                if (node == this) {
                    model.removeNodeFromParent(this);
                    break;
                }
            }
        }
        RootNode rootNode = (RootNode) model.getRoot();
        if (rootNode == null)
            return;
        HT header = (HT) getUserObject();
        if(header instanceof OrganizationHeader){
            OrganizationHeader oH = (OrganizationHeader) header;
            if(!oH.isAlias()){
                Set<AbstractTreeNode> foundNodes = rootNode.getAliasesForEntity(oH.getGoid());
                if(!foundNodes.isEmpty()){
                    for(AbstractTreeNode atn: foundNodes){
                        model.removeNodeFromParent(atn);
                    }
                    rootNode.removeEntity(oH.getGoid());
                }
            }else{
                rootNode.removeAlias(oH.getGoid(), this);
            }
        }
    }
}
