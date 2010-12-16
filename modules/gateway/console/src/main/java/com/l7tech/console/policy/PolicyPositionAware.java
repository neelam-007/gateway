package com.l7tech.console.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

/**
 * Policy position awareness is useful when adding new assertions to a policy.
 */
public interface PolicyPositionAware {

    /**
     * The policy position is set when the assertion about to be added to a policy.
     *
     * <p>The policy position can be used to extract contextual information from a
     * policy (such as variables that are set before this assertion)</p>
     *
     * @param policyPosition The position to be used.
     */
    public void setPolicyPosition( final PolicyPosition policyPosition );

    /**
     * Get the policy position (or null if not available.)
     *
     * @return The policy position.
     */
    public PolicyPosition getPolicyPosition();

    /**
     * Bean representing a position in a policy.
     */
    public static final class PolicyPosition {
        private final Assertion parentAssertion;
        private final int insertPosition;

        public PolicyPosition( final Assertion parentAssertion,
                               final int insertPosition ) {
            this.insertPosition = insertPosition;
            this.parentAssertion = parentAssertion;
        }

        /**
         * The parent assertion.
         *
         * @return The assertion.
         */
        public Assertion getParentAssertion() {
            return parentAssertion;
        }
        /**
         * The index that identifies the (potential) child position.
         *
         * @return The index
         */
        public int getInsertPosition() {
            return insertPosition;
        }

        public Assertion getPreviousAssertion() {
            if ( parentAssertion instanceof CompositeAssertion ) {
                CompositeAssertion compositeAssertion = (CompositeAssertion) parentAssertion;
                java.util.List<Assertion> children = compositeAssertion.getChildren();
                //children should never be null
                if(children == null || children.isEmpty()) return parentAssertion;

                if (insertPosition == 0) return parentAssertion;
                else if(children.size() > (insertPosition-1)) return children.get(insertPosition-1);
                else return children.get(children.size() - 1);
            }

            return null;
        }


    }
}
