package com.l7tech.objectmodel;

import com.l7tech.service.PublishedService;
import com.l7tech.common.policy.Policy;

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
              "",
              policy.getId());
    }

    public PolicyHeader(final boolean isSoap,
                        final String name,
                        final String description,
                        final String policyId) {
        super(policyId, EntityType.POLICY, name, description);

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