package com.l7tech.server.ems.migration;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * @author ghuang
 */
public class MockMigrationRecordManager extends EntityManagerStub<MigrationRecord, EntityHeader> implements MigrationRecordManager {

    @Override
    public MigrationRecord create( final User user, final String label, final byte[] data, final Functions.TernaryThrows<Pair<SsgCluster, SsgCluster>, String, String, String, SaveException> clusterCallback ) throws SaveException {
        return null;
    }

    @Override
    public MigrationRecord create(String name, User user, SsgCluster sourceCluster, SsgCluster targetCluster, MigrationSummary summary, MigrationBundle bundle) throws SaveException {
        return null;
    }

    @Override
    public int findCount(User user, Date start, Date end) throws FindException {
        return 0;
    }

    @Override
    public MigrationRecord findByPrimaryKeyNoBundle(Goid goid) throws FindException {
        return null;
    }

    @Override
    public Collection<MigrationRecord> findPage(User user, SortProperty sortProperty, boolean ascending, int offset, int count, Date start, Date end) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public Collection<MigrationRecord> findNamedMigrations(User user, int count, Date start, Date end) {
        return Collections.emptyList();
    }

    @Override
    public void deleteBySsgCluster(SsgCluster ssgCluster) throws DeleteException {
    }
}
