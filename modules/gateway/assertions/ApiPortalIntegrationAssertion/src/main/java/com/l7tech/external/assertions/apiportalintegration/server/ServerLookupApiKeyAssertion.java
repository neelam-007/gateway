package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.LookupApiKeyAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManager;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManagerFactory;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import javax.xml.bind.JAXBException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the LookupApiKeyAssertion.
 *
 * @see com.l7tech.external.assertions.apiportalintegration.LookupApiKeyAssertion
 */
public class ServerLookupApiKeyAssertion extends AbstractServerAssertion<LookupApiKeyAssertion> {
    private static final Logger LOGGER = Logger.getLogger(ServerLookupApiKeyAssertion.class.getName());
    private final PortalGenericEntityManager<ApiKeyData> legacyApiKeyManager;
    private final PortalGenericEntityManager<ApiKey> apiKeyManager;
    private final ResourceTransformer<ApiKeyResource, ApiKey> transformer;
    private final JAXBResourceMarshaller marshaller;

    public ServerLookupApiKeyAssertion(final LookupApiKeyAssertion assertion, final ApplicationContext context) throws PolicyAssertionException, JAXBException {
        this(assertion, ApiKeyManagerFactory.getInstance(), ApiKeyManager.getInstance(context), ApiKeyResourceTransformer.getInstance(), DefaultJAXBResourceMarshaller.getInstance());
    }

    /*
     * For tests.
     */
    ServerLookupApiKeyAssertion(@NotNull final LookupApiKeyAssertion assertion,
                                @NotNull final PortalGenericEntityManager<ApiKeyData> legacyApiKeyManager,
                                @NotNull final PortalGenericEntityManager<ApiKey> apiKeyManager,
                                @NotNull final ResourceTransformer<ApiKeyResource, ApiKey> transformer,
                                @NotNull final JAXBResourceMarshaller marshaller) throws PolicyAssertionException {
        super(assertion);
        validateAssertion(assertion);
        this.legacyApiKeyManager = legacyApiKeyManager;
        this.apiKeyManager = apiKeyManager;
        this.transformer = transformer;
        this.marshaller = marshaller;
    }

    /**
     * Looks up the api key set on the LookupApiKeyAssertion in a cluster property and sets the lookup result in
     * context variables.
     *
     * @param context the PolicyEnforcementContext.  Never null.
     * @return AssertionStatus.NONE if retrieving lookup results was successful or AssertionStatus.FAILED otherwise.
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *
     */
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws PolicyAssertionException {
        final String apiKeyValue = ExpandVariables.process(assertion.getApiKey(),
                context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());

        String serviceId = null;
        if (StringUtils.isNotBlank(apiKeyValue) && mustMatchServiceId(assertion)) {
            serviceId = ExpandVariables.process(assertion.getServiceId(),
                    context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
            if (StringUtils.isBlank(serviceId)) {
                getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "Service id misconfigured: " + assertion.getServiceId());
                return AssertionStatus.FAILED;
            }
        }

        setContextVariables(context, apiKeyValue, serviceId, assertion);

        return AssertionStatus.NONE;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        LOGGER.log(Level.INFO, "ServerLookupApiKeyAssertion is preparing itself to be unloaded");
    }

    /**
     * Currently supports both ApiKeyData and ApiKey. ApiKey is given precedence. Will only try to look up ApiKeyData if no ApiKey is found.
     */
    private void setContextVariables(final PolicyEnforcementContext context, final String apiKeyValue, final String serviceId,
                                     final LookupApiKeyAssertion assertion) throws PolicyAssertionException {
        final String prefix = assertion.getVariablePrefix();

        if (StringUtils.isNotBlank(apiKeyValue)) {
            ApiKeyData legacyKey = null;
            ApiKey key = null;
            try {
                key = apiKeyManager.find(apiKeyValue);
                if (key == null) {
                    // may be stored as a legacy key
                    legacyKey = legacyApiKeyManager.find(apiKeyValue);
                }
            } catch (final FindException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Unable to look up API key: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e);
            }
            if (key != null || legacyKey != null) {
                //key found
                if (key != null) {
                    try {
                        handleKeyFound(context, apiKeyValue, serviceId, assertion, prefix, key);
                    } catch (final JAXBException e) {
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Unable to marshal API key: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                        throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e);
                    }
                } else {
                    handleKeyFound(context, apiKeyValue, serviceId, assertion, prefix, legacyKey);
                }
            } else {
                // key not found
                LOGGER.fine("Key not found: " + apiKeyValue);
                setContextVariablesNotFound(context, prefix, apiKeyValue);
            }
        } else {
            // key was not specified
            LOGGER.fine("Key is not specified.");
            setContextVariablesNotFound(context, prefix, apiKeyValue);
        }
    }

    private void handleKeyFound(final PolicyEnforcementContext context, final String apiKeyValue, final String serviceId, final LookupApiKeyAssertion assertion, final String prefix, final ApiKeyData legacyKey) {
        if (mustMatchServiceId(assertion)) {
            final String plan = legacyKey.getServiceIds().get(serviceId);
            if (plan != null) {
                // key found and service matches
                LOGGER.fine("Key found with service match: " + apiKeyValue);
                setContextVariablesFound(context, prefix, legacyKey, serviceId);
            } else {
                // key is found but service id does not match
                LOGGER.fine("Key found but service does not match: " + apiKeyValue);
                setContextVariablesNotFound(context, prefix, apiKeyValue);
            }
        } else {
            // key found and no need to match by service id
            LOGGER.fine("Key found: " + apiKeyValue);
            setContextVariablesFound(context, prefix, legacyKey, null);
        }
    }

    private void handleKeyFound(final PolicyEnforcementContext context, final String apiKeyValue, final String serviceId, final LookupApiKeyAssertion assertion, final String prefix, final ApiKey key) throws JAXBException {
        if (mustMatchServiceId(assertion)) {
            final String plan = key.getServiceIds().get(serviceId);
            if (plan != null) {
                // key found and service matches
                LOGGER.fine("Key found with service match: " + apiKeyValue);
                setContextVariablesFound(context, prefix, key, serviceId);
            } else {
                // key is found but service id does not match
                LOGGER.fine("Key found but service does not match: " + apiKeyValue);
                setContextVariablesNotFound(context, prefix, apiKeyValue);
            }
        } else {
            // key found and no need to match by service id
            LOGGER.fine("Key found: " + apiKeyValue);
            setContextVariablesFound(context, prefix, key, null);
        }
    }

    private boolean mustMatchServiceId(final LookupApiKeyAssertion assertion) {
        return StringUtils.isNotBlank(assertion.getServiceId());
    }

    private void setContextVariablesFound(final PolicyEnforcementContext context, final String prefix, final ApiKeyData apiKeyData, final String serviceId) {
        context.setVariable(prefix + "." + LookupApiKeyAssertion.KEY_SUFFIX, apiKeyData.getKey());
        context.setVariable(prefix + "." + LookupApiKeyAssertion.FOUND_SUFFIX, true);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.STATUS_SUFFIX, apiKeyData.getStatus());
        context.setVariable(prefix + "." + LookupApiKeyAssertion.SECRET_SUFFIX, apiKeyData.getSecret());
        context.setVariable(prefix + "." + LookupApiKeyAssertion.XML_SUFFIX, apiKeyData.getXmlRepresentation());
        context.setVariable(prefix + "." + LookupApiKeyAssertion.VERSION_SUFFIX, String.valueOf(apiKeyData.getVersion()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.LABEL_SUFFIX, checkString(apiKeyData.getLabel()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.PLATFORM_SUFFIX, checkString(apiKeyData.getPlatform()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.OAUTH_CALLBACK_SUFFIX, checkString(apiKeyData.getOauthCallbackUrl()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.OAUTH_SCOPE_SUFFIX, checkString(apiKeyData.getOauthScope()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.OAUTH_TYPE_SUFFIX, checkString(apiKeyData.getOauthType()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.ACCOUNT_PLAN_MAPPING_ID_SUFFIX, checkString(apiKeyData.getAccountPlanMappingId()));
        if (StringUtils.isNotBlank(serviceId)) {
            context.setVariable(prefix + "." + LookupApiKeyAssertion.SERVICE_SUFFIX, serviceId);
            context.setVariable(prefix + "." + LookupApiKeyAssertion.PLAN_SUFFIX, apiKeyData.getServiceIds().get(serviceId));
        } else {
            context.setVariable(prefix + "." + LookupApiKeyAssertion.SERVICE_SUFFIX, StringUtils.EMPTY);
            context.setVariable(prefix + "." + LookupApiKeyAssertion.PLAN_SUFFIX, StringUtils.EMPTY);
        }
    }

    private void setContextVariablesFound(final PolicyEnforcementContext context, final String prefix, final ApiKey key, final String serviceId) throws JAXBException {
        context.setVariable(prefix + "." + LookupApiKeyAssertion.KEY_SUFFIX, key.getName());
        context.setVariable(prefix + "." + LookupApiKeyAssertion.FOUND_SUFFIX, true);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.STATUS_SUFFIX, key.getStatus());
        context.setVariable(prefix + "." + LookupApiKeyAssertion.SECRET_SUFFIX, key.getSecret());
        context.setVariable(prefix + "." + LookupApiKeyAssertion.VERSION_SUFFIX, String.valueOf(key.getVersion()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.LABEL_SUFFIX, checkString(key.getLabel()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.PLATFORM_SUFFIX, checkString(key.getPlatform()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.OAUTH_CALLBACK_SUFFIX, checkString(key.getOauthCallbackUrl()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.OAUTH_SCOPE_SUFFIX, checkString(key.getOauthScope()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.OAUTH_TYPE_SUFFIX, checkString(key.getOauthType()));
        context.setVariable(prefix + "." + LookupApiKeyAssertion.ACCOUNT_PLAN_MAPPING_ID_SUFFIX, checkString(key.getAccountPlanMappingId()));
        final ApiKeyResource resource = transformer.entityToResource(key);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.XML_SUFFIX, marshaller.marshal(resource));
        if (StringUtils.isNotBlank(serviceId)) {
            context.setVariable(prefix + "." + LookupApiKeyAssertion.SERVICE_SUFFIX, serviceId);
            context.setVariable(prefix + "." + LookupApiKeyAssertion.PLAN_SUFFIX, key.getServiceIds().get(serviceId));
        } else {
            context.setVariable(prefix + "." + LookupApiKeyAssertion.SERVICE_SUFFIX, StringUtils.EMPTY);
            context.setVariable(prefix + "." + LookupApiKeyAssertion.PLAN_SUFFIX, StringUtils.EMPTY);
        }
    }

    private void setContextVariablesNotFound(final PolicyEnforcementContext context, final String prefix, final String key) {
        context.setVariable(prefix + "." + LookupApiKeyAssertion.KEY_SUFFIX, key);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.FOUND_SUFFIX, false);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.SERVICE_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.PLAN_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.SECRET_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.STATUS_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.XML_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.LABEL_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.PLATFORM_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.OAUTH_CALLBACK_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.OAUTH_SCOPE_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.OAUTH_TYPE_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.VERSION_SUFFIX, StringUtils.EMPTY);
        context.setVariable(prefix + "." + LookupApiKeyAssertion.ACCOUNT_PLAN_MAPPING_ID_SUFFIX, StringUtils.EMPTY);
    }

    private void validateAssertion(final LookupApiKeyAssertion assertion) throws PolicyAssertionException {
        throwIfNullOrBlank(assertion, assertion.getApiKey(), "The api key is not set.");
        throwIfNullOrBlank(assertion, assertion.getVariablePrefix(), "The variable prefix is not set.");
    }

    private void throwIfNullOrBlank(final LookupApiKeyAssertion assertion, final String toCheck,
                                    final String errorMessage) throws PolicyAssertionException {
        if (StringUtils.isBlank(toCheck)) {
            throw new PolicyAssertionException(assertion, errorMessage);
        }
    }

    private String checkString(final String str) {
        if (str == null) {
            return StringUtils.EMPTY;
        } else {
            return str;
        }
    }
}
