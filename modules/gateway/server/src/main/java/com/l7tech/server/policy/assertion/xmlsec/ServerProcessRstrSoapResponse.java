package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ProcessRstrSoapResponse;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.wstrust.RstrInfo;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;

/**
 *
 */
public class ServerProcessRstrSoapResponse extends AbstractMessageTargetableServerAssertion<ProcessRstrSoapResponse> {

    //- PUBLIC

    public ServerProcessRstrSoapResponse( final ProcessRstrSoapResponse assertion,
                                          final BeanFactory factory ) {
        super(assertion, assertion);
        securityTokenResolver = factory.getBean( "securityTokenResolver", SecurityTokenResolver.class );
    }

    //- PROTECTED

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authenticationContext ) throws IOException, PolicyAssertionException {
        final Element soapBody;
        try {
            if ( !message.isSoap() ) {
                logAndAudit( AssertionMessages.RSTR_PROCESSOR_NOT_SOAP, messageDescription );
                return AssertionStatus.NOT_APPLICABLE;
            }
            soapBody = SoapUtil.getBodyElement( message.getXmlKnob().getDocumentReadOnly() );
        } catch ( SAXException e ) {
            logAndAudit( AssertionMessages.RSTR_PROCESSOR_BAD_XML, messageDescription, ExceptionUtils.getMessage(e));
            return AssertionStatus.FALSIFIED;
        } catch ( InvalidDocumentFormatException e ) {
            logAndAudit( AssertionMessages.RSTR_PROCESSOR_NOT_SOAP, messageDescription );
            return AssertionStatus.NOT_APPLICABLE;
        }

        final RstrInfo rstrInfo;
        try {
            final Element bodyChild = DomUtils.findOnlyOneChildElement( soapBody );
            rstrInfo = RstrInfo.parse( bodyChild, securityTokenResolver );

        } catch ( TooManyChildElementsException e ) {
            logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, "not an RSTR message" );
            return AssertionStatus.FALSIFIED;
        } catch ( InvalidDocumentFormatException e ) {
            logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, ExceptionUtils.getMessage( e ));
            return AssertionStatus.FALSIFIED;
        }

        if ( rstrInfo.getToken() == null ) {
            logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, "no token in response" );
            return AssertionStatus.FALSIFIED;
        } else if ( !validToken( rstrInfo.getToken(), assertion.getTokenType() ) ) {
            return AssertionStatus.FALSIFIED;
        }

        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_TOKEN), rstrInfo.getToken() );
        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_CREATE_TIME), rstrInfo.getCreatedText() );
        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_EXPIRY_TIME), rstrInfo.getExpiresText() );
        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_SERVER_ENTROPY), rstrInfo.getEntropyBase64() );
        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_KEY_SIZE), rstrInfo.getKeySize() );
        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_FULL_KEY), rstrInfo.getSecretBase64() );

        return AssertionStatus.NONE;
    }

    //- PRIVATE

    private final SecurityTokenResolver securityTokenResolver;

    private boolean validToken( final Element tokenElement, final SecurityTokenType expectedTokenType ) {
        boolean valid = false;

        if ( (expectedTokenType == SecurityTokenType.SAML_ASSERTION && !SoapConstants.SAML_NAMESPACE.equals(tokenElement.getNamespaceURI())) ||
             (expectedTokenType == SecurityTokenType.SAML2_ASSERTION && !SoapConstants.SAML_NAMESPACE2.equals(tokenElement.getNamespaceURI())) ||
             (expectedTokenType == SecurityTokenType.WSSC_CONTEXT && !ArrayUtils.contains( SoapConstants.WSSC_NAMESPACE_ARRAY, tokenElement.getNamespaceURI())) ) {
            logAndAudit( AssertionMessages.RSTR_PROCESSOR_UNEXPECTED_TOKEN, expectedTokenType.getName(), qName(tokenElement) );
        } else {
            valid = true;
        }

        return valid;
    }

    private String qName( final Element element ) {
        return new QName( element.getNamespaceURI(), element.getLocalName() ).toString();
    }

    private String prefix( final String name ) {
        String prefixed = name;

        if ( assertion.getVariablePrefix() != null ) {
            prefixed = assertion.getVariablePrefix()  + "." + name;
        }

        return prefixed;
    }
}
