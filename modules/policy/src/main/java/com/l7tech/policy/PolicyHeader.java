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
@XmlRootElement(name="policyHeader")
public class PolicyHeader extends OrganizationHeader {
    //- PUBLIC
    public PolicyHeader(final Policy policy) {
        this(policy.getOid(),
             policy.isSoap(),
             policy.getName(),
             policy.getInternalTag(),
             policy.getGuid(),
             policy.getFolder() == null ? null : policy.getFolder().getOid(),
             null,
             policy.getVersionOrdinal(),
             policy.getVersion());
    }

    @Override
    @XmlTransient
    public String getStrId() {
        return super.getStrId();
    }

    public PolicyHeader(final PolicyHeader policyHeader){
        this(policyHeader.getOid(),
             policyHeader.isSoap(),
             policyHeader.getName(),
             policyHeader.getDescription(),
             policyHeader.getGuid(),
             policyHeader.getFolderOid(),
             policyHeader.getAliasOid(),
             policyHeader.getPolicyRevision(),
             policyHeader.getVersion());
    }

    public PolicyHeader(final long oid,
                        final boolean isSoap,
                        final String name,
                        final String description,
                        final String policyGuid,
                        final Long folderOid,
                        final Long aliasOid,
                        final long policyRevision,
                        final int version )
    {
        super(oid, EntityType.POLICY, name, description, version);

        this.guid = policyGuid;
        this.isSoap = isSoap;
        this.folderOid = folderOid;
        this.aliasOid = aliasOid;
        this.policyRevision = policyRevision;
    }

    public boolean isSoap() {
        return isSoap;
    }

    /**
     * Get the policy revision ordinal if available.
     *
     * @return The policy revision or 0 if not available.
     */
    public long getPolicyRevision() {
        return policyRevision;
    }

    @Override
    public String toString() {
        return getName();
    }

    //- PRIVATE

    private final boolean isSoap;
    private final long policyRevision;
}
