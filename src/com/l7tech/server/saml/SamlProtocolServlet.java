/*
 * $Id$
 */

package com.l7tech.server.saml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import x0Protocol.oasisNamesTcSAML1.RequestDocument;


/**
 * The servlet that receives saml protocol request messages.
 *
 * @author emil
 */
public class SamlProtocolServlet extends HttpServlet {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private MessageFactory msgFactory;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        logger.info("saml protocol servlet initialized");
        try {
            msgFactory = MessageFactory.newInstance();
        } catch (SOAPException e) {
            throw new ServletException(e);
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        PrintWriter out = res.getWriter();
        out.println("<html>");
        out.println("<head><title>GET not supported!</title></head>");
        out.println("<body><h1>GET not supported!</h1>Use POST instead!</body>");
        out.close();
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
        Document document = null;
        try {
            document = XmlUtil.parse(req.getInputStream());

        } catch (SAXException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        Document docOut = onMessage(document);
    }

    private Document onMessage(Document message) {
        try {
            Element bodyElement = SoapUtil.getBodyElement(message);
            if (bodyElement == null) {
                throw new MessageNotSoapException();
            }
            Node firstChild = bodyElement.getFirstChild();
            if (firstChild == null) {
                throw new InvalidDocumentFormatException("Empty Body element");
            }
            RequestDocument rdoc = RequestDocument.Factory.parse(firstChild);
            return (Document)RequestResolver.resolve(rdoc).getResponse().newDomNode();
        } catch (Exception e) {
            return asFaultCode(e);
        }
    }

    private Document asFaultCode(Exception e) {
        logger.log(Level.WARNING, "Returning SOAP fault for ",e);
        try {
            SOAPMessage msg = msgFactory.createMessage();
            SoapUtil.addFaultTo(msg, SoapUtil.FC_SERVER, e.getMessage(), null);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            msg.writeTo(bos);
            return XmlUtil.stringToDocument(bos.toString());
        } catch (Exception se) {
            se.printStackTrace();
            throw new RuntimeException(se);
        }
    }
}

