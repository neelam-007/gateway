package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyDetail;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.util.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class PolicyRestResourceFactory extends WsmanBaseResourceFactory<PolicyMO, com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory> {

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public PolicyMO getResourceTemplate() {
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setGuid("Policy Guid");

        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setFolderId("FolderID");
        policyDetail.setName("Policy Name");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String,Object>builder().put("PropertyKey","PropertyValue").map());

        policyMO.setPolicyDetail(policyDetail);
        return policyMO;
    }
}