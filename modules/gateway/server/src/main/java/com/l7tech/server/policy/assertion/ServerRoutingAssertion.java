/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.SecurityKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.message.TcpKnob;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.KerberosSecurityToken;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.security.kerberos.KerberosRoutingClient;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.kerberos.KerberosException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

/**
 * Base class for routing assertions.
 *
 * @author alex
 */
public abstract class ServerRoutingAssertion<RAT extends RoutingAssertion> extends AbstractServerAssertion<RAT> implements ServerAssertion {

    //- PUBLIC

    public static final String ENCODING = "UTF-8";

    //- PROTECTED

    // instance
    protected final ApplicationContext applicationContext;

    /**
     *
     */
    protected ServerRoutingAssertion(RAT data, ApplicationContext applicationContext, Logger logger) {
        super(data);
        this.applicationContext = applicationContext;
        this.logger = logger;
        this.auditor = new Auditor(this, applicationContext, logger);
    }
    
    /**
     *
     */
    protected void handleProcessedSecurityHeader(PolicyEnforcementContext context,
                                                 int secHeaderHandlingOption,
                                                 String otherToPromote)
            throws SAXException, IOException, PolicyAssertionException
    {
        if (context.getService().isSoap()) {
            final XmlKnob requestXml = (XmlKnob)context.getRequest().getKnob(XmlKnob.class);
            final SecurityKnob requestSec = (SecurityKnob)context.getRequest().getKnob(SecurityKnob.class);
            if (requestXml == null) {
                logger.finest("skipping this because the message isn't XML");
                return;
            }
            if (requestSec == null || requestSec.getProcessorResult() == null) {
                logger.finest("skipping this because no security header were processed");
                return;
            }
            // DELETE CURRENT SECURITY HEADER IF NECESSARY
            if (secHeaderHandlingOption == RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER ||
                secHeaderHandlingOption == RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER) {
                Document doc = context.getRequest().getXmlKnob().getDocumentWritable();
                Element defaultSecHeader;
                try {
                    ProcessorResult pr = requestSec.getProcessorResult();
                    if (pr != null && pr.getProcessedActor() == SecurityActor.L7ACTOR) {
                        defaultSecHeader = SoapUtil.getSecurityElement(doc, SecurityActor.L7ACTOR.getValue());
                        if(defaultSecHeader == null) { // Try again looking for the secure_span actor
                            defaultSecHeader = SoapUtil.getSecurityElement(doc, SecurityActor.L7ACTOR_OLD.getValue());
                        }
                    } else {
                        defaultSecHeader = SoapUtil.getSecurityElement(doc);
                    }
                } catch (InvalidDocumentFormatException e) {
                    String msg = "this option is not supported for non-soap messages. this message is " +
                                 "supposed to be soap but does not appear to be";
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_FORMAT, null, e);
                    throw new PolicyAssertionException(assertion, msg);
                }
                if (defaultSecHeader != null) {
                    defaultSecHeader.getParentNode().removeChild(defaultSecHeader);

                    // we should not leave an empty header element
                    Element header;
                    try {
                        header = SoapUtil.getHeaderElement(doc);
                    } catch (InvalidDocumentFormatException e) {
                        String msg = "this option is not supported for non-soap messages. this message is " +
                                     "supposed to be soap but does not appear to be";
                        auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_FORMAT, null, e);
                        throw new PolicyAssertionException(assertion, msg);
                    }
                    if (header != null) {
                        if (DomUtils.elementIsEmpty(header)) {
                            header.getParentNode().removeChild(header);
                        }
                    }
                }
            } else if (secHeaderHandlingOption == RoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS) {
                Document doc = context.getRequest().getXmlKnob().getDocumentWritable();
                try {
                    ProcessorResult pr = requestSec.getProcessorResult();
                    // leaving the processed security header for passthrough means that if the processed
                    // actor was l7, we need to promote to default (unless there is a default present)
                    if (pr != null && pr.getProcessedActor() == SecurityActor.L7ACTOR) {
                        Element defaultSecHeader = SoapUtil.getSecurityElement(doc, SecurityActor.L7ACTOR.getValue());
                        if (defaultSecHeader != null) {
                            Element noActorSecHeader = SoapUtil.getSecurityElement(doc);
                            if (noActorSecHeader != null && noActorSecHeader != defaultSecHeader) {
                                logger.info("we can't promote l7 sec header as no actor because " +
                                            "there is already a noactor one present. there may be " +
                                            "something wrong with this policy");
                            } else {
                                SoapUtil.removeSoapAttr(defaultSecHeader, SoapConstants.ACTOR_ATTR_NAME);
                                SoapUtil.removeSoapAttr(defaultSecHeader, SoapConstants.MUSTUNDERSTAND_ATTR_NAME);
                            }
                        }
                    } else if (pr != null && pr.getProcessedActor() == SecurityActor.NOACTOR) {
                        Element defaultSecHeader = SoapUtil.getSecurityElement(doc);
                        if (defaultSecHeader != null) {
                            SoapUtil.removeSoapAttr(defaultSecHeader, SoapConstants.MUSTUNDERSTAND_ATTR_NAME);
                        }
                    }
                } catch (InvalidDocumentFormatException e) {
                    String msg = "this option is not supported for non-soap messages. this message is " +
                                 "supposed to be soap but does not appear to be";
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_FORMAT, null, e);
                    throw new PolicyAssertionException(assertion, msg);
                }
            }

            // PROMOTE ANOTHER ONE IF NECESSARY
            if (secHeaderHandlingOption == RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER && otherToPromote != null) {
                Document doc = context.getRequest().getXmlKnob().getDocumentWritable();
                // check if that actor is present
                Element secHeaderToPromote;
                try {
                    secHeaderToPromote = SoapUtil.getSecurityElement(doc, otherToPromote);
                } catch ( InvalidDocumentFormatException e) {
                    // the manager does not allow you to set this
                    // option for non-soap service therefore this
                    // should not hapen
                    String msg = "this option is not supported for non-soap messages. " +
                                 "something is wrong with this policy";
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_POLICY, null, e);
                    throw new PolicyAssertionException(assertion, msg);
                }
                if (secHeaderToPromote != null) {
                    // do it
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_PROMOTING_ACTOR, otherToPromote);
                    SoapUtil.nukeActorAttribute(secHeaderToPromote);
                } else {
                    // this is not a big deal but might indicate something wrong
                    // with the assertion => logging as info
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_NO_SECURITY_HEADER, otherToPromote);
                }
            }
        }
    }

    /**
     * Attach a sender-vouches SAML assertion to the request.
     *
     * @param context The pec to use
     * @param signerInfo For signing the assertion / message
     * @throws org.xml.sax.SAXException If the request is not XML
     * @throws java.io.IOException If there is an error getting the request document
     * @throws java.security.SignatureException If an error occurs when signing
     * @throws java.security.cert.CertificateException If the signing certificate is invalid.
     */
    protected void doAttachSamlSenderVouches(PolicyEnforcementContext context, SignerInfo signerInfo)
            throws SAXException, IOException, SignatureException, CertificateException {
        LoginCredentials svInputCredentials = context.getLastCredentials();
        if (svInputCredentials == null) {
            auditor.logAndAudit(AssertionMessages.HTTPROUTE_SAML_SV_NOT_AUTH);
        } else {
            Document document = context.getRequest().getXmlKnob().getDocumentWritable();
            SamlAssertionGenerator ag = new SamlAssertionGenerator(signerInfo);
            SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
            samlOptions.setAttestingEntity(signerInfo);
            TcpKnob requestTcp = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
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
    }    

    /**
     * Method to handle creating a delegated Kerberos ticket for use in downstream routing.
     *
     * @param context the policy enforcement context
     * @param server the server to that the delegated service ticket is destined for
     * @return a valid KerberosServiceTicket based on validated creds.  Null if
     * @throws KerberosException
     */
    protected KerberosServiceTicket getDelegatedKerberosTicket(PolicyEnforcementContext context, String server)
       throws KerberosException
    {
        KerberosServiceTicket delegatedServiceTicket = null;

        // first locate the Kerberos service ticket from the request
        ProcessorResult wssResults = null;
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

            for (int i = 0; i < tokens.length; i++) {
                XmlSecurityToken tok = tokens[i];
                if (tok instanceof KerberosSecurityToken) {
                    kerberosServiceTicket = ((KerberosSecurityToken) tok).getTicket().getServiceTicket();
                }
                else if(tok instanceof SecurityContextToken) {
                    SecurityContext securityContext = ((SecurityContextToken)tok).getSecurityContext();
                    if(securityContext instanceof SecureConversationSession) {
                        kerberosServiceTicket = (KerberosServiceTicket) ((SecureConversationSession) securityContext).getCredentials().getPayload();
                    } else {
                        logger.warning("Found security context of incorrect type '"+(securityContext==null ? "null" : securityContext.getClass().getName())+"'.");
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
     */
    protected int getStaleCheckCount() {
        return getIntProperty(ServerConfig.PARAM_IO_STALE_CHECK_PER_INTERVAL,0,1000,1);        
    }

    //- PRIVATE

    // instance
    private final Logger logger;
    private final Auditor auditor;

    /**
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
}
