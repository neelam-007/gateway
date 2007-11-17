package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.common.xml.Wsdl;
import org.jaxen.dom.DOMXPath;

import java.util.logging.Logger;

/**
 * Validates the <code>RequestWssConfidentiality</code> assertion internals. This validates
 * the XPath requirements and the the encryption method algorithm consistency. The processing
 * model requires that the same encryption method algorithm is used in the policy path.
 *
 * @author emil
 */
public class RequestWssConfidentialityValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(RequestWssConfidentialityValidator.class.getName());
    private final RequestWssConfidentiality assertion;
    private String errString = null;
    private Throwable errThrowable = null;

    public RequestWssConfidentialityValidator(RequestWssConfidentiality ra) {
        assertion = ra;
        String pattern = null;
        if (assertion.getXpathExpression() != null) {
            pattern = assertion.getXpathExpression().getExpression();
        }
        if (pattern == null) {
            final String str = "XPath pattern is missing";
            logger.info(str);
            errString = str;
        } else if (pattern.equals("/soapenv:Envelope")) {
            errString = "The path " + pattern + " is " +
                        "not valid for XML encryption";
        } else {
            try {
                new DOMXPath(pattern);
            } catch (Exception e) {
                final String str = "XPath pattern is not valid";
                logger.info(str);
                errString = str;
                errThrowable = e;
            }
        }
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if (errString != null) {
            result.addError(new PolicyValidatorResult.Error(assertion, path, errString, errThrowable));
            return;
        }

        Assertion[] assertionPath = path.getPath();
        for (int i = assertionPath.length - 1; i >= 0; i--) {
            Assertion a = assertionPath[i];
            if (a != assertion && a instanceof RequestWssConfidentiality) {
                RequestWssConfidentiality ra = (RequestWssConfidentiality)a;
                if (!ra.getXEncAlgorithm().equals(assertion.getXEncAlgorithm())) {
                    String message = "Multiple confidentiality assertions present with different Encryption Method Algorithms";
                    result.addError(new PolicyValidatorResult.Error(assertion, path, message, null));
                    logger.info(message);
                }
            }
        }
    }
}
