package com.l7tech.server.entity;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.InvalidGenericEntityException;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 *
 */
public class GenericEntityManagerStub extends EntityManagerStub<GenericEntity, GenericEntityHeader> implements GenericEntityManager {

    private List<GenericEntity> entities;
    private List<String> registeredClasses;

    public GenericEntityManagerStub() {
        entities = Collections.emptyList();
    }

    public GenericEntityManagerStub(GenericEntity... entitiesIn) {
        super(entitiesIn);
        this.entities = new ArrayList(Arrays.asList(entitiesIn));
    }

    public void setRegistedClasses(String... registedClasses){
        this.registeredClasses = new ArrayList(Arrays.asList(registedClasses));
    }

    @Override
    public <ET extends GenericEntity> void registerClass(@NotNull Class<ET> entityClass) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> void registerClass(@NotNull Class<ET> entityClass, @Nullable GenericEntityMetadata metadata) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public boolean unRegisterClass(String entityClassName) {
        return registeredClasses.remove(entityClassName);
    }

    @Override
    public boolean isRegistered(String entityClassName) {
        return registeredClasses.contains(entityClassName);
    }

    @Override
    public <ET extends GenericEntity> EntityManager<ET, GenericEntityHeader> getEntityManager(@NotNull Class<ET> entityClass) {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> ET findByGenericClassAndPrimaryKey(@NotNull Class<ET> entityClass, Goid goid) throws FindException {
        for (GenericEntity entity : entities) {
            if (entity.getEntityClassName().equals(entityClass.getName()) && entity.getGoid().equals(goid)) {
                return (ET) entity;
            }
        }
        return null;
    }

    @Override
    public <ET extends GenericEntity> Collection<ET> findAll(Class<ET> entityClass) throws FindException {
        return (Collection<ET>) entities;
    }

    @Override
    public Collection<GenericEntityHeader> findAllHeaders(@NotNull Class<? extends GenericEntity> entityClass) throws FindException {
        return new ArrayList<GenericEntityHeader>(Functions.map(entities, new Functions.Unary<GenericEntityHeader, GenericEntity>() {
            @Override
            public GenericEntityHeader call(GenericEntity genericEntity) {
                return new GenericEntityHeader(genericEntity);
            }
        }));
    }

    @Override
    public Collection<GenericEntityHeader> findAllHeaders(@NotNull Class<? extends GenericEntity> entityClass, int offset, int windowSize) throws FindException {
        return findAllHeaders(entityClass);
    }

    @Override
    public <ET extends GenericEntity> Goid save(@NotNull Class<ET> entityClass, ET entity) throws SaveException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> void save(@NotNull Goid id, @NotNull Class<ET> entityClass, ET entity) throws SaveException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> Integer getVersion(@NotNull Class<ET> entityClass, Goid goid) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> Map<Goid, Integer> findVersionMap(@NotNull Class<ET> entityClass) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> void delete(@NotNull Class<ET> entityClass, ET entity) throws DeleteException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> ET getCachedEntity(@NotNull Class<ET> entityClass, Goid o, int maxAge) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> ET findByUniqueName(@NotNull Class<ET> entityClass, String name) throws FindException {
        for (GenericEntity entity : entities) {
            if (entity.getName().equals(name)) {
                return (ET) entity;
            }
        }
        return null;
    }

    @Override
    public GenericEntity findByUniqueName(@NotNull String entityClass, String name) throws FindException {
        for (GenericEntity entity : entities) {
            if (entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public <ET extends GenericEntity> void delete(@NotNull Class<ET> entityClass, Goid goid) throws DeleteException, FindException {
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

    @Override
    public <ET extends GenericEntity> List<ET> findPagedMatching(@NotNull Class<ET> entityClass, int offset, int count, String sortProperty, Boolean ascending, Map<String, List<Object>> matchProperties) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public <ET extends GenericEntity> ET asConcreteEntity(GenericEntity genericEntity) throws InvalidGenericEntityException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }
}
