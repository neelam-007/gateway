package com.l7tech.policy.assertion.credential.http;

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
public class HttpNegotiate extends HttpCredentialSourceAssertion {
    public String scheme() {
        return HttpNegotiate.SCHEME;
    }

    public static final String SCHEME = "Negotiate";
}
