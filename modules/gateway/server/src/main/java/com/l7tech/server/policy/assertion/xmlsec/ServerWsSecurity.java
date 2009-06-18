package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.objectmodel.FindException;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.*;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.springframework.context.ApplicationContext;

/**
 * Applies collected and pending WSS message decorations; all decorations requirements are removed once they are applied.
 *
 * The decoration requirements are assumed to be in a state ready to be applied, i.e. no further processing is done
 * on them (such as setting default namespaces).
 *
 * Security headers that already exist in the message target are reused.
 *
 * @author jbufu
 */
public class ServerWsSecurity extends AbstractMessageTargetableServerAssertion<WsSecurity> {

    //- PUBLIC

    public ServerWsSecurity( final WsSecurity assertion, final ApplicationContext context ) {
        super( assertion, assertion );
        this.auditor = new Auditor(this, context, logger);
        this.trustedCertCache =  (TrustedCertCache) context.getBean( "trustedCertCache", TrustedCertCache.class );
    }

    //- PROTECTED

    @Override
    protected  AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                               final Message message,
                                               final String messageDescription,
                                               final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        try {
            if ( !message.isSoap() ) {
                auditor.logAndAudit(AssertionMessages.WSSECURITY_NON_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch ( SAXException e ) {
            auditor.logAndAudit(AssertionMessages.WSSECURITY_NON_SOAP);
            return AssertionStatus.NOT_APPLICABLE;
        }


        try {
            final Document document = message.getXmlKnob().getDocumentWritable();
            List<String> securityHeaderActors = new ArrayList<String>();
            if ( assertion.isApplyWsSecurity() ) {
                final SecurityKnob secKnob = message.getSecurityKnob();
                final DecorationRequirements[] decorations = secKnob.getDecorationRequirements();
                final SoapVersion soapVersion = SoapVersion.namespaceToSoapVersion(document.getNamespaceURI());

                //TODO [steve] set WS-Security version
                if (decorations != null && decorations.length > 0) {
                    WssDecorator decorator = new WssDecoratorImpl();
                    for (DecorationRequirements decoration : decorations) {
                        // process headers
                        processSecurityHeaderActor(decoration, assertion.isUseSecureSpanActor(), securityHeaderActors);
                        processSecurityHeader(decoration, document, soapVersion,
                                assertion.isReplaceSecurityHeader(),
                                assertion.isUseSecurityHeaderMustUnderstand());

                        // process certificate
                        processRecipientCertificate(
                                        decoration,
                                        assertion.getRecipientTrustedCertificateOid(),
                                        expandVariables(context, assertion.getRecipientTrustedCertificateName()) );

                        // run decoration
                        try {
                            decorator.decorateMessage(message, decoration);
                        } catch ( DecoratorException de ) {
                            auditor.logAndAudit(AssertionMessages.WSSECURITY_ERROR, assertion.getTargetName(), ExceptionUtils.getMessage(de));
                            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                        }
                    }
                    
                    secKnob.removeAllDecorationRequirements();
                }
            }

            if ( assertion.isRemoveUnmatchedSecurityHeaders() ) {
                Element l7SecHeader = securityHeaderActors.contains(null) ?
                        SoapUtil.getSecurityElementForL7(document) :
                        null;
                for ( Element securityElement : SoapUtil.getSecurityElements(document) ) {
                    String actor = SoapUtil.getActorValue(securityElement);
                    if ( !securityHeaderActors.contains(actor) && l7SecHeader!=securityElement ) {
                        securityElement.getParentNode().removeChild(securityElement);
                    }
                }
                SoapUtil.removeEmptySoapHeader(document);
            }

            return AssertionStatus.NONE;
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.WSSECURITY_ERROR, new String[]{assertion.getTargetName(), ExceptionUtils.getMessage(e)}, e);
        } catch (GeneralSecurityException e) {
            auditor.logAndAudit(AssertionMessages.WSSECURITY_ERROR, new String[]{assertion.getTargetName(), ExceptionUtils.getMessage(e)}, e);
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.WSSECURITY_ERROR, new String[]{assertion.getTargetName(), ExceptionUtils.getMessage(e)}, e);
        } catch (AssertionStatusException ase) {
            return ase.status;
        }

        return AssertionStatus.FALSIFIED;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerWsSecurity.class.getName());
    private final Auditor auditor;
    private final TrustedCertCache trustedCertCache;

    private static class AssertionStatusException extends Exception {
        final AssertionStatus status;
        AssertionStatusException( final AssertionStatus status ) {
            this.status = status;
        }
    }

    /**
     * Process the actor/role for the security heder
     */
    private void processSecurityHeaderActor( final DecorationRequirements decoration,
                                             final boolean useSecureSpanActor,
                                             final List<String> securityHeaderActors ) {
        if ( SoapConstants.L7_SOAP_ACTOR.equals(decoration.getSecurityHeaderActor()) ||
             decoration.getSecurityHeaderActor() == null ) {
            if ( useSecureSpanActor ) {
                decoration.setSecurityHeaderActor( SoapConstants.L7_SOAP_ACTOR );
            } else {
                decoration.setSecurityHeaderActor( null );
            }
            securityHeaderActors.add( null );
        } else {
            securityHeaderActors.add( decoration.getSecurityHeaderActor() );
        }
    }

    /**
     * Process the security header and set the actor/role and mustUnderstand attribute.
     */
    private void processSecurityHeader( final DecorationRequirements decoration,
                                        final Document document,
                                        final SoapVersion soapVersion,
                                        final boolean replaceSecurityHeader,
                                        final boolean useSecurityHeaderMustUnderstand )
            throws InvalidDocumentFormatException {
        Element securityHeader = SoapUtil.getSecurityElement(document, decoration.getSecurityHeaderActor());
        if ( securityHeader != null ) {
            if ( replaceSecurityHeader ) {
                securityHeader.getParentNode().removeChild( securityHeader );
                decoration.setSecurityHeaderMustUnderstand( useSecurityHeaderMustUnderstand, false );
            } else {
                decoration.setSecurityHeaderReusable(true);
                if ( SoapVersion.SOAP_1_2.isPriorVersion( soapVersion ) ) {
                    securityHeader.removeAttributeNS( document.getNamespaceURI(), SoapConstants.ACTOR_ATTR_NAME );
                    if ( decoration.getSecurityHeaderActor() != null )
                        SoapUtil.setSoapAttr( securityHeader, SoapConstants.ACTOR_ATTR_NAME, decoration.getSecurityHeaderActor() );
                } else {
                    securityHeader.removeAttributeNS( document.getNamespaceURI(), SoapConstants.ROLE_ATTR_NAME );
                    if ( decoration.getSecurityHeaderActor() != null )
                        SoapUtil.setSoapAttr( securityHeader, SoapConstants.ROLE_ATTR_NAME, decoration.getSecurityHeaderActor() );
                }
            }
        } else {
            decoration.setSecurityHeaderMustUnderstand( useSecurityHeaderMustUnderstand, false );
        }
    }

    /**
     * 
     */
    private void processRecipientCertificate( final DecorationRequirements decoration,
                                              final long trustedCertificateOid,
                                              final String trustedCertificateName ) throws AssertionStatusException {
        String description = "";

        try {
            if ( trustedCertificateOid > 0 ) {
                description = "id #" + trustedCertificateOid;
                TrustedCert trustedCertificate = trustedCertCache.findByPrimaryKey( trustedCertificateOid );
                if ( trustedCertificate != null ) {
                    decoration.setRecipientCertificate( trustedCertificate.getCertificate() );
                } else {
                    auditor.logAndAudit(AssertionMessages.WSSECURITY_RECIP_NO_CERT, description);
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }
            } else if ( trustedCertificateName != null ) {
                description = "name " + trustedCertificateName;
                Collection<TrustedCert> trustedCertificates = trustedCertCache.findByName( trustedCertificateName );
                X509Certificate certificate = null;
                for ( TrustedCert trustedCert : trustedCertificates ) {
                    if ( !isExpiredCert(trustedCert) ) {
                        certificate = trustedCert.getCertificate();
                        break;
                    }
                }
                
                if ( certificate != null ) {
                    decoration.setRecipientCertificate( certificate );
                } else {
                    auditor.logAndAudit(AssertionMessages.WSSECURITY_RECIP_NO_CERT, description);
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }
            }
        } catch ( FindException e ) {
            auditor.logAndAudit(AssertionMessages.WSSECURITY_RECIP_CERT_ERROR, description);
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }
    }

    private boolean isExpiredCert( final TrustedCert trustedCert ) {
        boolean expired = true;

        try {
            expired = trustedCert.isExpiredCert();
        } catch (CertificateException e) {
            auditor.logAndAudit(AssertionMessages.WSSECURITY_RECIP_CERT_EXP, new String[]{ trustedCert.getName() + " (#"+trustedCert.getOid()+")"}, e);
        }

        return expired;
    }

    private String expandVariables( final PolicyEnforcementContext context, 
                                    final String variableText ) throws AssertionStatusException {
        String expanded = null;

        if ( variableText != null ) {
            final String[] variablesUsed = Syntax.getReferencedNames(variableText);
            final Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);
            try {
                expanded = ExpandVariables.process(variableText, vars, auditor, true);
            } catch ( IllegalArgumentException iae ) {
                throw new AssertionStatusException(AssertionStatus.FALSIFIED); // already audited
            }
        }

        return expanded;
    }

}
