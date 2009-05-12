/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.util.Functions;

/**
 * @author alex
 * @version $Revision$
 */
@ProcessesRequest
public class WssBasic extends WssCredentialSourceAssertion {

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "WSS UsernameToken Basic");
        meta.put(AssertionMetadata.DESCRIPTION, "The requestor must provide BASIC credentials in a WSS Username Token");
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME);
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, WssBasic>() {
            @Override
            public String call( final WssBasic wssBasic ) {
                StringBuilder name = new StringBuilder("Require WSS UsernameToken Basic Authentication");
                name.append(SecurityHeaderAddressableSupport.getActorSuffix(wssBasic));
                return name.toString();
            }
        });

        return meta;
    }
}
