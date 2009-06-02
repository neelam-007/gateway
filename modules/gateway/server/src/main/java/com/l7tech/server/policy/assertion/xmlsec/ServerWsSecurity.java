package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.*;

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
 * TODO [steve] auditing, cleanup exception handling
 *
 * @author jbufu
 */
public class ServerWsSecurity extends AbstractMessageTargetableServerAssertion<WsSecurity> {

    //- PUBLIC

    public ServerWsSecurity( final WsSecurity assertion, final ApplicationContext context ) {
        super( assertion, assertion );
        this.auditor = new Auditor(this, context, logger);
        this.trustedCertManager =  (TrustedCertManager) context.getBean( "trustedCertManager", TrustedCertManager.class );
    }

    //- PROTECTED

    @Override
    protected  AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                           final Message message,
                                           final String messageDescription,
                                           final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        try {
            if ( !message.isSoap() ) {
                auditor.logAndAudit(AssertionMessages.WSS_DECORATION_NON_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch ( SAXException e ) {
            auditor.logAndAudit(AssertionMessages.WSS_DECORATION_NON_SOAP);
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
                        if ( SoapConstants.L7_SOAP_ACTOR.equals(decoration.getSecurityHeaderActor()) ||
                             decoration.getSecurityHeaderActor() == null ) {
                            if ( assertion.isUseSecureSpanActor() ) {
                                decoration.setSecurityHeaderActor( SoapConstants.L7_SOAP_ACTOR );
                            } else {
                                decoration.setSecurityHeaderActor( null );
                            }
                            securityHeaderActors.add( null );
                        } else {
                            securityHeaderActors.add( decoration.getSecurityHeaderActor() );                            
                        }

                        Element securityHeader = SoapUtil.getSecurityElement(document, decoration.getSecurityHeaderActor());
                        if ( securityHeader != null ) {
                            if ( assertion.isReplaceSecurityHeader() ) {
                                securityHeader.getParentNode().removeChild( securityHeader );                                
                                decoration.setSecurityHeaderMustUnderstand( assertion.isUseSecurityHeaderMustUnderstand(), false );
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
                            decoration.setSecurityHeaderMustUnderstand( assertion.isUseSecurityHeaderMustUnderstand(), false );
                        }

                        if ( assertion.getRecipientTrustedCertificateOid() > 0 ) {
                            TrustedCert trustedCertificate = trustedCertManager.findByPrimaryKey( assertion.getRecipientTrustedCertificateOid() );
                            if ( trustedCertificate != null ) {
                                decoration.setRecipientCertificate( trustedCertificate.getCertificate() );
                            }

                        } else if ( assertion.getRecipientTrustedCertificateName() != null ) {
                            Collection<TrustedCert> trustedCertificates = trustedCertManager.findByName( assertion.getRecipientTrustedCertificateName() );
                            for ( TrustedCert trustedCert : trustedCertificates ) {
                                if ( !trustedCert.isExpiredCert() ) {
                                    decoration.setRecipientCertificate( trustedCert.getCertificate() );
                                    break;
                                }
                            }
                        }

                        decorator.decorateMessage(message, decoration);
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
            }

            return AssertionStatus.NONE;
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.WSS_DECORATION_ERROR, new String[]{assertion.getTargetName(), ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.FAILED;
        }
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerWsSecurity.class.getName());
    private final Auditor auditor;
    private final TrustedCertManager trustedCertManager;
}
