package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import org.junit.Before;
import org.junit.Test;

import java.text.MessageFormat;

import static org.junit.Assert.*;

/**
 * @author alex
 */
public class RoleTest {
    private static final Goid SERVICE_GOID = new Goid(0,1234567);
    private static final String SERVICE_NAME = "Foo Bar";
    private static final String SERVICE_URI = "/foobar";
    private static final String IPC_NAME = "TestIP";
    private static final int IPC_OID = 1234;
    private Role role;

    @Before
    public void setup() {
        role = new Role();
    }

    @Test
    public void testRoleDescription() throws Exception {
        PublishedService service = new PublishedService();
        service.setGoid(SERVICE_GOID);
        service.setName(SERVICE_NAME);
        service.setRoutingUri(SERVICE_URI);

        role.setName(MessageFormat.format("Manage {0} Service (#{1})", SERVICE_NAME, SERVICE_GOID));
        role.setCachedSpecificEntity(service);
        role.setEntityType(EntityType.SERVICE);
        role.setEntityGoid(SERVICE_GOID);

        System.out.println(role.getDescriptiveName());
        assertEquals("Manage service role description", "Manage Foo Bar [/foobar] Service", role.getDescriptiveName());

        service.setRoutingUri(null);

        System.out.println(role.getDescriptiveName());
        assertEquals("Manage service role description (no uri)", "Manage Foo Bar Service", role.getDescriptiveName());

        IdentityProviderConfig ipc = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        ipc.setName(IPC_NAME);
        ipc.setOid(IPC_OID);

        role.setName(MessageFormat.format("Manage {0} Identity Provider (#{1})", IPC_NAME, IPC_OID));
        role.setEntityOid((long) IPC_OID);
        role.setEntityType(EntityType.ID_PROVIDER_CONFIG);
        role.setCachedSpecificEntity(ipc);

        System.out.println(role.getDescriptiveName());
        assertEquals("Manage identity provider role description", "Manage TestIP Identity Provider", role.getDescriptiveName());
    }

    @Test
    public void addAssignedGroup() {
        final GroupBean group = createGroup(1L, "abc123");
        role.addAssignedGroup(group);
        assertEquals(1, role.getRoleAssignments().size());
        final RoleAssignment assignment = role.getRoleAssignments().iterator().next();
        assertEquals(EntityType.GROUP.getName(), assignment.getEntityType());
        assertEquals(1L, assignment.getProviderId());
        assertEquals("abc123", assignment.getIdentityId());
    }

    @Test
    public void addAssignedGroupAlreadyAssigned() {
        role.getRoleAssignments().add(new RoleAssignment(role, 1L, "abc123", EntityType.GROUP));
        assertEquals(1, role.getRoleAssignments().size());
        role.addAssignedGroup(createGroup(1L, "abc123"));
        assertEquals(1, role.getRoleAssignments().size());
    }

    @Test
    public void addAssignedUser() {
        final UserBean user = createUser(1L, "abc123");
        role.addAssignedUser(user);
        assertEquals(1, role.getRoleAssignments().size());
        final RoleAssignment assignment = role.getRoleAssignments().iterator().next();
        assertEquals(EntityType.USER.getName(), assignment.getEntityType());
        assertEquals(1L, assignment.getProviderId());
        assertEquals("abc123", assignment.getIdentityId());
    }

    @Test
    public void addNullAssignedUser() {
        role.addAssignedUser(null);
        assertTrue(role.getRoleAssignments().isEmpty());
    }

    @Test
    public void addAssignedUserAlreadyAssigned() {
        role.getRoleAssignments().add(new RoleAssignment(role, 1L, "abc123", EntityType.USER));
        assertEquals(1, role.getRoleAssignments().size());
        role.addAssignedUser(createUser(1L, "abc123"));
        assertEquals(1, role.getRoleAssignments().size());
    }

    @Test
    public void removeAssignedUser() {
        role.getRoleAssignments().add(new RoleAssignment(role, 1L, "abc123", EntityType.USER));
        assertEquals(1, role.getRoleAssignments().size());
        role.removeAssignedUser(createUser(1L, "abc123"));
        assertTrue(role.getRoleAssignments().isEmpty());
    }

    @Test
    public void removeAssignedUserNull() {
        role.getRoleAssignments().add(new RoleAssignment(role, 1L, "abc123", EntityType.USER));
        assertEquals(1, role.getRoleAssignments().size());
        role.removeAssignedUser(null);
        assertEquals(1, role.getRoleAssignments().size());
    }

    @Test
    public void removeAssignedUserNotAssigned() {
        role.getRoleAssignments().add(new RoleAssignment(role, 1L, "abc123", EntityType.USER));
        assertEquals(1, role.getRoleAssignments().size());
        role.removeAssignedUser(createUser(1L, "notassigned"));
        assertEquals(1, role.getRoleAssignments().size());
    }

    @Test
    public void removeAssignedGroup() {
        role.getRoleAssignments().add(new RoleAssignment(role, 1L, "abc123", EntityType.GROUP));
        assertEquals(1, role.getRoleAssignments().size());
        role.removeAssignedGroup(createGroup(1L, "abc123"));
        assertTrue(role.getRoleAssignments().isEmpty());
    }

    @Test
    public void removeAssignedGroupNull() {
        role.getRoleAssignments().add(new RoleAssignment(role, 1L, "abc123", EntityType.GROUP));
        assertEquals(1, role.getRoleAssignments().size());
        role.removeAssignedGroup(null);
        assertEquals(1, role.getRoleAssignments().size());
    }

    @Test
    public void removeAssignedGroupNotAssigned() {
        role.getRoleAssignments().add(new RoleAssignment(role, 1L, "abc123", EntityType.GROUP));
        assertEquals(1, role.getRoleAssignments().size());
        role.removeAssignedGroup(createGroup(1L, "notassigned"));
        assertEquals(1, role.getRoleAssignments().size());
    }

    private GroupBean createGroup(final long providerId, final String uniqueId) {
        final GroupBean group = new GroupBean();
        group.setProviderId(providerId);
        group.setUniqueIdentifier(uniqueId);
        return group;
    }

    private UserBean createUser(final long providerId, final String uniqueId) {
        final UserBean user = new UserBean();
        user.setProviderId(providerId);
        user.setUniqueIdentifier(uniqueId);
        return user;
    }
}
