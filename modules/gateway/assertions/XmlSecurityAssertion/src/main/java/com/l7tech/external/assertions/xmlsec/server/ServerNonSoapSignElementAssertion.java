package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
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
 * Server imlementation of signing arbitrary XML elements in a non-SOAP message.
 */
public class ServerNonSoapSignElementAssertion extends ServerNonSoapSecurityAssertion<NonSoapSignElementAssertion> {
    private static final Logger logger = Logger.getLogger(ServerNonSoapSignElementAssertion.class.getName());

    public ServerNonSoapSignElementAssertion(NonSoapSignElementAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) throws InvalidXpathException {
        super(assertion, "sign", logger, beanFactory, eventPub);
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> affectedElements) throws Exception {
        // TODO
        return AssertionStatus.UNDEFINED;
    }
}
