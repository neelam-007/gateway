package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 23/01/13
 * Time: 10:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheAssertionValidator implements AssertionValidator {

    private RemoteCacheAssertion assertion = null;

    public RemoteCacheAssertionValidator(RemoteCacheAssertion _assertion) {
        assertion = _assertion;
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {

        RemoteCacheEntityAdmin impObject =
                Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);

        String errorMessage = "Invalid remote cache configured.  " +
                "Please ensure the assertion is configured with a valid remote cache";
        PolicyValidatorResult.Error error = new PolicyValidatorResult.Error(assertion, errorMessage, null);

        RemoteCacheEntity entity = null;

        //validate the remote cacheId
        try {
            entity = impObject.find(assertion.getRemoteCacheGoid());

            if (entity == null) {
                result.addError(error);
            }

        } catch (FindException fE) {
            result.addError(error);
        }

    }
}
