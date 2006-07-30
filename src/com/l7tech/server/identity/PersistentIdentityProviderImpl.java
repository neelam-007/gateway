/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.objectmodel.EntityHeaderComparator;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.server.KeystoreUtils;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.TreeSet;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistentIdentityProviderImpl<UT extends PersistentUser, GT extends PersistentGroup, UMT extends PersistentUserManager<UT>, GMT extends PersistentGroupManager<UT, GT>>
        implements InitializingBean, PersistentIdentityProvider<UT, GT, UMT, GMT>
{
    protected ClientCertManager clientCertManager;
    protected KeystoreUtils keystore;

    /**
     * searches for users and groups whose name (cn) match the pattern described in searchString
     * pattern may include wildcard such as * character. result is sorted by name property
     *
     * todo: (once we dont use hibernate?) replace this by one union sql query and have the results sorted
     * instead of sorting in collection.
     */
    public Collection<IdentityHeader> search(EntityType[] types, String searchString) throws FindException {
        if (types == null || types.length < 1) throw new IllegalArgumentException("must pass at least one type");
        boolean wantUsers = false;
        boolean wantGroups = false;
        for (EntityType type : types) {
            if (type == EntityType.USER) wantUsers = true;
            else if (type == EntityType.GROUP) wantGroups = true;
        }
        if (!wantUsers && !wantGroups) throw new IllegalArgumentException("types must contain users and or groups");
        Collection<IdentityHeader> searchResults = new TreeSet<IdentityHeader>(new EntityHeaderComparator());
        if (wantUsers) searchResults.addAll(getUserManager().search(searchString));
        if (wantGroups) searchResults.addAll(getGroupManager().search(searchString));
        return searchResults;
    }

    public Collection<IdentityHeader> search(boolean users, boolean groups, IdentityMapping mapping, Object value) throws FindException {
        throw new UnsupportedOperationException();
    }

    public void setClientCertManager(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    public void setKeystore(KeystoreUtils keystore) {
        this.keystore = keystore;
    }

    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called after population of this instance's bean properties.
     *
     * @throws Exception if initialization fails
     */
    public void afterPropertiesSet() throws Exception {
        if (clientCertManager == null) {
            throw new IllegalArgumentException("The Client Certificate Manager is required");
        }
    }
}
