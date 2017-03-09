package com.l7tech.external.assertions.quickstarttemplate.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionTemplate;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartServiceBuilder;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartServiceBuilderRestmanImpl;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PROOF-OF-CONCEPT!
 *
 * Server side implementation of the QuickStartTemplateAssertion.
 *
 * TODO How to consume the template file during deployment? Can we somehow pull from code repository and perform a cURL post it in the Docker file? In OpenShift? Solve by moving to  into restman?
 *
 *
 * @see com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion
 */
public class ServerQuickStartTemplateAssertion extends AbstractServerAssertion<QuickStartTemplateAssertion> {
    private static final Logger logger = Logger.getLogger(ServerQuickStartTemplateAssertion.class.getName());

    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;
    private QuickStartEncapsulatedAssertionLocator assertionLocator;

    // invoked using reflection
    @SuppressWarnings("unused")
    public ServerQuickStartTemplateAssertion( final QuickStartTemplateAssertion assertion, final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);
        encapsulatedAssertionConfigManager = applicationContext.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class);
        assertionLocator = QuickStartAssertionModuleLifecycle.getEncapsulatedAssertionLocator();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        // TODO get YAML (later)

        final Object serviceObject = getJsonMap(context.getRequest()).get("Service");
        if (!(serviceObject instanceof Map)) {
            throw new PolicyAssertionException(assertion, "Expecting service Map, but instead got " + serviceObject.getClass());
        }

        // get service and encapsulated assertion info from json payload
        final Map serviceMap = (Map) serviceObject;
        final List<EncapsulatedAssertion> encapsulatedAssertions = getEncapsulatedAssertions(serviceMap, context);
        final PublishedService publishedService = createPublishedService(serviceMap, encapsulatedAssertions);
        final QuickStartEncapsulatedAssertionTemplate quickStartEncapsulatedAssertionTemplate = new QuickStartEncapsulatedAssertionTemplate(publishedService, encapsulatedAssertions);

        // set authenticated user credential to use for restman request later
        final User currentUser = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
        final UserBean userBean = new UserBean(currentUser.getProviderId(), currentUser.getLogin());
        userBean.setUniqueIdentifier(currentUser.getId());

        QuickStartServiceBuilder quickStartServiceBuilder;
        try {
            // service builder - restman
            quickStartServiceBuilder = new QuickStartServiceBuilderRestmanImpl(quickStartEncapsulatedAssertionTemplate);

            // service builder - service manager; optionally substituted with com.l7tech.server.policy.ServiceManager
            //    e.g. follow logic in ServiceAPIResourceFactory#createResourceInternal or in com.l7tech.console.panels.AbstractPublishServiceWizard#checkResolutionConflictAndSave
            // QuickStartServiceBuilder quickStartServiceBuilder = new QuickStartServiceBuilderManagerImpl();

            // TODO for next iteration, validate encass output variables match that of the next encass config input? (e.g. validate encass chaining?)

            // use service builder to create service
            context.setVariable(QuickStartTemplateAssertion.QS_BUNDLE, quickStartServiceBuilder.createServiceBundle(Message.class));
            //quickStartServiceBuilder.createService();
        } catch (Exception e) {
            throw new PolicyAssertionException(assertion, e);
        }

        return AssertionStatus.NONE;
    }

    private Map getJsonMap(@NotNull final Message request) throws IOException, PolicyAssertionException {
        try {
            // TODO return a modifiable copy of the map
            return (Map) request.getJsonKnob().getJsonData().getJsonObject();
        } catch (InvalidJsonException e) {
            throw new PolicyAssertionException(assertion, e);
        }
    }

    @NotNull
    private PublishedService createPublishedService(
            @NotNull final Map serviceMap,
            @NotNull final List<EncapsulatedAssertion> encapsulatedAssertions
    ) throws PolicyAssertionException {
        // create and validate
        final PublishedService publishedService = new PublishedService();

        if (!serviceMap.containsKey("name")) {
            throw new PolicyAssertionException(assertion, "No service name provided.");
        }
        publishedService.setName((String) serviceMap.remove("name"));   // remove key/value processed pairs

        if (!serviceMap.containsKey("gatewayUri")) {
            throw new PolicyAssertionException(assertion, "No service gatewayUri provided.");
        }
        publishedService.setRoutingUri((String) serviceMap.remove("gatewayUri"));

        if (!serviceMap.containsKey("httpMethods")) {
            throw new PolicyAssertionException(assertion, "No service httpMethods provided.");
        }
        // TODO hard code for now
        Set<HttpMethod> httpMethods = new TreeSet<>();
        httpMethods.add(HttpMethod.GET);
        httpMethods.add(HttpMethod.POST);
        publishedService.setHttpMethods(httpMethods);
        serviceMap.remove("httpMethods");

        // TODO more validation and fields?

        // generate service policy based of the encaps
        generatePolicy(publishedService, encapsulatedAssertions);

        return publishedService;
    }

    /**
     * Utility method to generate the policy (from the ordered {@code encapsulatedAssertions}) for the specified {@code service}.
     *
     * @param service                   the {@link PublishedService} for which we are goningto generate the policy
     * @param encapsulatedAssertions    ordered list of {@link EncapsulatedAssertion}'s to use as a source when generating the policy
     */
    private void generatePolicy(@NotNull final PublishedService service, @NotNull final List<EncapsulatedAssertion> encapsulatedAssertions) {
        final Policy policy = service.getPolicy();
        final AllAssertion allAss = new AllAssertion();
        encapsulatedAssertions.forEach(allAss::addChild);
        policy.setXml(WspWriter.getPolicyXml(allAss));
    }

    /**
     * For each name
     *    - look up encass by name to get guid
     *    - if applicable set encass argument(s)
     */
    private List<EncapsulatedAssertion> getEncapsulatedAssertions(final Map serviceMap, final PolicyEnforcementContext context) throws PolicyAssertionException {
        final List<EncapsulatedAssertion> encapsulatedAssertions = new ArrayList<>();
        for (Object serviceMapKey : serviceMap.keySet()) {

            // get encass name
            if (!(serviceMapKey instanceof String)) {
                context.setVariable(QuickStartTemplateAssertion.QS_WARNINGS, "Unable to find encapsulated assertion template named : " + serviceMapKey);   // TODO append to existing warnings
                logger.log(Level.WARNING, "Unable to find encapsulated assertion template named : " + serviceMapKey);
                continue;
            }
            final String encassName = (String) serviceMapKey;

            // get encass config
            EncapsulatedAssertionConfig encassConfig;
            try {
                encassConfig = encapsulatedAssertionConfigManager.findByUniqueName(encassName);
                EncapsulatedAssertion assertion =  assertionLocator.findEncapsulatedAssertion(encassName);
                if (encassConfig == null) {
                    context.setVariable(QuickStartTemplateAssertion.QS_WARNINGS, "Unable to find encapsulated assertion template named : " + encassName);   // TODO append to existing warnings
                    logger.log(Level.WARNING, "Unable to find encapsulated assertion template named : " + encassName);
                    continue;
                }

            } catch (FindException e) {
                final String message = "Unable to find encapsulated assertion template: " + ExceptionUtils.getMessage(e);
                context.setVariable(QuickStartTemplateAssertion.QS_WARNINGS, message);   // TODO append to existing warnings
                logger.log(Level.WARNING, message, ExceptionUtils.getDebugException(e));
                continue;
            }

            final EncapsulatedAssertion encass = new EncapsulatedAssertion(encassConfig);

            // set encass arguments
            Object encassObject = serviceMap.get(encassName);
            if (encassObject instanceof Map) {
                setEncassArguments(encass, encassConfig.getArgumentDescriptors(), (Map) encassObject);
            } else if (!(encassObject instanceof Boolean)) {
                throw new PolicyAssertionException(assertion, "Expecting encapsulated assertion Map or Boolean, but instead got " + encassObject.getClass());
            }

            encapsulatedAssertions.add(encass);
        }

        return encapsulatedAssertions;
    }

    // TODO handle nested arguments
    private EncapsulatedAssertion setEncassArguments(@NotNull final EncapsulatedAssertion encass,
                                                     @NotNull final Set<EncapsulatedAssertionArgumentDescriptor> encassArgumentDescriptors,
                                                     @NotNull final Map encassMap) throws PolicyAssertionException {

        for (EncapsulatedAssertionArgumentDescriptor encassArgumentDescriptor : encassArgumentDescriptors) {
            final String argumentName = encassArgumentDescriptor.getArgumentName();

            // validate EncapsulatedAssertionConfig#getArgumentDescriptor with encassMap key-value
            if (!encassMap.containsKey(argumentName)) {
                throw new PolicyAssertionException(assertion, "Unable to find expected argument: " +
                        argumentName + ", for encapsulated assertion: " + encass.getEncapsulatedAssertionConfigName());
            }

            // set encass parameter with encassMap key-value
            // TODO test passing in context variables ${ctx}
            encass.putParameter(argumentName, encassMap.get(argumentName).toString());
        }

        return encass;
    }
}
