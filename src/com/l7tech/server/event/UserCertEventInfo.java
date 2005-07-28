/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.DeletedEntity;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.cert.CertEntryRow;
import org.springframework.context.ApplicationContext;

/**
 * @author alex
 * @version $Revision$
 */
public class UserCertEventInfo {
    public UserCertEventInfo(final CertEntryRow cer, String verb, EntityChangeSet changes, ApplicationContext springContext) {
        try {
            NamedEntity u = null;
            IdentityProviderFactory ipf = (IdentityProviderFactory)springContext.getBean("identityProviderFactory");
            IdentityProvider provider = ipf.getProvider(cer.getProvider());
            u = (PersistentUser)provider.getUserManager().findByPrimaryKey(cer.getUserId());

            if (u == null) u = new DeletedEntity(User.class, cer.getUserId());
            this.user = u;

            String note = verb;
            if (changes != null) {
                Object ocert = changes.getOldValue("cert");
                Object ncert = changes.getNewValue("cert");

                if (ocert == null && ncert != null) {
                    note = "set certificate";
                } else if (ncert == null && ocert != null) {
                    note = "cleared certificate";
                } else if (ncert != null && !ncert.equals(ocert)) {
                    note = "updated certificate";
                }
            }
            this.note = note;
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    public Entity getUser() {
        return user;
    }

    public String getNote() {
        return note;
    }

    private final Entity user;
    private final String note;
}
