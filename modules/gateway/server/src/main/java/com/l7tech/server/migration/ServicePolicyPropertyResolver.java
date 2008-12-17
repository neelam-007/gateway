package com.l7tech.server.migration;

import com.l7tech.objectmodel.migration.DefaultEntityPropertyResolver;
import com.l7tech.objectmodel.migration.PropertyResolverException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.gateway.common.service.PublishedService;

/**
 * @author jbufu
 */
public class ServicePolicyPropertyResolver extends DefaultEntityPropertyResolver {

    public void applyMapping(Entity sourceEntity, String propName, Object targetValue, EntityHeader originalHeader) throws PropertyResolverException {

        PublishedService service = (PublishedService) sourceEntity;
        long policyOid = service.getPolicy().getOid();
        super.applyMapping(sourceEntity, propName, targetValue, originalHeader);
        service.getPolicy().setOid(policyOid);

    }
}
