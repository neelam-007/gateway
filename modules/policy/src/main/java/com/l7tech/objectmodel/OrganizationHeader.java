package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.HasFolder;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 25, 2008
 * Time: 1:13:45 PM
 * When dealing with Services and Policies they have a lot in common - they can be put into folders and they
 * can be aliased. It is therefore convenint to write code that applies to both entities together instead of having
 * to write it twice for each entity. In order to facilitate this we need policy and service to have a common
 * super type which implements the interfaces required. See AliasManagerImpl for where this abstract
 * class is used
 *
 */
public abstract class OrganizationHeader extends EntityHeader implements Aliasable, HasFolder {

    public OrganizationHeader(String id, EntityType type, String name, String description) {
        super(id, type, name, description);
    }

    public OrganizationHeader(long oid, EntityType type, String name, String description) {
        super(oid, type, name, description);
    }

    public Long getFolderOid() {
        return folderOid;
    }

    public void setFolderOid(Long folderOid) {
        this.folderOid = folderOid;
    }

    public boolean isAlias() {
        return alias;
    }

    public void setAlias(boolean isAlias) {
        this.alias = isAlias;
    }

    //PRIVATE
    protected long folderOid;
    protected boolean alias;
}
