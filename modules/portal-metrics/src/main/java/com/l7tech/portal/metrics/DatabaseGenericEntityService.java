package com.l7tech.portal.metrics;

import org.apache.commons.lang.Validate;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dfernando
 * Date: 5/17/12
 * Time: 9:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseGenericEntityService implements GenericEntityService {
    private final DatabaseInfo databaseInfo;
    private final String tableName;
    private final String uuidColumnName;
    private final String serviceOidColumnName;
    private final String classnamePropertyColumn;


    public DatabaseGenericEntityService(final DatabaseInfo databaseInfo, final String tableName, final String uuidColumnName, final String serviceOidColumnName,final String classnamePropertyColumn) {
        Validate.notNull(databaseInfo, "Database info cannot be null.");
        Validate.notEmpty(tableName, "Table name cannot be null or empty.");
        Validate.notEmpty(uuidColumnName, "UUID column name cannot be null or empty.");
        Validate.notEmpty(serviceOidColumnName, "Service OID column name cannot be null or empty.");
        Validate.notEmpty(classnamePropertyColumn, "classname column name cannot be null or empty.");

        this.databaseInfo = databaseInfo;
        this.tableName = tableName;
        this.uuidColumnName = uuidColumnName;
        this.serviceOidColumnName = serviceOidColumnName;
        this.classnamePropertyColumn = classnamePropertyColumn;
    }

    @Override
    public Map<Long,String> getGenericEntityValue(final String classnamePropertyValue) throws ClusterPropertyException {
        Validate.notEmpty(classnamePropertyValue, "classname value property column cannot be null or empty.");
        Connection connection = null;
        HashMap<Long,String> value = null;
        try {
            connection = DriverManager.getConnection(databaseInfo.getUrl(), databaseInfo.getUsername(), databaseInfo.getPassword());
            connection.setReadOnly(true);
            final PreparedStatement statement = connection.prepareStatement(buildQuery());
            statement.setString(1, classnamePropertyValue);
            final ResultSet resultSet = statement.executeQuery();
            value = new HashMap<Long,String>();
            boolean clasnameExists = false;
            while (resultSet.next()) {
                clasnameExists = true;
                String uuid = resultSet.getString(uuidColumnName);
                Long oid = resultSet.getLong(serviceOidColumnName);
                //value.add(resultSet.getString(clusterPropertyValueColumnName));
                value.put(oid, uuid);
            }
            statement.close();
        } catch (final SQLException e) {
            // FIXME this should not be throwing a ClusterPropertyException
            throw new ClusterPropertyException("Error retrieving data from generic entity table : " + e.getMessage(), e);
        } finally {
            closeConnection(connection);
        }
        return value;
    }


    private String buildQuery() {
        return "SELECT "+serviceOidColumnName+","+ uuidColumnName+ " FROM " + tableName + " WHERE " + classnamePropertyColumn + " = ?";
    }

    private void closeConnection(final Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (final SQLException e) {
                throw new ClusterPropertyException("Error closing connection.", e);
            }
        }
    }
}
