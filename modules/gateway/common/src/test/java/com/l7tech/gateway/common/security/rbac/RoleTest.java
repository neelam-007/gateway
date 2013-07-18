package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityType;
import static org.junit.Assert.*;
import org.junit.Test;

import java.text.MessageFormat;

/**
 * @author alex
 */
public class RoleTest {
    private static final int SERVICE_OID = 1234567;
    private static final String SERVICE_NAME = "Foo Bar";
    private static final String SERVICE_URI = "/foobar";
    private static final String IPC_NAME = "TestIP";
    private static final int IPC_OID = 1234;

    @Test
    public void testRoleDescription() throws Exception {
        PublishedService service = new PublishedService();
        service.setOid(SERVICE_OID);
        service.setName(SERVICE_NAME);
        service.setRoutingUri(SERVICE_URI);

        Role role = new Role();
        role.setName(MessageFormat.format("Manage {0} Service (#{1})", SERVICE_NAME, SERVICE_OID));
        role.setCachedSpecificEntity(service);
        role.setEntityType(EntityType.SERVICE);
        role.setEntityOid( (long) SERVICE_OID );

        System.out.println(role.getDescriptiveName());
        assertEquals("Manage service role description", "Manage Foo Bar [/foobar] Service", role.getDescriptiveName());

        service.setRoutingUri(null);

        System.out.println(role.getDescriptiveName());
        assertEquals("Manage service role description (no uri)", "Manage Foo Bar Service", role.getDescriptiveName());

        IdentityProviderConfig ipc = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        ipc.setName(IPC_NAME);
        ipc.setOid(IPC_OID);

        role.setName(MessageFormat.format("Manage {0} Identity Provider (#{1})", IPC_NAME, IPC_OID));
        role.setEntityOid( (long) IPC_OID );
        role.setEntityType( EntityType.ID_PROVIDER_CONFIG);
        role.setCachedSpecificEntity(ipc);

        System.out.println(role.getDescriptiveName());
        assertEquals("Manage identity provider role description", "Manage TestIP Identity Provider", role.getDescriptiveName());
    }

    @Test
    public void addAssignedGroup() {
        final GroupBean group = new GroupBean();
        group.setProviderId(1L);
        group.setUniqueIdentifier("abc123");
        final Role role = new Role();
        role.addAssignedGroup(group);
        assertEquals(1, role.getRoleAssignments().size());
        final RoleAssignment assignment = role.getRoleAssignments().iterator().next();
        assertEquals(EntityType.GROUP.getName(), assignment.getEntityType());
        assertEquals(1L, assignment.getProviderId());
        assertEquals("abc123", assignment.getIdentityId());
    }
}
