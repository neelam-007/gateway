package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.*;
import com.l7tech.server.cassandra.CassandraConnectionHolder;
import com.l7tech.server.cassandra.CassandraConnectionManager;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.util.ApplicationContextInjector;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import com.l7tech.util.Triple;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static com.l7tech.external.assertions.portalbootstrap.server.PortalBootstrapManager.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * @author alee, 8/28/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalBootstrapManagerTest {
    private static final Goid ID_PROV_GOID = new Goid(0, 1);
    private static final Goid OTK_POLICY_GOID = new Goid(0,2);
    private static final Goid OTK_ENCASS_GOID = new Goid(0,3);
    private static final Goid OTK_JDBC_GOID = new Goid(0,4);
    private static final String USER_LOGIN = "admin";
    private static final String USER_ID = "abc123";
    private static final String OTK_CONN_ID = new Goid(1, 1).toString();
    private static final String OTK_CONN_NAME = "OTK Connection";
    private static final String LOCALHOST = "localhost";
    private static final String OTK_VERSION = "4.3.0";
    private UserBean user;
    @Mock
    private ApplicationContext context;
    @Mock
    private ApplicationContextInjector injector;
    @Mock
    private ClusterInfoManager clusterInfoManager;
    @Mock
    private ServerPolicyFactory serverPolicyFactory;
    @Mock
    private WspReader wspReader;
    @Mock
    private SsgKeyStoreManager ssgKeyStoreManager;
    @Mock
    private RbacServices rbacServices;
    @Mock
    private PolicyManager policyManager;
    @Mock
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;
    @Mock
    private JdbcConnectionManager jdbcConnectionManager;
    @Mock
    private CassandraConnectionManager cassandraConnectionManager;
    @Mock
    private Config config;
    @Mock
    private ClusterPropertyManager clusterPropertyManager;
    @Mock
    private TrustedCertManager trustedCertManager;
 
    private PortalBootstrapManager manager;
    private StashManagerFactory stashManagerFactory;

    @Before
    public void setup() {
        when(context.getBean("injector", ApplicationContextInjector.class)).thenReturn(injector);
        try {
            PortalBootstrapManager.initialize(context);
        } catch (final IllegalStateException e) {
            if (e.getMessage().equals("PortalBootstrapManager has already been created")) {
                System.out.println("PortalBootstrapManager has already been created");
            } else {
                throw e;
            }
        }
        manager = PortalBootstrapManager.getInstance();
        user = new UserBean(ID_PROV_GOID, USER_LOGIN);
        user.setUniqueIdentifier(USER_ID);
        manager.setConfig(config);
        stashManagerFactory = TestStashManagerFactory.getInstance();

        ApplicationContexts.inject(manager, CollectionUtils.<String, Object>mapBuilder()
                        .put("serverPolicyFactory", serverPolicyFactory)
                        .put("wspReader", wspReader)
                        .put("ssgKeyStoreManager", ssgKeyStoreManager)
                        .put("rbacServices", rbacServices)
                        .put("clusterInfoManager", clusterInfoManager)
                        .put("policyManager", policyManager)
                        .put("encapsulatedAssertionConfigManager", encapsulatedAssertionConfigManager)
                        .put("jdbcConnectionManager", jdbcConnectionManager)
                        .put("cassandraConnectionManager", cassandraConnectionManager)
                        .put("stashManagerFactory", stashManagerFactory)
                        .put("clusterPropertyManager", clusterPropertyManager)
                        .put("trustedCertManager", trustedCertManager)
                        .unmodifiableMap()
        );
        when(config.getProperty("clusterHost", "")).thenReturn(LOCALHOST);
    }

    @Test
    public void buildEnrollmentPostBody() throws Exception {
        PortalBootstrapManager spyManager = spy(manager);

        when(clusterInfoManager.retrieveClusterStatus()).thenReturn(Collections.singletonList(new ClusterNodeInfo()));
        final Policy otkPolicy = new Policy(PolicyType.INCLUDE_FRAGMENT, OTK_CLIENT_DB_GET_POLICY, "</xml>", false);
        otkPolicy.setGoid(OTK_POLICY_GOID);
        when(policyManager.findByUniqueName(OTK_CLIENT_DB_GET)).thenReturn(otkPolicy);
        final EncapsulatedAssertionConfig otkEncass = new EncapsulatedAssertionConfig();
        otkEncass.setGoid(OTK_ENCASS_GOID);
        when(encapsulatedAssertionConfigManager.findByUniqueName(OTK_REQUIRE_OAUTH_2_0_TOKEN)).thenReturn(otkEncass);
        final JdbcConnection otkJdbc = new JdbcConnection();
        otkJdbc.setGoid(OTK_JDBC_GOID);
        when(jdbcConnectionManager.findByUniqueName(OAUTH)).thenReturn(otkJdbc);

        doReturn(OTK_VERSION).when(spyManager).getOtkVersion(user);

        final byte[] postBody = spyManager.buildEnrollmentPostBody
                (user);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonNode = mapper.readTree(postBody);
        assertEquals(LOCALHOST, jsonNode.get("cluster_name").getTextValue());
        assertNotNull(jsonNode.get("version"));
        assertNotNull(jsonNode.get("build_info"));
        assertEquals(USER_ID, jsonNode.get("adminuser_id").getTextValue());
        assertEquals(ID_PROV_GOID.toString(), jsonNode.get("adminuser_providerid").getTextValue());
        assertEquals(OTK_POLICY_GOID.toString(), jsonNode.get(OTK_CLIENT_DB_GET_POLICY).getTextValue());
        assertEquals(OTK_ENCASS_GOID.toString(), jsonNode.get(OTK_REQUIRE_OAUTH_2_TOKEN_ENCASS).getTextValue());
        assertEquals(OTK_JDBC_GOID.toString(), jsonNode.get(OTK_JDBC_CONN).getTextValue());
        assertEquals(OTK_VERSION, jsonNode.get(manager.OTK_VERSION).getTextValue());
        assertEquals("1", jsonNode.get("node_count").getTextValue());
        assertNotNull(jsonNode.get("nodeInfo"));
    }

    @Test
    public void testGetOtkEntities() throws Exception {
        Policy otkPolicy = mock(Policy.class);
        when(policyManager.findByUniqueName(OTK_CLIENT_DB_GET)).thenReturn(otkPolicy);

        EncapsulatedAssertionConfig otkEncass = mock(EncapsulatedAssertionConfig.class);
        when(encapsulatedAssertionConfigManager.findByUniqueName(OTK_REQUIRE_OAUTH_2_0_TOKEN)).thenReturn(otkEncass);

        JdbcConnection otkJdbc = mock(JdbcConnection.class);
        when(jdbcConnectionManager.findByUniqueName(OAUTH)).thenReturn(otkJdbc);

        Triple<Policy,EncapsulatedAssertionConfig,JdbcConnection> response = manager.getOtkEntities();

        assertSame(otkPolicy, response.left);
        assertSame(otkEncass, response.middle);
        assertSame(otkJdbc, response.right);
    }

    @Test
    public void testGetOtkEntitiesWithCassandra() throws Exception {
        Policy otkPolicy = mock(Policy.class);
        when(policyManager.findByUniqueName(OTK_CLIENT_DB_GET)).thenReturn(null);
        when(policyManager.findByUniqueName(OTK_CLIENT_NOSQL_GET)).thenReturn(otkPolicy);

        EncapsulatedAssertionConfig otkEncass = mock(EncapsulatedAssertionConfig.class);
        when(encapsulatedAssertionConfigManager.findByUniqueName(OTK_REQUIRE_OAUTH_2_0_TOKEN)).thenReturn(otkEncass);

        when(jdbcConnectionManager.findByUniqueName(OAUTH)).thenReturn(null);
        CassandraConnectionHolder connectionHolder = mock(CassandraConnectionHolder.class);
        when(cassandraConnectionManager.getConnection(OAUTH_CASSANDRA)).thenReturn(connectionHolder);
        when(connectionHolder.getCassandraConnectionEntity()).thenReturn(mock(CassandraConnection.class));

        Triple<Policy,EncapsulatedAssertionConfig,JdbcConnection> response = manager.getOtkEntities();

        assertSame(otkPolicy, response.left);
        assertSame(otkEncass, response.middle);
        assertNull(response.right);
    }

    @Test(expected = IOException.class)
    public void testGetOtkEntitiesWithNoPolicy() throws Exception {
        when(policyManager.findByUniqueName(OTK_CLIENT_DB_GET)).thenReturn(null);
        when(policyManager.findByUniqueName(OTK_CLIENT_NOSQL_GET)).thenReturn(null);
        manager.getOtkEntities();
    }

    @Test(expected = IOException.class)
    public void testGetOtkEntitiesWithNoEncapsulatedAssertionConfig() throws Exception {
        when(policyManager.findByUniqueName(OTK_CLIENT_DB_GET)).thenReturn(mock(Policy.class));

        when(encapsulatedAssertionConfigManager.findByUniqueName(OTK_REQUIRE_OAUTH_2_0_TOKEN)).thenReturn(null);

        manager.getOtkEntities();
    }

    @Test(expected = IOException.class)
    public void testGetOtkEntitiesWithNoJdbcConnectionAndNoCassandraHolder() throws Exception {
        Policy otkPolicy = mock(Policy.class);
        when(policyManager.findByUniqueName(OTK_CLIENT_DB_GET)).thenReturn(null);
        when(policyManager.findByUniqueName(OTK_CLIENT_NOSQL_GET)).thenReturn(otkPolicy);

        EncapsulatedAssertionConfig otkEncass = mock(EncapsulatedAssertionConfig.class);
        when(encapsulatedAssertionConfigManager.findByUniqueName(OTK_REQUIRE_OAUTH_2_0_TOKEN)).thenReturn(otkEncass);

        when(jdbcConnectionManager.findByUniqueName(OAUTH)).thenReturn(null);
        CassandraConnectionHolder connectionHolder = mock(CassandraConnectionHolder.class);
        when(cassandraConnectionManager.getConnection(OAUTH_CASSANDRA)).thenReturn(connectionHolder);
        when(connectionHolder.getCassandraConnectionEntity()).thenReturn(null);

        manager.getOtkEntities();
    }

    @Test(expected = IOException.class)
    public void testGetOtkEntitiesWithNoConnection() throws Exception {
        when(policyManager.findByUniqueName(OTK_CLIENT_DB_GET)).thenReturn(mock(Policy.class));

        EncapsulatedAssertionConfig otkEncass = mock(EncapsulatedAssertionConfig.class);
        when(encapsulatedAssertionConfigManager.findByUniqueName(OTK_REQUIRE_OAUTH_2_0_TOKEN)).thenReturn(otkEncass);

        when(jdbcConnectionManager.findByUniqueName(OAUTH)).thenReturn(null);
        when(cassandraConnectionManager.getConnection(OAUTH_CASSANDRA)).thenReturn(null);

        manager.getOtkEntities();
    }

    @Test
    public void testInstallBundleMultipartMessage() throws Exception {
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
        ContentTypeHeader responseContentType = ContentTypeHeader.parseValue("multipart/form-data; charset=ISO-8859-1; boundary=enrollment-bundle-boundary");

        InputStream responseInputStream = mock(InputStream.class);
        ServerAssertion serverAssertion = mock(ServerAssertion.class);
        when(serverPolicyFactory.compilePolicy(null, false)).thenReturn(serverAssertion);
        when(serverAssertion.checkRequest(context)).thenReturn(AssertionStatus.NONE);

        try {
            manager.installBundle(responseInputStream, null, responseContentType, user, context);
        }
        catch (IOException ioe) {
            // expected to fail with restman error message, httpStatus = 200
        }
        assertEquals(context.getVariable("restGatewayMan.action"), "POST");
        assertEquals(context.getVariable("restGatewayMan.uri"), "1.0/solutionKitManagers");
    }

    @Test
    public void testInstallBundleXMLMessage() throws Exception {
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
        ContentTypeHeader responseContentType = ContentTypeHeader.parseValue("plain/xml; charset=UTF-8");
        InputStream responseInputStream = new ByteArrayInputStream("<test></test>".getBytes());

        ServerAssertion serverAssertion = mock(ServerAssertion.class);
        when(serverPolicyFactory.compilePolicy(null, false)).thenReturn(serverAssertion);
        when(serverAssertion.checkRequest(context)).thenReturn(AssertionStatus.NONE);

        try {
            manager.installBundle(responseInputStream, null, responseContentType, user, context);
        }
        catch (IOException ioe) {
            // expected to fail with restman error message, httpStatus = 200
        }
        assertEquals(context.getVariable("restGatewayMan.action"), "PUT");
        assertEquals(context.getVariable("restGatewayMan.uri"), "1.0/bundle");
    }

    @Test
    public void testInstallBundleWithSkarId() throws Exception {
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
        ContentTypeHeader responseContentType = ContentTypeHeader.parseValue("multipart/form-data; charset=ISO-8859-1; boundary=enrollment-bundle-boundary");
        final String skarId = "33b16742-d62d-4095-8f8d-4db707e9ad52";

        InputStream responseInputStream = mock(InputStream.class);
        ServerAssertion serverAssertion = mock(ServerAssertion.class);
        when(serverPolicyFactory.compilePolicy(null, false)).thenReturn(serverAssertion);
        when(serverAssertion.checkRequest(context)).thenReturn(AssertionStatus.NONE);

        try {
            manager.installBundle(responseInputStream, skarId, responseContentType, user, context);
        }
        catch (IOException ioe) {
            // expected to fail with restman error message, httpStatus = 200
        }
        assertEquals(context.getVariable("restGatewayMan.action"), "POST");
        assertEquals(context.getVariable("restGatewayMan.uri"), "1.0/solutionKitManagers?id=33b16742-d62d-4095-8f8d-4db707e9ad52");
    }

    @Test
    public void testIsGatewayEnrolledTrue() throws Exception {
        SsgKeyEntry ssgKeyEntry = SsgKeyEntry.createDummyEntityForAuditing(Goid.DEFAULT_GOID, "testSsgEntry");
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("portalman", PersistentEntity.DEFAULT_GOID)).thenReturn(ssgKeyEntry);
        when(clusterPropertyManager.getProperty("portal.config.node.id")).thenReturn("6d5a711f-66f5-11e5-887b-000c2971a55f");
        when(clusterPropertyManager.getProperty("portal.config.pssg.host")).thenReturn("abc.ca.com");
        when(clusterPropertyManager.getProperty("portal.config.name")).thenReturn("tenant1");
        when(clusterPropertyManager.getProperty("portal.bundle.version")).thenReturn("12345");
        assertTrue(manager.isGatewayEnrolled());
    }

    @Test
    public void testIsGatewayEnrolledFalse() throws Exception {
        SsgKeyEntry ssgKeyEntry = SsgKeyEntry.createDummyEntityForAuditing(Goid.DEFAULT_GOID, "testSsgEntry");
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("portalman", PersistentEntity.DEFAULT_GOID)).thenReturn(ssgKeyEntry);
        when(clusterPropertyManager.getProperty("portal.config.node.id")).thenReturn(null);
        when(clusterPropertyManager.getProperty("portal.config.pssg.host")).thenReturn("abc.ca.com");
        when(clusterPropertyManager.getProperty("portal.config.name")).thenReturn("tenant1");
        when(clusterPropertyManager.getProperty("portal.bundle.version")).thenReturn("12345");
        assertTrue(!manager.isGatewayEnrolled());
    }


    @Test
    public void testGetOTKVersionRequest() throws Exception{
        ServerAssertion serverAssertion = mock(ServerAssertion.class);
        PolicyEnforcementContext policyEnforcementContext = mock(PolicyEnforcementContext.class);

        when(serverPolicyFactory.compilePolicy(null, false)).thenReturn(serverAssertion);
        when(serverAssertion.checkRequest(policyEnforcementContext)).thenReturn(AssertionStatus.BAD_RESPONSE);

        try {
            manager.getOtkVersion(user);
        }
        catch (IOException ioe) {
            // expected to fail with restman error message, httpStatus = 200
            assertTrue(ioe.getMessage().contains("RESTMAN failed"));
        }

    }

}
