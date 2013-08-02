package com.l7tech.external.assertions.uddinotification.server;

import com.l7tech.external.assertions.uddinotification.UDDINotificationAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.uddi.UDDICoordinator;
import com.l7tech.server.uddi.NotificationUDDIEvent;
import com.l7tech.message.Message;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Server side implementation of the UDDINotificationAssertion.
 *
 * @see com.l7tech.external.assertions.uddinotification.UDDINotificationAssertion
 */
public class ServerUDDINotificationAssertion extends AbstractServerAssertion<UDDINotificationAssertion> {

    //- PUBLIC

    public ServerUDDINotificationAssertion( final UDDINotificationAssertion assertion,
                                            final ApplicationContext context ) throws PolicyAssertionException {
        super(assertion);
        this.uddiCoordinator = context.getBean( "uddiCoordinator", UDDICoordinator.class );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;

        final Message request = context.getRequest();
        final Message response = context.getResponse();

        Document requestDocument = null;
        try {
            requestDocument = request.getXmlKnob().getDocumentReadOnly();
        } catch ( SAXException e ) {
            logger.log( Level.WARNING, "Error processing request message '"+ ExceptionUtils.getMessage( e ) +"'." );
            status = AssertionStatus.FALSIFIED;
        }

        if ( status == AssertionStatus.NONE ) {
            try {
                uddiCoordinator.notifyEvent( new NotificationUDDIEvent(
                        context.getService().getGoid(),
                        XmlUtil.nodeToString( requestDocument ),
                        request.getTcpKnob().getRemoteAddress() ) );

                MessageFactory messageFactory = SoapUtil.getMessageFactory( SOAPConstants.SOAP_1_1_PROTOCOL );
                SOAPMessage message = messageFactory.createMessage();
                response.initialize( message.getSOAPPart() );
            } catch ( SOAPException se ) {
                logger.log( Level.WARNING, "Error generating SOAP response message" );
                status = AssertionStatus.FAILED;
            } catch ( IOException e ) {
                logger.log( Level.WARNING, "Error serializing request message '"+ ExceptionUtils.getMessage( e ) +"'." );
                status = AssertionStatus.FAILED;
            }
        }

        return status;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerUDDINotificationAssertion.class.getName());

    private final UDDICoordinator uddiCoordinator;
    
}
