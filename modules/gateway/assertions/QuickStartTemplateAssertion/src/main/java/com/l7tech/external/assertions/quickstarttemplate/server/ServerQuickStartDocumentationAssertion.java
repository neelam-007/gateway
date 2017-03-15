package com.l7tech.external.assertions.quickstarttemplate.server;

import com.google.common.annotations.VisibleForTesting;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartDocumentationAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.documentation.QuickStartDocumentationBuilder;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

public class ServerQuickStartDocumentationAssertion extends AbstractServerAssertion<QuickStartDocumentationAssertion> {
    private static final Logger LOGGER = Logger.getLogger(ServerQuickStartDocumentationAssertion.class.getName());
    private QuickStartDocumentationBuilder documentationBuilder = new QuickStartDocumentationBuilder();
    private QuickStartEncapsulatedAssertionLocator assertionLocator;

    // invoked using reflection
    @SuppressWarnings("unused")
    public ServerQuickStartDocumentationAssertion(final QuickStartDocumentationAssertion assertion, final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);
        assertionLocator = QuickStartAssertionModuleLifecycle.getEncapsulatedAssertionLocator();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            LOGGER.warning("JMK Checking documentation request...");
            final Set<EncapsulatedAssertion> encapsulatedAssertions = assertionLocator.findEncapsulatedAssertions();
            LOGGER.warning("JMK Found encapsulated assertions: " + encapsulatedAssertions.size());
            final String documentation = documentationBuilder.generate(encapsulatedAssertions);
            context.setVariable(QuickStartDocumentationAssertion.QS_DOC, documentation);
        } catch (final FindException e) {
            throw new PolicyAssertionException(getAssertion(), e);
        }
        return AssertionStatus.NONE;
    }

    // This is used to inject a mock instance of the assertion locator for testing purposes.
    @VisibleForTesting
    void setAssertionLocator(final QuickStartEncapsulatedAssertionLocator assertionLocator) {
        this.assertionLocator = assertionLocator;
    }

}
