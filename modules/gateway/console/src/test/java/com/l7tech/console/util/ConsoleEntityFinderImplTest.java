package com.l7tech.console.util;

import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.gateway.common.admin.EncapsulatedAssertionAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConsoleEntityFinderImplTest {
    private final String CONFIG_GUID = UUID.randomUUID().toString();
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
        config.setGuid(CONFIG_GUID);
    }

    @Test
    public void findByEntityTypeAndPrimaryId() throws Exception {
        when(encapAdmin.findByGuid(CONFIG_GUID)).thenReturn(config);
        final Entity entity = entityFinder.findByEntityTypeAndGuid(EntityType.ENCAPSULATED_ASSERTION, CONFIG_GUID);
        assertEquals(config, entity);
    }

    @Test(expected = UnsupportedEntityTypeException.class)
    public void findByEntityTypeAndPrimaryIdUnsupportedType() throws Exception {
        entityFinder.findByEntityTypeAndGuid(EntityType.POLICY, "1");
    }

    @Test(expected = FindException.class)
    public void findByEntityTypeAndPrimaryIdInvalidId() throws Exception {
        when(encapAdmin.findByGuid(anyString())).thenThrow(new FindException("not found"));
        entityFinder.findByEntityTypeAndGuid(EntityType.ENCAPSULATED_ASSERTION, "invalid");
    }

    @Test(expected = FindException.class)
    public void findByEntityTypeAndPrimaryIdNotFound() throws Exception {
        when(encapAdmin.findByGuid(anyString())).thenThrow(new FindException("mocking exception"));
        entityFinder.findByEntityTypeAndGuid(EntityType.ENCAPSULATED_ASSERTION, "1");
    }

    @Test
    public void findByGuidHeader() throws Exception {
        when(encapAdmin.findByGuid(CONFIG_GUID)).thenReturn(config);
        final GuidEntityHeader header = new GuidEntityHeader("1", EntityType.ENCAPSULATED_ASSERTION, null, null);
        header.setGuid(CONFIG_GUID);
        final Entity entity = entityFinder.find(header);
        assertEquals(config, entity);
    }

    @Test(expected = UnsupportedEntityTypeException.class)
    public void findByRegularHeader() throws Exception {
        when(encapAdmin.findByPrimaryKey(new Goid(0,1L))).thenReturn(config);
        final EntityHeader header = new EntityHeader("1", EntityType.ENCAPSULATED_ASSERTION, null, null);
        entityFinder.find(header);
    }

    private class TestableConsoleEntityFinderImpl extends ConsoleEntityFinderImpl {
        @NotNull
        @Override
        Registry registry() throws FindException {
            return registry;
        }

        @Override
        void attachPolicies(@NotNull EncapsulatedAssertionConfig found) throws FindException {
            // no action required for this test
        }
    }

}
