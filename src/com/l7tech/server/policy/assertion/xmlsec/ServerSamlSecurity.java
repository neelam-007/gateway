package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.saml.InvalidAssertionException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.apache.xmlbeans.XmlError;
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
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.*;
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

            //the asssertion validation is expensive, we may think
            XmlOptions xo = new XmlOptions();
            xo.setLoadLineNumbers();
            AssertionDocument doc = AssertionDocument.Factory.parse(assertionElement, xo);

            xo = new XmlOptions();
            Collection errors = new ArrayList();
            xo.setErrorListener(errors);
            xo.setSavePrettyPrint();
            xo.setSavePrettyPrintIndent(2);
            if(!doc.validate(xo)) {
                for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
                    XmlError xerr = (XmlError)iterator.next();
                    logger.warning("Error validating SAML assertion" +xerr);
                }
                StringWriter sw = new StringWriter();
                doc.save(sw, xo);
                logger.warning("Aborting request (invalid SAML assertion) \n"+sw.toString());
                return AssertionStatus.FALSIFIED;
            }

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
            logger.log(Level.SEVERE, "error parsing the SAML assertion", e);
            return AssertionStatus.FALSIFIED;
        } catch (InvalidAssertionException e) {
              logger.log(Level.SEVERE, "Invalid saml assertion", e);
            return AssertionStatus.BAD_REQUEST;
        }
    }

    private boolean validateIntervalConditions(AssertionType at)
      throws InvalidAssertionException {
        checkNonNullAssertionElement("Assertion", at);
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone("GMT")); // spec says UTC, that is GMT for our purpose
        now.setTime(new Date());
        ConditionsType type = at.getConditions();
        checkNonNullAssertionElement("Conditions", type);
        Calendar notBefore = type.getNotBefore();
        checkNonNullAssertionElement("Not Before", notBefore);
        Calendar notAfter = type.getNotOnOrAfter();
        checkNonNullAssertionElement("Not After", notAfter);
        final boolean retb = (notBefore.before(now) && notAfter.after(now));

        if (!retb && logger.getLevel().intValue() <= Level.INFO.intValue()) {
            StringBuffer sb = new StringBuffer();
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            sb.append("Date/time range check failed").append("\n")
              .append("Time Now is:"+df.format(now.getTime())).append("\n")
              .append("Not Before is:"+df.format(notBefore.getTime())).append("\n")
              .append("Not After is:"+df.format(notAfter.getTime()));
            logger.info(sb.toString());

        }
        return retb;
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

    /**
     * helper that checks assertion elements for <b>null</b>
     * @param element the element to check
     * @throws InvalidAssertionException if null
     */
    private void checkNonNullAssertionElement(String name, Object element)
      throws InvalidAssertionException {
        if (element == null) {
            name = (name == null) ? "" : name;
            throw new InvalidAssertionException("The required element '"+name+" ' is null");
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
