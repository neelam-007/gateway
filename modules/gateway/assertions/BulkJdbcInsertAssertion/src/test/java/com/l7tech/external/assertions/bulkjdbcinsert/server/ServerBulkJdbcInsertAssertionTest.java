package com.l7tech.external.assertions.bulkjdbcinsert.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.apache.commons.io.input.BOMInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test the BulkJdbcInsertAssertion.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/external/assertions/bulkjdbcinsert/server/bulkJdbcInsertApplicationContext.xml")
public class ServerBulkJdbcInsertAssertionTest {

    private static final java.lang.String MESSAGE_CONTENT_TYPE = "application/octet-stream";
    ServerBulkJdbcInsertAssertion fixture;

    private BulkJdbcInsertAssertion assertion;

    static DataSource mockDataSource;

    static InputStream inputStream;

    private Connection mockConnection;

    private PreparedStatement mockPreparedStatement;

    @BeforeClass
    public static void init() throws Exception {
        inputStream = ServerBulkJdbcInsertAssertionTest.class.getResourceAsStream("0700_20150625-072200.v2.csv.gz");
        mockDataSource = mock(DataSource.class);
    }

    @Autowired
    ApplicationContext mockApplicationContext;

    @Before
    public void setUp() throws Exception {
       mockConnection = mock(Connection.class);
       mockPreparedStatement = mock(PreparedStatement.class);
       assertion = new BulkJdbcInsertAssertion();
       assertion.setConnectionName("Connection");
       when(mockDataSource.getConnection()).thenReturn(mockConnection);
       when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
       fixture = new ServerBulkJdbcInsertAssertion(assertion, mockApplicationContext);
    }


    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void shouldProcessMessageContainingCSV() throws Exception {
        InputStream inputStream = ServerBulkJdbcInsertAssertionTest.class.getResourceAsStream("0700_20150625-072200.v2.csv.gz");
        Message request = new Message(new ByteArrayStashManager(),
                ContentTypeHeader.parseValue(MESSAGE_CONTENT_TYPE),
                inputStream);
        Message response = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        assertion.setConnectionName("Connection");
        assertion.setTableName("TableOne");
        List<BulkJdbcInsertAssertion.ColumnMapper> mapperList = new ArrayList<>();
        BulkJdbcInsertAssertion.ColumnMapper m1 = new BulkJdbcInsertAssertion.ColumnMapper();
        m1.setName("ColumnOne");
        m1.setOrder(1);
        m1.setTransformation("String");
        mapperList.add(m1);
        BulkJdbcInsertAssertion.ColumnMapper m2 = new BulkJdbcInsertAssertion.ColumnMapper();
        m2.setName("ColumnTwo");
        m2.setOrder(3);
        m2.setTransformation("String");
        mapperList.add(m2);
        assertion.setColumnMapperList(mapperList);
        doNothing().when(mockPreparedStatement).setString(eq(0), anyString());
        when(mockPreparedStatement.executeBatch()).thenReturn(new int[]{1});
        doNothing().when(mockConnection).commit();
        assertEquals(AssertionStatus.NONE, fixture.doCheckRequest(context, request, null, new AuthenticationContext()));
        verify(mockPreparedStatement, atLeastOnce()).setString(eq(1), anyString());
        verify(mockPreparedStatement, atLeastOnce()).setString(eq(2), anyString());
    }

    @Test
    public void testBuildSqlInsertStatement() throws Exception {
        List<BulkJdbcInsertAssertion.ColumnMapper> mapperList = new ArrayList<>();
        BulkJdbcInsertAssertion.ColumnMapper m1 = new BulkJdbcInsertAssertion.ColumnMapper();
        m1.setName("ColumnOne");
        m1.setOrder(1);
        BulkJdbcInsertAssertion.ColumnMapper m2 = new BulkJdbcInsertAssertion.ColumnMapper();
        m2.setName("ColumnTwo");
        m2.setOrder(2);
        m2.setTransformation("String");
        BulkJdbcInsertAssertion.ColumnMapper m3 = new BulkJdbcInsertAssertion.ColumnMapper();
        m3.setName("ColumnTwo");
        m3.setOrder(2);
        m3.setTransformation("Subtract");
        m3.setTransformParam("6");
        mapperList.add(m3);
        mapperList.add(m2);
        mapperList.add(m1);
        assertion.setColumnMapperList(mapperList);
        fixture = new ServerBulkJdbcInsertAssertion(assertion, mockApplicationContext);
        assertEquals("INSERT INTO tableOne(ColumnOne,ColumnTwo) VALUES (?,?)", fixture.buildSqlStatement("tableOne", fixture.getColumnMapperSet()));
    }



    public InputStreamReader newReader(final InputStream inputStream) {
        return new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
    }

    public static class MockJdbcConnectionPoolManager extends JdbcConnectionPoolManager{

        DataSource dataSource;

        public MockJdbcConnectionPoolManager(@NotNull JdbcConnectionManager jdbcConnectionManager) {
            super(jdbcConnectionManager);
            this.dataSource = mockDataSource;
        }

        public DataSource getDataSource(String jdbcConnName) throws NamingException, SQLException {
            return dataSource;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
        }
    }

    public static class MockJdbcConnectionManager implements JdbcConnectionManager {

        @Override
        public JdbcConnection getJdbcConnection(String connectionName) throws FindException {
            return null;
        }

        @Override
        public List<String> getSupportedDriverClass() {
            return null;
        }

        @Override
        public boolean isDriverClassSupported(String driverClass) {
            return false;
        }

        @Nullable
        @Override
        public JdbcConnection findByPrimaryKey(Goid goid) throws FindException {
            return null;
        }

        @Override
        public Collection<EntityHeader> findAllHeaders() throws FindException {
            return null;
        }

        @Override
        public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
            return null;
        }

        @Override
        public Collection<JdbcConnection> findAll() throws FindException {
            return null;
        }

        @Override
        public Class<? extends Entity> getImpClass() {
            return null;
        }

        @Override
        public Goid save(JdbcConnection entity) throws SaveException {
            return null;
        }

        @Override
        public void save(Goid id, JdbcConnection entity) throws SaveException {

        }

        @Override
        public Integer getVersion(Goid goid) throws FindException {
            return null;
        }

        @Override
        public Map<Goid, Integer> findVersionMap() throws FindException {
            return null;
        }

        @Override
        public void delete(JdbcConnection entity) throws DeleteException {

        }

        @Nullable
        @Override
        public JdbcConnection getCachedEntity(Goid goid, int maxAge) throws FindException {
            return null;
        }

        @Override
        public Class<? extends Entity> getInterfaceClass() {
            return null;
        }

        @Override
        public EntityType getEntityType() {
            return null;
        }

        @Override
        public String getTableName() {
            return null;
        }

        @Nullable
        @Override
        public JdbcConnection findByUniqueName(String name) throws FindException {
            return null;
        }

        @Override
        public void delete(Goid goid) throws DeleteException, FindException {

        }

        @Override
        public void update(JdbcConnection entity) throws UpdateException {

        }

        @Nullable
        @Override
        public JdbcConnection findByHeader(EntityHeader header) throws FindException {
            return null;
        }

        @Override
        public List<JdbcConnection> findPagedMatching(int offset, int count, @Nullable String sortProperty, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> matchProperties) throws FindException {
            return null;
        }
    }
}
