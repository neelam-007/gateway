package com.l7tech.external.assertions.adaptiveloadbalancing;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 9/6/12
 */
public abstract class AbstractAdaptiveLoadBalancing extends Assertion {
    public static final String DEFAULT_STRATEGY = "strategy";

    private String strategy = DEFAULT_STRATEGY;

    /**
     * The strategy variable name which stored the strategy
     * @return
     */
    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    /**
     * abstract class that validates adaptive load balancing assertions
     */
    public static abstract class AbstractValidator implements AssertionValidator {

        protected final AbstractAdaptiveLoadBalancing assertion;

        public AbstractValidator(AbstractAdaptiveLoadBalancing assertion) {
            this.assertion = assertion;
        }
        /**
         * Validate the assertion in the given path, service and store the result in the
         * validator result.
         *
         * @param path   the assertion path where the assertion is located
         * @param pvc    information about the context in which the assertion appears
         * @param result the result where the validation warnings or errors are collected
         */
        @Override
        public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
            int foundPrecedingAssertion = -1;
            Map<Integer, AbstractAdaptiveLoadBalancing> precedingAssertionMap = new HashMap<Integer, AbstractAdaptiveLoadBalancing>();
            Assertion[] assertions = path.getPath();
            for(int i=0; i < assertions.length; i++) {
                Assertion ass = assertions[i];
                if(ass.isEnabled()) {
                    if (ass == assertion) {
                        if (foundPrecedingAssertion == -1 || foundPrecedingAssertion > i) {
                            result.addError(new PolicyValidatorResult.Error(assertion, getValidationErrorMsg(), null));

                        }
                        else {
                            boolean foundStrategy = false;
                            for(Map.Entry<Integer,AbstractAdaptiveLoadBalancing> entry: precedingAssertionMap.entrySet()) {
                                if(assertion.getStrategy().equals(entry.getValue().getStrategy()) && i > entry.getKey()) {
                                    foundStrategy = true;
                                    checkVariables(result, entry.getValue());
                                    break;
                                }
                            }
                            if(!foundStrategy) {
                                result.addError(new PolicyValidatorResult.Error(assertion, "Strategy " + assertion.getStrategy() + " not found", null));
                            }
                        }
                        return;
                    }
                    else if(ass instanceof AbstractAdaptiveLoadBalancing && checkDependency(ass)) {
                        foundPrecedingAssertion = i;
                        precedingAssertionMap.put(i, (AbstractAdaptiveLoadBalancing) ass);

                    }
                }
            }
        }

        protected void checkVariables(PolicyValidatorResult result, AbstractAdaptiveLoadBalancing ass) {
            //nothing
        }

        public abstract boolean checkDependency(Assertion assertion);

        public abstract String getValidationErrorMsg();

    }

}
