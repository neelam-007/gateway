package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.xmlsec.WssSignElement;

/**
 * Validates the <code>ResponseWssIntegrity</code> assertion internals. This validates
 * the XPath requirements and the the Key Reference for consistency if multiple
 *  <code>ResponseWssIntegrity</code>  are present in the policy path. The processing
 * model requires that the same Key Reference is used in the policy path.
 *
 * @author emil
 */
public class WssSignElementValidator extends XpathBasedAssertionValidator {
    private final AssertionValidator wssDecorationAssertionValidator;
    private final AssertionValidator elementSelectingXpathValidator;

    public WssSignElementValidator( final WssSignElement wssSignElement ) {
        super(wssSignElement);
        wssDecorationAssertionValidator = new WssDecorationAssertionValidator(wssSignElement);
        elementSelectingXpathValidator = new ElementSelectingXpathValidator(wssSignElement);
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        super.validate( path, pvc, result );
        wssDecorationAssertionValidator.validate( path, pvc, result );
        elementSelectingXpathValidator.validate( path, pvc, result );
    }
}
