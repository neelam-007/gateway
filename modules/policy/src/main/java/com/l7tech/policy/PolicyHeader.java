package com.l7tech.policy;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.OrganizationHeader;

/**
 * Extension of EntityHeader with some policy information.
 *
 * @author Steve Jones
 */
public class PolicyHeader extends OrganizationHeader {
    //- PUBLIC
    public PolicyHeader(final Policy policy) {
        this(policy.getOid(),
             policy.isSoap(),
             policy.getName(),
             policy.getInternalTag(),
             policy.getGuid(),
             policy.getFolderOid(),
             policy.isAlias());
    }

    public PolicyHeader(final PolicyHeader policyHeader){
        this(policyHeader.getOid(),
             policyHeader.isSoap(),
             policyHeader.getName(),
             policyHeader.getDescription(),
             policyHeader.getGuid(),
             policyHeader.getFolderOid(),
             policyHeader.isAlias());
    }

    public PolicyHeader(final long oid,
                        final boolean isSoap,
                        final String name,
                        final String description,
                        final String policyGuid,
                        final Long folderOid,
                        final boolean alias)
    {
        super(oid, EntityType.POLICY, name, description);

        this.guid = policyGuid;
        this.isSoap = isSoap;
        this.folderOid = folderOid;
        this.alias = alias;
    }

    public boolean isSoap() {
        return isSoap;
    }

    public String getGuid() {
        return guid;
    }

    @Override
    public String toString() {
        return getName();
    }

    //- PRIVATE

    private final boolean isSoap;
    private final String guid;
}
