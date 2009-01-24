package com.l7tech.server.ems.standardreports;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;

/**
 * The manager managing standard report settings.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 20, 2009
 * @since Enterprise Manager 1.0
 */
public interface StandardReportSettingsManager extends EntityManager<StandardReportSettings, EntityHeader> {

    /**
     * Deletes the settings of a standard report.
     *
     * @param oid: the OID of the standard report settings.
     */
    void delete(String oid) throws DeleteException, FindException;
}
