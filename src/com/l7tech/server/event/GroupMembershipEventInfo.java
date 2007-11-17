/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.event;

import com.l7tech.identity.*;
import com.l7tech.identity.fed.FederatedGroupMembership;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.identity.IdentityProviderFactory;
import org.springframework.context.ApplicationContext;

/**
 * @author alex
 */
public class GroupMembershipEventInfo {
    public GroupMembershipEventInfo(final GroupMembership gm, String verb, ApplicationContext springContext) {
        long providerOid = gm.getThisGroupProviderOid();
        try {
            Group g = null;
            User u = null;
            IdentityProviderFactory ipf = (IdentityProviderFactory)springContext.getBean("identityProviderFactory");
            IdentityProvider provider = ipf.getProvider(providerOid);
            if (provider != null) {
                g = provider.getGroupManager().findByPrimaryKey(gm.getThisGroupId());
                u = provider.getUserManager().findByPrimaryKey(gm.getMemberUserId());
            }

            if (g == null) g = new AnonymousGroupReference(gm.getThisGroupId(), gm.getThisGroupProviderOid(), null);
            if (u == null) u = new AnonymousUserReference(gm.getMemberUserId(), gm.getThisGroupProviderOid(), null);
            this.group = g;
            String name = u.getName();
            if (name == null) {
                name = u.getLogin();
                if (name == null) name = u.getEmail();
            }
            this.note = "user #" + gm.getMemberUserId() + " (" + name + ") " + verb;
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    public Group getGroup() {
        return group;
    }

    public String getNote() {
        return note;
    }

    private final Group group;
    private final String note;
}
