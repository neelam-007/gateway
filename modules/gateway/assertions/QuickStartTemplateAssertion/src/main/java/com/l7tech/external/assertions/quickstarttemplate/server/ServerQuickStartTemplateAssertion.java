package com.l7tech.external.assertions.quickstarttemplate.server;

import com.google.common.annotations.VisibleForTesting;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartPolicyBuilderException;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartServiceBuilder;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.RestmanServiceBundleBuilder;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
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
@SuppressWarnings({"unused", "WeakerAccess"})
public class ServerQuickStartTemplateAssertion extends AbstractMessageTargetableServerAssertion<QuickStartTemplateAssertion> {
    private static final Logger logger = Logger.getLogger(ServerQuickStartTemplateAssertion.class.getName());
    private QuickStartParser parser = new QuickStartParser();
    private QuickStartServiceBuilder serviceBuilder;

    public ServerQuickStartTemplateAssertion(final QuickStartTemplateAssertion assertion, final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);
        this.serviceBuilder = QuickStartAssertionModuleLifecycle.getServiceBuilder();

        // QuickStartAssertionModuleLifecycle.onModuleLoaded# is too early
        //      - "Error creating bean with name 'assertionRegistry': Requested bean is currently in creation: Is there an unresolvable circular reference?"
        serviceBuilder.setAssertionRegistry(applicationContext.getBean("assertionRegistry", AssertionRegistry.class));
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
                    context.setVariable(QuickStartTemplateAssertion.QS_WARNINGS, StringEscapeUtils.escapeJava(ExceptionUtils.getMessage(arg)));
                    return AssertionStatus.FALSIFIED;
                } else {
                    logger.log(Level.WARNING, "Unable to parse JSON payload: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    context.setVariable(QuickStartTemplateAssertion.QS_WARNINGS, "Unable to parse JSON payload");
                    return AssertionStatus.FALSIFIED;
                }
            }

            try {
                final PublishedService publishedService;
                if (isUpdate(context, message)) {
                    final Goid goid = parseServiceId(context, message);
                    if (goid != null) {
                        publishedService = updatePublishedService(goid, serviceContainer);
                    } else {
                        publishedService = createPublishedService(serviceContainer);
                    }
                } else {
                    publishedService = createPublishedService(serviceContainer);
                }

                final RestmanServiceBundleBuilder bundleBuilder = new RestmanServiceBundleBuilder(publishedService);
                context.setVariable(QuickStartTemplateAssertion.QS_BUNDLE, bundleBuilder.createBundle(Message.class));
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

    // TODO: add jdoc
    private boolean isUpdate(@NotNull final PolicyEnforcementContext context, @NotNull final Message message) {
        boolean isUpdate = false;
        try {
            isUpdate = (message.isHttpRequest() && message.getHttpRequestKnob().getMethod() == HttpMethod.PUT) || HttpMethod.PUT.toString().equals(String.valueOf(context.getVariable("invokerMethod")));
        } catch (NoSuchVariableException e) {
            // do nothing
        }
        return isUpdate;
    }

    // TODO: add jdoc
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

    /**
     * Utility method that actually invokes {@link QuickStartServiceBuilder#createService(ServiceContainer)}
     * and puts the appropriate {@link PublishedService service} properties:
     * <ul>
     *     <li>{@link QuickStartTemplateAssertion#PROPERTY_QS_REGISTRAR_TMS PROPERTY_QS_REGISTRAR_TMS} to {@link System#currentTimeMillis()}</li>
     *     <li>TODO: perhaps it should also put {@link QuickStartTemplateAssertion#PROPERTY_QS_CREATE_METHOD PROPERTY_QS_CREATE_METHOD} to {@link QuickStartTemplateAssertion.QsServiceCreateMethod#SCALAR SCALAR}</li>
     * </ul>
     *
     * @see QuickStartServiceBuilder#createService(ServiceContainer)
     */
    @NotNull
    private PublishedService createPublishedService(@NotNull final ServiceContainer serviceContainer) throws QuickStartPolicyBuilderException, FindException {
        final PublishedService publishedService = serviceBuilder.createService(serviceContainer);
        publishedService.putProperty(PROPERTY_QS_REGISTRAR_TMS, String.valueOf(System.currentTimeMillis()));
        // TODO: perhaps add PROPERTY_QS_CREATE_METHOD property and set it to QuickStartTemplateAssertion.QsServiceCreateMethod.SCALAR:
        //publishedService.putProperty(PROPERTY_QS_CREATE_METHOD, String.valueOf(QuickStartTemplateAssertion.QsServiceCreateMethod.SCALAR));
        return publishedService;
    }

    /**
     * Utility method that actually invokes {@link QuickStartServiceBuilder#updateService(Goid, ServiceContainer)}
     * and increments {@link PublishedService service} {@link QuickStartTemplateAssertion#PROPERTY_QS_REGISTRAR_TMS PROPERTY_QS_REGISTRAR_TMS}
     * property to 1 (one).
     *
     * @see QuickStartServiceBuilder#updateService(Goid, ServiceContainer)
     */
    @NotNull
    private PublishedService updatePublishedService(
            @NotNull Goid goid,
            @NotNull final ServiceContainer serviceContainer
    ) throws QuickStartPolicyBuilderException, FindException {
        final PublishedService publishedService = serviceBuilder.updateService(goid, serviceContainer);
        // update PROPERTY_QS_REGISTRAR_TMS
        final String registrarTime = publishedService.getProperty(PROPERTY_QS_REGISTRAR_TMS);
        if (StringUtils.isNotEmpty(registrarTime)) {
            publishedService.putProperty(PROPERTY_QS_REGISTRAR_TMS, String.valueOf(Long.valueOf(registrarTime) + 1));
        } else {
            publishedService.putProperty(PROPERTY_QS_REGISTRAR_TMS, String.valueOf(System.currentTimeMillis()));
        }
        // TODO: perhaps make sure PROPERTY_QS_CREATE_METHOD property is added and its value is QuickStartTemplateAssertion.QsServiceCreateMethod.SCALAR:
        //publishedService.putProperty(PROPERTY_QS_CREATE_METHOD, String.valueOf(QuickStartTemplateAssertion.QsServiceCreateMethod.SCALAR));
        return publishedService;
    }

    @VisibleForTesting
    void setParser(final QuickStartParser parser) {
        this.parser = parser;
    }

    @VisibleForTesting
    void setServiceBuilder(final QuickStartServiceBuilder serviceBuilder) {
        this.serviceBuilder = serviceBuilder;
    }

}
