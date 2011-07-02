package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.BuildRstSoapRequest;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.wstrust.TokenServiceClient;
import com.l7tech.security.wstrust.WsTrustConfig;
import com.l7tech.security.wstrust.WsTrustConfigException;
import com.l7tech.security.wstrust.WsTrustConfigFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.util.Config;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import static com.l7tech.util.SoapConstants.*;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidatedConfig;
import com.l7tech.xml.WsTrustRequestType;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 */
public class ServerBuildRstSoapRequest extends AbstractServerAssertion<BuildRstSoapRequest> {

    //- PUBLIC

    public ServerBuildRstSoapRequest( final BuildRstSoapRequest assertion,
                                      final BeanFactory factory ) {
        super(assertion);
        config = validated( factory.getBean( "serverConfig", Config.class ), logger );
        variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        final SecurityTokenType securityTokenType = assertion.getTokenType();
        final SoapVersion soapVersion = assertion.getSoapVersion();
        final String wsTrustNamespace = assertion.getWsTrustNamespace();
        final WsTrustRequestType type = assertion.getRequestType();
        final WsTrustConfig wsTrustConfig = getWsTrustConfigForNamespaceUri( wsTrustNamespace );
        final TokenServiceClient client = new TokenServiceClient( wsTrustConfig, null, soapVersion );

        final int entropySizeBytes = assertion.getKeySize() != null ? assertion.getKeySize() / 8 : 32;
        final byte[] entropy = assertion.isIncludeClientEntropy() ?
                HexUtils.randomBytes( entropySizeBytes ) :
                null;
        final long lifetime = assertion.getLifetime() == null ?
                0L :
                assertion.getLifetime() == BuildRstSoapRequest.SYSTEM_LIFETIME ?
                    config.getTimeUnitProperty( CONFIG_DEFAULT_SESSION_DURATION, TimeUnit.HOURS.toMillis( 2L ) ):
                    assertion.getLifetime();

        final Map<String,Object> vars = context.getVariableMap( variablesUsed, getAudit() );

        final Element target;
        if ( assertion.getTargetTokenVariable()!=null ) {
            final Object targetToken = ExpandVariables.processSingleVariableAsObject( Syntax.getVariableExpression(assertion.getTargetTokenVariable()), vars, getAudit() );
            if ( targetToken instanceof Element ) {
                target = (Element) targetToken;
            } else if ( targetToken instanceof Document ) {
                target = ((Document) targetToken).getDocumentElement();
            } else if ( targetToken instanceof SecureConversationSession ) {
                target = buildSecurityTokenReference( (SecureConversationSession) targetToken );
            } else if ( targetToken instanceof String ) {
                try {
                    target = XmlUtil.parse( (String) targetToken ).getDocumentElement();
                } catch ( SAXException e ) {
                    logAndAudit( AssertionMessages.RST_BUILDER_ERROR, "Invalid token value: " + ExceptionUtils.getMessage( e ) );
                    return AssertionStatus.FAILED;
                }
            } else if ( targetToken == null ) {
                target = null;
            } else {
                logAndAudit( AssertionMessages.RST_BUILDER_ERROR, "Unsupported token variable type: " + targetToken.getClass() );
                return AssertionStatus.FAILED;
            }
        } else {
            target = null;
        }

        final Document rstDocument = client.createRequestSecurityTokenMessage(
                securityTokenType,
                type,
                target,
                expandVariables( assertion.getAppliesToAddress(), vars ),
                expandVariables( assertion.getIssuerAddress(), vars ),
                entropy,
                assertion.getKeySize()==null ? 0 : assertion.getKeySize(),
                lifetime );

        try {
            final Message output = context.getOrCreateTargetMessage( new MessageTargetableSupport( prefix(BuildRstSoapRequest.VARIABLE_RST_REQUEST) ) , false );
            output.initialize(
                    rstDocument,
                    soapVersion==SoapVersion.SOAP_1_1 ?
                            ContentTypeHeader.XML_DEFAULT :
                            ContentTypeHeader.SOAP_1_2_DEFAULT );
            output.getSecurityKnob().getOrMakeDecorationRequirements().getNamespaceFactory().setWsscNs( wsTrustConfig.getWsscNs() );
        } catch ( NoSuchVariableException e ) {
            logAndAudit( AssertionMessages.RST_BUILDER_OUTPUT, e.getVariable() );
            return AssertionStatus.FAILED;
        }
        if ( entropy != null ) {
            context.setVariable( prefix(BuildRstSoapRequest.VARIABLE_CLIENT_ENTROPY), HexUtils.encodeBase64(entropy) );
        }

        return AssertionStatus.NONE;
    }

    //- PRIVATE

    private static final String CONFIG_DEFAULT_SESSION_DURATION = "outbound.secureConversation.defaultSessionDuration";
    private static final long MIN_SESSION_DURATION = (long) (1000 * 60); // 1 min
    private static final long MAX_SESSION_DURATION = (long) (1000 * 60 * 60 * 24); // 24 hrs

    private final Config config;
    private final String[] variablesUsed;

    private WsTrustConfig getWsTrustConfigForNamespaceUri( final String wsTrustNamespace ) {
        try {
            return WsTrustConfigFactory.getWsTrustConfigForNamespaceUri( wsTrustNamespace );
        } catch ( WsTrustConfigException e ) {
            logAndAudit( AssertionMessages.RST_BUILDER_ERROR, ExceptionUtils.getMessage( e ) );
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }
    }

    private Element buildSecurityTokenReference( final SecureConversationSession session ) {
        final Document strDoc = XmlUtil.createEmptyDocument( SECURITYTOKENREFERENCE_EL_NAME, SECURITY_NAMESPACE_PREFIX, SECURITY_NAMESPACE );
        final Element strElement = strDoc.getDocumentElement();
        final Element referenceElement = DomUtils.createAndAppendElementNS(strElement, REFERENCE_EL_NAME, SECURITY_NAMESPACE, SECURITY_NAMESPACE_PREFIX);
        referenceElement.setAttributeNS( null, "URI", session.getIdentifier() );

        final String tokenUri;
        if ( WSSC_NAMESPACE.equals( session.getSCNamespace() )) {
            tokenUri = WSC_RST_SCT_TOKEN_TYPE;
        } else {
            tokenUri = session.getSCNamespace() + "/sct";
        }

        referenceElement.setAttributeNS( null, "ValueType", tokenUri );

        return strElement;
    }

    private String expandVariables( final String issuerAddress, final Map<String, Object> vars ) {
        String text = null;

        if ( issuerAddress != null ) {
            text = ExpandVariables.process( issuerAddress, vars, getAudit() );
        }

        return text;
    }

    private String prefix( final String name ) {
        String prefixed = name;

        if ( assertion.getVariablePrefix() != null ) {
            prefixed = assertion.getVariablePrefix()  + "." + name;
        }

        return prefixed;
    }

    private static Config validated( final Config config, final Logger logger ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger );

        vc.setMinimumValue( CONFIG_DEFAULT_SESSION_DURATION, MIN_SESSION_DURATION );
        vc.setMaximumValue( CONFIG_DEFAULT_SESSION_DURATION, MAX_SESSION_DURATION );

        return vc;
    }

}
