package com.l7tech.server.util;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.audit.MessagesUtil;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.message.SecurityKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.wsp.TypeMappingUtils;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.util.Charsets;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Config;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.EncryptedKey;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;

/**
 * Server side SoapFaultLevel utils.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 8, 2006<br/>
 */
public class SoapFaultManager implements ApplicationContextAware {

    //- PUBLIC

    public SoapFaultManager( final Config config,
                             final AuditContext auditContext,
                             Timer timer) {
        if (timer == null) timer = new Timer("Soap fault manager refresh", true);
        this.config = config;
        this.checker = timer;
        this.auditContext = auditContext;
    }

    /**
     * Read settings from server configuration and assemble a SoapFaultLevel based on the default values.
     */
    public SoapFaultLevel getDefaultBehaviorSettings() {
        // cache at least one minute
        if (fromSettings == null || (System.currentTimeMillis() - lastParsedFromSettings) > 60000) {
            return constructFaultLevelFromServerConfig();
        }
        return fromSettings;
    }

    /**
     * Constructs a soap fault based on the pec and the level desired.
     * 
     * @return returns a Pair of content type, string.  The string may be empty if faultLevel is SoapFaultLevel.DROP_CONNECTION.
     */
    public FaultResponse constructReturningFault( final SoapFaultLevel inFaultLevelInfo,
                                                  final PolicyEnforcementContext pec ) {
        return constructFault( inFaultLevelInfo, pec, false, "Policy Falsified", null );
    }

    /**
     * SOAP faults resulting from an exception that occurs in the processing of the policy.
     *
     * <p>Receiving such a fault may generally be considered a bug (though in some cases it is expected
     * behaviour).</p>
     */
    public FaultResponse constructExceptionFault( final Throwable throwable,
                                                  final SoapFaultLevel inFaultLevelInfo,
                                                  final PolicyEnforcementContext pec ) {
        final SoapFaultLevel faultLevelInfo = inFaultLevelInfo==null ? getDefaultBehaviorSettings() : inFaultLevelInfo;
        final boolean policyVersionFault = pec.isRequestClaimingWrongPolicyVersion();
        if ( policyVersionFault ) {
            SoapFaultLevel policyVersionFaultLevel = new SoapFaultLevel( faultLevelInfo );
            policyVersionFaultLevel.setLevel( SoapFaultLevel.GENERIC_FAULT );
            return constructFault(
                    policyVersionFaultLevel,
                    pec,
                    true,
                    "Incorrect policy version",
                    "" );
        } else {
            return constructFault(
                    inFaultLevelInfo,
                    pec,
                    false,
                    "Error in assertion processing",
                    ExceptionUtils.getMessage(throwable) );
        }
    }

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) {
        setBeanFactory( applicationContext );
        clusterPropertiesManager = applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        final SoapFaultManager soapFaultManager = this;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    soapFaultManager.updateOverrides();
                } catch(Exception e) {
                    logger.log(Level.WARNING, "Error updating message overrides.", e);
                }
            }
        };
        checker.schedule(task, 10000, 56893);
    }

    /**
     * A SOAP Fault or other error response.
     */
    public static class FaultResponse {
        private final int httpStatus;
        private final ContentTypeHeader contentType;
        private final String content;

        FaultResponse( final int httpStatus,
                       final ContentTypeHeader contentType,
                       final String content ) {
            this.httpStatus = httpStatus;
            this.contentType = contentType;
            this.content = content;
        }

        /**
         * Get the HTTP status for the response.
         *
         * @return The HTTP status code.
         */
        public int getHttpStatus() {
            return httpStatus;
        }

        /**
         * Get the content type for the response.
         *
         * @return The content type  (never null)
         */
        public ContentTypeHeader getContentType() {
            return contentType;
        }

        /**
         * Get the content for the response.
         *
         * @return The response body (never null)
         */
        public String getContent() {
            return content;
        }

        /**
         * Get the content for the response as bytes.
         *
         * <p>The encoding will be as per the content type.</p>
         *
         * @return The response body (never null)
         */
        public byte[] getContentBytes() {
            Charset charset = contentType.getEncoding();
            if ( !charset.canEncode() ) {
                charset = Charsets.UTF8; // fallback to UTF-8 rather than fail
            }
            return content.getBytes( charset );
        }
    }

    //- PACKAGE

    /**
     * For tests only
     */
    SoapFaultManager( Config config, AuditContext auditContext ) {
        this.config = config;
        this.checker = null;
        this.auditContext = auditContext;
    }

    void setBeanFactory( BeanFactory context ) throws BeansException {
        auditor = context instanceof ApplicationContext ?
                new Auditor(this, (ApplicationContext)context, logger) :
                new LogOnlyAuditor(logger);
        this.context = context;
    }

    //- PRIVATE

    public static final String FAULT_NS = "http://www.layer7tech.com/ws/policy/fault";

    private static final String FAULT_TEMPLATE_SOAP_1_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>soapenv:Server</faultcode>\n" +
                        "            <faultstring/>\n" +
                        "            <faultactor/>\n" +
                        "            <detail>\n" +
                        "                 <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                        "            </detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

    private static final String FAULT_TEMPLATE_SOAP_1_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\">\n" +
                         "    <soapenv:Body>\n" +
                         "        <soapenv:Fault>\n" +
                         "            <soapenv:Code>\n" +
                         "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
                         "            </soapenv:Code>\n" +
                         "            <soapenv:Reason>\n" +
                         "                <soapenv:Text xml:lang=\"en-US\"/>\n" +
                         "            </soapenv:Reason>\n" +
                         "            <soapenv:Role/>\n" +
                         "            <soapenv:Detail>\n" +
                         "                <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                         "            </soapenv:Detail>\n" +
                         "        </soapenv:Fault>\n" +
                         "    </soapenv:Body>\n" +
                         "</soapenv:Envelope>";

    private final Config config;
    private final Logger logger = Logger.getLogger(SoapFaultManager.class.getName());
    private long lastParsedFromSettings;
    private SoapFaultLevel fromSettings;
    private Auditor auditor;
    private final AuditContext auditContext;
    private ClusterPropertyManager clusterPropertiesManager;
    private BeanFactory context;
    private final HashMap<Integer, String> cachedOverrideAuditMessages = new HashMap<Integer, String>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Timer checker;

    private FaultResponse constructFault( final SoapFaultLevel inFaultLevelInfo,
                                          final PolicyEnforcementContext pec,
                                          final boolean isClientFault,
                                          final String faultString,
                                          final String statusTextOverride ) {
        final SoapFaultLevel faultLevelInfo = inFaultLevelInfo==null ? getDefaultBehaviorSettings() : inFaultLevelInfo;
        int httpStatus = 500;
        String output = "";
        AssertionStatus globalStatus = pec.getPolicyResult();
        ContentTypeHeader contentTypeHeader = ContentTypeHeader.XML_DEFAULT;
        if (globalStatus == null) {
            logger.fine("PolicyEnforcementContext.policyResult not set. Fallback on UNDEFINED");
            globalStatus = AssertionStatus.UNDEFINED;
        }
        int level = faultLevelInfo.getLevel();
        switch ( level ) {
            case SoapFaultLevel.DROP_CONNECTION:
                break;
            case SoapFaultLevel.TEMPLATE_FAULT:
                Map<String,Object> variables = pec.getVariableMap(faultLevelInfo.getVariablesUsed(), auditor);
                if ( faultLevelInfo.getFaultTemplateHttpStatus() != null ) {
                    String httpStatusText = ExpandVariables.process( faultLevelInfo.getFaultTemplateHttpStatus(), variables, auditor );
                    try {
                        httpStatus = Integer.parseInt( httpStatusText );
                    } catch ( NumberFormatException nfe ) {
                        logger.warning( "Ignoring invalid http status code for fault '"+httpStatusText+"'." );
                    }
                }
                output = ExpandVariables.process(faultLevelInfo.getFaultTemplate(), variables, auditor);
                if ( faultLevelInfo.getFaultTemplateContentType() != null ) {
                    String contentTypeText = ExpandVariables.process( faultLevelInfo.getFaultTemplateContentType(), variables, auditor );
                    try {
                        contentTypeHeader = ContentTypeHeader.parseValue( contentTypeText );
                    } catch ( IOException e ) {
                        contentTypeHeader = ContentTypeHeader.TEXT_DEFAULT;
                        logger.warning( "Ignoring invalid content type for fault '"+contentTypeText+"', "+ExceptionUtils.getMessage( e )+"." );
                    }
                } else {
                    if (output.contains(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE))
                        contentTypeHeader = ContentTypeHeader.SOAP_1_2_DEFAULT;
                }
                break;
            case SoapFaultLevel.GENERIC_FAULT:
                try {
                    Pair<ContentTypeHeader,Document> faultInfo =
                            buildGenericFault( pec, globalStatus, isClientFault, faultString, statusTextOverride, false );
                    output = XmlUtil.nodeToFormattedString(faultInfo.right);
                    contentTypeHeader = faultInfo.left;
                } catch (Exception e) {
                    // should not happen
                    logger.log(Level.WARNING, "could not construct generic fault", e);
                }
                break;
            case SoapFaultLevel.MEDIUM_DETAIL_FAULT:
                {
                    Pair<ContentTypeHeader, String> faultInfo =
                            buildDetailedFault(pec, globalStatus, isClientFault, faultString, statusTextOverride, false);
                    output = faultInfo.right;
                    contentTypeHeader = faultInfo.left;
                }
                break;
            case SoapFaultLevel.FULL_TRACE_FAULT:
                {
                    Pair<ContentTypeHeader, String> faultInfo =
                            buildDetailedFault(pec, globalStatus, isClientFault, faultString, statusTextOverride, true);
                    output = faultInfo.right;
                    contentTypeHeader = faultInfo.left;
                }
                break;
        }

        if ( output == null ) {
            output = "Server Error";
        } else if ( faultLevelInfo.isSignSoapFault() ) {
            output = signFault( output, pec.getAuthenticationContext( pec.getRequest() ), pec.getRequest().getSecurityKnob(), faultLevelInfo );
        }

        if ( contentTypeHeader == null ) {
            contentTypeHeader = ContentTypeHeader.TEXT_DEFAULT;
        }

        return new FaultResponse(httpStatus, contentTypeHeader, output);
    }

    private synchronized SoapFaultLevel constructFaultLevelFromServerConfig() {
        // parse default settings from system settings
        fromSettings = new SoapFaultLevel();
        String tmp = config.getProperty("defaultfaultlevel", null);
        // default setting not available through config ?
        if (tmp == null) {
            logger.warning("Cannot retrieve defaultfaultlevel server properties falling back on hardcoded defaults");
            populateUltimateDefaults(fromSettings);
        } else {
            try {
                fromSettings.setSignSoapFault( config.getBooleanProperty( "defaultfaultsign", false ) );
                String keyAlias = config.getProperty( "defaultfaultkeyalias", null );
                if ( fromSettings.isSignSoapFault() && keyAlias!=null && !keyAlias.trim().isEmpty() ) {
                    keyAlias = keyAlias.trim();
                    fromSettings.setUsesDefaultKeyStore( false );
                    fromSettings.setNonDefaultKeystoreId( -1 );
                    fromSettings.setKeyAlias( keyAlias );
                    int index = keyAlias.indexOf( ':' );
                    if ( index > 0 && index < keyAlias.length()-1 ) {
                        try {
                            fromSettings.setNonDefaultKeystoreId( Long.parseLong( keyAlias.substring( 0, index )));
                            fromSettings.setKeyAlias( keyAlias.substring( index+1 ));
                        } catch ( NumberFormatException nfe ) {
                            logger.fine( "Error processing key alias for SOAP fault signing." );
                        }
                    }
                }
                fromSettings.setLevel(Integer.parseInt(tmp));
                fromSettings.setIncludePolicyDownloadURL(config.getBooleanProperty("defaultfaultpolicyurl", true));
                fromSettings.setFaultTemplate(config.getProperty("defaultfaulttemplate", null));
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "user setting " + tmp + " for defaultfaultlevel is invalid", e);
                populateUltimateDefaults(fromSettings);
            }
        }
        lastParsedFromSettings = System.currentTimeMillis();
        return fromSettings;
    }

    private void populateUltimateDefaults(SoapFaultLevel input) {
        input.setLevel(SoapFaultLevel.GENERIC_FAULT);
        input.setIncludePolicyDownloadURL(true);
    }

    private String signFault( final String soapMessage,
                              final AuthenticationContext authenticationContext,
                              final SecurityKnob reqSec,
                              final PrivateKeyable privateKeyable ) {
        String signedSoapMessage = soapMessage;

        if ( soapMessage != null && !soapMessage.isEmpty() ) { // don't sign if dropping connection
            try {
                Document soapDoc = XmlUtil.parse( soapMessage );
                signFault( soapDoc, authenticationContext, reqSec, privateKeyable );
                signedSoapMessage = XmlUtil.nodeToString( soapDoc );
            } catch (SAXException e) {
                logger.log( Level.WARNING, "Error signing SOAP Fault '"+ ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );
            } catch (IOException e) {
                logger.log( Level.WARNING, "Error signing SOAP Fault.", e );
            }
        }

        return signedSoapMessage;
    }

    private void signFault( final Document soapDoc,
                            final AuthenticationContext authenticationContext,
                            final SecurityKnob reqSec,
                            final PrivateKeyable privateKeyable ) {
        try {
            final WssDecoratorImpl decorator = new WssDecoratorImpl();
            final DecorationRequirements requirements = new DecorationRequirements();
            final SignerInfo signerInfo = ServerAssertionUtils.getSignerInfo(context, privateKeyable);
            final boolean isPreferred = ServerAssertionUtils.isPreferredSigner( privateKeyable );
            SigningSecurityToken signingToken = null;

            if ( !isPreferred ) {
                SigningSecurityToken[] tokens = WSSecurityProcessorUtils.getSigningSecurityTokens( authenticationContext.getCredentials() );
                for ( SigningSecurityToken token : tokens ) {
                    if ( token instanceof KerberosSigningSecurityToken ||
                         token instanceof SecurityContextToken ||
                         token instanceof EncryptedKey ) {
                        if ( signingToken != null ) {
                            logger.warning( "Found multiple tokens in request, using default key for signing SOAP fault." );
                            signingToken = null;
                            break;
                        }
                        signingToken = token;
                    }
                }
            }

            // Configure signing token
            if ( signingToken == null ) {
                requirements.setSenderMessageSigningCertificate( signerInfo.getCertificate() );
                requirements.setSenderMessageSigningPrivateKey( signerInfo.getPrivate() );
            } else {
                WSSecurityProcessorUtils.setToken( requirements, signingToken );
            }

            // If the request was processed on the noactor sec header instead of the l7 sec actor, then
            // the response's decoration requirements should map this (if applicable)
            if ( reqSec.getProcessorResult() != null &&
                 reqSec.getProcessorResult().getProcessedActor() != null ) {
                String actorUri = reqSec.getProcessorResult().getProcessedActor()== SecurityActor.NOACTOR ?
                        null :
                        reqSec.getProcessorResult().getProcessedActorUri();

                requirements.setSecurityHeaderActor( actorUri );
            }

            requirements.setSignTimestamp();
            requirements.getElementsToSign().add( SoapUtil.getBodyElement(soapDoc));

            decorator.decorateMessage(new Message(soapDoc), requirements);
        } catch (InvalidDocumentFormatException e) {
            logger.log( Level.WARNING, "Error signing SOAP Fault.", e );
        } catch (SAXException e) {
            logger.log( Level.WARNING, "Error signing SOAP Fault.", e );
        } catch (IOException e) {
            logger.log( Level.WARNING, "Error signing SOAP Fault.", e );
        } catch (DecoratorException e) {
            logger.log( Level.WARNING, "Error signing SOAP Fault.", e );
        } catch (KeyStoreException kse) {
            ObjectNotFoundException onfe = ExceptionUtils.getCauseIfCausedBy( kse, ObjectNotFoundException.class );
            if ( onfe != null ) {
                logger.log( Level.WARNING, "Error signing SOAP Fault '"+ExceptionUtils.getMessage(onfe)+"'.", ExceptionUtils.getDebugException(kse));
            } else {
                logger.log( Level.WARNING, "Error signing SOAP Fault.", kse );
            }
        } catch (GeneralSecurityException e) {
            logger.log( Level.WARNING, "Error signing SOAP Fault.", e );
        }
    }

    private boolean isSoap12( final PolicyEnforcementContext pec ) {
        // If we can see that the request has a SOAP version, use the version from the request
        try {
            final Message request = pec.getRequest();
            if (request.isSoap()) {
                final XmlKnob xmlKnob = request.getXmlKnob();
                final ElementCursor cursor = xmlKnob.getElementCursor();
                cursor.moveToDocumentElement();
                String docNs = cursor.getNamespaceUri();
                return SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(docNs);
            }
            // Fall through and guess based on service
        } catch (IOException e) {
            // Fall through and guess based on service
        } catch (SAXException e) {
            // Fall through and guess based on service
        }

        return pec.getService() != null && SoapVersion.SOAP_1_2.equals(pec.getService().getSoapVersion());
    }

    /**
     * Build a generic fault from a template.
     *
     * @param pec The related policy enforcement context
     * @param globalStatus The policy status
     * @param isClientFault True for client faults (for the faultcode)
     * @param faultString The faultstring to use in the fault
     * @param statusTextOverride The policyResult/@status value to use (from globalStatus if null)
     * @return The fault document
     * @throws SAXException If the template cannot be processed.
     */
    private Pair<ContentTypeHeader,Document> buildGenericFault( final PolicyEnforcementContext pec,
                                                                final AssertionStatus globalStatus,
                                                                final boolean isClientFault,
                                                                final String faultString,
                                                                final String statusTextOverride,
                                                                final boolean declarePolicyNS ) throws SAXException {
        final boolean useSoap12 = isSoap12(pec);
        final Document faultDocument = XmlUtil.stringToDocument(useSoap12 ? FAULT_TEMPLATE_SOAP_1_2 : FAULT_TEMPLATE_SOAP_1_1 );

        // populate @status element
        NodeList res = faultDocument.getElementsByTagNameNS(FAULT_NS, "policyResult");
        Element policyResultEl = (Element)res.item(0);
        if ( "".equals(statusTextOverride) ) {
            // remove details
            policyResultEl.getParentNode().getParentNode().removeChild( policyResultEl.getParentNode() );
        } else {
            policyResultEl.setAttribute("status", statusTextOverride!=null ? statusTextOverride : globalStatus.getMessage());
            if ( declarePolicyNS ) {
                policyResultEl.setAttributeNS( DomUtils.XMLNS_NS, "xmlns:l7p", WspConstants.L7_POLICY_NS);
            }
        }

        // populate the faultcode
        if ( isClientFault ) {
            final String faultCode;
            if(useSoap12) {
                faultCode = "soapenv:Sender";
                res = faultDocument.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");
            } else {
                faultCode = "soapenv:Client";
                res = faultDocument.getElementsByTagName("faultcode");
            }
            Element faultcode = (Element)res.item(0);
            faultcode.setTextContent(faultCode);
        }

        // populate the faultstring
        if(useSoap12) {
            res = faultDocument.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Text");
        } else {
            res = faultDocument.getElementsByTagName("faultstring");
        }
        Element faultstring = (Element)res.item(0);
        faultstring.setTextContent(faultString);

        // populate the faultactor value
        String actor = getRequestUrlVariable(pec);
        if(useSoap12) {
            res = faultDocument.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Role");
        } else {
            res = faultDocument.getElementsByTagName("faultactor");
        }
        Element faultactor = (Element)res.item(0);
        faultactor.setTextContent(actor);

        return new Pair<ContentTypeHeader,Document>( useSoap12 ? ContentTypeHeader.SOAP_1_2_DEFAULT : ContentTypeHeader.XML_DEFAULT, faultDocument);
    }

    /**
     * returns soap faults in the form of:
     * <?xml version="1.0" encoding="UTF-8"?>
     *   <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     *      <soapenv:Body>
     *          <soapenv:Fault>
     *              <faultcode>Server</faultcode>
     *              <faultstring>Policy Falsified</faultstring>
     *              <faultactor>http://soong:8080/xml/blub</faultactor>
     *              <l7:policyResult status="Falsified" xmlns:l7="http://www.layer7tech.com/ws/policy/fault" xmlns:l7p="http://www.layer7tech.com/ws/policy">
     *                  <l7:assertionResult status="BAD" assertion="l7p:WssUsernameToken">
     *                      <l7:detailMessage id="4302">This request did not contain any WSS level security.</l7:detailMessage>
     *                      <l7:detailMessage id="5204">Request did not include an encrypted UsernameToken.</l7:detailMessage>
     *                  </l7:assertionResult>
     *              </l7:policyResult>
     *          </soapenv:Fault>
     *      </soapenv:Body>
     *  </soapenv:Envelope>
     */
    private Pair<ContentTypeHeader, String> buildDetailedFault( final PolicyEnforcementContext pec,
                                                                final AssertionStatus globalStatus,
                                                                final boolean isClientFault,
                                                                final String faultString,
                                                                final String statusTextOverride,
                                                                final boolean includeSuccesses) {
        String output = null;
        final boolean useSoap12 = isSoap12(pec);
        try {
            final Document faultDocument =
                    buildGenericFault( pec, globalStatus, isClientFault, faultString, statusTextOverride, true ).right;

            List<PolicyEnforcementContext.AssertionResult> results = pec.getAssertionResults();
            for (PolicyEnforcementContext.AssertionResult result : results) {
                if (result.getStatus() == AssertionStatus.NONE && !includeSuccesses) {
                    continue;
                }
                Element assertionResultEl = faultDocument.createElementNS(FAULT_NS, "l7:assertionResult");
                assertionResultEl.setAttribute("status", result.getStatus().getMessage());
                String assertionAttribute = "l7p:" + TypeMappingUtils.findTypeMappingByClass(result.getAssertion().getClass(), null).getExternalName();
                assertionResultEl.setAttribute("assertion", assertionAttribute);
                List<AuditDetail> details = auditContext.getDetails().get( result.getDetailsKey() );
                if (details != null) {
                    for (AuditDetail detail : details) {
                        int messageId = detail.getMessageId();
                        // only show details FINE and higher for medium details, show all details for full details
                        if (includeSuccesses || (MessagesUtil.getAuditDetailMessageById(messageId).getLevel().intValue() >= Level.INFO.intValue())) {
                            Element detailMsgEl = faultDocument.createElementNS(FAULT_NS, "l7:detailMessage");
                            detailMsgEl.setAttribute("id", Long.toString(detail.getMessageId()));
                            // add text node with actual message.
                            StringBuffer messageBuffer = new StringBuffer();
                            MessageFormat mf = new MessageFormat(getMessageById(messageId));
                            mf.format(detail.getParams(), messageBuffer, new FieldPosition(0));
                            detailMsgEl.setTextContent(messageBuffer.toString());
                            assertionResultEl.appendChild(faultDocument.importNode(detailMsgEl, true));
                        }
                    }
                }

                NodeList res = faultDocument.getElementsByTagNameNS(FAULT_NS, "policyResult");
                Element policyResultEl = (Element)res.item(0);
                policyResultEl.appendChild( assertionResultEl );
            }
            output = XmlUtil.nodeToFormattedString(faultDocument);
        } catch (Exception e) {
            logger.log(Level.WARNING, "could not construct detailed fault: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
            try {
                Pair<ContentTypeHeader,Document> faultInfo = buildGenericFault( pec, globalStatus, isClientFault, faultString, statusTextOverride, false );
                return new Pair<ContentTypeHeader,String>( faultInfo.left, XmlUtil.nodeToFormattedString(faultInfo.right) );
            } catch ( SAXException e1 ) {
                logger.log(Level.WARNING, "could not construct generic fault", e1);
            } catch ( IOException e1 ) {
                logger.log(Level.WARNING, "could not construct generic fault", e1);
            }
        }

        return new Pair<ContentTypeHeader,String>( useSoap12 ? ContentTypeHeader.SOAP_1_2_DEFAULT : ContentTypeHeader.XML_DEFAULT, output);
    }

    /**
     * gets the assertion detail message giving priority to overridden defaults in the cluster property table.
     * caches the cluster overrides so it does not have to look them up all the time.
     */
    private String getMessageById(int messageId) {
        ReentrantReadWriteLock.ReadLock lock = cacheLock.readLock();
        lock.lock();
        try {
            String cachedMessage = cachedOverrideAuditMessages.get(messageId);
            if (cachedMessage != null) {
                return cachedMessage;
            }
        } finally {
            lock.unlock();
        }
        AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(messageId);
        return message==null ? null : message.getMessage();
    }

    private void updateOverrides() {
        if (clusterPropertiesManager == null) {
            clusterPropertiesManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
            if (clusterPropertiesManager == null) {
                logger.info("cant get handle on ClusterPropertiesManager");
                return;
            }
        }
        try {
            Collection fromTable = clusterPropertiesManager.findAll();
            ReentrantReadWriteLock.WriteLock lock = cacheLock.writeLock();
            lock.lock();
            try {
                cachedOverrideAuditMessages.clear();
                for (Object aFromTable : fromTable) {
                    ClusterProperty clusterProperty = (ClusterProperty) aFromTable;
                    if (clusterProperty.getName() != null && clusterProperty.getName().startsWith(Messages.OVERRIDE_PREFIX)) {
                        try {
                            Integer key = new Integer(clusterProperty.getName().substring(Messages.OVERRIDE_PREFIX.length()));
                            if (clusterProperty.getValue() != null) {
                                cachedOverrideAuditMessages.put(key, clusterProperty.getValue());
                            }
                        } catch (NumberFormatException e) {
                            logger.fine("thought this was an override, but it's not (" + clusterProperty.getName() + ")");
                        }
                    }
                }
            } finally {
                lock.unlock();
            }

        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get cluster properties", e);
        }
    }

    /**
     * Gets the <code>request.url</code> context variable value.  Defaults to "ssg" if the variable is not set
     *
     * @param pec policy context
     * @return String value from the request.url variable, defaults to "ssg" if the variable is not set
     */
    private String getRequestUrlVariable(PolicyEnforcementContext pec) {

        String reqUrl;
        try {
            reqUrl = pec.getVariable("request.url").toString();
            // todo, catch cases when this throws and just fix it
        } catch (NoSuchVariableException e) {
            if (pec.getRequest().isHttpRequest())
                logger.log(Level.WARNING, "this variable is not found but should always be set: {0}", ExceptionUtils.getMessage(e));
            reqUrl = "ssg";
        }
        return reqUrl;
    }
}
