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
import com.l7tech.identity.internal.GroupMembership;
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
            providerOid = ((FederatedGroupMembership)gm).getProviderOid();
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
                g = (PersistentGroup)provider.getGroupManager().findByPrimaryKey(Long.toString(gm.getGroupOid()));
                u = (PersistentUser)provider.getUserManager().findByPrimaryKey(Long.toString(gm.getUserOid()));
            }

            if (g == null) g = new DeletedEntity(groupClass, gm.getGroupOid());
            if (u == null) u = new DeletedEntity(userClass, gm.getUserOid());
            this.group = g;
            String name = u.getName();
            if (name == null && u instanceof User) {
                name = ((User)u).getLogin();
                if (name == null) name = ((User)u).getEmail();
            }
            this.note = "user #" + gm.getUserOid() + " (" + name + ") " + verb;
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
