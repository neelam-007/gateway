package com.l7tech.server;

import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.token.KerberosSigningSecurityToken;

import javax.security.auth.x500.X500Principal;
import java.util.*;
import java.security.cert.X509Certificate;
import java.math.BigInteger;

/**
 * SecurityTokenResolver that delegates to a collection of resolvers.
 */
public class DelegatingSecurityTokenResolver implements SecurityTokenResolver {

    //- PUBLIC

    public DelegatingSecurityTokenResolver( final Collection<SecurityTokenResolver> resolvers ){
        this.resolvers = Collections.unmodifiableCollection( new ArrayList<SecurityTokenResolver>(resolvers) );
    }

    @Override
    public KerberosSigningSecurityToken getKerberosTokenBySha1( final String kerberosSha1 ) {
        KerberosSigningSecurityToken token = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            token = resolver.getKerberosTokenBySha1( kerberosSha1 );
            if ( token != null ) break;
        }
        return token;
    }

    @Override
    public byte[] getSecretKeyByEncryptedKeySha1( final String encryptedKeySha1 ) {
        byte[] secretKey = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            secretKey = resolver.getSecretKeyByEncryptedKeySha1( encryptedKeySha1 );
            if ( secretKey != null ) break;
        }
        return secretKey;
    }

    @Override
    public X509Certificate lookup( final String thumbprint ) {
        X509Certificate certificate = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            certificate = resolver.lookup( thumbprint );
            if ( certificate != null ) break;
        }
        return certificate;
    }

    @Override
    public X509Certificate lookupByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
        X509Certificate certificate = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            certificate = resolver.lookupByIssuerAndSerial( issuer, serial );
            if ( certificate != null ) break;
        }
        return certificate;
    }

    @Override
    public X509Certificate lookupByKeyName( final String keyName ) {
        X509Certificate certificate = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            certificate = resolver.lookupByKeyName( keyName );
            if ( certificate != null ) break;
        }
        return certificate;
    }

    @Override
    public X509Certificate lookupBySki( final String ski ) {
        X509Certificate certificate = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            certificate = resolver.lookupBySki( ski );
            if ( certificate != null ) break;
        }
        return certificate;
    }

    @Override
    public SignerInfo lookupPrivateKeyByCert( final X509Certificate cert ) {
        SignerInfo signerInfo = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            signerInfo = resolver.lookupPrivateKeyByCert( cert );
            if ( signerInfo != null ) break;
        }
        return signerInfo;
    }

    @Override
    public SignerInfo lookupPrivateKeyByKeyName( final String keyName ) {
        SignerInfo signerInfo = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            signerInfo = resolver.lookupPrivateKeyByKeyName( keyName );
            if ( signerInfo != null ) break;
        }
        return signerInfo;
    }

    @Override
    public SignerInfo lookupPrivateKeyBySki( final String ski ) {
        SignerInfo signerInfo = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            signerInfo = resolver.lookupPrivateKeyBySki( ski );
            if ( signerInfo != null ) break;
        }
        return signerInfo;
    }

    @Override
    public SignerInfo lookupPrivateKeyByX509Thumbprint( final String thumbprint ) {
        SignerInfo signerInfo = null;
        for ( SecurityTokenResolver resolver : resolvers ) {
            signerInfo = resolver.lookupPrivateKeyByX509Thumbprint( thumbprint );
            if ( signerInfo != null ) break;
        }
        return signerInfo;
    }

    @Override
    public void putSecretKeyByEncryptedKeySha1( final String encryptedKeySha1, final byte[] secretKey ) {
        for ( SecurityTokenResolver resolver : resolvers ) {
            resolver.putSecretKeyByEncryptedKeySha1( encryptedKeySha1, secretKey );
        }
    }

    //- PRIVATE

    private final Collection<SecurityTokenResolver> resolvers;
}
