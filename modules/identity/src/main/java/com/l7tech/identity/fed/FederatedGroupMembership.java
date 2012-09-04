/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.fed;

import com.l7tech.identity.GroupMembership;
import org.hibernate.annotations.Proxy;

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
    private long thisGroupOid;

    public FederatedGroupMembership() {
    }

    public FederatedGroupMembership(long providerOid, long groupOid, long userOid)
            throws NumberFormatException
    {
        this.thisGroupProviderOid = providerOid;
        this.thisGroupOid = groupOid;
        this.memberUserId = userOid;
    }

    /**
     * This ID is not a unique identifer.
     */
    @Transient
    public String getId() {
        return Long.toString(thisGroupOid);
    }

    @Override
    @Id
    @Column(name="provider_oid")
    public long getThisGroupProviderOid() {
        return super.getThisGroupProviderOid();
    }

    @Override
    @Id
    @Column(name="fed_user_oid",nullable=false)
    public long getMemberUserId() {
        return super.getMemberUserId();
    }

    @Override
    @Id
    @Column(name="fed_group_oid",nullable=false)
    public long getThisGroupId() {
        return thisGroupOid;
    }

    @Override
    public void setThisGroupId(long thisGroupId) throws NumberFormatException {
        thisGroupOid = thisGroupId;
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
    public long getOid() {
        return 0;
    }

    /**
     * This entity does not have an oid
     */
    public void setOid(long oid) {
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final FederatedGroupMembership that = (FederatedGroupMembership)o;

        if (thisGroupOid != that.thisGroupOid) return false;
        if (thisGroupProviderOid != that.thisGroupProviderOid) return false;
        return !(memberUserId != that.memberUserId);
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (int)(thisGroupOid ^ (thisGroupOid >>> 32));
        result = 29 * result + (int)(thisGroupProviderOid ^ (thisGroupProviderOid >>> 32));
        result = 29 * result + (int)(memberUserId ^ (memberUserId >>> 32));
        return result;
    }

    public static final class FederatedGroupMembershipPK implements Serializable {
        private long thisGroupProviderOid;
        private long thisGroupId;
        private long memberUserId;

        public FederatedGroupMembershipPK() {            
        }

        public FederatedGroupMembershipPK( final long thisGroupProviderOid,
                                           final long thisGroupId,
                                           final long memberUserId ) {
            this.thisGroupProviderOid = thisGroupProviderOid;
            this.thisGroupId = thisGroupId;
            this.memberUserId = memberUserId;
        }

        @Id
        @Column(name="provider_oid")
        public long getThisGroupProviderOid() {
            return thisGroupProviderOid;
        }

        public void setThisGroupProviderOid(long thisGroupProviderOid) {
            this.thisGroupProviderOid = thisGroupProviderOid;
        }

        @Id
        @Column(name="fed_group_oid",nullable=false)
        public long getThisGroupId() {
            return thisGroupId;
        }

        public void setThisGroupId(long thisGroupId) {
            this.thisGroupId = thisGroupId;
        }

        @Id
        @Column(name="fed_user_oid",nullable=false)
        public long getMemberUserId() {
            return memberUserId;
        }

        public void setMemberUserId(long memberUserId) {
            this.memberUserId = memberUserId;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FederatedGroupMembershipPK that = (FederatedGroupMembershipPK) o;

            if (thisGroupProviderOid != that.thisGroupProviderOid) return false;
            if (memberUserId != that.memberUserId) return false;
            if (thisGroupId != that.thisGroupId) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (thisGroupProviderOid ^ (thisGroupProviderOid >>> 32));
            result = 31 * result + (int) (thisGroupId ^ (thisGroupId >>> 32));
            result = 31 * result + (int) (memberUserId ^ (memberUserId >>> 32));
            return result;
        }
    }
}
