package com.l7tech.server.security.rbac;

import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.identity.internal.InternalGroupManager;
import com.l7tech.util.TimeUnit;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * 
 */
public abstract class RoleManagerIdentitySourceSupport implements RoleManagerIdentitySource {

    //- PUBLIC

    /**
     * Ensure that the current (perhaps not yet committed) role assignments are valid.
     *
     * <p>This check ensures that:</p>
     *
     * <li>
     *   <ul>There is an administrative assignement to at least one internal user or group.</ul>
     *   <ul>If a group is assigned that there is at least one user in that group.</ul>
     *   <ul>That the user that is assigned is not an expired account.</ul>
     * </li>
     *
     * @throws com.l7tech.objectmodel.UpdateException If there is a problem with assignments or an error while checking.
     */
    public void validateRoleAssignments() throws UpdateException {
        try {
            // Try internal first (internal accounts with the same credentials should hide externals)
            Set<IdentityProvider> providers = getAdminIdentityProviders();

            boolean found = false;
            IdentityProvider provider = !providers.isEmpty() ? providers.iterator().next() : null;
            if ( provider instanceof InternalIdentityProvider) {
                InternalIdentityProvider internalProvider = (InternalIdentityProvider) provider;
                InternalUserManager userManager = internalProvider.getUserManager();
                InternalGroupManager groupManager = internalProvider.getGroupManager();

                Role adminRole = roleManager.findByTag( Role.Tag.ADMIN );
                if ( adminRole != null ) {
                    Set<String> checkedUserOids = new HashSet<String>();

                    // Check for user assignment first
                    for ( RoleAssignment assignment : adminRole.getRoleAssignments() ) {
                        if ( assignment.getProviderId()==internalProvider.getConfig().getOid() ) {
                            if ( EntityType.USER.getName().equals(assignment.getEntityType()) ) {
                                InternalUser user = userManager.findByPrimaryKey( assignment.getIdentityId() );
                                if ( user != null && user.isEnabled() && (user.getExpiration()<0)) {
                                    found = true;
                                    break;
                                } else {
                                    checkedUserOids.add( assignment.getIdentityId() );
                                }
                            }
                        }
                    }

                    if ( !found ) {
                        // Check group assignments
                        for ( RoleAssignment assignment : adminRole.getRoleAssignments() ) {
                            if ( assignment.getProviderId()==internalProvider.getConfig().getOid() ) {
                                if ( EntityType.GROUP.getName().equals(assignment.getEntityType()) ) {
                                    InternalGroup group = groupManager.findByPrimaryKey(assignment.getIdentityId());
                                    if(!group.isEnabled())
                                        continue;
                                    Set<IdentityHeader> users = groupManager.getUserHeaders( assignment.getIdentityId() );
                                    for ( IdentityHeader userHeader : users ) {
                                        if ( checkedUserOids.contains( userHeader.getStrId() ) ) continue;

                                        InternalUser user = userManager.findByPrimaryKey( userHeader.getStrId() );
                                        if ( user != null && user.isEnabled() &&(user.getExpiration()<0 )) {
                                            found = true;
                                            break;
                                        } else {
                                            checkedUserOids.add( assignment.getIdentityId() );
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if ( !found ) {
                        throw new UpdateException( "At least one internal user with no expiration must be assigned to the administrative role (can be via group assignment)." );
                    }
                }
            }
        } catch ( FindException fe ) {
            throw new UpdateException( "Error checking role assignements.", fe );
        }
    }

    public Set<IdentityHeader> getGroups(final User user, boolean skipAccountValidation) throws FindException {
        return Collections.emptySet();
    }

    public Set<IdentityHeader> getGroups(final Group group) throws FindException {
        return Collections.emptySet();
    }

    //- PROTECTED

    protected RoleManager roleManager;

    /**
     * Access the current set of administrative identity providers.
     */
    protected abstract Set<IdentityProvider> getAdminIdentityProviders();

}
