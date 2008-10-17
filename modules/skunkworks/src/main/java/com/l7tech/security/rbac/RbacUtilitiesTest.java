/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.security.rbac;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityType;

import java.text.MessageFormat;

/**
 * @author alex
 */
public class RbacUtilitiesTest extends TestCase {
    private static final int SERVICE_OID = 1234567;
    private static final String SERVICE_NAME = "Foo Bar";
    private static final String SERVICE_URI = "/foobar";
    private static final String IPC_NAME = "Fippy";
    private static final int IPC_OID = 1234;

    /**
     * test <code>TrustedCertAdminTest</code> constructor
     */
    public RbacUtilitiesTest(String name) throws Exception {
        super(name);
    }

    public void testRoleDescription() throws Exception {
        PublishedService service = new PublishedService();
        service.setOid(SERVICE_OID);
        service.setName(SERVICE_NAME);
        service.setRoutingUri(SERVICE_URI);

        Role role = new Role();
        role.setName(MessageFormat.format("Manage {0} Service (#{1})", SERVICE_NAME, SERVICE_OID));
        role.setCachedSpecificEntity(service);
        role.setEntityType(EntityType.SERVICE);
        role.setEntityOid(Long.valueOf(SERVICE_OID));

        System.out.println(role.getDescriptiveName());

        service.setRoutingUri(null);

        System.out.println(role.getDescriptiveName());

        IdentityProviderConfig ipc = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        ipc.setName(IPC_NAME);
        ipc.setOid(IPC_OID);

        role.setName(MessageFormat.format("Manage {0} Identity Provider (#{1})", IPC_NAME, IPC_OID));
        role.setEntityOid(Long.valueOf(IPC_OID));
        role.setEntityType( EntityType.ID_PROVIDER_CONFIG);
        role.setCachedSpecificEntity(ipc);

        System.out.println(role.getDescriptiveName());
    }

    /**
     * create the <code>TestSuite</code> for the RbacUtilitiesTest <code>TestCase</code>
     */
    public static Test suite() {
        try {
            return new TestSuite(RbacUtilitiesTest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
