package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.BundleBuilder;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.APIUtilityLocator;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.bundling.EntityBundle;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.bundling.EntityMappingInstructions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BundleTransformerTest {
    @InjectMocks
    private BundleTransformer bundleTransformer;
    @Mock
    private APIUtilityLocator apiUtilityLocator;
    @Mock
    private PublishedServiceTransformer serviceTransformer;
    @Mock
    private ServiceAliasTransformer aliasTransformer;
    private ItemBuilder<ServiceMO> serviceItemBuilder;
    private ItemBuilder<ServiceAliasMO> serviceAliasBuilder;

    @Before
    public void setup() {
        serviceItemBuilder = new ItemBuilder<ServiceMO>("serviceMOBuilder", EntityType.SERVICE.toString());
        serviceAliasBuilder = new ItemBuilder<ServiceAliasMO>("serviceAliasBuilder", EntityType.SERVICE_ALIAS.toString());

        when(apiUtilityLocator.findTransformerByResourceType(EntityType.SERVICE.toString())).thenReturn(serviceTransformer);
        when(apiUtilityLocator.findTransformerByResourceType(EntityType.SERVICE_ALIAS.toString())).thenReturn(aliasTransformer);
    }

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
        mappingForTest.setProperties(propertiesForTest);

        final Bundle bundleForTest = ManagedObjectFactory.createBundle();
        bundleForTest.setMappings(Arrays.asList(new Mapping[]{mappingForTest}));

        // Use bundleTransformer to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
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

    /**
     * This test case is to use a bundleTransformer object to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
     * to convert EntityMappingInstructions from mapping which defines MapBy = "path".
     * 
     */
    @Test
    public void testMapByPathToConvertEntityMappingInstructionsFromMappingAndEntity() throws ResourceFactory.InvalidResourceException {
        final Mapping serviceMapping = createMappingForTest("SERVICE", Mapping.Action.NewOrUpdate, "799eca6846c453e9a8e23ec887d6a341");
        final Mapping folderMapping = createMappingForTest("FOLDER", Mapping.Action.AlwaysCreateNew, "799eca6846c453e9a8e23ec887d6a000");

        final Map<String, Object> propertiesForTest = new HashMap(2);
        propertiesForTest.put(BundleTransformer.MapBy, "path");
        serviceMapping.setProperties(propertiesForTest);
        propertiesForTest.put("MapTo", "/folder1/folder2");
        folderMapping.setProperties(propertiesForTest);

        final Bundle bundleForTest = ManagedObjectFactory.createBundle();
        bundleForTest.setMappings(Arrays.asList(new Mapping[]{serviceMapping, folderMapping}));

        // Use bundleTransformer to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
        EntityBundle entityBundle = bundleTransformer.convertFromMO(bundleForTest, null);

        final List<EntityMappingInstructions> mappingInstructions = entityBundle.getMappingInstructions();
        assertTrue(mappingInstructions != null && mappingInstructions.size() == 2);

        // Verify returned Mapping.Action
        final EntityMappingInstructions serviceMappingInstructions = mappingInstructions.get(0);
        final EntityMappingInstructions folderMappingInstructions = mappingInstructions.get(1);
        assertTrue(serviceMappingInstructions.getMappingAction() == EntityMappingInstructions.MappingAction.NewOrUpdate);
        assertTrue(folderMappingInstructions.getMappingAction() == EntityMappingInstructions.MappingAction.AlwaysCreateNew);


        // Verify returned service TargetMapping.Type
        final EntityMappingInstructions.TargetMapping serviceTargetMapping = serviceMappingInstructions.getTargetMapping();
        assertNotNull(serviceTargetMapping);
        assertTrue(serviceTargetMapping.getType() == EntityMappingInstructions.TargetMapping.Type.PATH);

        //Verify returned folder TargetMapping.Type
        assertNotNull(folderMappingInstructions.getTargetMapping());
        assertTrue( folderMappingInstructions.getTargetMapping().getType() == EntityMappingInstructions.TargetMapping.Type.PATH);
        assertTrue(folderMappingInstructions.getTargetMapping().getTargetID().equals("/folder1/folder2"));

        // Verify returned service Source EntityHeader
        final EntityHeader serviceSourceEntityHeader = serviceMappingInstructions.getSourceEntityHeader();
        assertNotNull(serviceSourceEntityHeader);
        assertTrue(serviceSourceEntityHeader.getType() == EntityType.SERVICE);
        assertTrue("799eca6846c453e9a8e23ec887d6a341".equals(serviceSourceEntityHeader.getStrId()));

        // Verify returned service Source EntityHeader
        final EntityHeader folderSourceEntityHeader = folderMappingInstructions.getSourceEntityHeader();
        assertNotNull(folderSourceEntityHeader);
        assertTrue(folderSourceEntityHeader.getType() == EntityType.FOLDER);
        assertTrue("799eca6846c453e9a8e23ec887d6a000".equals(folderSourceEntityHeader.getStrId()));
    }

    @Test
    public void convertBundleWithAliasSetsAliasNameOnMapping() throws Exception {
        final Goid serviceId = new Goid(0, 1);
        final String serviceName = "test";
        final PublishedService service = new PublishedService();
        service.setGoid(new Goid(serviceId));
        service.setName(serviceName);
        final Goid aliasId = new Goid(1, 1);
        final PublishedServiceAlias alias = new PublishedServiceAlias(service, new Folder("aliases", new Folder("root", null)));
        alias.setGoid(aliasId);
        when(serviceTransformer.convertFromMO(any(ServiceMO.class), eq(false), eq(null))).thenReturn(new EntityContainer<>(service));
        when(aliasTransformer.convertFromMO(any(ServiceAliasMO.class), eq(false), eq(null))).thenReturn(new EntityContainer<>(alias));

        final EntityBundle result = bundleTransformer.convertFromMO(new BundleBuilder().addServiceAlias(service, alias).build(), null);
        final Map<Goid, EntityMappingInstructions> instructionsMap = instructionsToMap(result.getMappingInstructions());
        assertEquals(2, instructionsMap.size());
        assertEquals("test alias", instructionsMap.get(aliasId).getSourceEntityHeader().getName());
    }

    private Map<Goid, EntityMappingInstructions> instructionsToMap(List<EntityMappingInstructions> instructions) {
        return instructions.stream().collect(Collectors.toMap(instruction -> instruction.getSourceEntityHeader().getGoid(), instruction -> instruction));
    }

    @NotNull
    private Mapping createMappingForTest(String type, Mapping.Action action, String id) {
        final Mapping mappingForTest = ManagedObjectFactory.createMapping();
        mappingForTest.setType(type);
        mappingForTest.setAction(action);
        mappingForTest.setSrcId(id);
        return mappingForTest;
    }
}
