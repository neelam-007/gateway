package com.l7tech.identity;

import com.l7tech.objectmodel.PersistentEntity;

import java.io.Serializable;

/**
 * Abstract superclass of all group memberships.
 */
public abstract class GroupMembership implements PersistentEntity, Serializable {
    protected long thisGroupProviderOid;
    protected String memberUserId;

    /**
     * The OID of the {@link Group} to which this membership pertains.
     *
     * Not the OID of the membership itself!
     */
    public abstract String getThisGroupId();

    /**
     * The OID of the {@link Group} to which this membership pertains.
     *
     * Not the OID of the membership itself!
     */
    public abstract void setThisGroupId(String thisGroupId);

    /**
     * The ID of the {@link User} who's a member of the group.  May be null.
     */
    public String getMemberUserId() {
        return memberUserId;
    }

    /**
     * The ID of the {@link User} who's a member of the group.  May be null.
     */
    public void setMemberUserId(String memberUserId) {
        this.memberUserId = memberUserId;
    }

    /**
     * The OID of the {@link IdentityProviderConfig} to which this group belongs.
     */
    public long getThisGroupProviderOid() {
        return thisGroupProviderOid;
    }

    /**
     * The OID of the {@link IdentityProviderConfig} to which this group belongs.
     */
    public void setThisGroupProviderOid(long thisGroupProviderOid) {
        this.thisGroupProviderOid = thisGroupProviderOid;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final GroupMembership that = (GroupMembership)o;

        if (thisGroupProviderOid != that.thisGroupProviderOid) return false;
        if (memberUserId != null ? !memberUserId.equals(that.memberUserId) : that.memberUserId != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (int)(thisGroupProviderOid ^ (thisGroupProviderOid >>> 32));
        result = 29 * result + (memberUserId != null ? memberUserId.hashCode() : 0);
        return result;
    }
}
