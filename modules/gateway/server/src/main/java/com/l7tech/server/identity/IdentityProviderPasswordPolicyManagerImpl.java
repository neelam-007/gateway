/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.identity.IdentityProviderPasswordPolicyManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateEntityManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This IdentityProviderPasswordPolicyManager is the server side manager who manages the identity
 * password policies.
 *
 * @author flascelles
 */
public class IdentityProviderPasswordPolicyManagerImpl
    extends HibernateEntityManager<IdentityProviderPasswordPolicy, EntityHeader>
    implements IdentityProviderPasswordPolicyManager
{
    private static final Logger logger = Logger.getLogger(IdentityProviderPasswordPolicyManagerImpl.class.getName());

    @Override
    public Class<IdentityProviderPasswordPolicy> getImpClass() {
        return IdentityProviderPasswordPolicy.class;
    }

    @Override
    public Class<IdentityProviderPasswordPolicy> getInterfaceClass() {
        return IdentityProviderPasswordPolicy.class;
    }

    public String getTableName() {
        return "password_policy";
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PASSWORD_POLICY;
    }
                
    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(IdentityProviderPasswordPolicy entity) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put("internalIdentityProviderGoid", entity.getInternalIdentityProviderGoid());
        return Arrays.asList(serviceOidMap);
    }

    @Override
    public IdentityProviderPasswordPolicy findByInternalIdentityProviderOid(Goid identityProviderOid) throws FindException {
        return findByUniqueKey( "internalIdentityProviderGoid", identityProviderOid );
    }
}
