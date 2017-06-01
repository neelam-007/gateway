package com.l7tech.external.assertions.quickstarttemplate.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartMapper;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.Service;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.resolution.NonUniqueServiceResolutionException;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Triple;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion.PROPERTY_QS_REGISTRAR_TMS;

/**
 * Server side implementation of the QuickStartTemplateAssertion.
 *
 * @see com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion
 */
@SuppressWarnings("unused")
public class ServerQuickStartTemplateAssertion extends AbstractMessageTargetableServerAssertion<QuickStartTemplateAssertion> {
    private static final Logger logger = Logger.getLogger(ServerQuickStartTemplateAssertion.class.getName());
    private QuickStartEncapsulatedAssertionLocator assertionLocator;
    private QuickStartPublishedServiceLocator serviceLocator;
    private QuickStartParser parser = new QuickStartParser();
    private QuickStartMapper mapper;
    private final ServiceCache serviceCache;

    public ServerQuickStartTemplateAssertion( final QuickStartTemplateAssertion assertion, final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);
        assertionLocator = QuickStartAssertionModuleLifecycle.getEncapsulatedAssertionLocator();
        serviceLocator = QuickStartAssertionModuleLifecycle.getPublishedServiceLocator();
        mapper = new QuickStartMapper(assertionLocator);
        serviceCache = applicationContext.getBean("serviceCache", ServiceCache.class);
    }

    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                          final Message message,
                                          final String messageDescription,
                                          final AuthenticationContext authContext) throws IOException, PolicyAssertionException {

        try {
            final ServiceContainer serviceContainer;
            try {
                serviceContainer = parser.parseJson(message.getMimeKnob().getEntireMessageBodyAsInputStream(false));
            } catch (final Exception e) {
                final IllegalArgumentException arg = ExceptionUtils.getCauseIfCausedBy(e, IllegalArgumentException.class);
                if (arg != null) {
                    logger.log(Level.WARNING, ExceptionUtils.getMessage(arg), ExceptionUtils.getDebugException(arg));
                    context.setVariable(QuickStartTemplateAssertion.QS_WARNINGS, ExceptionUtils.getMessage(arg));
                    return AssertionStatus.FALSIFIED;
                } else {
                    logger.log(Level.WARNING, "Unable to parse JSON payload: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    context.setVariable(QuickStartTemplateAssertion.QS_WARNINGS, "Unable to parse JSON payload: " + ExceptionUtils.getMessage(e));
                    return AssertionStatus.FALSIFIED;
                }
            }

            try {
                // get all the encapsulated assertions (and their properties) used by the service
                final List<EncapsulatedAssertion> encapsulatedAssertions = mapper.getEncapsulatedAssertions(serviceContainer.service);
                PublishedService publishedService;

                if (isUpdate(context, message)) {
                    Goid goid = parseServiceId(context, message);
                    if (goid != null) {
                        publishedService = updatePublishedService(goid, serviceContainer.service, encapsulatedAssertions);
                    } else {
                        publishedService = createPublishedService(serviceContainer.service, encapsulatedAssertions);
                    }
                } else {
                    publishedService = createPublishedService(serviceContainer.service, encapsulatedAssertions);
                }

                final QuickStartServiceBuilder quickStartServiceBuilder = new QuickStartServiceBuilderRestmanImpl(new QuickStartEncapsulatedAssertionTemplate(publishedService, encapsulatedAssertions));
                context.setVariable(QuickStartTemplateAssertion.QS_BUNDLE, quickStartServiceBuilder.createServiceBundle(Message.class));
            } catch (final QuickStartPolicyBuilderException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                context.setVariable(QuickStartTemplateAssertion.QS_WARNINGS, ExceptionUtils.getMessage(e));
                return AssertionStatus.FALSIFIED;
            }
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Unable to create service:" + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new PolicyAssertionException(assertion, e);
        }

        return AssertionStatus.NONE;
    }

    private boolean isUpdate(@NotNull final PolicyEnforcementContext context, @NotNull final Message message) {
        boolean isUpdate = false;
        try {
            isUpdate = (message.isHttpRequest() && message.getHttpRequestKnob().getMethod() == HttpMethod.PUT) || HttpMethod.PUT.toString().equals(String.valueOf(context.getVariable("invokerMethod")));
        } catch (NoSuchVariableException e) {
            // do nothing
        }
        return isUpdate;
    }

    private Goid parseServiceId(@NotNull final PolicyEnforcementContext context, @NotNull final Message message) {
        Goid goid = null;
        if (message.isHttpRequest()) {
            String url = message.getHttpRequestKnob().getRequestUrl();
            Pattern pattern = Pattern.compile("/([0-9a-f]{32})?$");
            Matcher matcher = pattern.matcher(url);

            if ( (matcher.find()) && (matcher.group(1) != null) ) {
                goid = new Goid(matcher.group(1));
            }
        } else {
            try {
                final String serviceId = String.valueOf(context.getVariable("serviceId"));
                goid = new Goid(serviceId.startsWith("/") ? serviceId.substring(1) : serviceId);
            } catch (NoSuchVariableException e) {
                // do nothing
            }
        }
        return goid;
    }

    // TODO in the future, refactor out common code from com.l7tech.console.panels.PublishInternalServiceWizard.saveNonSoapServiceWithResolutionCheck()
    private boolean hasResolutionConflict(final PublishedService newService) throws QuickStartPolicyBuilderException {
        try {
            return ( (!newService.isDisabled()) &&
                     (Goid.isDefault(newService.getGoid()) &&
                     (!generateResolutionReportForNewService(newService).isSuccess())) );
        } catch ( FindException e ) {
            throw new QuickStartPolicyBuilderException("Error checking for service resolution conflict.", e);
        }
    }

    // TODO in the future, refactor out common code from com.l7tech.server.service.ServiceAdminImpl.generateResolutionReportForNewService()
    private ServiceAdmin.ResolutionReport generateResolutionReportForNewService(final PublishedService newService) throws FindException {
        if ( !Goid.isDefault(newService.getGoid())) {
            throw new FindException("Service must not be persistent.");
        }

        final Collection<ServiceAdmin.ConflictInfo> conflictInformation = new ArrayList<>();
        try {
            serviceCache.checkResolution( newService );
        } catch ( NonUniqueServiceResolutionException e ) {
            for ( final Goid serviceGoid : e.getConflictingServices() ) {
                for ( final Triple<String,String,String> parameters : e.getParameters( serviceGoid ) ) {
                    conflictInformation.add( new ServiceAdmin.ConflictInfo(
                            parameters.left,
                            e.getServiceName( serviceGoid, true ),
                            e.getServiceName( serviceGoid, false ),
                            serviceGoid,
                            parameters.right,
                            parameters.middle) );
                }
            }
        } catch ( ServiceResolutionException e ) {
            throw new FindException( ExceptionUtils.getMessage(e) );
        }

        return new ServiceAdmin.ResolutionReport( newService.getRoutingUri()!=null,
                conflictInformation.toArray(new ServiceAdmin.ConflictInfo[conflictInformation.size()]) );
    }

    @NotNull
    private PublishedService createPublishedService(@NotNull final Service service, @NotNull final List<EncapsulatedAssertion> encapsulatedAssertions) throws QuickStartPolicyBuilderException {
        final PublishedService publishedService = new PublishedService();
        publishedService.setName(service.name);
        publishedService.putProperty(PROPERTY_QS_REGISTRAR_TMS, String.valueOf(System.currentTimeMillis()));

        // Set the service URI and fail if there is an existing service with the same URI (service URI conflict resolution)
        publishedService.setRoutingUri(service.gatewayUri);
        if (hasResolutionConflict(publishedService)) {
            throw new QuickStartPolicyBuilderException("Resolution parameters conflict for service '" + publishedService.getName() + "' " +
                    "because an existing service is already using the URI " + publishedService.getRoutingUri() + ". " +
                    "Try publishing this service using a different routing URI.");
        }

        publishedService.setHttpMethods(Sets.newHashSet(service.httpMethods));
        generatePolicy(publishedService, encapsulatedAssertions);

        return publishedService;
    }

    @NotNull
    private PublishedService updatePublishedService(@NotNull Goid goid, @NotNull final Service service,
                                                    @NotNull final List<EncapsulatedAssertion> encapsulatedAssertions) throws FindException, QuickStartPolicyBuilderException {
        PublishedService publishedService = serviceLocator.findByGoid(goid);

        // DO NOT Fail -> if the serviceID of the service to be updated does not exist
        //if (publishedService == null) {
        //    throw new QuickStartPolicyBuilderException("Unable to find a service with ServiceID " + goid.toString());
        //}

        if (publishedService == null) {
            //
            publishedService = createPublishedService(service, encapsulatedAssertions);
            //
        } else {

            publishedService.setName(service.name);
            publishedService.setRoutingUri(service.gatewayUri);
            final String registrarTime = publishedService.getProperty(PROPERTY_QS_REGISTRAR_TMS);
            if (StringUtils.isNotEmpty(registrarTime)) {
                publishedService.putProperty(PROPERTY_QS_REGISTRAR_TMS, String.valueOf(Long.valueOf(registrarTime) + 1));
            } else {
                publishedService.putProperty(PROPERTY_QS_REGISTRAR_TMS, String.valueOf(System.currentTimeMillis()));
            }
            publishedService.setHttpMethods(Sets.newHashSet(service.httpMethods));
            generatePolicy(publishedService, encapsulatedAssertions);
        }
        //
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


    @VisibleForTesting
    void setParser(final QuickStartParser parser) {
        this.parser = parser;
    }

    @VisibleForTesting
    void setMapper(final QuickStartMapper mapper) {
        this.mapper = mapper;
    }

}
