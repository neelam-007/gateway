/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.MessagesUtil;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.cluster.ClusterPropertyListener;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public class AuditLevelDetailFilterTest {

    private static MockClusterPropertyManager clusterPropManager;
    private static AuditLevelDetailFilter filter;
    private static ClusterPropertyCache cache;
    
    static {
        clusterPropManager = new MockClusterPropertyManager();
        cache = new ClusterPropertyCache();
        cache.setClusterPropertyManager(clusterPropManager);
        filter = new AuditLevelDetailFilter(cache);
        try {
            filter.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        cache.setClusterPropertyListener(new ClusterPropertyListener() {
            @Override
            public void clusterPropertyChanged(ClusterProperty clusterPropertyOld, ClusterProperty clusterPropertyNew) {
                filter.propertyChange(new PropertyChangeEvent(cache, clusterPropertyNew.getName(), clusterPropertyOld, clusterPropertyNew));
            }

            @Override
            public void clusterPropertyDeleted(ClusterProperty clusterProperty) {
                filter.propertyChange(new PropertyChangeEvent(cache, clusterProperty.getName(), clusterProperty, null));
            }
        });
    }

    @Before
    public void cleanUpAfterTests() throws Exception {
        final Collection<EntityHeader> headers = clusterPropManager.findAllHeaders();
        for (EntityHeader header : headers) {
            clusterPropManager.delete(header.getOid());
            fireEvent(header.getOid(), true);
        }
    }

    @Test
    public void testNever() throws Exception {
        final ClusterProperty property = clusterPropManager.putProperty("audit.auditDetailExcludeList", mixOfLevels);
        fireEvent(property.getOid());
        for (AuditDetailMessage auditDetailMessage : messageList) {
            final Level level = filter.filterLevelForAuditDetailMessage(auditDetailMessage.getId(), auditDetailMessage.getLevel());
            Assert.assertNull(level);
        }
    }

    @Test
    public void testSevere() throws Exception {
        testAuditLevelsChanged(Level.SEVERE);
    }

    @Test
    public void testWarning() throws Exception {
        testAuditLevelsChanged(Level.WARNING);
    }

    @Test
    public void testInfo() throws Exception {
        testAuditLevelsChanged(Level.INFO);
    }

    @Test
    public void testConfig() throws Exception {
        testAuditLevelsChanged(Level.CONFIG);
    }

    @Test
    public void testFine() throws Exception {
        testAuditLevelsChanged(Level.FINE);
    }

    @Test
    public void testFiner() throws Exception {
        testAuditLevelsChanged(Level.FINER);
    }

    @Test
    public void testFinest() throws Exception {
        testAuditLevelsChanged(Level.FINEST);
    }

    @Test
    public void testNoChange() throws Exception{
        final ClusterProperty property = clusterPropManager.putProperty("audit.setDetailLevel.FINE", mixOfLevels);
        fireEvent(property.getOid());
        final Level level = filter.filterLevelForAuditDetailMessage(4104, Level.INFO);
        Assert.assertEquals("Level should not have been changed", Level.INFO, level);
    }

    private void testAuditLevelsChanged(Level expectedLevel) throws Exception {
        final ClusterProperty property = clusterPropManager.putProperty("audit.setDetailLevel." + expectedLevel.getName(), mixOfLevels);
        fireEvent(property.getOid());
        for (AuditDetailMessage auditDetailMessage : messageList) {
            final Level level = filter.filterLevelForAuditDetailMessage(auditDetailMessage.getId(), auditDetailMessage.getLevel());
            Assert.assertEquals("Level should have been changed", expectedLevel, level);
        }
    }

    private void fireEvent(long cpOid){
        fireEvent(cpOid, false);
    }

    private void fireEvent(long cpOid, boolean isDelete){
        EntityInvalidationEvent event = new EntityInvalidationEvent(this, ClusterProperty.class, new long[]{cpOid}, new char[]{(isDelete)?'D': 'U'});
        cache.onApplicationEvent(event);
    }

    //2200 = SEVERE
    //-5  = WARNING
    //7245 = INFO
    // None
    // 3007 = FINE
    // 3214 = FINER
    //-1 = FINEST
    private String mixOfLevels = "2200 -5 7245 3007 3214 -1";

    private List<AuditDetailMessage> messageList = Arrays.asList(
            MessagesUtil.getAuditDetailMessageById(2200),
            MessagesUtil.getAuditDetailMessageById(-5),
            MessagesUtil.getAuditDetailMessageById(7245),
            MessagesUtil.getAuditDetailMessageById(3007),
            MessagesUtil.getAuditDetailMessageById(3214),
            MessagesUtil.getAuditDetailMessageById(-1));
}
