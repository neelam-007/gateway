package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityMappingInstructions;
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
        final Mapping mappingForTest = ManagedObjectFactory.createMapping();
        mappingForTest.setType("SERVICE");
        mappingForTest.setAction(Mapping.Action.NewOrUpdate);
        mappingForTest.setSrcId("799eca6846c453e9a8e23ec887d6a341");

        final Map<String, Object> propertiesForTest = new HashMap(1);
        propertiesForTest.put(BundleTransformer.MapBy, "routingUri");
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

        // Verify returned TargetMapping.Type
        final EntityMappingInstructions.TargetMapping serviceTargetMapping = serviceMappingInstructions.getTargetMapping();
        assertNotNull(serviceTargetMapping);
        assertTrue(serviceTargetMapping.getType() == EntityMappingInstructions.TargetMapping.Type.ROUTING_URI);

        // Verify returned Source EntityHeader
        final EntityHeader sourceEntityHeader = serviceMappingInstructions.getSourceEntityHeader();
        assertNotNull(sourceEntityHeader);
        assertTrue(sourceEntityHeader.getType() == EntityType.SERVICE);
        assertTrue("799eca6846c453e9a8e23ec887d6a341".equals(sourceEntityHeader.getStrId()));
    }
}
