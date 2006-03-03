/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.SecurityKnob;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.xml.SecurityActor;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.ServerConfig;

import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Base class for routing assertions.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class ServerRoutingAssertion implements ServerAssertion {

    //- PUBLIC

    public static final String ENCODING = "UTF-8";

    //- PROTECTED

    // instance
    protected final ApplicationContext applicationContext;

    /**
     *
     */
    protected ServerRoutingAssertion(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
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
                    } else {
                        defaultSecHeader = SoapUtil.getSecurityElement(doc);
                    }
                } catch (InvalidDocumentFormatException e) {
                    String msg = "this option is not supported for non-soap messages. this message is " +
                                 "supposed to be soap but does not appear to be";
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_FORMAT, null, e);
                    throw new PolicyAssertionException(msg);
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
                        throw new PolicyAssertionException(msg);
                    }
                    if (header != null) {
                        if (XmlUtil.elementIsEmpty(header)) {
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
                                SoapUtil.removeSoapAttr(defaultSecHeader, SoapUtil.ACTOR_ATTR_NAME);
                                SoapUtil.removeSoapAttr(defaultSecHeader, SoapUtil.MUSTUNDERSTAND_ATTR_NAME);
                            }
                        }
                    } else if (pr != null && pr.getProcessedActor() == SecurityActor.NOACTOR) {
                        Element defaultSecHeader = SoapUtil.getSecurityElement(doc);
                        if (defaultSecHeader != null) {
                            SoapUtil.removeSoapAttr(defaultSecHeader, SoapUtil.MUSTUNDERSTAND_ATTR_NAME);
                        }
                    }
                } catch (InvalidDocumentFormatException e) {
                    String msg = "this option is not supported for non-soap messages. this message is " +
                                 "supposed to be soap but does not appear to be";
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_FORMAT, null, e);
                    throw new PolicyAssertionException(msg);
                }
            }

            // PROMOTE ANOTHER ONE IF NECESSARY
            if (secHeaderHandlingOption == RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER && otherToPromote != null) {
                Document doc = context.getRequest().getXmlKnob().getDocumentWritable();
                // check if that actor is present
                Element secHeaderToPromote;
                try {
                    secHeaderToPromote = SoapUtil.getSecurityElement(doc, otherToPromote);
                } catch (InvalidDocumentFormatException e) {
                    // the manager does not allow you to set this
                    // option for non-soap service therefore this
                    // should not hapen
                    String msg = "this option is not supported for non-soap messages. " +
                                 "something is wrong with this policy";
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_NON_SOAP_WRONG_POLICY, null, e);
                    throw new PolicyAssertionException(msg);
                }
                if (secHeaderToPromote != null) {
                    // do it
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_PROMOTING_ACTOR, new String[] {otherToPromote});
                    SoapUtil.nukeActorAttribute(secHeaderToPromote);
                } else {
                    // this is not a big deal but might indicate something wrong
                    // with the assertion => logging as info
                    auditor.logAndAudit(AssertionMessages.HTTPROUTE_NO_SECURITY_HEADER, new String[] {otherToPromote});
                }
            }
        }
    }

    /**
     * Get the connection timeout to use (set using a system property)
     *
     * @return the configured or default timeout.
     */
    protected int getConnectionTimeout() {
        return getIntProperty(ServerConfig.PARAM_IO_BACK_CONNECTION_TIMEOUT,0,Integer.MAX_VALUE,0);
    }

    /**
     * Get the timeout to use (set using a system property)
     *
     * @return the configured or default timeout.
     */
    protected int getTimeout() {
        return getIntProperty(ServerConfig.PARAM_IO_BACK_READ_TIMEOUT,0,Integer.MAX_VALUE,0);
    }

    //- PRIVATE

    // class
    private static final Logger logger = Logger.getLogger(ServerRoutingAssertion.class.getName());

    // instance
    private final Auditor auditor;

    /**
     * Get a system property using the configured min, max and default values.
     */
    private int getIntProperty(String propName, int min, int max, int defaultValue) {
        int value = defaultValue;

        try {
            String configuredValue = ServerConfig.getInstance().getProperty(propName);
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
