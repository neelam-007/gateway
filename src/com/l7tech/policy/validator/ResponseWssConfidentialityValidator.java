package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.service.PublishedService;
import org.jaxen.dom.DOMXPath;

import java.util.logging.Logger;

/**
 * Validates the <code>ResponseWssConfidentiality</code> assertion internals. This validates
 * the XPath requirements and the the encryption method algorithm consistency. The processing
 * model requires that the same encryption method algorithm is used in the policy path.
 *
 * @author emil
 */
public class ResponseWssConfidentialityValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(ResponseWssConfidentialityValidator.class.getName());
    private final ResponseWssConfidentiality assertion;

    public ResponseWssConfidentialityValidator(ResponseWssConfidentiality ra) {
        assertion = ra;
    }

    public void validate(AssertionPath path, PublishedService service, PolicyValidatorResult result) {
        String pattern = null;
        if (assertion.getXpathExpression() != null) {
            pattern = assertion.getXpathExpression().getExpression();
        }
        if (pattern == null) {
            result.addError(new PolicyValidatorResult.Error(assertion, path, "XPath pattern is missing", null));
            logger.info("XPath pattern is missing");
            return;
        }

        if (pattern.equals("/soapenv:Envelope")) {
            result.addError(new PolicyValidatorResult.Error(assertion, path, "The path " + pattern + " is " +
              "not valid for XML encryption", null));

        }
        try {
            new DOMXPath(pattern);
        } catch (Exception e) {
            result.addError(new PolicyValidatorResult.Error(assertion, path, "XPath pattern is not valid", e));
            logger.info("XPath pattern is not valid");
            return;
        }
        Assertion[] assertionPath = path.getPath();
        for (int i = assertionPath.length - 1; i >= 0; i--) {
            Assertion a = assertionPath[i];
            if (a != assertion && a instanceof ResponseWssConfidentiality) {
                ResponseWssConfidentiality ra = (ResponseWssConfidentiality)a;
                if (!ra.getXEncAlgorithm().equals(assertion.getXEncAlgorithm())) {
                    String message = "Multiple confidentiality assertions present with different Encryption Method Algorithms";
                    result.addError(new PolicyValidatorResult.Error(assertion, path, message, null));
                    logger.info(message);
                }
            }
        }
    }
}
