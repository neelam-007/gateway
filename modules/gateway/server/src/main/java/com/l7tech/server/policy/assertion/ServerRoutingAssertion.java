/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.TcpKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.token.KerberosSecurityToken;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.security.kerberos.KerberosRoutingClient;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

/**
 * Base class for routing assertions.
 *
 * @author alex
 */
public abstract class ServerRoutingAssertion<RAT extends RoutingAssertion> extends AbstractServerAssertion<RAT> implements ServerAssertion {
    private static final Logger sraLogger = Logger.getLogger(ServerRoutingAssertion.class.getName());

    //- PUBLIC

    public static final String ENCODING = "UTF-8";

    //- PROTECTED

    // instance
    protected final ApplicationContext applicationContext;
    protected final ApplicationEventPublisher messageProcessingEventChannel;

    /**
     *
     * @param data  assertion bean holding config for this server assertion.  required.
     * @param applicationContext Spring application context for creating an auditor, or null to log only
     * @param logger logger to use, or null.
     */
    protected ServerRoutingAssertion(RAT data, ApplicationContext applicationContext, Logger logger) {
        super(data);
        this.applicationContext = applicationContext;
        if (applicationContext == null) {
            this.messageProcessingEventChannel = new EventChannel();
        } else {
            ApplicationEventPublisher mpchannel = (EventChannel) applicationContext.getBean("messageProcessingEventChannel", EventChannel.class);
            this.messageProcessingEventChannel = mpchannel != null ? mpchannel : applicationContext;             
        }
        this.logger = logger != null ? logger : sraLogger;
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    /**
     * Perform security header pocessing ont he specified message according to the current assertion's
     * secHeaderHandlingOption.
     *
     * @param message  the message whose Security headers to manipulate.  Required.
     *                 <p/>
     *                 If this message doesn't contain a SOAP envelope, this method will return without
     *                 taking action if doing so is reasonable given the secHeaderHandlingOption; otherwise,
     *                 it will throw AssertionStatusException.
     * @throws SAXException if the message can't be parsed as XML.
     * @throws IOException if there is a problem reading or writing a message.
     * @throws AssertionStatusException if the operation cannot be performed, and this method recommends that
     *                                  the overall assertion fail with the enclosed assertion status.
     */
    protected void handleProcessedSecurityHeader(Message message) throws SAXException, IOException {
        handleProcessedSecurityHeader(message, assertion.getCurrentSecurityHeaderHandling(), assertion.getXmlSecurityActorToPromote());
    }

    /**
     * Perform security header pocessing ont he specified message according to the specified
     * secHeaderHandlingOption.
     *
     * @param message  the message whose Security headers to manipulate.  Required.
     *                 <p/>
     *                 If this message doesn't contain a SOAP envelope, this method will return without
     *                 taking action if doing so is reasonable given the secHeaderHandlingOption; otherwise,
     *                 it will throw AssertionStatusException.
     * @param secHeaderHandlingOption the type of processing to perform.
     * @param otherToPromote the name of an actor whose security header should be promoted to replace the default
     *                       security header,  if secHeaderHandlingOption is PROMOTE_OTHER_SECURITY_HEADER.
     * @throws SAXException if the message can't be parsed as XML.
     * @throws IOException if there is a problem reading or writing a message.
     * @throws AssertionStatusException if the operation cannot be performed, and this method recommends that
     *                                  the overall assertion fail with the enclosed assertion status.
     */
    protected void handleProcessedSecurityHeader(Message message, int secHeaderHandlingOption, String otherToPromote) throws SAXException, IOException {
        if (secHeaderHandlingOption == RoutingAssertion.IGNORE_SECURITY_HEADER)
            return;

        final XmlKnob requestXml = (XmlKnob)message.getKnob(XmlKnob.class);
        if (requestXml == null) {
            logger.finest("skipping this because the message isn't XML");
            return;
        }

        final SecurityKnob requestSec = (SecurityKnob)message.getKnob(SecurityKnob.class);
        if (requestSec == null || requestSec.getProcessorResult() == null) {
            logger.finest("skipping this because no security header was processed");
            return;
        }

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        if (secHeaderHandlingOption == RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER) {
            deleteDefaultSecurityHeader(message.getXmlKnob(), requestSec.getProcessorResult());
            return;
        }

        if (secHeaderHandlingOption == RoutingAssertion.CLEANUP_CURRENT_SECURITY_HEADER) {
            ProcessorResult pr = requestSec.getProcessorResult();
            if (pr != null)
                cleanupProcessedSecurityHeader(message, pr.getProcessedActor(), pr.getProcessedActorUri());
            return;
        }

        // At this point we must be doing PROMOTE_OTHER
        if (secHeaderHandlingOption != RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unexpected secHeaderHandlingOption: " + secHeaderHandlingOption);
            return;
        }

        // Delete the existing...
        final XmlKnob xmlKnob = message.getXmlKnob();
        if (!deleteDefaultSecurityHeader(message.getXmlKnob(), requestSec.getProcessorResult())) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_POLICY);
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }

        // ...and promote the other, if requested
        if (otherToPromote == null)
            return;

        Document doc = xmlKnob.getDocumentReadOnly();
        // check if that actor is present
        Element secHeaderToPromote;
        try {
            secHeaderToPromote = SoapUtil.getSecurityElement(doc, otherToPromote);
        } catch ( InvalidDocumentFormatException e) {
            // Can't happen
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_POLICY, null, e);
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e);
        }
        if (secHeaderToPromote != null) {
            // do it
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_PROMOTING_ACTOR, otherToPromote);
            xmlKnob.getDocumentWritable();
            SoapUtil.nukeActorAttribute(secHeaderToPromote);
        } else {
            // this is not a big deal but might indicate something wrong
            // with the assertion => logging as info
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_NO_SECURITY_HEADER, otherToPromote);
        }
    }

    // returns false if message was not soap
    private boolean cleanupProcessedSecurityHeader(Message message, SecurityActor processedActor, String processedActorUri ) throws SAXException, IOException {
        Document doc = message.getXmlKnob().getDocumentReadOnly();
        if (!SoapUtil.isSoapMessageMaybe(doc))
            return false;

        try {
            // leaving the processed security header for passthrough means that if the processed
            // actor was l7, we need to promote to default (unless there is a default present)
            if (processedActor == SecurityActor.L7ACTOR) {
                Element defaultSecHeader = SoapUtil.getSecurityElement(doc, processedActorUri);
                if (defaultSecHeader != null) {
                    Element noActorSecHeader = SoapUtil.getSecurityElement(doc);
                    if (noActorSecHeader == null || noActorSecHeader == defaultSecHeader) {
                        Document wdoc = message.getXmlKnob().getDocumentWritable();
                        assert wdoc == doc;
                        SoapUtil.nukeActorAttribute(defaultSecHeader);
                        SoapUtil.removeSoapAttr(defaultSecHeader, SoapConstants.MUSTUNDERSTAND_ATTR_NAME);
                    } else {
                        logger.info("we can't promote l7 sec header as no actor because " +
                                    "there is already a noactor one present. there may be " +
                                    "something wrong with this policy");
                    }
                }
            } else if (processedActor == SecurityActor.NOACTOR) {
                Element defaultSecHeader = SoapUtil.getSecurityElement(doc);
                if (defaultSecHeader != null) {
                    Document wdoc = message.getXmlKnob().getDocumentWritable();
                    assert wdoc == doc;
                    SoapUtil.removeSoapAttr(defaultSecHeader, SoapConstants.MUSTUNDERSTAND_ATTR_NAME);
                }
            }
            return true;
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_FORMAT);
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Unable to clean up Security header: " + ExceptionUtils.getMessage(e), e);
        }
    }

    // returns false if message was not soap
    private boolean deleteDefaultSecurityHeader(XmlKnob xmlKnob, ProcessorResult pr) throws SAXException, IOException {
        Document doc = xmlKnob.getDocumentReadOnly();
        if (!SoapUtil.isSoapMessageMaybe(doc))
            return false;

        try {
            Element defaultSecHeader = findDefaultSecurityHeader(doc, pr);
            if (defaultSecHeader != null) {
                xmlKnob.getDocumentWritable();
                defaultSecHeader.getParentNode().removeChild(defaultSecHeader);
                SoapUtil.removeEmptySoapHeader(doc);
            }
            return true;
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_FORMAT);
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Unable to remove Security header: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private Element findDefaultSecurityHeader(Document doc, ProcessorResult pr) throws InvalidDocumentFormatException {
        if (pr == null || pr.getProcessedActor() != SecurityActor.L7ACTOR)
            return SoapUtil.getSecurityElement(doc);
        else
            return SoapUtil.getSecurityElementForL7(doc);
    }

    /**
     * Attach a sender-vouches SAML assertion to the request.
     *
     * @param message  the Message that should be decorated with a SAML assertion.  Required.
     * @param svInputCredentials  the credentials to assert in the SAML assertion.
     *                             If null, this method will audit a warning and take no further action.
     * @param signerInfo For signing the assertion / message
     * @throws org.xml.sax.SAXException If the request is not XML
     * @throws java.io.IOException If there is an error getting the request document
     * @throws java.security.SignatureException If an error occurs when signing
     * @throws java.security.cert.CertificateException If the signing certificate is invalid.
     */
    protected void doAttachSamlSenderVouches(Message message, LoginCredentials svInputCredentials, SignerInfo signerInfo)
            throws SAXException, IOException, SignatureException, CertificateException
    {
        if (svInputCredentials == null) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SAML_SV_NOT_AUTH);
            return;
        }

        Document document = message.getXmlKnob().getDocumentWritable();
        SamlAssertionGenerator ag = new SamlAssertionGenerator(signerInfo);
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setAttestingEntity(signerInfo);
        TcpKnob requestTcp = (TcpKnob) message.getKnob(TcpKnob.class);
        if (requestTcp != null) {
            try {
                InetAddress clientAddress = InetAddress.getByName(requestTcp.getRemoteAddress());
                samlOptions.setClientAddress(clientAddress);
            } catch (UnknownHostException e) {
                auditor.logAndAudit(AssertionMessages.HTTPROUTE_CANT_RESOLVE_IP, null, e);
            }
        }
        samlOptions.setVersion(assertion.getSamlAssertionVersion());
        samlOptions.setExpiryMinutes(assertion.getSamlAssertionExpiry());
        samlOptions.setIssuerKeyInfoType(assertion.isUseThumbprintInSamlSignature() ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT);
        KeyInfoInclusionType keyInfoType = assertion.isUseThumbprintInSamlSubject() ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT;
        if (assertion.getRecipientContext() != null)
            samlOptions.setSecurityHeaderActor(assertion.getRecipientContext().getActor());
        SubjectStatement statement = SubjectStatement.createAuthenticationStatement(
                svInputCredentials,
                                                        SubjectStatement.SENDER_VOUCHES,
                                                        keyInfoType, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        ag.attachStatement(document, statement, samlOptions);
    }    

    /**
     * Method to handle creating a delegated Kerberos ticket for use in downstream routing.
     *
     * @param context the policy enforcement context
     * @param server the server to that the delegated service ticket is destined for
     * @return a valid KerberosServiceTicket based on validated creds.  Null if
     * @throws KerberosException if there is an error obtaining the ticket
     */
    protected KerberosServiceTicket getDelegatedKerberosTicket(PolicyEnforcementContext context, String server)
       throws KerberosException
    {
        final KerberosServiceTicket delegatedServiceTicket;

        // first locate the Kerberos service ticket from the request
        final ProcessorResult wssResults;
        wssResults = context.getRequest().getSecurityKnob().getProcessorResult();

        KerberosServiceTicket kerberosServiceTicket = null;
        if (wssResults == null) {
            java.util.List<LoginCredentials> creds = context.getCredentials();

            for (LoginCredentials cred: creds) {
                if ( cred.getPayload() instanceof KerberosServiceTicket )
                    kerberosServiceTicket = (KerberosServiceTicket) cred.getPayload();
            }

        } else {
            XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
            if (tokens == null) {
              throw new KerberosException("No security tokens found in request");
            }

            for (XmlSecurityToken tok : tokens) {
                if (tok instanceof KerberosSecurityToken) {
                    kerberosServiceTicket = ((KerberosSecurityToken) tok).getTicket().getServiceTicket();
                } else if (tok instanceof SecurityContextToken) {
                    SecurityContext securityContext = ((SecurityContextToken) tok).getSecurityContext();
                    if (securityContext instanceof SecureConversationSession) {
                        kerberosServiceTicket = (KerberosServiceTicket) ((SecureConversationSession) securityContext).getCredentials().getPayload();
                    } else {
                        logger.warning("Found security context of incorrect type '" + (securityContext == null ? "null" : securityContext.getClass().getName()) + "'.");
                    }
                }
            }
        }

        if (kerberosServiceTicket == null)
            throw new KerberosException("No Kerberos service ticket found in the request");

        // create the delegated service ticket
        KerberosRoutingClient client = new KerberosRoutingClient();
        delegatedServiceTicket =
                client.getKerberosServiceTicket(KerberosRoutingClient.getGSSServiceName("http", server), kerberosServiceTicket);

        return delegatedServiceTicket;
    }


    /**
     * Get the connection timeout to use (set using a cluster/system property)
     *
     * @return the configured or default timeout.
     */
    protected int getConnectionTimeout() {
        return getIntProperty(ServerConfig.PARAM_IO_BACK_CONNECTION_TIMEOUT,0,Integer.MAX_VALUE,0);
    }

    /**
     * Get the timeout to use (set using a cluster/system property)
     *
     * @return the configured or default timeout.
     */
    protected int getTimeout() {
        return getIntProperty(ServerConfig.PARAM_IO_BACK_READ_TIMEOUT,0,Integer.MAX_VALUE,0);
    }

    /**
     * Get the stale check count to use (set using a cluster/system property)
     * @return the stale check count to use
     */
    protected int getStaleCheckCount() {
        return getIntProperty(ServerConfig.PARAM_IO_STALE_CHECK_PER_INTERVAL,0,1000,1);        
    }

    //- PRIVATE

    // instance
    private final Logger logger;
    private final Auditor auditor;

    /*
     * Get a system property using the configured min, max and default values.
     */
    private int getIntProperty(String propName, int min, int max, int defaultValue) {
        int value = defaultValue;

        try {
            String configuredValue = ServerConfig.getInstance().getPropertyCached(propName);
            if(configuredValue!=null) {
                value = Integer.parseInt(configuredValue);

                boolean useDefault = false;
                if(value<min) {
                    useDefault = true;
                    logger.warning("Configured value for property '"+propName+"', is BELOW the minimum '"+min+"', using default value '"+defaultValue+"'.");
                }
                else if(value>max) {
                    useDefault = true;
                    logger.warning("Configured value for property '"+propName+"', is ABOVE the maximum '"+max+"', using default value '"+defaultValue+"'.");
                }

                if(useDefault) value = defaultValue;
            }
        }
        catch(SecurityException se) {
            logger.warning("Cannot access property '"+propName+"', using default value '"+defaultValue+"', error is: " + se.getMessage());
        }
        catch(NumberFormatException nfe) {
            logger.warning("Cannot parse property '"+propName+"', using default value '"+defaultValue+"', error is: " + nfe.getMessage());
        }

        return value;
    }

    protected void firePostRouting(PolicyEnforcementContext context, URL url, int status) {
        messageProcessingEventChannel.publishEvent(new PostRoutingEvent(this, context, url, status));
    }

    protected void firePreRouting(PolicyEnforcementContext context, URL u) {
        messageProcessingEventChannel.publishEvent(new PreRoutingEvent(this, context, u));
    }
}
