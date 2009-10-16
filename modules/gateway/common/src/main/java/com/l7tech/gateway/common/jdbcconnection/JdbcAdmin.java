package com.l7tech.gateway.common.jdbcconnection;

import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.util.SyspropUtil;

import java.util.List;

/**
 * Admin interface for managing JDBC connections, Connection Pooling, and JDBC querying.
 *
 * @author ghuang
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types= EntityType.JDBC_CONNECTION)
@Administrative
public interface JdbcAdmin {

    static final String ORIGINAL_DRIVERCLASS_LIST = "com.mysql.jdbc.Driver"; // If you add more driver classes, separate them by '\n'.
    static final int ORIGINAL_MAX_RECORDS = 10;
    static final int ORIGINAL_C3P0_BASIC_POOL_CONFIG_MINPOOLSIZE = 3;
    static final int ORIGINAL_C3P0_BASIC_POOL_CONFIG_MAXPOOLSIZE = 15;
    static final int MAX_QUERY_LENGTH = SyspropUtil.getInteger("com.l7tech.jdbcquery.querylength", 4*1024);
    static final int UPPER_BOUND_MAX_RECORDS = SyspropUtil.getInteger("com.l7tech.jdbcquery.maxrecords.upperbound", 10000);

    @Transactional(readOnly=true)
    @Secured(types=EntityType.JDBC_CONNECTION, stereotype= MethodStereotype.FIND_ENTITY)
    JdbcConnection getJdbcConnection(String connectionName) throws FindException;

    @Transactional(readOnly=true)
    @Secured(types=EntityType.JDBC_CONNECTION, stereotype= MethodStereotype.FIND_ENTITIES)
    List<JdbcConnection> getAllJdbcConnections() throws FindException;

    @Transactional(readOnly=true)
    List<String> getAllJdbcConnectionNames() throws FindException;

    @Secured(types=EntityType.JDBC_CONNECTION, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    long saveJdbcConnection(JdbcConnection connection) throws UpdateException;

    @Secured(types=EntityType.JDBC_CONNECTION, stereotype= MethodStereotype.DELETE_ENTITY)
    void deleteJdbcConnection(JdbcConnection connection) throws DeleteException;

    @Transactional(readOnly=true)
    String testJdbcConnection(JdbcConnection connection);

    @Transactional(readOnly=true)
    Object performJdbcQuery(String connectionName, String query, int maxRecords, List<Object> preparedStmtParams);

    @Transactional(readOnly=true)
    String testJdbcQuery(String connectionName, String query);

    List<String> getPropertyDefaultDriverClassList();
    int getPropertyDefaultMaxRecords();
    int getPropertyDefaultMinPoolSize();

    int getPropertyDefaultMaxPoolSize();
}
