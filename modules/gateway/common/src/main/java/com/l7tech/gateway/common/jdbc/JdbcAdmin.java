package com.l7tech.gateway.common.jdbc;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.util.ConfigFactory;
import org.springframework.transaction.annotation.Transactional;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.FIND_ENTITIES;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;

import java.util.List;

/**
 * Admin interface for managing JDBC Connection Entities, JDBC Connection Pooling, and JDBC querying.
 *
 * @author ghuang
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types= EntityType.JDBC_CONNECTION)
@Administrative
public interface JdbcAdmin extends AsyncAdminMethods{

    // The original driver class list.  If you add more driver classes, separate them by '\n'.
    static final String ORIGINAL_DRIVERCLASS_LIST = "com.mysql.jdbc.Driver";

    // The original maximum number of records returned by a query
    static final int ORIGINAL_MAX_RECORDS = 10;

    // The original minimum pool size of C3P0 Basic Pool Configuration
    static final int ORIGINAL_C3P0_BASIC_POOL_CONFIG_MINPOOLSIZE = 3;

    // The original maximum pool size of C3P0 Basic Pool Configuration
    static final int ORIGINAL_C3P0_BASIC_POOL_CONFIG_MAXPOOLSIZE = 15;

    // The original maximum length of a query statement
    static final int MAX_QUERY_LENGTH = ConfigFactory.getIntProperty( "com.l7tech.jdbcquery.maxquerylength", 4 * 1024 );

    // The original upper bound of the maximum number of records returned by the query 
    static final int UPPER_BOUND_MAX_RECORDS = ConfigFactory.getIntProperty( "com.l7tech.jdbcquery.maxrecords.upperbound", 10000 );

    /**
     * Retrieve a JDBC Connection entity from the database by using a connection name.
     *
     * @param connectionName: the name of a JDBC connection
     * @return a JDBC Connection entity with the name, "connectionName".
     * @throws FindException: thrown when errors finding the JDBC Connection entity.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.JDBC_CONNECTION, stereotype= MethodStereotype.FIND_ENTITY)
    JdbcConnection getJdbcConnection(String connectionName) throws FindException;

    /**
     * Retrieve all JDBC Connection entities from the database.
     *
     * @return a list of JDBC Connection entities
     * @throws FindException: thrown when errors finding JDBC Connection entities.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.JDBC_CONNECTION, stereotype= MethodStereotype.FIND_ENTITIES)
    List<JdbcConnection> getAllJdbcConnections() throws FindException;

    /**
     * Get the names of all JDBC Connection entities.
     *
     * @return a list of the names of all JDBC Connection entities.
     * @throws FindException: thrown when errors finding JDBC Connection entities.
     */
    @Transactional(readOnly=true)
    List<String> getAllJdbcConnectionNames() throws FindException;

    /**
     * Save a JDBC Connection entity into the database.
     *
     * @param connection: the JDBC Connection entity to be saved.
     * @return a long, the saved entity object id.
     * @throws UpdateException: thrown when errors saving the JDBC Connection entity.
     */
    @Secured(types=EntityType.JDBC_CONNECTION, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    long saveJdbcConnection(JdbcConnection connection) throws UpdateException;

    /**
     * Delete a JDBC Connection entity from the database.
     *
     * @param connection: the JDBC Connection entity to be deleted.
     * @throws DeleteException: thrown when errors deleting the JDBC Connection entity.
     */
    @Secured(types=EntityType.JDBC_CONNECTION, stereotype= MethodStereotype.DELETE_ENTITY)
    void deleteJdbcConnection(JdbcConnection connection) throws DeleteException;

    /**
     * Test a JDBC Connection entity.
     *
     * @param connection: the JDBC Connection to be tested.
     * @return null if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    @Transactional(readOnly=true)
    @Secured(types = EntityType.JDBC_CONNECTION, stereotype = FIND_ENTITIES)
    AsyncAdminMethods.JobId<String> testJdbcConnection(JdbcConnection connection);

    /**
     * Test a JDBC query and see if it is a valid SQL statement.
     *
     * @param connectionName: the name of a JDBC Connection entity.
     * @param query: a SQL query statement.
     * @return null if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    @Transactional(readOnly=true)
    AsyncAdminMethods.JobId<String> testJdbcQuery(String connectionName, String query);

    /**
     * Get a property, default driver class list from the global cluster properties.  if failed to get its value,
     * then use the original driver class list defined in this interface.
     *
     * @return a list of driver classes.
     */
    @Transactional(readOnly=true)
    List<String> getPropertyDefaultDriverClassList();

    /**
     * Get a property, a list of driver classes which the JDBC Query Assertion is allowed to use. Note: these driver classes may not be supported.
     * The intersection of this list and {@link #getPropertyDefaultDriverClassList()} are the driver classes that are supported and usable by the JDBC Assertion.
     *
     * @return a white list of driver classes.
     */
    @Transactional(readOnly=true)
    List<String> getPropertySupportedDriverClass();

    /**
     * Get a property, default maximum number of records returned by a query from the global cluser properties.  If failed
     * to get its value, then use the original maximum number of records returned by a query defined in this interface.
     *
     * @return an integer, the default maximum number of records returned by a query
     */
    @Transactional(readOnly=true)
    int getPropertyDefaultMaxRecords();

    /**
     * Get a property, default minimum pool size.  If failed to get its value, then use the original minimum pool size
     * defined in this interface.
     *
     * @return an integer, the default minimum pool size.
     */
    @Transactional(readOnly=true)
    int getPropertyDefaultMinPoolSize();

    /**
     * Get a property, default maximum pool size.  If failed to get its value, then use the original maximum pool size
     * defined in this interface.
     *
     * @return an integer, the default maximum pool size.
     */
    @Transactional(readOnly=true)
    int getPropertyDefaultMaxPoolSize();

    /**
     * See if the jdbc drive Class is supported, the jdbcConnection.driverClass.whiteList under serverConfig.properties
     * defined all the supported jdbc driver class.
     *
     * @param driverClass The driver class
     * @return True if the jdbc driver class is supported, False if the jdbc driver class is not supported.
     */
    @Transactional(readOnly=true)
    boolean isDriverClassSupported(String driverClass);
}
