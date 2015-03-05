package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.AssertionSecurityZoneResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;
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
    @Named("assertionSecurityZoneResourceFactory")
    public void setFactory(AssertionSecurityZoneResourceFactory factory) {
        super.factory = factory;
    }

    public AssertionSecurityZoneMO updateResourceByName(String name, AssertionSecurityZoneMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return factory.putResource( CollectionUtils.MapBuilder.<String, String>builder().put("name", name).map(), resource);
    }

    public AssertionSecurityZoneMO getResourceByName(String name) throws ResourceFactory.ResourceNotFoundException {
        return factory.getResource(CollectionUtils.MapBuilder.<String, String>builder().put("name", name).map());
    }

    public List<AssertionSecurityZoneMO> listResources(final String sortKey, final Boolean ascending, final Map<String, List<Object>> filtersMap) {
        return Functions.map(factory.listResources(sortKey, ascending, filtersMap), new Functions.Unary<AssertionSecurityZoneMO, AssertionSecurityZoneMO>() {
            @Override
            public AssertionSecurityZoneMO call(AssertionSecurityZoneMO assertionSecurityZoneMO) {
                return assertionSecurityZoneMO;
            }
        });
    }
}
