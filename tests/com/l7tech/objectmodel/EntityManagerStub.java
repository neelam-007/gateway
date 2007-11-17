/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import java.util.*;

/**
 * Stub Entity Manager
 */
public abstract class EntityManagerStub<ET extends PersistentEntity> implements EntityManager<ET, EntityHeader> {
    protected final Map<Long, ET> entities;
    protected final Map<Long, EntityHeader> headers;
    
    private long nextOid;

    public EntityManagerStub() {
        this.entities = new HashMap<Long, ET>();
        this.headers = new HashMap<Long, EntityHeader>();
        this.nextOid = 1;
    }

    public EntityManagerStub(ET[] entities) {
        long maxOid = 0;
        Map<Long, ET> es = new HashMap<Long, ET>();
        Map<Long, EntityHeader> hs = new HashMap<Long, EntityHeader>();
        for (ET entity : entities) {
            es.put(entity.getOid(), entity);
            hs.put(entity.getOid(), header(entity));
            maxOid = Math.max(maxOid, entity.getOid());
        }
        this.entities = es;
        this.headers = hs;
        this.nextOid = ++maxOid;
    }

    public ET findByPrimaryKey(long oid) throws FindException {
        return entities.get(oid);
    }

    public synchronized void delete(long oid) throws DeleteException, FindException {
        entities.remove(oid);
        headers.remove(oid);
    }

    public synchronized void update(ET entity) throws UpdateException {
        if (entity.getOid() == PersistentEntity.DEFAULT_OID || entity.getId() == null) throw new IllegalArgumentException();
        entities.put(entity.getOid(), entity);
        headers.put(entity.getOid(), header(entity));
    }

    private EntityHeader header(ET entity) {
        return new EntityHeader(entity.getId(), getEntityType(), name(entity), null);
    }

    public synchronized Collection<EntityHeader> findAllHeaders() throws FindException {
        return Collections.unmodifiableCollection(headers.values());
    }

    public synchronized Collection<EntityHeader> findAllHeaders(int offset, int limit) throws FindException {
        EntityHeader[] dest = new EntityHeader[limit];
        EntityHeader[] all = headers.values().toArray(new EntityHeader[0]);
        System.arraycopy(all, offset, dest, 0, limit);
        return Arrays.asList(dest);
    }

    public ET findByUniqueName(String name) throws FindException {
        throw new UnsupportedOperationException();
    }

    public synchronized Collection<ET> findAll() throws FindException {
        return Collections.unmodifiableCollection(entities.values());
    }

    public Integer getVersion(long oid) throws FindException {
        ET ent = entities.get(oid);
        return ent == null ? null : ent.getVersion();
    }

    public synchronized Map<Long, Integer> findVersionMap() throws FindException {
        Map<Long, Integer> versions = new HashMap<Long, Integer>();
        for (Map.Entry<Long, ET> entry : entities.entrySet()) {
            versions.put(entry.getKey(), entry.getValue().getVersion());
        }
        return Collections.unmodifiableMap(versions);
    }

    public ET getCachedEntity(long o, int maxAge) throws FindException, CacheVeto {
        return entities.get(o);
    }

    public synchronized long save(ET entity) throws SaveException {
        long oid = nextOid++;

        entities.put(oid, entity);
        headers.put(oid, new EntityHeader(Long.toString(oid), getEntityType(), name(entity), null));

        entity.setOid(oid);

        return oid;
    }

    private String name(ET entity) {
        String name = null;
        if (entity instanceof NamedEntity) {
            NamedEntity namedEntity = (NamedEntity) entity;
            name = namedEntity.getName();
        }
        return name;
    }

    public synchronized void delete(ET entity) throws DeleteException {
        entities.remove(entity.getOid());
        headers.remove(entity.getOid());
    }

    public ET findEntity(long l) throws FindException {
        return entities.get(l);
    }

    public Class<? extends Entity> getImpClass() {
        return PersistentEntity.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return Entity.class;
    }

    public EntityType getEntityType() {
        return EntityType.UNDEFINED;
    }

    public String getTableName() {
        throw new UnsupportedOperationException();
    }
}
