package com.l7tech.server.cassandra;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import org.springframework.context.ApplicationListener;

/**
 * Created by yuri on 10/31/14.
 */
public interface CassandraConnectionEntityManager  extends EntityManager<CassandraConnection, EntityHeader> {
    /**
     * retrieves the cassandra entity from the dataStore by connection name
     * @param connectionName  name of the cassandra connection
     * @return CassandraConnectionEntity
     * @throws FindException
     */
    CassandraConnection getCassandraConnectionEntity(String connectionName) throws FindException;


}
