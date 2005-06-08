package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.service.PublishedService;
import org.jaxen.dom.DOMXPath;

import java.util.logging.Logger;

/**
 * Validates the <code>ResponseWssIntegrity</code> assertion internals. This validates
 * the XPath requirements and the the Key Reference for consistency if multiple
 *  <code>ResponseWssIntegrity</code>  are present in the policy path. The processing
 * model requires that the same Key Reference is used in the policy path.
 *
 * @author emil
 */
public class ResponseWssIntegrityValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(ResponseWssIntegrityValidator.class.getName());
    private final ResponseWssIntegrity assertion;

    public ResponseWssIntegrityValidator(ResponseWssIntegrity ra) {
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
            if (a != assertion && a instanceof ResponseWssIntegrity) {
                ResponseWssIntegrity ra = (ResponseWssIntegrity)a;
                if (!ra.getKeyReference().equals(assertion.getKeyReference())) {
                    String message = "Multiple integrity assertions present with different Key Reference requirements";
                    result.addError(new PolicyValidatorResult.Error(assertion, path, message, null));
                    logger.info(message);
                }
            }
        }
    }
}
