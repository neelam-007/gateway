/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.MessagesUtil;
import com.l7tech.util.Config;
import com.l7tech.util.MockConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class AuditLevelDetailFilterTest {

    private static AuditLevelDetailFilter filter;
    private Properties props;

    @Before
    public void setUp() throws Exception {
        props = new Properties();
        final Config config = new MockConfig(props);
        filter = new AuditLevelDetailFilter(config);
        filter.afterPropertiesSet();
    }

    @Test
    public void testNever() throws Exception {
        props.setProperty("audit.auditDetailExcludeList", mixOfLevels);
        filter.propertyChange(new PropertyChangeEvent(this, "audit.auditDetailExcludeList", null, mixOfLevels));

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
        props.setProperty("audit.setDetailLevel.FINE", mixOfLevels);
        filter.propertyChange(new PropertyChangeEvent(this, "audit.setDetailLevel.FINE", null, mixOfLevels));
        final Level level = filter.filterLevelForAuditDetailMessage(4104, Level.INFO);
        Assert.assertEquals("Level should not have been changed", Level.INFO, level);
    }

    private void testAuditLevelsChanged(Level expectedLevel) throws Exception {
        props.setProperty("audit.setDetailLevel." + expectedLevel.getName(), mixOfLevels);
        filter.propertyChange(new PropertyChangeEvent(this, "audit.setDetailLevel." + expectedLevel.getName(), null, mixOfLevels));
        for (AuditDetailMessage auditDetailMessage : messageList) {
            final Level level = filter.filterLevelForAuditDetailMessage(auditDetailMessage.getId(), auditDetailMessage.getLevel());
            Assert.assertEquals("Level should have been changed", expectedLevel, level);
        }
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
