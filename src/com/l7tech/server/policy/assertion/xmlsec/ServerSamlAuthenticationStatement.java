package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlStatementValidate;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class <code>ServerSamlAuthenticationStatement</code> represents the server
 * side saml Authentication Statement security policy assertion element.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerSamlAuthenticationStatement implements ServerAssertion {
    private SamlAuthenticationStatement assertion;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private ApplicationContext applicationContext;
    private PolicyFactory policyFactory;
    private SamlStatementValidate statementValidate;

    /**
     * Create the server side saml security policy element
     *
     * @param sa the saml
     */
    public ServerSamlAuthenticationStatement(SamlAuthenticationStatement sa, ApplicationContext context) {
        if (sa == null) {
            throw new IllegalArgumentException();
        }
        this.applicationContext = context;
        if (applicationContext == null) {
            throw new IllegalArgumentException("The Application Context is required");
        }

        assertion = sa;
        statementValidate = SamlStatementValidate.getValidate(assertion, context);

    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException {
        try {
            final XmlKnob xmlKnob = context.getRequest().getXmlKnob();
            if (!context.getRequest().isSoap()) {
                logger.finest("Request not SOAP; cannot validate Saml Statement");
                return AssertionStatus.BAD_REQUEST;
            }
            Collection errors = new ArrayList();

            ProcessorResult wssResults = xmlKnob.getProcessorResult();
            //statementValidate.validate(xmlKnob.getDocumentReadOnly(), wssResults);

        } catch (SAXException e) {
            throw (IOException)new IOException().initCause(e);
        }
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

}
