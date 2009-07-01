package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
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
public class WssEncryptElementValidator extends WssEncryptingDecorationAssertionValidator {
    private static final Logger logger = Logger.getLogger(WssEncryptElementValidator.class.getName());
    private final WssEncryptElement assertion;
    private final XpathBasedAssertionValidator xpathBasedAssertionValidator;

    public WssEncryptElementValidator( final WssEncryptElement wssEncryptElement ) {
        super(wssEncryptElement, true, false);
        assertion = wssEncryptElement;
        xpathBasedAssertionValidator = new XpathBasedAssertionValidator( wssEncryptElement );
    }

    @Override
    public void validate( final AssertionPath path,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        xpathBasedAssertionValidator.validate( path, wsdl, soap, result );

        super.validate( path, wsdl, soap, result );
    }

    @Override
    protected void validateAssertion( AssertionPath path, Assertion a, PolicyValidatorResult result ) {
        if ( a instanceof WssEncryptElement &&
             AssertionUtils.isSameTargetRecipient( assertion, a ) &&
             AssertionUtils.isSameTargetMessage(assertion, a)) {
            WssEncryptElement ra = (WssEncryptElement)a;
            if (!ra.getXEncAlgorithm().equals(assertion.getXEncAlgorithm())) {
                String message = "Multiple confidentiality assertions present with different Encryption Method Algorithms";
                result.addError(new PolicyValidatorResult.Error(assertion, path, message, null));
                logger.info(message);
            }
        }
    }
}
