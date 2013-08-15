package com.l7tech.identity.internal;

import com.l7tech.identity.GroupMembership;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.Goid;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * A row in a user-group intersect table.
 *
 * Each row constitutes an edge in the many-to-many relationship between Users and Groups.
 *
 * The inherited {@link com.l7tech.objectmodel.imp.GoidEntityImp#getGoid()} ()} property is
 * a generated GOID in this class.
 *
 * @author alex
 */
@Entity
@Proxy(lazy=false)
@Table(name="internal_user_group")
public class InternalGroupMembership extends GroupMembership {

    private Goid _goid;
    private int _version;
    private Goid thisGroupGoid; // The OID of the {@link InternalGroup} to which this membership belongs
    private Goid memberProviderGoid;
    private String memberSubgroupId;

    /**
     * This constructor is used mainly for hibernate.  If you wish to construct this object, USE the factories method
     * instead, as it defines theproper provider oid values.
     */
    protected InternalGroupMembership() {
        //bug 5726: internal group should only have one provider ID (INTERNALPROVIDER_SPECIAL_GOID)
        super.thisGroupProviderGoid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;
    }

    @Override
    @Id
    @Column(name="goid", nullable=false, updatable=false)
    @GenericGenerator( name="generator", strategy = "layer7-goid-generator" )
    @GeneratedValue( generator = "generator")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getGoid() {
        return _goid;
    }

    @Override
    @Transient
    public String getId() {
        return Goid.toString(_goid);
    }

    @Override
    public void setGoid( Goid goid ) {
        _goid = goid;
    }

    @Override
    @Column(name="user_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getMemberUserId() {
        return super.getMemberUserId();
    }

    @Override
    @Column(name="internal_group")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getThisGroupId() {
        return thisGroupGoid;
    }

    @Override
    public void setThisGroupId(Goid thisGroupId) {
        this.thisGroupGoid = thisGroupId;
    }

    @Column(name="provider_goid", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getMemberProviderGoid() {
        return memberProviderGoid;
    }

    public void setMemberProviderGoid(Goid memberProviderGoid) {
        this.memberProviderGoid = memberProviderGoid;
    }

    /**
     * Constructs an internal group membership for an internal user
     * @param internalGroupOid the OID of the {@link InternalGroup} to which the user belongs
     * @param internalUserOid the OID of the {@link InternalUser} being added to the group
     */
    public static InternalGroupMembership newInternalMembership(Goid internalGroupOid, Goid internalUserOid) {
        InternalGroupMembership mem = new InternalGroupMembership();

        mem.thisGroupGoid = internalGroupOid;
        mem.thisGroupProviderGoid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;

        mem.memberUserId = internalUserOid;
        mem.memberProviderGoid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;

        return mem;
    }

    /**
     * Constructs an internal group membership for a user in an arbitrary identity provider.
     * @param internalGroupOid the OID of the {@link InternalGroup} to which the membership belongs
     * @param memberProviderOid the OID of the {@link com.l7tech.identity.IdentityProviderConfig} in which the member is defined
     * @param memberUserId the ID of the {@link com.l7tech.identity.User} being added to this group
     */
    public static InternalGroupMembership newMetaUserMembership(Goid internalGroupOid, Goid memberProviderOid, Goid memberUserId) {
        InternalGroupMembership mem = new InternalGroupMembership();

        mem.thisGroupGoid = internalGroupOid;
        mem.thisGroupProviderGoid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;

        mem.memberProviderGoid = memberProviderOid;
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

        if (memberProviderGoid != null ? !memberProviderGoid.equals(that.memberProviderGoid) : that.memberProviderGoid != null)
            return false;
        if (thisGroupGoid != null ? !thisGroupGoid.equals(that.thisGroupGoid) : that.thisGroupGoid != null)
            return false;
        if (memberSubgroupId != null ? !memberSubgroupId.equals(that.memberSubgroupId) : that.memberSubgroupId != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (thisGroupGoid != null ? thisGroupGoid.hashCode() : 0);
        result = 31 * result + (memberProviderGoid != null ? memberProviderGoid.hashCode() : 0);
        result = 31 * result + (memberSubgroupId != null ? memberSubgroupId.hashCode() : 0);
        return result;
    }
}
