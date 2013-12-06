package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ClusterPropertyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class ClusterPropertyRestResourceFactory extends WsmanBaseResourceFactory<ClusterPropertyMO, ClusterPropertyResourceFactory> {

    public ClusterPropertyRestResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name").map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .map());
    }

    @Override
    @Inject
    public void setFactory(ClusterPropertyResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public ClusterPropertyMO getResourceTemplate() {
        ClusterPropertyMO clusterPropertyMO = ManagedObjectFactory.createClusterProperty();
        clusterPropertyMO.setName("Template Cluster Property Name");
        clusterPropertyMO.setValue("Template Cluster Property Value");
        return clusterPropertyMO;
    }
}