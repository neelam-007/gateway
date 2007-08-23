package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.service.PublishedService;

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
    public void validate(AssertionPath path, PublishedService service, PolicyValidatorResult result) {
        if(path!=null && result!=null) {
            String url = assertion.getProtectedServiceUrl();

            if (url != null && url.indexOf("${") > -1) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion, path,
                        "Variables are not supported in the URL when using SecureSpan Bridge Routing.", null));                    
            }
        }
    }

    //- PRIVATE

    private final BridgeRoutingAssertion assertion;

}
