package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.AssertionDocument;
import x0Assertion.oasisNamesTcSAML1.AssertionType;
import x0Assertion.oasisNamesTcSAML1.ConditionsType;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
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
    private static final String ASSERTION_EL_NAME = "Assertion";
    private static final String SAML_NS = "urn:oasis:names:tc:SAML:1.0:assertion";

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
            Element assertionElement = getAssertionElement(document);
            XmlOptions xo = new XmlOptions();
            xo.setLoadLineNumbers();
            AssertionDocument doc = AssertionDocument.Factory.parse(assertionElement, xo);

//            xo = new XmlOptions();
//            Collection errors = new ArrayList();
//            xo.setErrorListener(errors);
//            System.out.println("The document is " + (doc.validate(xo) ? "valid" : "invalid"));
//            for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
//                XmlError xerr = (XmlError)iterator.next();
//                System.out.println(xerr);
//            }

            AssertionType at = doc.getAssertion();
            if (assertion.isValidateValidityPeriod()) {
                if (!validateIntervalConditions(at)) {
                    return AssertionStatus.FALSIFIED;
                }
            }

            return AssertionStatus.NONE;
        } catch (NoSuchElementException e) {
            logger.log(Level.SEVERE, "SAML Assertion element missing", e);
            return AssertionStatus.FALSIFIED;
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "error getting the xml document", e);
            return AssertionStatus.FALSIFIED;
        } catch (XmlException e) {
            logger.log(Level.SEVERE, "error pasrsing the SAML assertion", e);
            return AssertionStatus.FALSIFIED;
        }
    }

    private boolean validateIntervalConditions(AssertionType at) {
        Date now = new Date();
        ConditionsType type = at.getConditions();
        Calendar notBefore = type.getNotBefore();
        Calendar notAfter = type.getNotOnOrAfter();
        return (notBefore.before(now) && notAfter.after(now));
    }

    /**
     * Returns the Security element from the header of a soap message. If the
     * message does not have a header throwsNoSuchElementException
     * 
     * @param document DOM document containing the soap message
     * @return the security element (never null)
     */
    private Element getAssertionElement(Document document)
      throws NoSuchElementException {
        NodeList listSecurityElements = document.getElementsByTagNameNS(SAML_NS, ASSERTION_EL_NAME);
        if (listSecurityElements.getLength() > 0) {
            return (Element)listSecurityElements.item(0);
        }

        throw new NoSuchElementException("Could not find security header in " + document);
    }

    private Logger logger = LogManager.getInstance().getSystemLogger();
}
