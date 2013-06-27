package com.l7tech.server;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EntityCrudImplTest {
    private static final long ZONE_OID = 1234L;
    private EntityCrud entityCrud;
    @Mock
    private EntityFinder entityFinder;
    @Mock
    private EntityManager<Policy, EntityHeader> policyEntityManager;
    @Mock
    private EntityManager<SecurityZone, EntityHeader> zoneEntityManager;
    @Mock
    private EntityManager<PublishedService, EntityHeader> serviceEntityManager;
    private List<Long> oids;
    private SecurityZone zone;
    private Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
    private Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test2", "test2", false);
    private PublishedService service = new PublishedService();

    @Before
    public void setup() {
        entityCrud = new EntityCrudImpl(entityFinder,
                new ReadOnlyEntityManager[]{new StubPolicyEntityManager(), new StubSecurityZoneEntityManager(), new StubServiceManager()});
        oids = new ArrayList<>();
        zone = new SecurityZone();
    }

    @Test
    public void setSecurityZoneForEntities() throws Exception {
        oids.add(1L);
        oids.add(2L);
        when(zoneEntityManager.findByPrimaryKey(ZONE_OID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(1L)).thenReturn(policy);
        when(policyEntityManager.findByPrimaryKey(2L)).thenReturn(policy2);

        entityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
        verify(policyEntityManager).update(policy);
        verify(policyEntityManager).update(policy2);
    }

    @Test
    public void setSecurityZoneForEntitiesNone() throws Exception {
        entityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, Collections.<Long>emptyList());
        verify(zoneEntityManager, never()).findByPrimaryKey(anyLong());
        verify(policyEntityManager, never()).update(any(Policy.class));
    }

    @Test
    public void setSecurityZoneForEntitiesNullZone() throws Exception {
        oids.add(1L);
        when(policyEntityManager.findByPrimaryKey(1L)).thenReturn(policy);

        entityCrud.setSecurityZoneForEntities(null, EntityType.POLICY, oids);
        verify(zoneEntityManager, never()).findByPrimaryKey(anyLong());
        verify(policyEntityManager).update(policy);
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesZoneNotFound() throws Exception {
        oids.add(1L);
        when(zoneEntityManager.findByPrimaryKey(ZONE_OID)).thenReturn(null);
        try {
            entityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            assertEquals("Unable to set security zone for entities: Security zone with oid 1234 does not exist", e.getMessage());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesAtLeastOneEntityNotFound() throws Exception {
        oids.add(1L);
        oids.add(2L);
        when(zoneEntityManager.findByPrimaryKey(ZONE_OID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(1L)).thenReturn(policy);
        when(policyEntityManager.findByPrimaryKey(2L)).thenReturn(null);

        try {
            entityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            assertEquals("Policy with oid 2 does not exist or is not security zoneable", e.getMessage());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesErrorFindingZone() throws Exception {
        oids.add(1L);
        when(zoneEntityManager.findByPrimaryKey(anyLong())).thenThrow(new FindException("mocking exception"));
        entityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesErrorFindingEntity() throws Exception {
        oids.add(1L);
        when(zoneEntityManager.findByPrimaryKey(ZONE_OID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(anyLong())).thenThrow(new FindException("mocking exception"));
        entityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesErrorUpdatingEntity() throws Exception {
        oids.add(1L);
        when(zoneEntityManager.findByPrimaryKey(ZONE_OID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(1L)).thenReturn(policy);
        doThrow(new UpdateException("mocking exception")).when(policyEntityManager).update(any(Policy.class));
        entityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSecurityZoneForEntitiesEntityTypeNotZoneable() throws Exception {
        oids.add(1L);
        try {
            entityCrud.setSecurityZoneForEntities(null, EntityType.RBAC_ROLE, oids);
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
        final Map<EntityType, Collection<Long>> entities = new HashMap<>();
        entities.put(EntityType.POLICY, Arrays.asList(1L));
        entities.put(EntityType.SERVICE, Arrays.asList(2L));
        when(zoneEntityManager.findByPrimaryKey(ZONE_OID)).thenReturn(zone);
        when(policyEntityManager.findByPrimaryKey(1L)).thenReturn(policy);
        when(serviceEntityManager.findByPrimaryKey(2L)).thenReturn(service);

        entityCrud.setSecurityZoneForEntities(ZONE_OID, entities);
        verify(zoneEntityManager, times(1)).findByPrimaryKey(ZONE_OID);
        verify(policyEntityManager).update(policy);
        verify(serviceEntityManager).update(service);
    }

    @Test
    public void setSecurityZoneForEntitiesMultipleEntityTypesNone() throws Exception {
        entityCrud.setSecurityZoneForEntities(ZONE_OID, Collections.<EntityType, Collection<Long>>emptyMap());
        verify(zoneEntityManager, never()).findByPrimaryKey(anyLong());
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesMultipleEntityTypesZoneNotFound() throws Exception {
        final Map<EntityType, Collection<Long>> entities = new HashMap<>();
        entities.put(EntityType.POLICY, Arrays.asList(1L));
        entities.put(EntityType.SERVICE, Arrays.asList(2L));
        when(zoneEntityManager.findByPrimaryKey(ZONE_OID)).thenReturn(null);

        try {
            entityCrud.setSecurityZoneForEntities(ZONE_OID, entities);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            assertEquals("Unable to set security zone for entities: Security zone with oid 1234 does not exist", e.getMessage());
            throw e;
        }
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesMultipleEntityTypesErrorFindingZone() throws Exception {
        final Map<EntityType, Collection<Long>> entities = new HashMap<>();
        entities.put(EntityType.POLICY, Arrays.asList(1L));
        entities.put(EntityType.SERVICE, Arrays.asList(2L));
        when(zoneEntityManager.findByPrimaryKey(ZONE_OID)).thenThrow(new FindException("mocking exception"));
        entityCrud.setSecurityZoneForEntities(ZONE_OID, entities);
    }

    /**
     * Delegates to the mock.
     */
    private class StubPolicyEntityManager implements ReadOnlyEntityManager<Policy, EntityHeader>, EntityManager<Policy, EntityHeader> {
        @Override
        public long save(Policy entity) throws SaveException {
            return policyEntityManager.save(entity);
        }

        @Override
        public Integer getVersion(long oid) throws FindException {
            return policyEntityManager.getVersion(oid);
        }

        @Override
        public Map<Long, Integer> findVersionMap() throws FindException {
            return policyEntityManager.findVersionMap();
        }

        @Override
        public void delete(Policy entity) throws DeleteException {
            policyEntityManager.delete(entity);
        }

        @Override
        public Policy getCachedEntity(long o, int maxAge) throws FindException {
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
        public void delete(long oid) throws DeleteException, FindException {
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

    private class StubSecurityZoneEntityManager implements ReadOnlyEntityManager<SecurityZone, EntityHeader>, EntityManager<SecurityZone, EntityHeader> {

        @Override
        public long save(SecurityZone entity) throws SaveException {
            return zoneEntityManager.save(entity);
        }

        @Override
        public Integer getVersion(long oid) throws FindException {
            return zoneEntityManager.getVersion(oid);
        }

        @Override
        public Map<Long, Integer> findVersionMap() throws FindException {
            return zoneEntityManager.findVersionMap();
        }

        @Override
        public void delete(SecurityZone entity) throws DeleteException {
            zoneEntityManager.delete(entity);
        }

        @Override
        public SecurityZone getCachedEntity(long o, int maxAge) throws FindException {
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
        public void delete(long oid) throws DeleteException, FindException {
            zoneEntityManager.delete(oid);
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

    private class StubServiceManager implements ReadOnlyEntityManager<PublishedService, EntityHeader>, EntityManager<PublishedService, EntityHeader> {

        @Override
        public long save(PublishedService entity) throws SaveException {
            return serviceEntityManager.save(entity);
        }

        @Override
        public Integer getVersion(long oid) throws FindException {
            return serviceEntityManager.getVersion(oid);
        }

        @Override
        public Map<Long, Integer> findVersionMap() throws FindException {
            return serviceEntityManager.findVersionMap();
        }

        @Override
        public void delete(PublishedService entity) throws DeleteException {
            serviceEntityManager.delete(entity);
        }

        @Override
        public PublishedService getCachedEntity(long o, int maxAge) throws FindException {
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
        public void delete(long oid) throws DeleteException, FindException {
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
}
