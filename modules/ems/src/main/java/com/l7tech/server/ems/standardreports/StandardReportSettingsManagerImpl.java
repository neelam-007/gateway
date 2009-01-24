package com.l7tech.server.ems.standardreports;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 20, 2009
 * @since Enterprise Manager 1.0
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class StandardReportSettingsManagerImpl extends HibernateEntityManager<StandardReportSettings, EntityHeader> implements StandardReportSettingsManager {

    @Override
    public Class<? extends Entity> getImpClass() {
        return StandardReportSettings.class;
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return StandardReportSettings.class;
    }

    @Override
    public String getTableName() {
        return "standard_report_settings";
    }

    @Override
    public void delete(String oid) throws DeleteException, FindException {
        findAndDelete(Integer.parseInt(oid));
    }
}
