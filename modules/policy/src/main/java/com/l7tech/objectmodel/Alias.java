package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * Alias was created as a means of referring to any class which can be aliases generically.
 *
 * @param <ET> the type of entity that this is an alias to
 * @author darmstrong
 */
public abstract class Alias<ET extends PersistentEntity> extends PersistentEntityImp implements EntityAlias {
    private long entityOid;
    private Folder folder;

    @Deprecated // For Serialization and persistence only
    protected Alias() { }

    protected Alias(ET entity, Folder folder) {
        this.entityOid = entity.getOid();
        this.folder = folder;
    }

    /** The OID of the entity to which this alias refers. */
    public long getEntityOid() {
        return entityOid;
    }

    @Deprecated
    protected void setEntityOid(long entityOid) {
        this.entityOid = entityOid;
    }

    /** The folder where this alias lives. */
    public Folder getFolder() {
        return folder;
    }

    /**
     * Not deprecated, because modifying the folder is a convenient way to move aliases.
     */
    public void setFolder(Folder folder) {
        this.folder = folder;
    }
}

