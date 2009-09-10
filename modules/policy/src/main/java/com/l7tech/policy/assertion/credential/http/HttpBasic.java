/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

public class HttpBasic extends HttpCredentialSourceAssertion {

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Require HTTP Basic Credentials");
        meta.put(AssertionMetadata.LONG_NAME, "The requestor must provide credentials using the HTTP BASIC authentication method.");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/authentication.gif");

        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        
        return meta;
    }
}
