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
        return aliasOid != null;
    }

    public Long getAliasOid() {
        return aliasOid;
    }

    @Override
    public void setAliasOid(Long aliasOid) {
        this.aliasOid = aliasOid;
    }

    public boolean isPolicyDisabled() {
        return isPolicyDisabled;
    }

    public void setPolicyDisabled(final boolean isPolicyDisabled) {
        this.isPolicyDisabled = isPolicyDisabled;
    }

    public abstract String getDisplayName();

    //PRIVATE
    protected Long folderOid;
    protected Long aliasOid;

    protected boolean isPolicyDisabled;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final OrganizationHeader that = (OrganizationHeader) o;

        if (isPolicyDisabled != that.isPolicyDisabled) return false;
        if (aliasOid != null ? !aliasOid.equals(that.aliasOid) : that.aliasOid != null) return false;
        if (folderOid != null ? !folderOid.equals(that.folderOid) : that.folderOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (folderOid != null ? folderOid.hashCode() : 0);
        result = 31 * result + (aliasOid != null ? aliasOid.hashCode() : 0);
        result = 31 * result + (isPolicyDisabled ? 1 : 0);
        return result;
    }
}
