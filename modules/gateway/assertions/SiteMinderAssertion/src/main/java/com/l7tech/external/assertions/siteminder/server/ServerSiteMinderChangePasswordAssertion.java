package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderContext;
import com.l7tech.external.assertions.siteminder.SiteMinderChangePasswordAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.UUID;

/**
 * Server side implementation of the SiteMinderChangePasswordAssertion.
 *
 * @see com.l7tech.external.assertions.siteminder.SiteMinderChangePasswordAssertion
 */
public class ServerSiteMinderChangePasswordAssertion extends AbstractServerSiteMinderAssertion<SiteMinderChangePasswordAssertion> {
    private final SecurePasswordManager securePasswordManager;
    private final String[] variablesUsed;

    public ServerSiteMinderChangePasswordAssertion(final SiteMinderChangePasswordAssertion assertion, final ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context);

        this.securePasswordManager = context.getBean("securePasswordManager", SecurePasswordManager.class);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                          final Message message,
                                          final String messageDescription,
                                          final AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        final Map<String, Object> varMap = context.getVariableMap(variablesUsed, getAudit());
        final String domOid = ExpandVariables.process(assertion.getDomOid(), varMap, getAudit());
        final String username = ExpandVariables.process(assertion.getUsername(), varMap, getAudit());
        final String oldPassword = ExpandVariables.process(assertion.getOldPassword(), varMap, getAudit());
        final String newPassword = ExpandVariables.process(assertion.getNewPassword(), varMap, getAudit());
        final SiteMinderContext smContext = new SiteMinderContext();

        if (!initSmAgentFromContext(assertion.getAgentGoid(), smContext)) {
            return AssertionStatus.FALSIFIED;
        }

        final String transactionId = UUID.randomUUID().toString();//generate SiteMinder transaction id.
        smContext.setTransactionId(transactionId);
        smContext.setConfig(getSmConfig(assertion.getAgentGoid()));

        try {
            final String adminUsername = smContext.getConfig().getUserName();
            final String adminPassword = this.getAdminPassword(smContext.getConfig().getPasswordGoid());
            final int result = hla.processChangePasswordRequest(adminUsername, adminPassword, domOid, username, oldPassword, newPassword, smContext);
            if (result == 0) {
                logAndAudit(AssertionMessages.SINGLE_SIGN_ON_FINE, (String) (assertion.meta().get(AssertionMetadata.SHORT_NAME)),
                        "Changed user password for username: '" + username + "'");
                return AssertionStatus.NONE;
            } else {
                context.setVariable(SiteMinderChangePasswordAssertion.REASON_CODE_CONTEXT_VAR_NAME, result);
                logAndAudit(AssertionMessages.SINGLE_SIGN_ON_WARNING, (String) (assertion.meta().get(AssertionMetadata.SHORT_NAME)),
                        "Failed to change user password for username: '" + username + "'. Reason code: " + result);
                return AssertionStatus.FALSIFIED;
            }

        } catch (SiteMinderApiClassException | FindException | ParseException e) {
            logAndAudit(AssertionMessages.SINGLE_SIGN_ON_ERROR, (String) (assertion.meta().get(AssertionMetadata.SHORT_NAME)), e.getMessage());
            return AssertionStatus.FAILED;//something really bad happened
        }
    }

    private String getAdminPassword (final Goid adminPasswordId) throws FindException, ParseException {
        final SecurePassword securePassword = securePasswordManager.findByPrimaryKey(adminPasswordId);
        if (securePassword == null) {
            throw new FindException("Unable to find password.");
        }

        return new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
    }
}
