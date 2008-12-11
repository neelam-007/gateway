/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.ValidationException;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.CertEntryRow;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.security.auth.x500.X500Principal;
import java.util.TreeSet;
import java.util.List;
import java.security.cert.X509Certificate;
import java.math.BigInteger;

/**
 * Provides functionality shared by internal and federated providers.
 * 
 * @param <UT> the type of user found in this provider
 * @param <GT> the type of group found in this provider
 * @param <UMT> the type of UserManager provided by this provider
 * @param <GMT> the type of GroupManager provided by this provider
 */
public abstract class PersistentIdentityProviderImpl<UT extends PersistentUser, GT extends PersistentGroup, UMT extends PersistentUserManager<UT>, GMT extends PersistentGroupManager<UT, GT>>
        implements ApplicationContextAware, InitializingBean, PersistentIdentityProvider<UT, GT, UMT, GMT>
{
    protected ApplicationContext applicationContext;
    protected ClientCertManager clientCertManager;

    /**
     * searches for users and groups whose name (cn) match the pattern described in searchString
     * pattern may include wildcard such as * character. result is sorted by name property
     *
     * todo: (once we dont use hibernate?) replace this by one union sql query and have the results sorted
     * instead of sorting in collection.
     */
    public EntityHeaderSet<IdentityHeader> search(EntityType[] types, String searchString) throws FindException {
        if (types == null || types.length < 1) throw new IllegalArgumentException("must pass at least one type");
        boolean wantUsers = false;
        boolean wantGroups = false;
        for (EntityType type : types) {
            if (type == EntityType.USER) wantUsers = true;
            else if (type == EntityType.GROUP) wantGroups = true;
        }
        if (!wantUsers && !wantGroups) throw new IllegalArgumentException("types must contain users and or groups");
        EntityHeaderSet<IdentityHeader> searchResults = new EntityHeaderSet<IdentityHeader>(new TreeSet<IdentityHeader>());
        if (wantUsers) searchResults.addAll(getUserManager().search(searchString));
        if (wantGroups) searchResults.addAll(getGroupManager().search(searchString));
        return searchResults;
    }

    public EntityHeaderSet<IdentityHeader> search(boolean users, boolean groups, IdentityMapping mapping, Object value) throws FindException {
        throw new UnsupportedOperationException();
    }

    public void validate(UT u) throws ValidationException {
        throw new ValidationException("Validation failed for user '"+u.getLogin()+"' (not supported).");
    }

    public void setClientCertManager(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called after population of this instance's bean properties.
     *
     * @throws Exception if initialization fails
     */
    public void afterPropertiesSet() throws Exception {
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public X509Certificate findCertByIssuerAndSerial(X500Principal issuer, BigInteger serial) throws FindException {
        final long providerOid = getConfig().getOid();
        List<CertEntryRow> rows = clientCertManager.findByIssuerAndSerial(issuer, serial);
        X509Certificate got = null;
        for (CertEntryRow row : rows) {
            if (row.getProvider() == providerOid) {
                if (got != null) throw new FindException("Found multiple matching certificates");
                got = row.getCertificate();
            }
        }
        return got;
    }
}
