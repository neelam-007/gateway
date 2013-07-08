/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderedGoidEntityManager;
import com.l7tech.objectmodel.imp.GoidEntityImp;

import java.util.*;

/**
 * Stub Entity Manager
 */
public abstract class GoidEntityManagerStub<ET extends GoidEntity, EH extends EntityHeader> implements FolderedGoidEntityManager<ET, EH>, RoleAwareGoidEntityManager<ET> {
    protected final Map<Goid, ET> entities;
    protected final Map<Goid, EH> headers;
    private final boolean canHasNames = NamedEntity.class.isAssignableFrom(getImpClass());

    private long nextOid;

    public GoidEntityManagerStub() {
        this.entities = new HashMap<Goid, ET>();
        this.headers = new HashMap<Goid, EH>();
        nextOid = 1;
    }

    public GoidEntityManagerStub(ET... entitiesIn) {
        long maxOid = 0;
        Map<Goid, ET> entities = new LinkedHashMap<Goid, ET>();
        Map<Goid, EH> headers = new LinkedHashMap<Goid, EH>();
        for (ET entity : entitiesIn) {
            entities.put(entity.getGoid(), entity);
            headers.put(entity.getGoid(), header(entity));
            maxOid = Math.max(maxOid, entity.getGoid().getLow());
        }
        this.entities = entities;
        this.headers = headers;
        this.nextOid = ++maxOid;
    }

    @Override
    public ET findByPrimaryKey(long oid) throws FindException {
        throw new UnsupportedOperationException("cannot find goidified entities by using oid's");
    }

    @Override
    public ET findByPrimaryKey(Goid goid) throws FindException {
        return entities.get(goid);
    }

    @Override
    public ET findByHeader(EntityHeader header) throws FindException {
        return findByPrimaryKey(header.getGoid());
    }

    @Override
    public synchronized void delete(Goid goid) throws DeleteException, FindException {
        entities.remove(goid);
        headers.remove(goid);
    }

    @Override
    public synchronized void update(ET entity) throws UpdateException {
        if (GoidEntity.DEFAULT_GOID.equals(entity.getGoid()) || entity.getId() == null) throw new IllegalArgumentException();
        entity.setVersion( entity.getVersion() + 1 );
        entities.put(entity.getGoid(), entity);
        headers.put(entity.getGoid(), header(entity));
    }

    @Override
    public void updateWithFolder(ET entity) throws UpdateException {
        update(entity);
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
    public Integer getVersion(Goid goid) throws FindException {
        ET ent = entities.get(goid);
        return ent == null ? null : ent.getVersion();
    }

    @Override
    public synchronized Map<Goid, Integer> findVersionMap() throws FindException {
        Map<Goid, Integer> versions = new HashMap<Goid, Integer>();
        for (Map.Entry<Goid, ET> entry : entities.entrySet()) {
            versions.put(entry.getKey(), entry.getValue().getVersion());
        }
        return Collections.unmodifiableMap(versions);
    }

    @Override
    public ET getCachedEntity(Goid o, int maxAge) throws FindException {
        return entities.get(o);
    }

    @Override
    public synchronized Goid save(ET entity) throws SaveException {
        long id = nextOid++;
        final Goid goid = new Goid(0,id);
        entity.setGoid(goid);

        entities.put(goid, entity);
        headers.put(goid, header(entity));

        return goid;
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
    public void updateFolder( final Goid entityId, final Folder folder ) throws UpdateException {
    }

    @Override
    public void updateFolder( final ET entity, final Folder folder ) throws UpdateException {
    }

    @Override
    public synchronized void delete(ET entity) throws DeleteException {
        entities.remove(entity.getGoid());
        headers.remove(entity.getGoid());
    }

    @Override
    public Class<? extends GoidEntityImp> getImpClass() {
        return GoidEntityImp.class;
    }

    @Override
    public Class<? extends GoidEntity> getInterfaceClass() {
        return GoidEntity.class;
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
    public void deleteRoles( final Goid entityOid ) throws DeleteException {
    }
}
