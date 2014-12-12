package com.l7tech.external.assertions.jwt.server;


import com.l7tech.external.assertions.jwt.EncodeJsonWebTokenAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.external.assertions.jwt.JwtUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.InvalidAlgorithmException;
import org.jose4j.lang.JoseException;

import javax.inject.Inject;
import java.io.IOException;
import java.security.Key;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateParsingException;
import java.util.Map;


public class ServerEncodeJsonWebTokenAssertion extends AbstractServerAssertion<EncodeJsonWebTokenAssertion> {

    @Inject
    private SsgKeyStoreManager ssgKeyStoreManager;

    public ServerEncodeJsonWebTokenAssertion(@NotNull EncodeJsonWebTokenAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map<String, Object> variables = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
        //find the payload
        final String sourcePayload = ExpandVariables.process(assertion.getSourceVariable(), variables, getAudit(), true);
        if (sourcePayload == null || sourcePayload.trim().isEmpty()) {
            logAndAudit(AssertionMessages.JWT_MISSING_SOURCE_PAYLOAD);
            return AssertionStatus.FAILED;
        }
        //determine what to do with header and possibly get header
        String sourceHeaders = null;
        if (!JsonWebTokenConstants.HEADERS_USE_DEFAULT.equals(assertion.getHeaderAction())) {
            sourceHeaders = ExpandVariables.process(assertion.getSourceHeaders(), variables, getAudit(), true);
            if (sourceHeaders == null || sourceHeaders.trim().isEmpty()) {
                logAndAudit(AssertionMessages.JWT_MISSING_HEADERS, assertion.getHeaderAction());
                return AssertionStatus.FAILED;
            }
        }
        String jwsCompact = null;
        //unsecure --- why are we even doing this?!
        if ( (assertion.getSignatureAlgorithm() == null || "None".equals(assertion.getSignatureAlgorithm())) && (assertion.getKeyManagementAlgorithm() == null || "None".equals(assertion.getKeyManagementAlgorithm())) ){
            try {
                final JsonWebSignature str = new JsonWebSignature();
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
            }
        }
        //create JWS if set
        if (assertion.getSignatureAlgorithm() != null && !"None".equals(assertion.getSignatureAlgorithm())) {
            try {
                final JsonWebSignature jws = new JsonWebSignature();
                jws.setDoKeyValidation(true);
                jws.setPayload(sourcePayload);
                jws.setAlgorithmHeaderValue(assertion.getSignatureAlgorithm());
                final Key signingKey = getSigningKey(jws, context);
                if (signingKey == null) {
                    logAndAudit(AssertionMessages.JWT_JWS_KEY_ERROR);
                    return AssertionStatus.FAILED;
                }
                jws.setKey(signingKey);
                //we don't have an encryption operation...we're done...set context vars and what not
                if (assertion.getKeyManagementAlgorithm() == null || "None".equals(assertion.getKeyManagementAlgorithm())) {
                    AssertionStatus status = handleHeaders(jws, sourceHeaders);
                    if (!AssertionStatus.NONE.equals(status)) {
                        return status;
                    }
                }
                jwsCompact = jws.getCompactSerialization();
                context.setVariable(assertion.getTargetVariable() + ".compact", jwsCompact);
            } catch(JoseException e) {
                logAndAudit(AssertionMessages.JWT_JOSE_ERROR, e.getMessage());
                return AssertionStatus.FAILED;
            }
        }
        //encryption
        if (assertion.getKeyManagementAlgorithm() != null && !"None".equals(assertion.getKeyManagementAlgorithm())) {
            final JsonWebEncryption jwe = new JsonWebEncryption();
            jwe.setDoKeyValidation(true);
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
            } catch (JoseException e) {
                logAndAudit(AssertionMessages.JWT_JOSE_ERROR, e.getMessage());
                return AssertionStatus.FAILED;
            }
        }


        return AssertionStatus.NONE;
    }

    private Key getSigningKey(final JsonWebSignature jws, final PolicyEnforcementContext context) throws InvalidAlgorithmException {
        final Map<String, Object> variables = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
        final String signatureAlgorithm = assertion.getSignatureAlgorithm();

        //set key base in configuration
        if (signatureAlgorithm.startsWith("HS")) {
            final String secretKey = ExpandVariables.process(assertion.getSignatureSecretKey(), variables, getAudit(), true);
            if (secretKey == null || secretKey.trim().isEmpty()) {
                logAndAudit(AssertionMessages.JWT_MISSING_JWS_HMAC_SECRET);
                return null;
            }
            return new HmacKey(secretKey.getBytes());
        } else if (!signatureAlgorithm.equals("None")) {
            if (assertion.isPrivateKeyFromVariable()) {
                String key = ExpandVariables.process(assertion.getSignatureSourceVariable(), variables, getAudit(), true);
                if (key == null || key.trim().isEmpty()) {
                    logAndAudit(AssertionMessages.JWT_MISSING_JWS_PRIVATE_KEY);
                    return null;
                }
                final String keyType = assertion.getSignatureKeyType();
                if (JsonWebTokenConstants.KEY_TYPE_JWK.equals(keyType)) {
                    return JwtUtils.getKeyFromJWK(getAudit(), key, true);
                } else if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyType)) {
                    final String kid = ExpandVariables.process(assertion.getSignatureJwksKeyId(), variables, getAudit(), true);
                    if (kid == null || kid.trim().isEmpty()) {
                        logAndAudit(AssertionMessages.JWT_MISSING_JWS_KID);
                        return null;
                    }
                    return JwtUtils.getKeyFromJWKS(getAudit(), jws, key, kid, true);
                }
            } else {
                try {
                    final SsgKeyEntry ssgKeyEntry = JwtUtils.getKeyFromStore(ssgKeyStoreManager, getAudit(), Goid.parseGoid(assertion.getPrivateKeyGoid()), assertion.getPrivateKeyAlias());
                    KeyUsageChecker.requireActivity(KeyUsageActivity.signXml, ssgKeyEntry.getCertificate());
                    return ssgKeyEntry.getPrivateKey();
                } catch (KeyUsageException e) {
                    logAndAudit(AssertionMessages.JWT_INVALID_KEY_USAGE, "Invalid key usage: " + e.getMessage());
                } catch (CertificateParsingException e) {
                    logAndAudit(AssertionMessages.JWT_INVALID_KEY_USAGE, "Could not determine key usage: " + e.getMessage());
                } catch (UnrecoverableKeyException e) {
                    logAndAudit(AssertionMessages.JWT_KEY_RECOVERY_ERROR);
                }
            }
        }
        return null;
    }

    private Key getEncryptionKey(final JsonWebEncryption jwe, final PolicyEnforcementContext context) {
        final Map<String, Object> variables = context.getVariableMap(assertion.getVariablesUsed(), getAudit());

        String key = ExpandVariables.process(assertion.getEncryptionKey(), variables, getAudit(), true);
        if (key == null || key.trim().isEmpty()) {
            logAndAudit(AssertionMessages.JWT_MISSING_JWS_PRIVATE_KEY);
            return null;
        }
        final String keyType = assertion.getEncryptionKeyType();
        if (JsonWebTokenConstants.KEY_TYPE_CERTIFICATE.equals(keyType)) {
            return JwtUtils.getPublicKeyFromPem(getAudit(), key);
        } else if (JsonWebTokenConstants.KEY_TYPE_JWK.equals(keyType)) {
            return JwtUtils.getKeyFromJWK(getAudit(), key, false);
        } else if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyType)) {
            final String kid = ExpandVariables.process(assertion.getEncryptionKeyId(), variables, getAudit(), true);
            if (kid == null || kid.trim().isEmpty()) {
                logAndAudit(AssertionMessages.JWT_MISSING_JWS_KID);
                return null;
            }
            return JwtUtils.getKeyFromJWKS(getAudit(), jwe, key, kid, false);
        }
        return null;
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
