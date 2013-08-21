package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.imp.ZoneableEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.search.Dependency;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlRootElement;

import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

/**
 * Alias was created as a means of referring to any class which can be aliases generically.
 *
 * @param <ET> the type of entity that this is an alias to
 * @author darmstrong
 */
@XmlRootElement
public abstract class Alias<ET extends PersistentEntity> extends ZoneableEntityImp implements EntityAlias {
    protected Goid entityGoid;
    private Folder folder;

    @Deprecated // For Serialization and persistence only
    protected Alias() { }

    protected Alias( final Goid entityGoid,
                     final Folder folder,
                     @Nullable final SecurityZone securityZone) {
        this.entityGoid = entityGoid;
        this.folder = folder;
        this.securityZone = securityZone;
    }

    protected Alias(ET entity, Folder folder, @Nullable final SecurityZone securityZone) {
        this( entity.getGoid(), folder, securityZone);
    }

    /**
     * The GOID of the entity to which this alias refers.
     * Needs to be overridden with the proper Migration annotation.
     */
    public abstract Goid getEntityGoid();

    @Deprecated
    protected void setEntityGoid(Goid entityGoid) {
        this.entityGoid = entityGoid;
    }

    /** The folder where this alias lives. */
    @Migration(mapName = NONE, mapValue = NONE)
    @Dependency(isDependency = false)
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

