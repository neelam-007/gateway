package com.l7tech.external.assertions.mysqlclusterinfo;

import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterInfoService;
import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterNodeSharedInfo;
import com.l7tech.external.assertions.mysqlclusterinfo.server.MysqlClusterInfoService;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.util.Config;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MysqlClusterInfoServiceTest {

    @Mock
    ClusterInfoManager manager;

    @Mock
    Config serverConfig;

    Collection<ClusterNodeInfo> collection;

    ClusterInfoService clusterInfoService;

    @Before
    public void setUp() throws FindException {
        collection = new ArrayList<>();
        collection.add(new ClusterNodeInfo() {{
            setLastUpdateTimeStamp(Instant.now().toEpochMilli());
            setName("node1");
        }});
        collection.add(new ClusterNodeInfo() {{
            setLastUpdateTimeStamp(Instant.now().toEpochMilli());
            setName("node2");
        }});
        when(manager.retrieveClusterStatus()).thenReturn(collection);
        when(serverConfig.getLongProperty(MysqlClusterInfoService.PARAM_CLUSTER_STATUS_INTERVAL, 8000L)).thenReturn(8000L);
        when(serverConfig.getLongProperty(MysqlClusterInfoService.PARAM_CLUSTER_POLL_INTERVAL, 43000L)).thenReturn(2000L);

        clusterInfoService = new MysqlClusterInfoService(manager, serverConfig);
    }

    @Test
    public void givenClusterHasTwoNodes_whenCacheIsEmpty_twoNodesAreReturned() {
        Collection<ClusterNodeSharedInfo> collection = clusterInfoService.getActiveNodes();
        Assert.assertEquals(2, collection.size());
    }

    @Test
    public void givenClusterHasTwoNodes_whenAllNodesAreRemovedAndCacheIsValid_twoNodesAreReturned() throws InterruptedException, FindException {
        clusterInfoService.getActiveNodes();
        Thread.sleep(1000);
        when(manager.retrieveClusterStatus()).thenReturn(new ArrayList<>());
        Collection<ClusterNodeSharedInfo> collection = clusterInfoService.getActiveNodes();
        Assert.assertEquals(2, collection.size());
    }

    @Test
    public void givenClusterHasTwoNodes_whenAllNodesAreRemovedAndCacheIsInvalid_emptyReturned() throws InterruptedException, FindException {
        Collection<ClusterNodeSharedInfo> collection = clusterInfoService.getActiveNodes();
        Assert.assertEquals(2, collection.size());
        when(manager.retrieveClusterStatus()).thenReturn(new ArrayList<>());
        Thread.sleep(2000L);
        collection = clusterInfoService.getActiveNodes();
        Assert.assertEquals(0, collection.size());
    }
}
