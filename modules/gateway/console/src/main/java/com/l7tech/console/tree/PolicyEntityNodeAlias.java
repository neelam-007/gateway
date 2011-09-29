package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.console.action.*;

import javax.swing.*;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;

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
        Action secureCopy = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.COPY, EntityType.POLICY_ALIAS);
        if(secureCut != null){
            actions.add(secureCut);
        }
        if(secureCopy != null){
            actions.add(secureCopy);
        }

        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public String getName() {
        return super.getName()+" alias";
    }
}
