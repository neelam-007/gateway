package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.HasFolderOid;

/**
 * When dealing with Services and Policies they have a lot in common - they can be put into folders and they
 * can be aliased. It is therefore convenint to write code that applies to both entities together instead of having
 * to write it twice for each entity. In order to facilitate this we need policy and service to have a common
 * super type which implements the interfaces required. See AliasManagerImpl for where this abstract
 * class is used
 *
 * @author darmstrong
 */
public abstract class OrganizationHeader extends GuidEntityHeader implements Aliasable, HasFolderOid {
    protected OrganizationHeader(long oid, EntityType type, String name, String description, int version) {
        super(oid, type, name, description, version);
    }

    @Override
    public Long getFolderOid() {
        return folderOid;
    }

    @Override
    public void setFolderOid(Long folderOid) {
        this.folderOid = folderOid;
    }

    @Override
    public boolean isAlias() {
        return alias;
    }

    @Override
    public void setAlias(boolean isAlias) {
        this.alias = isAlias;
    }

    //PRIVATE
    protected Long folderOid;
    protected boolean alias;
}
