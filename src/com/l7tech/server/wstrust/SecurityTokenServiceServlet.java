/*
* $Id$
 */

package com.l7tech.server.wstrust;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.server.AuthenticatableHttpServlet;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The servlet that receives saml protocol request messages.
 *
 * @author emil
 */
public class SecurityTokenServiceServlet extends AuthenticatableHttpServlet {
    private final Logger logger = Logger.getLogger(getClass().getName());

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        logger.info("saml protocol servlet initialized");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
        ServletOutputStream os = res.getOutputStream();
        try {
            MessageContext ctx = getServletContext(req, res);
            res.setContentType("text/xml");
            MessageInvocation mi = new MessageInvocation(SoapUtil.parseSoapMessage(req.getInputStream()), ctx, getHandlers());
            Document response = mi.getResponseDocument();
            XmlUtil.nodeToOutputStream(response, os);
        } catch (MessageNotSoapException e) {
            streamFault("Message not SOAP", e, os);
        } catch (SAXException e) {
            streamFault("SAX parser error", e, os);
        }
    }

    /**
     * Obtain the message context for the
     * @param req the servlet request
     * @param res the servlet response
     * @return the message context
     */
    private MessageContext getServletContext(HttpServletRequest req, HttpServletResponse res) {
        MessageContext ctx = new MessageContext();
        ctx.setProperty("javax.servlet.http.HttpServletRequest", req);
        ctx.setProperty("javax.servlet.http.HttpServletResponse", res);
        return ctx;
    }


    private Handler[] getHandlers() {
        return new Handler[] {new CredentialsFinder()};
    }

    /**
     * Stream soap fault code into the output stream for a given exception.
     *
     * @param e  the exception
     * @param os the outputr stream
     */
    private void streamFault(String faultString, Exception e, OutputStream os) {
        logger.log(Level.WARNING, "Returning SOAP fault " + faultString, e);
        try {
            SOAPMessage msg = SoapUtil.makeMessage();
            SoapUtil.addFaultTo(msg, SoapUtil.FC_SERVER, faultString, null);
            final SOAPEnvelope envelope = msg.getSOAPPart().getEnvelope();
            SOAPFault fault = envelope.getBody().getFault();
            Detail detail = fault.addDetail();
            DetailEntry de = detail.addDetailEntry(envelope.createName("FaultDetails"));
            de.addTextNode(e.toString());
            msg.writeTo(os);
        } catch (Exception se) {
            se.printStackTrace();
            throw new RuntimeException(se);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        PrintWriter out = res.getWriter();
        out.println("<html>");
        out.println("<head><title>GET not supported!</title></head>");
        out.println("<body><h1>GET not supported!</h1>Use POST instead!</body>");
        out.close();
    }

}

