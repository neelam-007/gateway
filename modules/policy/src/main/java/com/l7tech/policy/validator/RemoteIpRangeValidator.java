package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.variable.Syntax;

/**
 * AssertionValidator for RemoteIpRange
 *
 * @author rraquepo
 */
public class RemoteIpRangeValidator implements AssertionValidator {
    private final RemoteIpRange ass;
    private boolean useContextVariableInStartIp;

    public RemoteIpRangeValidator(final RemoteIpRange ass) {
        this.ass = ass;
        if (ass.getStartIp() != null) {
            useContextVariableInStartIp = Syntax.getReferencedNames(ass.getStartIp()).length > 0;
        }
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        if (useContextVariableInStartIp)
            result.addWarning(new PolicyValidatorResult.Warning(ass, "Using context variable in the IP Address Range may result in Invalid IP Address Range", null));
    }
}
