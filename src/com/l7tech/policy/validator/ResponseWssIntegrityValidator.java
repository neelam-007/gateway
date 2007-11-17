package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.common.xml.Wsdl;
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
    private String errString;
    private Throwable errThrowable;

    public ResponseWssIntegrityValidator(ResponseWssIntegrity ra) {
        assertion = ra;
        String pattern = null;
        if (assertion.getXpathExpression() != null)
            pattern = assertion.getXpathExpression().getExpression();
        if (pattern == null) {
            errString = "XPath pattern is missing";
            logger.info(errString);
        } else {
            try {
                new DOMXPath(pattern);
            } catch (Exception e) {
                errString = "XPath pattern is not valid";
                logger.info(errString);
                errThrowable = e;
            }
        }
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, path, errString, errThrowable));

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
