package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

/**
 * Header class for service/policy folders.
 *
 * @author darmstrong
 */
public class FolderHeader extends EntityHeader {

    //- PUBLIC

    public FolderHeader(final Folder folder) {
        this( folder.getOid(),
              folder.getName(),
              folder.getFolder() == null ? null : folder.getFolder().getOid(),
              folder.getVersion(),
              folder.getPath() );
    }

    public FolderHeader( final long objectid,
                         final String name,
                         final Long parentFolderOid,
                         final Integer version,
                         final String path) {
        super(objectid, EntityType.FOLDER, name, path, version);
        this.parentFolderOid = parentFolderOid;
        this.path = path;
    }

    public Long getParentFolderOid() {
        return parentFolderOid;
    }

    /**
     * Get the full path for the Folder.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return getName();
    }

    //- PRIVATE

    private final Long parentFolderOid;
    private final String path;
}

