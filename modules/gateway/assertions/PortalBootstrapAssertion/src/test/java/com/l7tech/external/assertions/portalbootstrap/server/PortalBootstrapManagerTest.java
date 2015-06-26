package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.util.ApplicationContextInjector;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * @author ALEE, 6/25/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalBootstrapManagerTest {
    private static final Goid ID_PROV_GOID = new Goid(0, 1);
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
            } else{
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
                        .unmodifiableMap()
        );
        when(config.getProperty("clusterHost", "")).thenReturn(LOCALHOST);
    }

    @Test
    public void buildEnrollmentPostBody() throws Exception {
        when(clusterInfoManager.retrieveClusterStatus()).thenReturn(Collections.singletonList(new ClusterNodeInfo()));
        final byte[] postBody = PortalBootstrapManager.getInstance().buildEnrollmentPostBody
                (user, OTK_CONN_ID, OTK_CONN_NAME);

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonNode = mapper.readTree(postBody);
        assertEquals(LOCALHOST, jsonNode.get("cluster_name").getTextValue());
        assertNotNull(jsonNode.get("version"));
        assertNotNull(jsonNode.get("build_info"));
        assertEquals(USER_ID, jsonNode.get("adminuser_id").getTextValue());
        assertEquals(ID_PROV_GOID.toString(), jsonNode.get("adminuser_providerid").getTextValue());
        assertEquals(OTK_CONN_ID, jsonNode.get("otkJdbcConnectionId").getTextValue());
        assertEquals(OTK_CONN_NAME, jsonNode.get("otkJdbcConnectionName").getTextValue());
        assertEquals("1", jsonNode.get("node_count").getTextValue());
        assertNotNull(jsonNode.get("nodeInfo"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void enrollWithPortalNullOtkConnectionId() throws Exception {
        Subject.doAs(new Subject(true, Collections.singleton(user), Collections.emptySet(), Collections.emptySet()), new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    PortalBootstrapManager.getInstance().enrollWithPortal(
                            "https://tenant4.hybrid-pssg.l7tech.com:9446/enroll/tenant4?sckh=NXxvnw7kgxzziBHmUw5eFdA63lyCcC99fElKRxuj4qw&token=57281b1e-1b73-11e5-a87e-000c299656bb",
                            null, OTK_CONN_NAME);
                    fail("Expected IllegalArgumentException");
                } catch (final IllegalArgumentException e) {
                    // expected
                    assertEquals("otkConnectionId is required", e.getMessage());
                    throw e;
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }

    @Test(expected=IllegalArgumentException.class)
    public void enrollWithPortalEmptyOtkConnectionId() throws Exception {
        Subject.doAs(new Subject(true, Collections.singleton(user), Collections.emptySet(), Collections.emptySet()), new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    PortalBootstrapManager.getInstance().enrollWithPortal(
                            "https://tenant4.hybrid-pssg.l7tech.com:9446/enroll/tenant4?sckh=NXxvnw7kgxzziBHmUw5eFdA63lyCcC99fElKRxuj4qw&token=57281b1e-1b73-11e5-a87e-000c299656bb",
                            " ", OTK_CONN_NAME);
                    fail("Expected IllegalArgumentException");
                } catch (final IllegalArgumentException e) {
                    // expected
                    assertEquals("otkConnectionId is required", e.getMessage());
                    throw e;
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }

    @Test(expected=IllegalArgumentException.class)
    public void enrollWithPortalNullOtkConnectionName() throws Exception {
        Subject.doAs(new Subject(true, Collections.singleton(user), Collections.emptySet(), Collections.emptySet()), new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    PortalBootstrapManager.getInstance().enrollWithPortal(
                            "https://tenant4.hybrid-pssg.l7tech.com:9446/enroll/tenant4?sckh=NXxvnw7kgxzziBHmUw5eFdA63lyCcC99fElKRxuj4qw&token=57281b1e-1b73-11e5-a87e-000c299656bb",
                            OTK_CONN_ID, null);
                    fail("Expected IllegalArgumentException");
                } catch (final IllegalArgumentException e) {
                    // expected
                    assertEquals("otkConnectionName is required", e.getMessage());
                    throw e;
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }

    @Test(expected=IllegalArgumentException.class)
    public void enrollWithPortalEmptyOtkConnectionName() throws Exception {
        Subject.doAs(new Subject(true, Collections.singleton(user), Collections.emptySet(), Collections.emptySet()), new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    PortalBootstrapManager.getInstance().enrollWithPortal(
                            "https://tenant4.hybrid-pssg.l7tech.com:9446/enroll/tenant4?sckh=NXxvnw7kgxzziBHmUw5eFdA63lyCcC99fElKRxuj4qw&token=57281b1e-1b73-11e5-a87e-000c299656bb",
                            OTK_CONN_ID, " ");
                    fail("Expected IllegalArgumentException");
                } catch (final IllegalArgumentException e) {
                    // expected
                    assertEquals("otkConnectionName is required", e.getMessage());
                    throw e;
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }
}
