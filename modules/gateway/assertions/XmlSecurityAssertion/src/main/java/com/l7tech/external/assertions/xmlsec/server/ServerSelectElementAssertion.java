package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.SelectElementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.xml.InvalidXpathException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

/**
 *
 */
public class ServerSelectElementAssertion extends ServerNonSoapSecurityAssertion<SelectElementAssertion> {

    public ServerSelectElementAssertion(SelectElementAssertion assertion) throws InvalidXpathException {
        super(assertion);
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> affectedElements) throws Exception {
        if (affectedElements.isEmpty()) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "XPath did not match an element");
            return AssertionStatus.FALSIFIED;
        }

        if (affectedElements.size() > 1) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "XPath selected more than one element");
            return AssertionStatus.FAILED;
        }

        context.setVariable(assertion.getElementVariable(), affectedElements.get(0));
        return AssertionStatus.NONE;
    }        
}
