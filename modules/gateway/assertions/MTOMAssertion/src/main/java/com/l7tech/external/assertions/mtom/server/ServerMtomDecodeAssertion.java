package com.l7tech.external.assertions.mtom.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.external.assertions.mtom.MtomDecodeAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.message.Message;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Config;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

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
        this.stashManagerFactory = context.getBean( "stashManagerFactory", StashManagerFactory.class );
        this.config = context.getBean( "serverConfig", Config.class );
    }

    //- PROTECTED

    protected ServerMtomDecodeAssertion( final MtomDecodeAssertion assertion,
                                         final StashManagerFactory stashManagerFactory,
                                         final Config config ){
        super( assertion, assertion );
        this.stashManagerFactory = stashManagerFactory;
        this.config = config;
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        AssertionStatus status;

        ContentTypeHeader header = message.getMimeKnob().getOuterContentType();

        if ( XOPUtils.isXop( header ) ) {
            if ( assertion.isProcessSecuredOnly() ) {
                if ( config.getBooleanProperty( ClusterProperty.asServerConfigPropertyName(MtomDecodeAssertion.PROP_DECODE_SECURED), false ) ) {
                    status = reconstituteIfSecured( context, message );
                } else {
                    status = AssertionStatus.NONE;
                }
            } else {
                status = reconstitute( context, message );
            }
        } else if ( assertion.isRequireEncoded() ) {
            logAndAudit( MTOM_DECODE_ERROR, "Message not encoded" );
            status = getBadMessageStatus();
        } else {
            status = AssertionStatus.NONE;
        }

        return status;
    }



    //- PRIVATE

    private static final int DEFAULT_ATTACHMENT_MAX = 1024 * 1024;

    private final Config config;
    private final StashManagerFactory stashManagerFactory;

    private AssertionStatus reconstitute( final PolicyEnforcementContext context,
                                          final Message message ) {
        AssertionStatus status = AssertionStatus.UNDEFINED;

        Message outputMessage = null;
        if ( assertion.getOutputTarget() == null ) {
            outputMessage = message;
        } else {
            try {
                outputMessage = context.getOrCreateTargetMessage( assertion.getOutputTarget(), false );
            } catch (NoSuchVariableException nsve) {
                status = AssertionStatus.FAILED;
                logAndAudit( MTOM_ENCODE_ERROR, ExceptionUtils.getMessage(nsve));
            } catch (VariableNotSettableException vnse) {
                status = AssertionStatus.FAILED;
                logAndAudit( MTOM_ENCODE_ERROR, ExceptionUtils.getMessage(vnse));
            }
        }

        if ( outputMessage != null ) {
            int attachmentMaxSize = config.getIntProperty( "ioXmlPartMaxBytes", DEFAULT_ATTACHMENT_MAX );
            try {
                XOPUtils.reconstitute( message, outputMessage, assertion.isRemovePackaging(), attachmentMaxSize, stashManagerFactory );
                status = AssertionStatus.NONE;
            } catch ( Exception e) {
                status = AssertionStatus.FALSIFIED;
                logAndAudit(
                        MTOM_DECODE_ERROR,
                        new String[]{"Error decoding XOP '"+ ExceptionUtils.getMessage(e)+"'"},
                        e instanceof XOPUtils.XOPException ? e.getCause() : e );
            }
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
            logAndAudit( MTOM_DECODE_ERROR, "Invalid SOAP message: " + ExceptionUtils.getMessage(e));
            status = getBadMessageStatus();
        } catch (SAXException e) {
            logAndAudit( MTOM_DECODE_ERROR, "Error parsing message: " + ExceptionUtils.getMessage(e));
            status = getBadMessageStatus();
        } catch (NoSuchPartException e) {
            logAndAudit( MTOM_DECODE_ERROR, "Error accessing message: " + ExceptionUtils.getMessage(e));
            status = AssertionStatus.FAILED; // error due to destructive read of first part
        }

        return status;
    }

}
