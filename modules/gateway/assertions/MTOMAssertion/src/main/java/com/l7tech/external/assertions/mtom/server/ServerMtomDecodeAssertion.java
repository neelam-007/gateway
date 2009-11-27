package com.l7tech.external.assertions.mtom.server;

import com.l7tech.server.audit.Auditor;
import com.l7tech.external.assertions.mtom.MtomDecodeAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.message.Message;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server side implementation of the MtomDecodeAssertion.
 *
 * @see com.l7tech.external.assertions.mtom.MtomDecodeAssertion
 */
public class ServerMtomDecodeAssertion extends AbstractMessageTargetableServerAssertion<MtomDecodeAssertion> {

    //- PUBLIC

    public ServerMtomDecodeAssertion( final MtomDecodeAssertion assertion,
                                      final ApplicationContext context) throws PolicyAssertionException {
        super( assertion, assertion );

        this.auditor = new Auditor(this, context, logger);
        this.stashManagerFactory = (StashManagerFactory) context.getBean( "stashManagerFactory", StashManagerFactory.class );
    }

    //- PROTECTED

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        AssertionStatus status;

        ContentTypeHeader header = message.getMimeKnob().getOuterContentType();

        if ( XOPUtils.isXop( header ) ) {
            if ( assertion.isProcessSecuredOnly() ) {
                status = reconstituteIfSecured( context, message );
            } else {
                status = reconstitute( context, message );
            }
        } else if ( assertion.isRequireEncoded() ) {
            auditor.logAndAudit( MTOM_DECODE_ERROR, "Message not encoded" );
            status = getBadMessageStatus();
        } else {
            status = AssertionStatus.NONE;
        }

        return status;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerMtomDecodeAssertion.class.getName());
    private final Auditor auditor;
    private final StashManagerFactory stashManagerFactory;

    private AssertionStatus reconstitute( final PolicyEnforcementContext context,
                                          final Message message ) {
        AssertionStatus status;

        final Message outputMessage;
        if ( assertion.getOutputTarget() == null ) {
            outputMessage = message;
        } else if ( assertion.getOutputTarget().getTarget()==TargetMessageType.OTHER ) {
            outputMessage = new Message();
            context.setVariable(assertion.getOutputTarget().getOtherTargetMessageVariable(), outputMessage);
            context.runOnClose( new Runnable() {
                @Override
                public void run() {
                    outputMessage.close();
                }
            } );
        } else if ( assertion.getOutputTarget().getTarget()==TargetMessageType.REQUEST ) {
            outputMessage = context.getRequest();
        } else {
            outputMessage = context.getResponse();            
        }

        try {
            XOPUtils.reconstitute( message, outputMessage, assertion.isRemovePackaging(), stashManagerFactory );
            status = AssertionStatus.NONE;
        } catch ( Exception e) {
            status = AssertionStatus.FALSIFIED;
            auditor.logAndAudit(
                    MTOM_DECODE_ERROR,
                    new String[]{"Error decoding XOP '"+ ExceptionUtils.getMessage(e)+"'"},
                    e );
        }
        
        return status;
    }

    private AssertionStatus reconstituteIfSecured( final PolicyEnforcementContext context,
                                                   final Message message ) throws IOException {
        AssertionStatus status;

        try {
            if ( message.getSoapKnob().isSecurityHeaderPresent() ) {
                Document document = message.getXmlKnob().getDocumentReadOnly();

                if ( SoapUtil.getSecurityElementForL7(document) != null ||
                     SoapUtil.getSecurityElement(document) != null ) {
                    status = reconstitute( context, message );
                } else {
                    status = AssertionStatus.NONE;
                }
            } else {
                status = AssertionStatus.NONE;
            }
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit( MTOM_DECODE_ERROR, "Invalid SOAP message: " + ExceptionUtils.getMessage(e));
            status = getBadMessageStatus();
        } catch (SAXException e) {
            auditor.logAndAudit( MTOM_DECODE_ERROR, "Error parsing message: " + ExceptionUtils.getMessage(e));
            status = getBadMessageStatus();
        } catch (NoSuchPartException e) {
            auditor.logAndAudit( MTOM_DECODE_ERROR, "Error accessing message: " + ExceptionUtils.getMessage(e));
            status = AssertionStatus.FAILED; // error due to destructive read of first part
        }

        return status;
    }

}
