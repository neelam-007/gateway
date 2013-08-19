package com.l7tech.server.util;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.wsp.TypeMappingUtils;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.mime.ContentTypeHeader.TEXT_DEFAULT;
import static com.l7tech.util.Option.optional;

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
                             Timer timer) {
        if (timer == null) timer = new Timer("Soap fault manager refresh", true);
        this.config = config;
        this.checker = timer;
    }

    /**
     * Read settings from server configuration and assemble a SoapFaultLevel based on the default values.
     */
    public SoapFaultLevel getDefaultBehaviorSettings() {
        // cache at least one minute
        if (fromSettings == null || (System.currentTimeMillis() - lastParsedFromSettings) > 60000L) {
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
        final boolean faultType = inFaultLevelInfo == null ? false: inFaultLevelInfo.isUseClientFault();
        return constructFault( inFaultLevelInfo, pec, faultType, "Policy Falsified", null );
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

    /**
     * Construct a general purpose fault.
     *
     * @param useSoap12 True to use SOAP 1.2, false for SOAP 1.1
     * @param actor The faultactor
     * @param isClientFault True if this is a client fault
     * @param faultstring The fault text to use
     * @return The fault response
     */
    public FaultResponse constructFault( final boolean useSoap12,
                                         final String actor,
                                         final boolean isClientFault,
                                         final String faultstring ) {
        final Pair<ContentTypeHeader,Document> faultInfo =
                buildGenericFault( useSoap12,
                                   actor,
                                   isClientFault,
                                   faultstring,
                                   "",
                                   false );

        String output;
        try {
            output = XmlUtil.nodeToFormattedString(faultInfo.right);
        } catch ( IOException e ) {
            logger.log(Level.WARNING, "Could not construct fault", e);
            output = "";
        }
        ContentTypeHeader contentTypeHeader = optional(faultInfo.left).orSome( TEXT_DEFAULT );

        return new FaultResponse(
                HttpConstants.STATUS_SERVER_ERROR,
                contentTypeHeader,
                output,
                Collections.<Pair<String,String>>emptyList());
    }

    /**
     * Convenience method that will pass any extra headers to the specified HTTP servlet response.
     *
     * @param fault fault response that may include HTTP headers.  Required.
     * @param hresp HTTP servlet response that will be given zero or more additional headers.  Required.
     */
    public void sendExtraHeaders(@NotNull FaultResponse fault, @NotNull HttpServletResponse hresp) {
        List<Pair<String, String>> heads = fault.getExtraHeaders();
        if (heads == null) return;
        for (Pair<String, String> head : heads) {
            hresp.addHeader(head.getKey(), head.getValue());
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
        checker.schedule(task, 10000L, 56893L);
    }

    /**
     * A SOAP Fault or other error response.
     */
    public static class FaultResponse {
        private final int httpStatus;
        private final ContentTypeHeader contentType;
        private final String content;
        private final List<Pair<String,String>> extraHeaders;

        FaultResponse( final int httpStatus,
                       final ContentTypeHeader contentType,
                       final String content,
                       final List<Pair<String,String>> extraHeaders) {
            if (extraHeaders == null) throw new NullPointerException("extraHeaders may not be null");
            this.httpStatus = httpStatus;
            this.contentType = contentType;
            this.content = content;
            this.extraHeaders = extraHeaders;
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

        /**
         * @return extra headers to add.  May be empty but never null.
         */
        public List<Pair<String, String>> getExtraHeaders() {
            return extraHeaders;
        }
    }

    //- PACKAGE

    /**
     * For tests only
     */
    SoapFaultManager( Config config ) {
        this.config = config;
        this.checker = null;
    }

    void setBeanFactory( BeanFactory context ) throws BeansException {
        auditor = context.getBean( "auditFactory", AuditFactory.class ).newInstance( this, logger );
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
    private Audit auditor;
    private ClusterPropertyManager clusterPropertiesManager;
    private BeanFactory context;
    private final HashMap<Integer, String> cachedOverrideAuditMessages = new HashMap<Integer, String>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Timer checker;

    private FaultResponse constructFault( final SoapFaultLevel inFaultLevelInfo,
                                          final PolicyEnforcementContext pec,
                                          final boolean isClientFault,
                                          final String faultString,
                                          @Nullable final String statusTextOverride ) {
        final SoapFaultLevel faultLevelInfo = inFaultLevelInfo==null ? getDefaultBehaviorSettings() : inFaultLevelInfo;
        int httpStatus = HttpConstants.STATUS_SERVER_ERROR;
        String output = "";
        List<Pair<String, String>> extraHeaders = new ArrayList<Pair<String, String>>();
        AssertionStatus globalStatus = pec.getPolicyResult();
        ContentTypeHeader contentTypeHeader = ContentTypeHeader.XML_DEFAULT;
        if (globalStatus == null) {
            logger.fine("PolicyEnforcementContext.policyResult not set. Fallback on UNDEFINED");
            globalStatus = AssertionStatus.UNDEFINED;
        }
        int level = faultLevelInfo.getLevel();
        Map<String,Object> variables;
        switch ( level ) {
            case SoapFaultLevel.DROP_CONNECTION:
                break;
            case SoapFaultLevel.TEMPLATE_FAULT:
                variables = pec.getVariableMap(faultLevelInfo.getVariablesUsed(), auditor);
                if ( faultLevelInfo.getFaultTemplateHttpStatus() != null ) {
                    String httpStatusText = ExpandVariables.process( faultLevelInfo.getFaultTemplateHttpStatus(), variables, auditor );
                    try {
                        httpStatus = Integer.parseInt( httpStatusText );
                    } catch ( NumberFormatException nfe ) {
                        logger.warning( "Ignoring invalid http status code for fault '"+httpStatusText+"'." );
                    }
                }
                output = ExpandVariables.process(faultLevelInfo.getFaultTemplate(), variables, auditor);
                addExtraHeaders(extraHeaders, faultLevelInfo, variables, auditor);
                if ( faultLevelInfo.getFaultTemplateContentType() != null ) {
                    String contentTypeText = ExpandVariables.process( faultLevelInfo.getFaultTemplateContentType(), variables, auditor );
                    try {
                        contentTypeHeader = ContentTypeHeader.parseValue( contentTypeText );
                    } catch ( IOException e ) {
                        contentTypeHeader = TEXT_DEFAULT;
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
                    variables = pec.getVariableMap(faultLevelInfo.getVariablesUsed(), auditor);
                    addExtraHeaders(extraHeaders, faultLevelInfo, variables, auditor);
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
                    variables = pec.getVariableMap(faultLevelInfo.getVariablesUsed(), auditor);
                    addExtraHeaders(extraHeaders, faultLevelInfo, variables, auditor);
                }
                break;
            case SoapFaultLevel.FULL_TRACE_FAULT:
                {
                    Pair<ContentTypeHeader, String> faultInfo =
                            buildDetailedFault(pec, globalStatus, isClientFault, faultString, statusTextOverride, true);
                    output = faultInfo.right;
                    contentTypeHeader = faultInfo.left;
                    variables = pec.getVariableMap(faultLevelInfo.getVariablesUsed(), auditor);
                    addExtraHeaders(extraHeaders, faultLevelInfo, variables, auditor);
                }
                break;
        }

        if ( output == null ) {
            output = "Server Error";
        } else if ( faultLevelInfo.isSignSoapFault() ) {
            output = signFault( output, pec.getAuthenticationContext( pec.getRequest() ), pec.getRequest().getSecurityKnob(), faultLevelInfo );
        }

        if ( contentTypeHeader == null ) {
            contentTypeHeader = TEXT_DEFAULT;
        }

        return new FaultResponse(httpStatus, contentTypeHeader, output, extraHeaders);
    }

    private void addExtraHeaders(List<Pair<String, String>> extraHeaders, SoapFaultLevel faultLevelInfo, Map<String, Object> variables, Audit auditor) {
        NameValuePair[] toadd = faultLevelInfo.getExtraHeaders();
        if (toadd == null) return;
        for (NameValuePair nameValuePair : toadd) {
            String name = ExpandVariables.process(nameValuePair.getKey(), variables, auditor);
            String value = ExpandVariables.process(nameValuePair.getValue(), variables, auditor);
            extraHeaders.add(new Pair<String, String>(name, value));
        }
    }

    private synchronized SoapFaultLevel constructFaultLevelFromServerConfig() {
        // parse default settings from system settings
        fromSettings = new SoapFaultLevel();
        String tmp = config.getProperty( "defaultfaultlevel" );
        // default setting not available through config ?
        if (tmp == null) {
            logger.warning("Cannot retrieve defaultfaultlevel server properties falling back on hardcoded defaults");
            populateUltimateDefaults(fromSettings);
        } else {
            try {
                fromSettings.setSignSoapFault( config.getBooleanProperty( "defaultfaultsign", false ) );
                String keyAlias = config.getProperty( "defaultfaultkeyalias" );
                if ( fromSettings.isSignSoapFault() && keyAlias!=null && !keyAlias.trim().isEmpty() ) {
                    keyAlias = keyAlias.trim();
                    fromSettings.setUsesDefaultKeyStore( false );
                    fromSettings.setNonDefaultKeystoreId(GoidEntity.DEFAULT_GOID);
                    fromSettings.setKeyAlias( keyAlias );
                    int index = keyAlias.indexOf( ':' );
                    if ( index > 0 && index < keyAlias.length()-1 ) {
                        try {
                            fromSettings.setNonDefaultKeystoreId( GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, keyAlias.substring(0, index)));
                            fromSettings.setKeyAlias( keyAlias.substring( index+1 ));
                        } catch ( NumberFormatException nfe ) {
                            logger.fine( "Error processing key alias for SOAP fault signing." );
                        }
                    }
                }
                fromSettings.setLevel(Integer.parseInt(tmp));
                fromSettings.setIncludePolicyDownloadURL(config.getBooleanProperty("defaultfaultpolicyurl", true));
                fromSettings.setFaultTemplate( config.getProperty( "defaultfaulttemplate" ) );
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

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
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

            requirements.setSignTimestamp(true);
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
        } catch (UnrecoverableKeyException e) {
            logger.log( Level.WARNING, "Error signing SOAP Fault: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
        } catch (GeneralSecurityException e) {
            logger.log( Level.WARNING, "Error signing SOAP Fault.", e );
        }
    }

    private boolean isSoap12( final PolicyEnforcementContext pec ) {
        // If we can see that the request has a SOAP version, use the version from the request
        try {
            final Message request = pec.getRequest();
            if (request.isSoap()) {
                return SoapVersion.SOAP_1_2.equals(request.getSoapKnob().getSoapVersion());
            }
            // Fall through and guess based on service
        } catch (IOException e) {
            // Fall through and guess based on service
        } catch (SAXException e) {
            // Fall through and guess based on service
        } catch (MessageNotSoapException e) {
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
                                                                final boolean declarePolicyNS ) {
        final boolean useSoap12 = isSoap12(pec);
        final String actor = getRequestUrlVariable(pec);
        final String statusText = statusTextOverride!=null ? statusTextOverride : globalStatus.getMessage();
        return buildGenericFault( useSoap12, actor, isClientFault, faultString, statusText, declarePolicyNS );
    }

    /**
     * Build a generic fault from a template.
     *
     * @param useSoap12 True to use SOAP 1.2
     * @param actor The faultactor
     * @param isClientFault True for client faults (for the faultcode)
     * @param faultString The faultstring to use in the fault
     * @param statusText The policyResult/@status value to use
     * @return The fault document
     * @throws SAXException If the template cannot be processed.
     */
    private Pair<ContentTypeHeader,Document> buildGenericFault( final boolean useSoap12,
                                                                final String actor,
                                                                final boolean isClientFault,
                                                                final String faultString,
                                                                final String statusText,
                                                                final boolean declarePolicyNS ) {
        final Document faultDocument = XmlUtil.stringAsDocument( useSoap12 ? FAULT_TEMPLATE_SOAP_1_2 : FAULT_TEMPLATE_SOAP_1_1 );

        // populate @status element
        NodeList res = faultDocument.getElementsByTagNameNS(FAULT_NS, "policyResult");
        Element policyResultEl = (Element)res.item(0);
        if ( "".equals(statusText) ) {
            // remove details
            policyResultEl.getParentNode().getParentNode().removeChild( policyResultEl.getParentNode() );
        } else {
            policyResultEl.setAttributeNS( null, "status", statusText );
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
        final AuditContext auditContext = optional(pec.getAuditContext()).orSome(AuditContextFactory.getCurrent());
        String output = null;
        final boolean useSoap12 = isSoap12(pec);
        try {
            final Document faultDocument =
                    buildGenericFault( pec, globalStatus, isClientFault, faultString, statusTextOverride, true ).right;

            // track which audit detail messages have been used to eliminate duplicate entries in "additionalInfo"
            Set<Integer> usedMsgDetails = new TreeSet<Integer>();

            List<PolicyEnforcementContext.AssertionResult> results = pec.getAssertionResults();
            for (PolicyEnforcementContext.AssertionResult result : results) {
                if (result.getStatus() == AssertionStatus.NONE && !includeSuccesses) {
                    continue;
                }
                Element assertionResultEl = faultDocument.createElementNS(FAULT_NS, "l7:assertionResult");
                assertionResultEl.setAttributeNS( null, "status", result.getStatus().getMessage() );
                String assertionAttribute = "l7p:" + TypeMappingUtils.findTypeMappingByClass(result.getAssertion().getClass(), null).getExternalName();
                assertionResultEl.setAttributeNS( null, "assertion", assertionAttribute );
                List<AuditDetail> details = auditContext.getDetails().get( result.getDetailsKey() );
                if (details != null) {
                    for (AuditDetail detail : details) {
                        int messageId = detail.getMessageId();
                        usedMsgDetails.add(messageId);
                        // only show details FINE and higher for medium details, show all details for full details
                        if (includeSuccesses || (MessagesUtil.getAuditDetailMessageById(messageId).getLevel().intValue() >= Level.INFO.intValue())) {
                            Element detailMsgEl = faultDocument.createElementNS(FAULT_NS, "l7:detailMessage");
                            detailMsgEl.setAttributeNS(null, "id", Integer.toString(detail.getMessageId()));
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

            /*
             * [bug 9402] This handles the case where processing errors occur before the request can be passed
             * into the message processor in which case the errors are not displayed in the "Full Detail" SOAP
             * fault. The change below handles when the always use SOAP fault flag is set but no assertion
             * results exists in the audit context.
             */
            if (pec.getFaultlevel() != null && pec.getFaultlevel().isAlwaysReturnSoapFault() && !auditContext.getDetails().isEmpty()) {

                List<AuditDetail> fullDetailsList = new ArrayList<AuditDetail>();
                for (List<AuditDetail> dtls : auditContext.getDetails().values()) {
                    if (!dtls.isEmpty())
                        fullDetailsList.addAll(dtls);
                }

                // sort by ordinal value
                Collections.sort( fullDetailsList, new Comparator<AuditDetail>() {
                    @Override
                    public int compare(AuditDetail d1, AuditDetail d2) {
                        Integer ord1 = d1.getOrdinal();
                        Integer ord2 = d2.getOrdinal();
                        return ord1.compareTo(ord2);
                    }
                });

                Element additionalInfoEl = faultDocument.createElementNS(FAULT_NS, "l7:additionalInfo");
                for (AuditDetail detail : fullDetailsList) {
                    int messageId = detail.getMessageId();
                    // only display the messages that have not already been shown in the assertion status loop
                    if ((MessagesUtil.getAuditDetailMessageById(messageId).getLevel().intValue() >= Level.INFO.intValue()) && !usedMsgDetails.contains(messageId)) {
                        Element detailMsgEl = faultDocument.createElementNS(FAULT_NS, "l7:detailMessage");
                        detailMsgEl.setAttributeNS( null, "id", Integer.toString( detail.getMessageId() ) );
                        detailMsgEl.setAttributeNS( null, "ordinal", Integer.toString( detail.getOrdinal() ) );
                        // add text node with actual message.
                        StringBuffer messageBuffer = new StringBuffer();
                        MessageFormat mf = new MessageFormat(getMessageById(messageId));
                        mf.format(detail.getParams(), messageBuffer, new FieldPosition(0));
                        detailMsgEl.setTextContent(messageBuffer.toString());
                        additionalInfoEl.appendChild(faultDocument.importNode(detailMsgEl, true));
                    }
                }

                NodeList res = faultDocument.getElementsByTagNameNS(FAULT_NS, "policyResult");
                Element policyResultEl = (Element)res.item(0);
                policyResultEl.appendChild( additionalInfoEl );
            }

            output = XmlUtil.nodeToFormattedString(faultDocument);
        } catch (Exception e) {
            logger.log(Level.WARNING, "could not construct detailed fault: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
            try {
                Pair<ContentTypeHeader,Document> faultInfo = buildGenericFault( pec, globalStatus, isClientFault, faultString, statusTextOverride, false );
                return new Pair<ContentTypeHeader,String>( faultInfo.left, XmlUtil.nodeToFormattedString(faultInfo.right) );
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
