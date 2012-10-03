package com.l7tech.external.assertions.apiportalintegration.console;

import com.l7tech.external.assertions.apiportalintegration.server.upgrade.*;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePortalActionTest {
    @Mock
    private UpgradePortalAdmin admin;
    private List<UpgradedEntity> upgradedServices;
    private List<UpgradedEntity> upgradedKeys;

    @Before
    public void setup() {
        UpgradePortalAction.DEFAULT_WAIT = 0;
        upgradedServices = new ArrayList<UpgradedEntity>();
        upgradedKeys = new ArrayList<UpgradedEntity>();
    }

    @Test
    public void doUpgrade() {
        upgradedServices.add(new UpgradedEntity("1", UpgradedEntity.API, "API description"));
        upgradedKeys.add(new UpgradedEntity("2", UpgradedEntity.KEY, "key description"));
        when(admin.upgradeServicesTo2_1()).thenReturn(upgradedServices);
        when(admin.upgradeKeysTo2_1()).thenReturn(upgradedKeys);

        final Pair<List<UpgradedEntity>, String> result = UpgradePortalAction.doUpgrade(admin);

        assertEquals(2, result.getKey().size());
        assertEquals(UpgradedEntity.API, result.getKey().get(0).getType());
        assertEquals(UpgradedEntity.KEY, result.getKey().get(1).getType());
        assertNull(result.getValue());
        verify(admin).deleteUnusedClusterProperties();
    }

    @Test
    public void doUpgradeErrorUpgradingApis() {
        when(admin.upgradeServicesTo2_1()).thenThrow(new UpgradeServiceException("mocking exception"));

        final Pair<List<UpgradedEntity>, String> result = UpgradePortalAction.doUpgrade(admin);

        assertTrue(result.getKey().isEmpty());
        assertEquals(UpgradePortalAction.API_ERROR, result.getValue());
        verify(admin, never()).upgradeKeysTo2_1();
        verify(admin, never()).deleteUnusedClusterProperties();
    }

    @Test
    public void doUpgradeErrorUpgradingKeys() {
        upgradedServices.add(new UpgradedEntity("1", UpgradedEntity.API, "API description"));
        when(admin.upgradeServicesTo2_1()).thenReturn(upgradedServices);
        when(admin.upgradeKeysTo2_1()).thenThrow(new UpgradeKeyException("mocking exception"));

        final Pair<List<UpgradedEntity>, String> result = UpgradePortalAction.doUpgrade(admin);

        assertEquals(1, result.getKey().size());
        assertEquals(UpgradedEntity.API, result.getKey().get(0).getType());
        assertEquals(UpgradePortalAction.KEY_ERROR, result.getValue());
        verify(admin, never()).deleteUnusedClusterProperties();
    }

    @Test
    public void doUpgradeErrorDeletingClusterProperties() {
        upgradedServices.add(new UpgradedEntity("1", UpgradedEntity.API, "API description"));
        upgradedKeys.add(new UpgradedEntity("2", UpgradedEntity.KEY, "key description"));
        when(admin.upgradeServicesTo2_1()).thenReturn(upgradedServices);
        when(admin.upgradeKeysTo2_1()).thenReturn(upgradedKeys);
        doThrow(new UpgradeClusterPropertyException("mocking exception")).when(admin).deleteUnusedClusterProperties();

        final Pair<List<UpgradedEntity>, String> result = UpgradePortalAction.doUpgrade(admin);

        assertEquals(2, result.getKey().size());
        assertEquals(UpgradedEntity.API, result.getKey().get(0).getType());
        assertEquals(UpgradedEntity.KEY, result.getKey().get(1).getType());
        // error should not be shown on dialog
        assertNull(result.getValue());
        verify(admin).deleteUnusedClusterProperties();
    }
}
