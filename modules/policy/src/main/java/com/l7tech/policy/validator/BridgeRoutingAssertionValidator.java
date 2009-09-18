package com.l7tech.policy.validator;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;

/**
 * Assertion validator for Bridge Routing.
 *
 * @author $Author: steve $
 */
public class BridgeRoutingAssertionValidator implements AssertionValidator {

    //- PUBLIC

    /**
     *
     */
    public BridgeRoutingAssertionValidator(BridgeRoutingAssertion assertion) {
        this.assertion = assertion;
    }

    /**
     *
     */
    @Override
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if(path!=null && result!=null) {
            String url = assertion.getProtectedServiceUrl();

            if (url != null && url.indexOf("${") > -1) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion, path,
                        "Variables are not supported in the URL when using the Route via SecureSpan Bridge assertion.", null));
            }
        }
    }

    //- PRIVATE

    private final BridgeRoutingAssertion assertion;

}
