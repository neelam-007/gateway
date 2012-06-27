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
 * Responsible for logging and auditing error when the MQ jars are not available on the Gateway.  Return SERVER_ERROR.
 * Unlike module load listener which logs a similar issue at FINE, construction of this assertion means there is an instance of the assertion in a policy.
 * <p/>
 * Note: this server assertion is reference by name in the meta data for MqNativeRoutingAssertion.
 */
public class DelegatingServerMqNativeRoutingAssertion extends ServerRoutingAssertion<MqNativeRoutingAssertion> {

    public DelegatingServerMqNativeRoutingAssertion(final MqNativeRoutingAssertion data,
                                                    final ApplicationContext applicationContext) {
        super(data, applicationContext);
        logAndAudit(AssertionMessages.MQ_ROUTING_CONFIGURATION_ERROR, "MQ Native jars are not installed on the Gateway. Assertion will fail if consumed.");
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        logAndAudit(AssertionMessages.MQ_ROUTING_CONFIGURATION_ERROR, "MQ Native jars are not installed on the Gateway");
        return AssertionStatus.SERVER_ERROR;
    }
}
