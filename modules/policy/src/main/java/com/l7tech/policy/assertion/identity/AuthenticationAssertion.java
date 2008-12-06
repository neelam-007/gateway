/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.identity;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.EntityHeader;

/**
 * Authenticates the credentials against a specified provider, but does not authorize any particular
 * user or group.  Otherwise known as "Wildcard Identity Assertion."
 * @author alex
 */
public class AuthenticationAssertion extends IdentityAssertion {
    private String loggingIdentity;

    public AuthenticationAssertion() {
    }

    public void setIdentityProviderOid(long provider) {
        super.setIdentityProviderOid(provider);
        updateLoggingIdentity();
    }

    private void updateLoggingIdentity() {
        long poid = getIdentityProviderOid();
        if (-1 == poid)
            loggingIdentity = "default identity provider";
        else
            loggingIdentity = "identity provider ID " + poid;
    }

    public String loggingIdentity() {
        return loggingIdentity;
    }

    @Migration(mapName = MigrationMappingSelection.REQUIRED)
    @Override
    public long getIdentityProviderOid() {
        return super.getIdentityProviderOid();
    }

    @Migration(mapName = MigrationMappingSelection.REQUIRED)
    public EntityHeader[] getEntitiesUsed() {
        return super.getEntitiesUsed();
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.LONG_NAME, "Authenticate requestor credentials using a selected Identity Provider");        
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/user16.png");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.AddAuthenticationAssertionAdvice");
        meta.put(AssertionMetadata.POLICY_NODE_CLASSNAME, "com.l7tech.console.tree.policy.AuthenticationAssertionPolicyNode");
        return meta;
    }
}
