/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

/**
 * @author mike
 */
public class EncryptedUsernameTokenAssertion extends WssBasic implements SecurityHeaderAddressable {

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "Encrypted UsernameToken");
        meta.put(AssertionMetadata.DESCRIPTION, "The requestor must provide BASIC credentials in a WSS Username Token, encrypted and signed using an ephemeral EncryptedKey");
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME, "Require Encrypted UsernameToken Authentication");

        return meta;
    }
}
