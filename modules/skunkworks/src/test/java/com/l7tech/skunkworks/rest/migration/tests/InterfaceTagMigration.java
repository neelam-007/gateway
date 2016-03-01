package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.skunkworks.rest.tools.JVMDatabaseBasedRestManagementEnvironment;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class InterfaceTagMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(InterfaceTagMigration.class.getName());

    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
        //delete all interface tags
        RestResponse response = getSourceEnvironment().processRequest("interfaceTags", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        ItemsList<InterfaceTagMO> interfaceTags = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        if(interfaceTags.getContent() != null) {
            for (Item<InterfaceTagMO> interfaceTagItem : interfaceTags.getContent()) {
                response = getSourceEnvironment().processRequest("interfaceTags/" + interfaceTagItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
                assertOkEmptyResponse(response);
            }
        }

        response = getTargetEnvironment().processRequest("interfaceTags", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        interfaceTags = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        if(interfaceTags.getContent() != null) {
            for (Item<InterfaceTagMO> interfaceTagItem : interfaceTags.getContent()) {
                response = getTargetEnvironment().processRequest("interfaceTags/" + interfaceTagItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
                assertOkEmptyResponse(response);
            }
        }
    }

    @Test
    public void testExportSingle() throws Exception {
        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));

        RestResponse response = getSourceEnvironment().processRequest("bundle?interfaceTag=" + sourceInterfaceTagItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A interfaceTag", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 items. A interfaceTag", 1, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnoreInterfaceTagDependencies() throws Exception {
        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));


        RestResponse response = getSourceEnvironment().processRequest("bundle?interfaceTag=" + sourceInterfaceTagItem.getId() + "&requireInterfaceTag=" + sourceInterfaceTagItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A interfaceTag", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. A interfaceTag", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
    }

    @Test
    public void testImportNewOrExisting() throws Exception {
        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, interfaceTagMapping.getActionTaken());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());
        Assert.assertEquals(interfaceTagMapping.getSrcId(), interfaceTagMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrExistingFailOnNew() throws Exception {
        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);

        interfaceTagMapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnNew", true).map());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.TargetNotFound, interfaceTagMapping.getErrorType());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrExistingFailOnExisting() throws Exception {

        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));
        Item<InterfaceTagMO> targetInterfaceTagItem = createInterfaceTag(getTargetEnvironment(), "MyTag", Arrays.asList("3.3.3.3"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);

        interfaceTagMapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnExisting", true).map());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.TargetExists, interfaceTagMapping.getErrorType());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrExistingUseExisting() throws Exception {

        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));
        Item<InterfaceTagMO> targetInterfaceTagItem = createInterfaceTag(getTargetEnvironment(), "MyTag2", Arrays.asList("3.3.3.3"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setTargetId(targetInterfaceTagItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, interfaceTagMapping.getActionTaken());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());
        Assert.assertEquals(targetInterfaceTagItem.getId(), interfaceTagMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrUpdate() throws Exception {
        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setAction(Mapping.Action.NewOrUpdate);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, interfaceTagMapping.getActionTaken());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());
        Assert.assertEquals(interfaceTagMapping.getSrcId(), interfaceTagMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrUpdateFailOnNew() throws Exception {
        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setAction(Mapping.Action.NewOrUpdate);
        interfaceTagMapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnNew", true).map());


        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.TargetNotFound, interfaceTagMapping.getErrorType());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrUpdateFailOnExisting() throws Exception {
        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));
        Item<InterfaceTagMO> targetInterfaceTagItem = createInterfaceTag(getTargetEnvironment(), "MyTag", Arrays.asList("3.3.3.3"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setAction(Mapping.Action.NewOrUpdate);
        interfaceTagMapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnExisting", true).map());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.TargetExists, interfaceTagMapping.getErrorType());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrUpdateUpdateExisting() throws Exception {

        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));
        Item<InterfaceTagMO> targetInterfaceTagItem = createInterfaceTag(getTargetEnvironment(), "MyTag2", Arrays.asList("3.3.3.3"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setAction(Mapping.Action.NewOrUpdate);
        interfaceTagMapping.setTargetId(targetInterfaceTagItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, interfaceTagMapping.getActionTaken());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());
        Assert.assertEquals(targetInterfaceTagItem.getId(), interfaceTagMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportAlwaysCreateNew() throws Exception {
        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setAction(Mapping.Action.AlwaysCreateNew);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, interfaceTagMapping.getActionTaken());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());
        Assert.assertEquals(interfaceTagMapping.getSrcId(), interfaceTagMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportAlwaysCreateNewExisting() throws Exception {

        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));
        Item<InterfaceTagMO> targetInterfaceTagItem = createInterfaceTag(getTargetEnvironment(), "MyTag2", Arrays.asList("3.3.3.3"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: remove this method once SSG-13071 is fixed and the root cause of this test intermittent failure is found and fixed
        printAnyPotentialFailures(sourceInterfaceTagItem, bundleItem, response);
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setAction(Mapping.Action.AlwaysCreateNew);
        interfaceTagMapping.setTargetId(targetInterfaceTagItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, interfaceTagMapping.getActionTaken());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());
        Assert.assertEquals(interfaceTagMapping.getSrcId(), interfaceTagMapping.getTargetId());

        validate(mappings);
    }

    // TODO: remove this method once SSG-13071 is fixed and the root cause of this test intermittent failure is found and fixed
    private void printAnyPotentialFailures(final Item<InterfaceTagMO> sourceInterfaceTagItem, final Item<Bundle> bundleItem, final RestResponse response) {
        try {
            final String id = sourceInterfaceTagItem.getId();
            final Collection<Mapping> mappings = bundleItem.getContent().getMappings();
            final Mapping mapping = Functions.grepFirst(mappings, new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return id.equals(mapping.getSrcId());
                }
            });
            if (mapping == null) {
                System.out.println("============================================================================================");
                System.out.println("InterfaceTagMigration FAILURE mapping is NULL!!!!");
                System.out.println("============================================================================================");
                System.out.println(
                        "mappings: " + Arrays.toString(Functions.map(mappings, new Functions.Unary<String, Mapping>() {
                            @Override
                            public String call(final Mapping mapping) {
                                return "Mapping [type=" + mapping.getType() + ", " +
                                        "action=" + mapping.getAction() + ", " +
                                        "actionTaken=" + mapping.getActionTaken() + ", " +
                                        "errorType=" + mapping.getErrorType() + ", " +
                                        "srcId=" + mapping.getSrcId() + ", " +
                                        "targetId=" + mapping.getTargetId() + "]";
                            }
                        }).toArray(new String[mappings.size()]))
                );
                System.out.println("response: " + response.getBody());
                System.out.println("============================================================================================");
            }
        } catch (final Exception e) {
            System.err.println("!!!! InterfaceTagMigration FAILURE: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    @Test
    public void testImportAlwaysCreateNewConflict() throws Exception {

        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));
        Item<InterfaceTagMO> targetInterfaceTagItem = createInterfaceTag(getTargetEnvironment(), "MyTag", Arrays.asList("3.3.3.3"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setAction(Mapping.Action.AlwaysCreateNew);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.UniqueKeyConflict, interfaceTagMapping.getErrorType());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());

        validate(mappings);
    }

    @Test
    public void testDelete() throws Exception {

        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));
        Item<InterfaceTagMO> targetInterfaceTagItem = createInterfaceTag(getTargetEnvironment(), "MyTag2", Arrays.asList("3.3.3.3"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setAction(Mapping.Action.Delete);
        interfaceTagMapping.setTargetId(targetInterfaceTagItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, interfaceTagMapping.getActionTaken());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());
        Assert.assertEquals(targetInterfaceTagItem.getId(), interfaceTagMapping.getTargetId());

        validate(mappings);

        response = getTargetEnvironment().processRequest("interfaceTags/" + targetInterfaceTagItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
        assertNotFoundResponse(response);
    }

    @Test
    public void testDeleteEntityMissing() throws Exception {

        Item<InterfaceTagMO> sourceInterfaceTagItem = createInterfaceTag(getSourceEnvironment(), "MyTag", Arrays.asList("1.1.1.1", "2.2.2.2"));

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the interface tags are exported
        Mapping interfaceTagMapping = getMapping(bundleItem.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        interfaceTagMapping.setAction(Mapping.Action.Delete);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        interfaceTagMapping = getMapping(mappings.getContent().getMappings(), sourceInterfaceTagItem.getId());
        Assert.assertNotNull(interfaceTagMapping);
        Assert.assertEquals(EntityType.INTERFACE_TAG.toString(), interfaceTagMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, interfaceTagMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, interfaceTagMapping.getActionTaken());
        Assert.assertEquals(sourceInterfaceTagItem.getId(), interfaceTagMapping.getSrcId());

        validate(mappings);

    }

    private Item<InterfaceTagMO> createInterfaceTag(JVMDatabaseBasedRestManagementEnvironment environment, String name, List<String> addresses) throws Exception {
        InterfaceTagMO interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setName(name);
        interfaceTagMO.setAddressPatterns(addresses);

        RestResponse response = environment.processRequest("interfaceTags", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(interfaceTagMO)));
        assertOkCreatedResponse(response);
        Item createdItem =  MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = environment.processRequest("interfaceTags/"+createdItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);
        return MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }
}
