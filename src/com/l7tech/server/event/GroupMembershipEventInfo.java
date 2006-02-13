/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.identity.*;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedGroupMembership;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.DeletedEntity;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.server.identity.IdentityProviderFactory;
import org.springframework.context.ApplicationContext;

/**
 * @author alex
 * @version $Revision$
 */
public class GroupMembershipEventInfo {
    public GroupMembershipEventInfo(final GroupMembership gm, String verb, ApplicationContext springContext) {
        long providerOid;
        Class userClass, groupClass;
        if ((gm instanceof FederatedGroupMembership)) {
            providerOid = gm.getThisGroupProviderOid();
            userClass = FederatedUser.class;
            groupClass = FederatedGroup.class;
        } else {
            providerOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
            userClass = InternalUser.class;
            groupClass = InternalGroup.class;
        }

        try {
            NamedEntity g = null;
            NamedEntity u = null;
            IdentityProviderFactory ipf = (IdentityProviderFactory)springContext.getBean("identityProviderFactory");
            IdentityProvider provider = ipf.getProvider(providerOid);
            if (provider != null) {
                g = (PersistentGroup)provider.getGroupManager().findByPrimaryKey(gm.getThisGroupId());
                u = (PersistentUser)provider.getUserManager().findByPrimaryKey(gm.getMemberUserId());
            }

            if (g == null) g = new DeletedEntity(groupClass, gm.getThisGroupId());
            if (u == null) u = new DeletedEntity(userClass, gm.getMemberUserId());
            this.group = g;
            String name = u.getName();
            if (name == null && u instanceof User) {
                name = ((User)u).getLogin();
                if (name == null) name = ((User)u).getEmail();
            }
            this.note = "user #" + gm.getMemberUserId() + " (" + name + ") " + verb;
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    public Entity getGroup() {
        return group;
    }

    public String getNote() {
        return note;
    }

    private final Entity group;
    private final String note;
}
