package com.l7tech.server.entity;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.GenericEntity;
import com.l7tech.server.EntityManagerStub;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 *
 */
public class GenericEntityManagerStub extends EntityManagerStub<GenericEntity, GenericEntityHeader> implements GenericEntityManager {
    @Override
    public void registerClass(@NotNull Class<? extends GenericEntity> entityClass) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public boolean unRegisterClass(String entityClassName) {
        return false;
    }

    @Override
    public <ET extends GenericEntity> EntityManager<ET, GenericEntityHeader> getEntityManager(@NotNull Class<ET> entityClass) {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> ET findByGenericClassAndPrimaryKey(@NotNull Class<ET> entityClass, long oid) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> Collection<ET> findAll(Class<ET> entityClass) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public Collection<GenericEntityHeader> findAllHeaders(@NotNull Class<? extends GenericEntity> entityClass) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public Collection<GenericEntityHeader> findAllHeaders(@NotNull Class<? extends GenericEntity> entityClass, int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> long save(@NotNull Class<ET> entityClass, ET entity) throws SaveException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> Integer getVersion(@NotNull Class<ET> entityClass, long oid) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> Map<Long, Integer> findVersionMap(@NotNull Class<ET> entityClass) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> void delete(@NotNull Class<ET> entityClass, ET entity) throws DeleteException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> ET getCachedEntity(@NotNull Class<ET> entityClass, long o, int maxAge) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> ET findByUniqueName(@NotNull Class<ET> entityClass, String name) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> void delete(@NotNull Class<ET> entityClass, long oid) throws DeleteException, FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> void update(@NotNull Class<ET> entityClass, ET entity) throws UpdateException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> ET findByHeader(@NotNull Class<ET> entityClass, EntityHeader header) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }
}
