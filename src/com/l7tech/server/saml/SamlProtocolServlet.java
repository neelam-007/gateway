/*
 * $Id$
 */

package com.l7tech.server.saml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import x0Protocol.oasisNamesTcSAML1.ResponseDocument;


/**
 * The servlet that receives saml protocol request messages.
 *
 * @author emil
 */
public class SamlProtocolServlet extends HttpServlet {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private SoapRequestGenerator soapRequestGenerator = new SoapRequestGenerator();
    private SoapResponseGenerator soapResponseGenerator = new SoapResponseGenerator();
    private Authority authority = new AuthorityImpl();

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        logger.info("saml protocol servlet initialized");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
        ServletOutputStream os = res.getOutputStream();
        try {
            ResponseDocument response = authority.process(soapRequestGenerator.fromSoapInputStream(req.getInputStream()));
            // soapResponseGenerator.asSoapMessage(
        } catch (SAXException e) {
            soapResponseGenerator.streamFault(e, os);
        } catch (InvalidDocumentFormatException e) {
            soapResponseGenerator.streamFault(e, os);
        } catch (XmlException e) {
            soapResponseGenerator.streamFault(e, os);
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

