package com.l7tech.server.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.logging.Level;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuditRecordManagerImplTest {
    private static final Goid SERVICE_GOID = new Goid(0,8859);
    private AuditRecordManagerImpl manager;
    private MessageSummaryAuditRecord messageSummary;
    private SecurityZone zone;
    private PublishedService service;
    @Mock
    private HibernateTemplate hibernateTemplate;
    @Mock
    private ServiceCache serviceCache;
    @Mock
    private ApplicationContext applicationContext;

    @Before
    public void setup() {
        manager = new TestableAuditRecorManager();
        manager.setApplicationContext(applicationContext);
        ApplicationContexts.inject(manager, CollectionUtils.<String, Object>mapBuilder().put("serviceCache", serviceCache).map(), false);
        messageSummary = new MessageSummaryAuditRecord(Level.INFO, "node1", "2342345-4545", AssertionStatus.NONE, "3.2.1.1", null, 4833, null, 9483, 200, 232, SERVICE_GOID, "ACMEWarehouse", "listProducts", true, SecurityTokenType.HTTP_BASIC, new Goid(0,-2), "alice", "41123",null);
        zone = new SecurityZone();
        service = new PublishedService();
        service.setSecurityZone(zone);
    }

    @Test
    public void findByPrimaryKeySetsSecurityZoneForMessageSummary() throws Exception {
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(messageSummary);
        when(serviceCache.getCachedService(SERVICE_GOID)).thenReturn(service);
        final MessageSummaryAuditRecord found = (MessageSummaryAuditRecord) manager.findByPrimaryKey(new Goid(0,1L));
        assertEquals(zone, found.getSecurityZone());
    }

    @Test
    public void findByPrimaryKeyNotMessageSummary() throws Exception {
        final SystemAuditRecord systemAuditRecord = new SystemAuditRecord(Level.INFO, "node1", Component.GATEWAY, "test", true, new Goid(0,1234), "user", "userId", "action", "127.0.0.1");
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(systemAuditRecord);
        final AuditRecord found = manager.findByPrimaryKey(new Goid(0,1L));
        assertEquals(systemAuditRecord, found);
        verify(serviceCache, never()).getCachedService(any(Goid.class));
    }

    @Test
    public void findByPrimaryKeyMessageSummaryCachedServiceNotFound() throws Exception {
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(messageSummary);
        when(serviceCache.getCachedService(SERVICE_GOID)).thenReturn(null);
        final MessageSummaryAuditRecord found = (MessageSummaryAuditRecord) manager.findByPrimaryKey(new Goid(0,1L));
        assertNull(found.getSecurityZone());
    }

    private class TestableAuditRecorManager extends AuditRecordManagerImpl {
        private TestableAuditRecorManager() {
            setHibernateTemplate(hibernateTemplate);
        }
    }
}
