package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.external.assertions.saml2attributequery.DecryptElementAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.message.Message;

import java.io.IOException;
import java.security.SignatureException;
import java.security.GeneralSecurityException;

import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.bouncycastle.jce.X509Principal;

import javax.xml.xpath.XPathExpressionException;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 28-Jan-2009
 * Time: 7:30:01 PM
 */
public class ServerDecryptElementAssertion extends AbstractServerAssertion<DecryptElementAssertion> {
    private SecurityTokenResolver securityTokenResolver;

    @SuppressWarnings({"UnusedDeclaration"})
    public ServerDecryptElementAssertion( final DecryptElementAssertion assertion,
                                             final ApplicationContext context )
        throws PolicyAssertionException
    {
        super(assertion);
        securityTokenResolver = (SecurityTokenResolver)context.getBean("securityTokenResolver");
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Saml2WssProcessorImpl securityProcessor = new Saml2WssProcessorImpl(context.getRequest());
        securityProcessor.setSecurityTokenResolver(securityTokenResolver);

        try {
            Document doc = null;
            if(assertion.getInputMessageSource() == DecryptElementAssertion.REQUEST_MESSAGE) {
                doc = context.getRequest().getXmlKnob().getDocumentWritable();
            } else if(assertion.getInputMessageSource() == DecryptElementAssertion.RESPONSE_MESSAGE) {
                doc = context.getResponse().getXmlKnob().getDocumentWritable();
            } else if(assertion.getInputMessageSource() == DecryptElementAssertion.MESSAGE_VARIABLE) {
                try {
                    Message message = (Message)context.getVariable(assertion.getInputMessageVariableName());
                    doc = message.getXmlKnob().getDocumentWritable();
                } catch(NoSuchVariableException nsve) {
                    logAndAudit( AssertionMessages.VARIABLE_IS_NULL, assertion.getInputMessageVariableName() );
                    return AssertionStatus.FAILED;
                }
            }
            ElementCursor cursor = new DomElementCursor(doc);
            cursor.moveToRoot();
            XpathResult xpathResult = cursor.getXpathResult(assertion.getXpathExpression().compile(), new PolicyEnforcementContextXpathVariableFinder(context), true);

            if(xpathResult.getType() != XpathResult.TYPE_NODESET || xpathResult.getNodeSet().isEmpty()) {
                return AssertionStatus.FAILED;
            }

            for(XpathResultIterator it = xpathResult.getNodeSet().getIterator();it.hasNext();) {
                ElementCursor ec = it.nextElementAsCursor();
                securityProcessor.decryptElement(ec.asDomElement());
            }
        } catch(SAXException se) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, se.toString() );
            return AssertionStatus.FAILED;
        } catch(SignatureException se) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, se.toString() );
            return AssertionStatus.FAILED;
        } catch(InvalidXpathException ixe) {
            logAndAudit( AssertionMessages.XPATH_PATTERN_INVALID );
            return AssertionStatus.FAILED;
        } catch(XPathExpressionException xee) {
            logAndAudit( AssertionMessages.XPATH_PATTERN_INVALID );
            return AssertionStatus.FAILED;
        } catch(GeneralSecurityException gse) {
            logAndAudit( AssertionMessages.XPATH_PATTERN_INVALID );
        } catch(ProcessorException pe) {
            logAndAudit( AssertionMessages.XPATH_PATTERN_INVALID );
        } catch(InvalidDocumentFormatException idfe) {
            logAndAudit( AssertionMessages.XPATH_PATTERN_INVALID );
        }


        return AssertionStatus.NONE;
    }

    private static String reverseDN(String dn) {
        X509Principal p = new X509Principal(true, dn);
        return p.getName();
    }
}
