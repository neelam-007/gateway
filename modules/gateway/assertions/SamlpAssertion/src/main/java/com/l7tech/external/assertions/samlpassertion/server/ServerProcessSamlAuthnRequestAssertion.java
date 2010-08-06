package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAuthnRequestAssertion;
import static com.l7tech.external.assertions.samlpassertion.ProcessSamlAuthnRequestAssertion.*;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import saml.support.ds.KeyInfoType;
import saml.support.ds.SignatureType;
import saml.support.ds.X509DataType;
import saml.v2.assertion.NameIDType;
import saml.v2.assertion.SubjectType;
import saml.v2.protocol.AuthnRequestType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

/**
 * Server assertion for Process SAML AuthnRequest assertion
 *
 * TODO [steve] auditing
 */
public class ServerProcessSamlAuthnRequestAssertion extends AbstractMessageTargetableServerAssertion<ProcessSamlAuthnRequestAssertion> {

    //- PUBLIC

    public ServerProcessSamlAuthnRequestAssertion( final ProcessSamlAuthnRequestAssertion assertion,
                                                   final ApplicationContext applicationContext ) throws ServerPolicyException {
        super( assertion, assertion );
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    //- PROTECTED

    @Override
    protected Audit getAuditor() {
        return auditor;
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        Element samlpAuthnRequest = null;
        if ( assertion.getSamlProtocolBinding() == null ) {
            try {
                if ( message.isSoap() ) {
                    samlpAuthnRequest = SoapUtil.getPayloadElement( message.getXmlKnob().getDocumentReadOnly() );
                } else if ( message.isXml() ) {
                    samlpAuthnRequest = message.getXmlKnob().getDocumentReadOnly().getDocumentElement();
                }
            } catch ( SAXException e ) {
                auditor.logAndAudit( Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ ExceptionUtils.getMessage(e)}, e );
            } catch ( InvalidDocumentFormatException e ) {
                auditor.logAndAudit( Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ ExceptionUtils.getMessage(e)}, e );
            }
        } else {
            final HttpRequestKnob hrk = message.getKnob(HttpRequestKnob.class);
            if ( hrk != null ) {
                InputStream samlRequestIn = null;
                try {
                    switch ( assertion.getSamlProtocolBinding() ) {
                        case HttpPost:
                            {
                                final String samlRequest = hrk.getParameter( "SAMLRequest" );
                                if ( samlRequest != null ) {
                                    samlRequestIn = new ByteArrayInputStream(HexUtils.decodeBase64( samlRequest, true ));
                                } else {
                                    // TODO audit                                    
                                }
                            }
                            break;
                        case HttpRedirect:
                            {
                                final String encoding = hrk.getParameter( "SAMLEncoding" );
                                if ( encoding == null || "urn:oasis:names:tc:SAML:2.0:bindings:URL-Encoding:DEFLATE".equals(encoding) ) {
                                    String samlRequest = hrk.getParameter( "SAMLRequest" );
                                    if ( samlRequest != null ) {
                                        samlRequest = URLDecoder.decode( samlRequest, "utf-8" );
                                        final byte[] samlRequestData = HexUtils.decodeBase64( samlRequest, true );    
                                        samlRequestIn =  new InflaterInputStream(new ByteArrayInputStream(samlRequestData));
                                    } else {
                                        // TODO audit
                                    }
                                } else {
                                    // TODO audit
                                }
                            }
                            break;
                    }

                    if ( samlRequestIn != null ) {
                        try {
                            samlpAuthnRequest = XmlUtil.parse( samlRequestIn ).getDocumentElement(); //TODO [steve] check for BOM?
                        } catch ( SAXException e ) {
                            auditor.logAndAudit( Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ ExceptionUtils.getMessage(e)}, e );
                        }
                    }
                } finally {
                    ResourceUtils.closeQuietly( samlRequestIn );
                }
            } else {
                // TODO audit no hrk                
            }
        }

        if ( samlpAuthnRequest != null ) {
            final AuthnRequestType authnRequest = unmarshal( samlpAuthnRequest );

            final Pair<String, X509Certificate> certData = validateAuthnRequest( authnRequest );
            status = AssertionStatus.NONE;

            if ( isSetVariables() )  {
                context.setVariable( prefix(SUFFIX_SUBJECT), getSubject(authnRequest.getSubject()) );
                context.setVariable( prefix(SUFFIX_X509CERT_BASE64), certData.left );
                context.setVariable( prefix(SUFFIX_X509CERT), certData.right );
                context.setVariable( prefix(SUFFIX_ACS_URL), authnRequest.getAssertionConsumerServiceURL() );
                context.setVariable( prefix(SUFFIX_ID), authnRequest.getID() );
                context.setVariable( prefix(SUFFIX_VERSION), authnRequest.getVersion() );
                context.setVariable( prefix(SUFFIX_ISSUE_INSTANT), getIsoTime(authnRequest.getIssueInstant()) );
                context.setVariable( prefix(SUFFIX_DESTINATION), authnRequest.getDestination() );
                context.setVariable( prefix(SUFFIX_CONSENT), authnRequest.getConsent() );
                context.setVariable( prefix(SUFFIX_ISSUER), getName(authnRequest.getIssuer()) );
                context.setVariable( prefix(SUFFIX_ISSUER_NAME_QUALIFIER), getNameQualifier(authnRequest.getIssuer()) );
                context.setVariable( prefix(SUFFIX_ISSUER_SP_NAME_QUALIFIER), getSPNameQualifier(authnRequest.getIssuer()) );
                context.setVariable( prefix(SUFFIX_ISSUER_FORMAT), getNameFormat(authnRequest.getIssuer()) );
                context.setVariable( prefix(SUFFIX_ISSUER_SP_PROVIDED_ID), getSPProvidedID(authnRequest.getIssuer()) );
                context.setVariable( prefix(SUFFIX_EXTENSIONS), null );
            }
        } else {
            // TODO audit
        }

        return status;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerProcessSamlAuthnRequestAssertion.class.getName());

    private final Auditor auditor;

    private AuthnRequestType unmarshal( final Element authnRequestElement ) {
        AuthnRequestType result = null;
        try {
            //TODO [steve] check the element is the right one ...
            //TODO [steve] don't recreate the context all the time this is extremely slow
            final JAXBContext context = JAXBContext.newInstance( "saml.v2.protocol", ServerProcessSamlAuthnRequestAssertion.class.getClassLoader());
            final Unmarshaller um = context.createUnmarshaller();
            um.setEventHandler( new ValidationEventHandler(){
                @Override
                public boolean handleEvent( final ValidationEvent event ) {
                    return false;
                }
            } );

            Object resultObj = um.unmarshal( authnRequestElement );
            result = ((JAXBElement<AuthnRequestType>) resultObj).getValue();
        } catch ( JAXBException e ) {
            auditor.logAndAudit( Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ ExceptionUtils.getMessage(e)}, e );
        }

        return result;
    }

    /**
     * Validate signature, conditions and any other validation requirements.
     */
    private Pair<String, X509Certificate> validateAuthnRequest( final AuthnRequestType authnRequest ) {
        String certBase64 = getCertBase64( authnRequest.getSignature() );
        X509Certificate certificate = null;

        // TODO validate signature

        // TODO validate conditions (times, audience restriction)

        // TODO additional validation rules
        
//The following will be validated, assertion will fail if these rules are not met:
//
//    * <Issuer> is present. If <Format> is supplied it must be 'urn:oasis:names:tc:SAML:2.0:nameid-format:entity'
//    * No SubjectConfirmation elements should be present.

//        if ( certBase64 == null && certificate != null ) {
//            try {
//                certBase64 = HexUtils.encodeBase64( certificate.getEncoded(), true );
//            } catch ( CertificateEncodingException e ) {
//                logger.log( Level.FINE,
//                        "Unable to generate Base64 data for certificate '"+certificate.getSubjectDN()+"' due to '"+ExceptionUtils.getMessage(e)+"'",
//                        ExceptionUtils.getDebugException( e ));
//            }
//        }

        return new Pair<String, X509Certificate>( certBase64, certificate );
    }

    private String getSubject( final SubjectType subjectType ) {
        String subject = null;

        if ( subjectType != null  ) {
            final List<JAXBElement<?>> content = subjectType.getContent();
            if ( content != null && !content.isEmpty() ) {
                JAXBElement<?> contentElement = content.get( 0 );
                if ( NameIDType.class.isAssignableFrom(contentElement.getDeclaredType()) ) {
                    final JAXBElement<NameIDType> nameIDType = (JAXBElement<NameIDType>) contentElement;
                    subject = nameIDType.getValue().getValue();
                }
            }
        }

        return subject;
    }

    private String getName( final NameIDType nameIDType ) {
        String name = null;
        if ( nameIDType != null  ) {
            name = nameIDType.getValue();
        }
        return name;
    }

    private String getNameQualifier( final NameIDType nameIDType ) {
        String nameQualifier = null;
        if ( nameIDType != null  ) {
            nameQualifier = nameIDType.getNameQualifier();
        }
        return nameQualifier;
    }

    private String getSPNameQualifier( final NameIDType nameIDType ) {
        String spNameQualifier = null;
        if ( nameIDType != null  ) {
            spNameQualifier = nameIDType.getSPNameQualifier();
        }
        return spNameQualifier;
    }

    private String getSPProvidedID( final NameIDType nameIDType ) {
        String format = null;
        if ( nameIDType != null  ) {
            format = nameIDType.getSPProvidedID();
        }
        return format;
    }

    private String getNameFormat( final NameIDType nameIDType ) {
        String format = null;
        if ( nameIDType != null  ) {
            format = nameIDType.getFormat();
        }
        return format;
    }

    private String getIsoTime( final XMLGregorianCalendar xmlCalendar ) {
        String time = null;
        if ( xmlCalendar != null ) {
            time = ISO8601Date.format(xmlCalendar.toGregorianCalendar().getTime());   //TODO [steve] reserialize into original format?
        }
        return time;
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
                            final X509DataType x509Data = ((JAXBElement<X509DataType>) content).getValue();
                            final List<Object> dataContents = x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName();
                            if ( dataContents != null ) {
                                for ( Object dataContent : dataContents ) {
                                    if ( dataContent instanceof JAXBElement ) {
                                        JAXBElement element = (JAXBElement) dataContent;
                                        if ( element.getValue() instanceof byte[] ) {
                                            certBase64 = HexUtils.encodeBase64( (byte[]) element.getValue(), true ); //TODO [steve] check only one cert in message
                                            break outer;
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
