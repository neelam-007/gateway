package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.util.Functions;

import java.util.List;

/**
 *
 */
public class MockEntityMonitoringPropertySetupManager extends EntityManagerStub<EntityMonitoringPropertySetup, EntityHeader> implements EntityMonitoringPropertySetupManager {
    public MockEntityMonitoringPropertySetupManager() {
    }

    public MockEntityMonitoringPropertySetupManager(EntityMonitoringPropertySetup... entitiesIn) {
        super(entitiesIn);
    }

    public List<EntityMonitoringPropertySetup> findByEntityGuid(final String entityGuid) throws FindException {
        return Functions.grep(findAll(), new Functions.Unary<Boolean, EntityMonitoringPropertySetup>() {
            public Boolean call(EntityMonitoringPropertySetup entityMonitoringPropertySetup) {
                return entityGuid.equals(entityMonitoringPropertySetup.getEntityGuid());
            }
        });
    }

    public EntityMonitoringPropertySetup findByEntityGuidAndPropertyType(final String entityGuid, final String propertyType) throws FindException {
        List<EntityMonitoringPropertySetup> found = Functions.grep(findAll(), new Functions.Unary<Boolean, EntityMonitoringPropertySetup>() {
            public Boolean call(EntityMonitoringPropertySetup entityMonitoringPropertySetup) {
                return entityGuid.equals(entityMonitoringPropertySetup.getEntityGuid()) &&
                       propertyType.equals(entityMonitoringPropertySetup.getPropertyType());
            }
        });
        return found.size() < 1 ? null : found.get(0);
    }

    public void deleteBySsgClusterGuid(String guid) throws FindException, DeleteException {
    }
}
