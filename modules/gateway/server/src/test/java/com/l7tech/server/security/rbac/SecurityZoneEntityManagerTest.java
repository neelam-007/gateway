package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.PersistentEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecurityZoneEntityManagerTest {
    private static final long ZONE_OID = 1234L;
    private TestableSecurityZoneEntityManager manager;
    private List<PersistentEntityStub> entities;
    @Mock
    private HibernateTemplate hibernateTemplate;

    @Before
    public void setup() {
        manager = new TestableSecurityZoneEntityManager();
        manager.setHibernateTemplate(hibernateTemplate);
        entities = new ArrayList<>();
        entities.add(new PersistentEntityStub());
    }

    @Test
    public void findBySecurityZoneOid() {
        // uses EntityType.ANY because the stub doesn't have a specific EntityType
        final String query = "from stub in class " + EntityType.ANY.getEntityClass().getName() + " where stub.securityZone.oid = ?";
        when(hibernateTemplate.find(query, ZONE_OID)).thenReturn(entities);
        final Collection<PersistentEntityStub> found = manager.findBySecurityZoneOid(1234L);
        assertEquals(entities, found);
    }

    private class TestableSecurityZoneEntityManager extends AbstractSecurityZoneEntityManager<PersistentEntityStub, EntityHeader> {

        @Override
        public Class<? extends Entity> getImpClass() {
            return PersistentEntityStub.class;
        }


        @Override
        @Transactional(propagation = Propagation.SUPPORTS)
        public String getTableName() {
            return "stub";
        }

        @Override
        void validateEntityType() {
            // do nothing
        }
    }

    private class PersistentEntityStub implements PersistentEntity {

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public void setVersion(int version) {
        }

        @Override
        public long getOid() {
            return 0;
        }

        @Override
        public void setOid(long oid) {
        }

        @Override
        public String getId() {
            return null;
        }
    }

}
