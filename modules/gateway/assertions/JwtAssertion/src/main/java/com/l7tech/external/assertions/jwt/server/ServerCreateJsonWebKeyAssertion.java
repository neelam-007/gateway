package com.l7tech.external.assertions.jwt.server;

import com.google.common.collect.Lists;
import com.l7tech.external.assertions.jwt.CreateJsonWebKeyAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.external.assertions.jwt.JwkKeyInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;

import javax.inject.Inject;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.List;
import java.util.Map;

public class ServerCreateJsonWebKeyAssertion extends AbstractServerAssertion<CreateJsonWebKeyAssertion> {

    @Inject
    private DefaultKey defaultKey;

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
        final String s = new JsonWebKeySet(jwks).toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
        ObjectMapper mapper = new ObjectMapper();
        Object ob = mapper.readValue(s, Object.class);
        context.setVariable(assertion.getTargetVariable(), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ob));
        return AssertionStatus.NONE;
    }

    private JsonWebKey createJwk(@NotNull final JwkKeyInfo jwkKeyInfo, @NotNull final Map<String, Object> variables) {
        final Goid goid = jwkKeyInfo.getSourceKeyGoid();
        final String alias = jwkKeyInfo.getSourceKeyAlias();

        try {
            final SsgKeyEntry entry = defaultKey.lookupKeyByKeyAlias(alias, goid);
            final JsonWebKey jwk = JsonWebKey.Factory.newJwk(entry.getPublic());
            final String keyId = ExpandVariables.process(jwkKeyInfo.getKeyId(), variables, getAudit(), false);
            if(keyId == null || keyId.isEmpty()){
                logAndAudit(AssertionMessages.JWT_JWK_ERROR, "Could not find the specified key id");
                return null;
            }
            jwk.setKeyId(keyId);
            jwk.setUse(JsonWebTokenConstants.PUBLIC_KEY_USE.inverse().get(jwkKeyInfo.getPublicKeyUse()));
            return jwk;
        } catch (FindException e) {
            logAndAudit(AssertionMessages.JWT_JWK_NOT_FOUND, jwkKeyInfo.getSourceKeyAlias());
        } catch (KeyStoreException e){
            logAndAudit(AssertionMessages.JWT_KEYSTORE_ERROR);
        } catch (JoseException e) {
            logAndAudit(AssertionMessages.JWT_JWK_ERROR, e.getMessage());
        } catch (IOException e) {
            logAndAudit(AssertionMessages.JWT_KEYSTORE_ERROR);
        }
        return null;
    }
}
