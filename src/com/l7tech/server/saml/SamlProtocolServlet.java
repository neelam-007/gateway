/*
 * $Id$
 */

package com.l7tech.server.saml;

import com.l7tech.common.util.SoapUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.xml.messaging.JAXMServlet;
import javax.xml.messaging.ReqRespListener;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

import x0Protocol.oasisNamesTcSAML1.RequestDocument;


/**
 * Sample servlet that receives saml protocol request messages.
 *
 * @author emil
 */
public class SamlProtocolServlet
  extends JAXMServlet implements ReqRespListener {
    private final Logger logger = Logger.getLogger(getClass().getName());

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        logger.info("saml protocol servlet initialized");
    }

    public SOAPMessage onMessage(SOAPMessage message) {
        try {
            // parse/locate the SAML request in SOAP message...
            SOAPMessage msg = msgFactory.createMessage();
            SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
            // add the response

            return msg;
        } catch (Exception e) {
            return asFaultCode(e);
        }
    }

    private SOAPMessage asFaultCode(Exception e) {
        logger.log(Level.WARNING, "Returning SOAP fault for ",e);
        try {
            SOAPMessage msg = msgFactory.createMessage();
            SoapUtil.addFaultTo(msg, SoapUtil.FC_SERVER, e.getMessage(), null);
            return msg;
        } catch (SOAPException se) {
            se.printStackTrace();
            throw new RuntimeException(se);
        }
    }
}

