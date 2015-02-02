package com.l7tech.skunkworks.rest.migration;

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
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.logging.Logger;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class SampleMessageMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(SampleMessageMigrationTest.class.getName());

    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
        //delete all sample messages
        RestResponse response = getSourceEnvironment().processRequest("sampleMessages", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        ItemsList<SampleMessageMO> sampleMessages = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        if(sampleMessages.getContent() != null) {
            for (Item<SampleMessageMO> sampleMessageItem : sampleMessages.getContent()) {
                response = getSourceEnvironment().processRequest("sampleMessages/" + sampleMessageItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
                assertOkEmptyResponse(response);
            }
        }

        response = getTargetEnvironment().processRequest("sampleMessages", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        sampleMessages = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        if(sampleMessages.getContent() != null) {
            for (Item<SampleMessageMO> sampleMessageItem : sampleMessages.getContent()) {
                response = getTargetEnvironment().processRequest("sampleMessages/" + sampleMessageItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
                assertOkEmptyResponse(response);
            }
        }
    }

    @Test
    public void testImportNewOrExisting() throws Exception {
        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MyMessage");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample messages are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, sampleMessageMapping.getActionTaken());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());
        Assert.assertEquals(sampleMessageMapping.getSrcId(), sampleMessageMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrExistingFailOnNew() throws Exception {
        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MyMessage");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample messages are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);

        sampleMessageMapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnNew", true).map());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.TargetNotFound, sampleMessageMapping.getErrorType());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrExistingFailOnExisting() throws Exception {

        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MyMessage");
        Item<SampleMessageMO> targetSampleMessageItem = createSampleMessage(getTargetEnvironment(), "MyMessage");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample messages are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);

        sampleMessageMapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnExisting", true).put("MapBy", "name").map());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.TargetExists, sampleMessageMapping.getErrorType());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrExistingUseExisting() throws Exception {

        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MyMessage");
        Item<SampleMessageMO> targetSampleMessageItem = createSampleMessage(getTargetEnvironment(), "MyMessage2");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample messages are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        sampleMessageMapping.setTargetId(targetSampleMessageItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, sampleMessageMapping.getActionTaken());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());
        Assert.assertEquals(targetSampleMessageItem.getId(), sampleMessageMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrUpdate() throws Exception {
        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MySampleMessage");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample messages are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        sampleMessageMapping.setAction(Mapping.Action.NewOrUpdate);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, sampleMessageMapping.getActionTaken());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());
        Assert.assertEquals(sampleMessageMapping.getSrcId(), sampleMessageMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrUpdateFailOnNew() throws Exception {
        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MySampleMessage");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample messages are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        sampleMessageMapping.setAction(Mapping.Action.NewOrUpdate);
        sampleMessageMapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnNew", true).map());


        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.TargetNotFound, sampleMessageMapping.getErrorType());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrUpdateFailOnExisting() throws Exception {
        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MySampleMessage");
        Item<SampleMessageMO> targetSampleMessageItem = createSampleMessage(getTargetEnvironment(), "MySampleMessage");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample message are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        sampleMessageMapping.setAction(Mapping.Action.NewOrUpdate);
        sampleMessageMapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnExisting", true).put("MapBy", "name").map());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.TargetExists, sampleMessageMapping.getErrorType());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());

        validate(mappings);
    }

    @Test
    public void testImportNewOrUpdateUpdateExisting() throws Exception {

        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MySampleMessage");
        Item<SampleMessageMO> targetSampleMessageItem = createSampleMessage(getTargetEnvironment(), "MySampleMessage2");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample message are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        sampleMessageMapping.setAction(Mapping.Action.NewOrUpdate);
        sampleMessageMapping.setTargetId(targetSampleMessageItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, sampleMessageMapping.getActionTaken());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());
        Assert.assertEquals(targetSampleMessageItem.getId(), sampleMessageMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportAlwaysCreateNew() throws Exception {
        Item<SampleMessageMO> sampleMessageItem = createSampleMessage(getSourceEnvironment(), "MySampleMessage");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample message are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        sampleMessageMapping.setAction(Mapping.Action.AlwaysCreateNew);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, sampleMessageMapping.getActionTaken());
        Assert.assertEquals(sampleMessageItem.getId(), sampleMessageMapping.getSrcId());
        Assert.assertEquals(sampleMessageMapping.getSrcId(), sampleMessageMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportAlwaysCreateNewExisting() throws Exception {

        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MySampleMessage");
        Item<SampleMessageMO> targetSampleMessageItem = createSampleMessage(getTargetEnvironment(), "MySampleMessage2");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample message are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        sampleMessageMapping.setAction(Mapping.Action.AlwaysCreateNew);
        sampleMessageMapping.setTargetId(targetSampleMessageItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.AlwaysCreateNew, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, sampleMessageMapping.getActionTaken());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());
        Assert.assertNotSame(sampleMessageMapping.getSrcId(), sampleMessageMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testDelete() throws Exception {

        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MySampleMessage");
        Item<SampleMessageMO> targetSampleMessageItem = createSampleMessage(getTargetEnvironment(), "MySampleMessage2");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample message are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        sampleMessageMapping.setAction(Mapping.Action.Delete);
        sampleMessageMapping.setTargetId(targetSampleMessageItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, sampleMessageMapping.getActionTaken());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());
        Assert.assertEquals(targetSampleMessageItem.getId(), sampleMessageMapping.getTargetId());

        validate(mappings);

        response = getTargetEnvironment().processRequest("sampleMessage/" + targetSampleMessageItem.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
        assertNotFoundResponse(response);
    }

    @Test
    public void testDeleteEntityMissing() throws Exception {

        Item<SampleMessageMO> sourceSampleMessageItem = createSampleMessage(getSourceEnvironment(), "MySampleMessage");

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the sample messages are exported
        Mapping sampleMessageMapping = getMapping(bundleItem.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        sampleMessageMapping.setAction(Mapping.Action.Delete);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        sampleMessageMapping = getMapping(mappings.getContent().getMappings(), sourceSampleMessageItem.getId());
        Assert.assertNotNull(sampleMessageMapping);
        Assert.assertEquals(EntityType.SAMPLE_MESSAGE.toString(), sampleMessageMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, sampleMessageMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, sampleMessageMapping.getActionTaken());
        Assert.assertEquals(sourceSampleMessageItem.getId(), sampleMessageMapping.getSrcId());

        validate(mappings);

    }

    private Item<SampleMessageMO> createSampleMessage(JVMDatabaseBasedRestManagementEnvironment environment, String name) throws Exception {
        SampleMessageMO sampleMessageMO = ManagedObjectFactory.createSampleMessageMO();
        sampleMessageMO.setName(name);
        sampleMessageMO.setXml("zksdhg;lkdjsfv");

        RestResponse response = environment.processRequest("sampleMessages", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(sampleMessageMO)));
        assertOkCreatedResponse(response);
        Item createdItem =  MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = environment.processRequest("sampleMessages/"+createdItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);
        return MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }
}
