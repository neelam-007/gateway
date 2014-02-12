package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.AssertionSecurityZoneResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Component
public class AssertionSecurityZoneAPIResourceFactory extends WsmanBaseResourceFactory<AssertionSecurityZoneMO, AssertionSecurityZoneResourceFactory> {

    public AssertionSecurityZoneAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name")
                        .put("securityZone.id", "securityZone.id")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.ASSERTION_ACCESS;
    }

    @Override
    @Inject
    public void setFactory(AssertionSecurityZoneResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public AssertionSecurityZoneMO getResourceTemplate() {
        AssertionSecurityZoneMO activeConnectorMO = ManagedObjectFactory.createAssertionAccess();
        activeConnectorMO.setName("TemplateAssertionSecurityZone");
        activeConnectorMO.setSecurityZoneId("SecurityZoneID");
        return activeConnectorMO;
    }

    public AssertionSecurityZoneMO updateResourceByName(String name, AssertionSecurityZoneMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return factory.putResource( CollectionUtils.MapBuilder.<String, String>builder().put("name", name).map(), resource);
    }

    public AssertionSecurityZoneMO getResourceByName(String name) throws ResourceFactory.ResourceNotFoundException {
        return factory.getResource(CollectionUtils.MapBuilder.<String, String>builder().put("name", name).map());
    }

    public Iterable<AssertionSecurityZoneMO> listResources(final String sortKey, final Boolean ascending, final Map<String, List<Object>> filtersMap) {
        return factory.listResources(sortKey,ascending, filtersMap);
    }
}
