package com.l7tech.server.policy.assertion.xml;

import com.l7tech.policy.assertion.xml.RemoveElement;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.message.Message;
import com.l7tech.gateway.common.audit.AssertionMessages;

import java.io.IOException;
import java.util.logging.Logger;

import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.springframework.context.ApplicationContext;

/**
 * @author jbufu
 */
public class ServerRemoveElement extends AbstractMessageTargetableServerAssertion<RemoveElement> {

    private static final Logger logger = Logger.getLogger(ServerRemoveElement.class.getName());
    private final Auditor auditor;

    public ServerRemoveElement( final RemoveElement assertion, final ApplicationContext context ) {
        super(assertion, assertion);
        this.auditor = new Auditor(this, context, logger);
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        if ( !message.isXml() ) {
            auditor.logAndAudit(AssertionMessages.REMOVE_ELEMENT_NOT_XML);
            return AssertionStatus.NOT_APPLICABLE;
        }

        try {
            final Object val = context.getVariable(assertion.getElementFromVariable());
            Element[] elementsToRemove = new Element[0];
            if ( val instanceof Element[] ) {
                elementsToRemove = (Element[])val;
            } else if ( val instanceof Element ) {
                elementsToRemove = new Element[]{ (Element)val };
            }

            if ( elementsToRemove.length > 0 ) {
                Document document = message.getXmlKnob().getDocumentWritable();
                for( Element e : elementsToRemove ) {
                    if ( document == e.getOwnerDocument() ) {
                        e.getParentNode().removeChild(e);
                    }
                }
            }

            return AssertionStatus.NONE;
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, assertion.getElementFromVariable());
            return AssertionStatus.FAILED;
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.REMOVE_ELEMENT_NOT_XML);
            return AssertionStatus.NOT_APPLICABLE;
        }
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }
}
