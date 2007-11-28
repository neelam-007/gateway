package com.l7tech.server.event;

import com.l7tech.common.policy.Policy;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired whenever policy XML may be changing, so the PolicyVersionManager can add a new entry to
 * the revision history if necessary.
 */
public class PolicyCheckpointEvent extends ApplicationEvent {
    private final Policy policyBeingSaved;

    /**
     * Report that a policy is being mutated, with the mutation not yet committed to the database.
     * The mutated policy will become the active version when the transaction is committed.
     *
     * @param source  the event source
     * @param policyBeingSaved the possibly-mutated version of the policy.  Required
     */
    public PolicyCheckpointEvent(Object source, Policy policyBeingSaved) {
        super(source);
        if (policyBeingSaved == null) throw new NullPointerException();
        this.policyBeingSaved = policyBeingSaved;
    }

    /**
     * @return the possibly-mutated version of the policy.  Never null.
     */
    public Policy getPolicyBeingSaved() {
        return policyBeingSaved;
    }
}
