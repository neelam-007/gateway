/*
 * Copyright (C) 2003-2005 Layer 7 Technologies Inc.
 */

package com.l7tech.identity.internal;

import com.l7tech.identity.GroupMembership;
import com.l7tech.identity.IdentityProviderConfigManager;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Version;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

/**
 * A row in a user-group intersect table.
 *
 * Each row constitutes an edge in the many-to-many relationship between Users and Groups.
 *
 * The inherited {@link com.l7tech.objectmodel.imp.PersistentEntityImp#getOid()} property is
 * a generated OID in this class.
 *
 * @author alex
 */
@Entity
@Table(name="internal_user_group")
public class InternalGroupMembership extends GroupMembership {

    private long _oid;
    private int _version;
    private long thisGroupOid; // The OID of the {@link InternalGroup} to which this membership belongs
    private long memberProviderOid;
    private String memberSubgroupId;

    @Id
    @Column(name="objectid", nullable=false, updatable=false)
    @GenericGenerator( name="generator", strategy = "hilo" )
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator = "generator")
    public long getOid() {
        return _oid;
    }

    @Transient
    public String getId() {
        return Long.toString(_oid);
    }

    public void setOid( long oid ) {
        _oid = oid;
    }

    @Override
    @Column(name="user_id", length=255)
    public String getMemberUserId() {
        return super.getMemberUserId();
    }

    @Column(name="internal_group")
    public String getThisGroupId() {
        return Long.toString(thisGroupOid);
    }

    public void setThisGroupId(String thisGroupId) {
        this.thisGroupOid = Long.parseLong(thisGroupId);
    }

    @Column(name="provider_oid", nullable=false)
    public long getMemberProviderOid() {
        return memberProviderOid;
    }

    public void setMemberProviderOid(long memberProviderOid) {
        this.memberProviderOid = memberProviderOid;
    }

    /**
     * Constructs an internal group membership for an internal user
     * @param internalGroupOid the OID of the {@link InternalGroup} to which the user belongs
     * @param internalUserOid the OID of the {@link InternalUser} being added to the group
     */
    public static InternalGroupMembership newInternalMembership(long internalGroupOid, long internalUserOid) {
        InternalGroupMembership mem = new InternalGroupMembership();

        mem.thisGroupOid = internalGroupOid;
        mem.thisGroupProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;

        mem.memberUserId = Long.toString(internalUserOid);
        mem.memberProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;

        return mem;
    }

    /**
     * Constructs an internal group membership for a user in an arbitrary identity provider.
     * @param internalGroupOid the OID of the {@link InternalGroup} to which the membership belongs
     * @param memberProviderOid the OID of the {@link com.l7tech.identity.IdentityProviderConfig} in which the member is defined
     * @param memberUserId the ID of the {@link com.l7tech.identity.User} being added to this group
     */
    public static InternalGroupMembership newMetaUserMembership(long internalGroupOid, long memberProviderOid, String memberUserId) {
        InternalGroupMembership mem = new InternalGroupMembership();

        mem.thisGroupOid = internalGroupOid;
        mem.thisGroupProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;

        mem.memberProviderOid = memberProviderOid;
        mem.memberUserId = memberUserId;
        mem.memberSubgroupId = null;

        return mem;
    }

    /**
     * Constructs an internal group membership for a subgroup in an arbitrary identity provider.
     * @param internalGroupOid the OID of the {@link InternalGroup} to which the membership belongs
     * @param memberProviderOid the OID of the {@link com.l7tech.identity.IdentityProviderConfig} in which the member is defined
     * @param memberSubgroupId the ID of the {@link com.l7tech.identity.Group} being added to this group
     */
    public static InternalGroupMembership newMetaSubgroupMembership(long internalGroupOid, long memberProviderOid, String memberSubgroupId) {
        InternalGroupMembership mem = new InternalGroupMembership();

        mem.thisGroupOid = internalGroupOid;
        mem.thisGroupProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;

        mem.memberProviderOid = memberProviderOid;
        mem.memberSubgroupId = memberSubgroupId;
        mem.memberUserId = null;

        return mem;
    }

    @Column(name="subgroup_id", length=255)
    public String getMemberSubgroupId() {
        return memberSubgroupId;
    }

    public void setMemberSubgroupId(String memberSubgroupId) {
        this.memberSubgroupId = memberSubgroupId;
    }

    @Version
    @Column(name="version")
    public int getVersion() {
        return _version;
    }

    public void setVersion(int version) {
        this._version = version;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final InternalGroupMembership that = (InternalGroupMembership)o;

        if (memberProviderOid != that.memberProviderOid) return false;
        if (thisGroupOid != that.thisGroupOid) return false;
        if (memberSubgroupId != null ? !memberSubgroupId.equals(that.memberSubgroupId) : that.memberSubgroupId != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int)(thisGroupOid ^ (thisGroupOid >>> 32));
        result = 31 * result + (int)(memberProviderOid ^ (memberProviderOid >>> 32));
        result = 31 * result + (memberSubgroupId != null ? memberSubgroupId.hashCode() : 0);
        return result;
    }
}
