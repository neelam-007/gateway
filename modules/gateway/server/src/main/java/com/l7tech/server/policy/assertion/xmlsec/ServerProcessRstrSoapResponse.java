package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ProcessRstrSoapResponse;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.KeyInfoElement;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.UnexpectedKeyInfoException;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class ServerProcessRstrSoapResponse extends AbstractMessageTargetableServerAssertion<ProcessRstrSoapResponse> {

    //- PUBLIC

    public ServerProcessRstrSoapResponse( final ProcessRstrSoapResponse assertion,
                                          final BeanFactory factory ) {
        super(assertion, assertion);
        auditor = factory instanceof ApplicationContext ?
                new Auditor(this, (ApplicationContext)factory, logger) :
                new LogOnlyAuditor(logger);
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
                auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_NOT_SOAP, messageDescription );
                return AssertionStatus.NOT_APPLICABLE;
            }
            soapBody = SoapUtil.getBodyElement( message.getXmlKnob().getDocumentReadOnly() );
        } catch ( SAXException e ) {
            auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_BAD_XML, messageDescription, ExceptionUtils.getMessage(e));
            return AssertionStatus.FALSIFIED;
        } catch ( InvalidDocumentFormatException e ) {
            auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_NOT_SOAP, messageDescription );
            return AssertionStatus.NOT_APPLICABLE;
        }

        final Element rstrElement;
        try {
            final Element bodyChild = DomUtils.findOnlyOneChildElement( soapBody );

            if ( !ArrayUtils.contains( SoapConstants.WST_NAMESPACE_ARRAY, bodyChild.getNamespaceURI() )) {
                auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, "not an RSTR message." );
                return AssertionStatus.FALSIFIED;
            }

            if ( "RequestSecurityTokenResponseCollection".equals(bodyChild.getLocalName()) ) {
                final List<Element> requestSecurityTokenResponses = DomUtils.findChildElementsByName( bodyChild, bodyChild.getNamespaceURI(), "RequestSecurityTokenResponse" );

                if ( requestSecurityTokenResponses.size() != 1 ) {
                    auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, "Unexpected response count: " + requestSecurityTokenResponses.size() );
                    return AssertionStatus.FALSIFIED;
                } else {
                    rstrElement = requestSecurityTokenResponses.get( 0 );
                }
            } else if ( "RequestSecurityTokenResponse".equals(bodyChild.getLocalName()) ) {
                rstrElement = bodyChild;
            } else {
                auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, "not an RSTR message." );
                return AssertionStatus.FALSIFIED;
            }
        } catch ( TooManyChildElementsException e ) {
            auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, "not an RSTR message." );
            return AssertionStatus.FALSIFIED;
        }

        final String wsTrustNamespace = rstrElement.getNamespaceURI();

        final Element tokenElement;
        String lifetimeCreated = "";
        String lifetimeExpires = "";
        String serverEntropy = "";
        String serverKey = "";
        try {
            final Element requestedTokenElement = DomUtils.findExactlyOneChildElementByName( rstrElement, wsTrustNamespace, "RequestedSecurityToken" );
            tokenElement = DomUtils.findExactlyOneChildElement( requestedTokenElement );
            if ( !validToken(tokenElement, assertion.getTokenType()) )
                return AssertionStatus.FALSIFIED;

            final Element lifetime = DomUtils.findOnlyOneChildElementByName( rstrElement, wsTrustNamespace, "Lifetime" );
            if ( lifetime != null ) {
                lifetimeCreated = getChildText( lifetime, SoapConstants.WSU_NAMESPACE, "Created" );
                lifetimeExpires = getChildText( lifetime, SoapConstants.WSU_NAMESPACE, "Expires" );
            }

            final Element entropy = DomUtils.findOnlyOneChildElementByName( rstrElement, wsTrustNamespace, "Entropy" );
            if ( entropy != null ) {
                serverEntropy = getBinarySecret( entropy, wsTrustNamespace );
            }

            final Element proof = DomUtils.findOnlyOneChildElementByName( rstrElement, wsTrustNamespace, "RequestedProofToken" );
            if ( proof != null ) {
                serverKey = getBinarySecret( proof, wsTrustNamespace );
            }
        } catch ( TooManyChildElementsException e ) {
            auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, ExceptionUtils.getMessage(e));
            return AssertionStatus.FALSIFIED;
        } catch ( MissingRequiredElementException e ) {
            auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, ExceptionUtils.getMessage(e));
            return AssertionStatus.FALSIFIED;
        } catch ( GeneralSecurityException e ) {
            auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_ENCRYPTION_ERROR, ExceptionUtils.getMessage(e));
            return AssertionStatus.FALSIFIED;
        } catch ( UnexpectedKeyInfoException e ) {
            auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_ENCRYPTION_ERROR, ExceptionUtils.getMessage(e));
            return AssertionStatus.FALSIFIED;
        } catch ( InvalidDocumentFormatException e ) {
            auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_ENCRYPTION_ERROR, ExceptionUtils.getMessage(e));
            return AssertionStatus.FALSIFIED;
        }

        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_TOKEN), tokenElement );
        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_CREATE_TIME), lifetimeCreated );
        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_EXPIRY_TIME), lifetimeExpires );
        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_SERVER_ENTROPY), serverEntropy );
        context.setVariable( prefix(ProcessRstrSoapResponse.VARIABLE_FULL_KEY), serverKey );

        return AssertionStatus.NONE;
    }

    private String getBinarySecret( final Element binarySecretHolder,
                                    final String wsTrustNamespace ) throws InvalidDocumentFormatException, UnexpectedKeyInfoException, GeneralSecurityException {
        String binarySecret = getChildText( binarySecretHolder, wsTrustNamespace, "BinarySecret" );

        final Element encryptedKeyEl = DomUtils.findOnlyOneChildElementByName( binarySecretHolder, SoapConstants.XMLENC_NS, "EncryptedKey" );

        if ( encryptedKeyEl != null ) {
            if ( binarySecret != null ) {
                auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_INVALID, "Only one of BinarySecret or EncryptedKey is permitted.");
                throw new AssertionStatusException( AssertionStatus.FALSIFIED );
            }

            final SignerInfo signerInfo = KeyInfoElement.getTargetPrivateKeyForEncryptedType(encryptedKeyEl, securityTokenResolver, null);

            // verify that the algo is supported
            XencUtil.checkEncryptionMethod(encryptedKeyEl);

            byte[] data = XencUtil.decryptKey(encryptedKeyEl, signerInfo.getPrivate());
            binarySecret = HexUtils.encodeBase64( data, true );
        }

        return binarySecret;
    }

    private boolean validToken( final Element tokenElement, final SecurityTokenType expectedTokenType ) {
        boolean valid = false;

        if ( (expectedTokenType == SecurityTokenType.SAML_ASSERTION && !SoapConstants.SAML_NAMESPACE.equals(tokenElement.getNamespaceURI())) ||
             (expectedTokenType == SecurityTokenType.SAML2_ASSERTION && !SoapConstants.SAML_NAMESPACE2.equals(tokenElement.getNamespaceURI())) ||
             (expectedTokenType == SecurityTokenType.WSSC_CONTEXT && !ArrayUtils.contains( SoapConstants.WSSC_NAMESPACE_ARRAY, tokenElement.getNamespaceURI())) ) {
            auditor.logAndAudit( AssertionMessages.RSTR_PROCESSOR_UNEXPECTED_TOKEN, expectedTokenType.getName(), qName(tokenElement) );
        } else {
            valid = true;
        }

        return valid;
    }

    private String qName( final Element element ) {
        return new QName( element.getNamespaceURI(), element.getLocalName() ).toString();
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ServerBuildRstSoapRequest.class.getName() );

    private final Auditor auditor;
    private final SecurityTokenResolver securityTokenResolver;

    private String prefix( final String name ) {
        String prefixed = name;

        if ( assertion.getVariablePrefix() != null ) {
            prefixed = assertion.getVariablePrefix()  + "." + name;
        }

        return prefixed;
    }

    private String getChildText( final Element parent, final String namespace, final String localName ) throws TooManyChildElementsException {
        String text = null;

        final Element childElement = DomUtils.findOnlyOneChildElementByName( parent, namespace, localName );
        if ( childElement != null ) {
            text = DomUtils.getTextValue( childElement );
        }

        return text;
    }

}
