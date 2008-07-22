/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.Policy;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.AbstractReferenceCounted;

import java.io.IOException;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;

/**
 * Ensures that {@link ServerAssertion#close()} can be called safely when no more traffic will arrive by
 * giving out handles that maintain a reference count, and only
 * closing the policy only when the reference count hits zero.
 */
public class ServerPolicy extends AbstractReferenceCounted<ServerPolicyHandle> {

    //- PUBLIC

    /**
     * Create a server policy
     *
     * @param policy The policy that the server policy is based on
     * @param policyMetadata The metadata for the policy
     * @param usedPolicyIds Identifiers of all used policies (dependencies)
     * @param dependentVersions Version map for the polcicy dependencies
     * @param rootAssertion The root of the server policy
     */
    public ServerPolicy(final Policy policy,
                        final PolicyMetadata policyMetadata,
                        final Set<Long> usedPolicyIds,
                        final Map<Long, Integer> dependentVersions,
                        final ServerAssertion rootAssertion ) {
        if ( policy == null ) throw new IllegalArgumentException("policy must not be null");
        if ( policyMetadata == null ) throw new IllegalArgumentException("policyMetadata must not be null");
        if ( usedPolicyIds == null ) throw new IllegalArgumentException("usedPolicyIds must not be null");
        if ( dependentVersions == null ) throw new IllegalArgumentException("dependentVersions must not be null");
        if ( rootAssertion == null ) throw new IllegalArgumentException("rootAssertion must not be null");

        this.serverPolicyMetadata = new Metadata(
                policy,
                buildPolicyUniqueIdentifier(policy, dependentVersions, usedPolicyIds),
                usedPolicyIds);
        this.policyMetadata = policyMetadata;
        this.rootAssertion = rootAssertion;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        return rootAssertion.checkRequest(context);
    }

    //- PROTECTED

    @Override
    protected ServerPolicyHandle createHandle() {
        return new ServerPolicyHandle(this, policyMetadata, serverPolicyMetadata);
    }

    /**
     * Closes this policy.  May block until all message traffic passing through this policy has concluded.
     */
    @Override
    protected void doClose() {
        rootAssertion.close();
    }

    //- PRIVATE

    private final PolicyMetadata policyMetadata;
    private final ServerPolicyMetadata serverPolicyMetadata;
    private final ServerAssertion rootAssertion;

    private PolicyUniqueIdentifier buildPolicyUniqueIdentifier( final Policy policy,
                                                                final Map<Long, Integer> dependentVersions,
                                                                final Set<Long> usedPolicyIds ) {
        Map<Long, Integer> usedPoliciesAndVersions = new HashMap<Long,Integer>();

        for ( Long policyOid : usedPolicyIds ) {
            Integer usedVersion = dependentVersions.get( policyOid );
            if ( usedVersion == null ) {
                throw new IllegalArgumentException("Missing version for policy with oid " + policyOid);
            }
            usedPoliciesAndVersions.put( policyOid, usedVersion );
        }

        return new PolicyUniqueIdentifier( policy.getOid(), policy.getVersion(), usedPoliciesAndVersions );
    }

    private static final class Metadata implements ServerPolicyMetadata {
        private final Policy policy;
        private final PolicyUniqueIdentifier policyUVID;
        private final Set<Long> usedPolicyIds;

        private Metadata( final Policy policy,
                          final PolicyUniqueIdentifier puvid,
                          final Set<Long> usedPolicyIds ) {
            this.policy = policy;
            this.policyUVID = puvid;
            this.usedPolicyIds = usedPolicyIds;
        }

        public String getPolicyUniqueIdentifier() {
            return policyUVID.getPolicyUniqueIdentifer();
        }
        
        public Policy getPolicy() {
            return policy;
        }

        public Set<Long> getUsedPolicyIds( final boolean includeSelf ) {
            if ( !includeSelf) {
                return usedPolicyIds;
            } else {
                Set<Long> ids = new HashSet<Long>(usedPolicyIds);
                ids.add( getPolicy().getOid() );
                return Collections.unmodifiableSet( ids );
            }
        }

        public Map<Long,Integer> getDependentVersions( final boolean includeSelf ) {
            return policyUVID.getUsedPoliciesAndVersions( includeSelf );
        }
    }
}
