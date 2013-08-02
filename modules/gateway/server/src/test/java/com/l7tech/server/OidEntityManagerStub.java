/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.objectmodel.*;

import java.util.*;

/**
 * Stub Entity Manager
 */
public abstract class OidEntityManagerStub<ET extends PersistentEntity, EH extends EntityHeader> implements EntityManager<ET, EH>, RoleAwareEntityManager<ET> {
    protected final Map<Long, ET> entities;
    protected final Map<Long, EH> headers;
    private final boolean canHasNames = NamedEntity.class.isAssignableFrom(getImpClass());

    private long nextOid;

    public OidEntityManagerStub() {
        this.entities = new HashMap<Long, ET>();
        this.headers = new HashMap<Long, EH>();
        this.nextOid = 1;
    }

    public OidEntityManagerStub(ET... entitiesIn) {
        long maxOid = 0;
        Map<Long, ET> entities = new LinkedHashMap<Long, ET>();
        Map<Long, EH> headers = new LinkedHashMap<Long, EH>();
        for (ET entity : entitiesIn) {
            entities.put(entity.getOid(), entity);
            headers.put(entity.getOid(), header(entity));
            maxOid = Math.max(maxOid, entity.getOid());
        }
        this.entities = entities;
        this.headers = headers;
        this.nextOid = ++maxOid;
    }

    @Override
    public ET findByPrimaryKey(long oid) throws FindException {
        return entities.get(oid);
    }

    @Override
    public ET findByPrimaryKey(Goid goid) throws FindException {
        return null;
    }

    @Override
    public ET findByHeader(EntityHeader header) throws FindException {
        return findByPrimaryKey(header.getOid());
    }

    @Override
    public synchronized void delete(long oid) throws DeleteException, FindException {
        entities.remove(oid);
        headers.remove(oid);
    }

    @Override
    public synchronized void update(ET entity) throws UpdateException {
        if (entity.getOid() == PersistentEntity.DEFAULT_OID || entity.getId() == null) throw new IllegalArgumentException();
        entity.setVersion( entity.getVersion() + 1 );
        entities.put(entity.getOid(), entity);
        headers.put(entity.getOid(), header(entity));
    }

    protected EH header(ET entity) {
        return (EH) new EntityHeader(entity.getId(), getEntityType(), name(entity), null);
    }

    @Override
    public synchronized Collection<EH> findAllHeaders() throws FindException {
        return Collections.unmodifiableCollection(headers.values());
    }

    public synchronized Collection<EH> findHeaders(int offset, int max, Map<String,String> filter) throws FindException {
        return Collections.unmodifiableCollection(headers.values());
    }

    @Override
    public synchronized Collection<EH> findAllHeaders(int offset, int limit) throws FindException {
        EH[] dest = (EH[]) new EntityHeader[limit];
        EH[] all = (EH[]) headers.values().toArray(new EntityHeader[limit]);
        System.arraycopy(all, offset, dest, 0, limit);
        return Arrays.asList(dest);
    }

    @Override
    public ET findByUniqueName(String name) throws FindException {
        if (!canHasNames) throw new FindException(getImpClass() + " has no name");
        ET got = null;
        for (ET et : entities.values()) {
            if (!(et instanceof NamedEntity)) throw new FindException(String.format("I was told that I would get NamedEntities but I found a %s and I'm going to burn down the building", et.getClass().getSimpleName()));

            NamedEntity namedEntity = (NamedEntity)et;
            if (name.equals(namedEntity.getName())) {
                if (got != null) throw new FindException(String.format("Found two %s with name %s", getImpClass().getSimpleName(), name));
                got = et;
            }
        }
        return got;
    }

    @Override
    public synchronized Collection<ET> findAll() throws FindException {
        return Collections.unmodifiableCollection(entities.values());
    }

    @Override
    public Integer getVersion(long oid) throws FindException {
        ET ent = entities.get(oid);
        return ent == null ? null : ent.getVersion();
    }

    @Override
    public synchronized Map<Long, Integer> findVersionMap() throws FindException {
        Map<Long, Integer> versions = new HashMap<Long, Integer>();
        for (Map.Entry<Long, ET> entry : entities.entrySet()) {
            versions.put(entry.getKey(), entry.getValue().getVersion());
        }
        return Collections.unmodifiableMap(versions);
    }

    @Override
    public ET getCachedEntity(long o, int maxAge) throws FindException {
        return entities.get(o);
    }

    @Override
    public synchronized long save(ET entity) throws SaveException {
        long oid = nextOid++;
        entity.setOid(oid);

        entities.put(oid, entity);
        headers.put(oid, header(entity));

        return oid;
    }

    private String name(ET entity) {
        String name = null;
        if (entity instanceof NamedEntity ) {
            NamedEntity namedEntity = (NamedEntity) entity;
            name = namedEntity.getName();
        }
        return name;
    }

    @Override
    public synchronized void delete(ET entity) throws DeleteException {
        entities.remove(entity.getOid());
        headers.remove(entity.getOid());
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return PersistentEntity.class;
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return Entity.class;
    }

    @Override
    public EntityType getEntityType() {
        return EntityTypeRegistry.getEntityType(getImpClass());
    }

    @Override
    public String getTableName() {
        return getEntityType().name().toLowerCase();
    }

    @Override
    public void createRoles(ET entity) throws SaveException {
    }

    @Override
    public void updateRoles( final ET entity ) throws UpdateException {
    }

    @Override
    public void deleteRoles( final long entityOid ) throws DeleteException {
    }
}
