package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.EntityFinderStub;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RbacServicesTest {

    //- PUBLIC

    @Test
    public void testPermittedForEntity() throws Exception {
        final RbacServices rbacServices = new RbacServicesImpl(roleManager , entityFinder );

        final PublishedService service = new PublishedService();
        service.setGoid(new Goid(0,123));
        service.setName( "Test Service" );

        final boolean permitted1 = rbacServices.isPermittedForEntity( new UserBean("none"), service, OperationType.READ, null );
        assertFalse("No permissions", permitted1);

        final boolean permitted2 = rbacServices.isPermittedForEntity( new UserBean("any"), service, OperationType.READ, null );
        assertTrue("Any permission", permitted2);

        final boolean permitted3 = rbacServices.isPermittedForEntity( new UserBean("some"), service, OperationType.READ, null );
        assertTrue("Some permission", permitted3);

        final boolean permitted4 = rbacServices.isPermittedForEntity( new UserBean("some-other"), service, OperationType.READ, null );
        assertFalse("Some other entity permission", permitted4);

        final boolean permitted5 = rbacServices.isPermittedForSomeEntityOfType( new UserBean("other-ent"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other entity type permission", permitted5);

        final boolean permitted6 = rbacServices.isPermittedForSomeEntityOfType( new UserBean("other-perm"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other operation permission", permitted6);
    }

    @Test
    public void testPermittedForAnyEntity() throws Exception {
        final RbacServices rbacServices = new RbacServicesImpl(roleManager , entityFinder );

        final boolean permitted1 = rbacServices.isPermittedForAnyEntityOfType( new UserBean("none"), OperationType.READ, EntityType.SERVICE );
        assertFalse("No permissions", permitted1);

        final boolean permitted2 = rbacServices.isPermittedForAnyEntityOfType( new UserBean("any"), OperationType.READ, EntityType.SERVICE );
        assertTrue("Any permission", permitted2);

        final boolean permitted3 = rbacServices.isPermittedForAnyEntityOfType( new UserBean("some"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Some permission", permitted3);

        final boolean permitted4 = rbacServices.isPermittedForSomeEntityOfType( new UserBean("other-ent"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other entity permission", permitted4);

        final boolean permitted5 = rbacServices.isPermittedForSomeEntityOfType( new UserBean("other-perm"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other operation permission", permitted5);
    }

    @Test
    public void testPermittedForSomeEntity() throws Exception {
        final RbacServices rbacServices = new RbacServicesImpl(roleManager , entityFinder );

        final boolean permitted1 = rbacServices.isPermittedForSomeEntityOfType( new UserBean("none"), OperationType.READ, EntityType.SERVICE );
        assertFalse("No permissions", permitted1);

        final boolean permitted2 = rbacServices.isPermittedForSomeEntityOfType( new UserBean("any"), OperationType.READ, EntityType.SERVICE );
        assertTrue("Any permission", permitted2);

        final boolean permitted3 = rbacServices.isPermittedForSomeEntityOfType( new UserBean("some"), OperationType.READ, EntityType.SERVICE );
        assertTrue("Some permission", permitted3);

        final boolean permitted4 = rbacServices.isPermittedForSomeEntityOfType( new UserBean("other-ent"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other entity permission", permitted4);

        final boolean permitted5 = rbacServices.isPermittedForSomeEntityOfType( new UserBean("other-perm"), OperationType.READ, EntityType.SERVICE );
        assertFalse("Other operation permission", permitted5);
    }

    //- PRIVATE

    private final EntityFinder entityFinder = new EntityFinderStub();
    private final RoleManager roleManager = new MockRoleManager( entityFinder ){
            @Override
            public Collection<Role> getAssignedRoles( final User user ) throws FindException {
                if ( "some".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<Role>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.READ, EntityType.SERVICE, new Goid(0,123).toHexString() );
                    roles.add( role );
                    return roles;
                } else if ( "some-other".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<Role>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.READ, EntityType.SERVICE, new Goid(0,456).toHexString() );
                    roles.add( role );
                    return roles;
                } else if ( "any".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<Role>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.READ, EntityType.SERVICE, null );
                    roles.add( role );
                    return roles;
                } else if ( "other-ent".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<Role>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.READ, EntityType.POLICY, null );
                    roles.add( role );
                    return roles;
                } else if ( "other-perm".equals( user.getLogin() ) ) {
                    Collection<Role> roles = new ArrayList<Role>();
                    Role role = new Role();
                    role.addEntityPermission( OperationType.UPDATE, EntityType.SERVICE, null );
                    roles.add( role );
                    return roles;
                } else { // none
                    return Collections.emptyList();
                }
            }
        };
}
