package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.xmlsec.WssSignElement;
import com.l7tech.policy.assertion.xmlsec.WssDecorationConfig;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.wsdl.Wsdl;
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
public class WssSignElementValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(WssSignElementValidator.class.getName());
    private final WssSignElement assertion;
    private final boolean requiresDecoration;
    private String errString;
    private Throwable errThrowable;

    public WssSignElementValidator(WssSignElement ra) {
        assertion = ra;
        requiresDecoration = !Assertion.isResponse( assertion );
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

    @Override
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, path, errString, errThrowable));

        boolean seenSelf = true;
        boolean seenDecoration = false;
        Assertion[] assertionPath = path.getPath();
        for (int i = assertionPath.length - 1; i >= 0; i--) {
            Assertion a = assertionPath[i];
            if (!a.isEnabled()) continue;
            if ( a != assertion &&
                 a instanceof WssDecorationConfig &&
                 AssertionUtils.isSameTargetRecipient( assertion, a ) &&
                 AssertionUtils.isSameTargetMessage( assertion, a ) ) {
                WssDecorationConfig ra = (WssDecorationConfig)a;
                if (!ra.getKeyReference().equals(assertion.getKeyReference())) {
                    String message = "Multiple integrity assertions present with different Key Reference requirements";
                    result.addError(new PolicyValidatorResult.Error(assertion, path, message, null));
                    logger.info(message);
                }
                if (ra.isProtectTokens() != assertion.isProtectTokens()) {
                    String message = "Multiple integrity assertions present with different token signature requirements";
                    result.addError(new PolicyValidatorResult.Error(assertion, path, message, null));
                    logger.info(message);
                }
            }
            if ( a == assertion ) {
                seenSelf = false; // loop is backwards
            }
            if ( seenSelf && a instanceof WsSecurity && ((WsSecurity)a).isApplyWsSecurity() && AssertionUtils.isSameTargetMessage( assertion, a ) ) {
                seenDecoration = true;
            }
        }

        if ( requiresDecoration && !seenDecoration ) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, path, "This assertion will be ignored. An \"Add or remove WS-Security\" assertion should be used to apply security.", null));            
        }
    }
}
