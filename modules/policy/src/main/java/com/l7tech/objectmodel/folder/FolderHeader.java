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
              folder.getVersion() );
    }

    public FolderHeader( final long objectid,
                         final String name,
                         final Long parentFolderOid,
                         final Integer version) {
        super(objectid, EntityType.FOLDER, name, name, version);
        this.parentFolderOid = parentFolderOid;
    }

    public Long getParentFolderOid() {
        return parentFolderOid;
    }

    @Override
    public String toString() {
        return getName();
    }

    //- PRIVATE

    private final Long parentFolderOid;
}

