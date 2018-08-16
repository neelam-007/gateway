package com.l7tech.server.cluster;

import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterNodeSharedInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

public class LocalClusterInfoServiceTest {

    private LocalClusterInfoService localClusterInfoService;

    @Before
    public void setup() {
        localClusterInfoService = new LocalClusterInfoService();
    }

    @Test
    public void givenLocalClusterInfoService_whenGetActiveNodes_OneNodeReturned() {
        Collection<ClusterNodeSharedInfo> collection = localClusterInfoService.getActiveNodes();
        Assert.assertEquals(1, collection.size());
    }
}
