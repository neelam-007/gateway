package com.l7tech.server.event;

import com.l7tech.common.policy.Policy;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired whenever policy XML may be changing, so the PolicyVersionManager can add a new entry to
 * the revision history if necessary.
 */
public class PolicyCheckpointEvent extends ApplicationEvent {
    private final Policy policyBeingSaved;
    private final boolean activated;

    /**
     * Report that a policy is being mutated, with the mutation not yet committed to the database.
     * The mutated policy will become the active version when the transaction is committed.
     *
     * @param source  the event source
     * @param policyBeingSaved the possibly-mutated version of the policy.  Required
     * @param activated if true, the policy being saved is also being made the active version of the policy.
     *                  if false, the policy is being saved for possible future use but not being made active.
     */
    public PolicyCheckpointEvent(Object source, Policy policyBeingSaved, boolean activated) {
        super(source);
        if (policyBeingSaved == null) throw new NullPointerException();
        if (policyBeingSaved.getOid() == Policy.DEFAULT_OID)
            throw new IllegalArgumentException("Policy must have valid OID before it can be checkpointed"); // checkoint before update, but after save
        this.policyBeingSaved = policyBeingSaved;
        this.activated = activated;
    }

    /**
     * @return the possibly-mutated version of the policy.  Never null.
     */
    public Policy getPolicyBeingSaved() {
        return policyBeingSaved;
    }

    /**
     * @return true if this policy is being made the active version.
     */
    public boolean isActivated() {
        return activated;
    }
}
