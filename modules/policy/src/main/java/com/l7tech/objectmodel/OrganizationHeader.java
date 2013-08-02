package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.HasFolderGoid;

/**
 * When dealing with Services and Policies they have a lot in common - they can be put into folders and they
 * can be aliased. It is therefore convenint to write code that applies to both entities together instead of having
 * to write it twice for each entity. In order to facilitate this we need policy and service to have a common
 * super type which implements the interfaces required. See AliasManagerImpl for where this abstract
 * class is used
 *
 * @author darmstrong
 */
public abstract class OrganizationHeader extends ZoneableGuidEntityHeader implements Aliasable, HasFolderGoid {
    protected OrganizationHeader(Goid goid, EntityType type, String name, String description, int version) {
        super(goid, type, name, description, version);
    }

    @Override
    public Goid getFolderGoid() {
        return folderGoid;
    }

    @Override
    public void setFolderGoid(Goid folderGoid) {
        this.folderGoid = folderGoid;
    }

    @Override
    public boolean isAlias() {
        return aliasGoid != null;
    }

    public Goid getAliasGoid() {
        return aliasGoid;
    }

    @Override
    public void setAliasGoid(Goid aliasGoid) {
        this.aliasGoid = aliasGoid;
    }

    public boolean isPolicyDisabled() {
        return isPolicyDisabled;
    }

    public void setPolicyDisabled(final boolean isPolicyDisabled) {
        this.isPolicyDisabled = isPolicyDisabled;
    }

    public abstract String getDisplayName();

    //PRIVATE
    protected Goid folderGoid;
    protected Goid aliasGoid;

    protected boolean isPolicyDisabled;
}
