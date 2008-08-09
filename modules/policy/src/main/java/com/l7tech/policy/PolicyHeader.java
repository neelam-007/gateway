package com.l7tech.policy;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

/**
 * Extension of EntityHeader with some policy information.
 *
 * @author Steve Jones
 */
public class PolicyHeader extends EntityHeader {

    //- PUBLIC
    public PolicyHeader(final Policy policy) {
        this(policy.getOid(), policy.isSoap(), policy.getName(), policy.getInternalTag(), policy.getGuid());
    }

    public PolicyHeader(final long oid,
                        final boolean isSoap,
                        final String name,
                        final String description,
                        final String policyGuid)
    {
        super(oid, EntityType.POLICY, name, description);

        this.guid = policyGuid;
        this.isSoap = isSoap;
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
