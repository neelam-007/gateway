package com.l7tech.external.assertions.idattr.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.external.assertions.idattr.IdentityAttributesAssertion;
import com.l7tech.identity.User;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.identity.mapping.AttributeConfig;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.mapping.AttributeExtractor;
import com.l7tech.server.identity.mapping.ExtractorFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Server side implementation of the IdentityAttributesAssertion.
 *
 * @see com.l7tech.external.assertions.idattr.IdentityAttributesAssertion
 */
public class ServerIdentityAttributesAssertion extends AbstractServerAssertion<IdentityAttributesAssertion> {
    private final List<IdentityMapping> lookupAttributes;
    private final List<AttributeExtractor> attributeExtractors;
    private final String variablePrefix;

    public ServerIdentityAttributesAssertion(final IdentityAttributesAssertion assertion) {
        super(assertion);

        List<AttributeExtractor> extractors = new ArrayList<AttributeExtractor>();
        List<IdentityMapping> lattrs = Collections.unmodifiableList(Arrays.asList(assertion.getLookupAttributes()));
        if (lattrs != null && lattrs.size() > 0) {
            for (IdentityMapping im : lattrs) {
                extractors.add(ExtractorFactory.getExtractor(im));
            }
        }

        this.attributeExtractors = Collections.unmodifiableList(extractors);
        this.lookupAttributes = lattrs;
        String p = assertion.getVariablePrefix();
        if (p == null) p = IdentityAttributesAssertion.DEFAULT_VAR_PREFIX;
        this.variablePrefix = p;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AuthenticationContext authContext = context.getAuthenticationContext(context.getRequest());
        List<AuthenticationResult> results = authContext.getAllAuthenticationResults();

        User foundUser = null;
        for (AuthenticationResult result : results) {
            User u = result.getUser();
            if (u == null) continue;
            if (u.getProviderId().equals(assertion.getIdentityProviderOid())) {
                if (foundUser != null) {
                    logAndAudit(AssertionMessages.IDENTITY_ATTRIBUTE_MULTI_USERS);
                    break;
                }
                foundUser = u;
            }
        }

        if (foundUser == null) {
            logAndAudit(AssertionMessages.IDENTITY_ATTRIBUTE_NO_USER);
            return AssertionStatus.FAILED;
        }

        setUserAttributeVariables(foundUser, context);
        return AssertionStatus.NONE;
    }

    private void setUserAttributeVariables(User user, PolicyEnforcementContext context) {
        final IdentityMapping[] lattrs = assertion.getLookupAttributes();
        if (lattrs == null || lattrs.length == 0) return;

        for (int i = 0; i < lookupAttributes.size(); i++) {
            IdentityMapping im = lookupAttributes.get(i);
            AttributeExtractor extractor = attributeExtractors.get(i);
            Object[] vals = extractor.extractValues(user); // An NPE here indicates real problems, let it happen
            if (vals == null || vals.length == 0) continue;
            final AttributeConfig config = im.getAttributeConfig();
            context.setVariable(variablePrefix + "." + config.getVariableName(), im.isMultivalued() ? vals : vals[0]);
        }
    }
}
