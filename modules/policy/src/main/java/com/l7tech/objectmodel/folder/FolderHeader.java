package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ZoneableEntityHeader;

/**
 * Header class for service/policy folders.
 *
 * @author darmstrong
 */
public class FolderHeader extends ZoneableEntityHeader implements HasFolderOid {

    //- PUBLIC

    public FolderHeader(final Folder folder) {
        this( folder.getOid(),
              folder.getName(),
              folder.getFolder() == null ? null : folder.getFolder().getOid(),
              folder.getVersion(),
              folder.getPath(),
              folder.getSecurityZone() == null ? null : folder.getSecurityZone().getGoid());
    }

    public FolderHeader( final long objectid,
                         final String name,
                         final Long parentFolderOid,
                         final Integer version,
                         final String path,
                         final Goid securityZoneGoid) {
        super(objectid, EntityType.FOLDER, name, path, version);
        this.parentFolderOid = parentFolderOid;
        this.path = path;
        this.securityZoneGoid = securityZoneGoid;
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

    @Override
    public Long getFolderOid() {
        return getParentFolderOid();
    }

    @Override
    public void setFolderOid(Long folderOid) {
        throw new UnsupportedOperationException("set folderOid not supported");
    }

    //- PRIVATE

    private final Long parentFolderOid;
    private final String path;
}

