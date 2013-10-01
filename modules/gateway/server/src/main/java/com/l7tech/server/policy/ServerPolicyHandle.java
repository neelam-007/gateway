package com.l7tech.server.policy;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import static com.l7tech.objectmodel.EntityUtil.id;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.util.Handle;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Eithers;
import com.l7tech.util.Eithers.E2;
import com.l7tech.util.Functions.Nullary;
import static com.l7tech.util.Functions.map;
import static java.util.Collections.singleton;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Handle pointing at a ServerPolicy instance.
 */
public class ServerPolicyHandle extends Handle<ServerPolicy> {

    //- PUBLIC

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws PolicyAssertionException, IOException {
        final ServerPolicy target = getTarget();
        if (target == null) throw new IllegalStateException("ServerPolicyHandle has already been closed");

        return Eithers.extract2( HybridDiagnosticContext.doInContext(
                getContext(),
                new Nullary<E2<PolicyAssertionException, IOException, AssertionStatus>>() {
                    @Override
                    public E2<PolicyAssertionException, IOException, AssertionStatus> call() {
                        final PolicyMetadata prev = context == null ? null : context.getCurrentPolicyMetadata();
                        try {
                            if ( context != null ) context.setCurrentPolicyMetadata( policyMetadata );
                            return Eithers.right2( target.checkRequest( context ) );
                        } catch ( PolicyAssertionException e ) {
                            return Eithers.left2_1( e );
                        } catch ( IOException e ) {
                            return Eithers.left2_2( e );
                        } finally {
                            if ( context != null ) context.setCurrentPolicyMetadata( prev );
                        }
                    }
                } ) );
    }

    public Assertion getPolicyAssertion() throws IOException {
        final ServerPolicy target = getTarget();
        if (target == null) throw new IllegalStateException("ServerPolicyHandle has already been closed");
        return target.getPolicyAssertion();
    }

    public PolicyMetadata getPolicyMetadata() {
        return policyMetadata;
    }

    //- PACKAGE

    ServerPolicyHandle( final ServerPolicy cs,
                        final PolicyMetadata policyMetadata,
                        final ServerPolicyMetadata metadata ) {
        super(cs);
        this.policyMetadata = policyMetadata;
        this.metadata = metadata;
    }

    ServerPolicyMetadata getMetadata() {
        return metadata;
    }

    //- PRIVATE

    private final PolicyMetadata policyMetadata;
    private final ServerPolicyMetadata metadata;

    private Map<String,Collection<String>> getContext() {
        return CollectionUtils.<String,Collection<String>>mapBuilder()
                .put( GatewayDiagnosticContextKeys.POLICY_ID, singleton(policyMetadata.getPolicyHeader().getStrId()) )
                .put( GatewayDiagnosticContextKeys.FOLDER_ID, map( metadata.getFolderPath().call(), id() ) )
                .map();
    }
}
