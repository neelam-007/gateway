package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;

/**
 * Support class for server assertions that perform encryption.
 */
public abstract class ServerAddWssEncryption<AT extends Assertion & MessageTargetable & SecurityHeaderAddressable & IdentityTargetable> extends AbstractMessageTargetableServerAssertion<AT> {

    //- PUBLIC

    public ServerAddWssEncryption( final AT assertion ) {
        super( assertion );
        this.addWssEncryptionSupport = new AddWssEncryptionSupport(getAuditHaver(), logger, assertion, assertion, assertion);
    }

    //- PROTECTED

    protected AddWssEncryptionContext buildEncryptionContext(PolicyEnforcementContext context) throws AddWssEncryptionSupport.MultipleTokensException, PolicyAssertionException {
        return addWssEncryptionSupport.buildEncryptionContext(context);
    }

    protected void applyDecorationRequirements( final PolicyEnforcementContext policyEnforcementContext,
                                                final DecorationRequirements wssReq,
                                                final AddWssEncryptionContext encryptionContext)
    {
        addWssEncryptionSupport.applyDecorationRequirements(policyEnforcementContext, wssReq, encryptionContext, this);
    }

    protected final AddWssEncryptionSupport addWssEncryptionSupport;
}
