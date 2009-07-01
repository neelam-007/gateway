package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.xmlsec.WssSignElement;
import com.l7tech.wsdl.Wsdl;

/**
 * Validates the <code>ResponseWssIntegrity</code> assertion internals. This validates
 * the XPath requirements and the the Key Reference for consistency if multiple
 *  <code>ResponseWssIntegrity</code>  are present in the policy path. The processing
 * model requires that the same Key Reference is used in the policy path.
 *
 * @author emil
 */
public class WssSignElementValidator extends XpathBasedAssertionValidator {
    private final WssDecorationAssertionValidator wssDecorationAssertionValidator;

    public WssSignElementValidator( final WssSignElement wssSignElement ) {
        super(wssSignElement);
        wssDecorationAssertionValidator = new WssDecorationAssertionValidator(wssSignElement);
    }

    @Override
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        super.validate( path, wsdl, soap, result );
        wssDecorationAssertionValidator.validate( path, wsdl, soap, result );
    }
}
