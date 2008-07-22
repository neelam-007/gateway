package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.Operation;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.gateway.common.audit.AssertionMessages;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.xml.sax.SAXException;
import org.springframework.context.ApplicationContext;

import javax.wsdl.WSDLException;

/**
 * Runtime execution of the WSDL Operation assertion. Gets operation from the context and tries to match it against
 * an operation name recorded in the WSDL Operation assertion. Returns AssertionStatus.FALSIFIED if the message matched
 * an operation described in the WSDL but not the one wanted by the assertion, AssertionStatus.FAILED if the message
 * could not match any operation described in the WSDL, and AssertionStatus.NONE if there is a match.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2006<br/>
 *
 * @see Operation
 */
public class ServerOperation extends AbstractServerAssertion {
    private final Operation subject;
    private final Auditor auditor;
    private static final Logger logger = Logger.getLogger(ServerOperation.class.getName());
    public ServerOperation(Operation subject, ApplicationContext context) {
        super(subject);
        this.subject = subject;
        auditor = new Auditor(this, context, logger);
    }

    /**
     * @return AssertionStatus.FALSIFIED if the message matched
     * an operation described in the WSDL but not the one wanted by the assertion, AssertionStatus.FAILED if the message
     * could not match any operation described in the WSDL, and AssertionStatus.NONE if there is a match
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            javax.wsdl.Operation cntxop = context.getOperation();
            if (cntxop != null) {
                String tmp = cntxop.getName();
                if (subject.getOperationName().equals(tmp)) {
                    return AssertionStatus.NONE;
                } else {
                    auditor.logAndAudit(AssertionMessages.WSDLOPERATION_NOMATCH, new String[] {tmp, subject.getOperationName()});
                    return AssertionStatus.FALSIFIED;
                }
            }
        } catch (SAXException e) {
            logger.log(Level.WARNING, "error getting wsdl operation from context", e);
        } catch (InvalidDocumentFormatException e) {
            logger.log(Level.WARNING, "error getting wsdl operation from context", e);
        } catch (WSDLException e) {
            logger.log(Level.WARNING, "error getting wsdl operation from context", e);
        }
        auditor.logAndAudit(AssertionMessages.WSDLOPERATION_CANNOTIDENTIFY);
        return AssertionStatus.FAILED;
    }
}
