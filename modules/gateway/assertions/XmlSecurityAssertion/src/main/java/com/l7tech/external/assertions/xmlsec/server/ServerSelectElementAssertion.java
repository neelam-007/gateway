package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.SelectElementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.xml.InvalidXpathException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class ServerSelectElementAssertion extends ServerNonSoapSecurityAssertion<SelectElementAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSelectElementAssertion.class.getName());

    public ServerSelectElementAssertion(SelectElementAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) throws InvalidXpathException {
        super(assertion, logger, beanFactory, eventPub);
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> affectedElements) throws Exception {
        if (affectedElements.isEmpty()) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "XPath did not match an element");
            return AssertionStatus.FALSIFIED;
        }

        if (affectedElements.size() > 1) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "XPath selected more than one element");
            return AssertionStatus.FAILED;
        }

        context.setVariable(assertion.getElementVariable(), affectedElements.get(0));
        return AssertionStatus.NONE;
    }        
}
