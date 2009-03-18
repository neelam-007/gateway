/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.console.action.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;

import javax.swing.*;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.logging.Level;

/** @author alex */
public class PolicyEntityNode extends EntityWithPolicyNode<Policy, PolicyHeader> {
    protected volatile Reference<Policy> policy;

    public PolicyEntityNode(PolicyHeader e) {
        this(e, null);
    }

    public PolicyEntityNode(PolicyHeader e, Comparator c) {
        super(e, c);
    }

    @Override
    public void updateUserObject() throws FindException{
        policy = null;
        getEntity();
    }
    
    public Policy getPolicy() throws FindException {
        if (policy != null) {
            Policy p = policy.get();
            if (p != null)
                return p;
        }

        PolicyHeader eh = getEntityHeader();
        Policy updatedPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(eh.getGuid());

        // throw something if null, the service may have been deleted
        if (updatedPolicy == null) {
            orphanMe();
            return null; // Unreached; method always throws
        }

        PolicyHeader newEh = new PolicyHeader(updatedPolicy);
        newEh.setAliasOid(eh.getAliasOid());
        newEh.setFolderOid(eh.getFolderOid());
        setUserObject(newEh);
        firePropertyChange(this, "UserObject", eh, newEh);

        this.policy = new SoftReference<Policy>(updatedPolicy);
        return updatedPolicy;
    }

    public Policy getEntity() throws FindException {
        return getPolicy();
    }

    @Override
    public Action[] getActions() {
        Collection<Action> actions = new ArrayList<Action>();
        actions.add(new EditPolicyAction(this));
        actions.add(new EditPolicyProperties(this));
        actions.add(new DeletePolicyAction(this));
        actions.add(new MarkEntityToAliasAction(this));
        actions.add(new PolicyRevisionsAction(this));
        actions.add(new RefreshTreeNodeAction(this));

        Action secureCut = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.CUT);
        if(secureCut != null){
            actions.add(secureCut);
        }
        
        return actions.toArray(new Action[0]);
    }

    @Override
    protected String getEntityName() {
        return getEntityHeader().getName();
    }

    public void clearCachedEntities() {
        policy = null;
    }

    @Override
    public String getName() {
        return getEntityHeader().getName();
    }

    @Override
    protected String iconResource(boolean open) {
        PolicyHeader header = getEntityHeader();
        if(header == null) return "com/l7tech/console/resources/include16.png";

        boolean isSoap;
        boolean isInternal;
        try {
            isSoap = getPolicy().isSoap();
            isInternal = getPolicy().getType() == PolicyType.INTERNAL;
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(
                  Level.SEVERE, e,
                "Error accessing policy entity");
            return "com/l7tech/console/resources/include16.png";
        }

        if (isInternal) {
            if (isSoap){
                if(header.isAlias()){
                    return "com/l7tech/console/resources/include_internalsoap16Alias.png";                    
                }else{
                    return "com/l7tech/console/resources/include_internalsoap16.png";
                }
            }
            else{
                if(header.isAlias()){
                    return "com/l7tech/console/resources/include_internal16Alias.png";                    
                }else{
                    return "com/l7tech/console/resources/include_internal16.png";
                }
            }
        } else {
            if (isSoap){
                if(header.isAlias()){
                    return "com/l7tech/console/resources/include_soap16Alias.png";                    
                }else{
                    return "com/l7tech/console/resources/include_soap16.png";
                }
            }
            else{
                if(header.isAlias()){
                    return "com/l7tech/console/resources/include16Alias.png";                    
                }else{
                    return "com/l7tech/console/resources/include16.png";
                }
            }
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
