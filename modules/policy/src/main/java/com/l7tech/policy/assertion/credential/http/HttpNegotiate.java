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
 * @version $Revision$
 */
public class HttpNegotiate extends HttpCredentialSourceAssertion implements SetsVariables {
    @Override
    public String scheme() {
        return HttpNegotiate.SCHEME;
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata("kerberos.realm", false, false, "kerberos.realm", false, DataType.STRING),
        };
    }

    public static final String SCHEME = "Negotiate";
}
