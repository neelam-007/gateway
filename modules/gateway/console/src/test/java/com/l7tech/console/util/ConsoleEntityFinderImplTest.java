package com.l7tech.console.util;

import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.gateway.common.admin.EncapsulatedAssertionAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConsoleEntityFinderImplTest {
    private TestableConsoleEntityFinderImpl entityFinder;
    private RegistryStub registry;
    private EncapsulatedAssertionConfig config;
    @Mock
    private EncapsulatedAssertionAdmin encapAdmin;

    @Before
    public void setup() {
        entityFinder = new TestableConsoleEntityFinderImpl();
        registry = new RegistryStub();
        registry.setAdminContextPresent(true);
        registry.setEncapsulatedAssertionAdmin(encapAdmin);
        config = new EncapsulatedAssertionConfig();
    }

    @Test
    public void findByEntityTypeAndPrimaryId() throws Exception {
        when(encapAdmin.findByPrimaryKey(1L)).thenReturn(config);
        final Entity entity = entityFinder.findByEntityTypeAndPrimaryId(EntityType.ENCAPSULATED_ASSERTION, "1");
        assertEquals(config, entity);
    }

    @Test(expected = UnsupportedEntityTypeException.class)
    public void findByEntityTypeAndPrimaryIdUnsupportedType() throws Exception {
        entityFinder.findByEntityTypeAndPrimaryId(EntityType.POLICY, "1");
    }

    @Test(expected = FindException.class)
    public void findByEntityTypeAndPrimaryIdInvalidId() throws Exception {
        entityFinder.findByEntityTypeAndPrimaryId(EntityType.ENCAPSULATED_ASSERTION, "invalid");
    }

    @Test(expected = FindException.class)
    public void findByEntityTypeAndPrimaryIdNotFound() throws Exception {
        when(encapAdmin.findByPrimaryKey(1L)).thenThrow(new FindException("mocking exception"));
        entityFinder.findByEntityTypeAndPrimaryId(EntityType.ENCAPSULATED_ASSERTION, "1");
    }

    @Test
    public void findByHeader() throws Exception {
        when(encapAdmin.findByPrimaryKey(1L)).thenReturn(config);
        final EntityHeader header = new EntityHeader("1", EntityType.ENCAPSULATED_ASSERTION, null, null);
        final Entity entity = entityFinder.find(header);
        assertEquals(config, entity);
    }

    private class TestableConsoleEntityFinderImpl extends ConsoleEntityFinderImpl {
        @NotNull
        @Override
        Registry registry() throws FindException {
            return registry;
        }
    }

}
