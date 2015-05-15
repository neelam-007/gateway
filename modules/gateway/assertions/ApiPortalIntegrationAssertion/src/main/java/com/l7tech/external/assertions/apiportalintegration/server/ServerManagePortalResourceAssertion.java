package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion.*;

/**
 * Server side implementation of ManagePortalResourceAssertion.
 */
public class ServerManagePortalResourceAssertion extends AbstractServerAssertion<ManagePortalResourceAssertion> {
    public ServerManagePortalResourceAssertion(@NotNull final ManagePortalResourceAssertion assertion, @NotNull final ApplicationContext context) throws JAXBException {
        this(assertion, DefaultJAXBResourceMarshaller.getInstance(),
                DefaultJAXBResourceUnmarshaller.getInstance(),
                ApiResourceHandler.getInstance(context),
                ApiPlanResourceHandler.getInstance(context),
                ApiKeyResourceHandler.getInstance(context),
                ApiKeyDataResourceHandler.getInstance(context),
                AccountPlanResourceHandler.getInstance(context),
                ApiFragmentResourceHandler.getInstance(context),
                new PolicyHelper(context),
                PolicyValidationMarshaller.getInstance());
    }

    /**
     * Assertion will only fail if an unexpected error occurs. It will not fail if the PolicyEnforcementContext has invalid or missing mandatory context variables.
     */
    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus assertionStatus = AssertionStatus.NONE;
        final String foundOp = ExpandVariables.process("${" + OPERATION + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        final String resourceUri = ExpandVariables.process("${" + RESOURCE_URI + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());

        try {
            final ManageOperation manageOperation = validate(foundOp, resourceUri);
            if (APIS_URI.equals(manageOperation.resourceType)) {
                handleApis(context, manageOperation);
            } else if (PLANS_URI.equals(manageOperation.resourceType)) {
                handlePlans(context, manageOperation);
            } else if (KEYS_URI.equals(manageOperation.resourceType)) {
                handleKeys(context, manageOperation);
            } else if (GATEWAY_URI.equals(manageOperation.resourceType)) {
                handleGateway(context, manageOperation);
            } else if (ACCOUNT_PLANS_URI.equals(manageOperation.resourceType)) {
                handleAccountPlans(context, manageOperation);
            } else if(API_FRAGMENTS_URI.equals(manageOperation.resourceType)) {
                handleApiFragments(context, manageOperation);
            } else if (POLICY_UPDATE_URI.equals(manageOperation.resourceType)) {
                assertionStatus = handlePolicyUpdate(context, manageOperation, false);
            } else if (POLICY_VALIDATE_URI.equals(manageOperation.resourceType)) {
                assertionStatus = handlePolicyUpdate(context, manageOperation, true);
            }
        } catch (final IllegalArgumentException e) {
            assertionStatus = handleInvalidRequest(context, e.getMessage());
        } catch (final JAXBException e) {
            assertionStatus = handleError("Error marshalling or unmarshalling portal resource", e);
        } catch (final FindException e) {
            assertionStatus = handleError("Error retrieving portal resource", e);
        } catch (final DeleteException e) {
            assertionStatus = handleError("Error deleting portal resource", e);
        } catch (final ObjectModelException e) {
            assertionStatus = handleError("Error managing portal resource", e);
        }

        return assertionStatus;
    }

    static final String ROOT_URI = "/1/";
    static final String SUCCESS = "success";
    static final String APIS_URI = "apis";
    static final String PLANS_URI = "api/plans";
    static final String KEYS_URI = "api/keys";
    static final String GATEWAY_URI = "gateway";
    static final String ACCOUNT_PLANS_URI = "account/plans";
    static final String API_FRAGMENTS_URI = "api/fragments";
    static final String POLICY_UPDATE_URI = "policy/update";
    static final String POLICY_VALIDATE_URI = "policy/validate";

    ServerManagePortalResourceAssertion(@NotNull final ManagePortalResourceAssertion assertion,
                                        @NotNull final JAXBResourceMarshaller resourceMarshaller,
                                        @NotNull final JAXBResourceUnmarshaller resourceUnmarshaller,
                                        @NotNull final ApiResourceHandler apiResourceHandler,
                                        @NotNull final ApiPlanResourceHandler planResourceHandler,
                                        @NotNull final ApiKeyResourceHandler keyResourceHandler,
                                        @NotNull final ApiKeyDataResourceHandler keyLegacyResourceHandler,
                                        @NotNull final AccountPlanResourceHandler accountPlanResourceHandler,
                                        @NotNull final ApiFragmentResourceHandler apiFragmentResourceHandler,
                                        @NotNull final PolicyHelper policyHelper,
                                        @NotNull final PolicyValidationMarshaller policyValidationMarshaller) {
        super(assertion);
        this.resourceMarshaller = resourceMarshaller;
        this.resourceUnmarshaller = resourceUnmarshaller;
        this.apiResourceHandler = apiResourceHandler;
        this.planResourceHandler = planResourceHandler;
        this.keyResourceHandler = keyResourceHandler;
        this.keyLegacyResourceHandler = keyLegacyResourceHandler;
        this.accountPlanResourceHandler = accountPlanResourceHandler;
        this.policyHelper = policyHelper;
        this.policyValidationMarshaller = policyValidationMarshaller;
        this.apiFragmentResourceHandler = apiFragmentResourceHandler;
    }

    private final JAXBResourceMarshaller resourceMarshaller;
    private final JAXBResourceUnmarshaller resourceUnmarshaller;
    private final ApiResourceHandler apiResourceHandler;
    private final ApiPlanResourceHandler planResourceHandler;
    private final ApiKeyResourceHandler keyResourceHandler;
    private final ApiKeyDataResourceHandler keyLegacyResourceHandler;
    private final AccountPlanResourceHandler accountPlanResourceHandler;
    private final ApiFragmentResourceHandler apiFragmentResourceHandler;
    private final PolicyHelper policyHelper;
    private final PolicyValidationMarshaller policyValidationMarshaller;

    /**
     * If the parameters are valid, returns an appropriate ManageOperation.
     * <p/>
     * The resource id may be null.
     */
    private ManageOperation validate(final String operation, final String resourceUri) {
        Validate.isTrue(!StringUtils.isBlank(operation), "Context variable " + OPERATION + " not set");
        Validate.isTrue(!StringUtils.isBlank(resourceUri), "Context variable " + RESOURCE_URI + " not set");
        Validate.isTrue(resourceUri.startsWith(ROOT_URI), "Invalid resource uri: " + resourceUri);

        HttpMethod httpMethod = null;
        try {
            httpMethod = HttpMethod.valueOf(operation);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Context variable " + OPERATION + " is not a valid http method: " + operation);
        }

        ManageOperation manageOperation = null;

        if (resourceUri.startsWith(ROOT_URI + APIS_URI) && HttpMethod.GET.equals(httpMethod)) {
            final String stripped = resourceUri.replaceFirst(ROOT_URI + APIS_URI, "");
            if (StringUtils.isBlank(stripped)) {
                manageOperation = new ManageOperation(httpMethod, APIS_URI, null);
            } else if (isValidResourceId(stripped)) {
                manageOperation = new ManageOperation(httpMethod, APIS_URI, stripped.replaceFirst("/", ""));
            }
        } else if (resourceUri.startsWith(ROOT_URI + PLANS_URI)) {
            final String stripped = resourceUri.replaceFirst(ROOT_URI + PLANS_URI, "");
            switch (httpMethod) {
                case DELETE: {
                    // delete MUST have a resource id
                    if (isValidResourceId(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, PLANS_URI, stripped.replaceFirst("/", ""));
                    }
                    break;
                }
                case PUT: {
                    // put must NOT have a resource id
                    if (StringUtils.isBlank(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, PLANS_URI, null);
                    }
                    break;
                }
                case GET: {
                    // get may or may not have a resource id
                    if (StringUtils.isBlank(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, PLANS_URI, null);
                    } else if (isValidResourceId(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, PLANS_URI, stripped.replaceFirst("/", ""));
                    }
                }
            }
        } else if (resourceUri.startsWith(ROOT_URI + KEYS_URI)) {
            final String stripped = resourceUri.replaceFirst(ROOT_URI + KEYS_URI, "");
            switch (httpMethod) {
                case PUT: {
                    // put must NOT have a resource id
                    if (StringUtils.isBlank(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, KEYS_URI, null);
                    }
                    break;
                }
                case GET:
                    // get may or may not have a resource id
                    if (StringUtils.isBlank(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, KEYS_URI, null);
                    } else if (isValidResourceId(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, KEYS_URI, stripped.replaceFirst("/", ""));
                    }
                    break;
                case DELETE: {
                    //delete MUST have a resource id
                    if (isValidResourceId(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, KEYS_URI, stripped.replaceFirst("/", ""));
                    }
                    break;
                }
            }
        } else if (resourceUri.startsWith(ROOT_URI + GATEWAY_URI)) {
            final String stripped = resourceUri.replaceFirst(ROOT_URI + GATEWAY_URI, "");
            if(httpMethod == HttpMethod.GET){//we only support GET
                // must NOT have a resource id
                if (StringUtils.isBlank(stripped)) {
                    manageOperation = new ManageOperation(httpMethod, GATEWAY_URI, null);
                }
            }
        } else if (resourceUri.startsWith(ROOT_URI + ACCOUNT_PLANS_URI)) {
            final String stripped = resourceUri.replaceFirst(ROOT_URI + ACCOUNT_PLANS_URI, "");
            switch (httpMethod) {
                case DELETE: {
                    // delete MUST have a resource id
                    if (isValidResourceId(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, ACCOUNT_PLANS_URI, stripped.replaceFirst("/", ""));
                    }
                    break;
                }
                case PUT: {
                    // put must NOT have a resource id
                    if (StringUtils.isBlank(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, ACCOUNT_PLANS_URI, null);
                    }
                    break;
                }
                case GET: {
                    // get may or may not have a resource id
                    if (StringUtils.isBlank(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, ACCOUNT_PLANS_URI, null);
                    } else if (isValidResourceId(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, ACCOUNT_PLANS_URI, stripped.replaceFirst("/", ""));
                    }
                }
            }
        } else if (resourceUri.startsWith(ROOT_URI + POLICY_UPDATE_URI)) {
            final String stripped = resourceUri.replaceFirst(ROOT_URI + POLICY_UPDATE_URI, "");
            if(httpMethod == HttpMethod.PUT){//we only support PUT
                // must NOT have a resource id
                if (StringUtils.isBlank(stripped)) {
                    manageOperation = new ManageOperation(httpMethod, POLICY_UPDATE_URI, null);
                }
            }
        } else if (resourceUri.startsWith(ROOT_URI + POLICY_VALIDATE_URI)) {
            final String stripped = resourceUri.replaceFirst(ROOT_URI + POLICY_VALIDATE_URI, "");
            if(httpMethod == HttpMethod.PUT){//we only support PUT
                // must NOT have a resource id
                if (StringUtils.isBlank(stripped)) {
                    manageOperation = new ManageOperation(httpMethod, POLICY_VALIDATE_URI, null);
                }
            }
        } else if (resourceUri.startsWith(ROOT_URI + API_FRAGMENTS_URI) && HttpMethod.GET.equals(httpMethod)) {
            final String stripped = resourceUri.replaceFirst(ROOT_URI + API_FRAGMENTS_URI, "");
            if (StringUtils.isBlank(stripped)) {
                manageOperation = new ManageOperation(httpMethod, API_FRAGMENTS_URI, null);
            } else if (isValidResourceId(stripped)) {
                manageOperation = new ManageOperation(httpMethod, API_FRAGMENTS_URI, stripped.replaceFirst("/", ""));
            }
        }

        if (manageOperation == null) {
            throw new IllegalArgumentException("Invalid operation and/or resourceUri: " + httpMethod + " - " + resourceUri);
        }
        return manageOperation;
    }

    private void handlePlans(final PolicyEnforcementContext context, final ManageOperation manageOperation) throws JAXBException, ObjectModelException {
        switch (manageOperation.httpMethod) {
            case GET: {
                final List<ApiPlanResource> resources = planResourceHandler.get(createCommonFilters(manageOperation));
                if (manageOperation.resourceId != null && resources.isEmpty()) {
                    final String message = "Cannot find ApiPlan with planId=" + manageOperation.resourceId;
                    setContextVariables(context, 404, message, null);
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
                } else {
                    final String xml = resourceMarshaller.marshal(new ApiPlanListResource(resources));
                    setContextVariables(context, 200, SUCCESS, xml);
                }
                break;
            }
            case PUT: {
                final String resourceXml = ExpandVariables.process("${" + RESOURCE + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
                final ApiPlanListResource inputList = (ApiPlanListResource) resourceUnmarshaller.unmarshal(resourceXml, ApiPlanListResource.class);
                if (!inputList.getApiPlans().isEmpty()) {
                    // ensure all ids are present
                    for (final ApiPlanResource plan : inputList.getApiPlans()) {
                        Validate.notEmpty(plan.getPlanId(), "Resource id missing");
                    }
                    final String deleteOmittedString = ExpandVariables.process("${" + OPTION_REMOVE_OMITTED + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
                    final List<ApiPlanResource> result = planResourceHandler.put(inputList.getApiPlans(), Boolean.valueOf(deleteOmittedString).booleanValue());
                    String updatedResourceXml = "N/A";
                    try {
                        updatedResourceXml = resourceMarshaller.marshal(new ApiPlanListResource(result));
                    } catch (final JAXBException e) {
                        // resource has been persisted already so
                        // we don't want to fail the assertion just because we can't return the resource xml
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Error marshalling portal resource"}, ExceptionUtils.getDebugException(e));
                    }
                    setContextVariables(context, 200, SUCCESS, updatedResourceXml);
                } else {
                    setContextVariables(context, 200, SUCCESS, resourceMarshaller.marshal(new ApiPlanListResource()));
                }
                break;
            }
            case DELETE: {
                try {
                    planResourceHandler.delete(manageOperation.resourceId);
                    setContextVariables(context, 200, SUCCESS, null);
                } catch (final FindException e) {
                    final String message = "Cannot find ApiPlan with planId=" + manageOperation.resourceId;
                    setContextVariables(context, 404, message, null);
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
                }
                break;
            }
        }
    }

    private void handleApiFragments(final PolicyEnforcementContext context, final ManageOperation manageOperation) throws JAXBException, ObjectModelException {
        final List<ApiFragmentResource> resources = apiFragmentResourceHandler.get(createFiltersForApiFragments(manageOperation));
        if (manageOperation.resourceId != null && resources.isEmpty()) {
            final String message = "Cannot find Api Fragment with guid=" + manageOperation.resourceId;
            setContextVariables(context, 404, message, null);
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
        } else {
            final String xml = resourceMarshaller.marshal(new ApiFragmentListResource(resources));
            setContextVariables(context, 200, SUCCESS, xml);
        }
    }

    private void handleApis(final PolicyEnforcementContext context, final ManageOperation manageOperation) throws FindException, JAXBException {
        final List<ApiResource> resources = apiResourceHandler.get(createFiltersForApis(context, manageOperation));
        if (manageOperation.resourceId != null && resources.isEmpty()) {
            final String message = "Cannot find Api with apiId=" + manageOperation.resourceId;
            setContextVariables(context, 404, message, null);
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
        } else {
            final String xml = resourceMarshaller.marshal(new ApiListResource(resources));
            setContextVariables(context, 200, SUCCESS, xml);
        }
    }

    private void handleKeys(final PolicyEnforcementContext context, final ManageOperation manageOperation) throws FindException, JAXBException, UpdateException, SaveException, DeleteException, ObjectModelException {
        switch (manageOperation.httpMethod) {
            case GET: {
                final List<ApiKeyResource> resources = keyResourceHandler.get(createFiltersForApiKeys(context, manageOperation));
                final List<ApiKeyResource> resourcesLegacy = keyLegacyResourceHandler.get(createFiltersForApiKeys(context, manageOperation));
                if (manageOperation.resourceId != null && resources.isEmpty() && resourcesLegacy.isEmpty()) {
                    final String message = "Cannot find ApiKey with key=" + manageOperation.resourceId;
                    setContextVariables(context, 404, message, null);
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
                } else {
                    List<ApiKeyResource> mergedList = new ArrayList<ApiKeyResource>();
                    if(!resources.isEmpty()){
                        mergedList.addAll(resources);
                    }
                    if(!resourcesLegacy.isEmpty()){
                        mergedList.addAll(resourcesLegacy);
                    }
                    final String xml = resourceMarshaller.marshal(new ApiKeyListResource(mergedList));
                    setContextVariables(context, 200, SUCCESS, xml);
                }
                break;
            }
            case PUT: {
                final String resourceXml = ExpandVariables.process("${" + RESOURCE + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
                final ApiKeyListResource inputList = (ApiKeyListResource) resourceUnmarshaller.unmarshal(resourceXml, ApiKeyListResource.class);
                final String deleteOmittedString = ExpandVariables.process("${" + OPTION_REMOVE_OMITTED + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
                boolean removeOmitted = Boolean.valueOf(deleteOmittedString).booleanValue();
                if (!inputList.getApis().isEmpty()) {
                    // ensure all ids are present
                    final Map<String, String> filters = new HashMap<String, String>();
                    //validate that the list has the id/key & secret
                    for (final ApiKeyResource api : inputList.getApis()) {
                        Validate.notEmpty(api.getKey(), "API Key missing");
                        Validate.notEmpty(api.getSecret(), "API Secret missing");
                    }
                    if(!removeOmitted){
                        //now moved all legacyKey in the request to the new format
                        for (final ApiKeyResource api : inputList.getApis()) {
                            final String id = api.getKey();
                            filters.put(AbstractResourceHandler.ID, id);
                            try{
                                ApiKeyResource keyLegacy = keyLegacyResourceHandler.get(id);
                                if(keyLegacy!=null){//if a legacy key, move it to new format
                                    keyLegacyResourceHandler.doDelete(id);
                                    keyResourceHandler.put(keyLegacy);//save it to the new format
                                }
                            }catch(FindException e){
                                //probably not a legacy
                            }
                        }//end of for
                    } else { //if removeOmitted=true, move all legacyKey to new format
                        for (final ApiKeyResource api : keyLegacyResourceHandler.doGet(new HashMap<String, String>())) {
                            keyLegacyResourceHandler.doDelete(api.getKey());
                            keyResourceHandler.put(api);//save it to the new format
                        }
                    }
                    final List<ApiKeyResource> result = keyResourceHandler.put(inputList.getApis(), removeOmitted);
                    String updatedResourceXml = "N/A";
                    try {
                        updatedResourceXml = resourceMarshaller.marshal(new ApiKeyListResource(result));
                    } catch (final JAXBException e) {
                        // resource has been persisted already so
                        // we don't want to fail the assertion just because we can't return the resource xml
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Error marshalling portal resource"}, ExceptionUtils.getDebugException(e));
                    }
                    setContextVariables(context, 200, SUCCESS, updatedResourceXml);
                } else {
                    setContextVariables(context, 200, SUCCESS, resourceMarshaller.marshal(new ApiKeyListResource()));
                }
                break;
            }
            case DELETE: {
                try {
                    keyResourceHandler.delete(manageOperation.resourceId);
                    setContextVariables(context, 200, SUCCESS, null);
                } catch (final FindException e) {
                    final String message = "Cannot find ApiKey with key=" + manageOperation.resourceId;
                    setContextVariables(context, 404, message, null);
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
                }
            }
        }
    }

    private void handleGateway(final PolicyEnforcementContext context, final ManageOperation manageOperation) throws JAXBException, ObjectModelException {
        if(manageOperation.httpMethod==HttpMethod.GET){
            GatewayResource gatewayResource = new GatewayResource();
            GatewayStatResource apiStat = new GatewayStatResource();
                apiStat.setCount(String.valueOf(apiResourceHandler.get(new HashMap<String, String>()).size()));
                apiStat.setCacheItems(String.valueOf(apiResourceHandler.getCacheItems()));
            GatewayStatResource apiPlanStat = new GatewayStatResource();
                apiPlanStat.setCount(String.valueOf(planResourceHandler.get(new HashMap<String, String>()).size()));
                apiPlanStat.setCacheItems(String.valueOf(planResourceHandler.getCacheItems()));
            GatewayStatResource apiKeyStat = new GatewayStatResource();
                apiKeyStat.setCount(String.valueOf(keyResourceHandler.get(new HashMap<String, String>()).size()));
                apiKeyStat.setCacheItems(String.valueOf(keyResourceHandler.getCacheItems()));
            GatewayStatResource apiLegacyKeyStat = new GatewayStatResource();
                apiLegacyKeyStat.setCount(String.valueOf(keyLegacyResourceHandler.get(new HashMap<String, String>()).size()));
                apiLegacyKeyStat.setCacheItems(String.valueOf(keyLegacyResourceHandler.getCacheItems()));
            GatewayStatResource accountPlanStat = new GatewayStatResource();
                accountPlanStat.setCount(String.valueOf(accountPlanResourceHandler.get(new HashMap<String, String>()).size()));
                accountPlanStat.setCacheItems(String.valueOf(accountPlanResourceHandler.getCacheItems()));
            GatewayStatResource apiFragmentStat = new GatewayStatResource();
                apiFragmentStat.setCount(String.valueOf(apiFragmentResourceHandler.get(new HashMap<String, String>()).size()));
                apiFragmentStat.setCacheItems(String.valueOf(apiFragmentResourceHandler.getCacheItems()));
            gatewayResource.setApi(apiStat);
            gatewayResource.setApiKey(apiKeyStat);
            gatewayResource.setApiPlan(apiPlanStat);
            gatewayResource.setApiLegacyKey(apiLegacyKeyStat);
            gatewayResource.setAccountPlan(accountPlanStat);
            gatewayResource.setApiFragment(apiFragmentStat);
            setContextVariables(context, 200, SUCCESS, resourceMarshaller.marshal(gatewayResource));
        }
    }

    private void handleAccountPlans(final PolicyEnforcementContext context, final ManageOperation manageOperation) throws JAXBException, ObjectModelException {
        switch (manageOperation.httpMethod) {
            case GET: {
                final List<AccountPlanResource> resources = accountPlanResourceHandler.get(createCommonFilters(manageOperation));
                if (manageOperation.resourceId != null && resources.isEmpty()) {
                    final String message = "Cannot find AccountPlan with planId=" + manageOperation.resourceId;
                    setContextVariables(context, 404, message, null);
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
                } else {
                    final String xml = resourceMarshaller.marshal(new AccountPlanListResource(resources));
                    setContextVariables(context, 200, SUCCESS, xml);
                }
                break;
            }
            case PUT: {
                final String resourceXml = ExpandVariables.process("${" + RESOURCE + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
                final AccountPlanListResource inputList = (AccountPlanListResource) resourceUnmarshaller.unmarshal(resourceXml, AccountPlanListResource.class);
                if (!inputList.getAccountPlans().isEmpty()) {
                    // ensure all ids are present
                    for (final AccountPlanResource plan : inputList.getAccountPlans()) {
                        Validate.notEmpty(plan.getPlanId(), "Resource id missing");
                    }
                    final String deleteOmittedString = ExpandVariables.process("${" + OPTION_REMOVE_OMITTED + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
                    final List<AccountPlanResource> result = accountPlanResourceHandler.put(inputList.getAccountPlans(), Boolean.valueOf(deleteOmittedString).booleanValue());
                    String updatedResourceXml = "N/A";
                    try {
                        updatedResourceXml = resourceMarshaller.marshal(new AccountPlanListResource(result));
                    } catch (final JAXBException e) {
                        // resource has been persisted already so
                        // we don't want to fail the assertion just because we can't return the resource xml
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Error marshalling portal resource"}, ExceptionUtils.getDebugException(e));
                    }
                    setContextVariables(context, 200, SUCCESS, updatedResourceXml);
                } else {
                    setContextVariables(context, 200, SUCCESS, resourceMarshaller.marshal(new AccountPlanListResource()));
                }
                break;
            }
            case DELETE: {
                try {
                    accountPlanResourceHandler.delete(manageOperation.resourceId);
                    setContextVariables(context, 200, SUCCESS, null);
                } catch (final FindException e) {
                    final String message = "Cannot find AccountPlan with planId=" + manageOperation.resourceId;
                    setContextVariables(context, 404, message, null);
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
                }
                break;
            }
        }
    }

    private AssertionStatus handlePolicyUpdate(final PolicyEnforcementContext context, final ManageOperation manageOperation, final boolean validateOnly) {
        final PolicyUpdateResult status = new PolicyUpdateResult(AssertionStatus.FAILED);
        if (manageOperation.httpMethod == HttpMethod.PUT) {
            final String resourceXml = ExpandVariables.process("${" + RESOURCE + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
            final String guid = ExpandVariables.process("${" + OPTION_POLICY_GUID + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
            Validate.notEmpty(guid, "GUID is missing");
            Validate.notEmpty(resourceXml, "Policy resource XML is missing");
            try {
                final PolicyHelper.OperationResult result;
                if(validateOnly){
                    result = policyHelper.validatePolicy(guid, resourceXml);
                } else {
                    String userLogin=null;
                    if(context.getDefaultAuthenticationContext()!=null){
                        final Principal user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
                        if(user!=null){
                            userLogin = user.getName() != null ? user.getName() : "";
                        }
                    }
                    result = policyHelper.updatePolicy(guid, resourceXml, userLogin);
                }
                if (result.hasError()) {
                    setContextVariables(context, 500, result.getResult(), policyValidationMarshaller.marshal(result.getPolicyValidationResult()));
                } else {
                    setContextVariables(context, 200, SUCCESS, policyValidationMarshaller.marshal(result.getPolicyValidationResult()));
                    status.setStatus(AssertionStatus.NONE);
                }
            } catch (Exception e) {
                final String message = "Error Updating Policy : "+e.getMessage();
                setContextVariables(context, 500, message, null);
            }
        }
        return status.getStatus();
    }

    private boolean isValidResourceId(final String toValidate) {
        boolean isValid = false;
        if (toValidate.startsWith("/") && StringUtils.countMatches(toValidate, "/") == 1) {
            isValid = true;
        }
        return isValid;
    }

    private AssertionStatus handleInvalidRequest(final PolicyEnforcementContext context, final String message) {
        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
        setContextVariables(context, 400, message, null);
        return AssertionStatus.NONE;
    }

    private AssertionStatus handleError(final String message, final Throwable error) {
        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message}, ExceptionUtils.getDebugException(error));
        return AssertionStatus.FAILED;
    }

    private void setContextVariables(final PolicyEnforcementContext context, final int responseStatus, final String message, final String xml) {
        context.setVariable(RESPONSE_RESOURCE, xml);
        context.setVariable(RESPONSE_STATUS, responseStatus);
        context.setVariable(RESPONSE_DETAIL, message);
    }

    private Map<String, String> createCommonFilters(final ManageOperation manageOperation) {
        final Map<String, String> filters = new HashMap<String, String>();
        if (StringUtils.isNotBlank(manageOperation.resourceId)) {
            filters.put(AbstractResourceHandler.ID, manageOperation.resourceId);
        }
        return filters;
    }

    private Map<String, String> createFiltersForApis(final PolicyEnforcementContext context, final ManageOperation manageOperation) {
        final Map<String, String> filters = createCommonFilters(manageOperation);
        final String apiGroup = ExpandVariables.process("${" + OPTION_API_GROUP + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        if (StringUtils.isNotBlank(apiGroup)) {
            filters.put(ApiResourceHandler.API_GROUP, apiGroup);
        }
        return filters;
    }

    private Map<String, String> createFiltersForApiFragments(final ManageOperation manageOperation) {
        final Map<String, String> filters = new HashMap<String, String>();
        if (StringUtils.isNotBlank(manageOperation.resourceId)) {
            filters.put(ApiFragmentResourceHandler.GUID, manageOperation.resourceId);
        }
        return filters;
    }

    private Map<String, String> createFiltersForApiKeys(final PolicyEnforcementContext context, final ManageOperation manageOperation) {
        final Map<String, String> filters = createCommonFilters(manageOperation);
        final String apiKeyStatus = ExpandVariables.process("${" + OPTION_API_KEY_STATUS + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        if (StringUtils.isNotBlank(apiKeyStatus)) {
            filters.put(ApiKeyResourceHandler.APIKEY_STATUS, apiKeyStatus);
            filters.put(ApiKeyDataResourceHandler.APIKEY_STATUS, apiKeyStatus);
        }
        return filters;
    }

    private class ManageOperation {
        HttpMethod httpMethod;
        String resourceType;
        String resourceId;

        public ManageOperation(final HttpMethod httpMethod, final String resourceType, final String resourceId) {
            this.httpMethod = httpMethod;
            this.resourceType = resourceType;
            this.resourceId = resourceId;
        }
    }

    private class PolicyUpdateResult {
        public PolicyUpdateResult(AssertionStatus status) {
            this.status = status;
        }

        public AssertionStatus getStatus() {
            return status;
        }

        public void setStatus(AssertionStatus status) {
            this.status = status;
        }

        private AssertionStatus status = AssertionStatus.FAILED;
    }
}
