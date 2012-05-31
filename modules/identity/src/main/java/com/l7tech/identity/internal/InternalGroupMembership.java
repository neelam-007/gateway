package com.l7tech.identity.internal;

import com.l7tech.identity.GroupMembership;
import com.l7tech.identity.IdentityProviderConfigManager;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;

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
@Proxy(lazy=false)
@Table(name="internal_user_group")
public class InternalGroupMembership extends GroupMembership {

    private long _oid;
    private int _version;
    private long thisGroupOid; // The OID of the {@link InternalGroup} to which this membership belongs
    private long memberProviderOid;
    private String memberSubgroupId;

    /**
     * This constructor is used mainly for hibernate.  If you wish to construct this object, USE the factories method
     * instead, as it defines theproper provider oid values.
     */
    protected InternalGroupMembership() {
        //bug 5726: internal group should only have one provider ID (INTERNALPROVIDER_SPECIAL_OID)        
        super.thisGroupProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
    }

    @Override
    @Id
    @Column(name="objectid", nullable=false, updatable=false)
    @GenericGenerator( name="generator", strategy = "layer7-generator" )
    @GeneratedValue( generator = "generator")
    public long getOid() {
        return _oid;
    }

    @Override
    @Transient
    public String getId() {
        return Long.toString(_oid);
    }

    @Override
    public void setOid( long oid ) {
        _oid = oid;
    }

    @Override
    @Column(name="user_id")
    public long getMemberUserId() {
        return super.getMemberUserId();
    }

    @Override
    @Column(name="internal_group")
    public long getThisGroupId() {
        return thisGroupOid;
    }

    @Override
    public void setThisGroupId(long thisGroupId) {
        this.thisGroupOid = thisGroupId;
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

        mem.memberUserId = internalUserOid;
        mem.memberProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;

        return mem;
    }

    /**
     * Constructs an internal group membership for a user in an arbitrary identity provider.
     * @param internalGroupOid the OID of the {@link InternalGroup} to which the membership belongs
     * @param memberProviderOid the OID of the {@link com.l7tech.identity.IdentityProviderConfig} in which the member is defined
     * @param memberUserId the ID of the {@link com.l7tech.identity.User} being added to this group
     */
    public static InternalGroupMembership newMetaUserMembership(long internalGroupOid, long memberProviderOid, long memberUserId) {
        InternalGroupMembership mem = new InternalGroupMembership();

        mem.thisGroupOid = internalGroupOid;
        mem.thisGroupProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;

        mem.memberProviderOid = memberProviderOid;
        mem.memberUserId = memberUserId;
        mem.memberSubgroupId = null;

        return mem;
    }

    @Column(name="subgroup_id", length=255)
    public String getMemberSubgroupId() {
        return memberSubgroupId;
    }

    public void setMemberSubgroupId(String memberSubgroupId) {
        this.memberSubgroupId = memberSubgroupId;
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return _version;
    }

    @Override
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
