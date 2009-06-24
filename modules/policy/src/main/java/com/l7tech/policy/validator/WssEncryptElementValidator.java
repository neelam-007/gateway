package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.wsdl.Wsdl;
import org.jaxen.dom.DOMXPath;

import java.util.logging.Logger;

/**
 * Validates the <code>WssEncryptElement</code> assertion internals. This validates
 * the XPath requirements and the the encryption method algorithm consistency. The processing
 * model requires that the same encryption method algorithm is used in the policy path.
 *
 * @author emil
 */
public class WssEncryptElementValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(WssEncryptElementValidator.class.getName());
    private final WssEncryptElement assertion;
    private final boolean requiresDecoration;
    private String errString;
    private Throwable errThrowable;

    public WssEncryptElementValidator(WssEncryptElement ra) {
        assertion = ra;
        requiresDecoration = !Assertion.isResponse( assertion );
        String pattern = null;
        if (assertion.getXpathExpression() != null) {
            pattern = assertion.getXpathExpression().getExpression();
        }
        if (pattern == null) {
            errString = "XPath pattern is missing";
            logger.info(errString);
        } else if (pattern.equals("/soapenv:Envelope")) {
            errString = "The path " + pattern + " is not valid for XML encryption";
        } else {
            try {
                new DOMXPath(pattern);
            } catch (Exception e) {
                errString = "XPath pattern is not valid";
                errThrowable = e;
                logger.info(errString);
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
                 a instanceof WssEncryptElement &&
                 AssertionUtils.isSameTargetRecipient( assertion, a ) &&
                 AssertionUtils.isSameTargetMessage(assertion, a)) {
                WssEncryptElement ra = (WssEncryptElement)a;
                if (!ra.getXEncAlgorithm().equals(assertion.getXEncAlgorithm())) {
                    String message = "Multiple confidentiality assertions present with different Encryption Method Algorithms";
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
