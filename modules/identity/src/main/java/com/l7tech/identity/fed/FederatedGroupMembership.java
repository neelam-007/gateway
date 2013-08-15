/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.fed;

import com.l7tech.identity.GroupMembership;
import com.l7tech.objectmodel.Goid;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;

/**
 *
 */
@Entity
@Proxy(lazy=false)
@Table(name="fed_user_group")
@IdClass(FederatedGroupMembership.FederatedGroupMembershipPK.class)
public class FederatedGroupMembership extends GroupMembership {
    private Goid thisGroupGoid;

    public FederatedGroupMembership() {
    }

    public FederatedGroupMembership(Goid providerOid, Goid groupGoid, Goid userOid)
            throws NumberFormatException
    {
        this.thisGroupProviderGoid = providerOid;
        this.thisGroupGoid = groupGoid;
        this.memberUserId = userOid;
    }

    /**
     * This ID is not a unique identifer.
     */
    @Transient
    public String getId() {
        return Goid.toString(thisGroupGoid);
    }

    @Override
    @Id
    @Column(name="provider_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getThisGroupProviderGoid() {
        return super.getThisGroupProviderGoid();
    }

    @Override
    @Id
    @Column(name="fed_user_goid",nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getMemberUserId() {
        return super.getMemberUserId();
    }

    @Override
    @Id
    @Column(name="fed_group_goid",nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getThisGroupId() {
        return thisGroupGoid;
    }

    @Override
    public void setThisGroupId(Goid thisGroupId) throws NumberFormatException {
        thisGroupGoid = thisGroupId;
    }

    /**
     * This entity is not versioned
     */
    @Transient
    public int getVersion() {
        return 0;
    }

    /**
     * This entity is not versioned
     */
    public void setVersion(int version) {
    }

    /**
     * This entity does not have an oid
     */
    @Transient
    public Goid getGoid() {
        return null;
    }

    /**
     * This entity does not have an oid
     */
    public void setGoid(Goid goid) {
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final FederatedGroupMembership that = (FederatedGroupMembership)o;

        if (!thisGroupGoid.equals(that.thisGroupGoid)) return false;
        if (!thisGroupProviderGoid.equals(that.thisGroupProviderGoid)) return false;
        return (memberUserId.equals(that.memberUserId));
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (thisGroupGoid != null ? thisGroupGoid.hashCode() : 0);
        result = 29 * result + (thisGroupGoid != null ? thisGroupProviderGoid.hashCode() : 0);
        result = 29 * result + (thisGroupGoid!=null?memberUserId.hashCode():0);
        return result;
    }

    public static final class FederatedGroupMembershipPK implements Serializable {
        private Goid thisGroupProviderGoid;
        private Goid thisGroupId;
        private Goid memberUserId;

        public FederatedGroupMembershipPK() {
        }

        public FederatedGroupMembershipPK( final Goid thisGroupProviderGoid,
                                           final Goid thisGroupId,
                                           final Goid memberUserId ) {
            this.thisGroupProviderGoid = thisGroupProviderGoid;
            this.thisGroupId = thisGroupId;
            this.memberUserId = memberUserId;
        }

        @Id
        @Column(name="provider_goid")
        @Type(type = "com.l7tech.server.util.GoidType")
        public Goid getThisGroupProviderGoid() {
            return thisGroupProviderGoid;
        }

        public void setThisGroupProviderGoid(Goid thisGroupProviderGoid) {
            this.thisGroupProviderGoid = thisGroupProviderGoid;
        }

        @Id
        @Column(name="fed_group_goid",nullable=false)
        @Type(type = "com.l7tech.server.util.GoidType")
        public Goid getThisGroupId() {
            return thisGroupId;
        }

        public void setThisGroupId(Goid thisGroupId) {
            this.thisGroupId = thisGroupId;
        }

        @Id
        @Column(name="fed_user_goid",nullable=false)
        @Type(type = "com.l7tech.server.util.GoidType")
        public Goid getMemberUserId() {
            return memberUserId;
        }

        public void setMemberUserId(Goid memberUserId) {
            this.memberUserId = memberUserId;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FederatedGroupMembershipPK that = (FederatedGroupMembershipPK) o;

            if (!thisGroupProviderGoid.equals(that.thisGroupProviderGoid)) return false;
            if (!memberUserId.equals(that.memberUserId)) return false;
            if (!thisGroupId.equals(that.thisGroupId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = thisGroupProviderGoid.hashCode();
            result = 31 * result + thisGroupId.hashCode();
            result = 31 * result + memberUserId.hashCode();
            return result;
        }
    }
}
