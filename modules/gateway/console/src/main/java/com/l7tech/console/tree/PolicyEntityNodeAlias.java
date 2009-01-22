/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.action.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;

import javax.swing.*;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Level;

public class PolicyEntityNodeAlias extends PolicyEntityNode{
    public PolicyEntityNodeAlias(PolicyHeader e) {
        super(e);
    }

    public PolicyEntityNodeAlias(PolicyHeader e, Comparator c) {
        super(e, c);
    }

    @Override
    public Action[] getActions() {
        Collection<Action> actions = new ArrayList<Action>();
        actions.add(new EditPolicyAction(this));
        actions.add(new DeletePolicyAliasAction(this));
        actions.add(new PolicyRevisionsAction(this));
        actions.add(new RefreshTreeNodeAction(this));

        Action secureCut = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.CUT);
        if(secureCut != null){
            actions.add(secureCut);
        }

        return actions.toArray(new Action[0]);
    }

    @Override
    public String getName() {
        return getEntityHeader().getName()+" alias";
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
                return "com/l7tech/console/resources/include_internalsoap16Alias.png";
            }
            else{
                return "com/l7tech/console/resources/include_internal16Alias.png";
            }
        } else {
            if (isSoap){
                return "com/l7tech/console/resources/include_soap16Alias.png";
            }
            else{
                return "com/l7tech/console/resources/include16Alias.png";
            }
        }

    }
}
