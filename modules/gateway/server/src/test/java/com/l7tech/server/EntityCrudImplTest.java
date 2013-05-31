package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    private List<Long> oids;
    private SecurityZone zone;
    private Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
    private Policy policy2 = new Policy(PolicyType.INCLUDE_FRAGMENT, "test2", "test2", false);

    @Before
    public void setup() {
        entityCrud = new EntityCrudImpl(entityFinder,
                new ReadOnlyEntityManager[]{new StubPolicyEntityManager(), new StubSecurityZoneEntityManager()});
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
    public void setSecurityZoneForEntitiesNullZone() throws Exception {
        oids.add(1L);
        when(policyEntityManager.findByPrimaryKey(1L)).thenReturn(policy);

        entityCrud.setSecurityZoneForEntities(null, EntityType.POLICY, oids);
        verify(zoneEntityManager, never()).findByPrimaryKey(anyLong());
        verify(policyEntityManager).update(policy);
    }

    @Test(expected = UpdateException.class)
    public void setSecurityZoneForEntitiesZoneNotFound() throws Exception {
        when(zoneEntityManager.findByPrimaryKey(ZONE_OID)).thenReturn(null);
        try {
            entityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.POLICY, oids);
            fail("Expected UpdateException");
        } catch (final UpdateException e) {
            assertEquals("Security zone with oid 1234 does not exist", e.getMessage());
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
        try {
            entityCrud.setSecurityZoneForEntities(ZONE_OID, EntityType.RBAC_ROLE, oids);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            verify(zoneEntityManager, never()).findByPrimaryKey(anyLong());
            verify(policyEntityManager, never()).findByPrimaryKey(anyLong());
            verify(policyEntityManager, never()).update(any(Policy.class));
            throw e;
        }
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
}
