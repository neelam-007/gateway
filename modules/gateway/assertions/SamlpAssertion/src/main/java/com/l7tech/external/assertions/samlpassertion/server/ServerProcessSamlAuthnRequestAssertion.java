package com.l7tech.external.assertions.samlpassertion.server;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAuthnRequestAssertion;
import static com.l7tech.external.assertions.samlpassertion.ProcessSamlAuthnRequestAssertion.*;
import static com.l7tech.external.assertions.samlpassertion.server.ProtocolRequestUtilities.*;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.KeyInfoElement;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.TooManyChildElementsException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import saml.support.ds.KeyInfoType;
import saml.support.ds.SignatureType;
import saml.support.ds.X509DataType;
import saml.v2.assertion.NameIDType;
import saml.v2.assertion.SubjectConfirmationType;
import saml.v2.assertion.SubjectType;
import saml.v2.protocol.AuthnRequestType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Server assertion for Process SAML AuthnRequest assertion
 */
public class ServerProcessSamlAuthnRequestAssertion extends AbstractMessageTargetableServerAssertion<ProcessSamlAuthnRequestAssertion> {

    //- PUBLIC

    public ServerProcessSamlAuthnRequestAssertion( final ProcessSamlAuthnRequestAssertion assertion,
                                                   final ApplicationContext applicationContext ) throws ServerPolicyException {
        super( assertion );
        this.securityTokenResolver = applicationContext.getBean( "securityTokenResolver", SecurityTokenResolver.class );
    }

    //- PROTECTED

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authenticationContext ) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        final Element authnRequestElement = getAuthnRequestElement( message );
        if ( isSetVariables() )  { // Set the request message variable early to assist debugging on failures
            if ( assertion.getSamlProtocolBinding() != null && authnRequestElement != null) {
                final Message requestMessage = getOrCreateAuthnRequestMessage( context );
                requestMessage.initialize( authnRequestElement.getOwnerDocument() );
            } else {
                context.setVariable( prefix(SUFFIX_REQUEST), null );
            }
        }

        AuthnRequestType authnRequest = null;
        if ( authnRequestElement != null ) {
            // Unmarshal to JAXB type
            authnRequest = unmarshal( authnRequestElement );
        }

        if ( authnRequest != null ) {
            // Validate the request
            final Pair<String, X509Certificate> certData = validateAuthnRequest( authnRequest, authnRequestElement );
            status = AssertionStatus.NONE; // validation was successful

            if ( isSetVariables() )  {
                context.setVariable( prefix(SUFFIX_SUBJECT), getSubject(authnRequest.getSubject()) );
                context.setVariable( prefix(SUFFIX_SUBJECT_NAME_QUALIFIER), getNameQualifier(getSubjectNameID(authnRequest.getSubject())) );
                context.setVariable( prefix(SUFFIX_SUBJECT_SP_NAME_QUALIFIER), getSPNameQualifier(getSubjectNameID(authnRequest.getSubject())) );
                context.setVariable( prefix(SUFFIX_SUBJECT_FORMAT), getNameFormat(getSubjectNameID(authnRequest.getSubject())) );
                context.setVariable( prefix(SUFFIX_SUBJECT_SP_PROVIDED_ID), getSPProvidedID(getSubjectNameID(authnRequest.getSubject())) );
                context.setVariable( prefix(SUFFIX_X509CERT_BASE64), certData.left );
                context.setVariable( prefix(SUFFIX_X509CERT), certData.right );
                context.setVariable( prefix(SUFFIX_ACS_URL), authnRequest.getAssertionConsumerServiceURL() );
                context.setVariable( prefix(SUFFIX_ID), authnRequest.getID() );
                context.setVariable( prefix(SUFFIX_VERSION), authnRequest.getVersion() );
                context.setVariable( prefix(SUFFIX_ISSUE_INSTANT), getIsoTime(authnRequest.getIssueInstant()) );
                context.setVariable( prefix(SUFFIX_DESTINATION), authnRequest.getDestination() );
                context.setVariable( prefix(SUFFIX_CONSENT), getConsent(authnRequest) );
                context.setVariable( prefix(SUFFIX_ISSUER), getName(authnRequest.getIssuer()) );
                context.setVariable( prefix(SUFFIX_ISSUER_NAME_QUALIFIER), getNameQualifier(authnRequest.getIssuer()) );
                context.setVariable( prefix(SUFFIX_ISSUER_SP_NAME_QUALIFIER), getSPNameQualifier(authnRequest.getIssuer()) );
                context.setVariable( prefix(SUFFIX_ISSUER_FORMAT), getNameFormat(authnRequest.getIssuer()) );
                context.setVariable( prefix(SUFFIX_ISSUER_SP_PROVIDED_ID), getSPProvidedID(authnRequest.getIssuer()) );
            }
        }

        return status;
    }

    //- PRIVATE

    private static final String PARAMETER_SAML_REQUEST = "SAMLRequest";
    private static final String PARAMETER_SAML_ENCODING = "SAMLEncoding";

    private static final String URL_ENCODING_DEFLATE = "urn:oasis:names:tc:SAML:2.0:bindings:URL-Encoding:DEFLATE";

    private static final String ELEMENT_AUTHN_REQUEST = "AuthnRequest";

    private static final boolean allowMultipleCertificates = ConfigFactory.getBooleanProperty( "com.l7tech.external.assertions.samlpassertion.allowMultipleCertificates", false );
    private static final boolean validateSSOProfileDetails = ConfigFactory.getBooleanProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile", true );

    private final SecurityTokenResolver securityTokenResolver;

    /**
     * Get the AuthnRequest element as per the configured binding.
     */
    private Element getAuthnRequestElement( final Message message ) throws IOException {
        Element authnRequestElement = null;

        if ( assertion.getSamlProtocolBinding() == null ) {
            try {
                if ( message.isXml() ) {
                    authnRequestElement = message.getXmlKnob().getDocumentReadOnly().getDocumentElement();
                }
            } catch ( SAXException e ) {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_INVALID_REQUEST,
                        new String[]{"Error parsing request - " + ExceptionUtils.getMessage( e )},
                        ExceptionUtils.getDebugException( e ) );
            }
        } else {
            final HttpRequestKnob hrk = message.getKnob(HttpRequestKnob.class);
            if ( hrk != null ) {
                authnRequestElement = getAuthnRequestForBinding( hrk, assertion.getSamlProtocolBinding() );
            } else {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_BINDING_ERROR, assertion.getSamlProtocolBinding().toString(), "Message not an HTTP request" );
            }
        }

        return authnRequestElement;
    }

    private Element getAuthnRequestForBinding( final HttpRequestKnob hrk,
                                               final SamlProtocolBinding binding ) throws IOException {
        Element authnRequestElement = null;

        String samlRequest = hrk.getParameter( PARAMETER_SAML_REQUEST );
        if ( samlRequest != null ) {
            InputStream samlRequestIn = null;
            try {
                switch ( binding ) {
                    case HttpPost:
                        samlRequestIn = new ByteArrayInputStream(HexUtils.decodeBase64( samlRequest, true ));
                        break;
                    case HttpRedirect:
                        final String encoding = hrk.getParameter( PARAMETER_SAML_ENCODING );
                        if ( encoding == null || URL_ENCODING_DEFLATE.equals(encoding) ) {
                            final byte[] samlRequestData = HexUtils.decodeBase64( samlRequest, true );
                            samlRequestIn =  new InflaterInputStream(new ByteArrayInputStream(samlRequestData), new Inflater(true));
                        } else {
                            logAndAudit( AssertionMessages.SAMLP_PROCREQ_BINDING_ERROR, assertion.getSamlProtocolBinding().toString(), "Unsupported encoding '"+encoding+"'" );
                        }
                        break;
                    default:
                        logAndAudit( AssertionMessages.SAMLP_PROCREQ_BINDING_ERROR, assertion.getSamlProtocolBinding().toString(), "Unknown binding type" );
                        break;
                }

                if ( samlRequestIn != null ) {
                    samlRequestIn = new ByteOrderMarkInputStream( samlRequestIn );
                    authnRequestElement = XmlUtil.parse( samlRequestIn ).getDocumentElement();
                }
            } catch ( SAXException e ) {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_BINDING_ERROR,
                        new String[]{assertion.getSamlProtocolBinding().toString(), "Error parsing request - " + ExceptionUtils.getMessage( e )},
                        ExceptionUtils.getDebugException( e ) );
            } finally {
                ResourceUtils.closeQuietly( samlRequestIn );
            }
        } else {
            logAndAudit( AssertionMessages.SAMLP_PROCREQ_BINDING_ERROR, assertion.getSamlProtocolBinding().toString(), PARAMETER_SAML_REQUEST + " parameter is required" );
        }

        return authnRequestElement;
    }

    private AuthnRequestType unmarshal( final Element authnRequestElement ) {
        AuthnRequestType result = null;

        if ( ELEMENT_AUTHN_REQUEST.equals(authnRequestElement.getLocalName()) &&
             SamlConstants.NS_SAMLP2.equals(authnRequestElement.getNamespaceURI())) {
            try {
                final Unmarshaller um = JaxbUtil.getUnmarshallerV2();
                um.setEventHandler( new ValidationEventHandler(){
                    @Override
                    public boolean handleEvent( final ValidationEvent event ) {
                        return false;
                    }
                } );

                final Object resultObj = um.unmarshal( authnRequestElement );
                if ( resultObj instanceof JAXBElement &&
                     AuthnRequestType.class.isAssignableFrom(((JAXBElement) resultObj).getDeclaredType())) {
                    result = (AuthnRequestType)((JAXBElement) resultObj).getValue();
                } else {
                    logAndAudit( AssertionMessages.SAMLP_PROCREQ_INVALID_REQUEST, "Unexpected request type" );
                }
            } catch ( JAXBException e ) {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_INVALID_REQUEST,
                        new String[]{ "Request error - " + ExceptionUtils.getMessage(e) }, 
                        ExceptionUtils.getDebugException( e ) );
            }
        } else {
            logAndAudit(AssertionMessages.SAMLP_PROCREQ_INVALID_REQUEST, "Not an AuthnRequest");
        }

        return result;
    }

    /**
     * Validate signature, conditions and any other validation requirements.
     */
    private Pair<String, X509Certificate> validateAuthnRequest( final AuthnRequestType authnRequest,
                                                                final Element authnRequestElement ) {
        final String certBase64 = getCertBase64( authnRequest.getSignature() );
        X509Certificate certificate = null;

        // Basic validation rules
        validateDetails( authnRequest );

        // Additional validation rules
        if ( validateSSOProfileDetails ) {
            validateSSOProfileDetails( authnRequest );
        }

        // Validate signature
        if ( assertion.isVerifySignature() ) {
            certificate = validateSignature( authnRequest.getID(), authnRequestElement );
        }

        return new Pair<String, X509Certificate>( certBase64, certificate );
    }

    private X509Certificate validateSignature( final String authnRequestId,
                                               final Element authnRequestElement ) {
        try {
            final Element signature = DomUtils.findExactlyOneChildElementByName(authnRequestElement, SoapConstants.DIGSIG_URI, "Signature");
            final Element keyInfo = DomUtils.findExactlyOneChildElementByName(signature, SoapConstants.DIGSIG_URI, "KeyInfo");
            final KeyInfoElement keyInfoElement = KeyInfoElement.parse(keyInfo, securityTokenResolver, KeyInfoInclusionType.ANY);
            final X509Certificate signingCert = keyInfoElement.getCertificate();
            PublicKey signingKey = signingCert.getPublicKey();

            // Validate signature
            final boolean[] resolvedAuthnRequestId = new boolean[1];
            final SignatureContext sigContext = DsigUtil.createSignatureContextForValidation();
            sigContext.setIDResolver(new IDResolver() {
                @Override
                public Element resolveID( final Document doc, final String id ) {
                    if ( id.equals(authnRequestId) ) {
                        resolvedAuthnRequestId[0] = true;
                        return authnRequestElement;
                    } else {
                        return null;
                    }
                }
            });
            final WssProcessorAlgorithmFactory algFactory = new WssProcessorAlgorithmFactory();
            sigContext.setAlgorithmFactory(algFactory);
            try {
                KeyUsageChecker.requireActivity( KeyUsageActivity.verifyXml, signingCert);
            } catch ( KeyUsageException e) {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, ExceptionUtils.getMessage(e) );
                throw new AssertionStatusException( AssertionStatus.FALSIFIED);
            } catch ( CertificateException e) {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR,
                        new String[]{ "Certificate processing failed - " + ExceptionUtils.getMessage(e)},
                        e );
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            final Validity validity = DsigUtil.verify(sigContext, signature, signingKey);

            if ( !validity.getCoreValidity() ) {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, "Unable to verify signature of SAML assertion: Validity not achieved. " + DsigUtil.getInvalidSignatureMessage(validity) );
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            if (!resolvedAuthnRequestId[0]) {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, "Signature does not cover AuthnRequest element" );
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            if (!algFactory.isSawEnvelopedTransform()) {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, "Signature must be enveloped" );
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            if (validity.getNumberOfReferences() != 1) {
                logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, "Signature covers multiple references" );
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            return signingCert;
        } catch ( TooManyChildElementsException e) {
            logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, ExceptionUtils.getMessage(e) );
        } catch ( MissingRequiredElementException e ) {
            logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, ExceptionUtils.getMessage(e) );
        } catch ( SignatureException e ) {
            logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, ExceptionUtils.getMessage(e) );
        } catch ( SAXException e ) { // this will be a KeyInfo processing error
            logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, ExceptionUtils.getMessage(e) );
        } catch ( KeyInfoElement.MissingResolverException e ) {
            logAndAudit( Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ ExceptionUtils.getMessage(e)}, e );
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        throw new AssertionStatusException(AssertionStatus.FALSIFIED);
    }

    /**
     * Validate that required details are present.
     *
     * - Assertion Consumer Service URL
     * - ID
     * - Version
     * - IssueInstant
     */
    private void validateDetails( final AuthnRequestType authnRequest ) {
        checkMissingOrEmpty( authnRequest.getAssertionConsumerServiceURL(), "AssertionConsumerServiceURL" );
        checkMissingOrEmpty( authnRequest.getID(), "ID" );
        checkMissingOrEmpty( authnRequest.getVersion(), "Version" );
        checkMissing( authnRequest.getIssueInstant(), "IssueInstant" );
    }

    private void checkMissingOrEmpty( final String value, final String description ) {
        if ( value == null || value.trim().isEmpty() ) {
            failDetails( description );
        }
    }

    private void checkMissing( final Object value, final String description ) {
        if ( value == null ) {
            failDetails( description );
        }
    }

    private void failDetails( final String missing ) {
        logAndAudit(AssertionMessages.SAMLP_PROCREQ_INVALID_REQUEST, missing + " is required" );
        throw new AssertionStatusException( AssertionStatus.FALSIFIED);
    }

    /**
     * Validate additional profile constraints from     4.1.4.1 <AuthnRequest> Usage.
     *
     * - check issuer format
     * - no subject confirmation
     */
    private void validateSSOProfileDetails( final AuthnRequestType authnRequest ) {
        final NameIDType issuer = authnRequest.getIssuer();
        if ( issuer == null ) {
            logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION, "Issuer is required" );
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        } else if ( issuer.getFormat() != null && !SamlConstants.NAMEIDENTIFIER_ENTITY.equals( issuer.getFormat() ) ) {
            logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION, "Issuer format must be " + SamlConstants.NAMEIDENTIFIER_ENTITY );
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        final SubjectType subject = authnRequest.getSubject();
        if ( subject != null ) {
            final List<JAXBElement<?>> contents = subject.getContent();
            if ( contents != null ) {
                for ( JAXBElement<?> content : contents ) {
                    if ( SubjectConfirmationType.class.isAssignableFrom( content.getDeclaredType() )) {
                        logAndAudit( AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION, "SubjectConfirmation elements are not permitted" );
                        throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                    }
                }
            }
        }
    }

    private Message getOrCreateAuthnRequestMessage( final PolicyEnforcementContext context ) {
        final String requestMessageVariableName = prefix(SUFFIX_REQUEST);
        Message requestMessage;
        try {
            requestMessage = context.getOrCreateTargetMessage( new MessageTargetableSupport(requestMessageVariableName), false );
        } catch ( NoSuchVariableException e ) {
            logAndAudit( Messages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{"Unexpected error creating message variable"},
                    e );
            throw new AssertionStatusException( AssertionStatus.FAILED );
        }
        return requestMessage;
    }

    private String getCertBase64( final SignatureType signature ) {
        String certBase64 = null;
        if ( signature != null ) {
            final KeyInfoType keyInfoType = signature.getKeyInfo();
            if ( keyInfoType != null ) {
                final List<Object> contents = keyInfoType.getContent();
                if ( contents != null ) {
                    outer:
                    for ( Object content : contents ) {
                        if ( content instanceof JAXBElement && X509DataType.class.isAssignableFrom(((JAXBElement)content).getDeclaredType()) ) {
                            final X509DataType x509Data = (X509DataType)((JAXBElement) content).getValue();
                            final List<Object> dataContents = x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName();
                            if ( dataContents != null ) {
                                for ( Object dataContent : dataContents ) {
                                    if ( dataContent instanceof JAXBElement ) {
                                        JAXBElement element = (JAXBElement) dataContent;
                                        if ( element.getValue() instanceof byte[] ) {
                                            if ( certBase64 != null ) {
                                                logAndAudit( AssertionMessages.SAMLP_PROCREQ_SIGNING_ERROR, "Multiple certificates in request" );
                                                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                                            }
                                            certBase64 = HexUtils.encodeBase64( (byte[]) element.getValue(), true );
                                            if ( allowMultipleCertificates ) break outer;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return certBase64;
    }

    private boolean isSetVariables() {
        return assertion.getVariablePrefix() != null && !assertion.getVariablePrefix().isEmpty();
    }

    private String prefix( final String variableName ) {
        return assertion.getVariablePrefix() + "." + variableName;
    }
}
