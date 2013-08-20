package com.l7tech.server.ems.standardreports;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;

import java.util.Collection;
import java.util.Collections;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 23, 2009
 */
public class MockStandardReportSettingsManager extends EntityManagerStub<StandardReportSettings, EntityHeader> implements StandardReportSettingsManager {

    public Collection<StandardReportSettings> findByUser(User user) throws FindException {
        return Collections.emptyList();
    }

    public StandardReportSettings findByPrimaryKeyForUser(User user, long oid) throws FindException {
        return super.findByPrimaryKey(oid);
    }

    public StandardReportSettings findByNameAndUser(User user, String name) throws FindException {
        return super.findByUniqueName(name);
    }
}
