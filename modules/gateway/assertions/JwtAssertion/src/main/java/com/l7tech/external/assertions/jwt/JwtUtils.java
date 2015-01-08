package com.l7tech.external.assertions.jwt;


import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.ExceptionUtils;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwk.*;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.JoseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public final class JwtUtils {

    public static SsgKeyEntry getKeyFromStore(final DefaultKey defaultKey, final Audit audit, final Goid goid, final String alias) {
        try {
            final SsgKeyEntry ssgKeyEntry = defaultKey.lookupKeyByKeyAlias(alias, goid);
            return ssgKeyEntry;
        } catch (FindException e) {
            audit.logAndAudit(AssertionMessages.JWT_PRIVATE_KEY_NOT_FOUND);
        } catch (KeyStoreException e) {
            audit.logAndAudit(AssertionMessages.JWT_KEYSTORE_ERROR);
        } catch (IOException e) {
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

    public static Key getKeyFromJWK(final Audit audit, final Object json, final boolean getPrivate, final String usage) {
        try {
            final String jsonStr = getJson(audit, json);
            if (jsonStr == null) {
                return null;
            }
            final org.jose4j.jwk.JsonWebKey jwk = org.jose4j.jwk.JsonWebKey.Factory.newJwk(jsonStr);
            if (getPrivate) {
                return getPrivateKey(audit, jwk);
            }
            //want public key --- what's our usage? enc or sig?
            if (!usage.equalsIgnoreCase(jwk.getUse())) {
                audit.logAndAudit(AssertionMessages.JWT_INVALID_KEY, "from jwk", usage);
                return null;
            }
            return jwk.getKey();
        } catch (JoseException e) {
            audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Error parsing JSON: " + ExceptionUtils.getMessage(e.getCause()));
        }
        return null;
    }

    public static Key getKeyFromJWKS(final Audit audit, final JsonWebStructure jwt, final Object json, final String kid, final boolean getPrivateKey) {
        try {
            final String jsonStr = getJson(audit, json);
            if (jsonStr == null) {
                return null;
            }
            //work around for JsonWebKeySet throwing NPE when receiving a JWK instead of a JWKS
            final Map<String,Object> parsed = JsonUtil.parseJson(jsonStr);
            if(parsed.get(JsonWebKeySet.JWK_SET_MEMBER_NAME) == null){
                audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Invalid JWKS, json does not contain the 'keys' member.");
                return null;
            }
            final JsonWebKeySet jwks = new JsonWebKeySet(jsonStr);
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
            if (found.isEmpty()) return null;
            if (getPrivateKey) {
                return getPrivateKey(audit, found.get(0));
            }
            return found.get(0).getKey();
        } catch (JoseException e) {
            audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, ExceptionUtils.getMessage(e.getCause()));
        }
        return null;
    }

    public static String getJson(final Audit audit, final Object json) {
        try {
            //it's a string, attempt to parse it, if it passes, return the string
            if(json instanceof String){
                JsonUtil.parseJson((String)json);
                return (String) json;
            } else if(json instanceof Message){
                Message msg = (Message) json;
                if(msg.getMimeKnob().getOuterContentType().isJson()){
                    try {
                        final String j = msg.getJsonKnob().getJsonData().getJsonData();
                        JsonUtil.parseJson(j);
                        return j;
                    } catch (IOException e) {
                        audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Could not retrieve JSON data.");
                    } catch (InvalidJsonException e) {
                        audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Invalid JSON.");
                    }
                } else {
                    audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Unsupported content type: " + msg.getMimeKnob().getOuterContentType().toString());
                }
            } else {
                audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Unsupported data found.");
            }
        } catch (JoseException e) {
            audit.logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Error parsing json: " + ExceptionUtils.getMessage(e.getCause()));
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
