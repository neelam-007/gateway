package com.l7tech.objectmodel.folder;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 11, 2008
 * Time: 6:33:49 PM
 * To change this template use File | Settings | File Templates.
 */
/**
 * Header class for service/policy folders.
 */
public class FolderHeader extends EntityHeader {
    //- PUBLIC

    public FolderHeader(final Folder folder) {
        this( folder.getOid(),
              folder.getName(),
              folder.getParentFolderOid());
    }

    public FolderHeader(final long objectid,
                              final String name,
                              final Long parentFolderOid) {
        super(Long.toString(objectid), EntityType.FOLDER, name, name);
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

