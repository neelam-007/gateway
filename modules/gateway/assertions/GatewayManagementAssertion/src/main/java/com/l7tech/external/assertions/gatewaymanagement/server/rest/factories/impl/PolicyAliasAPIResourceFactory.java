package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.PolicyAliasMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class PolicyAliasAPIResourceFactory extends WsmanBaseResourceFactory<PolicyAliasMO, PolicyAliasResourceFactory> {

    public PolicyAliasAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("policy.id", "entityGoid")
                        .put("folder.id", "folder.id")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("policy.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("entityGoid", RestResourceFactoryUtils.goidConvert))
                        .put("folder.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("folder.id", RestResourceFactoryUtils.goidConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public EntityType getEntityType(){
        return EntityType.POLICY_ALIAS;
    }

    @Override
    @Inject
    public void setFactory(PolicyAliasResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public PolicyAliasMO getResourceTemplate() {
        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId("Folder ID");
        policyAliasMO.setPolicyReference(new ManagedObjectReference());
        return policyAliasMO;

    }
}
