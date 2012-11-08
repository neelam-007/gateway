package com.l7tech.policy;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.OrganizationHeader;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Extension of EntityHeader with some policy information.
 *
 * @author Steve Jones
 */
@SuppressWarnings( { "serial" } )
@XmlRootElement(name="policyHeader")
public class PolicyHeader extends OrganizationHeader {

    //- PUBLIC

    public PolicyHeader(final Policy policy) {
        this( policy, policy.getVersionOrdinal() );
    }

    public PolicyHeader(final Policy policy, final long policyRevision) {
        this(policy.getOid(),
             policy.isSoap(),
             policy.getType(),
             policy.getName(),
             policy.getInternalTag(),
             policy.getGuid(),
             policy.getFolder() == null ? null : policy.getFolder().getOid(),
             null,
             policyRevision,
             policy.getVersion(),
             policy.isDisabled());
    }

    @Override
    @XmlTransient
    public String getStrId() {
        return super.getStrId();
    }

    public PolicyHeader(final PolicyHeader policyHeader){
        this(policyHeader.getOid(),
             policyHeader.isSoap(),
             policyHeader.getPolicyType(),
             policyHeader.getName(),
             policyHeader.getDescription(),
             policyHeader.getGuid(),
             policyHeader.getFolderOid(),
             policyHeader.getAliasOid(),
             policyHeader.getPolicyRevision(),
             policyHeader.getVersion(),
             policyHeader.isPolicyDisabled());
    }

    public PolicyHeader(final long oid,
                        final boolean isSoap,
                        final PolicyType policyType,
                        final String name,
                        final String description,
                        final String policyGuid,
                        final Long folderOid,
                        final Long aliasOid,
                        final long policyRevision,
                        final int version,
                        final boolean isPolicyDisabled)
    {
        super(oid, EntityType.POLICY, name, description, version);

        this.guid = policyGuid;
        this.isSoap = isSoap;
        this.policyType = policyType;
        this.folderOid = folderOid;
        this.aliasOid = aliasOid;
        this.policyRevision = policyRevision;
        this.isPolicyDisabled = isPolicyDisabled;
    }

    public boolean isSoap() {
        return isSoap;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    /**
     * Get the policy revision ordinal if available.
     *
     * @return The policy revision or 0 if not available.
     */
    public long getPolicyRevision() {
        return policyRevision;
    }

    /**
     * Get the display name for this policy.
     *
     * <p>The display name is the policy name with an optional tag appended.</p>
     *
     * @return The display name.
     */
    @Override
    public String getDisplayName() {
        String displayName = getName();

        String tag = policyType.getGuiTags().isEmpty() ? null : this.getDescription();
        if ( tag != null) {
            displayName += " [" + tag + "]";
        }

        return displayName;    }

    @Override
    public String toString() {
        return getName();
    }

    //- PRIVATE

    private final boolean isSoap;
    private final long policyRevision;
    private final PolicyType policyType;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final PolicyHeader that = (PolicyHeader) o;

        if (isSoap != that.isSoap) return false;
        if (policyRevision != that.policyRevision) return false;
        if (policyType != that.policyType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (isSoap ? 1 : 0);
        result = 31 * result + (int) (policyRevision ^ (policyRevision >>> 32));
        result = 31 * result + (policyType != null ? policyType.hashCode() : 0);
        return result;
    }
}
