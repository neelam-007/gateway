package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.AssertionSecurityZoneResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
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
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.ASSERTION_ACCESS.toString();
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
