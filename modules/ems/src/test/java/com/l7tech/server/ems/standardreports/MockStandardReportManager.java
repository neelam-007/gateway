package com.l7tech.server.ems.standardreports;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.ems.enterprise.SsgCluster;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class MockStandardReportManager extends EntityManagerStub<StandardReport, EntityHeader> implements StandardReportManager {
    @Override
    public List<StandardReport> findPage(User user, String sortProperty, boolean ascending, int offset, int count) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public int findCount(User user) throws FindException {
        return 0;
    }

    @Override
    public void deleteBySsgCluster(SsgCluster ssgCluster) throws DeleteException {
    }

    @Override
    public Collection<StandardReport> findByStatus(String status) throws FindException {
        return Collections.emptyList();
    }
}
