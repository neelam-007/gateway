package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityMappingInstructions;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class BundleTransformerTest {

    /**
     * This test case is to use a bundleTransformer object to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
     * to convert EntityMappingInstructions from mapping which defines MapBy = "routingUri".
     *
     * This test will verify a service EntityMappingInstructions with a right TargetMapping type (ROUTING_URI) and a source
     * entity header with right Mapping.Action (i.e., NewOrUpdate), right EntityType (i.e., SERVICE), and right srcId.
     */
    @Test
    public void testMapByUriToConvertEntityMappingInstructionsFromMappingAndEntity() throws ResourceFactory.InvalidResourceException {
        final Mapping mappingForTest = createMappingForTest("SERVICE", Mapping.Action.NewOrUpdate, "799eca6846c453e9a8e23ec887d6a341");

        final Map<String, Object> propertiesForTest = new HashMap(1);
        propertiesForTest.put(BundleTransformer.MapBy, "routingUri");
        propertiesForTest.put(BundleTransformer.MapTo, "/testMapByRoutingUri");
        mappingForTest.setProperties(propertiesForTest);

        final Bundle bundleForTest = ManagedObjectFactory.createBundle();
        bundleForTest.setMappings(Arrays.asList(new Mapping[]{mappingForTest}));

        // Use bundleTransformer to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
        final BundleTransformer bundleTransformer = new BundleTransformer();
        EntityBundle entityBundle = bundleTransformer.convertFromMO(bundleForTest, null);
        
        final List<EntityMappingInstructions> mappingInstructions = entityBundle.getMappingInstructions();
        assertTrue(mappingInstructions != null && mappingInstructions.size() == 1);

        // Verify returned Mapping.Action
        final EntityMappingInstructions serviceMappingInstructions = mappingInstructions.get(0);
        assertTrue(serviceMappingInstructions.getMappingAction() == EntityMappingInstructions.MappingAction.NewOrUpdate);

        // Verify returned TargetMapping.Type and TargetMapping.targetID
        final EntityMappingInstructions.TargetMapping serviceTargetMapping = serviceMappingInstructions.getTargetMapping();
        assertNotNull(serviceTargetMapping);
        assertTrue(serviceTargetMapping.getType() == EntityMappingInstructions.TargetMapping.Type.ROUTING_URI);
        assertTrue(serviceMappingInstructions.getTargetMapping().getTargetID().equals("/testMapByRoutingUri"));

        // Verify returned Source EntityHeader
        final EntityHeader sourceEntityHeader = serviceMappingInstructions.getSourceEntityHeader();
        assertNotNull(sourceEntityHeader);
        assertTrue(sourceEntityHeader.getType() == EntityType.SERVICE);
        assertTrue("799eca6846c453e9a8e23ec887d6a341".equals(sourceEntityHeader.getStrId()));
    }

    @NotNull
    private Mapping createMappingForTest(String type, Mapping.Action action, String id) {
        final Mapping mappingForTest = ManagedObjectFactory.createMapping();
        mappingForTest.setType(type);
        mappingForTest.setAction(action);
        mappingForTest.setSrcId(id);
        return mappingForTest;
    }

    /**
     * This test case is to use a bundleTransformer object to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
     * to convert EntityMappingInstructions from mapping which defines MapBy = "path".
     * 
     */
    @Test
    public void testMapByPathToConvertEntityMappingInstructionsFromMappingAndEntity() throws ResourceFactory.InvalidResourceException {
        final Mapping serviceMapping = createMappingForTest("SERVICE", Mapping.Action.NewOrUpdate, "799eca6846c453e9a8e23ec887d6a341");

        final Map<String, Object> propertiesForTest = new HashMap(2);
        propertiesForTest.put(BundleTransformer.MapBy, "path");
        propertiesForTest.put(BundleTransformer.MapTo, "/folder1/folder2/service1");
        serviceMapping.setProperties(propertiesForTest);

        final Bundle bundleForTest = ManagedObjectFactory.createBundle();
        bundleForTest.setMappings(Arrays.asList(new Mapping[]{serviceMapping}));

        // Use bundleTransformer to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
        final BundleTransformer bundleTransformer = new BundleTransformer();
        EntityBundle entityBundle = bundleTransformer.convertFromMO(bundleForTest, null);

        final List<EntityMappingInstructions> mappingInstructions = entityBundle.getMappingInstructions();
        assertTrue(mappingInstructions != null && mappingInstructions.size() == 1);

        // Verify returned Mapping.Action
        final EntityMappingInstructions serviceMappingInstructions = mappingInstructions.get(0);
        assertTrue(serviceMappingInstructions.getMappingAction() == EntityMappingInstructions.MappingAction.NewOrUpdate);

        // Verify returned service TargetMapping.Type and TargetMapping.targetID
        final EntityMappingInstructions.TargetMapping serviceTargetMapping = serviceMappingInstructions.getTargetMapping();
        assertNotNull(serviceTargetMapping);
        assertTrue(serviceTargetMapping.getType() == EntityMappingInstructions.TargetMapping.Type.PATH);
        assertTrue(serviceMappingInstructions.getTargetMapping().getTargetID().equals("/folder1/folder2/service1"));

        // Verify returned service Source EntityHeader
        final EntityHeader serviceSourceEntityHeader = serviceMappingInstructions.getSourceEntityHeader();
        assertNotNull(serviceSourceEntityHeader);
        assertTrue(serviceSourceEntityHeader.getType() == EntityType.SERVICE);
        assertTrue("799eca6846c453e9a8e23ec887d6a341".equals(serviceSourceEntityHeader.getStrId()));
    }
}
