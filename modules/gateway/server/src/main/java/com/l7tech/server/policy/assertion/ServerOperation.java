package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.Operation;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.gateway.common.audit.AssertionMessages;

import java.io.IOException;
import java.util.logging.Level;

import com.l7tech.util.Pair;
import org.xml.sax.SAXException;

import javax.wsdl.Binding;
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
public class ServerOperation extends AbstractServerAssertion<Operation> {

    public ServerOperation(Operation assertion) {
        super( assertion );
    }

    /**
     * @return AssertionStatus.FALSIFIED if the message matched
     * an operation described in the WSDL but not the one wanted by the assertion, AssertionStatus.FAILED if the message
     * could not match any operation described in the WSDL, and AssertionStatus.NONE if there is a match
     */
    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            final Pair<Binding,javax.wsdl.Operation> pair = context.getBindingAndOperation();
            if (pair != null) {
                javax.wsdl.Operation cntxop = pair.right;
                String tmp = cntxop.getName();
                if ( assertion.getOperationName().equals(tmp)) {
                    return AssertionStatus.NONE;
                } else {
                    logAndAudit( AssertionMessages.WSDLOPERATION_NOMATCH, tmp, assertion.getOperationName() );
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
        logAndAudit( AssertionMessages.WSDLOPERATION_CANNOTIDENTIFY );
        return AssertionStatus.FAILED;
    }
}
