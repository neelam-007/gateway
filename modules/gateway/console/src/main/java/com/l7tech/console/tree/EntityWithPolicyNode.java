/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;

import java.util.logging.Logger;
import java.util.Comparator;

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

    public abstract ET getEntity() throws FindException;
    protected abstract String getEntityName();
    public abstract void clearCachedEntities();

    public abstract Policy getPolicy() throws FindException;

    public abstract boolean isAlias();
}
