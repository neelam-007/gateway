package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.Operation;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.common.xml.InvalidDocumentFormatException;

import java.io.IOException;

import org.xml.sax.SAXException;
import org.springframework.context.ApplicationContext;

import javax.wsdl.WSDLException;

/**
 * [desc here]
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2006<br/>
 */
public class ServerOperation extends AbstractServerAssertion {
    private final Operation subject;
    public ServerOperation(Operation subject, ApplicationContext context) {
        super(subject);
        this.subject = subject;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            if (subject.getOperationName().equals(context.getOperation().getName())) {
                return AssertionStatus.NONE;
            } else {
                return AssertionStatus.FALSIFIED;
            }
        } catch (SAXException e) {
            // todo
        } catch (InvalidDocumentFormatException e) {
            // todo
        } catch (WSDLException e) {
            // todo
        }
        return AssertionStatus.FAILED;
    }
}
