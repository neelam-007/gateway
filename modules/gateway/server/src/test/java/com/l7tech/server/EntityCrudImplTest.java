package com.l7tech.server;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EntityCrudImplTest {
    private static final Goid ZONE_GOID = new Goid(0,1234L);
    private EntityCrud entityCrud;
    @Mock
    private EntityFinder entityFinder;
    @Mock
    private GoidEntityManager<Policy, EntityHeader> policyEntityManager;
    @Mock
    private GoidEntityManager<SecurityZone, EntityHeader> zoneEntityManager;
    @Mock
    private GoidEntityManager<PublishedService, EntityHeader> serviceEntityManager;
    @Mock
    private GoidEntityManager<JdbcConnection, EntityHeader> jdbcConnectionEntityManager;
    private List<Serializable> ids;
    private SecurityZone zone;
    private Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
    private Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test2", "test2", false);
    private JdbcConnection jdbcConnection1 = new JdbcConnection();
    private JdbcConnection jdbcConnection2 = new JdbcConnection();
    {
        jdbcConnection1.setName("connection1");
        jdbcConnection2.setName("connection2");
    }
    private PublishedService service = new PublishedService();

    @Before
    public void setup() {
        entityCrud = new EntityCrudImpl(entityFinder,
                new ReadOnlyEntityManager[]{new StubPolicyEntityManager(), new StubSecurityZoneEntityManager(), new StubServiceManager(), new StudJdbcConnectionManager()});
        ids = new ArrayList<>();
        zone = new SecurityZone();
    }

    @Test
    public void setSecurityZoneForEntities() throws Exception {
        ids.add(new Goid(0,1L));
        ids.add(new Goid(0,2L));
        when(zoneEntityManager.findByPrimaryKey(ZONE_GOID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(policy);
        when(policyEntityManager.findByPrimaryKey(new Goid(0,2L))).thenReturn(policy2);

        entityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, ids);
        verify(policyEntityManager).update(policy);
        verify(policyEntityManager).update(policy2);
    }

    @Test
    public void setSecurityZoneForGoidEntities() throws Exception {
        Goid goid1 = new Goid(0,1);
        Goid goid2 = new Goid(0,2);
        ids.add(goid1);
        ids.add(goid2);
        when(zoneEntityManager.findByPrimaryKey(ZONE_GOID)).thenReturn(zone);
        when(jdbcConnectionEntityManager.findByPrimaryKey(goid1)).thenReturn(jdbcConnection1);
        when(jdbcConnectionEntityManager.findByPrimaryKey(goid2)).thenReturn(jdbcConnection2);

        entityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.JDBC_CONNECTION, ids);
        verify(jdbcConnectionEntityManager).update(jdbcConnection1);
        verify(jdbcConnectionEntityManager).update(jdbcConnection2);
    }

    @Test
    public void setSecurityZoneForEntitiesNone() throws Exception {
        entityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, Collections.<Serializable>emptyList());
        verify(zoneEntityManager, never()).findByPrimaryKey(anyLong());
        verify(policyEntityManager, never()).update(any(Policy.class));
    }

    @Test
    public void setSecurityZoneForEntitiesNullZone() throws Exception {
        ids.add(new Goid(0,1L));
        when(policyEntityManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(policy);

        entityCrud.setSecurityZoneForEntities(null, EntityType.POLICY, ids);
        verify(zoneEntityManager, never()).findByPrimaryKey(any(Goid.class));
        verify(policyEntityManager).update(policy);
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesZoneNotFound() throws Exception {
        ids.add(1L);
        when(zoneEntityManager.findByPrimaryKey(ZONE_GOID)).thenReturn(null);
        try {
            entityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, ids);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            assertEquals("Unable to set security zone for entities: Security zone with goid "+new Goid(0,1234).toHexString()+" does not exist", e.getMessage());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesAtLeastOneEntityNotFound() throws Exception {
        ids.add(new Goid(0,1L));
        ids.add(new Goid(0,2L));
        when(zoneEntityManager.findByPrimaryKey(ZONE_GOID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(policy);
        when(policyEntityManager.findByPrimaryKey(new Goid(0,2L))).thenReturn(null);

        try {
            entityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, ids);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            assertEquals("Policy with id "+new Goid(0,2L).toHexString()+" does not exist or is not security zoneable", e.getMessage());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesErrorFindingZone() throws Exception {
        ids.add(1L);
        when(zoneEntityManager.findByPrimaryKey(anyLong())).thenThrow(new FindException("mocking exception"));
        entityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, ids);
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesErrorFindingEntity() throws Exception {
        ids.add(new Goid(0,1L));
        when(zoneEntityManager.findByPrimaryKey(ZONE_GOID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(any(Goid.class))).thenThrow(new FindException("mocking exception"));
        entityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, ids);
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesErrorUpdatingEntity() throws Exception {
        ids.add(new Goid(0,1L));
        when(zoneEntityManager.findByPrimaryKey(ZONE_GOID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(policy);
        doThrow(new UpdateException("mocking exception")).when(policyEntityManager).update(any(Policy.class));
        entityCrud.setSecurityZoneForEntities(ZONE_GOID, EntityType.POLICY, ids);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSecurityZoneForEntitiesEntityTypeNotZoneable() throws Exception {
        ids.add(1L);
        try {
            entityCrud.setSecurityZoneForEntities(null, EntityType.RBAC_ROLE, ids);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            verify(zoneEntityManager, never()).findByPrimaryKey(anyLong());
            verify(policyEntityManager, never()).findByPrimaryKey(anyLong());
            verify(policyEntityManager, never()).update(any(Policy.class));
            throw e;
        }
    }

    @Test
    public void setSecurityZoneForEntitiesMultipleEntityTypes() throws Exception {
        final Map<EntityType, Collection<Serializable>> entities = new HashMap<>();
        entities.put(EntityType.POLICY, Arrays.<Serializable>asList(new Goid(0,1L)));
        entities.put(EntityType.SERVICE, Arrays.<Serializable>asList(new Goid(0,2L)));
        entities.put(EntityType.JDBC_CONNECTION, Arrays.<Serializable>asList(new Goid(0,3)));
        when(zoneEntityManager.findByPrimaryKey(ZONE_GOID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(policy);
        when(serviceEntityManager.findByPrimaryKey(new Goid(0,2L))).thenReturn(service);
        when(jdbcConnectionEntityManager.findByPrimaryKey(new Goid(0,3))).thenReturn(jdbcConnection1);

        entityCrud.setSecurityZoneForEntities(ZONE_GOID, entities);
        verify(zoneEntityManager, times(1)).findByPrimaryKey(ZONE_GOID);
        verify(policyEntityManager).update(policy);
        verify(serviceEntityManager).update(service);
        verify(jdbcConnectionEntityManager).update(jdbcConnection1);
    }

    @Test
    public void setSecurityZoneForEntitiesMultipleEntityTypesNone() throws Exception {
        entityCrud.setSecurityZoneForEntities(ZONE_GOID, Collections.<EntityType, Collection<Serializable>>emptyMap());
        verify(zoneEntityManager, never()).findByPrimaryKey(anyLong());
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesMultipleEntityTypesZoneNotFound() throws Exception {
        final Map<EntityType, Collection<Serializable>> entities = new HashMap<>();
        entities.put(EntityType.POLICY, Arrays.<Serializable>asList(1L));
        entities.put(EntityType.SERVICE, Arrays.<Serializable>asList(2L));
        when(zoneEntityManager.findByPrimaryKey(ZONE_GOID)).thenReturn(null);

        try {
            entityCrud.setSecurityZoneForEntities(ZONE_GOID, entities);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            assertEquals("Unable to set security zone for entities: Security zone with goid "+new Goid(0,1234).toHexString()+" does not exist", e.getMessage());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesMultipleEntityTypesErrorFindingZone() throws Exception {
        final Map<EntityType, Collection<Serializable>> entities = new HashMap<>();
        entities.put(EntityType.POLICY, Arrays.<Serializable>asList(1L));
        entities.put(EntityType.SERVICE, Arrays.<Serializable>asList(2L));
        when(zoneEntityManager.findByPrimaryKey(ZONE_GOID)).thenThrow(new FindException("mocking exception"));
        entityCrud.setSecurityZoneForEntities(ZONE_GOID, entities);
    }

    /**
     * Delegates to the mock.
     */
    private class StubPolicyEntityManager implements ReadOnlyEntityManager<Policy, EntityHeader>, GoidEntityManager<Policy, EntityHeader> {
        @Override
        public Goid save(Policy entity) throws SaveException {
            return policyEntityManager.save(entity);
        }

        @Override
        public Integer getVersion(Goid oid) throws FindException {
            return policyEntityManager.getVersion(oid);
        }

        @Override
        public Map<Goid, Integer> findVersionMap() throws FindException {
            return policyEntityManager.findVersionMap();
        }

        @Override
        public void delete(Policy entity) throws DeleteException {
            policyEntityManager.delete(entity);
        }

        @Override
        public Policy getCachedEntity(Goid o, int maxAge) throws FindException {
            return policyEntityManager.getCachedEntity(o, maxAge);
        }

        @Override
        public Class<? extends Entity> getInterfaceClass() {
            return Policy.class;
        }

        @Override
        public EntityType getEntityType() {
            return EntityType.POLICY;
        }

        @Override
        public String getTableName() {
            return "policy";
        }

        @Override
        public Policy findByUniqueName(String name) throws FindException {
            return policyEntityManager.findByUniqueName(name);
        }

        @Override
        public void delete(Goid oid) throws DeleteException, FindException {
            policyEntityManager.delete(oid);
        }

        @Override
        public void update(Policy entity) throws UpdateException {
            policyEntityManager.update(entity);
        }

        @Override
        public Policy findByHeader(EntityHeader header) throws FindException {
            return policyEntityManager.findByHeader(header);
        }

        @Override
        public Policy findByPrimaryKey(long oid) throws FindException {
            return policyEntityManager.findByPrimaryKey(oid);
        }

        @Override
        public Policy findByPrimaryKey(Goid goid) throws FindException {
            return policyEntityManager.findByPrimaryKey(goid);
        }

        @Override
        public Collection<EntityHeader> findAllHeaders() throws FindException {
            return policyEntityManager.findAllHeaders();
        }

        @Override
        public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
            return policyEntityManager.findAllHeaders(offset, windowSize);
        }

        @Override
        public Collection<Policy> findAll() throws FindException {
            return policyEntityManager.findAll();
        }

        @Override
        public Class<? extends Entity> getImpClass() {
            return Policy.class;
        }
    }

    private class StubSecurityZoneEntityManager implements ReadOnlyEntityManager<SecurityZone, EntityHeader>, GoidEntityManager<SecurityZone, EntityHeader> {

        @Override
        public Goid save(SecurityZone entity) throws SaveException {
            return zoneEntityManager.save(entity);
        }

        @Override
        public Integer getVersion(Goid goid) throws FindException {
            return zoneEntityManager.getVersion(goid);
        }

        @Override
        public Map<Goid, Integer> findVersionMap() throws FindException {
            return zoneEntityManager.findVersionMap();
        }

        @Override
        public void delete(SecurityZone entity) throws DeleteException {
            zoneEntityManager.delete(entity);
        }

        @Override
        public SecurityZone getCachedEntity(Goid o, int maxAge) throws FindException {
            return zoneEntityManager.getCachedEntity(o, maxAge);
        }

        @Override
        public Class<? extends Entity> getInterfaceClass() {
            return SecurityZone.class;
        }

        @Override
        public EntityType getEntityType() {
            return EntityType.SECURITY_ZONE;
        }

        @Override
        public String getTableName() {
            return "security_zone";
        }

        @Override
        public SecurityZone findByUniqueName(String name) throws FindException {
            return zoneEntityManager.findByUniqueName(name);
        }

        @Override
        public void delete(Goid goid) throws DeleteException, FindException {
            zoneEntityManager.delete(goid);
        }

        @Override
        public void update(SecurityZone entity) throws UpdateException {
            zoneEntityManager.update(entity);
        }

        @Override
        public SecurityZone findByHeader(EntityHeader header) throws FindException {
            return zoneEntityManager.findByHeader(header);
        }

        @Override
        public SecurityZone findByPrimaryKey(long oid) throws FindException {
            return zoneEntityManager.findByPrimaryKey(oid);
        }

        @Override
        public SecurityZone findByPrimaryKey(Goid goid) throws FindException {
            return zoneEntityManager.findByPrimaryKey(goid);
        }

        @Override
        public Collection<EntityHeader> findAllHeaders() throws FindException {
            return zoneEntityManager.findAllHeaders();
        }

        @Override
        public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
            return zoneEntityManager.findAllHeaders(offset, windowSize);
        }

        @Override
        public Collection<SecurityZone> findAll() throws FindException {
            return zoneEntityManager.findAll();
        }

        @Override
        public Class<? extends Entity> getImpClass() {
            return SecurityZone.class;
        }
    }

    private class StubServiceManager implements ReadOnlyEntityManager<PublishedService, EntityHeader>, GoidEntityManager<PublishedService, EntityHeader> {

        @Override
        public Goid save(PublishedService entity) throws SaveException {
            return serviceEntityManager.save(entity);
        }

        @Override
        public Integer getVersion(Goid oid) throws FindException {
            return serviceEntityManager.getVersion(oid);
        }

        @Override
        public Map<Goid, Integer> findVersionMap() throws FindException {
            return serviceEntityManager.findVersionMap();
        }

        @Override
        public void delete(PublishedService entity) throws DeleteException {
            serviceEntityManager.delete(entity);
        }

        @Override
        public PublishedService getCachedEntity(Goid o, int maxAge) throws FindException {
            return serviceEntityManager.getCachedEntity(o, maxAge);
        }

        @Override
        public Class<? extends Entity> getInterfaceClass() {
            return PublishedService.class;
        }

        @Override
        public EntityType getEntityType() {
            return EntityType.SERVICE;
        }

        @Override
        public String getTableName() {
            return "published_service";
        }

        @Override
        public PublishedService findByUniqueName(String name) throws FindException {
            return serviceEntityManager.findByUniqueName(name);
        }

        @Override
        public void delete(Goid oid) throws DeleteException, FindException {
            serviceEntityManager.delete(oid);
        }

        @Override
        public void update(PublishedService entity) throws UpdateException {
            serviceEntityManager.update(entity);
        }

        @Override
        public PublishedService findByHeader(EntityHeader header) throws FindException {
            return serviceEntityManager.findByHeader(header);
        }

        @Override
        public PublishedService findByPrimaryKey(long oid) throws FindException {
            return serviceEntityManager.findByPrimaryKey(oid);
        }

        @Override
        public PublishedService findByPrimaryKey(Goid goid) throws FindException {
            return serviceEntityManager.findByPrimaryKey(goid);
        }

        @Override
        public Collection<EntityHeader> findAllHeaders() throws FindException {
            return serviceEntityManager.findAllHeaders();
        }

        @Override
        public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
            return serviceEntityManager.findAllHeaders(offset, windowSize);
        }

        @Override
        public Collection<PublishedService> findAll() throws FindException {
            return serviceEntityManager.findAll();
        }

        @Override
        public Class<? extends Entity> getImpClass() {
            return PublishedService.class;
        }
    }

    private class StudJdbcConnectionManager implements ReadOnlyEntityManager<JdbcConnection, EntityHeader>, GoidEntityManager<JdbcConnection, EntityHeader>{

        @Override
        public Goid save(JdbcConnection entity) throws SaveException {
            return jdbcConnectionEntityManager.save(entity);
        }

        @Override
        public Integer getVersion(Goid goid) throws FindException {
            return jdbcConnectionEntityManager.getVersion(goid);
        }

        @Override
        public Map<Goid, Integer> findVersionMap() throws FindException {
            return jdbcConnectionEntityManager.findVersionMap();
        }

        @Override
        public void delete(JdbcConnection entity) throws DeleteException {
            jdbcConnectionEntityManager.delete(entity);
        }

        @Override
        public JdbcConnection getCachedEntity(Goid goid, int maxAge) throws FindException {
            return jdbcConnectionEntityManager.getCachedEntity(goid,maxAge);
        }

        @Override
        public Class<? extends Entity> getInterfaceClass() {
            return JdbcConnection.class;
        }

        @Override
        public EntityType getEntityType() {
            return EntityType.JDBC_CONNECTION;
        }

        @Override
        public String getTableName() {
            return "jdbc_connection";
        }

        @Override
        public JdbcConnection findByUniqueName(String name) throws FindException {
            return jdbcConnectionEntityManager.findByUniqueName(name);
        }

        @Override
        public void delete(Goid goid) throws DeleteException, FindException {
            jdbcConnectionEntityManager.delete(goid);
        }

        @Override
        public void update(JdbcConnection entity) throws UpdateException {
            jdbcConnectionEntityManager.update(entity);
        }

        @Override
        public JdbcConnection findByHeader(EntityHeader header) throws FindException {
            return jdbcConnectionEntityManager.findByHeader(header);
        }

        @Override
        public JdbcConnection findByPrimaryKey(long oid) throws FindException {
            return jdbcConnectionEntityManager.findByPrimaryKey(oid);
        }

        @Override
        public JdbcConnection findByPrimaryKey(Goid goid) throws FindException {
            return jdbcConnectionEntityManager.findByPrimaryKey(goid);
        }

        @Override
        public Collection<EntityHeader> findAllHeaders() throws FindException {
            return jdbcConnectionEntityManager.findAllHeaders();
        }

        @Override
        public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
            return jdbcConnectionEntityManager.findAllHeaders(offset,windowSize);
        }

        @Override
        public Collection<JdbcConnection> findAll() throws FindException {
            return jdbcConnectionEntityManager.findAll();
        }

        @Override
        public Class<? extends Entity> getImpClass() {
            return JdbcConnection.class;
        }
    }
}
