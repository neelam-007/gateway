/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

/**
 * @author alex
 * @version $Revision$
 */
public class WssBasic extends WssCredentialSourceAssertion {

    final static String baseName = "Require WS-Security UsernameToken Profile Credentials";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WssCredentialSourceAssertion>(){
        @Override
        public String getAssertionName( final WssCredentialSourceAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "The requestor must provide BASIC credentials in a WSS Username Token");
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/authentication.gif");

        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        
        return meta;
    }
}
