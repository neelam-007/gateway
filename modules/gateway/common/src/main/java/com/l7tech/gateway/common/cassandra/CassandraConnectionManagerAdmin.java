package com.l7tech.gateway.common.cassandra;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Created by yuri on 10/31/14.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types = EntityType.CASSANDRA_CONFIGURATION)
@Administrative
public interface CassandraConnectionManagerAdmin extends AsyncAdminMethods {
    /**
     * Retrieve a Cassandra Connection entity from the database by using a connection name.
     *
     * @param connectionName: the name of a Cassandra connection
     * @return a Cassandra Connection entity with the name, "connectionName".
     * @throws FindException: thrown when errors finding the Cassandra Connection entity.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CASSANDRA_CONFIGURATION, stereotype= MethodStereotype.FIND_ENTITY)
    CassandraConnection getCassandraConnection(String connectionName) throws FindException;

    /**
     * Retrieve all Cassandra Connection entities from the database.
     *
     * @return a list of Cassandra Connection entities
     * @throws FindException: thrown when errors finding Cassandra Connection entities.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CASSANDRA_CONFIGURATION, stereotype= MethodStereotype.FIND_ENTITIES)
    List<CassandraConnection> getAllCassandraConnections() throws FindException;

    /**
     * Get the names of all Cassandra Connection entities.
     *
     * @return a list of the names of all Cassandra Connection entities.
     * @throws FindException: thrown when errors finding Cassandra Connection entities.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    List<String> getAllCassandraConnectionNames() throws FindException;

    /**
     * Save a Cassandra Connection entity into the database.
     *
     * @param connection: the Cassandra Connection entity to be saved.
     * @return a long, the saved entity object id.
     * @throws UpdateException: thrown when errors saving the Cassandra Connection entity.
     */
    @Secured(types=EntityType.CASSANDRA_CONFIGURATION, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    Goid saveCassandraConnection(CassandraConnection connection) throws UpdateException, SaveException;

    /**
     * Delete a Cassandra Connection entity from the database.
     *
     * @param connection: the Cassandra Connection entity to be deleted.
     * @throws DeleteException: thrown when errors deleting the Cassandra Connection entity.
     */
    @Secured(types=EntityType.CASSANDRA_CONFIGURATION, stereotype= MethodStereotype.DELETE_ENTITY)
    void deleteCassandraConnection(CassandraConnection connection) throws DeleteException;

    /**
     * Test a Cassandra Connection entity.
     *
     * @param connection: the Cassandra Connection to be tested.
     * @return null if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    @Transactional(readOnly=true)
    @Secured(types = EntityType.CASSANDRA_CONFIGURATION, stereotype = MethodStereotype.TEST_CONFIGURATION)
    AsyncAdminMethods.JobId<String> testCassandraConnection(CassandraConnection connection);

    /**
     * Test a Cassandra query and see if it is a valid SQL statement.
     *
     * @param connectionName: the name of a Cassandra Connection entity.
     * @param query: a SQL query statement.
     * @param queryTimeout maximum query execution time in seconds.
     * @return null if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    @Transactional(readOnly=true)
    @Secured(types = EntityType.CASSANDRA_CONFIGURATION, stereotype = MethodStereotype.TEST_CONFIGURATION)
    AsyncAdminMethods.JobId<String> testCassandraQuery(String connectionName, String query, int queryTimeout);
}
