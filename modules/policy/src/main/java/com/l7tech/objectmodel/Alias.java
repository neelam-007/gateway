package com.l7tech.objectmodel;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 25, 2008
 * Time: 2:14:03 PM
 * AliasEntity was created as a means of referring to any class which can be aliases generically.
 * @param <HT> represents the concrete subclass. This is a bit strange however it allows for classes which want to
 * deal with concrete classes of AliasEntity generically to do so. As AliasEntity is paramatarized, if you have a
 * method which can return any of a selection of subtypes and we want type information to be available, by giving this
 * abstract class this information in HT we can make use of this elsewhere. See the Manager impl's which use AliasEntity
 */
public abstract class Alias<HT extends NamedEntityImp> extends NamedEntityImp implements EntityAlias {

    private long entityOid;
    private long folderOid;

    @Deprecated // For Serialization and persistence only
    public Alias() {
    }

    public Alias(HT entity, long folderOid) {
        super(entity);
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

