/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.console.action.*;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;

public class ServiceNodeAlias extends ServiceNode{
    public ServiceNodeAlias(ServiceHeader e) throws IllegalArgumentException {
        super(e);
    }

    public ServiceNodeAlias(ServiceHeader e, Comparator c) {
        super(e, c);
    }

    @Override
    public Action[] getActions() {
        final Collection<Action> actions = new ArrayList<Action>();

        actions.add(new EditPolicyAction(this));
        if (getEntityHeader().isSoap() && !TopComponents.getInstance().isApplet()) actions.add(new PublishPolicyToUDDIRegistry(this));
        actions.add(new DeleteServiceAliasAction(this));
        actions.add(new PolicyRevisionsAction(this));
        actions.add(new RefreshTreeNodeAction(this));
        Action secureCut = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.CUT);
        if(secureCut != null){
            actions.add(secureCut);
        }
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public String getName() {
        return getEntityHeader().getDisplayName()+" alias"; 
    }

    @Override
    protected String iconResource(boolean open) {
        ServiceHeader header = getEntityHeader();
        if (header == null) {
            return "com/l7tech/console/resources/services_disabled16.png";
        }
        else if (header.isDisabled()) {
            if (!header.isSoap()) {
                return "com/l7tech/console/resources/xmlObject_disabled16Alias.png";
            } else {
                return "com/l7tech/console/resources/services_disabled16Alias.png";
            }
        } else {
            if(header.isSoap()) {
                return "com/l7tech/console/resources/services16Alias.png";
            } else {
                return "com/l7tech/console/resources/xmlObject16Alias.gif";
            }
        }
    }
}
