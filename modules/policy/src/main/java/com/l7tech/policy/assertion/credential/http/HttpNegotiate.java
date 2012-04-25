/*
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;

/**
 * Secure invoker for SPNEGO / Negotiate (transport level Kerberos).
 *
 * <p>Note that we do not support NTLM, only Kerberos.</p>
 *
 * <p>For background see http://en.wikipedia.org/wiki/SPNEGO</p>
 *
 * @author $Author$
 */
public class HttpNegotiate extends HttpCredentialSourceAssertion implements SetsVariables {

    public static final String KERBEROS_REALM = "kerberos.realm";
    public static final String KERBEROS_DATA = "kerberos.data";

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(KERBEROS_REALM, false, false, KERBEROS_REALM, false, DataType.STRING),
            new VariableMetadata(KERBEROS_DATA, false, false, KERBEROS_DATA, false, DataType.BINARY)
        };
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});

        meta.put(AssertionMetadata.SHORT_NAME, "Require Windows Integrated Authentication Credentials");
        meta.put(AssertionMetadata.DESCRIPTION, "The requestor must provide credentials using Integrated Windows authentication");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

        meta.putNull(AssertionMetadata.PROPERTIES_ACTION_FACTORY);
        return meta;
    }

    public static final String SCHEME = "Negotiate";
}
