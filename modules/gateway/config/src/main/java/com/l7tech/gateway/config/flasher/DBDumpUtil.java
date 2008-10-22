package com.l7tech.gateway.config.flasher;

import com.l7tech.util.HexUtils;
import com.l7tech.util.SyspropUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.*;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * Methods for dumping
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>ma
 */
class DBDumpUtil {

    private static final Logger logger = Logger.getLogger(DBDumpUtil.class.getName());
    public static final String DBDUMPFILENAME_CLONE = "dbdump_restore.sql";
    public static final String LICENCEORIGINALID = "originallicenseobjectid.txt";
    private static final String DEFAULT_DB_URL = "jdbc:mysql://{0}:{1}/{2}?autoReconnect=false&characterEncoding=UTF8&characterSetResults=UTF8&socketTimeout=120000&connectTimeout=10000";
    private static final String[] TABLE_NEVER_EXPORT = {"cluster_info", "message_id", "service_metrics", "service_metrics_details", "service_usage"};
    private static final String JDBC_DRIVER_NAME = "com.mysql.jdbc.Driver";

    static {
        try {
            Class.forName(JDBC_DRIVER_NAME);
        } catch( ClassNotFoundException cnfe ) {
            logger.info("MySQL JDBC driver not available.");
        }
    }        

    /**
     * Retrieve a database connection to the given database. This connection will need to be closed by the caller to
     * avoid resource leaks
     *
     * @param dburl a URL of the form jdbc:dbtype://host/dbname where dbtype is the name of the db driver/vendor
     * (in our case it's mysql)
     * @param dbuser the user to connect with
     * @param dbpasswd the password for the db user. Not null, pass "" for no password
     * @return an established connection to the DB specified in dburl. Caller is responsible for closing and
     * maintinaining this connection
     * @throws SQLException if there was an exception while connecting to the DB
     */
    public static Connection getConnection(String dburl, String dbuser, String dbpasswd) throws SQLException {
        return DriverManager.getConnection(dburl, dbuser, dbpasswd);
    }

    /**
     * Retrieve a database connection to the given database. This connection will need to be closed by the caller to
     * avoid resource leaks
     *
     * @param hostname The database host
     * @param hostname The database host
     * @param hostname The database host
     * @param dbuser the user to connect with
     * @param dbpasswd the password for the db user. Not null, pass "" for no password
     * @return an established connection to the DB specified in dburl. Caller is responsible for closing and
     * maintinaining this connection
     * @throws SQLException if there was an exception while connecting to the DB
     */
    public static Connection getConnection(String hostname, int port, String dbName, String dbuser, String dbpasswd) throws SQLException {
        String urlPattern = SyspropUtil.getString("com.l7tech.migration.dburl", DEFAULT_DB_URL);
        String databaseURL = MessageFormat.format( urlPattern, hostname, Integer.toString(port), dbName, dbuser, dbpasswd );
        return getConnection(databaseURL, dbuser, dbpasswd);
    }

    /**
     * outputs database dump files
     * @param databaseHost database host
     * @param databasePort database port
     * @param databaseName database host
     * @param databaseUser database user
     * @param databasePasswd database password
     * @param includeAudit whether or not audit tables should be included
     * @param outputDirectory the directory path where the dump files should go to
     * @param stdout    stream for verbose output; <code>null</code> for no verbose output
     * @throws java.sql.SQLException problem getting data out of db
     * @throws java.io.IOException problem with dump files
     */
    public static void dump(String databaseHost, int databasePort, String databaseName, String databaseUser, String databasePasswd,
                            boolean includeAudit, String outputDirectory, PrintStream stdout) throws SQLException, IOException {
        Connection c = getConnection(databaseHost, databasePort, databaseName, databaseUser, databasePasswd);
        DatabaseMetaData metadata = c.getMetaData();
        String[] tableTypes = {
                "TABLE"
        };
        ResultSet tableNames = metadata.getTables(null, "%", "%", tableTypes);
        FileOutputStream cloneoutput = new FileOutputStream(outputDirectory + File.separator + DBDUMPFILENAME_CLONE);
        if (stdout != null) stdout.print("Dumping database to " + outputDirectory + " ..");
        cloneoutput.write("SET FOREIGN_KEY_CHECKS = 0;\n".getBytes());
        while (tableNames.next()) {
            String tableName = tableNames.getString("TABLE_NAME");
            // drop and recreate table
            cloneoutput.write(("DROP TABLE IF EXISTS " + tableName + ";\n").getBytes());
            Statement getCreateTablesStmt = c.createStatement();
            ResultSet createTables = getCreateTablesStmt.executeQuery("show create table " + tableName);
            while (createTables.next()) {
                String s = createTables.getString(2);
                s = s.replace("\r", " ");
                s = s.replace("\n", " ");
                s = s.replace("`", "");
                cloneoutput.write((s + ";\n").getBytes());
            }
            if (tableInList(tableName, TABLE_NEVER_EXPORT)) continue;
            if (!includeAudit) {
                if (tableName.startsWith("audit_")) continue;
            }
            Statement tdata = c.createStatement();
            ResultSet tdataList = tdata.executeQuery("select * from " + tableName);
            while (tdataList.next()) {
                if (tableName.equals("cluster_properties")) { // dont include license in image
                    String tmp = tdataList.getString(3);
                    if (tmp != null && tmp.equals("license")) {
                        // we dont include the license, however, we will record the object id
                        // in order to be able to put back same id when we restore the original
                        // target license. this will avoid clashing object id
                        long licenseobjectid = tdataList.getLong(1);
                        FileOutputStream licenseFos = new FileOutputStream(outputDirectory + File.separator + LICENCEORIGINALID);
                        licenseFos.write(Long.toString(licenseobjectid).getBytes());
                        licenseFos.close();
                        continue;
                    }
                }

                StringBuffer insertStatementToRecord = new StringBuffer("INSERT INTO " + tableName + " VALUES (");
                ResultSetMetaData rowInfo = tdataList.getMetaData();
                for (int i = 1; i <= rowInfo.getColumnCount(); i++) {
                    int colType = rowInfo.getColumnType(i);
                    switch (colType) {
                        case Types.BIGINT: {
                            final long value = tdataList.getLong(i);
                            insertStatementToRecord.append(tdataList.wasNull() ? "NULL" : value);
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;
                        }
                        case Types.INTEGER:
                        case Types.BIT:
                        case Types.TINYINT: {
                            final int value = tdataList.getInt(i);
                            insertStatementToRecord.append(tdataList.wasNull() ? "NULL" : value);
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;
                        }
                        case Types.DOUBLE: {
                            final double value = tdataList.getDouble(i);
                            insertStatementToRecord.append(tdataList.wasNull() ? "NULL" : value);
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;
                        }
                        case Types.VARCHAR:
                        case Types.CHAR:
                        case Types.LONGVARCHAR: // medium text
                            String tmp = tdataList.getString(i);
                            if (tmp != null) {
                                tmp = escapeForSQLInsert(tmp);
                                insertStatementToRecord.append("'");
                                insertStatementToRecord.append(tmp);
                                insertStatementToRecord.append("'");
                            } else {
                                insertStatementToRecord.append("NULL");
                            }
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;
                        case Types.LONGVARBINARY: // medium blob
                            final byte[] tmpBytes = tdataList.getBytes(i);
                            if (tmpBytes != null) {
                                insertStatementToRecord.append("0x");
                                insertStatementToRecord.append(HexUtils.hexDump(tmpBytes));
                            } else {
                                insertStatementToRecord.append("NULL");
                            }
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;
                        default:
                            logger.severe("unexpected java.sql.Type value " + colType);
                            throw new RuntimeException("unhandled column type: " + colType);
                    }
                }
                insertStatementToRecord.append(");\n");
                cloneoutput.write(insertStatementToRecord.toString().getBytes());
            }
            tdataList.close();
        }
        c.close();
        cloneoutput.write("SET FOREIGN_KEY_CHECKS = 1;\n".getBytes());
        cloneoutput.close();
        if (stdout != null) stdout.println(". Done");
    }

    private static String escapeForSQLInsert(String in) {
        String output = in.replace("\"", "\\\"");
        output = output.replace("\'", "\\\'");
        output = output.replace("\n", "\\n");
        output = output.replace("\r", "\\r");
        return output;
    }

    private static boolean tableInList(String tableName, String[] tableList) {
        for (String s : tableList) {
            if (s.equals(tableName)) return true;
        }
        return false;
    }

}
