package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.ValidateContentTypeAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server assertion to validate the syntax of a message's content type.
 */
public class ServerValidateContentTypeAssertion extends AbstractMessageTargetableServerAssertion<ValidateContentTypeAssertion> {
    private static final Logger logger = Logger.getLogger(ServerValidateContentTypeAssertion.class.getName());
    private final Audit auditor;

    public ServerValidateContentTypeAssertion(final ValidateContentTypeAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) {
        super(assertion, assertion);
        this.auditor = new Auditor(this, beanFactory, eventPub, logger);
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
            throws IOException, PolicyAssertionException
    {
        ContentTypeHeader ctype = message.getMimeKnob().getOuterContentType();
        try {
            ctype.validate();
            return AssertionStatus.NONE;
        } catch (IOException e) {
            auditor.logAndAudit(AssertionMessages.MESSAGE_BAD_CONTENT_TYPE, messageDescription, ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }
    }
}
