package com.l7tech.server.policy;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.AbstractReferenceCounted;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.ResourceUtils;

import java.io.IOException;
import java.util.*;

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
                        final Set<Goid> usedPolicyIds,
                        final Map<Goid, Integer> dependentVersions,
                        final ServerAssertion rootAssertion,
                        final Nullary<Collection<Folder>> folderPathCallback ) {
        if ( policy == null ) throw new IllegalArgumentException("policy must not be null");
        if ( policyMetadata == null ) throw new IllegalArgumentException("policyMetadata must not be null");
        if ( usedPolicyIds == null ) throw new IllegalArgumentException("usedPolicyIds must not be null");
        if ( dependentVersions == null ) throw new IllegalArgumentException("dependentVersions must not be null");
        if ( rootAssertion == null ) throw new IllegalArgumentException("rootAssertion must not be null");
        if ( folderPathCallback == null ) throw new IllegalArgumentException("folderPathCallback must not be null");

        this.serverPolicyMetadata = new Metadata(
                policy,
                buildPolicyUniqueIdentifier(policy, dependentVersions, usedPolicyIds),
                usedPolicyIds,
                folderPathCallback );
        this.policyMetadata = policyMetadata;
        this.rootAssertion = rootAssertion;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        AssertionStatus result;

        if (context != null)
            context.assertionStarting(rootAssertion);

        try {
            result = rootAssertion.checkRequest(context);
        } catch (AssertionStatusException e) {
            result = e.getAssertionStatus();
        }

        if (context != null)
            context.assertionFinished(rootAssertion, result);

        return result;
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
        ResourceUtils.closeQuietly(rootAssertion);
    }

    //- PRIVATE

    private final PolicyMetadata policyMetadata;
    private final ServerPolicyMetadata serverPolicyMetadata;
    private final ServerAssertion rootAssertion;

    private PolicyUniqueIdentifier buildPolicyUniqueIdentifier( final Policy policy,
                                                                final Map<Goid, Integer> dependentVersions,
                                                                final Set<Goid> usedPolicyIds ) {
        Map<Goid, Integer> usedPoliciesAndVersions = new HashMap<Goid,Integer>();

        for ( Goid policyGoid : usedPolicyIds ) {
            Integer usedVersion = dependentVersions.get( policyGoid );
            if ( usedVersion == null ) {
                throw new IllegalArgumentException("Missing version for policy with goid " + policyGoid);
            }
            usedPoliciesAndVersions.put( policyGoid, usedVersion );
        }

        return new PolicyUniqueIdentifier( policy.getGoid(), policy.getVersion(), usedPoliciesAndVersions );
    }

    private static final class Metadata implements ServerPolicyMetadata {
        private final Policy policy;
        private final PolicyUniqueIdentifier policyUVID;
        private final Set<Goid> usedPolicyIds;
        private final Nullary<Collection<Folder>> folderPathCallback;

        private Metadata( final Policy policy,
                          final PolicyUniqueIdentifier puvid,
                          final Set<Goid> usedPolicyIds,
                          final Nullary<Collection<Folder>> folderPathCallback ) {
            this.policy = policy;
            this.policyUVID = puvid;
            this.usedPolicyIds = usedPolicyIds;
            this.folderPathCallback = folderPathCallback;
        }

        @Override
        public String getPolicyUniqueIdentifier() {
            return policyUVID.getPolicyUniqueIdentifer();
        }
        
        @Override
        public Policy getPolicy() {
            return policy;
        }

        @Override
        public Set<Goid> getUsedPolicyIds( final boolean includeSelf ) {
            if ( !includeSelf) {
                return usedPolicyIds;
            } else {
                Set<Goid> ids = new HashSet<Goid>(usedPolicyIds);
                ids.add( getPolicy().getGoid() );
                return Collections.unmodifiableSet( ids );
            }
        }

        @Override
        public Map<Goid,Integer> getDependentVersions( final boolean includeSelf ) {
            return policyUVID.getUsedPoliciesAndVersions( includeSelf );
        }

        @Override
        public Nullary<Collection<Folder>> getFolderPath() {
            return folderPathCallback;
        }
    }
}
