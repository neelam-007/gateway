/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
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
            IdentityProviderFactory ipf = (IdentityProviderFactory)springContext.getBean("identityProviderFactory");
            IdentityProvider provider = ipf.getProvider(cer.getProvider());

            // For the case: no such identity provider associated with the client certificate
            if (provider == null) {
                throw new FindException("The identity provider associated with the client certificate does not exist.");
            }

            this.user = provider.getUserManager().findByPrimaryKey(cer.getUserId());

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

    public User getUser() {
        return user;
    }

    public String getNote() {
        return note;
    }

    private final User user;
    private final String note;
}
