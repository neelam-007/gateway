package com.l7tech.identity;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;

import java.io.Serializable;

/**
 * Abstract superclass of all group memberships.
 */
public abstract class GroupMembership implements GoidEntity, Serializable {
    protected Goid thisGroupProviderGoid;
    protected Goid memberUserId;

    /**
     * The Goid of the {@link Group} to which this membership pertains.
     *
     * Not the Goid of the membership itself!
     */
    public abstract Goid getThisGroupId();

    /**
     * The Goid of the {@link Group} to which this membership pertains.
     *
     * Not the Goid of the membership itself!
     */
    public abstract void setThisGroupId(Goid thisGroupId);

    /**
     * This should never be used.  This entity does not have a primary key
     * @return
     */
    @Deprecated
    @Override
    public boolean isUnsaved() {
        return false;
    }

    /**
     * The ID of the {@link User} who's a member of the group.  May be null.
     */
    public Goid getMemberUserId() {
        return memberUserId;
    }

    /**
     * The ID of the {@link User} who's a member of the group.  May be null.
     */
    public void setMemberUserId(Goid memberUserId) {
        this.memberUserId = memberUserId;
    }

    /**
     * The OID of the {@link IdentityProviderConfig} to which this group belongs.
     */
    public Goid getThisGroupProviderGoid() {
        return thisGroupProviderGoid;
    }

    /**
     * The OID of the {@link IdentityProviderConfig} to which this group belongs.
     */
    public void setThisGroupProviderGoid(Goid thisGroupProviderGoid) {
        this.thisGroupProviderGoid = thisGroupProviderGoid;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final GroupMembership that = (GroupMembership)o;

        if (!thisGroupProviderGoid.equals(that.thisGroupProviderGoid)) return false;
        if (!memberUserId.equals(that.memberUserId)) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + thisGroupProviderGoid.hashCode();
        result = 29 * result + memberUserId.hashCode();
        return result;
    }
}
