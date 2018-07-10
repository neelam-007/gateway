package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class GeneralMigration extends MigrationTestBase {
    private static final Logger logger = Logger.getLogger(GeneralMigration.class.getName());

    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    }

    /**
     * Test empty bundle references
     * @throws Exception
     */
    @Test
    public void emptyBundleReferencesTest() throws Exception {
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        folderMO.setName("New Target Folder");
        RestResponse response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));
        assertOkCreatedResponse(response);
        Item<FolderMO> folderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderMO.setId(folderCreated.getId());
        folderCreated.setContent(folderMO);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(folderCreated.getId());
        mapping.setType(folderCreated.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(folderCreated.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
        bundle.setReferences(Collections.<Item>emptyList());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 2 mapping after the import", 2, mappings.getContent().getMappings().size());
        Mapping activeConnectorMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), activeConnectorMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
        Assert.assertEquals(folderCreated.getId(), activeConnectorMapping.getTargetId());

        Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), activeConnectorMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

        response = getTargetEnvironment().processRequest("folders/"+folderCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);
    }

    /**
     * Test null bundle references
     * @throws Exception
     */
    @Test
    public void missingBundleReferencesTest() throws Exception {
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        folderMO.setName("New Target Folder");
        RestResponse response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));
        assertOkCreatedResponse(response);
        Item<FolderMO> folderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderMO.setId(folderCreated.getId());
        folderCreated.setContent(folderMO);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(folderCreated.getId());
        mapping.setType(folderCreated.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(folderCreated.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 2 mapping after the import", 2, mappings.getContent().getMappings().size());
        Mapping activeConnectorMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), activeConnectorMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
        Assert.assertEquals(folderCreated.getId(), activeConnectorMapping.getTargetId());

        Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), activeConnectorMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

        response = getTargetEnvironment().processRequest("folders/"+folderCreated.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);
    }

    @Test
    public void ignoreAndFailOnNewTest() throws Exception {

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Ignore);
        mapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnNew", true).map());
        mapping.setTargetId(Goid.DEFAULT_GOID.toString());
        mapping.setType("FOLDER");

        bundle.setMappings(Arrays.asList(mapping));
        bundle.setReferences(Collections.<Item>emptyList());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        RestResponse response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 1 mapping after the import", 1, mappings.getContent().getMappings().size());
        Mapping folderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.Ignore, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, folderMapping.getActionTaken());
    }

    @Test
    public void ignoreAndFailOnExistingTest() throws Exception {
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        folderMO.setName("New Target Folder");
        RestResponse response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));
        assertOkCreatedResponse(response);
        Item<FolderMO> folderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderMO.setId(folderCreated.getId());
        folderCreated.setContent(folderMO);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Ignore);
        mapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnExisting", true).map());
        mapping.setTargetId(folderCreated.getId());
        mapping.setType(folderCreated.getType());

        bundle.setMappings(Arrays.asList(mapping));
        bundle.setReferences(Collections.<Item>emptyList());

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 1 mapping after the import", 1, mappings.getContent().getMappings().size());
        Mapping folderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.Ignore, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, folderMapping.getActionTaken());

        response = getTargetEnvironment().processRequest("folders/"+folderCreated.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void importCreateNewWithMapByName() throws Exception{
        // create role with specific id
        RbacRoleMO roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setId("9ac49df76998c179cd4b48512b550000");
        roleMO.setName("MyAlwaysCreateNewRole");
        roleMO.setUserCreated(true);
        RestResponse response =  getTargetEnvironment().processRequest("roles/"+roleMO.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                        objectToString(roleMO));
        assertOkCreatedResponse(response);

        try {

            // create bundle
            Bundle bundle = ManagedObjectFactory.createBundle();
            bundle.setReferences(CollectionUtils.<Item>list(new ItemBuilder<RbacRoleMO>(roleMO.getName(), roleMO.getId(), EntityType.RBAC_ROLE.toString()).setContent(roleMO).build()));
            Mapping mapping = ManagedObjectFactory.createMapping();
            mapping.setAction(Mapping.Action.AlwaysCreateNew);
            mapping.setSrcId(roleMO.getId());
            mapping.setType(EntityType.RBAC_ROLE.toString());
            mapping.setProperties(CollectionUtils.<String, Object>mapBuilder().put("MapBy", "name").put("MapTo", roleMO.getName() + " Updated").map());
            bundle.setMappings(CollectionUtils.<Mapping>list(mapping));

            // import bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundle));
            assertOkResponse(response);

            // check created with different id
            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals("There should be 1 mapping after the import", 1, mappings.getContent().getMappings().size());
            Mapping roleMappping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.RBAC_ROLE.toString(), roleMappping.getType());
            Assert.assertEquals(Mapping.Action.AlwaysCreateNew, roleMappping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, roleMappping.getActionTaken());
            Assert.assertNotSame(roleMappping.getTargetId(), roleMappping.getSrcId());

            response =  getTargetEnvironment().processRequest("roles/"+roleMappping.getTargetId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);

        }finally{
            // delete role
            response =  getTargetEnvironment().processRequest("roles/"+roleMO.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);
        }
    }
}
