package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorAssertion;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ExtensibleSocketConnectorAssertion.
 *
 * @see com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorAssertion
 */
public class ServerExtensibleSocketConnectorAssertion extends ServerRoutingAssertion<ExtensibleSocketConnectorAssertion> {
    private static final Logger logger = Logger.getLogger(ServerExtensibleSocketConnectorAssertion.class.getName());

    private final ExtensibleSocketConnectorAssertion assertion;
    private final String[] variablesUsed;

    public ServerExtensibleSocketConnectorAssertion(ExtensibleSocketConnectorAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context);

        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            //If a variable is specified that contains the sessionId then
            //retrieve the sessionId from the variable.
            String sessionId = "";
            if (assertion.getSessionIdVariable() != null && !assertion.getSessionIdVariable().trim().isEmpty()) {
                Map<String, Object> refVariablesMap = context.getVariableMap(variablesUsed, getAudit());
                sessionId = ExpandVariables.process(Syntax.SYNTAX_PREFIX + assertion.getSessionIdVariable() + Syntax.SYNTAX_SUFFIX, refVariablesMap, getAudit(), true);
            }

            //send the message
            Message request = context.getTargetMessage(assertion.getRequestTarget());
            SocketConnectorManager.OutgoingMessageResponse responseDetails = SocketConnectorManager.getInstance().sendMessage(request, assertion.getSocketConnectorGoid(), sessionId, assertion.isFailOnNoSession());

            if (responseDetails != null) {

                //If a variable is specified to store the sessionId to then
                //create the variable and assign it the value of the session id
                if (assertion.getSessionIdStoreVariable() != null && !assertion.getSessionIdStoreVariable().trim().isEmpty()) {
                    context.setVariable(assertion.getSessionIdStoreVariable(), Long.toString(responseDetails.getSessionId()));
                }

                switch (assertion.getResponseTarget().getTarget()) {
                    case REQUEST:
                        context.getRequest().initialize(ContentTypeHeader.create(responseDetails.getContentType()), responseDetails.getMessageBytes());
                        break;
                    case RESPONSE:
                        context.getResponse().initialize(ContentTypeHeader.create(responseDetails.getContentType()), responseDetails.getMessageBytes());
                        break;
                    case OTHER:
                        Message m = new Message();
                        m.initialize(ContentTypeHeader.create(responseDetails.getContentType()), responseDetails.getMessageBytes());
                        context.setVariable(assertion.getResponseTarget().getOtherTargetMessageVariable(), m);
                        break;
                }
            }
        } catch (Exception e) {
            getAudit().logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, e.getMessage());
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerExtensibleSocketConnectorAssertion is preparing itself to be unloaded");
    }
}
