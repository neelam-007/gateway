package com.l7tech.server.migration;

import com.l7tech.objectmodel.migration.DefaultEntityPropertyResolver;
import com.l7tech.objectmodel.migration.PropertyResolverException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;

/**
 * When applying mappings for Service entities, and if the Service needs to up updated (rather than copied),
 * the OID and version of the policy from the mapped/target entity must be preserved.
 *
 * @author jbufu
 */
public class ServicePolicyPropertyResolver extends DefaultEntityPropertyResolver {

    public void applyMapping(Entity sourceEntity, String propName, Object targetValue, EntityHeader originalHeader) throws PropertyResolverException {

        Policy originalPolicy = ((PublishedService) sourceEntity).getPolicy();
        long originalPolicyOid = originalPolicy.getOid();
        int originalPolicyVersion = originalPolicy.getVersion();

        super.applyMapping(sourceEntity, propName, targetValue, originalHeader);

        ((PublishedService) sourceEntity).getPolicy().setOid(originalPolicyOid);
        ((PublishedService) sourceEntity).getPolicy().setVersion(originalPolicyVersion);

    }
}
