package com.l7tech.external.assertions.jwt.server;


import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.jwt.DecodeJsonWebTokenAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.external.assertions.jwt.JwtUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.server.DefaultKey;
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
import org.jose4j.jwa.AlgorithmFactory;
import org.jose4j.jwa.AlgorithmFactoryFactory;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithm;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwe.RsaKeyManagementAlgorithm;
import org.jose4j.jwk.Use;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.CompactSerializer;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.AesKey;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.io.IOException;
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

    private final boolean singleVarKeyExpr;

    public ServerDecodeJsonWebTokenAssertion(@NotNull DecodeJsonWebTokenAssertion assertion) {
        super(assertion);
        singleVarKeyExpr = assertion.getSignatureSecret() != null 
                && Syntax.isOnlyASingleVariableReferenced(assertion.getSignatureSecret());
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
                final SsgKeyEntry ssgKeyEntry = JwtUtils.getKeyFromStore(defaultKey, getAudit(), assertion.getKeyGoid(), assertion.getKeyAlias());
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
                final AlgorithmFactory<KeyManagementAlgorithm> algorithmFactory = AlgorithmFactoryFactory.getInstance().getJweKeyManagementAlgorithmFactory();
                if(!algorithmFactory.isAvailable("RSA/ECB/OAEPWithSHA1AndMGF1Padding")){
                    algorithmFactory.registerAlgorithm(new RsaKeyManagementAlgorithm("RSA/ECB/OAEPWithSHA1AndMGF1Padding", KeyManagementAlgorithmIdentifiers.RSA_OAEP));
                }
                if(!algorithmFactory.isAvailable("RSA/ECB/OAEPWithSHA256AndMGF1Padding")){
                    algorithmFactory.registerAlgorithm(new RsaKeyManagementAlgorithm("RSA/ECB/OAEPWithSHA256AndMGF1Padding", KeyManagementAlgorithmIdentifiers.RSA_OAEP_256));
                }
                final JsonWebEncryption jwe = (JsonWebEncryption) structure;
                context.setVariable(assertion.getTargetVariablePrefix() + ".plaintext", String.valueOf(jwe.getPlaintextString()));
                context.setVariable(assertion.getTargetVariablePrefix() + ".valid", String.valueOf(true));
            }
        } catch (JoseException e) {
            logAndAudit(AssertionMessages.JWT_GENERAL_DECODE_ERROR, "Could not validate JWT payload: " + e.getMessage());
            return AssertionStatus.FAILED;
        } catch(NullPointerException e){
            //catch NPE cause underlying api throws NPE when it encounter an unsupported key type
            logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Unsupported Key Type");
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

        if (!structure.getHeader(HeaderParameterNames.ALGORITHM).startsWith("HS") && !structure.getHeader(HeaderParameterNames.ALGORITHM).startsWith("dir")) {
            logAndAudit(AssertionMessages.JWT_INVALID_ALGORITHM, "A secret cannot be used validate/decrypt JWT with 'alg' of type " + structure.getHeader(HeaderParameterNames.ALGORITHM));
            return AssertionStatus.FAILED;
        }
        
        //we good?
        final Object secretObj;
        
        if (singleVarKeyExpr) {
            secretObj = ExpandVariables.processSingleVariableAsObject(assertion.getSignatureSecret(), variables, getAudit(), true);
        } else {
            if (assertion.getSignatureSecret() == null ||
                    ExpandVariables.isVariableReferencedNotFound(assertion.getSignatureSecret(), variables, getAudit())) {
                logAndAudit(AssertionMessages.GENERATE_HASH_VARIABLE_NOT_SET, "Key");
                throw new AssertionStatusException("Key variable not set");
            }

            secretObj = ExpandVariables.process(assertion.getSignatureSecret(), variables, getAudit(), true);
        }
        
        byte[] secret;
        
        try {
            secret = toByteArray(secretObj, "<Key>");
        } catch (ContextVariableUtils.NoBinaryRepresentationException e) {
            // Fall back to legacy behavior and treat input as string expression then encode in UTF-8
            secret = ExpandVariables.process(assertion.getSignatureSecret(), variables, getAudit(), true).getBytes(Charsets.UTF8);
        }
        
        if (secret == null || secret.length < 1) {
            logAndAudit(AssertionMessages.JWT_DECODE_MISSING_SECRET);
            return AssertionStatus.FAILED;
        }

        if (assertion.isBase64Encoded()) {
            secret = HexUtils.decodeBase64(new String(secret));
        }
        
        try {
            if (structure instanceof JsonWebSignature) {
                structure.setKey(new HmacKey(secret));
                context.setVariable(assertion.getTargetVariablePrefix() + ".valid", String.valueOf(((JsonWebSignature) structure).verifySignature()));
            }
            if (structure instanceof JsonWebEncryption) {
                structure.setKey(new SecretKeySpec(secret, AesKey.ALGORITHM));
                context.setVariable(assertion.getTargetVariablePrefix() + ".plaintext", String.valueOf(((JsonWebEncryption)structure).getPlaintextString()));
                context.setVariable(assertion.getTargetVariablePrefix() + ".valid", String.valueOf(true));
            }
        } catch (JoseException e) {
            logAndAudit(AssertionMessages.JWT_GENERAL_DECODE_ERROR, "Could not validate JWS: " + e.getMessage());
            return AssertionStatus.FAILED;
        }
        
        return AssertionStatus.NONE;
    }

    private byte[] toByteArray(Object obj, String what) throws ContextVariableUtils.NoBinaryRepresentationException {
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
}
