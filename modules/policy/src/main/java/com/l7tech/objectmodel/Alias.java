package com.l7tech.objectmodel;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 25, 2008
 * Time: 2:14:03 PM
 * Alias was created as a means of referring to any class which can be aliases generically.
 * @param <HT> represents the entity being aliased, we need it's oid only
 */
public abstract class Alias<HT extends NamedEntityImp> extends PersistentEntityImp implements EntityAlias {

    private long entityOid;
    private long folderOid;

    @Deprecated // For Serialization and persistence only
    public Alias() {
    }

    public Alias(HT entity, long folderOid) {
        this.folderOid = folderOid;
        this.entityOid = entity.getOid();
    }

    /**
     * Create a copy of the given AliasEntity.
     *
     * <p>This will copy the identity of the orginal, if you don't want this
     * you will need to reset the id and version.</p>
     *
     * @param entity The AliasEntity to duplicate.
     */
    public Alias(final HT entity) {
        super(entity);
    }

    /*
   * entityOid is the entity oid of the real entity an instance of this interface
   * is aliasing
   * */
    public long getEntityOid() {
        return entityOid;
    }

    public void setEntityOid(long entityOid) {
        this.entityOid = entityOid;
    }

    public void setFolderOid(long folderOid) {
        this.folderOid = folderOid;
    }

    public long getFolderOid() {
        return folderOid;
    }
}

