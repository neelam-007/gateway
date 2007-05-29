package com.l7tech.common.security.xml;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;

/**
 * Wraps an existing token resolver to look up new stuff
 */
public class WrapSSTR extends DelegatingSecurityTokenResolver {
    public WrapSSTR(X509Certificate cert, PrivateKey key, SecurityTokenResolver rest) {
        super(rest == null 
        ? new SecurityTokenResolver[] { new SimpleSecurityTokenResolver(cert, key) }
        : new SecurityTokenResolver[] { new SimpleSecurityTokenResolver(cert, key), rest });
    }

    public WrapSSTR(X509Certificate cert, PrivateKey key) {
        super(new SimpleSecurityTokenResolver(cert, key));
    }
}
