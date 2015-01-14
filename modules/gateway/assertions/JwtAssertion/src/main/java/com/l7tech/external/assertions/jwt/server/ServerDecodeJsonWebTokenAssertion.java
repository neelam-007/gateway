package com.l7tech.external.assertions.jwt.server;


import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.l7tech.external.assertions.jwt.DecodeJsonWebTokenAssertion;
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
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwk.Use;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.CompactSerializer;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public class ServerDecodeJsonWebTokenAssertion extends AbstractServerAssertion<DecodeJsonWebTokenAssertion> {

    @Inject
    private DefaultKey defaultKey;

    public ServerDecodeJsonWebTokenAssertion(@NotNull DecodeJsonWebTokenAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map<String, Object> variables = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
        //find the payload
        final String sourcePayload = ExpandVariables.process(assertion.getSourcePayload(), variables, getAudit(), false);
        if (sourcePayload == null || sourcePayload.trim().isEmpty()) {
            logAndAudit(AssertionMessages.JWT_MISSING_SOURCE_PAYLOAD);
            return AssertionStatus.FAILED;
        }

        final String[] parts = CompactSerializer.deserialize(sourcePayload);
        if (parts.length != JsonWebEncryption.COMPACT_SERIALIZATION_PARTS && parts.length != JsonWebSignature.COMPACT_SERIALIZATION_PARTS) {
            logAndAudit(AssertionMessages.JWT_DECODE_ERROR);
            return AssertionStatus.FAILED;
        }
        AssertionStatus status = expandHeaders(context, parts[0]);
        if(status.equals(AssertionStatus.FAILED)){
            return status;
        }
        if (parts.length == JsonWebEncryption.COMPACT_SERIALIZATION_PARTS) {
            context.setVariable(assertion.getTargetVariablePrefix() + ".type", "JWE");
            context.setVariable(assertion.getTargetVariablePrefix() + ".encrypted_key", parts[1]);
            context.setVariable(assertion.getTargetVariablePrefix() + ".initialization_vector", parts[2]);
            context.setVariable(assertion.getTargetVariablePrefix() + ".cipher_text", parts[3]);
            context.setVariable(assertion.getTargetVariablePrefix() + ".authentication_tag", parts[4]);
        } else if (parts.length == JsonWebSignature.COMPACT_SERIALIZATION_PARTS) {
            context.setVariable(assertion.getTargetVariablePrefix() + ".type", "JWS");
            context.setVariable(assertion.getTargetVariablePrefix() + ".payload", new String(BaseEncoding.base64Url().decode(parts[1])));
            context.setVariable(assertion.getTargetVariablePrefix() + ".signature", parts[2]);

        }
        final JsonWebStructure structure = getJWT(sourcePayload);
        if (structure == null) {
            return AssertionStatus.FAILED;
        }
        structure.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS);
        structure.setDoKeyValidation(true);
        final String validate = assertion.getValidationType();
        //no validation - just exit as we have the context vars set already
        if (JsonWebTokenConstants.VALIDATION_NONE.equals(validate)) {
            //no validation
            return AssertionStatus.NONE;
        }

        //validate MAC
        if (JsonWebTokenConstants.VALIDATION_USING_SECRET.equals(validate)) {
            return jwsValidateUsingSecret(context, structure, variables);
        }
        //validate with KEY
        Key key = null;
        if (JsonWebTokenConstants.VALIDATION_USING_PK.equals(validate)) {
            try {
                final SsgKeyEntry ssgKeyEntry = JwtUtils.getKeyFromStore(defaultKey, getAudit(), assertion.getNonDefaultKeystoreId(), assertion.getKeyAlias());
                //jws
                if(parts.length == JsonWebSignature.COMPACT_SERIALIZATION_PARTS){
                    try {
                        KeyUsageChecker.requireActivity(KeyUsageActivity.verifyXml, ssgKeyEntry.getCertificate());
                        key = ssgKeyEntry.getPublic();
                    } catch (KeyUsageException e) {
                        logAndAudit(AssertionMessages.JWT_INVALID_KEY_USAGE, "Invalid key usage: " + e.getMessage());
                    } catch (CertificateParsingException e) {
                        logAndAudit(AssertionMessages.JWT_INVALID_KEY_USAGE, "Could not determine key usage: " + e.getMessage());
                    }
                }
                //jwe
                else if (parts.length == JsonWebEncryption.COMPACT_SERIALIZATION_PARTS){
                    key = ssgKeyEntry.getPrivateKey();
                }
            } catch (UnrecoverableKeyException e) {
                logAndAudit(AssertionMessages.JWT_KEY_RECOVERY_ERROR);
            }
        } else if (JsonWebTokenConstants.VALIDATION_USING_CV.equals(validate)) {
            final Object keySource = ExpandVariables.processSingleVariableAsObject(assertion.getPrivateKeySource(), variables, getAudit(), false);
            if (keySource == null) {
                logAndAudit(AssertionMessages.JWT_PRIVATE_KEY_NOT_FOUND);
                return AssertionStatus.FAILED;
            }
            if (JsonWebTokenConstants.KEY_TYPE_JWK.equals(assertion.getKeyType())) {
                //when we decrypt and it's a JWE we want the private key, otherwise get the public key
                String use = structure instanceof JsonWebEncryption ? null : Use.SIGNATURE;
                key = JwtUtils.getKeyFromJWK(getAudit(), keySource, structure instanceof JsonWebEncryption, use);
            } else if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(assertion.getKeyType())) {
                final String kid = ExpandVariables.process(assertion.getKeyId(), variables, getAudit(), false);
                if (kid == null || kid.trim().isEmpty()) {
                    logAndAudit(AssertionMessages.JWT_MISSING_JWS_KID);
                    return AssertionStatus.FAILED;
                }
                key = JwtUtils.getKeyFromJWKS(getAudit(), structure, keySource, kid, structure instanceof JsonWebEncryption);
            } else if (JsonWebTokenConstants.KEY_TYPE_CERTIFICATE.equals(assertion.getKeyType())) {
                //ok we used a cert but we got a jwe, can't do anything
                if (parts.length == JsonWebEncryption.COMPACT_SERIALIZATION_PARTS){
                    logAndAudit(AssertionMessages.JWT_GENERAL_DECODE_ERROR, "Invalid configuration - can not use a certificate (public key) to decrypt JWE.");
                    return AssertionStatus.FAILED;
                }
                Object sourceKey = ExpandVariables.processSingleVariableAsObject(assertion.getPrivateKeySource(), variables, getAudit(), false);
                if (sourceKey == null) {
                    logAndAudit(AssertionMessages.JWT_JWE_KEY_ERROR);
                }
                //cert was given via context var.
                //grab it and check if it can be use to encrypt data
                X509Certificate cert = null;
                if (sourceKey instanceof X509Certificate) {
                    cert = (X509Certificate) sourceKey;
                } else if (sourceKey instanceof String) {
                    cert = JwtUtils.getPublicKeyFromPem(getAudit(), sourceKey.toString());
                }
                if (cert != null) {
                    try {
                        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.verifyXml, cert, cert.getPublicKey());
                        key = cert.getPublicKey();
                    } catch (CertificateException e) {
                        logAndAudit(AssertionMessages.JWT_INVALID_KEY_USAGE, "Invalid key usage: " + e.getMessage());
                    }
                }
            }
        }
        if (key == null) {
            logAndAudit(AssertionMessages.JWT_PRIVATE_KEY_NOT_FOUND);
            return AssertionStatus.FAILED;
        }
        //validate w/ the key
        structure.setKey(key);

        try {
            if (structure instanceof JsonWebSignature) {
                final JsonWebSignature jws = (JsonWebSignature) structure;
                context.setVariable(assertion.getTargetVariablePrefix() + ".valid", String.valueOf(jws.verifySignature()));
            }
            if (structure instanceof JsonWebEncryption) {
                final JsonWebEncryption jwe = (JsonWebEncryption) structure;
                context.setVariable(assertion.getTargetVariablePrefix() + ".plaintext", String.valueOf(jwe.getPlaintextString()));
                context.setVariable(assertion.getTargetVariablePrefix() + ".valid", String.valueOf(true));
            }
        } catch (JoseException e) {
            logAndAudit(AssertionMessages.JWT_GENERAL_DECODE_ERROR, "Could not validate JWT payload: " + e.getMessage());
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    private AssertionStatus expandHeaders(final PolicyEnforcementContext context, final String header) {
        try {
            final String decoded = new String(BaseEncoding.base64Url().decode(header));
            final Map<String, Object> headers = new ObjectMapper().reader(Map.class).readValue(decoded);
            final List<String> names = Lists.newArrayList();
            context.setVariable(assertion.getTargetVariablePrefix() + ".header", decoded);
            for (Map.Entry<String, Object> ent : headers.entrySet()) {
                names.add(ent.getKey());
                context.setVariable(assertion.getTargetVariablePrefix() + ".header." + ent.getKey(), ent.getValue());
            }
            context.setVariable(assertion.getTargetVariablePrefix() + ".header.names", names);
        } catch (IOException e) {
            logAndAudit(AssertionMessages.JWT_GENERAL_DECODE_ERROR, "could not parse headers.");
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    private JsonWebStructure getJWT(final String payload) {
        try {
            return JsonWebStructure.fromCompactSerialization(payload);
        } catch (JoseException e) {
            logAndAudit(AssertionMessages.JWT_DECODE_ERROR);
        }
        return null;
    }

    private AssertionStatus jwsValidateUsingSecret(final PolicyEnforcementContext context, final JsonWebStructure structure, final Map<String, Object> variables) {
        //not JWS but configure to use a secret...fail
        if (!(structure instanceof JsonWebSignature)) {
            logAndAudit(AssertionMessages.JWT_DECODE_INVALID_TYPE);
            return AssertionStatus.FAILED;
        }
        //JWS but did not configure a secret...fail
        final JsonWebSignature jws = (JsonWebSignature) structure;
        if (!jws.getHeader(HeaderParameterNames.ALGORITHM).startsWith("HS")) {
            logAndAudit(AssertionMessages.JWT_INVALID_ALGORITHM, "A secret must be used to validate JWT with 'alg' of type " + jws.getHeader(HeaderParameterNames.ALGORITHM));
            return AssertionStatus.FAILED;
        }
        //we good?
        final String secret = ExpandVariables.process(assertion.getSignatureSecret(), variables, getAudit(), false);
        if (secret == null || secret.trim().isEmpty()) {
            logAndAudit(AssertionMessages.JWT_DECODE_MISSING_SECRET);
            return AssertionStatus.FAILED;
        }
        try {
            jws.setKey(new HmacKey(secret.getBytes("UTF-8")));
            context.setVariable(assertion.getTargetVariablePrefix() + ".valid", String.valueOf(jws.verifySignature()));
        } catch (JoseException e) {
            logAndAudit(AssertionMessages.JWT_GENERAL_DECODE_ERROR, "Could not validate JWS: " + e.getMessage());
            return AssertionStatus.FAILED;
        } catch (UnsupportedEncodingException e) {
            logAndAudit(AssertionMessages.JWT_GENERAL_DECODE_ERROR, "Invalid encoding: " + e.getCause());
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }
}
