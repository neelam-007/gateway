package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.BundleBuilder;
import com.l7tech.external.assertions.gatewaymanagement.server.MappingBuilder;
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

    @Before
    public void setup() {
        when(apiUtilityLocator.findTransformerByResourceType(EntityType.SERVICE.toString())).thenReturn(serviceTransformer);
        when(apiUtilityLocator.findTransformerByResourceType(EntityType.SERVICE_ALIAS.toString())).thenReturn(aliasTransformer);
    }

    /**
     * This test case is to use a bundleTransformer object to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
     * to convert EntityMappingInstructions from mapping which defines MAP_BY = "routingUri".
     *
     * This test will verify a service EntityMappingInstructions with a right TargetMapping type (ROUTING_URI) and a source
     * entity header with right Mapping.Action (i.e., NewOrUpdate), right EntityType (i.e., SERVICE), and right srcId.
     */
    @Test
    public void testMapByUriToConvertEntityMappingInstructionsFromMappingAndEntity() throws ResourceFactory.InvalidResourceException {
        final Mapping mappingForTest = createMappingForTest("SERVICE", Mapping.Action.NewOrUpdate, "799eca6846c453e9a8e23ec887d6a341");

        final Map<String, Object> propertiesForTest = new HashMap(1);
        propertiesForTest.put(BundleTransformer.MAP_BY, "routingUri");
        propertiesForTest.put(BundleTransformer.MAP_TO, "/testMapByRoutingUri");
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

    /**
     * This test case is to use a bundleTransformer object to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
     * to convert EntityMappingInstructions from mapping which defines MAP_BY = "path".
     * 
     */
    @Test
    public void testMapByPathToConvertEntityMappingInstructionsFromMappingAndEntity() throws ResourceFactory.InvalidResourceException {
        final Mapping serviceMapping = createMappingForTest("SERVICE", Mapping.Action.NewOrUpdate, "799eca6846c453e9a8e23ec887d6a341");

        final Map<String, Object> propertiesForTest = new HashMap(2);
        propertiesForTest.put(BundleTransformer.MAP_BY, "path");
        propertiesForTest.put(BundleTransformer.MAP_TO, "/folder1/folder2/service1");
        serviceMapping.setProperties(propertiesForTest);

        final Bundle bundleForTest = ManagedObjectFactory.createBundle();
        bundleForTest.setMappings(Arrays.asList(new Mapping[]{serviceMapping}));

        // Use bundleTransformer to call convertFromMO, which will call convertEntityMappingInstructionsFromMappingAndEntity
        final EntityBundle entityBundle = bundleTransformer.convertFromMO(bundleForTest, null);

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

    @Test
    public void testMultiBundleOnConvertFromMO() throws ResourceFactory.InvalidResourceException {
        // Prepare one mapping list for bundle #1
        final Mapping mapping1_1 = createMappingForTest("FOLDER", Mapping.Action.NewOrUpdate, "599eca6846c453e9a8e23ec887d6a341");
        final Mapping mapping1_2 = createMappingForTest("SERVICE", Mapping.Action.NewOrUpdate, "699eca6846c453e9a8e23ec887d6a342");
        final List<Mapping> mappingList1 = Arrays.asList(new Mapping[]{mapping1_1, mapping1_2});

        // Prepare one mapping list for bundle #2
        final Mapping mapping2_1 = createMappingForTest("SERVICE", Mapping.Action.NewOrUpdate, "799eca6846c453e9a8e23ec887d6a343");
        final Mapping mapping2_2 = createMappingForTest("POLICY", Mapping.Action.NewOrUpdate, "899eca6846c453e9a8e23ec887d6a344");
        final List<Mapping> mappingList2 = Arrays.asList(new Mapping[]{mapping2_1, mapping2_2});

        // Prepare a BundleList object using the above two mapping lists.
        final BundleList bundleList = ManagedObjectFactory.createBundleList();
        final Bundle bundle1 = ManagedObjectFactory.createBundle();
        final Bundle bundle2 = ManagedObjectFactory.createBundle();

        bundle1.setMappings(mappingList1);
        bundle2.setMappings(mappingList2);
        bundleList.setBundles(Arrays.asList(new Bundle[]{bundle1, bundle2}));

        // Call convertFromMO for multiple bundles
        final List<EntityBundle> entityBundles = bundleTransformer.convertFromMO(bundleList, null);
        assertTrue(entityBundles.size() == 2);

        // Verify the first EntityBundle object.
        List<EntityMappingInstructions> mappingInstructionsList = entityBundles.get(0).getMappingInstructions();
        EntityMappingInstructions mappingInstructions = mappingInstructionsList.get(0);
        assertTrue(mappingInstructions.getSourceEntityHeader().getType() == EntityType.FOLDER);
        assertTrue(mappingInstructions.getSourceEntityHeader().getStrId().equals("599eca6846c453e9a8e23ec887d6a341"));
        assertTrue(mappingInstructions.getMappingAction() == EntityMappingInstructions.MappingAction.NewOrUpdate);

        mappingInstructions = mappingInstructionsList.get(1);
        assertTrue(mappingInstructions.getSourceEntityHeader().getType() == EntityType.SERVICE);
        assertTrue(mappingInstructions.getSourceEntityHeader().getStrId().equals("699eca6846c453e9a8e23ec887d6a342"));
        assertTrue(mappingInstructions.getMappingAction() == EntityMappingInstructions.MappingAction.NewOrUpdate);

        // Verify the second EntityBundle object.
        mappingInstructionsList = entityBundles.get(1).getMappingInstructions();
        mappingInstructions = mappingInstructionsList.get(0);
        assertTrue(mappingInstructions.getSourceEntityHeader().getType() == EntityType.SERVICE);
        assertTrue(mappingInstructions.getSourceEntityHeader().getStrId().equals("799eca6846c453e9a8e23ec887d6a343"));
        assertTrue(mappingInstructions.getMappingAction() == EntityMappingInstructions.MappingAction.NewOrUpdate);

        mappingInstructions = mappingInstructionsList.get(1);
        assertTrue(mappingInstructions.getSourceEntityHeader().getType() == EntityType.POLICY);
        assertTrue(mappingInstructions.getSourceEntityHeader().getStrId().equals("899eca6846c453e9a8e23ec887d6a344"));
        assertTrue(mappingInstructions.getMappingAction() == EntityMappingInstructions.MappingAction.NewOrUpdate);
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

    private Map<Goid, EntityMappingInstructions> instructionsToMap(final List<EntityMappingInstructions> instructions) {
        final Map<Goid, EntityMappingInstructions> map = new HashMap<>();
        for (final EntityMappingInstructions instruction : instructions) {
            map.put(instruction.getSourceEntityHeader().getGoid(), instruction);
        }
        return map;
    }

    @NotNull
    private Mapping createMappingForTest(final String type, final Mapping.Action action, final String id) {
        return new MappingBuilder().withType(type).withAction(action).withSrcId(id).build();
    }
}
