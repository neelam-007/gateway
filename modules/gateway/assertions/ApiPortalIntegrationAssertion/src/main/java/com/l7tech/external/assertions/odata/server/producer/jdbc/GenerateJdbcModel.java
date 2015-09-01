package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcColumn;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcPrimaryKey;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcTable;
import org.core4j.ThrowingFunc1;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class GenerateJdbcModel implements ThrowingFunc1<Connection, JdbcModel> {

    @Override
    public JdbcModel apply(Connection conn) throws Exception {
        JdbcModel model = new JdbcModel();
        DatabaseMetaData meta = conn.getMetaData();

        //TODO: review oracle schema support, current implementation is auto-detect with fallback using meta.getUserName as the schema
        String databaseTypeName = Util.getDatabaseTypeName(conn);
        String paramSchemaName = meta.getUserName();
        if (databaseTypeName.indexOf("oracle") >= 0) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select sys_context( 'userenv', 'current_schema' ) as schemaNameTxt from dual");
            if (rs != null && rs.next()) {//we only support the first returned schema for oracle
                paramSchemaName = rs.getString(1);
            }
        } else if (databaseTypeName.contains("hsql database")) {
            paramSchemaName = null;
        }

        ResultSet tables = meta.getTables(null, paramSchemaName, null, new String[]{"TABLE", "VIEW"});
        while (tables.next()) {
            String schemaName = tables.getString("TABLE_SCHEM");//oracle specific
            if (schemaName == null) {
                schemaName = tables.getString(1);//mysql fallback
            }
            //String schemaName = tables.getString(1);//tables.getString("TABLE_SCHEM");
            String tableName = tables.getString(3);//tables.getString("TABLE_NAME");
            JdbcTable table = model.getOrCreateTable(schemaName, tableName);
            table.tableType = tables.getString("TABLE_TYPE");
            ResultSet columns = meta.getColumns(null, schemaName, tableName, null);
            while (columns.next()) {
                //        String schemaName = tables.getString(1);//tables.getString("TABLE_SCHEM");
                //        String tableName = tables.getString(3);//tables.getString("TABLE_NAME");
                String columnName = columns.getString("COLUMN_NAME");
                JdbcColumn column = model.getOrCreateColumn(schemaName, tableName, columnName);
                column.columnType = columns.getInt("DATA_TYPE");
                column.columnTypeName = columns.getString("TYPE_NAME");
                column.columnSize = (Integer) columns.getObject("COLUMN_SIZE");
                column.isNullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                column.ordinalPosition = columns.getInt("ORDINAL_POSITION");
            }

            // primary keys
            ResultSet primaryKeys = meta.getPrimaryKeys(null, schemaName, tableName);
            while (primaryKeys.next()) {
                //        String schemaName = primaryKeys.getString("TABLE_SCHEM");
                //        String tableName = primaryKeys.getString("TABLE_NAME");
                //        JdbcTable table = model.getTable(schemaName, tableName);
                JdbcPrimaryKey primaryKey = new JdbcPrimaryKey();
                primaryKey.columnName = primaryKeys.getString("COLUMN_NAME");
                primaryKey.sequenceNumber = primaryKeys.getInt("KEY_SEQ");
                primaryKey.primaryKeyName = primaryKeys.getString("PK_NAME");
                table.primaryKeys.add(primaryKey);
            }
        }

        return model;
    }

}