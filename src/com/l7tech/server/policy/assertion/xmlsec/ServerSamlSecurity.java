package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>ServerSamlSecurity</code> represents the server side saml
 * security policy assertion element.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerSamlSecurity implements ServerAssertion {
    private SamlSecurity assertion;
    public static final String HEADER_EL_NAME = "Header";
    public static final String SECURITY_EL_NAME = "Security";
    public static final String SECURITY_NAMESPACE_PREFIX = "wsse";
    public static final String SECURITY_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    public static final String SECURITY_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/12/secext";

    /**
     * Create the server side saml security policy element
     * 
     * @param sa the saml
     */
    public ServerSamlSecurity(SamlSecurity sa) {
        if (sa == null) {
            throw new IllegalArgumentException();
        }
        assertion = sa;
    }

    /**
     * SSG Server-side processing of the given request.
     * 
     * @param request  (In/Out) The request to check.  May be modified by processing.
     * @param response (Out) The response to send back.  May be replaced during processing.
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(Request request, Response response)
      throws IOException, PolicyAssertionException {
        try {
            SoapRequest sr = (SoapRequest)request;
            Document document = sr.getDocument();
            Element securityElement = getSecurityElement(document);
        } catch (NoSuchElementException e) {
            logger.log(Level.SEVERE, "Security header missing", e);
            return AssertionStatus.FALSIFIED;
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "error getting the xml document", e);
            return AssertionStatus.FALSIFIED;
        }
        return AssertionStatus.NONE;
    }

    /**
     * Returns the Security element from the header of a soap message. If the
     * message does not have a header throwsNoSuchElementException
     * 
     * @param document DOM document containing the soap message
     * @return the security element (never null)
     */
    private Element getSecurityElement(Document document)
      throws NoSuchElementException {
        NodeList listSecurityElements = document.getElementsByTagNameNS(SECURITY_NAMESPACE, SECURITY_EL_NAME);
        if (listSecurityElements.getLength() > 0) {
            return (Element)listSecurityElements.item(0);
        }
        listSecurityElements = document.getElementsByTagNameNS(SECURITY_NAMESPACE2, SECURITY_EL_NAME);

        if (listSecurityElements.getLength() > 0) {
            return (Element)listSecurityElements.item(0);
        }
        throw new NoSuchElementException("Could not find security header in "+document);
    }
    private Logger logger = LogManager.getInstance().getSystemLogger();
}
