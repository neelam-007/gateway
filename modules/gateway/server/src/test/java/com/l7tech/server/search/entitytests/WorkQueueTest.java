package com.l7tech.server.search.entitytests;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.*;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.processors.DependencyTestBaseClass;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

public class WorkQueueTest extends DependencyTestBaseClass {

    AtomicLong idCount = new AtomicLong(1);

    @Test
    public void test() throws FindException, CannotRetrieveDependenciesException {

        SecurityZone securityZone = new SecurityZone();
        Goid securityZoneGoid = new Goid(0, idCount.getAndIncrement());
        securityZone.setGoid(securityZoneGoid);
        mockEntity(securityZone, new EntityHeader(securityZoneGoid, EntityType.SECURITY_ZONE, null, null));


        WorkQueue wq = new WorkQueue();
        final Goid wqOid = new Goid(0, idCount.getAndIncrement());
        wq.setGoid(wqOid);
        wq.setName("Work Queue 1");
        wq.setMaxQueueSize(10);
        wq.setThreadPoolMax(5);
        wq.setRejectPolicy(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY);
        wq.setSecurityZone(securityZone);

        final EntityHeader wqHeader = new EntityHeader(wqOid, EntityType.WORK_QUEUE, null, null);

        mockEntity(wq, wqHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(wqHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(wqOid, new Goid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.WORK_QUEUE, (result.getDependent()).getDependencyType().getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(1, result.getDependencies().size());
        Assert.assertEquals(securityZoneGoid.toString(), ((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityHeader().getStrId());
        Assert.assertEquals(EntityType.SECURITY_ZONE, (result.getDependencies().get(0).getDependent()).getDependencyType().getEntityType());
    }
}
