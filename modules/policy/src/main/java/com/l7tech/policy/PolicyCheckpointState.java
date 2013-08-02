package com.l7tech.policy;

import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Holds information about the checkpoint state (policy OID, PolicyVersion OID and activation state) of a Policy
 * that has just been saved.
 */
public class PolicyCheckpointState implements Serializable {
    private static final long serialVersionUID = -362032826443260105L;

    private final Goid policyGoid;
    private final String policyGuid;
    private final long policyVersionOrdinal;
    private final boolean policyVersionActive;

    /**
     * Create a PolicyCheckpointState to hold information about a Policy that was just saved.
     *
     * @param policyGoid  the GOID that got assigned to the policy that was saved.
     * @param policyGuid the GUID for the policy that was saved.
     * @param policyVersionOrdinal the ordinal that was assigned to this version of the policy XML.
     * @param policyVersionActive true if this ordinal matches that of the currently-active version of the policy.
     */
    public PolicyCheckpointState( final Goid policyGoid,
                                  @NotNull final String policyGuid,
                                  final long policyVersionOrdinal,
                                  final boolean policyVersionActive) {
        this.policyGoid = policyGoid;
        this.policyGuid = policyGuid;
        this.policyVersionOrdinal = policyVersionOrdinal;
        this.policyVersionActive = policyVersionActive;
    }

    /** @return the GOID of the policy that was saved. */
    public Goid getPolicyGoid() {
        return policyGoid;
    }

    /** @return the GUID of the policy that was saved. */
    @NotNull
    public String getPolicyGuid() {
        return policyGuid;
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
