package com.l7tech.server;

import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.audit.AuditRecordManager;
import org.hibernate.HibernateException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityFinderImplTest {
    private EntityFinderImpl finder;
    @Mock
    private AuditRecordManager auditRecordManager;
    @Mock
    private HibernateTemplate hibernateTemplate;
    private List<Entity> entities;

    @Before
    public void setup() {
        finder = new EntityFinderImpl();
        finder.setAuditRecordManager(auditRecordManager);
        finder.setHibernateTemplate(hibernateTemplate);
        entities = new ArrayList<>();
    }

    @Test
    public void findAuditRecordHeaderDelegatesToManager() throws Exception {
        final AuditRecordHeader auditRecordHeader = new AuditRecordHeader(1234L, "Name", "Description", null, null, "nodeId", 1234L, Level.INFO, 0);
        finder.find(auditRecordHeader);
        verify(auditRecordManager).findByHeader(auditRecordHeader);
    }

    @Test
    public void findByEntityTypeAndSecurityZoneOid() throws Exception {
        entities.add(new PublishedService());
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(entities);
        final Collection<Entity> found = finder.findByEntityTypeAndSecurityZoneOid(EntityType.SERVICE, 1234L);
        assertEquals(entities, found);
    }

    @Test(expected = IllegalArgumentException.class)
    public void findByEntityTypeAndSecurityZoneOidNotZoneable() throws Exception {
        finder.findByEntityTypeAndSecurityZoneOid(EntityType.ANY, 1234L);
    }

    @Test(expected = FindException.class)
    public void findByEntityTypeAndSecurityZoneOidHibernateException() throws Exception {
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenThrow(new HibernateException("mocking exception"));
        finder.findByEntityTypeAndSecurityZoneOid(EntityType.SSG_KEY_ENTRY, 1234L);
    }
}
