package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.RequestIdentityTargetable;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.IdentityTagable;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Functions;

import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;

/**
 * Policy validator for IdentityTargetable assertions.
 */
public class IdentityTargetableAssertionValidator implements AssertionValidator {

    //- PUBLIC

    public IdentityTargetableAssertionValidator( final Assertion assertion ) {
        this.assertion = assertion;
        this.warning = getValidationWarning(assertion);
    }

    @Override
    public void validate( final AssertionPath assertionPath,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        if ( warning != null ) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, assertionPath, warning, null));
        }
    }

    //- PRIVATE

    private final Assertion assertion;
    private final String warning;

    private String getValidationWarning( final Assertion a ) {
        String warning = null;

        if (a instanceof IdentityTargetable && ((IdentityTargetable)a).getIdentityTarget()!=null ) {
            MessageTargetable target = new MessageTargetableSupport();
            if ( a instanceof RequestIdentityTargetable ) {
                // request is he target
            } else if ( a instanceof MessageTargetable ) {
                target = (MessageTargetable) a;
            } else if ( Assertion.isResponse( a ) ) {
                target.setTarget( TargetMessageType.RESPONSE);
            }
            if ( !(a instanceof RequestIdentityTargetable) && !hasFlag(a, ValidatorFlag.REQUIRE_SIGNATURE) ) {
                warning = "Assertion targets an identity, but signing is not required. The \"Target Identity\" should be cleared, or the assertion should require a signature.";
            } else if ( !ArrayUtils.contains( getIdentityTargetOptions(a.getPath()[0], a, target), ((IdentityTargetable)a).getIdentityTarget() ) ) {
                IdentityTarget identityTarget = ((IdentityTargetable)a).getIdentityTarget();
                if ( identityTarget.getTargetIdentityType() == IdentityTarget.TargetIdentityType.TAG ) {
                    warning = "Assertion targets an invalid identity, so will always fail. The \"Target Identity\" should be corrected.";
                } else {
                    warning = "Assertion targets an identity not explicitly required by the policy, so may fail. The \"Target Identity\" should be corrected.";
                }
            }
        }

        return warning;
    }

    /**
     * Check if the assertions validation metadata contains the given flag
     */
    private boolean hasFlag(final Assertion a, final ValidatorFlag flag) {
        boolean flagged = false;

        Functions.Unary<Set<ValidatorFlag>,Assertion> flagAccessor =
             a.meta().get( AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY );

        if ( flagAccessor != null ) {
            Set<ValidatorFlag> flags = flagAccessor.call(a);
            flagged = flags!=null && flags.contains(flag);
        }

        return flagged;
    }

    /**
     * Find options for the identity that preceed the given assertion in the
     * policy for the same target message.
     */
    private static IdentityTarget[] getIdentityTargetOptions( final Assertion policy,
                                                              final Assertion identityTargetableAssertion,
                                                              final MessageTargetable messageTargetable ) {
        final TreeSet<IdentityTarget> targetOptions = new TreeSet<IdentityTarget>();
        final Iterator<Assertion> assertionIterator = policy.preorderIterator();

        while( assertionIterator.hasNext() ){
            Assertion assertion = assertionIterator.next();
            if ( assertion == identityTargetableAssertion ) {
                break;
            }

            if ( !assertion.isEnabled() ) {
                continue;
            }

            if ( assertion instanceof IdentityAssertion &&
                 AssertionUtils.isSameTargetMessage( assertion, messageTargetable ) ) {
                targetOptions.add( ((IdentityAssertion)assertion).getIdentityTarget() );
            }

            if ( assertion instanceof IdentityTagable &&
                 AssertionUtils.isSameTargetMessage( assertion, messageTargetable ) &&
                 ((IdentityTagable)assertion).getIdentityTag() != null ) {
                targetOptions.add( new IdentityTarget(((IdentityTagable)assertion).getIdentityTag()) );
            }
        }

        return targetOptions.toArray(new IdentityTarget[targetOptions.size()]);
    }

}
