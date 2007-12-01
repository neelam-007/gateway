package com.l7tech.server.log;

import com.l7tech.common.log.SinkConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.HibernateEntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Provides the ability to do CRUD operations on SinkConfiguration rows in the database.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SinkManager
        extends HibernateEntityManager<SinkConfiguration, EntityHeader>
{

    private Set<SinkConfiguration> sinkConfigurations = new HashSet<SinkConfiguration>();

    protected void initDao() throws Exception {
        super.initDao();
    }

    public Class<SinkConfiguration> getImpClass() {
        return SinkConfiguration.class;
    }

    public Class<SinkConfiguration> getInterfaceClass() {
        return SinkConfiguration.class;
    }

    public String getTableName() {
        return "sink_config";
    }
}
