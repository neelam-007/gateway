package com.l7tech.external.assertions.jwt;


import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwk.*;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.JoseException;

import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public final class JwtUtils {

    public static SsgKeyEntry getKeyFromStore(final SsgKeyStoreManager ssgKeyStoreManager, final Audit audit, final Goid goid, final String alias) {
        try {
            final SsgKeyEntry ssgKeyEntry = ssgKeyStoreManager.findByPrimaryKey(goid).getCertificateChain(alias);
            return ssgKeyEntry;
        } catch (FindException e) {
            audit.logAndAudit(AssertionMessages.JWT_PRIVATE_KEY_NOT_FOUND);
        } catch (KeyStoreException e) {
            audit.logAndAudit(AssertionMessages.JWT_KEYSTORE_ERROR);
        }
        return null;
    }

    public static X509Certificate getPublicKeyFromPem(final Audit audit, final String pem) {
        try {
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(pem.getBytes()));
        } catch (CertificateException e) {
            audit.logAndAudit(AssertionMessages.JWT_JWE_PUBLIC_KEY_ERROR);
        }
        return null;
    }

    public static Key getKeyFromJWK(final Audit audit, final String json, final boolean getPrivate) {
        try {
            final org.jose4j.jwk.JsonWebKey jwk = org.jose4j.jwk.JsonWebKey.Factory.newJwk(json);
            if (getPrivate) {
                return getPrivateKey(audit, jwk);
            }
            return jwk.getKey();
        } catch (JoseException e) {
            audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, e.getMessage());
        }
        return null;
    }

    public static Key getKeyFromJWKS(final Audit audit, final JsonWebStructure jwt, final String json, final String kid, final boolean getPrivateKey) {
        try {
            final JsonWebKeySet jwks = new JsonWebKeySet(json);
            final String keyId = kid == null || kid.trim().isEmpty() ? jwt.getKeyIdHeaderValue() : kid;
            String usage = null;
            String keyType = null;
            if (jwt instanceof JsonWebSignature) {
                //no need to set 'usage' here as 'use' is use to convey public key usage
                //since we sign, we need private key
                keyType = ((JsonWebSignature) jwt).getKeyType();
            } else if (jwt instanceof JsonWebEncryption) {
                usage = Use.ENCRYPTION;
                keyType = ((JsonWebEncryption) jwt).getKeyManagementModeAlgorithm().getKeyType();
            }
            List<org.jose4j.jwk.JsonWebKey> found = jwks.findJsonWebKeys(keyId, keyType, usage, null);
            if(found.isEmpty()) return null;
            if(getPrivateKey){
                return getPrivateKey(audit, found.get(0));
            }
            return found.get(0).getKey();
        } catch (JoseException e) {
            audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, e.getMessage());
        }
        return null;
    }

    private static Key getPrivateKey(final Audit audit, final org.jose4j.jwk.JsonWebKey jwk) {
        if (jwk instanceof RsaJsonWebKey) {
            return ((RsaJsonWebKey) jwk).getRsaPrivateKey();
        } else if (jwk instanceof EllipticCurveJsonWebKey) {
            return ((EllipticCurveJsonWebKey) jwk).getEcPrivateKey();
        } else if (jwk instanceof OctetSequenceJsonWebKey) {
            return jwk.getKey();
        }
        //should never be
        audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Unknown key algorithm found.");
        return null;
    }
}
