package com.l7tech.ntlm.protocol;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public interface NtlmCredential
{
  public NtlmSecurityPrincipal getSecurityPrincipal();

  public void destroy();
}
