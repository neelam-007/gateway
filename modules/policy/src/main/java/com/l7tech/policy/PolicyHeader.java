package com.l7tech.policy;

import com.l7tech.policy.Policy;
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
        this( policy.isSoap(),
              policy.getName(),
              policy.getInternalTag(),
              policy.getGuid());
    }

    public PolicyHeader(final boolean isSoap,
                        final String name,
                        final String description,
                        final String policyGuid) {
        super(policyGuid, EntityType.POLICY, name, description);

        this.isSoap = isSoap;
    }

    public boolean isSoap() {
        return isSoap;
    }

    @Override
    public String toString() {
        return getName();
    }

    //- PRIVATE

    private final boolean isSoap;
}