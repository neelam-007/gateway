package com.l7tech.portal.metrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.*;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DatabaseGenericEntityServiceTest {

    private static final String UUID_VALUE = "abcd12345";
    private static final Long OID_VALUE= 0101010101L;
    private static final String CLASSNAME_VALUE= "com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";
    private static final String DB = "jdbc:hsqldb:mem:testdb";
    private static final String TABLE_NAME = "generic_entity";
    private static final String UUID_COL_NAME = "name";
    private static final String OID_COL_NAME = "description";
    private static final String CLASSNAME_COL_NAME = "classname";


    private DatabaseGenericEntityService service;
    private DatabaseInfo dbInfo;
    private Connection connection;

    @Before
    public void setup() throws SQLException {
        dbInfo = new DatabaseInfo(DB, USERNAME, PASSWORD);
        service = new DatabaseGenericEntityService(dbInfo, TABLE_NAME, UUID_COL_NAME, OID_COL_NAME,CLASSNAME_COL_NAME);
        connection = DriverManager.getConnection(DB);
        createTable();
    }

    @After
    public void teardown() throws SQLException {
        dropTable();
        if (connection != null) {
            connection.close();
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructorNullDbInfo(){
        service = new DatabaseGenericEntityService(null, TABLE_NAME, UUID_COL_NAME, OID_COL_NAME,CLASSNAME_COL_NAME);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructorNullTableName(){
        service = new DatabaseGenericEntityService(dbInfo, null, UUID_COL_NAME, OID_COL_NAME,CLASSNAME_COL_NAME);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructorNullValUuid(){
        service = new DatabaseGenericEntityService(dbInfo, TABLE_NAME, null, OID_COL_NAME,CLASSNAME_COL_NAME);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructorNullValOid(){
        service = new DatabaseGenericEntityService(dbInfo, TABLE_NAME, UUID_COL_NAME, null,CLASSNAME_COL_NAME);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructorNullValClassname(){
        service = new DatabaseGenericEntityService(dbInfo, TABLE_NAME, UUID_COL_NAME, OID_COL_NAME,null);
    }


    @Test(expected=IllegalArgumentException.class)
    public void constructorEmptyTableName(){
        service = new DatabaseGenericEntityService(dbInfo, "", UUID_COL_NAME, OID_COL_NAME,CLASSNAME_COL_NAME);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructorEmptyUuid(){
        service = new DatabaseGenericEntityService(dbInfo, TABLE_NAME, "", OID_COL_NAME,CLASSNAME_COL_NAME);
    }

    @Test(expected=IllegalArgumentException.class)
    public void constructorEmptyOid(){
        service = new DatabaseGenericEntityService(dbInfo, TABLE_NAME, UUID_COL_NAME, "",CLASSNAME_COL_NAME);
    }


    @Test(expected=IllegalArgumentException.class)
    public void constructorEmptyClassname(){
        service = new DatabaseGenericEntityService(dbInfo, TABLE_NAME, UUID_COL_NAME, OID_COL_NAME,"");
    }

    @Test
    public void getValue() throws SQLException {
        insertGenericEntity(1L, UUID_VALUE, OID_VALUE,CLASSNAME_VALUE);

        final Map<Long,String> result = service.getGenericEntityValue(CLASSNAME_VALUE);

        assertEquals(UUID_VALUE,result.get(OID_VALUE));
        assertEquals(OID_VALUE,result.keySet().iterator().next());

    }

    @Test
    public void getValueEmpty() throws SQLException {
        insertGenericEntity(1L, UUID_VALUE, null, CLASSNAME_VALUE);

        final Map<Long,String> result = service.getGenericEntityValue(CLASSNAME_VALUE);

        assertNull(result.get(OID_VALUE));
        //assertEquals(result.keySet().iterator().next(), OID_VALUE);


    }

    @Test
    public void doesNotExist() throws SQLException {
        final Map<Long,String> result = service.getGenericEntityValue(CLASSNAME_VALUE);
        assertEquals(result.isEmpty(), true);

    }


    @Test(expected=IllegalArgumentException.class)
    public void nullPropertyName() throws SQLException{
        service.getGenericEntityValue(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void emptyPropertyName() throws SQLException{
        service.getGenericEntityValue("");
    }

    private void insertGenericEntity(final Long id, final String name, final Long description, final String classname) throws SQLException {
        final PreparedStatement statement = connection.prepareStatement("INSERT INTO "+TABLE_NAME+" VALUES(?,?,?,?,?,1,'XML')");
        int i = 0;
        statement.setLong(++i, id);
        statement.setInt(++i, 1);
        statement.setString(++i, name);
        if(description != null)
            statement.setLong(++i, description);
        else
            statement.setNull(++i, Types.BIGINT);
        statement.setString(++i, classname);
        statement.execute();
        statement.close();
    }


    private void createTable() throws SQLException {
        final Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE generic_entity ( " +
                "objectid bigint NOT NULL, " +
                "version integer NOT NULL, " +
                "name varchar(255) NULL, " +
                "description bigint NULL, " +
                "classname varchar(255) NULL, " +
                "enabled int NULL, " +
                "valueXML varchar(255) NULL, " +
                "PRIMARY KEY (objectid), " +
                ");");
        statement.close();
    }

    private void dropTable() throws SQLException {
        final Statement statement = connection.createStatement();
        statement.execute("DROP TABLE generic_entity");
        statement.close();
    }
}
