package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.external.assertions.amqpassertion.RouteViaAMQPAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the RouteViaAMQPAssertion.
 *
 * @see com.l7tech.external.assertions.amqpassertion.RouteViaAMQPAssertion
 */
public class ServerRouteViaAMQPAssertion extends ServerRoutingAssertion<RouteViaAMQPAssertion> {
    private final String[] variablesUsed;
    public static final String AMQP_PREFIX = "amqp.property";
    public static final String DELIVERY_MODE = "delivery_mode";
    public static final String CONTENT_TYPE = "content_type";
    public static final String CONTENT_ENCODING = "content_encoding";
    public static final String PRIORITY = "priority";
    public static final String CORRELATION_ID = "correlation_id";
    public static final String REPLY_TO = "reply_to";
    public static final String EXPIRATION = "expiration";
    public static final String MESSAGE_ID = "message_id";
    public static final String TIMESTAMP = "timestamp";
    public static final String TYPE = "type";
    public static final String USER_ID = "user_id";
    public static final String APP_ID = "app_id";
    public static final String CLUSTER_ID = "cluster_id";
    private static final Logger logger = Logger.getLogger(ServerRouteViaAMQPAssertion.class.getName());

    public ServerRouteViaAMQPAssertion(final RouteViaAMQPAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
        final String routingKey = ExpandVariables.process(assertion.getRoutingKeyExpression(), vars, getAudit(), true);
        HashMap amqpProperties = gatherAmqpProperties(context);

        Message requestMsg;
        Message responseMsg = null;
        try {
            requestMsg = context.getTargetMessage(assertion.getRequestTarget());
        } catch (NoSuchVariableException e) {
            getAudit().logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, assertion.getRequestTarget().getOtherTargetMessageVariable());
            return AssertionStatus.FAILED;
        }

        try {
            responseMsg = context.getTargetMessage(assertion.getResponseTarget());
        } catch (NoSuchVariableException e) {
            switch (assertion.getResponseTarget().getTarget()) {
                case RESPONSE:
                    responseMsg = context.getResponse();
                    break;
                case OTHER:
                    responseMsg = new Message();
                    break;
                default:
                    getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Cannot write the response to the default request message.");
                    return AssertionStatus.FAILED;
            }
        }

        boolean success = ServerAMQPDestinationManager.getInstance(this.applicationContext).queueMessage(
                assertion.getSsgActiveConnectorGoid(),
                routingKey,
                requestMsg,
                responseMsg,
                amqpProperties
        );

        if (success) {
            if (assertion.getResponseTarget().getTarget() == TargetMessageType.OTHER) {
                context.setVariable(assertion.getResponseTarget().getOtherTargetMessageVariable(), responseMsg);
            }

            return AssertionStatus.NONE;
        } else {
            return AssertionStatus.FAILED;
        }
    }

    /**
     * Builds the map of AMQP properties for a message based on what is set as context variables
     *
     * @param context
     * @return
     */
    private HashMap gatherAmqpProperties(PolicyEnforcementContext context) {
        Integer deliveryMode = null;
        try {
            final String delivery = ExpandVariables.process("${" + AMQP_PREFIX + "." + DELIVERY_MODE + "}",
                    context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
            if (null != delivery && !delivery.isEmpty()) {
                deliveryMode = Integer.parseInt(delivery);
            }
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, AMQP_PREFIX + "." + DELIVERY_MODE + ": " + nfe.toString());
        }
        final String encoding = ExpandVariables.process("${" + AMQP_PREFIX + "." + CONTENT_ENCODING + "}",
                context.getVariableMap(getUpdateVars(), getAudit()), getAudit());

        Integer priority = null;
        try {
            final String amqpPriority = ExpandVariables.process("${" + AMQP_PREFIX + "." + PRIORITY + "}",
                    context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
            if (null != amqpPriority && !amqpPriority.isEmpty()) {
                priority = Integer.parseInt(amqpPriority);
            }
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, AMQP_PREFIX + "." + PRIORITY + ": " + nfe.toString());
        }
        final String expiration = ExpandVariables.process("${" + AMQP_PREFIX + "." + EXPIRATION + "}",
                context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
        final String type = ExpandVariables.process("${" + AMQP_PREFIX + "." + TYPE + "}",
                context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
        final String userId = ExpandVariables.process("${" + AMQP_PREFIX + "." + USER_ID + "}",
                context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
        final String appId = ExpandVariables.process("${" + AMQP_PREFIX + "." + APP_ID + "}",
                context.getVariableMap(getUpdateVars(), getAudit()), getAudit());
        final String clusterId = ExpandVariables.process("${" + AMQP_PREFIX + "." + CLUSTER_ID + "}",
                context.getVariableMap(getUpdateVars(), getAudit()), getAudit());

        HashMap amqpProperties = new HashMap();
        if (null != deliveryMode) {
            amqpProperties.put(AMQP_PREFIX + "." + DELIVERY_MODE, deliveryMode);
        }
        if (!encoding.isEmpty()) {
            amqpProperties.put(AMQP_PREFIX + "." + CONTENT_ENCODING, encoding);
        }
        if (null != priority) {
            amqpProperties.put(AMQP_PREFIX + "." + PRIORITY, priority);
        }
        if (!expiration.isEmpty()) {
            amqpProperties.put(AMQP_PREFIX + "." + EXPIRATION, expiration);
        }
        if (!type.isEmpty()) {
            amqpProperties.put(AMQP_PREFIX + "." + TYPE, expiration);
        }
        if (!userId.isEmpty()) {
            amqpProperties.put(AMQP_PREFIX + "." + USER_ID, userId);
        }
        if (!appId.isEmpty()) {
            amqpProperties.put(AMQP_PREFIX + "." + APP_ID, appId);
        }
        if (!clusterId.isEmpty()) {
            amqpProperties.put(AMQP_PREFIX + "." + CLUSTER_ID, clusterId);
        }
        return amqpProperties;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     *
     * DELETEME if not required.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
    }

    public String[] getUpdateVars() {
        return new String[]{
                AMQP_PREFIX + "." + DELIVERY_MODE,
                AMQP_PREFIX + "." + CONTENT_ENCODING,
                AMQP_PREFIX + "." + PRIORITY,
                AMQP_PREFIX + "." + EXPIRATION,
                AMQP_PREFIX + "." + TYPE,
                AMQP_PREFIX + "." + USER_ID,
                AMQP_PREFIX + "." + APP_ID,
                AMQP_PREFIX + "." + CLUSTER_ID
        };
    }
}
