/*
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.SetsVariables;
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

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata("kerberos.realm", false, false, "kerberos.realm", false, DataType.STRING),
        };
    }

    public static final String SCHEME = "Negotiate";
}
