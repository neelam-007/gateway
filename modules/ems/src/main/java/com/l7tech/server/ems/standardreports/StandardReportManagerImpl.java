package com.l7tech.server.ems.standardreports;

import com.l7tech.server.HibernateEntityManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.User;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * 
 */
public class StandardReportManagerImpl  extends HibernateEntityManager<StandardReport, EntityHeader> implements StandardReportManager {

    //- PUBLIC

    @Override
    public Class<StandardReport> getImpClass() {
        return StandardReport.class;
    }

    @Override
    public Class<StandardReport> getInterfaceClass() {
        return StandardReport.class;
    }

    @Override
    public String getTableName() {
        return "report";
    }

    //- PRIVATE
}
