/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.AssertionUtils;

/**
 * @author mike
 */
public class EncryptedUsernameTokenAssertion extends WssBasic implements SecurityHeaderAddressable {

    final static String baseName = "Require Encrypted UsernameToken Profile Credentials";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<EncryptedUsernameTokenAssertion>(){
        @Override
        public String getAssertionName( final EncryptedUsernameTokenAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "The requestor must provide BASIC credentials in a WSS Username Token, encrypted and signed using an ephemeral EncryptedKey");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/authentication.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        
        return meta;
    }
}
