package com.l7tech.external.assertions.jwt.server;


import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.jwt.EncodeJsonWebTokenAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.external.assertions.jwt.JwtUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.server.DefaultKeyCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.ContextVariableUtils;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwk.Use;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.AesKey;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.InvalidAlgorithmException;
import org.jose4j.lang.InvalidKeyException;
import org.jose4j.lang.JoseException;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.io.IOException;
import java.security.Key;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;


public class ServerEncodeJsonWebTokenAssertion extends AbstractServerAssertion<EncodeJsonWebTokenAssertion> {

    @Inject
    private DefaultKeyCache defaultKey;

    private final boolean singleSignatureSecretVarKeyExpr;
    private final boolean singleEncryptionSecretVarKeyExpr;

    public ServerEncodeJsonWebTokenAssertion(@NotNull EncodeJsonWebTokenAssertion assertion) {
        super(assertion);
        singleSignatureSecretVarKeyExpr = assertion.getSignatureSecretKey() != null
                && Syntax.isOnlyASingleVariableReferenced(assertion.getSignatureSecretKey());
        singleEncryptionSecretVarKeyExpr = assertion.getEncryptionSecret() != null
                && Syntax.isOnlyASingleVariableReferenced(assertion.getEncryptionSecret());
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map<String, Object> variables = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
        //find the payload
        final String sourcePayload = ExpandVariables.process(assertion.getSourceVariable(), variables, getAudit(), false);
        if (sourcePayload == null || sourcePayload.trim().isEmpty()) {
            logAndAudit(AssertionMessages.JWT_MISSING_SOURCE_PAYLOAD);
            return AssertionStatus.FAILED;
        }
        //determine what to do with header and possibly get header
        String sourceHeaders = null;
        if (!JsonWebTokenConstants.HEADERS_USE_DEFAULT.equals(assertion.getHeaderAction())) {
            final Object o = ExpandVariables.processSingleVariableAsObject(assertion.getSourceHeaders(), variables, getAudit(), false);
            sourceHeaders = JwtUtils.getJson(getAudit(), o);
            if (sourceHeaders == null || sourceHeaders.trim().isEmpty()) {
                logAndAudit(AssertionMessages.JWT_MISSING_HEADERS, assertion.getHeaderAction());
                return AssertionStatus.FAILED;
            }
        }

        String jwsCompact = null;
        if (assertion.isSignPayload()) {
            try {
                final JsonWebSignature jws = new JsonWebSignature();
                jws.setHeader(HeaderParameterNames.TYPE, "JWT");
                jws.setPayload(sourcePayload);
                jws.setAlgorithmHeaderValue(assertion.getSignatureAlgorithm());
                final Key signingKey = getSigningKey(jws, context);
                if (signingKey == null) {
                    logAndAudit(AssertionMessages.JWT_JWS_KEY_ERROR);
                    return AssertionStatus.FAILED;
                }
                jws.setKey(signingKey);
                //we don't have an encryption operation...we're done...set context vars and what not
                if (!assertion.isEncryptPayload()) {
                    AssertionStatus status = handleHeaders(jws, sourceHeaders);
                    if (!AssertionStatus.NONE.equals(status)) {
                        return status;
                    }
                }
                jwsCompact = jws.getCompactSerialization();
                context.setVariable(assertion.getTargetVariable() + ".compact", jwsCompact);
            } catch (InvalidKeyException e) {
                logAndAudit(AssertionMessages.JWT_INVALID_KEY_USAGE, e.getMessage());
                return AssertionStatus.FAILED;
            } catch (JoseException e) {
                logAndAudit(AssertionMessages.JWT_JOSE_ERROR, e.getMessage());
                return AssertionStatus.FAILED;
            }
        }
        if (assertion.isEncryptPayload()) {
            final JsonWebEncryption jwe = new JsonWebEncryption();
            //type is different from CTY
            jwe.setHeader(HeaderParameterNames.TYPE, "JWT");
            if (jwsCompact != null) {
                jwe.setHeader(HeaderParameterNames.CONTENT_TYPE, "JWT");
                jwe.setPayload(jwsCompact);
            } else {
                jwe.setPayload(sourcePayload);
            }
            jwe.setAlgorithmHeaderValue(assertion.getKeyManagementAlgorithm());
            jwe.setEncryptionMethodHeaderParameter(assertion.getContentEncryptionAlgorithm());
            final Key encryptionKey = getEncryptionKey(jwe, context);
            if (encryptionKey == null) {
                logAndAudit(AssertionMessages.JWT_JWE_KEY_ERROR);
                return AssertionStatus.FAILED;
            }
            jwe.setKey(encryptionKey);
            try {
                AssertionStatus status = handleHeaders(jwe, sourceHeaders);
                if (!AssertionStatus.NONE.equals(status)) {
                    return status;
                }
                final String jweCompact = jwe.getCompactSerialization();
                context.setVariable(assertion.getTargetVariable() + ".compact", jweCompact);
            } catch (InvalidKeyException e) {
                logAndAudit(AssertionMessages.JWT_INVALID_KEY_USAGE, e.getMessage());
                return AssertionStatus.FAILED;
            } catch (JoseException e) {
                logAndAudit(AssertionMessages.JWT_JOSE_ERROR, e.getMessage());
                return AssertionStatus.FAILED;
            } catch(NullPointerException e){
                //catch NPE cause underlying api throws NPE when it encounter an unsupported key type
                logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Unsupported Key Type");
                return AssertionStatus.FAILED;
            }
        }
        //create unsecure
        if (!assertion.isSignPayload() && !assertion.isEncryptPayload()) {
            try {
                final JsonWebSignature str = new JsonWebSignature();
                str.setHeader(HeaderParameterNames.TYPE, "JWT");
                str.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS);
                str.setHeader(HeaderParameterNames.ALGORITHM, AlgorithmIdentifiers.NONE);
                str.setPayload(sourcePayload);
                AssertionStatus status = handleHeaders(str, sourceHeaders);
                if (!AssertionStatus.NONE.equals(status)) {
                    return status;
                }
                context.setVariable(assertion.getTargetVariable() + ".compact", str.getCompactSerialization());
            } catch (JoseException e) {
                logAndAudit(AssertionMessages.JWT_JOSE_ERROR, e.getMessage());
                return AssertionStatus.FAILED;
            } catch(NullPointerException e){
                //catch NPE cause underlying api throws NPE when it encounter an unsupported key type
                logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Unsupported Key Type");
                return AssertionStatus.FAILED;
            }
        }
        //do header things

        return AssertionStatus.NONE;
    }

    private Key getSigningKey(final JsonWebSignature jws, final PolicyEnforcementContext context) throws InvalidAlgorithmException {
        final Map<String, Object> variables = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
        if (assertion.getSignatureSourceType() == JsonWebTokenConstants.SOURCE_SECRET) {
            final Object secretObj;

            if (singleSignatureSecretVarKeyExpr) {
                secretObj = ExpandVariables.processSingleVariableAsObject(assertion.getSignatureSecretKey(), variables, getAudit(), true);
            } else {
                if (assertion.getSignatureSecretKey() == null ||
                        ExpandVariables.isVariableReferencedNotFound(assertion.getSignatureSecretKey(), variables, getAudit())) {
                    logAndAudit(AssertionMessages.JWT_MISSING_JWS_HMAC_SECRET);
                    return null;
                }

                secretObj = ExpandVariables.process(assertion.getSignatureSecretKey(), variables, getAudit(), true);
            }

            byte[] secret;

            try {
                secret = toByteArray(secretObj, "<Key>");
            } catch (ContextVariableUtils.NoBinaryRepresentationException e) {
                // Fall back to legacy behavior and treat input as string expression then encode in UTF-8
                secret = ExpandVariables.process(assertion.getSignatureSecretKey(), variables, getAudit(), true).getBytes(Charsets.UTF8);
            }

            if (secret == null || secret.length < 1) {
                logAndAudit(AssertionMessages.JWT_MISSING_JWS_HMAC_SECRET);
                return null;
            }

            if (assertion.isSignatureSecretBase64Encoded()) {
                secret = HexUtils.decodeBase64(new String(secret));
            }

            return new HmacKey(secret);
        } else if (assertion.getSignatureSourceType() == JsonWebTokenConstants.SOURCE_PK) {
            try {
                final SsgKeyEntry ssgKeyEntry = JwtUtils.getKeyFromStore(defaultKey, getAudit(), assertion.getKeyGoid(), assertion.getKeyAlias());
                if (ssgKeyEntry != null) {
                    return ssgKeyEntry.getPrivateKey();
                }
            } catch (UnrecoverableKeyException e) {
                logAndAudit(AssertionMessages.JWT_KEY_RECOVERY_ERROR);
            }
        } else if (assertion.getSignatureSourceType() == JsonWebTokenConstants.SOURCE_CV) {
            Object key = ExpandVariables.processSingleVariableAsObject(assertion.getSignatureSourceVariable(), variables, getAudit(), false);
            if (key == null) {
                logAndAudit(AssertionMessages.JWT_MISSING_JWS_PRIVATE_KEY);
                return null;
            }
            final String keyType = assertion.getSignatureKeyType();
            if (JsonWebTokenConstants.KEY_TYPE_JWK.equals(keyType)) {
                return JwtUtils.getKeyFromJWK(getAudit(), key, true, null);
            } else if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyType)) {
                final String kid = ExpandVariables.process(assertion.getSignatureJwksKeyId(), variables, getAudit(), false);
                if (kid == null || kid.trim().isEmpty()) {
                    logAndAudit(AssertionMessages.JWT_MISSING_JWS_KID);
                    return null;
                }
                return JwtUtils.getKeyFromJWKS(getAudit(), jws, key, kid, true);
            }
        }
        return null;
    }

    private Key getEncryptionKey(final JsonWebEncryption jwe, final PolicyEnforcementContext context) {
        final Map<String, Object> variables = context.getVariableMap(assertion.getVariablesUsed(), getAudit());

        if (assertion.getEncryptionSourceType() == JsonWebTokenConstants.SOURCE_SECRET) {
            final Object secretObj;

            if (singleEncryptionSecretVarKeyExpr) {
                secretObj = ExpandVariables.processSingleVariableAsObject(assertion.getEncryptionSecret(), variables, getAudit(), true);
            } else {
                if (assertion.getEncryptionSecret() == null ||
                        ExpandVariables.isVariableReferencedNotFound(assertion.getEncryptionSecret(), variables, getAudit())) {
                    logAndAudit(AssertionMessages.JWT_JWE_KEY_ERROR);
                    return null;
                }

                secretObj = ExpandVariables.process(assertion.getEncryptionSecret(), variables, getAudit(), true);
            }

            byte[] secret;

            try {
                secret = toByteArray(secretObj, "<Key>");
            } catch (ContextVariableUtils.NoBinaryRepresentationException e) {
                // Fall back to legacy behavior and treat input as string expression then encode in UTF-8
                secret = ExpandVariables.process(assertion.getEncryptionSecret(), variables, getAudit(), true).getBytes(Charsets.UTF8);
            }

            if (secret == null || secret.length < 1) {
                logAndAudit(AssertionMessages.JWT_JWE_KEY_ERROR);
                return null;
            }

            if (assertion.isEncryptionSecretBase64Encoded()) {
                secret = HexUtils.decodeBase64(new String(secret));
            }

            return new SecretKeySpec(secret, AesKey.ALGORITHM);
        } else if (assertion.getEncryptionSourceType() == JsonWebTokenConstants.SOURCE_CV) {
            Object key = ExpandVariables.processSingleVariableAsObject(assertion.getEncryptionKey(), variables, getAudit(), false);
            if (key == null) {
                logAndAudit(AssertionMessages.JWT_JWE_KEY_ERROR);
                return null;
            }
            final String keyType = assertion.getEncryptionKeyType();
            if (JsonWebTokenConstants.KEY_TYPE_CERTIFICATE.equals(keyType)) {
                //cert was given via context var.
                //grab it and check if it can be use to encrypt data
                X509Certificate cert = null;
                if (key instanceof X509Certificate) {
                    cert = (X509Certificate) key;
                } else if (key instanceof String) {
                    cert = JwtUtils.getPublicKeyFromPem(getAudit(), key.toString());
                }
                if (cert != null) {
                    try {
                        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.encryptXml, cert, cert.getPublicKey());
                        return cert.getPublicKey();
                    } catch (CertificateException e) {
                        logAndAudit(AssertionMessages.JWT_INVALID_KEY_USAGE, "Invalid key usage: " + e.getMessage());
                    }
                }
            } else if (JsonWebTokenConstants.KEY_TYPE_JWK.equals(keyType)) {
                return JwtUtils.getKeyFromJWK(getAudit(), key, false, Use.ENCRYPTION);
            } else if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyType)) {
                if (assertion.getEncryptionKeyId() == null || assertion.getEncryptionKeyId().trim().isEmpty()) {
                    logAndAudit(AssertionMessages.JWT_MISSING_JWS_KID);
                    return null;
                }
                final String kid = ExpandVariables.process(assertion.getEncryptionKeyId(), variables, getAudit(), false);
                if (kid == null || kid.trim().isEmpty()) {
                    logAndAudit(AssertionMessages.JWT_MISSING_JWS_KID);
                    return null;
                }
                return JwtUtils.getKeyFromJWKS(getAudit(), jwe, key, kid, false);
            }
        }
        return null;
    }

    private byte[]toByteArray(Object obj, String what) throws ContextVariableUtils.NoBinaryRepresentationException {
        try {
            // Preserve old behavior of using UTF-8 for string values
            return ContextVariableUtils.convertContextVariableValueToByteArray(obj, -1, Charsets.UTF8);
        } catch (NoSuchPartException e) {
            getAudit().logAndAudit(AssertionMessages.NO_SUCH_PART, new String[] {what, e.getWhatWasMissing()},
                    ExceptionUtils.getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Unable to read " + what + ":" + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] {"Unable to read source object: " + ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Unable to read " + what + ":" + ExceptionUtils.getMessage(e), e);
        }
    }

    private AssertionStatus handleHeaders(final JsonWebStructure jws, final String sourceHeaders) {
        if (assertion.getHeaderAction() != null && !JsonWebTokenConstants.HEADERS_USE_DEFAULT.equals(assertion.getHeaderAction())) {
            try {
                Map<String, Object> headers = new ObjectMapper().reader(Map.class).readValue(sourceHeaders);
                if (JsonWebTokenConstants.HEADERS_MERGE.equals(assertion.getHeaderAction())) {
                    for (Map.Entry<String, Object> ent : headers.entrySet()) {
                        jws.setHeader(ent.getKey(), ent.getValue().toString());
                    }
                } else if (JsonWebTokenConstants.HEADERS_REPLACE.equals(assertion.getHeaderAction())) {
                    jws.getHeaders().setFullHeaderAsJsonString(sourceHeaders);
                }
            } catch (IOException | JoseException e) {
                logAndAudit(AssertionMessages.JWT_SOURCE_HEADERS_ERROR, e.getMessage());
                return AssertionStatus.FAILED;
            }
        }
        return AssertionStatus.NONE;
    }


}
