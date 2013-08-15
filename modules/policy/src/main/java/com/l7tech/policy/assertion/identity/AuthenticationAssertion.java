/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.identity;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
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

    @Override
    public void setIdentityProviderOid(Goid provider) {
        super.setIdentityProviderOid(provider);
        updateLoggingIdentity();
    }

    private void updateLoggingIdentity() {
        Goid poid = getIdentityProviderOid();
        if ( poid == null)
            loggingIdentity = "default identity provider";
        else
            loggingIdentity = "identity provider ID " + poid;
    }

    @Override
    public String loggingIdentity() {
        return loggingIdentity;
    }

    @Override
    public Goid getIdentityProviderOid() {
        return super.getIdentityProviderOid();
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        return super.getEntitiesUsed();
    }

    @Override
    public IdentityTarget getIdentityTarget() {
        return new IdentityTarget(IdentityTarget.TargetIdentityType.PROVIDER, getIdentityProviderOid());
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Authenticate Against Identity Provider");
        meta.put(AssertionMetadata.DESCRIPTION, "Authenticate requestor credentials using a selected Identity Provider");        
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/user16.png");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.AddAuthenticationAssertionAdvice");
        meta.put(AssertionMetadata.POLICY_NODE_CLASSNAME, "com.l7tech.console.tree.policy.AuthenticationAssertionPolicyNode");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Change Authentication Identity Provider");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Properties16.gif");

        //Change Authentication Identity Provider

        return meta;
    }
}
