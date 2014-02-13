package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.RevocationCheckingPolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.RevocationCheckingPolicyMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
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
public class RevocationCheckingPolicyAPIResourceFactory extends WsmanBaseResourceFactory<RevocationCheckingPolicyMO, RevocationCheckingPolicyResourceFactory> {

    public RevocationCheckingPolicyAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.REVOCATION_CHECK_POLICY.toString();
    }

    @Override
    @Inject
    public void setFactory(RevocationCheckingPolicyResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public RevocationCheckingPolicyMO getResourceTemplate() {
        RevocationCheckingPolicyMO revocationCheckingPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        revocationCheckingPolicyMO.setName("TemplateRevocationCheckPolicy");
        revocationCheckingPolicyMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("Property", "PropertyValue").map());
        return revocationCheckingPolicyMO;
    }
}
