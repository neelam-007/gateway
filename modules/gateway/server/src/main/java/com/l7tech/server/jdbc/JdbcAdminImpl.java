package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.jdbc.InvalidPropertyException;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.util.Background;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

import static com.l7tech.server.event.AdminInfo.find;

/**
 * The implementation of the interface JdbcAdmin to manage JDBC Connection Entities, JDBC Connection Pooling, and JDBC Querying.
 */
public class JdbcAdminImpl extends AsyncAdminMethodsImpl implements JdbcAdmin {
    private JdbcConnectionManager jdbcConnectionManager;
    private JdbcQueryingManager jdbcQueryingManager;
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private Config config;

    public JdbcAdminImpl(JdbcConnectionManager jdbcConnectionManager,
                         JdbcQueryingManager jdbcQueryingManager,
                         JdbcConnectionPoolManager jdbcConnectionPoolManager,
                         Config config) {
        this.jdbcConnectionManager = jdbcConnectionManager;
        this.jdbcQueryingManager = jdbcQueryingManager;
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
        this.config = config;
    }

    /**
     * Retrieve a JDBC Connection entity from the database by using a connection name.
     *
     * @param connectionName: the name of a JDBC connection
     * @return a JDBC Connection entity with the name, "connectionName".
     * @throws FindException: thrown when errors finding the JDBC Connection entity.
     */
    @Override
    public JdbcConnection getJdbcConnection(String connectionName) throws FindException {
        return jdbcConnectionManager.getJdbcConnection(connectionName);
    }

    /**
     * Retrieve all JDBC Connection entities from the database.
     *
     * @return a list of JDBC Connection entities
     * @throws FindException: thrown when errors finding JDBC Connection entities.
     */
    @Override
    public List<JdbcConnection> getAllJdbcConnections() throws FindException {
        List<JdbcConnection> connections = new ArrayList<JdbcConnection>();
        connections.addAll(jdbcConnectionManager.findAll());
        return connections;
    }

    /**
     * Get the names of all JDBC Connection entities.
     *
     * @return a list of the names of all JDBC Connection entities.
     * @throws FindException: thrown when errors finding JDBC Connection entities.
     */
    @Override
    public List<String> getAllJdbcConnectionNames() throws FindException {
        List<JdbcConnection> connList = getAllJdbcConnections();
        List<String> nameList = new ArrayList<String>(connList.size());
        for (JdbcConnection conn : connList) {
            nameList.add(conn.getName());
        }
        return nameList;
    }

    /**
     * Save a JDBC Connection entity into the database.
     *
     * @param connection: the JDBC Connection entity to be saved.
     * @return a long, the saved entity object id.
     * @throws UpdateException: thrown when errors saving the JDBC Connection entity.
     */
    @Override
    public long saveJdbcConnection(JdbcConnection connection) throws UpdateException {
        jdbcConnectionManager.update(connection);
        return connection.getOid();
    }

    /**
     * Delete a JDBC Connection entity from the database.
     *
     * @param connection: the JDBC Connection entity to be deleted.
     * @throws DeleteException: thrown when errors deleting the JDBC Connection entity.
     */
    @Override
    public void deleteJdbcConnection(JdbcConnection connection) throws DeleteException {
        jdbcConnectionManager.delete(connection);
    }

    /**
     * Test a JDBC Connection entity.
     *
     * @param connection: the JDBC Connection to be tested.
     * @return empty string. Never null. Empty if the testing is successful, otherwise it will contain an error message.
     */
    @Override
    public AsyncAdminMethods.JobId<String> testJdbcConnection(final JdbcConnection connection) {
        final FutureTask<String> connectTask = new FutureTask<String>(find(false).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {

                final ComboPooledDataSource cpds;
                try {
                    cpds = jdbcConnectionPoolManager.getTestConnectionPool(connection);
                } catch (InvalidPropertyException e) {
                    return e.getMessage();
                } catch (RuntimeException re) {
                    final String msg = "Unexpected problem testing connection.";
                    logger.log(Level.WARNING, msg + " " + ExceptionUtils.getMessage(re), ExceptionUtils.getDebugException(re));
                    return msg;
                }

                Connection conn = null;
                try {
                    conn = cpds.getConnection();
                } catch (SQLException e) {
                    return "invalid connection properties setting. \n" + ExceptionUtils.getMessage(e);
                } catch (Throwable e) {
                    return "unexpected error, " + e.getClass().getSimpleName() + " thrown";
                } finally {
                    ResourceUtils.closeQuietly(conn);
                    cpds.close();
                }
                return "";
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                connectTask.run();
            }
        }, 0L);

        return registerJob(connectTask, String.class);
    }

    /**
     * Test a JDBC query and see if it is a valid SQL statement.
     *
     * @param connectionName: the name of a JDBC Connection entity.
     * @param query:          a SQL query statement.
     * @return null if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    @Override
    public AsyncAdminMethods.JobId<String> testJdbcQuery(final String connectionName, final String query) {
        final FutureTask<String> queryTask = new FutureTask<String>(find(false).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {

                Object result = jdbcQueryingManager.performJdbcQuery(connectionName, query, 1, new ArrayList<Object>());
                return (result instanceof String) ? (String) result : null;
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                queryTask.run();
            }
        }, 0L);

        return registerJob(queryTask, String.class);
    }

    /**
     * Get a property, default driver class list from the global cluster properties.  if failed to get its value,
     * then use the original driver class list defined in this interface.
     *
     * @return a list of driver classes.
     */
    @Override
    public List<String> getPropertyDefaultDriverClassList() {
        List<String> driverClassList = new ArrayList<String>();

        String defaultList = config.getProperty(ServerConfigParams.PARAM_JDBC_CONNECTION_DEFAULT_DRIVERCLASS_LIST);
        if (defaultList != null && !defaultList.isEmpty()) {
            StringTokenizer tokens = new StringTokenizer(defaultList, "\n");
            while (tokens.hasMoreTokens()) {
                String driverClass = tokens.nextToken();
                if (driverClass != null && !driverClass.isEmpty()) driverClassList.add(driverClass);
            }
        }

        if (driverClassList.isEmpty()) driverClassList.add(ORIGINAL_DRIVERCLASS_LIST);
        return driverClassList;
    }

    /**
     * Get a property, default maximum number of records returned by a query from the global cluser properties.  If failed
     * to get its value, then use the original maximum number of records returned by a query defined in this interface.
     *
     * @return an integer, the default maximum number of records returned by a query
     */
    @Override
    public int getPropertyDefaultMaxRecords() {
        return config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAXRECORDS_DEFAULT, ORIGINAL_MAX_RECORDS);
    }

    /**
     * Get a property, default minimum pool size.  If failed to get its value, then use the original minimum pool size
     * defined in this interface.
     *
     * @return an integer, the default minimum pool size.
     */
    @Override
    public int getPropertyDefaultMinPoolSize() {
        return config.getIntProperty(ServerConfigParams.PARAM_JDBC_CONNECTION_POOLING_DEFAULT_MINPOOLSIZE, ORIGINAL_C3P0_BASIC_POOL_CONFIG_MINPOOLSIZE);
    }

    /**
     * Get a property, default maximum pool size.  If failed to get its value, then use the original maximum pool size
     * defined in this interface.
     *
     * @return an integer, the default maximum pool size.
     */
    @Override
    public int getPropertyDefaultMaxPoolSize() {
        return config.getIntProperty(ServerConfigParams.PARAM_JDBC_CONNECTION_POOLING_DEFAULT_MAXPOOLSIZE, ORIGINAL_C3P0_BASIC_POOL_CONFIG_MAXPOOLSIZE);
    }

    @Override
    public  AsyncAdminMethods.JobId<String> testAuditSinkSchema(final String connectionName, final String auditRecordTableName, final String auditDetailTableName){
        final FutureTask<String> queryTask = new FutureTask<String>( find( false ).wrapCallable( new Callable<String>(){
            @Override
            public String call() throws Exception {

                long bigInt = 123456789012345678L;
                String auditRecordId = UUID.randomUUID().toString();
                Map<String,Object> recordQueryObjects = new HashMap<String, Object>();
                recordQueryObjects.put("id",auditRecordId); // varchar(40)
                recordQueryObjects.put("nodeid","0123456789012345678901234567890123456789"); // varchar(40)
                recordQueryObjects.put("time", bigInt );  // bigint
                recordQueryObjects.put("type","record type");   //  varchar(50)
                recordQueryObjects.put("audit_level","very bad 123");   //  varchar(12)
                recordQueryObjects.put("name","very long name");   //  varchar(255)
                recordQueryObjects.put("message","very long message");   //  varchar(255)
                recordQueryObjects.put("ip_address","255.255.255.255");   //  varchar(39)
                recordQueryObjects.put("user_name","user name");   //  varchar(255)
                recordQueryObjects.put("user_id","user id");   //  varchar(255)
                recordQueryObjects.put("provider_oid", bigInt);   //  bigint
                recordQueryObjects.put("signature","123456789-123456789-123456789-123456789-123456789-123456789-123456789-123456789-123456789");   //  varchar(1024)
                recordQueryObjects.put("entity_class","entity class");   //  varchar(255)
                recordQueryObjects.put("entity_id",bigInt);   //  bigint
                recordQueryObjects.put("status","200");   //  varchar(50)
                recordQueryObjects.put("request_id","request id");   //  varchar(40)
                recordQueryObjects.put("service_oid",bigInt);   //  bigint
                recordQueryObjects.put("operation_name","operation name");   //  varchar(255)
                recordQueryObjects.put("authenticated", true );   //  bit
                recordQueryObjects.put("authenticationType", "auth type" );   //  int
                recordQueryObjects.put("request_length", 123);   //  int
                recordQueryObjects.put("response_length",123);   //  int
                recordQueryObjects.put("request_xml","123456789-123456789-123456789-123456789-123456789-123456789-123456789".getBytes());   //  varbinary(max)
                recordQueryObjects.put("response_xml","123456789-123456789-123456789-123456789-123456789-123456789-123456789".getBytes());   //  varbinary(max)
                recordQueryObjects.put("response_status",123);   // int
                recordQueryObjects.put("routing_latency",123);   //  int
                recordQueryObjects.put("properties","some properties");   //  varchar(max)
                recordQueryObjects.put("component_id",123);   // int
                recordQueryObjects.put("action","action");   //  varchar(32)

                List<Object> values = new ArrayList<Object>();
                StringBuffer query =  createTestQuery(recordQueryObjects,values,auditRecordTableName);

                Object result = jdbcQueryingManager.performJdbcQuery(connectionName, query.toString(), 10, values);
                boolean success = result instanceof Integer && (Integer)result==1;
                if(!success)
                    return "Failed audit record";


                // test audit detail
                String auditDetailId = UUID.randomUUID().toString();
                Map<String,Object> detailQueryObjects = new HashMap<String, Object>();
                detailQueryObjects.put("id",auditDetailId); // varchar(40)
                detailQueryObjects.put("audit_oid",auditRecordId); // varchar(40)
                detailQueryObjects.put("time",bigInt); // bigint
                detailQueryObjects.put("component_id",456); // int
                detailQueryObjects.put("message_id",123); // int)
                detailQueryObjects.put("exception_message","exception message"); // varchar(max)
                detailQueryObjects.put("properties","some properties"); // varchar(max)

                values = new ArrayList<Object>();
                query =  createTestQuery(detailQueryObjects,values,auditDetailTableName);

                result = jdbcQueryingManager.performJdbcQuery(connectionName, query.toString(), 10, values);
                success = result instanceof Integer && (Integer)result==1;
                if(!success)
                    return "Failed audit record";

                // remove audit record
                String queryStr = "delete from "+auditRecordTableName+" where id like'"+auditRecordId+"'";
                result = jdbcQueryingManager.performJdbcQuery(connectionName, queryStr, 10, null);
                success = result instanceof Integer && (Integer)result==1;
                if(!success)
                    return "Failed audit record";


                // check if audit detail is removed
                queryStr = "select * from "+auditDetailTableName+" where id like'"+auditDetailId+"'";
                result = jdbcQueryingManager.performJdbcQuery(connectionName, queryStr, 10, null);
                success = result instanceof SqlRowSet && ((SqlRowSet) result).next()== false;


                // cleanup - remove audit detail?
                if(!success){
                    queryStr = "delete from "+auditDetailTableName+" where id like'"+auditDetailId+"'";
                    result = jdbcQueryingManager.performJdbcQuery(connectionName, queryStr, 10, null);
                    return "Failed audit record";

                }
                return "" ;
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                queryTask.run();
            }
        }, 0L);

        return registerJob( queryTask, String.class);
    }

    private StringBuffer createTestQuery(Map<String, Object> recordQueryObjects, List<Object> values, String auditRecordTable) {
        StringBuffer query = new StringBuffer( "insert into "+auditRecordTable+" (");
        Object[] keys = recordQueryObjects.keySet().toArray();
        StringBuffer valueQuery = new StringBuffer(" values (");
        for(Object key:keys){
            query.append(key);
            query.append(',');
            values.add(recordQueryObjects.get(key));

            valueQuery.append("?");
            valueQuery.append(',');
        }
        query.deleteCharAt(query.length()-1);
        valueQuery.deleteCharAt(valueQuery.length()-1);

        query.append(") ");
        query.append(valueQuery);
        query.append(");");
        return query;
    }

}
