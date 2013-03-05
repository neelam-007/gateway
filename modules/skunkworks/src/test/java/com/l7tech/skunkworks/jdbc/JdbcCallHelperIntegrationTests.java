package com.l7tech.skunkworks.jdbc;

import com.l7tech.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.io.StringWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This was created: 2/22/13 as 3:10 PM
 *
 * @author Victor Kazakov
 */
public class JdbcCallHelperIntegrationTests extends JdbcCallHelperIntegrationAbstractBaseTestClass {

    /**
     * Tests retrieving a clob from a procedure.
     *
     * @throws Exception
     */
    @Test
    public void testClob() throws Exception {
        try {
            createDropItem(CreateClobOutProcedure);

            int length = 64;
            String query = "CALL " + ClobOutProcedureName + " " + length;

            Object result = getJdbcQueryingManager().performJdbcQuery(getDataSource(), query, "qatest", 100, Collections.emptyList());

            Assert.assertNotNull(result);
            Assert.assertTrue("Incorrect type of result: " + result.getClass(), result instanceof ArrayList);

            @SuppressWarnings("unchecked") List<SqlRowSet> resultList = (List<SqlRowSet>) result;

            SqlRowSet rtn = resultList.get(0);
            rtn.next();
            Object value = rtn.getObject("OUTCLOB");

            StringWriter writer = new StringWriter();
            IOUtils.copyStream(((Clob) value).getCharacterStream(), writer);

            String stringRtn = writer.toString();
            Assert.assertTrue(stringRtn.length() == length * 1024);
        } finally {
            createDropItem(DropClobOutProcedure);
        }
    }

    /**
     * Tests retrieving a blob from a procedure
     *
     * @throws Exception
     */
    @Test
    public void testBlob() throws Exception {
        try {
            createDropItem(CreateBlobOutProcedure);

            int length = 128;
            String query = "CALL " + BlobOutProcedureName + " " + length;

            Object result = getJdbcQueryingManager().performJdbcQuery(getDataSource(), query, "qatest", 100, Collections.emptyList());

            Assert.assertNotNull(result);
            Assert.assertTrue("Incorrect type of result: " + result.getClass(), result instanceof ArrayList);

            @SuppressWarnings("unchecked") List<SqlRowSet> resultList = (List<SqlRowSet>) result;

            SqlRowSet rtn = resultList.get(0);
            rtn.next();
            byte[] value = (byte[]) rtn.getObject("OUTBLOB");

            Assert.assertTrue(value.length == length * 1024);
        } finally {
            createDropItem(DropBlobOutProcedure);
        }
    }

    /**
     * Retrieve metadata for a procedure
     *
     * @throws SQLException
     */
    @Test
    public void testMetaData() throws SQLException {
        try {
            createDropItem(CreateDataTypeProcedure);
            try (Connection connection = getDataSource().getConnection()) {
                ResultSet resultSet = connection.getMetaData().getProcedureColumns(null, "QATEST", CreateDataTypeProcedureName, "%");
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                StringWriter sw = new StringWriter();
                sw.append("{| class=\"wikitable sortable\"\n" +
                        "|-\n");
                for (int i = 2; i < resultSetMetaData.getColumnCount(); i++) {
                    sw.append("! ").append(resultSetMetaData.getColumnName(i)).append(" !");
                }
                sw.append("\n|-\n");
                int param = 0;
                while (resultSet.next()) {
                    param++;
                    Assert.assertEquals("Parameter is of wrong type: Parameter: " + resultSet.getString(4), metadataTypes[param], resultSet.getInt(6));
                    Assert.assertEquals("Parameter in out type is incorrect: " + resultSet.getString(4), parameterTypes[param], resultSet.getInt(5));
                    for (int i = 2; i < resultSetMetaData.getColumnCount(); i++) {
                        sw.append("| ").append(String.valueOf(resultSet.getObject(i))).append(" |");
                    }
                    sw.append("\n|-\n");
                }
                sw.append("|}");
                System.out.println(sw.toString());
            }
        } finally {
            createDropItem(DropCreateDataTypeProcedure);
        }
    }

    /**
     * Returns the type info
     *
     * @throws SQLException
     */
    @Test
    public void testGetTypeInfo() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            ResultSet resultSet = connection.getMetaData().getTypeInfo();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            StringWriter sw = new StringWriter();
            sw.append("{| class=\"wikitable sortable\"\n" +
                    "|-\n");
            for (int i = 1; i < resultSetMetaData.getColumnCount(); i++) {
                sw.append("! ").append(resultSetMetaData.getColumnName(i)).append(" !");
            }
            sw.append("\n|-\n");
            while (resultSet.next()) {
                for (int i = 1; i < resultSetMetaData.getColumnCount(); i++) {
                    sw.append("| ").append(String.valueOf(resultSet.getObject(i))).append(" |");
                }
                sw.append("\n|-\n");
            }
            sw.append("|}");
            System.out.println(sw.toString());
        }

    }

    /**
     * Returns the current driver version
     *
     * @throws SQLException
     */
    @Test
    public void testGetDriverVersion() throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            int majorVersion = connection.getMetaData().getDriverMajorVersion();
            int minorVersion = connection.getMetaData().getDriverMinorVersion();
            String name = connection.getMetaData().getDriverName();
            String version = connection.getMetaData().getDriverVersion();

            StringWriter sw = new StringWriter();
            sw.append("Driver Major version: ").append(String.valueOf(majorVersion)).append("\n");
            sw.append("Driver Minor version: ").append(String.valueOf(minorVersion)).append("\n");
            sw.append("Driver Name: ").append(name).append("\n");
            sw.append("Driver version: ").append(version).append("\n");

            System.out.println(sw.toString());
        }

    }

    private static final String ClobOutProcedureName = "TESTGETCLOB";
    public static final String DropClobOutProcedure = "DROP PROCEDURE " + ClobOutProcedureName;
    public static final String CreateClobOutProcedure =
            "create or replace\n" +
                    "procedure \"" + ClobOutProcedureName + "\"\n" +
                    "(size_of_blob IN NUMBER,\n" +
                    "outclob out clob)\n" +
                    "is\n" +
                    "begin\n" +
                    "  declare\n" +
                    "      lcntr number;\n" +
                    "  begin    \n" +
                    "      lcntr := 0;\n" +
                    "      for lcntr in 1..size_of_blob\n" +
                    "      loop\n" +
                    "      outclob:= outclob || lpad('0',1024,'1');\n" +
                    "      end loop; \n" +
                    "  end ;    \n" +
                    "end ;";

    private static final String BlobOutProcedureName = "TESTGETBLOB";
    public static final String DropBlobOutProcedure = "DROP PROCEDURE " + BlobOutProcedureName;
    public static final String CreateBlobOutProcedure =
            "create or replace\n" +
                    "procedure \"" + BlobOutProcedureName + "\"\n" +
                    "(size_of_blob IN NUMBER,\n" +
                    "outblob out blob)\n" +
                    "is\n" +
                    "begin\n" +
                    "  declare\n" +
                    "      lcntr number;\n" +
                    "      temp_blob BLOB;\n" +
                    "      tempClob clob;\n" +
                    "      dest_offset NUMBER := 1;\n" +
                    "      src_offset NUMBER := 1;\n" +
                    "      amount INTEGER := dbms_lob.lobmaxsize;\n" +
                    "      blob_csid NUMBER := dbms_lob.default_csid;\n" +
                    "      lang_ctx INTEGER := dbms_lob.default_lang_ctx;\n" +
                    "      warning INTEGER;\n" +
                    "  begin    \n" +
                    "      lcntr := 0;\n" +
                    "      for lcntr in 1..size_of_blob\n" +
                    "      loop\n" +
                    "      tempClob:= tempClob || lpad('0',1024,'1');\n" +
                    "      end loop;\n" +
                    "      DBMS_LOB.CREATETEMPORARY(lob_loc=>temp_blob, cache=>TRUE);\n" +
                    "      DBMS_LOB.CONVERTTOBLOB(temp_blob, tempClob,amount,dest_offset,src_offset,blob_csid,lang_ctx,warning);\n" +
                    "      outBlob := temp_blob;\n" +
                    "  end ;    \n" +
                    "end ;";

    private static final String CreateDataTypeProcedureName = "DATATYPES";
    public static final String DropCreateDataTypeProcedure = "DROP PROCEDURE " + CreateDataTypeProcedureName;
    public static final String CreateDataTypeProcedure =
            "create or replace\n" +
                    "procedure \"" + CreateDataTypeProcedureName + "\"\n" +
                    "(VARCHAR2_type IN varchar2,\n" +
                    "NVARCHAR2_type out NVARCHAR2,\n" +
                    "VARCHAR_type in out VARCHAR,\n" +
                    "CHAR_type in CHAR,\n" +
                    "NCHAR_type in NCHAR, \n" +
                    "NUMBER_type in NUMBER,\n" +
                    "BINARY_FLOAT_type in BINARY_FLOAT,\n" +
                    "BINARY_DOUBLE_type in BINARY_DOUBLE,\n" +
                    "BOOLEAN_type in boolean,\n" +
                    "PLS_INTEGER_type in PLS_INTEGER,\n" +
                    "BINARY_INTEGER_type in BINARY_INTEGER,\n" +
                    "LONG_type in LONG,\n" +
                    "DATE_type in DATE,\n" +
                    "TIMESTAMP_type in TIMESTAMP,\n" +
                    "RAW_type in RAW,\n" +
                    "CLOB_type in CLOB,\n" +
                    "NCLOB_type in NCLOB,\n" +
                    "BLOB_type in BLOB,\n" +
                    "BFILE_type in BFILE,\n" +
                    "XMLType_type in XMLType,\n" +
                    "out_type out NVARCHAR2)\n" +
                    "is \n" +
                    "begin\n" +
                    " select 1 into out_type from dual;\n" +
                    "end ;";
    public static final int[] metadataTypes = new int[]{-99, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.CHAR, Types.CHAR, Types.DECIMAL
            , Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.LONGVARCHAR
            , Types.TIMESTAMP, Types.TIMESTAMP, Types.VARBINARY, Types.LONGVARCHAR
            , Types.CLOB, Types.LONGVARBINARY, Types.LONGVARBINARY, Types.VARCHAR, Types.VARCHAR};
    public static final int[] parameterTypes = new int[]{DatabaseMetaData.procedureColumnUnknown,
            DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnOut, DatabaseMetaData.procedureColumnInOut,
            DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn,
            DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn,
            DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn,
            DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn,
            DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn,
            DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnOut};
}
