package com.l7tech.server;

import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.*;
import com.l7tech.server.audit.AuditRecordManager;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
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
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityFinderImplTest {
    private static final Goid GOID = new Goid(0,1234L);
    private static final Goid ZONE_GOID = new Goid(0,1111L);
    private static final String NAME = "test";
    private EntityFinderImpl finder;
    @Mock
    private AuditRecordManager auditRecordManager;
    @Mock
    private HibernateTemplate hibernateTemplate;
    @Mock
    private Session session;
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private ClassMetadata metadata;
    @Mock
    private List<Entity> entities;
    private SecurityZone zone;

    @Before
    public void setup() {
        finder = new EntityFinderImpl();
        finder.setAuditRecordManager(auditRecordManager);
        finder.setHibernateTemplate(hibernateTemplate);
        entities = new ArrayList<>();
        zone = new SecurityZone();
        zone.setGoid(ZONE_GOID);

        when(hibernateTemplate.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getClassMetadata(any(Class.class))).thenReturn(metadata);
    }

    @Test
    public void findAuditRecordHeaderDelegatesToManager() throws Exception {
        final AuditRecordHeader auditRecordHeader = new AuditRecordHeader(new Goid(3634,1234L), "Name", "Description", null, null, "nodeId", 1234L, Level.INFO, 0);
        finder.find(auditRecordHeader);
        verify(auditRecordManager).findByHeader(auditRecordHeader);
    }

    @Test
    public void findByEntityTypeAndSecurityZoneGoid() throws Exception {
        final PublishedService service = new PublishedService();
        service.setGoid(GOID);
        service.setName(NAME);
        service.setSecurityZone(zone);
        entities.add(service);
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(entities);

        final Collection<EntityHeader> found = finder.findByEntityTypeAndSecurityZoneGoid(EntityType.SERVICE, new Goid(0,1234L));

        assertEquals(1, found.size());
        final EntityHeader header = found.iterator().next();
        assertEquals(ZONE_GOID, ((HasSecurityZoneGoid)header).getSecurityZoneGoid());
        assertEquals(GOID, header.getGoid());
        assertEquals(NAME, header.getName());
        assertEquals(EntityType.SERVICE, header.getType());
        assertEquals(NAME, header.getDescription());
    }

    @Test
    public void findByGoidEntityTypeAndSecurityZoneGoid() throws Exception {
        final JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setGoid(GOID);
        jdbcConnection.setName(NAME);
        jdbcConnection.setSecurityZone(zone);
        entities.add(jdbcConnection);
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(entities);

        final Collection<EntityHeader> found = finder.findByEntityTypeAndSecurityZoneGoid(EntityType.JDBC_CONNECTION, new Goid(0,1234L));

        assertEquals(1, found.size());
        final EntityHeader header = found.iterator().next();
        assertEquals(ZONE_GOID, ((HasSecurityZoneGoid)header).getSecurityZoneGoid());
        assertEquals(GOID, header.getGoid());
        assertEquals(NAME, header.getName());
        assertEquals(EntityType.JDBC_CONNECTION, header.getType());
    }

    @Test
    public void findByEntityTypeAndSecurityZoneGoidNotNamedEntity() throws Exception {
        final PublishedServiceAlias alias = new PublishedServiceAlias(new PublishedService(), null);
        alias.setGoid(GOID);
        alias.setSecurityZone(zone);
        entities.add(alias);
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(entities);

        final Collection<EntityHeader> found = finder.findByEntityTypeAndSecurityZoneGoid(EntityType.SERVICE_ALIAS, new Goid(0,1234L));

        assertEquals(1, found.size());
        final EntityHeader header = found.iterator().next();
        assertEquals(ZONE_GOID, ((HasSecurityZoneGoid)header).getSecurityZoneGoid());
        assertEquals(GOID, header.getGoid());
        assertNull(header.getName());
        assertEquals(EntityType.SERVICE_ALIAS, header.getType());
        assertNull(header.getDescription());
    }

    @Test(expected = IllegalArgumentException.class)
    public void findByEntityTypeAndSecurityZoneGoidNotZoneable() throws Exception {
        finder.findByEntityTypeAndSecurityZoneGoid(EntityType.ANY, new Goid(0,1234L));
    }

    @Test(expected = FindException.class)
    public void findByEntityTypeAndSecurityZoneGoidHibernateException() throws Exception {
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenThrow(new HibernateException("mocking exception"));
        finder.findByEntityTypeAndSecurityZoneGoid(EntityType.SSG_KEY_ENTRY, new Goid(0,1234L));
    }
}
