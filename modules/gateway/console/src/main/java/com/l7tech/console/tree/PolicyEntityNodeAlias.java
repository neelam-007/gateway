package com.l7tech.console.tree;

import com.l7tech.console.util.EntitySaver;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.console.action.*;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

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
        final PolicyAlias alias = getAlias();
        if (alias != null) {
            actions.add(new ConfigureSecurityZoneAction<PolicyAlias>(alias, new EntitySaver<PolicyAlias>() {
                @Override
                public PolicyAlias saveEntity(@NotNull final PolicyAlias entity) throws SaveException {
                    final long oid = Registry.getDefault().getPolicyAdmin().saveAlias(entity);
                    entity.setOid(oid);
                    return entity;
                }
            }));
        }
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

    private PolicyAlias getAlias() {
        PolicyAlias alias = null;
        final PolicyHeader header = getEntityHeader();
        try {
            alias = Registry.getDefault().getPolicyAdmin().findAliasByEntityAndFolder(header.getOid(), header.getFolderOid());
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve policy alias: " + e.getMessage(), ExceptionUtils.getDebugException(e));
        }
        return alias;
    }
}
