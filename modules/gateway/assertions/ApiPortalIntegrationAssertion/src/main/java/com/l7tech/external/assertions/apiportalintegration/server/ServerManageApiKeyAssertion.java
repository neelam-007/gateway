package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ManageApiKeyAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManagerFactory;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyXmlUtil;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.HashMap;
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
    private final ApiKeyResourceHandler keyResourceHandler;
    private final ApiKeyDataResourceHandler keyLegacyResourceHandler;

    public ServerManageApiKeyAssertion(ManageApiKeyAssertion assertion, ApplicationContext context) throws PolicyAssertionException, JAXBException {
        this(assertion, context, null, ApiKeyResourceHandler.getInstance(context), ApiKeyDataResourceHandler.getInstance(context));
    }

    /*
     * For tests.
     */
    ServerManageApiKeyAssertion(ManageApiKeyAssertion assertion, ApplicationContext context,
                                @Nullable final PortalGenericEntityManager<ApiKeyData> apiKeyManager,
                                @NotNull final ApiKeyResourceHandler keyResourceHandler,
                                @NotNull final ApiKeyDataResourceHandler keyLegacyResourceHandler
                                ) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
        this.action = assertion.getAction();
        this.apiKeyManager = apiKeyManager != null ? apiKeyManager : ApiKeyManagerFactory.getInstance();
        this.keyResourceHandler = keyResourceHandler;
        this.keyLegacyResourceHandler = keyLegacyResourceHandler;
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

            ApiKeyResource data1 = keyLegacyResourceHandler.get(apiKey);
            ApiKeyResource data2 = keyResourceHandler.get(apiKey);

            // perform specified action

            // do Add
            if (ManageApiKeyAssertion.ACTION_ADD.equals(action)) {

                //we are still expecting legacy key request for this Add Action
                ApiKeyData data = new ApiKeyData(apiKeyXml);
                ApiKeyXmlUtil.updateApiKeyDataFromXml(data, apiKeyXml);
                data.setKey(apiKey);
                ApiKeyResource apiKeyResource = ApiKeyDataResourceTransformer.getInstance().entityToResource(data);

                try {
                    if(data1!=null){
                        keyLegacyResourceHandler.delete(apiKey);
                        keyResourceHandler.put(data1);
                    }
                    if(data1!=null || data2!=null){
                        throw new DuplicateObjectException();
                    }
                    keyResourceHandler.put(apiKeyResource);
                } catch (DuplicateObjectException e) {
                    // error case, duplicate key error
                    logger.warning("Duplicate key error");
                    return AssertionStatus.FALSIFIED;
                }

                context.setVariable(assertion.getVariablePrefix() + "." + ManageApiKeyAssertion.SUFFIX_KEY, apiKey);

            } else if (ManageApiKeyAssertion.ACTION_UPDATE.equals(action)) {

                try {
                    if (data1 != null) {
                        keyLegacyResourceHandler.delete(apiKey);
                        keyResourceHandler.put(data1);//move this legacy key to new format
                        data2 = keyResourceHandler.get(apiKey);
                    } else if (data1 == null && data2 == null) {
                        throw new ObjectNotFoundException();
                    }
                    if (data2 == null) {//data2 should never be null
                        throw new ObjectNotFoundException();
                    }
                    final String keySecret = ExpandVariables.process("${" + API_KEY_SECRET + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    final String keyStatus = ExpandVariables.process("${" + API_KEY_STATUS + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    final String assignApis = ExpandVariables.process("${" + ASSIGN_APIS + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    final String label = ExpandVariables.process("${" + API_LABEL + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    final String callbackUrl = ExpandVariables.process("${" + API_CALLBACK_URL + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    final String oauthScope = ExpandVariables.process("${" + API_OAUTH_SCOPE + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    final String oauthType = ExpandVariables.process("${" + API_OAUTH_TYPE + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    final String platform = ExpandVariables.process("${" + API_PLATFORM + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    final String accountPlanMappingId = ExpandVariables.process("${" + API_ACCOUNT_PLAN_MAPPING_ID + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    final String customMetaData = ExpandVariables.process("${" + API_CUSTOM_METADATA + "}", context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
                    if (StringUtils.isNotBlank(assignApis)) {
                        Map<String, String> services = new HashMap<String, String>();
                        if (assignApis.length() > 0) {
                            for (String str : assignApis.split("\\+\\+")) {
                                String[] api = str.split(";");
                                services.put(api[0], api[1]);
                            }
                        }
                        data2.setApis(services);
                    } else if (StringUtils.isNotBlank(keyStatus)) {
                        data2.setStatus(keyStatus);
                    } else if (StringUtils.isNotBlank(keySecret)) {
                        data2.setSecret(keySecret);
                    } else { //other info
                        data2.setLabel(label);
                        data2.setPlatform(platform);
                        SecurityDetails securityDetails = new SecurityDetails();
                        OAuthDetails oAuthDetails = new OAuthDetails();
                        oAuthDetails.setCallbackUrl(callbackUrl);
                        oAuthDetails.setScope(oauthScope);
                        oAuthDetails.setType(oauthType);
                        securityDetails.setOauth(oAuthDetails);
                        data2.setSecurity(securityDetails);
                        //fail safe that the accountPlanMappingId will never be empty on update
                        if(accountPlanMappingId!=null && accountPlanMappingId.length()>0){
                            data2.setAccountPlanMappingId(accountPlanMappingId);
                        }
                        data2.setCustomMetaData(customMetaData);
                    }
                    keyResourceHandler.put(data2);
                } catch (ObjectNotFoundException e) {
                    // error case, key mapping not found
                    logger.log(Level.WARNING, "Key Entry not found for update: {0}", apiKey);
                    return AssertionStatus.FALSIFIED;
                }

            } else if (ManageApiKeyAssertion.ACTION_REMOVE.equals(action)) {

                try {
                    if (data1 != null) {
                        keyLegacyResourceHandler.delete(apiKey);
                    }
                    if (data2 != null) {
                        keyResourceHandler.delete(apiKey);
                    }
                    if(data1==null && data2==null){
                        throw new ObjectNotFoundException();
                    }
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

    private String[] getUpdateVars() {
        return new String[]{API_KEY_SECRET, API_KEY_STATUS, ASSIGN_APIS, API_LABEL, API_CALLBACK_URL, API_OAUTH_SCOPE, API_OAUTH_TYPE, API_PLATFORM, API_ACCOUNT_PLAN_MAPPING_ID, API_CUSTOM_METADATA};
    }

    public static final String API_KEY_SECRET = "pman.options.apikeySecret";
    public static final String API_KEY_STATUS = "pman.options.apikeyStatus";
    public static final String ASSIGN_APIS = "pman.options.assignApis";
    public static final String API_LABEL = "pman.options.label";
    public static final String API_CALLBACK_URL = "pman.options.callbackUrl";
    public static final String API_OAUTH_SCOPE = "pman.options.oauthScope";
    public static final String API_OAUTH_TYPE = "pman.options.oauthType";
    public static final String API_PLATFORM = "pman.options.platform";
    public static final String API_ACCOUNT_PLAN_MAPPING_ID = "pman.options.accountPlanMappingId";
    public static final String API_CUSTOM_METADATA = "pman.options.customMetaData";

}
