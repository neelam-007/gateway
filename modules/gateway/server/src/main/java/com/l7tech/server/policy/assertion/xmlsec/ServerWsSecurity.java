package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        super( assertion );
        this.trustedCertCache = context.getBean( "trustedCertCache", TrustedCertCache.class );
        this.wssDecorator = context.getBean( "wssDecorator", WssDecorator.class );
    }

    //- PROTECTED

    @SuppressWarnings({ "deprecation" })
    @Override
    protected  AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                               final Message message,
                                               final String messageDescription,
                                               final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        try {
            if ( !message.isSoap() ) {
                logAndAudit( AssertionMessages.WSSECURITY_NON_SOAP );
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch ( SAXException e ) {
            logAndAudit( AssertionMessages.WSSECURITY_NON_SOAP );
            return AssertionStatus.NOT_APPLICABLE;
        }

        final SecurityKnob securityKnob = message.getSecurityKnob();
        try {
            final Document document = message.getXmlKnob().getDocumentWritable();
            List<String> securityHeaderActors = new ArrayList<String>();
            if ( assertion.isApplyWsSecurity() ) {
                final DecorationRequirements[] decorations = securityKnob.getDecorationRequirements();
                final SoapVersion soapVersion = SoapVersion.namespaceToSoapVersion(document.getDocumentElement().getNamespaceURI());

                if ( assertion.getWsSecurityVersion() != null )
                    securityKnob.setPolicyWssVersion(assertion.getWsSecurityVersion());

                if (decorations != null && decorations.length > 0) {
                    for (DecorationRequirements decoration : decorations) {
                        // process headers
                        processSecurityHeaderActor(context, decoration, assertion.isUseSecureSpanActor(), securityHeaderActors);
                        processSecurityHeader(decoration, document, soapVersion, securityKnob,
                                assertion.isReplaceSecurityHeader(),
                                assertion.isUseSecurityHeaderMustUnderstand());

                        // process certificate
                        processRecipientCertificate(
                                        decoration,
                                        assertion.getRecipientTrustedCertificateOid(),
                                        expandVariables(context, assertion.getRecipientTrustedCertificateName()),
                                        expandCertificate( context, assertion.getRecipientTrustedCertificateVariable() ));

                        // run decoration
                        try {
                            // add signature confirmations
                            WSSecurityProcessorUtils.addSignatureConfirmations(message, getAudit());
                            wssDecorator.decorateMessage(message, decoration);
                        } catch ( DecoratorException de ) {
                            logAndAudit( AssertionMessages.WSSECURITY_ERROR, new String[] { assertion.getTargetName(), ExceptionUtils.getMessage( de ) }, ExceptionUtils.getDebugException(de) );
                            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                        }
                    }
                    
                    securityKnob.removeAllDecorationRequirements();
                }
            }

            if ( assertion.isRemoveUnmatchedSecurityHeaders() ) {
                // If the L7_SOAP_ACTOR is used we have to match the element since the actor can be multiple values 
                Element l7SecHeader = securityHeaderActors.contains(SoapConstants.L7_SOAP_ACTOR) ?
                        SoapUtil.getSecurityElementForL7(document) :
                        null;
                for ( Element securityElement : SoapUtil.getSecurityElements(document) ) {
                    String actor = SoapUtil.getActorValue(securityElement);
                    if ( !securityHeaderActors.contains(actor) && l7SecHeader!=securityElement ) {
                        securityElement.getParentNode().removeChild(securityElement);
                        securityKnob.removeDecorationResults(actor);
                    }
                }
                SoapUtil.removeEmptySoapHeader(document);
            }

            if ( assertion.isClearDecorationRequirements() ) {
                securityKnob.removeAllDecorationRequirements();
                context.addDeferredAssertion( this, new AbstractServerAssertion<Assertion>(assertion){
                    @Override
                    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
                        securityKnob.removeAllDecorationRequirements();
                        return AssertionStatus.NONE;
                    }
                } );
            }

            return AssertionStatus.NONE;
        } catch (InvalidDocumentFormatException e) {
            logAndAudit( AssertionMessages.WSSECURITY_ERROR, new String[]{ assertion.getTargetName(), ExceptionUtils.getMessage( e ) }, e );
        } catch (GeneralSecurityException e) {
            logAndAudit( AssertionMessages.WSSECURITY_ERROR, new String[]{ assertion.getTargetName(), ExceptionUtils.getMessage( e ) }, e );
        } catch (SAXException e) {
            logAndAudit( AssertionMessages.WSSECURITY_ERROR, new String[]{ assertion.getTargetName(), ExceptionUtils.getMessage( e ) }, e );
        } catch (AssertionStatusException ase) {
            return ase.status;
        }

        return AssertionStatus.FALSIFIED;
    }



    //- PRIVATE

    private final TrustedCertCache trustedCertCache;
    private final WssDecorator wssDecorator;

    private static class AssertionStatusException extends Exception {
        final AssertionStatus status;
        AssertionStatusException( final AssertionStatus status ) {
            this.status = status;
        }
    }

    /**
     * Process the actor/role for the security heder
     */
    private void processSecurityHeaderActor( final PolicyEnforcementContext context,
                                             final DecorationRequirements decoration,
                                             final boolean useSecureSpanActor,
                                             final List<String> securityHeaderActors ) {
        if ( SoapConstants.L7_SOAP_ACTOR.equals(decoration.getSecurityHeaderActor()) ||
             decoration.getSecurityHeaderActor() == null ) {
            if ( useSecureSpanActor ) {
                String actor = SoapConstants.L7_SOAP_ACTOR;
                if ( isResponse() ) {
                    ProcessorResult result = context.getRequest().getSecurityKnob().getProcessorResult();
                    if ( result != null && result.getProcessedActorUri() != null ) {
                        actor = result.getProcessedActorUri();
                    }
                }
                decoration.setSecurityHeaderActor( actor );
                securityHeaderActors.add( actor );
            } else {
                decoration.setSecurityHeaderActor( null );
                securityHeaderActors.add( null );
            }
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
                                        final SecurityKnob securityKnob,
                                        final boolean replaceSecurityHeader,
                                        final boolean useSecurityHeaderMustUnderstand )
            throws InvalidDocumentFormatException {
        String securityHeaderActor = decoration.getSecurityHeaderActor();
        Element securityHeader = SoapUtil.getSecurityElement(document, securityHeaderActor);
        if ( securityHeader != null ) {
            if ( replaceSecurityHeader ) {
                securityHeader.getParentNode().removeChild( securityHeader );
                decoration.setSecurityHeaderMustUnderstand( useSecurityHeaderMustUnderstand, false );
                securityKnob.removeDecorationResults(securityHeaderActor);
            } else {
                final String soapUri = document.getDocumentElement().getNamespaceURI();
                decoration.setSecurityHeaderReusable(true);
                if ( SoapVersion.SOAP_1_2.isPriorVersion( soapVersion ) ) {
                    // SOAP 1.1 or earlier
                    boolean defaultNS = securityHeader.hasAttribute( SoapConstants.ACTOR_ATTR_NAME );
                    securityHeader.removeAttribute( SoapConstants.ACTOR_ATTR_NAME );
                    securityHeader.removeAttributeNS( soapUri, SoapConstants.ACTOR_ATTR_NAME );
                    if ( securityHeaderActor != null ) {
                        if ( defaultNS ) {
                            securityHeader.setAttributeNS( null, SoapConstants.ACTOR_ATTR_NAME, securityHeaderActor);
                        } else {
                            SoapUtil.setSoapAttr( securityHeader, SoapConstants.ACTOR_ATTR_NAME, securityHeaderActor);
                        }
                    }
                    if ( useSecurityHeaderMustUnderstand )
                        SoapUtil.setSoapAttr( securityHeader, SoapConstants.MUSTUNDERSTAND_ATTR_NAME, "1");
                    else
                        securityHeader.removeAttributeNS( soapUri, SoapConstants.MUSTUNDERSTAND_ATTR_NAME );
                } else {
                    // SOAP 1.2 or later
                    securityHeader.removeAttributeNS( soapUri, SoapConstants.ROLE_ATTR_NAME );
                    if ( securityHeaderActor != null )
                        SoapUtil.setSoapAttr( securityHeader, SoapConstants.ROLE_ATTR_NAME, securityHeaderActor);
                    if ( useSecurityHeaderMustUnderstand )
                        SoapUtil.setSoapAttr( securityHeader, SoapConstants.MUSTUNDERSTAND_ATTR_NAME, "true");
                    else
                        securityHeader.removeAttributeNS( soapUri, SoapConstants.MUSTUNDERSTAND_ATTR_NAME );
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
                                              final Goid trustedCertificateOid,
                                              @Nullable final String trustedCertificateName,
                                              @Nullable final X509Certificate recipientCertificate ) throws AssertionStatusException {
        String description = "";

        try {
            if ( trustedCertificateOid != null ) {
                description = "id #" + trustedCertificateOid;
                TrustedCert trustedCertificate = trustedCertCache.findByPrimaryKey( trustedCertificateOid );
                if ( trustedCertificate != null ) {
                    decoration.setRecipientCertificate( trustedCertificate.getCertificate() );
                } else {
                    logAndAudit( AssertionMessages.WSSECURITY_RECIP_NO_CERT, description );
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }
            } else if ( trustedCertificateName != null ) {
                description = "name " + trustedCertificateName;
                Collection<TrustedCert> trustedCertificates = trustedCertCache.findByName( trustedCertificateName );
                X509Certificate certificate = null;
                X509Certificate expiredCertificate = null;
                for ( TrustedCert trustedCert : trustedCertificates ) {
                    if ( !isExpiredCert(trustedCert) ) {
                        certificate = trustedCert.getCertificate();
                        break;
                    } else if ( expiredCertificate == null ) {
                        expiredCertificate = trustedCert.getCertificate();
                    }
                }
                
                if ( certificate != null || expiredCertificate != null ) {
                    decoration.setRecipientCertificate( certificate!=null ? certificate : expiredCertificate );
                } else {
                    logAndAudit( AssertionMessages.WSSECURITY_RECIP_NO_CERT, description );
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }
            } else {
                decoration.setRecipientCertificate( recipientCertificate );
            }
        } catch ( FindException e ) {
            logAndAudit( AssertionMessages.WSSECURITY_RECIP_CERT_ERROR, description );
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }
    }

    private boolean isExpiredCert( final TrustedCert trustedCert ) {
        boolean expired = true;

        try {
            expired = trustedCert.isExpiredCert();
        } catch (CertificateException e) {
            logAndAudit( AssertionMessages.WSSECURITY_RECIP_CERT_EXP, new String[]{ trustedCert.getName() + " (#" + trustedCert.getGoid() + ")" }, e );
        }

        return expired;
    }

    private String expandVariables( final PolicyEnforcementContext context, 
                                    final String variableText ) throws AssertionStatusException {
        String expanded = null;

        if ( variableText != null ) {
            final String[] variablesUsed = Syntax.getReferencedNames(variableText);
            final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
            try {
                expanded = ExpandVariables.process(variableText, vars, getAudit(), true);
            } catch ( IllegalArgumentException iae ) {
                throw new AssertionStatusException(AssertionStatus.FALSIFIED); // already audited
            }
        }

        return expanded;
    }

    private X509Certificate expandCertificate( final PolicyEnforcementContext context,
                                               final String variableText ) throws AssertionStatusException {
        X509Certificate expanded = null;

        if ( variableText != null ) {
            final String expression = Syntax.getVariableExpression(variableText);
            final String[] variablesUsed = Syntax.getReferencedNames(expression);
            final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
            try {
                final Object expandedObject = ExpandVariables.processSingleVariableAsObject(expression, vars, getAudit(), true);
                if ( expandedObject instanceof X509Certificate ) {
                    expanded = (X509Certificate) expandedObject;
                }
            } catch ( IllegalArgumentException iae ) {
                throw new AssertionStatusException(AssertionStatus.FALSIFIED); // already audited
            }

            if ( expanded == null ) {
                logAndAudit( AssertionMessages.WSSECURITY_RECIP_NO_CERT, "variable " + variableText );
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }
        }

        return expanded;
    }
}
