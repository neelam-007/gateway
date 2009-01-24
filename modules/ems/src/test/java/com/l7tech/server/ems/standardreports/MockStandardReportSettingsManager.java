package com.l7tech.server.ems.standardreports;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 23, 2009
 */
public class MockStandardReportSettingsManager extends EntityManagerStub<StandardReportSettings, EntityHeader> implements StandardReportSettingsManager {
    public void delete(String oid) throws DeleteException, FindException {
    }
}
