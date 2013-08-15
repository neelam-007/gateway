package com.l7tech.console.tree.policy;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.FindException;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for working with policy trees
 */
public class PolicyTreeUtils {

    //- PUBLIC

    /**
     * Update any info that is not stored in the policy but is useful
     * for display.
     */
    @SuppressWarnings({"unchecked"})
    public static void updateAssertions( final AssertionTreeNode atn,
                                         final Map<Goid,String> identityProviderNameMap ) {
        Assertion assertion = atn.asAssertion();
        if ( assertion instanceof IdentityTargetable ) {
            IdentityTargetable identityTargetable = (IdentityTargetable) assertion;
            if ( identityTargetable.getIdentityTarget() != null &&
                 identityTargetable.getIdentityTarget().needsIdentityProviderName() ) {
                Goid providerOid = identityTargetable.getIdentityTarget().getIdentityProviderOid();
                String name = identityProviderNameMap.get( providerOid );
                if ( name == null ) {
                    try {
                        IdentityAdmin ia = Registry.getDefault().getIdentityAdmin();
                        IdentityProviderConfig config
                                = ia.findIdentityProviderConfigByID( providerOid );
                        if ( config != null ) {
                            name = config.getName();
                            identityProviderNameMap.put( providerOid, name );
                        }
                    } catch (FindException e) {
                        logger.log( Level.WARNING, "Error loading provider name for '#"+providerOid+"'.", e);
                    } catch (IllegalStateException ise) {
                        logger.log(Level.WARNING, "Identity admin not available when loading provider name for '#"+providerOid+"'.");
                    }
                }
                if ( name != null ) {
                    identityTargetable.getIdentityTarget().setIdentityProviderName( name );
                }
            }
        }

        for ( AssertionTreeNode child : (List<AssertionTreeNode>) Collections.list(atn.children()) ) {
            updateAssertions( child, identityProviderNameMap );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyTreeUtils.class.getName() );    
}
