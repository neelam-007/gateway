package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.identity.User;

import java.util.Date;
import java.util.Collection;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Dec 10, 2008
 */
public class MockMigrationRecordManager extends EntityManagerStub<MigrationRecord, EntityHeader> implements MigrationRecordManager {
    public MigrationRecord create(String name, User user, SsgCluster source, SsgCluster destination, String summary, byte[] data) throws SaveException {
        return null;
    }

    public int findCount(User user, Date start, Date end) throws FindException {
        return 0;
    }

    public Collection<MigrationRecord> findPage(User user, SortProperty sortProperty, boolean ascending, int offset, int count, Date start, Date end) throws FindException {
        return null;
    }

    public void deleteBySsgCluster(SsgCluster ssgCluster) throws DeleteException {
    }
}
