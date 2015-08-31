package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.util.ApplicationContextInjector;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static com.l7tech.external.assertions.portalbootstrap.server.PortalBootstrapManager.*;

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
    private Config config;
    private PortalBootstrapManager manager;

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
        ApplicationContexts.inject(manager, CollectionUtils.<String, Object>mapBuilder()
                        .put("serverPolicyFactory", serverPolicyFactory)
                        .put("wspReader", wspReader)
                        .put("ssgKeyStoreManager", ssgKeyStoreManager)
                        .put("rbacServices", rbacServices)
                        .put("clusterInfoManager", clusterInfoManager)
                        .put("policyManager", policyManager)
                        .put("encapsulatedAssertionConfigManager", encapsulatedAssertionConfigManager)
                        .put("jdbcConnectionManager", jdbcConnectionManager)
                        .unmodifiableMap()
        );
        when(config.getProperty("clusterHost", "")).thenReturn(LOCALHOST);
    }

    @Test
    public void buildEnrollmentPostBody() throws Exception {
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

        final byte[] postBody = manager.buildEnrollmentPostBody
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
        assertEquals("1", jsonNode.get("node_count").getTextValue());
        assertNotNull(jsonNode.get("nodeInfo"));
    }
}