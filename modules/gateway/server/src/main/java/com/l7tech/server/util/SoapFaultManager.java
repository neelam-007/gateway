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
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
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
    private final Config config;
    private final Logger logger = Logger.getLogger(SoapFaultManager.class.getName());
    private long lastParsedFromSettings;
    private SoapFaultLevel fromSettings;
    private Auditor auditor;
    private ClusterPropertyManager clusterPropertiesManager;
    private BeanFactory context;
    private final HashMap<Integer, String> cachedOverrideAuditMessages = new HashMap<Integer, String>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    public static final String FAULT_NS = "http://www.layer7tech.com/ws/policy/fault";

    private final Timer checker;

    public SoapFaultManager( Config config, Timer timer) {
        if (timer == null) timer = new Timer("Soap fault manager refresh", true);
        this.config = config;
        this.checker = timer;
    }

    /**
     * For tests only 
     */
    SoapFaultManager( Config config ) {
        this.config = config;
        this.checker = null;
    }

    /**
     * Read settings from server configuration and assemble a SoapFaultLevel based on the default values.
     */
    public SoapFaultLevel getDefaultBehaviorSettings() {
        // cache at least one minute. todo, review
        if (fromSettings == null || (System.currentTimeMillis() - lastParsedFromSettings) > 60000) {
            return constructFaultLevelFromServerConfig();
        }
        return fromSettings;
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

    /**
     * constructs a soap fault based on the pec and the level desired.
     * @return returns a Pair of content type, string.  The string may be empty if faultLevel is SoapFaultLevel.DROP_CONNECTION.
     */
    public Pair<ContentTypeHeader, String> constructReturningFault( final SoapFaultLevel faultLevelInfo,
                                                                    final PolicyEnforcementContext pec ) {
        String output = "";
        AssertionStatus globalstatus = pec.getPolicyResult();
        ContentTypeHeader ctype = ContentTypeHeader.XML_DEFAULT;
        if (globalstatus == null) {
            // if this happens, it means a bug needs fixing where a path fails to set a value on the policy result
            logger.severe("PolicyEnforcementContext.policyResult not set. Fallback on SERVER_ERROR");
            globalstatus = AssertionStatus.SERVER_ERROR;
        }
        switch (faultLevelInfo.getLevel()) {
            case SoapFaultLevel.DROP_CONNECTION:
                break;
            case SoapFaultLevel.TEMPLATE_FAULT:
                output = ExpandVariables.process(faultLevelInfo.getFaultTemplate(), pec.getVariableMap(faultLevelInfo.getVariablesUsed(), auditor), auditor);
                if (output.contains(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE))
                    ctype = ContentTypeHeader.SOAP_1_2_DEFAULT;
                break;
            case SoapFaultLevel.GENERIC_FAULT:
                try {
                    boolean useSoap12 = pec.getService() != null && pec.getService().getSoapVersion() == SoapVersion.SOAP_1_2;
                    Document tmp = XmlUtil.stringToDocument(useSoap12 ? GENERIC_FAULT_SOAP_1_2 : GENERIC_FAULT);
                    NodeList res = tmp.getElementsByTagNameNS(FAULT_NS, "policyResult");
                    // populate @status element
                    Element policyResultEl = (Element)res.item(0);
                    policyResultEl.setAttribute("status", globalstatus.getMessage());
                    // populate the faultactor value
                    String actor = getRequestUrlVariable(pec);
                    if(useSoap12) {
                        res = tmp.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Role");
                    } else {
                        res = tmp.getElementsByTagName("faultactor");
                    }
                    Element faultactor = (Element)res.item(0);
                    faultactor.setTextContent(actor);
                    output = XmlUtil.nodeToFormattedString(tmp);
                } catch (Exception e) {
                    // should not happen
                    logger.log(Level.WARNING, "could not construct generic fault", e);
                }
                break;
            case SoapFaultLevel.MEDIUM_DETAIL_FAULT:
                output = buildDetailedFault(pec, globalstatus, false);
                break;
            case SoapFaultLevel.FULL_TRACE_FAULT:
                output = buildDetailedFault(pec, globalstatus, true);
                break;
        }

        if ( faultLevelInfo.isSignSoapFault() ) {
            output = signFault( output, pec.getAuthenticationContext( pec.getRequest() ), pec.getRequest().getSecurityKnob(), faultLevelInfo );
        }

        return new Pair<ContentTypeHeader, String>(ctype, output);
    }

    private String signFault( final String soapMessage,
                              final AuthenticationContext authContext,
                              final SecurityKnob reqSec,
                              final PrivateKeyable privateKeyable ) {
        String signedSoapMessage = soapMessage;

        try {
            Document soapDoc = XmlUtil.parse( soapMessage );
            signFault( soapDoc, authContext, reqSec, privateKeyable );
            signedSoapMessage = XmlUtil.nodeToString( soapDoc );
        } catch (SAXException e) {
            logger.log( Level.WARNING, "Error signing SOAP Fault '"+ ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );
        } catch (IOException e) {
            logger.log( Level.WARNING, "Error signing SOAP Fault.", e );
        }

        return signedSoapMessage;
    }

    private void signFault( final Document soapDoc,
                            final AuthenticationContext authContext,
                            final SecurityKnob reqSec,
                            final PrivateKeyable privateKeyable ) {
        try {
            final WssDecoratorImpl decorator = new WssDecoratorImpl();
            final DecorationRequirements reqmts = new DecorationRequirements();
            final SignerInfo signerInfo = ServerAssertionUtils.getSignerInfo(context, privateKeyable);
            final boolean isPreferred = ServerAssertionUtils.isPreferredSigner( privateKeyable );
            SigningSecurityToken signingToken = null;

            if ( !isPreferred ) {
                SigningSecurityToken[] tokens = WSSecurityProcessorUtils.getSigningSecurityTokens( authContext.getCredentials() );
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
                reqmts.setSenderMessageSigningCertificate( signerInfo.getCertificate() );
                reqmts.setSenderMessageSigningPrivateKey( signerInfo.getPrivate() );
            } else {
                WSSecurityProcessorUtils.setToken( reqmts, signingToken );   
            }

            // If the request was processed on the noactor sec header instead of the l7 sec actor, then
            // the response's decoration requirements should map this (if applicable)
            if ( reqSec.getProcessorResult() != null &&
                 reqSec.getProcessorResult().getProcessedActor() != null ) {
                String actorUri = reqSec.getProcessorResult().getProcessedActor()== SecurityActor.NOACTOR ?
                        null :
                        reqSec.getProcessorResult().getProcessedActorUri();

                reqmts.setSecurityHeaderActor( actorUri );
            }

            reqmts.setSignTimestamp();
            reqmts.getElementsToSign().add( SoapUtil.getBodyElement(soapDoc));

            decorator.decorateMessage(new Message(soapDoc), reqmts);
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

    private boolean isSoap12(PolicyEnforcementContext pec) {
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
            // Fallthrough and guess based on service
        } catch (IOException e) {
            // Fallthrough and guess based on service
        } catch (SAXException e) {
            // Fallthrough and guess based on service
        }

        return pec.getService() != null && SoapVersion.SOAP_1_2.equals(pec.getService().getSoapVersion());
    }


    /**
     * SOAP faults resulting from an exception that occurs in the processing of the policy.
     * Receiving such a fault sould be considered a bug.
     */
    public String constructExceptionFault(Throwable e, PolicyEnforcementContext pec) {
        String output = null;
        try {
            boolean policyVersionFault = pec.isRequestClaimingWrongPolicyVersion();
            if(isSoap12(pec)) {
                Document tmp = XmlUtil.stringToDocument(policyVersionFault ?
                        POLICY_VERSION_EXCEPTION_FAULT_SOAP_1_2 : EXCEPTION_FAULT_SOAP_1_2);

                if (!policyVersionFault) {
                    NodeList res = tmp.getElementsByTagNameNS(FAULT_NS, "policyResult");
                    Element policyResultEl = (Element) res.item(0);

                    // populate @status element
                    policyResultEl.setAttribute("status", e.getMessage());
                }
                // populate the faultactor value
                String role = getRequestUrlVariable(pec);

                NodeList res = tmp.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Role");
                Element faultrole = (Element) res.item(0);
                faultrole.setTextContent(role);
                output = XmlUtil.nodeToFormattedString(tmp);
            } else {
                Document tmp = XmlUtil.stringToDocument(policyVersionFault ?
                        POLICY_VERSION_EXCEPTION_FAULT : EXCEPTION_FAULT);

                if (!policyVersionFault) {
                    NodeList res = tmp.getElementsByTagNameNS(FAULT_NS, "policyResult");
                    Element policyResultEl = (Element) res.item(0);

                    // populate @status element
                    policyResultEl.setAttribute("status", e.getMessage());
                }
                // populate the faultactor value
                String actor = getRequestUrlVariable(pec);

                NodeList res = tmp.getElementsByTagName("faultactor");
                Element faultactor = (Element) res.item(0);
                faultactor.setTextContent(actor);
                output = XmlUtil.nodeToFormattedString(tmp);
            }
        } catch (Exception el) {
            // should not happen
            logger.log(Level.WARNING, "Unexpected exception", el);
        }

        SoapFaultLevel faultLevelInfo = pec.getFaultlevel();
        if ( faultLevelInfo.isSignSoapFault() ) {
            output = signFault( output, pec.getAuthenticationContext( pec.getRequest() ), pec.getRequest().getSecurityKnob(), faultLevelInfo );
        }

        return output;
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
    private String buildDetailedFault(PolicyEnforcementContext pec, AssertionStatus globalstatus, boolean includeSuccesses) {
        String output = null;
        try {
            boolean useSoap12 = isSoap12(pec);
            Document tmp = XmlUtil.stringToDocument(useSoap12 ? GENERIC_FAULT_SOAP_1_2 : GENERIC_FAULT);
            NodeList res = tmp.getElementsByTagNameNS(FAULT_NS, "policyResult");
            // populate @status element
            Element policyResultEl = (Element)res.item(0);
            policyResultEl.setAttribute("status", globalstatus.getMessage());
            policyResultEl.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:l7p", WspConstants.L7_POLICY_NS);

            // populate the faultactor value
            String actor = getRequestUrlVariable(pec);

            res = useSoap12 ? tmp.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Role") : tmp.getElementsByTagName("faultactor");
            Element faultactor = (Element)res.item(0);
            faultactor.setTextContent(actor);

            List<PolicyEnforcementContext.AssertionResult> results = pec.getAssertionResults(pec.getAuditContext());
            for (PolicyEnforcementContext.AssertionResult result : results) {
                if (result.getStatus() == AssertionStatus.NONE && !includeSuccesses) {
                    continue;
                }
                Element assertionResultEl = tmp.createElementNS(FAULT_NS, "l7:assertionResult");
                assertionResultEl.setAttribute("status", result.getStatus().getMessage());
                String assertionattr = "l7p:" + TypeMappingUtils.findTypeMappingByClass(result.getAssertion().getClass(), null).getExternalName();
                assertionResultEl.setAttribute("assertion", assertionattr);
                List<AuditDetail> details = result.getDetails();
                if (details != null) {
                    for (AuditDetail detail : details) {
                        int msgid = detail.getMessageId();
                        // only show details FINE and higher for medium details, show all details for full details
                        if (includeSuccesses || (MessagesUtil.getAuditDetailMessageById(msgid).getLevel().intValue() >= Level.INFO.intValue())) {
                            Element detailMsgEl = tmp.createElementNS(FAULT_NS, "l7:detailMessage");
                            detailMsgEl.setAttribute("id", Long.toString(detail.getMessageId()));
                            // add text node with actual message. see below for logpanel sample:
                            StringBuffer msgbuf = new StringBuffer();
                            MessageFormat mf = new MessageFormat(getMessageById(msgid));
                            mf.format(detail.getParams(), msgbuf, new FieldPosition(0));
                            detailMsgEl.setTextContent(msgbuf.toString());
                            assertionResultEl.appendChild(tmp.importNode(detailMsgEl, true));
                        }
                    }
                }
                policyResultEl.appendChild(tmp.importNode(assertionResultEl, true));
            }
            output = XmlUtil.nodeToFormattedString(tmp);
        } catch (Exception e) {
            logger.log(Level.WARNING, "could not construct generic fault", e);
        }
        return output;
    }

    /**
     * gets the assertion detail message giving priority to overriden defaults in the cluster property table.
     * caches the cluster overrides so it does not have to look them up all the time.
     */
    private String getMessageById(int msgid) {
        ReentrantReadWriteLock.ReadLock lock = cacheLock.readLock();
        lock.lock();
        try {
            String cachedMessage = cachedOverrideAuditMessages.get(msgid);
            if (cachedMessage != null) {
                return cachedMessage;
            }
        } finally {
            lock.unlock();
        }
        AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(msgid);
        return message==null ? null : message.getMessage();
    }

    private static final String GENERIC_FAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>soapenv:Server</faultcode>\n" +
                        "            <faultstring>Policy Falsified</faultstring>\n" +
                        "            <faultactor/>\n" +
                        "            <detail>\n" +
                        "                 <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                        "            </detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

    private static final String GENERIC_FAULT_SOAP_1_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\">\n" +
                         "    <soapenv:Body>\n" +
                         "        <soapenv:Fault>\n" +
                         "            <soapenv:Code>\n" +
                         "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
                         "            </soapenv:Code>\n" +
                         "            <soapenv:Reason>\n" +
                         "                <soapenv:Text xml:lang=\"en-US\">Policy Falsified</soapenv:Text>\n" +
                         "            </soapenv:Reason>\n" +
                         "            <soapenv:Role/>\n" +
                         "            <soapenv:Detail>\n" +
                         "                <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                         "            </soapenv:Detail>\n" +
                         "        </soapenv:Fault>\n" +
                         "    </soapenv:Body>\n" +
                         "</soapenv:Envelope>";

    private static final String EXCEPTION_FAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>soapenv:Server</faultcode>\n" +
                        "            <faultstring>Error in assertion processing</faultstring>\n" +
                        "            <faultactor/>\n" +
                        "            <detail>\n" +
                        "                 <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                        "            </detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

    private static final String EXCEPTION_FAULT_SOAP_1_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <soapenv:Code>\n" +
                        "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
                        "            </soapenv:Code>\n" +
                        "            <soapenv:Reason>\n" +
                        "                <soapenv:Text xml:lang=\"en-US\">Error in assertion processing</soapenv:Text>\n" +
                        "            </soapenv:Reason>\n" +
                        "            <soapenv:Role/>\n" +
                        "            <soapenv:Detail>\n" +
                        "                <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                        "            </soapenv:Detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
    
    private static final String POLICY_VERSION_EXCEPTION_FAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>soapenv:Client</faultcode>\n" +
                        "            <faultstring>Incorrect policy version</faultstring>\n" +
                        "            <faultactor/>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

    private static final String POLICY_VERSION_EXCEPTION_FAULT_SOAP_1_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <soapenv:Code>\n" +
                        "                <soapenv:Value>soapenv:Sender</soapenv:Value>\n" +
                        "            </soapenv:Code>\n" +
                        "            <soapenv:Reason>\n" +
                        "                <soapenv:Text xml:lang=\"en-US\">Incorrect policy version</soapenv:Text>\n" +
                        "            </soapenv:Reason>\n" +
                        "            <soapenv:Role/>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) {
        setBeanFactory( applicationContext );
        clusterPropertiesManager = (ClusterPropertyManager)applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        final SoapFaultManager tasker = this;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    tasker.updateOverrides();
                } catch(Exception e) {
                    logger.log(Level.WARNING, "Error updating message overrides.", e);
                }
            }
        };
        checker.schedule(task, 10000, 56893);
    }

    void setBeanFactory( BeanFactory context ) throws BeansException {
        auditor = context instanceof ApplicationContext ?
                new Auditor(this, (ApplicationContext)context, logger) :
                new LogOnlyAuditor(logger);
        this.context = context;
    }

    private void updateOverrides() {
        if (clusterPropertiesManager == null) {
            clusterPropertiesManager = (ClusterPropertyManager)context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
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
        } catch (NoSuchVariableException notfound) {
            if (pec.getRequest().isHttpRequest())
                logger.log(Level.WARNING, "this variable is not found but should always be set: {0}", ExceptionUtils.getMessage(notfound));
            reqUrl = "ssg";
        }
        return reqUrl;
    }
}
