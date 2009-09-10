package com.l7tech.security.xml;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.PrivateKey;
import java.util.Collection;

/**
 * Wraps an existing token resolver to look up new stuff
 */
public class WrapSSTR extends DelegatingSecurityTokenResolver {
    public WrapSSTR(X509Certificate cert, PrivateKey key, SecurityTokenResolver rest) throws CertificateEncodingException {
        super(rest == null 
        ? new SecurityTokenResolver[] { new SimpleSecurityTokenResolver(cert, key) }
        : new SecurityTokenResolver[] { new SimpleSecurityTokenResolver(cert, key), rest });
    }

    public void addCerts(X509Certificate[] newcerts) throws CertificateEncodingException {
        Collection<SecurityTokenResolver> resolvers = getDelegates();
        for (SecurityTokenResolver resolver : resolvers) {
            if (resolver instanceof SimpleSecurityTokenResolver) {
                SimpleSecurityTokenResolver simpleResolver = (SimpleSecurityTokenResolver) resolver;
                simpleResolver.addCerts(newcerts);
                return;
            }
        }
    }

    public WrapSSTR(X509Certificate cert, PrivateKey key) throws CertificateEncodingException {
        super(new SimpleSecurityTokenResolver(cert, key));
    }
}
