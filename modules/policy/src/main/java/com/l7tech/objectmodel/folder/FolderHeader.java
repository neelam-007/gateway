package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ZoneableEntityHeader;

/**
 * Header class for service/policy folders.
 *
 * @author darmstrong
 */
public class FolderHeader extends ZoneableEntityHeader implements HasFolderId {

    //- PUBLIC

    public FolderHeader(final Folder folder) {
        this( folder.getGoid(),
              folder.getName(),
              folder.getFolder() == null ? null : folder.getFolder().getGoid(),
              folder.getVersion(),
              folder.getPath(),
              folder.getSecurityZone() == null ? null : folder.getSecurityZone().getGoid());
    }

    public FolderHeader( final Goid goid,
                         final String name,
                         final Goid parentFolderGoid,
                         final Integer version,
                         final String path,
                         final Goid securityZoneGoid) {
        super(goid, EntityType.FOLDER, name, path, version);
        this.parentFolderGoid = parentFolderGoid;
        this.path = path;
        this.securityZoneGoid = securityZoneGoid;
    }

    public Goid getParentFolderGoid() {
        return parentFolderGoid;
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
    public Goid getFolderId() {
        return getParentFolderGoid();
    }

    @Override
    public void setFolderId(Goid folderId) {
        throw new UnsupportedOperationException("set folderId not supported");
    }

    //- PRIVATE

    private final Goid parentFolderGoid;
    private final String path;
}

