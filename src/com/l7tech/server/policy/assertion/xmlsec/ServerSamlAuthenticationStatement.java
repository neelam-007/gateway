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
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
        policyFactory = (PolicyFactory)applicationContext.getBean("policyFactory");

        assertion = sa;
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
            ProcessorResult wssResults = xmlKnob.getProcessorResult();
            Element documentElement = xmlKnob.getDocumentReadOnly().getDocumentElement();

            String securityNS = wssResults.getSecurityNS();
            if (null == securityNS) {
                logger.finest("No Security Header found");
                return AssertionStatus.BAD_REQUEST;
            }

            String securityHeaderXpath = SoapUtil.SOAP_HEADER_XPATH +"/" + securityNS + ":Security";
            XpathExpression shx = XpathExpression.soapBodyXpathValue();
            shx.getNamespaces().put(SoapUtil.SECURITY_NAMESPACE_PREFIX, securityNS);
            List verifySignatures = policyFactory.makeCompositePolicy(new AllAssertion(Arrays.asList(new Object[]{
                new RequestWssIntegrity(), new RequestWssIntegrity(shx)
            })));

        } catch (SAXException e) {
            throw (IOException)new IOException().initCause(e);
        }
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

}
