package com.l7tech.server.ems.gateway;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.management.api.node.GatewayApi;
import static org.junit.Assert.*;
import org.junit.*;

import java.util.*;

/**
 *
 */
public class GatewayClusterClientTest {
    private SsgCluster fooCluster;
    private List<GatewayContext> nodeContexts;
    private GatewayApi.EntityInfo blahfolder;

    private SsgNode makeSsgNode() {
        SsgNode sn = new SsgNode();
        sn.setGuid(UUID.randomUUID().toString());
        return sn;
    }

    private GatewayContext makeNodeContext(String host) throws Exception {
        return new MockGatewayContext(UUID.randomUUID().toString(), host, "bob") {
            public GatewayApi getApi() {
                return new GatewayApi() {
                    public Collection<EntityInfo> getEntityInfo(Collection<EntityType> entityTypes) throws GatewayException {
                        return Arrays.asList(
                                blahfolder
                        );
                    }

                    public ClusterInfo getClusterInfo() {
                        return null;
                    }

                    public Collection<GatewayInfo> getGatewayInfo() {
                        return null;
                    }
                };
            }
        };
    }

    public GatewayClusterClientTest() throws Exception {
        blahfolder = new GatewayApi.EntityInfo(EntityType.FOLDER, UUID.randomUUID().toString(), "blahfolder", null, 1);

        fooCluster = new SsgCluster("foo", "test-foo.nowhere.example.com", 8443, null);
        Set<SsgNode> nodes = new LinkedHashSet<SsgNode>();
        nodes.add(makeSsgNode());
        nodes.add(makeSsgNode());
        nodes.add(makeSsgNode());
        fooCluster.setNodes(nodes);

        nodeContexts = new ArrayList<GatewayContext>();
        nodeContexts.add(makeNodeContext("test-foo1.nowhere.example.com"));
        nodeContexts.add(makeNodeContext("test-foo2.nowhere.example.com"));
        nodeContexts.add(makeNodeContext("test-foo3.nowhere.example.com"));
    }

    @Test
    public void testCache() throws Exception {
        GatewayClusterClientImpl cc = new GatewayClusterClientImpl(fooCluster, nodeContexts);
        assertTrue("cluster matches", fooCluster == cc.getCluster());
        assertTrue("results filtered", cc.getEntityInfo(Arrays.asList(EntityType.AUDIT_ADMIN)).isEmpty());
        assertTrue("folder returned", blahfolder == cc.getEntityInfo(Arrays.asList(EntityType.FOLDER)).iterator().next());

        // TODO ensure subsequent lookups cached
        // TODO ensure clearing cache works
    }

    @Test
    public void testFailover() throws Exception {
        // TODO
    }
}
