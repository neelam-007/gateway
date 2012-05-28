package com.l7tech.ntlm.protocol;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class PasswordCredential implements NtlmCredential {
    private char[] password = null;
    private NtlmSecurityPrincipal principalNtlm;

    private String host;

    public PasswordCredential(String acctname, char[] password)
            throws AuthenticationManagerException {
        this(null, null, acctname, password);
    }

    public PasswordCredential(String host, String domain, String acctname, char[] password)
            throws AuthenticationManagerException {
        this.host = host;
        this.principalNtlm = new NtlmSecurityPrincipal(domain, acctname);
        if (password != null) {
            this.password = new char[password.length];
            System.arraycopy(password, 0, this.password, 0, password.length);
        }
    }

    public NtlmSecurityPrincipal getSecurityPrincipal() {
        return this.principalNtlm;
    }

    public char[] getPassword() {
        return this.password;
    }

    public String getHost() {
        return host;
    }

    public String toString() {
        return this.principalNtlm.getName();
    }
}
