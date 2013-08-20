package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;

/**
 *
 */
public class MockSsgClusterNotificationSetupManager extends EntityManagerStub<SsgClusterNotificationSetup, EntityHeader> implements SsgClusterNotificationSetupManager {
    public SsgClusterNotificationSetup findByEntityGuid(String ssgClusterGuid) throws FindException {
        return null;
    }

    public void deleteBySsgClusterGuid(String guid) throws FindException, DeleteException {
    }
}
