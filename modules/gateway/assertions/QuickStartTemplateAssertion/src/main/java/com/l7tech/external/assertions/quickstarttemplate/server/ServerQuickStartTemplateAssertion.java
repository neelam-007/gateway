package com.l7tech.external.assertions.quickstarttemplate.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.Service;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionStringEncoding;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private QuickStartEncapsulatedAssertionLocator assertionLocator;

    // invoked using reflection
    @SuppressWarnings("unused")
    public ServerQuickStartTemplateAssertion( final QuickStartTemplateAssertion assertion, final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);
        assertionLocator = QuickStartAssertionModuleLifecycle.getEncapsulatedAssertionLocator();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        try {
            final ServiceContainer serviceContainer;
            try {
                serviceContainer = parseJson(context.getRequest().getMimeKnob().getEntireMessageBodyAsInputStream());
            } catch (final IOException | NoSuchPartException e) {
                logger.log(Level.WARNING, "Invalid JSON payload: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                context.setVariable(QuickStartTemplateAssertion.QS_WARNINGS, "Invalid JSON payload: " + ExceptionUtils.getMessage(e));
                return AssertionStatus.FAILED;
            }

            try {
                // get all the encapsulated assertions (and their properties) used by the service
                final List<EncapsulatedAssertion> encapsulatedAssertions = getEncapsulatedAssertions(serviceContainer.service, context);
                final PublishedService publishedService = createPublishedService(serviceContainer.service, encapsulatedAssertions);

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

    @VisibleForTesting
    ServiceContainer parseJson(final InputStream is) throws IOException {
        return new ObjectMapper().readValue(is, ServiceContainer.class);
    }

    @NotNull
    private PublishedService createPublishedService(@NotNull final Service service, @NotNull final List<EncapsulatedAssertion> encapsulatedAssertions) {
        final PublishedService publishedService = new PublishedService();
        publishedService.setName(service.name);
        publishedService.setRoutingUri(service.gatewayUri);
        publishedService.setHttpMethods(Sets.newHashSet(service.httpMethods));
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
    @NotNull
    private List<EncapsulatedAssertion> getEncapsulatedAssertions(@NotNull final Service service, @NotNull final PolicyEnforcementContext context)
            throws QuickStartPolicyBuilderException, FindException {
        final List<EncapsulatedAssertion> encapsulatedAssertions = new ArrayList<>();
        for (final Map<String, Map<String, ?>> policyMap : service.policy) {
            // We know there is only one thing in this map, we've previously validated this.
            final String name = policyMap.keySet().iterator().next();
            final EncapsulatedAssertion encapsulatedAssertion = assertionLocator.findEncapsulatedAssertion(name);
            if (encapsulatedAssertion == null) {
                throw new QuickStartPolicyBuilderException("Unable to find encapsulated assertion template named : " + name);
            }
            setEncassArguments(encapsulatedAssertion, policyMap.get(name));
            encapsulatedAssertions.add(encapsulatedAssertion);
        }
        return encapsulatedAssertions;
    }


    private void setEncassArguments(@NotNull final EncapsulatedAssertion encapsulatedAssertion, @NotNull final Map<String, ?> properties) throws QuickStartPolicyBuilderException {
        if (encapsulatedAssertion.config() == null) {
            throw new IllegalStateException("Unable to obtain the encapsulated assertion config object.");
        }

        for (final Map.Entry<String, ?> entry : properties.entrySet()) {
            final EncapsulatedAssertionArgumentDescriptor descriptor = findArgumentDescriptor(entry.getKey(), encapsulatedAssertion);
            if (descriptor == null) {
                throw new QuickStartPolicyBuilderException("Incorrect encapsulated assertion property: " + entry.getKey() + ", for encapsulated assertion: " + encapsulatedAssertion.config().getName());
            }
            // Don't know the type... Can't, so we have to check a number of different types.
            final Object propertyValue = entry.getValue();
            String resultingValue;
            if (propertyValue instanceof Iterable) {
                // If it's an iterable, we cannot pass arrays to encapsulated assertions, so we merge them together
                // like this into a semicolon delimited string.
                resultingValue = Joiner.on(";").join((Iterable) propertyValue);
            } else {
                // Convert the value using the encapsulated assertion encoding type.
                resultingValue = EncapsulatedAssertionStringEncoding.encodeToString(descriptor.dataType(), propertyValue);
                // If we couldn't convert it, try a string as a last resort.
                if (resultingValue == null) {
                    resultingValue = propertyValue.toString();
                }
            }
            encapsulatedAssertion.putParameter(entry.getKey(), resultingValue);
        }
    }

    @Nullable
    private static EncapsulatedAssertionArgumentDescriptor findArgumentDescriptor(@NotNull final String name, @NotNull final EncapsulatedAssertion ea) {
        assert ea.config() != null;
        assert ea.config().getArgumentDescriptors() != null;
        return ea.config().getArgumentDescriptors().stream()
                .filter(ad -> name.equals(ad.getArgumentName()))
                .findFirst()
                .orElse(null);
    }

}
