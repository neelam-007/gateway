package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ems.enterprise.MockSsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.Updated;
import static org.junit.Assert.*;
import org.junit.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class GatewayClusterClientManagerTest {
    private SsgCluster fooCluster;
    private MockSsgClusterManager ssgClusterManager;
    private MockGatewayContextFactory gatewayContextFactory;
    private User userAlice;
    private User userBob;

    private User makeUser(Goid oid, String login) {
        UserBean ub = new UserBean(new Goid(0,-1), login);
        ub.setUniqueIdentifier(Goid.toString(oid));
        return ub;
    }

    private SsgNode makeSsgNode() {
        SsgNode sn = new SsgNode();
        sn.setGuid(UUID.randomUUID().toString());
        return sn;
    }

    public GatewayClusterClientManagerTest() {
        fooCluster = new SsgCluster("foo", "test-foo.nowhere.example.com", 8443, null);
        fooCluster.setTrustStatus(true);
        Set<SsgNode> nodes = new LinkedHashSet<SsgNode>();
        nodes.add(makeSsgNode());
        nodes.add(makeSsgNode());
        nodes.add(makeSsgNode());
        fooCluster.setNodes(nodes);

        ssgClusterManager = new MockSsgClusterManager() {
            @Override
            public SsgCluster findByGuid(String guid) throws FindException {
                if ("fooCluster".equals(guid))
                    return fooCluster;
                return super.findByGuid(guid);
            }
        };
        gatewayContextFactory = new MockGatewayContextFactory();

        userAlice = makeUser(new Goid(0,321), "alice");
        userBob = makeUser(new Goid(0,123), "bob");
    }

    @Test
    public void testReuseCachedAndInvalidateCaches() throws Exception {
        GatewayClusterClientManagerImpl ccm = new GatewayClusterClientManagerImpl(gatewayContextFactory, ssgClusterManager);

        GatewayClusterClient aliceClient = ccm.getGatewayClusterClient("fooCluster", userAlice);
        assertNotNull("client created", aliceClient);

        GatewayClusterClient client2 = ccm.getGatewayClusterClient("fooCluster", userAlice);
        assertTrue("existing client instance reused", aliceClient == client2);

        GatewayClusterClient bobClient = ccm.getGatewayClusterClient("fooCluster", userBob);
        assertNotNull("client created", bobClient);
        assertTrue("each user gets their own client", aliceClient != bobClient);
        
        ccm.onApplicationEvent(new Updated<SsgCluster>(fooCluster, new EntityChangeSet(new String[0], new Object[0], new Object[0])));

        GatewayClusterClient client3 = ccm.getGatewayClusterClient("fooCluster", userBob);
        assertTrue("new client instance created after cache flush due to Updated cluster", client3 != client2);

        ccm.onApplicationEvent(new Deleted<User>(userBob));

        GatewayClusterClient client4 = ccm.getGatewayClusterClient("fooCluster", userBob);
        assertTrue("new client instance created after cache flush due to Deleted user", client4 != client3);

    }
}
