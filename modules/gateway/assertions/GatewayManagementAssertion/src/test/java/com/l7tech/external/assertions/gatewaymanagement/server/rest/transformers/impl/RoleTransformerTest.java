package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EntityManagerResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.RbacRoleResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.RbacRoleMO;
import com.l7tech.gateway.common.security.rbac.Role;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author alee, 1/23/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class RoleTransformerTest {
    private RoleTransformer transformer;
    @Mock
    private RbacRoleResourceFactory factory;
    @Mock
    private EntityManagerResourceFactory.EntityBag<Role> entityBag;
    private Role entity;
    private RbacRoleMO mo;

    @Before
    public void setup() {
        transformer = new RoleTransformer();
        transformer.setFactory(factory);
        mo = ManagedObjectFactory.createRbacRoleMO();
        entity = new Role();
    }

    @Test
    public void convertFromMOUserCreated() throws Exception {
        mo.setUserCreated(true);
        entity.setUserCreated(false);
        setupMOToEntity(mo, entity);
        final Role result = transformer.convertFromMO(mo, false, null).getEntity();
        assertTrue(result.isUserCreated());
    }

    @Test
    public void convertFromMONotUserCreated() throws Exception {
        mo.setUserCreated(false);
        entity.setUserCreated(true);
        setupMOToEntity(mo, entity);
        final Role result = transformer.convertFromMO(mo, false, null).getEntity();
        assertFalse(result.isUserCreated());
    }

    private void setupMOToEntity(final RbacRoleMO expectedMO, final Role resultEntity) throws ResourceFactory.InvalidResourceException {
        when(factory.fromResourceAsBag(expectedMO, false)).thenReturn(entityBag);
        when(entityBag.getEntity()).thenReturn(resultEntity);
    }
}
