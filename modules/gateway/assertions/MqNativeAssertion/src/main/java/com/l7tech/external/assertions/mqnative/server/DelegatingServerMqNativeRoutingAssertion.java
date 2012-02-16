package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * Responsible for creating and delegating calls to checkRequest to {@link ServerMqNativeRoutingAssertion} provided
 * the MQ jars are available on the Gateway. If they are not available then this server assertion will log and audit
 * the issue and return SERVER_ERROR.
 * <p/>
 * Note: this server assertion is reference by name in the meta data for MqNativeRoutingAssertion.
 */
public class DelegatingServerMqNativeRoutingAssertion extends ServerRoutingAssertion<MqNativeRoutingAssertion> {

    public DelegatingServerMqNativeRoutingAssertion(final MqNativeRoutingAssertion data,
                                                    final ApplicationContext applicationContext) {
        super(data, applicationContext);

        ServerMqNativeRoutingAssertion serverMqNativeRoutingAssertion = null;
        try {
            serverMqNativeRoutingAssertion = new ServerMqNativeRoutingAssertion(data, applicationContext);
        } catch (NoClassDefFoundError e) {
            // expected when jars not installed
            // unlike module load which logs a similar issue at FINE, construction of this assertion means there is an
            // instance of the assertion in a policy.
            logAndAudit(AssertionMessages.MQ_ROUTING_CONFIGURATION_ERROR, "MQ Native jars are not installed on the Gateway. Assertion will fail if consumed.");
        }

        mqNativeRoutingAssertion = serverMqNativeRoutingAssertion;
    }

    @Override
    protected void injectDependencies() {
        if (mqNativeRoutingAssertion != null) {
            inject(mqNativeRoutingAssertion);
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        if (mqNativeRoutingAssertion != null) {
            return mqNativeRoutingAssertion.checkRequest(context);
        }

        logAndAudit(AssertionMessages.MQ_ROUTING_CONFIGURATION_ERROR, "MQ Native jars are not installed on the Gateway");
        return AssertionStatus.SERVER_ERROR;
    }

    // - PRIVATE
    private final ServerMqNativeRoutingAssertion mqNativeRoutingAssertion;
}
