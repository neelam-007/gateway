/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.console.action.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.EntityType;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Comparator;
import java.util.logging.Level;

/** @author alex */
public class PolicyEntityNode extends EntityWithPolicyNode<Policy, PolicyHeader>{
    protected volatile Policy policy;

    public PolicyEntityNode(PolicyHeader e) {
        this(e, null);
    }

    public PolicyEntityNode(PolicyHeader e, Comparator c) {
        super(e, c);
    }

    @Override
    public void updateUserObject() throws FindException{
        policy = null;
        getPolicy();
    }
    
    public Policy getPolicy() throws FindException {
        if (policy != null) return policy;

        PolicyHeader eh = getEntityHeader();
        Policy updatedPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(eh.getGuid());
        updatedPolicy.setAlias(eh.isAlias());
        if(eh.isAlias()){
            //Adjust it's folder property
            updatedPolicy.setFolderOid(eh.getFolderOid());
        }

        // throw something if null, the service may have been deleted
        if (updatedPolicy == null) {
            TopComponents creg = TopComponents.getInstance();
            JTree tree = (JTree)creg.getComponent(ServicesAndPoliciesTree.NAME);
            if (tree !=null) {
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                Enumeration kids = this.getParent().children();
                while (kids.hasMoreElements()) {
                    TreeNode node = (TreeNode) kids.nextElement();
                    if (node == this) {
                        model.removeNodeFromParent(this);
                        break;
                    }
                }
            }
            throw new FindException("The policy for '"+eh.getName()+"' does not exist any more.");
        }

        PolicyHeader newEh = new PolicyHeader(updatedPolicy);
        setUserObject(newEh);
        firePropertyChange(this, "UserObject", eh, newEh);

        this.policy = updatedPolicy;
        return this.policy;
    }

    public boolean isAlias() {
        PolicyHeader policyHeader = (PolicyHeader) this.getUserObject();
        return policyHeader.isAlias();
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
        if(!isAlias()) actions.add(new MarkEntityToAliasAction(this));
        actions.add(new PolicyRevisionsAction(this));
        actions.add(new RefreshTreeNodeAction(this));
        
        Action secureCut = ServicesAndPoliciesTree.getSecuredAction(EntityType.FOLDER,
                                                                OperationType.UPDATE,
                                                                ServicesAndPoliciesTree.ClipboardActionType.CUT);
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
        if(isAlias()){
            return getEntityHeader().getName()+" alias";
        }
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
}
