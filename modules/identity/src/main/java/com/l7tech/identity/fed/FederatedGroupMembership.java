/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.fed;

import com.l7tech.identity.GroupMembership;
import com.l7tech.objectmodel.PersistentEntity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Transient;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;

/**
 * 
 */
@Entity
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
        this.memberUserId = Long.toString(userOid);
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
    public String getMemberUserId() {
        return super.getMemberUserId();
    }

    @Id
    @Column(name="fed_group_oid",nullable=false)
    public String getThisGroupId() {
        return Long.toString(thisGroupOid);
    }

    public void setThisGroupId(String thisGroupId) throws NumberFormatException {
        if (thisGroupId == null) {
            thisGroupOid = PersistentEntity.DEFAULT_OID;
        } else {
            thisGroupOid = Long.parseLong(thisGroupId);
        }
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
        return !(memberUserId != null ? !memberUserId.equals(that.memberUserId) : that.memberUserId != null);
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (int)(thisGroupOid ^ (thisGroupOid >>> 32));
        result = 29 * result + (int)(thisGroupProviderOid ^ (thisGroupProviderOid >>> 32));
        result = 29 * result + (memberUserId != null ? memberUserId.hashCode() : 0);
        return result;
    }

    public static final class FederatedGroupMembershipPK implements Serializable {
        private long thisGroupProviderOid;
        private String thisGroupId;
        private String memberUserId;

        public FederatedGroupMembershipPK() {            
        }

        public FederatedGroupMembershipPK( final long thisGroupProviderOid,
                                           final String thisGroupId,
                                           final String memberUserId ) {
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
        public String getThisGroupId() {
            return thisGroupId;
        }

        public void setThisGroupId(String thisGroupId) {
            this.thisGroupId = thisGroupId;
        }

        @Id
        @Column(name="fed_user_oid",nullable=false)
        public String getMemberUserId() {
            return memberUserId;
        }

        public void setMemberUserId(String memberUserId) {
            this.memberUserId = memberUserId;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FederatedGroupMembershipPK that = (FederatedGroupMembershipPK) o;

            if (thisGroupProviderOid != that.thisGroupProviderOid) return false;
            if (!memberUserId.equals(that.memberUserId)) return false;
            if (!thisGroupId.equals(that.thisGroupId)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (int) (thisGroupProviderOid ^ (thisGroupProviderOid >>> 32));
            result = 31 * result + thisGroupId.hashCode();
            result = 31 * result + memberUserId.hashCode();
            return result;
        }
    }
}
