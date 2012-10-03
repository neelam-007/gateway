package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ManageApiKeyAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManagerFactory;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyXmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ManageApiKeyAssertion.
 *
 * @see com.l7tech.external.assertions.apiportalintegration.ManageApiKeyAssertion
 */
public class ServerManageApiKeyAssertion extends AbstractServerAssertion<ManageApiKeyAssertion> {
    private static final Logger logger = Logger.getLogger(ServerManageApiKeyAssertion.class.getName());

    private final ManageApiKeyAssertion assertion;
    private final String[] variablesUsed;
    private final String action;

    private final PortalGenericEntityManager<ApiKeyData> apiKeyManager;

    public ServerManageApiKeyAssertion(ManageApiKeyAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        this(assertion, context, null);
    }

    /*
     * For tests.
     */
    ServerManageApiKeyAssertion(ManageApiKeyAssertion assertion, ApplicationContext context, @Nullable final PortalGenericEntityManager<ApiKeyData> apiKeyManager) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
        this.action = assertion.getAction();
        this.apiKeyManager = apiKeyManager != null ? apiKeyManager : ApiKeyManagerFactory.getInstance();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        /*
         * Define interface for Add/Update/Delete of API keys
         *
         * 1) action = "add"; call ClusterPropertyKeyStore.add(XML_Document);
         *
         * 2) action = "update"; call ClusterPropertyKeyStore.update(XML_Document);
         *
         * 3) action = "remove"; call ClusterPropertyStore.remove(XML_Document);
         */
        try {
            Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());

            final String apiKey = ExpandVariables.process(assertion.getApiKey(), vars, getAudit(), true);
            final String apiKeyXml = ExpandVariables.process(assertion.getApiKeyElement(), vars, getAudit(), true);

            // apiKey is required
            if (apiKey == null || apiKey.length() == 0) {
                final String errorMsg = "Missing API key param required for action: " + action;
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, errorMsg);
                return AssertionStatus.FAILED;
            }
            // apiKeyXml is required for Add and Update actions
            if ("Add".equals(action) || "Update".equals(action)) {
                if (apiKeyXml == null || apiKeyXml.length() == 0) {
                    final String errorMsg = "Missing API key element required for action: " + action;
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, errorMsg);
                    return AssertionStatus.FAILED;
                }
            }

            // perform specified action

            ApiKeyData data = new ApiKeyData(apiKeyXml);
            ApiKeyXmlUtil.updateApiKeyDataFromXml(data, apiKeyXml);
            data.setKey(apiKey);

            // do Add
            if (ManageApiKeyAssertion.ACTION_ADD.equals(action)) {

                try {
                    apiKeyManager.add(data);
                } catch (DuplicateObjectException e) {
                    // error case, duplicate key error
                    logger.warning("Duplicate key error");
                    return AssertionStatus.FALSIFIED;
                }

                context.setVariable(assertion.getVariablePrefix() + "." + ManageApiKeyAssertion.SUFFIX_KEY, apiKey);

            } else if (ManageApiKeyAssertion.ACTION_UPDATE.equals(action)) {

                try {
                    apiKeyManager.update(data);
                } catch (ObjectNotFoundException e) {
                    // error case, key mapping not found
                    logger.log(Level.WARNING, "Key Entry not found for update: {0}", apiKey);
                    return AssertionStatus.FALSIFIED;
                }

            } else if (ManageApiKeyAssertion.ACTION_REMOVE.equals(action)) {

                try {
                    apiKeyManager.delete(apiKey);
                } catch (ObjectNotFoundException e) {
                    // warning case: log, no further action taken
                    logger.log(Level.WARNING, "Key Entry not found for removal: {0}, no action taken", apiKey);
                }

            } else {
                // unknown action, should never happen
                logger.log(Level.WARNING, "Unknown ACTION [{0}]: assertion mis-configured", assertion.getAction());
            }

            logger.log(Level.FINER, "Manage API Keys: action={0}, key={1}, asXml={2}", new Object[] {action, apiKey, apiKeyXml});

        } catch (Exception ex) {
            final String errorMsg = "Error performing manage API key operation: " + action;
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{errorMsg}, ExceptionUtils.getDebugException(ex));
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }
}
