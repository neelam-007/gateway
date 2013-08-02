package com.l7tech.policy;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.OrganizationHeader;
import org.jetbrains.annotations.Nullable;

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
        this(policy.getGoid(),
             policy.isSoap(),
             policy.getType(),
             policy.getName(),
             policy.getInternalTag(),
             policy.getGuid(),
             policy.getFolder() == null ? null : policy.getFolder().getGoid(),
             null,
             policyRevision,
             policy.getVersion(),
             policy.isDisabled(),
             policy.getSecurityZone() == null ? null : policy.getSecurityZone().getGoid());
    }

    @Override
    @XmlTransient
    public String getStrId() {
        return super.getStrId();
    }

    public PolicyHeader(final PolicyHeader policyHeader){
        this(policyHeader.getGoid(),
             policyHeader.isSoap(),
             policyHeader.getPolicyType(),
             policyHeader.getName(),
             policyHeader.getDescription(),
             policyHeader.getGuid(),
             policyHeader.getFolderGoid(),
             policyHeader.getAliasGoid(),
             policyHeader.getPolicyRevision(),
             policyHeader.getVersion(),
             policyHeader.isPolicyDisabled(),
             policyHeader.getSecurityZoneGoid());
    }

    public PolicyHeader(final Goid goid,
                        final boolean isSoap,
                        final PolicyType policyType,
                        final String name,
                        final String description,
                        final String policyGuid,
                        final Goid folderGoid,
                        final Goid aliasGoid,
                        final long policyRevision,
                        final int version,
                        final boolean isPolicyDisabled,
                        @Nullable final Goid securityZoneGoid)
    {
        super(goid, EntityType.POLICY, name, description, version);

        this.guid = policyGuid;
        this.isSoap = isSoap;
        this.policyType = policyType;
        this.folderGoid = folderGoid;
        this.aliasGoid = aliasGoid;
        this.policyRevision = policyRevision;
        this.isPolicyDisabled = isPolicyDisabled;
        this.securityZoneGoid = securityZoneGoid;
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

        return displayName;
    }

    @Override
    public String toString() {
        return getName();
    }

    //- PRIVATE

    private final boolean isSoap;
    private final long policyRevision;
    private final PolicyType policyType;
}
