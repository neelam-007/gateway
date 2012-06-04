package com.l7tech.ntlm.protocol;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public interface AuthenticationProvider {

    public byte[] processAuthentication(byte[] token) throws AuthenticationManagerException;

    public byte[] requestAuthentication(byte[] token, NtlmCredential creds) throws AuthenticationManagerException;

    public NtlmAuthenticationState getNtlmAuthenticationState();

    public void resetAuthentication();

}
