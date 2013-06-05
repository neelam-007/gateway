package com.l7tech.server;

import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.audit.AuditRecordManager;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.PropertyProjection;
import org.hibernate.metadata.ClassMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
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
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityFinderImplTest {
    private static final long OID = 1234L;
    private static final Long ZONE_OID = 1111L;
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
    private Criteria criteria;
    private List<Entity> entities;
    private List<String> propertyNames;
    private List queryResults;
    private SecurityZone zone;

    @Before
    public void setup() {
        finder = new EntityFinderImpl();
        finder.setAuditRecordManager(auditRecordManager);
        finder.setHibernateTemplate(hibernateTemplate);
        entities = new ArrayList<>();
        propertyNames = new ArrayList<>();
        queryResults = new ArrayList();
        zone = new SecurityZone();
        zone.setOid(ZONE_OID);

        when(hibernateTemplate.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getClassMetadata(any(Class.class))).thenReturn(metadata);
        when(session.createCriteria(any(Class.class))).thenReturn(criteria);
    }

    @Test
    public void findAuditRecordHeaderDelegatesToManager() throws Exception {
        final AuditRecordHeader auditRecordHeader = new AuditRecordHeader(1234L, "Name", "Description", null, null, "nodeId", 1234L, Level.INFO, 0);
        finder.find(auditRecordHeader);
        verify(auditRecordManager).findByHeader(auditRecordHeader);
    }

    @Test
    public void findByEntityTypeAndSecurityZoneOid() throws Exception {
        final PublishedService service = new PublishedService();
        service.setOid(OID);
        service.setName(NAME);
        service.setSecurityZone(zone);
        entities.add(service);
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(entities);

        final Collection<ZoneableEntityHeader> found = finder.findByEntityTypeAndSecurityZoneOid(EntityType.SERVICE, 1234L);

        assertEquals(1, found.size());
        final ZoneableEntityHeader header = found.iterator().next();
        assertEquals(ZONE_OID, header.getSecurityZoneOid());
        assertEquals(OID, header.getOid());
        assertEquals(NAME, header.getName());
        assertEquals(EntityType.SERVICE, header.getType());
        assertNull(header.getDescription());
    }

    @Test
    public void findByEntityTypeAndSecurityZoneOidNotNamedEntity() throws Exception {
        final PublishedServiceAlias alias = new PublishedServiceAlias(new PublishedService(), null);
        alias.setOid(OID);
        alias.setSecurityZone(zone);
        entities.add(alias);
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(entities);

        final Collection<ZoneableEntityHeader> found = finder.findByEntityTypeAndSecurityZoneOid(EntityType.SERVICE_ALIAS, 1234L);

        assertEquals(1, found.size());
        final ZoneableEntityHeader header = found.iterator().next();
        assertEquals(ZONE_OID, header.getSecurityZoneOid());
        assertEquals(OID, header.getOid());
        assertNull(header.getName());
        assertEquals(EntityType.SERVICE_ALIAS, header.getType());
        assertNull(header.getDescription());
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

    @Test
    public void findHeaderNonZoneableEntity() throws Exception {
        final Role role = new Role();
        role.setOid(OID);
        role.setName("test");
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(role);

        final EntityHeader header = finder.findHeader(EntityType.RBAC_ROLE, OID);
        assertFalse(header instanceof ZoneableEntityHeader);
        assertEquals(OID, header.getOid());
        assertEquals(EntityType.RBAC_ROLE, header.getType());
        assertEquals("test", header.getName());
        assertNull(header.getDescription());
    }

    @Test
    public void findHeaderZoneableEntity() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        policy.setOid(OID);
        final SecurityZone zone = new SecurityZone();
        zone.setOid(ZONE_OID);
        policy.setSecurityZone(zone);
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(policy);

        final EntityHeader header = finder.findHeader(EntityType.POLICY, OID);
        assertTrue(header instanceof ZoneableEntityHeader);
        assertEquals(ZONE_OID, ((ZoneableEntityHeader) header).getSecurityZoneOid());
        assertEquals(OID, header.getOid());
        assertEquals(EntityType.POLICY, header.getType());
        assertEquals("test", header.getName());
        assertNull(header.getDescription());
    }

    @Test
    public void findHeaderZoneableEntityNullZone() throws Exception {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        policy.setOid(OID);
        policy.setSecurityZone(null);
        when(hibernateTemplate.execute(any(HibernateCallback.class))).thenReturn(policy);

        final EntityHeader header = finder.findHeader(EntityType.POLICY, OID);
        assertTrue(header instanceof ZoneableEntityHeader);
        assertNull(((ZoneableEntityHeader) header).getSecurityZoneOid());
    }

    @Test
    public void readOnlyFindAllCallbackHasNameAndZone() throws Exception {
        queryResults.add(new Object[]{OID, "test", ZONE_OID});
        propertyNames.add("name");
        when(metadata.getPropertyNames()).thenReturn(propertyNames.toArray(new String[propertyNames.size()]));
        when(criteria.list()).thenReturn(queryResults);
        final EntityFinderImpl.ReadOnlyFindAll callback = finder.new ReadOnlyFindAll(Policy.class, EntityType.POLICY);

        final EntityHeaderSet<EntityHeader> headers = callback.doInHibernateReadOnly(session);

        verify(criteria).setProjection(argThat(new ProjectionListMatcher(true, true)));
        assertEquals(1, headers.size());
        final Object[] headersArray = headers.toArray();
        assertTrue(headersArray[0] instanceof ZoneableEntityHeader);
        final ZoneableEntityHeader header = (ZoneableEntityHeader) headersArray[0];
        assertEquals(OID, header.getOid());
        assertEquals("test", header.getName());
        assertEquals(ZONE_OID, header.getSecurityZoneOid());
        assertEquals(EntityType.POLICY, header.getType());
        assertNull(header.getDescription());
    }

    @Test
    public void readOnlyFindAllCallbackHasNameButNoZone() throws Exception {
        queryResults.add(new Object[]{OID, "test"});
        propertyNames.add("name");
        when(metadata.getPropertyNames()).thenReturn(propertyNames.toArray(new String[propertyNames.size()]));
        when(criteria.list()).thenReturn(queryResults);
        final EntityFinderImpl.ReadOnlyFindAll callback = finder.new ReadOnlyFindAll(Role.class, EntityType.RBAC_ROLE);

        final EntityHeaderSet<EntityHeader> headers = callback.doInHibernateReadOnly(session);

        verify(criteria).setProjection(argThat(new ProjectionListMatcher(true, false)));
        assertEquals(1, headers.size());
        final Object[] headersArray = headers.toArray();
        assertFalse(headersArray[0] instanceof ZoneableEntityHeader);
        final EntityHeader header = (EntityHeader) headersArray[0];
        assertEquals(OID, header.getOid());
        assertEquals("test", header.getName());
        assertEquals(EntityType.RBAC_ROLE, header.getType());
        assertNull(header.getDescription());
    }

    @Test
    public void readOnlyFindAllCallbackHasZoneButNoName() throws Exception {
        queryResults.add(new Object[]{OID, ZONE_OID});
        when(metadata.getPropertyNames()).thenReturn(propertyNames.toArray(new String[propertyNames.size()]));
        when(criteria.list()).thenReturn(queryResults);
        final EntityFinderImpl.ReadOnlyFindAll callback = finder.new ReadOnlyFindAll(PolicyAlias.class, EntityType.POLICY_ALIAS);

        final EntityHeaderSet<EntityHeader> headers = callback.doInHibernateReadOnly(session);

        verify(criteria).setProjection(argThat(new ProjectionListMatcher(false, true)));
        assertEquals(1, headers.size());
        final Object[] headersArray = headers.toArray();
        assertTrue(headersArray[0] instanceof ZoneableEntityHeader);
        final ZoneableEntityHeader header = (ZoneableEntityHeader) headersArray[0];
        assertEquals(OID, header.getOid());
        assertEquals(String.valueOf(OID), header.getName());
        assertEquals(ZONE_OID, header.getSecurityZoneOid());
        assertEquals(EntityType.POLICY_ALIAS, header.getType());
        assertNull(header.getDescription());
    }

    @Test
    public void readOnlyFindAllCallbackNoNameOrZone() throws Exception {
        queryResults.add(new Object[]{OID});
        when(metadata.getPropertyNames()).thenReturn(propertyNames.toArray(new String[propertyNames.size()]));
        when(criteria.list()).thenReturn(queryResults);
        final EntityFinderImpl.ReadOnlyFindAll callback = finder.new ReadOnlyFindAll(MetricsBin.class, EntityType.METRICS_BIN);

        final EntityHeaderSet<EntityHeader> headers = callback.doInHibernateReadOnly(session);

        verify(criteria).setProjection(argThat(new ProjectionListMatcher(false, false)));
        assertEquals(1, headers.size());
        final Object[] headersArray = headers.toArray();
        assertFalse(headersArray[0] instanceof ZoneableEntityHeader);
        final EntityHeader header = (EntityHeader) headersArray[0];
        assertEquals(OID, header.getOid());
        assertEquals(String.valueOf(OID), header.getName());
        assertEquals(EntityType.METRICS_BIN, header.getType());
        assertNull(header.getDescription());
    }

    private class ProjectionListMatcher extends ArgumentMatcher<ProjectionList> {
        private final boolean expectName;
        private final boolean expectZone;

        private ProjectionListMatcher(final boolean expectName, final boolean expectZone) {
            this.expectName = expectName;
            this.expectZone = expectZone;
        }

        @Override
        public boolean matches(final Object o) {
            boolean match = false;
            if (o instanceof ProjectionList) {
                final ProjectionList pj = (ProjectionList) o;
                assertEquals("oid", ((PropertyProjection) pj.getProjection(0)).getPropertyName());
                if (expectName && expectZone) {
                    assertEquals(3, pj.getLength());
                    assertEquals("name", ((PropertyProjection) pj.getProjection(1)).getPropertyName());
                    assertEquals("securityZone.oid", ((PropertyProjection) pj.getProjection(2)).getPropertyName());
                } else if (expectName) {
                    assertEquals(2, pj.getLength());
                    assertEquals("name", ((PropertyProjection) pj.getProjection(1)).getPropertyName());
                } else if (expectZone) {
                    assertEquals(2, pj.getLength());
                    assertEquals("securityZone.oid", ((PropertyProjection) pj.getProjection(1)).getPropertyName());
                } else {
                    assertEquals(1, pj.getLength());
                }
                match = true;
            }
            return match;
        }
    }
}
