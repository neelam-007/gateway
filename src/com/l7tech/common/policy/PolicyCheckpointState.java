package com.l7tech.common.policy;

import java.io.Serializable;

/**
 * Holds information about the checkpoint state (policy OID, PolicyVersion OID and activation state) of a Policy
 * that has just been saved.
 */
public class PolicyCheckpointState implements Serializable {
    private static final long serialVersionUID = -2389027329118377434L;

    private final long policyOid;
    private final long policyVersionOrdinal;
    private final boolean policyVersionActive;

    /**
     * Create a PolicyCheckpointState to hold information about a Policy that was just saved.
     *
     * @param policyOid  the OID that got assigned to the policy that was saved.
     * @param policyVersionOrdinal the ordinal that was assigned to this version of the policy XML.
     * @param policyVersionActive true if this ordinal matches that of the currently-active version of the policy.
     */
    public PolicyCheckpointState(long policyOid, long policyVersionOrdinal, boolean policyVersionActive) {
        this.policyOid = policyOid;
        this.policyVersionOrdinal = policyVersionOrdinal;
        this.policyVersionActive = policyVersionActive;
    }

    /** @return the OID of the policy that was saved. */
    public long getPolicyOid() {
        return policyOid;
    }

    /**
     * @return the ordinal that was assigned to this version of the policy XML.
     *         This may be a preexisting ordinal if the policy XML has not changed.
     */
    public long getPolicyVersionOrdinal() {
        return policyVersionOrdinal;
    }

    /**
     * @return true if this ordinal matches that of the currently-active version of the policy.
     *         This may be true even if the policy was checkpointed without the intention of making it the
     *         active version, if the checkpointed policy XML happened to be identical to the XML of
     *         the active version.
     */
    public boolean isPolicyVersionActive() {
        return policyVersionActive;
    }
}
