package com.l7tech.external.assertions.jwt.server;

import com.google.common.collect.Lists;
import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.jwt.CreateJsonWebKeyAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.external.assertions.jwt.JwkKeyInfo;
import com.l7tech.external.assertions.jwt.JwtUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.DefaultKeyCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import javax.inject.Inject;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ServerCreateJsonWebKeyAssertion extends AbstractServerAssertion<CreateJsonWebKeyAssertion> {

    @Inject
    private DefaultKeyCache defaultKey;

    public ServerCreateJsonWebKeyAssertion(@NotNull final CreateJsonWebKeyAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map<String, Object> variables = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
        final List<JwkKeyInfo> keys = assertion.getKeys();
        final List<JsonWebKey> jwks = Lists.newArrayList();
        if (keys != null && !keys.isEmpty()) {
            for (JwkKeyInfo k : keys) {
                final JsonWebKey jwk = createJwk(k, variables);
                if (jwk != null) {
                    jwks.add(jwk);
                }
            }
        }
        try {
            final String s = new JsonWebKeySet(jwks).toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
            ObjectMapper mapper = new ObjectMapper();
            Object ob = mapper.readValue(s, Object.class);
            context.setVariable(assertion.getTargetVariable(), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ob));
        } catch (NullPointerException e) {
            //catch NPE cause underlying api throws NPE when it encounter an unsupported key type
            logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Unsupported Key Type");
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    private JsonWebKey createJwk(@NotNull final JwkKeyInfo jwkKeyInfo, @NotNull final Map<String, Object> variables) {
        final Goid goid = jwkKeyInfo.getSourceKeyGoid();
        final String alias = jwkKeyInfo.getSourceKeyAlias();

        final SsgKeyEntry entry = JwtUtils.getKeyFromStore(defaultKey, getAudit(), goid, alias);
        if (entry == null){
            return null;
        }

        final String keyId = ExpandVariables.process(jwkKeyInfo.getKeyId(), variables, getAudit(), false);

        if (keyId == null || keyId.isEmpty()) {
            logAndAudit(AssertionMessages.JWT_JWK_ERROR, "Could not find the specified key id");
            return null;
        }

        final JsonWebKey jwk;

        try {
            jwk = JsonWebKey.Factory.newJwk(entry.getPublic());
        } catch (JoseException e) {
            logAndAudit(AssertionMessages.JWT_JWK_ERROR, e.getMessage());
            return null;
        } catch (NullPointerException e) {
            //catch NPE cause underlying api throws NPE when it encounter an unsupported key type
            logAndAudit(AssertionMessages.JWT_JOSE_ERROR, "Unsupported Key Type");
            return null;
        }

        jwk.setKeyId(keyId);
        jwk.setUse(JsonWebTokenConstants.PUBLIC_KEY_USE.inverse().get(jwkKeyInfo.getPublicKeyUse()));

        if (jwk instanceof PublicJsonWebKey) {
            final PublicJsonWebKey publicJwk = (PublicJsonWebKey) jwk;
            publicJwk.setCertificateChain(entry.getCertificateChain()); // RFC 7517 - 'x5c' parameter

            try {
                publicJwk.setX509CertificateSha1Thumbprint(CertUtils.getThumbprintSHA1(entry.getCertificate(), CertUtils.FINGERPRINT_BASE64URL)); // RFC 7517 - 'x5t' parameter, RFC 4648 base64url encoding
            } catch(CertificateEncodingException e) {
                logAndAudit(AssertionMessages.JWT_JWK_ERROR, e.getMessage());
            }
        } else {
            // this scenario should never happen because we don't support using symmetric keys to create JWKs
            logger.log(Level.FINE, "Unable to retrieve certificate chain or SHA-1 thumbprint for key with the alias of {0}", alias);
        }

        return jwk;
    }
}
