package com.l7tech.external.assertions.idattr.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.external.assertions.idattr.IdentityAttributesAssertion;
import com.l7tech.identity.User;
import com.l7tech.identity.mapping.AttributeConfig;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.mapping.AttributeExtractor;
import com.l7tech.server.identity.mapping.ExtractorFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the IdentityAttributesAssertion.
 *
 * @see com.l7tech.external.assertions.idattr.IdentityAttributesAssertion
 */
public class ServerIdentityAttributesAssertion extends AbstractServerAssertion<IdentityAttributesAssertion> {
    private static final Logger logger = Logger.getLogger(ServerIdentityAttributesAssertion.class.getName());

    private final Auditor auditor;
    private final Map<IdentityMapping, AttributeExtractor> attributeExtractors;
    private final String variablePrefix;

    public ServerIdentityAttributesAssertion(IdentityAttributesAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.auditor = new Auditor(this, context, logger);

        Map<IdentityMapping, AttributeExtractor> extractors = new HashMap<IdentityMapping, AttributeExtractor>();
        final IdentityMapping[] lattrs = assertion.getLookupAttributes();
        if (lattrs != null && lattrs.length > 0) {
            for (IdentityMapping im : lattrs) {
                extractors.put(im, ExtractorFactory.getExtractor(im));
            }
        }

        this.attributeExtractors = extractors;
        String p = assertion.getVariablePrefix();
        if (p == null) p = IdentityAttributesAssertion.DEFAULT_VAR_PREFIX;
        this.variablePrefix = p;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        List<AuthenticationResult> results = context.getAllAuthenticationResults();

        User foundUser = null;
        for (AuthenticationResult result : results) {
            User u = result.getUser();
            if (u == null) continue;
            if (u.getProviderId() == assertion.getIdentityProviderOid()) {
                if (foundUser != null) {
                    auditor.logAndAudit(AssertionMessages.IDENTITY_ATTRIBUTE_MULTI_USERS);
                    break;
                }
                foundUser = u;
            }
        }

        if (foundUser == null) {
            auditor.logAndAudit(AssertionMessages.IDENTITY_ATTRIBUTE_NO_USER);
            return AssertionStatus.FAILED;
        }

        setUserAttributeVariables(foundUser, context);
        return AssertionStatus.NONE;
    }

    private void setUserAttributeVariables(User user, PolicyEnforcementContext context) {
        final IdentityMapping[] lattrs = assertion.getLookupAttributes();
        if (lattrs == null || lattrs.length == 0) return;

        for (IdentityMapping im : lattrs) {
            AttributeExtractor extractor = attributeExtractors.get(im);
            Object[] vals = extractor.extractValues(user); // An NPE here indicates real problems, let it happen
            if (vals == null || vals.length == 0) continue;
            final AttributeConfig config = im.getAttributeConfig();
            context.setVariable(variablePrefix + "." + config.getVariableName(), im.isMultivalued() ? vals : vals[0]);
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerIdentityAttributesAssertion is preparing itself to be unloaded");
    }
}
