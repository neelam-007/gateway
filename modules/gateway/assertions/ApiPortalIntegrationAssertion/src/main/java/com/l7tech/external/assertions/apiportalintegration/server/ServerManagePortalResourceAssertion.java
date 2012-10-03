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
                ApiKeyResourceHandler.getInstance(context));
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

    ServerManagePortalResourceAssertion(@NotNull final ManagePortalResourceAssertion assertion,
                                        @NotNull final JAXBResourceMarshaller resourceMarshaller,
                                        @NotNull final JAXBResourceUnmarshaller resourceUnmarshaller,
                                        @NotNull final ApiResourceHandler apiResourceHandler,
                                        @NotNull final ApiPlanResourceHandler planResourceHandler,
                                        @NotNull final ApiKeyResourceHandler keyResourceHandler) {
        super(assertion);
        this.resourceMarshaller = resourceMarshaller;
        this.resourceUnmarshaller = resourceUnmarshaller;
        this.apiResourceHandler = apiResourceHandler;
        this.planResourceHandler = planResourceHandler;
        this.keyResourceHandler = keyResourceHandler;
    }

    private final JAXBResourceMarshaller resourceMarshaller;
    private final JAXBResourceUnmarshaller resourceUnmarshaller;
    private final ApiResourceHandler apiResourceHandler;
    private final ApiPlanResourceHandler planResourceHandler;
    private final ApiKeyResourceHandler keyResourceHandler;

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
                case DELETE: {
                    // get and delete MUST have a resource id
                    if (isValidResourceId(stripped)) {
                        manageOperation = new ManageOperation(httpMethod, KEYS_URI, stripped.replaceFirst("/", ""));
                    }
                    break;
                }
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

    private void handleKeys(final PolicyEnforcementContext context, final ManageOperation manageOperation) throws FindException, JAXBException, UpdateException, SaveException, DeleteException {
        switch (manageOperation.httpMethod) {
            case GET: {
                final ApiKeyResource key = keyResourceHandler.get(manageOperation.resourceId);
                if (key != null) {
                    final String xml = resourceMarshaller.marshal(key);
                    setContextVariables(context, 200, SUCCESS, xml);
                } else {
                    final String message = "Cannot find ApiKey with key=" + manageOperation.resourceId;
                    setContextVariables(context, 404, message, null);
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{message});
                }
                break;
            }
            case PUT: {
                final String resourceXml = ExpandVariables.process("${" + RESOURCE + "}", context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
                final ApiKeyResource key = (ApiKeyResource) resourceUnmarshaller.unmarshal(resourceXml, ApiKeyResource.class);
                Validate.notEmpty(key.getKey(), "Resource id missing");
                final ApiKeyResource result = keyResourceHandler.put(key);
                setContextVariables(context, 200, SUCCESS, resourceMarshaller.marshal(result));
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
}
