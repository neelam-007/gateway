/*
 * $Id$
 */

package com.l7tech.server.saml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.AuthenticatableHttpServlet;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;

import javax.security.auth.login.FailedLoginException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Logger;


/**
 * The servlet that receives saml protocol request messages.
 *
 * @author emil
 */
public class SamlProtocolServlet extends AuthenticatableHttpServlet {
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
            User user = authenticate(req);
            res.setContentType(XmlUtil.TEXT_XML);
            ResponseDocument response = authority.process(soapRequestGenerator.fromSoapInputStream(req.getInputStream()));
            soapResponseGenerator.streamSoapMessage(response, res.getOutputStream());
            // SAML SOAP processing error reporting as per SAML binding 3.1.3.6 Error Reporting
        } catch (SAXException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            soapResponseGenerator.streamFault("Error parsing SOAP request", e, os);
        } catch (InvalidDocumentFormatException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            soapResponseGenerator.streamFault("Bad SOAP request", e, os);
        } catch (XmlException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            soapResponseGenerator.streamFault("Bad SAML request", e, os);
        } catch (SOAPException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            soapResponseGenerator.streamFault("Server SOAP error", e, os);
        } catch (AuthenticationException e) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (GeneralSecurityException e) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    private User authenticate(HttpServletRequest req)
      throws AuthenticationException, GeneralSecurityException {
        if (req.isSecure()) {
            throw new GeneralSecurityException("Not secured request; use https");
        }
        LoginCredentials cred = findCredentialsBasic(req);
        if (cred == null) {
            throw new BadCredentialsException("Credentials not received");
        }
        List users = authenticateRequestBasic(req);
        if (users.isEmpty()) {
            throw new FailedLoginException("No such user "+cred.getLogin());
        } else if (users.size() < 1) {
            throw new BadCredentialsException("Could not resolve, unambiguosly user "+cred.getLogin());
        }
        return (User)users.get(0);
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

