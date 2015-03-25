package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
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
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ServiceAliasMigration extends MigrationTestBase {
    private static final Logger logger = Logger.getLogger(ServiceAliasMigration.class.getName());

    private Item<ServiceMO> serviceItem;
    private Item<FolderMO> folderItem;
    private Item<ServiceAliasMO> serviceAliasItem;
    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {
        //create service
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("Source Service");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        ServiceDetail.HttpMapping serviceMapping = ManagedObjectFactory.createHttpMapping();
        serviceMapping.setUrlPattern("/srcService");
        serviceMapping.setVerbs(Arrays.asList("POST"));
        ServiceDetail.SoapMapping soapMapping = ManagedObjectFactory.createSoapMapping();
        soapMapping.setLax(false);
        serviceDetail.setServiceMappings(Arrays.asList(serviceMapping,soapMapping));
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", true)
                .put("soapVersion", "1.2")
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent(assXml );
        ResourceSet wsdlResourceSet = ManagedObjectFactory.createResourceSet();
        wsdlResourceSet.setTag("wsdl");
        wsdlResourceSet.setRootUrl("http://localhost:8080/test.wsdl");
        Resource wsdlResource = ManagedObjectFactory.createResource();
        wsdlResourceSet.setResources(Arrays.asList(wsdlResource));
        wsdlResource.setType("wsdl");
        wsdlResource.setSourceUrl("http://localhost:8080/test.wsdl");
        wsdlResource.setContent("<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>" );
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet,wsdlResourceSet));

        RestResponse response = getSourceEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);

        //create folder item
        FolderMO parentFolderMO = ManagedObjectFactory.createFolder();
        parentFolderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        parentFolderMO.setName("Source folder");
        response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(parentFolderMO)));

        assertOkCreatedResponse(response);

        folderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderItem.setContent(parentFolderMO);

        // create service alias
        ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setFolderId(folderItem.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, serviceItem.getId()));
        response = getSourceEnvironment().processRequest("serviceAliases", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceAliasMO)));
        assertOkCreatedResponse(response);
        serviceAliasItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceAliasItem.setContent(serviceAliasMO);
    }

    @After
    public void after() throws Exception {

        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response;

        response = getSourceEnvironment().processRequest("serviceAliases/" + serviceAliasItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("folders/" + folderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A service, folder, and service alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. 2 folders, a service, and a service alias", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping serviceMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        Mapping serviceAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceAliasMapping.getActionTaken());
        Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());
        Assert.assertEquals(serviceAliasMapping.getSrcId(), serviceAliasMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportExisting() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A service, folder, and service alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. 2 folders, a service, and a service alias", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping serviceMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        Mapping serviceAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceAliasMapping.getActionTaken());
        Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());
        Assert.assertEquals(serviceAliasMapping.getSrcId(), serviceAliasMapping.getTargetId());

        validate(mappings);

        //import the bundle again
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        serviceMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        serviceAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, serviceAliasMapping.getActionTaken());
        Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());
        Assert.assertEquals(serviceAliasMapping.getSrcId(), serviceAliasMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportExistingUpdate() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), "defaultAction=NewOrUpdate", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A service, folder, and service alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. 2 folders, a service, and a service alias", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping serviceMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        Mapping serviceAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceAliasMapping.getActionTaken());
        Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());
        Assert.assertEquals(serviceAliasMapping.getSrcId(), serviceAliasMapping.getTargetId());

        validate(mappings);

        //import the bundle again
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        serviceMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        serviceAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, serviceAliasMapping.getActionTaken());
        Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());
        Assert.assertEquals(serviceAliasMapping.getSrcId(), serviceAliasMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportExistingUpdateAliasConflict() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), "defaultAction=NewOrUpdate", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A service, folder, and service alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. 2 folders, a service, and a service alias", 4, bundleItem.getContent().getMappings().size());

        //don't migrate the alias
        getMapping(bundleItem.getContent().getMappings(), serviceAliasItem.getId()).setAction(Mapping.Action.Ignore);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

        Mapping serviceMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        Mapping serviceAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.Ignore, serviceAliasMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, serviceAliasMapping.getActionTaken());
        Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());

        validate(mappings);

        // create an alias for the service
        ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setFolderId(folderItem.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, serviceItem.getId()));
        response = getTargetEnvironment().processRequest("serviceAliases", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceAliasMO)));
        assertOkCreatedResponse(response);
        Item<ServiceAliasMO> serviceAliasTargetItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceAliasMO.setId(serviceAliasTargetItem.getId());
        serviceAliasTargetItem.setContent(serviceAliasMO);

        try {
            //don't ignore the alias
            getMapping(bundleItem.getContent().getMappings(), serviceAliasItem.getId()).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle again
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertConflictResponse(response);

            mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            folderMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, folderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, folderMapping.getActionTaken());
            Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
            Assert.assertEquals(folderMapping.getSrcId(), folderMapping.getTargetId());

            serviceMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            serviceAliasMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceAliasMapping.getAction());
            Assert.assertEquals(Mapping.ErrorType.UniqueKeyConflict, serviceAliasMapping.getErrorType());
            Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("serviceAliases/" + serviceAliasTargetItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testImportNewMapAliasToBeInFolderWithAliasForSameService() throws Exception {
        Item<FolderMO> folderItem2 = null;
        Item<ServiceAliasMO> serviceAliasItem2 = null;
        RestResponse response;
        try {
            //create folder item
            FolderMO parentFolderMO = ManagedObjectFactory.createFolder();
            parentFolderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
            parentFolderMO.setName("Source folder 2");
            response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(parentFolderMO)));

            assertOkCreatedResponse(response);

            folderItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            folderItem2.setContent(parentFolderMO);

            // create service alias
            ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
            serviceAliasMO.setFolderId(folderItem2.getId());
            serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, serviceItem.getId()));
            response = getSourceEnvironment().processRequest("serviceAliases", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                    XmlUtil.nodeToString(ManagedObjectFactory.write(serviceAliasMO)));
            assertOkCreatedResponse(response);
            serviceAliasItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            serviceAliasItem2.setContent(serviceAliasMO);

            response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 5 item. A service, 2 folders, and 2 service aliases", 5, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 6 mappings. 3 folders, a service, and 2 service aliases", 6, bundleItem.getContent().getMappings().size());

            //map the alias folder to the other alias folder containing the alias service
            Mapping aliasFolderMapping = getMapping(bundleItem.getContent().getMappings(), folderItem.getId());
            aliasFolderMapping.setTargetId(folderItem2.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertConflictResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //verify the mappings
            Assert.assertEquals("There should be 6 mappings after the import", 6, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping folderMapping = getMapping(mappings.getContent().getMappings(), folderItem.getId());
            Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
            //either created new or used existing. it depends on the order that the mapping is exported, it could so wither way.
            Assert.assertTrue(Mapping.ActionTaken.UsedExisting.equals(folderMapping.getActionTaken()) || Mapping.ActionTaken.CreatedNew.equals(folderMapping.getActionTaken()));
            Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
            Assert.assertEquals(folderItem2.getId(), folderMapping.getTargetId());

            Mapping folder2Mapping = getMapping(mappings.getContent().getMappings(), folderItem2.getId());
            Assert.assertEquals(EntityType.FOLDER.toString(), folder2Mapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, folder2Mapping.getAction());
            //either created new or used existing. it depends on the order that the mapping is exported, it could so wither way.
            Assert.assertTrue(Mapping.ActionTaken.UsedExisting.equals(folder2Mapping.getActionTaken()) || Mapping.ActionTaken.CreatedNew.equals(folder2Mapping.getActionTaken()));
            Assert.assertEquals(folderItem2.getId(), folder2Mapping.getSrcId());
            Assert.assertEquals(folderItem2.getId(), folder2Mapping.getTargetId());

            Mapping serviceMapping = getMapping(mappings.getContent().getMappings(), serviceItem.getId());
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            Mapping serviceAliasMapping = getMapping(mappings.getContent().getMappings(), serviceAliasItem.getId());
            Mapping serviceAlias2Mapping = getMapping(mappings.getContent().getMappings(), serviceAliasItem2.getId());

            Assert.assertTrue("at least one of the service aliases need to have errored because it was attempted to be created in a folder with the other (the second one should error)", serviceAliasMapping.getErrorType() != null || serviceAlias2Mapping.getErrorType() != null);

        } finally {
            mappingsToClean = null;

            if(serviceAliasItem2 != null) {
                response = getSourceEnvironment().processRequest("serviceAliases/" + serviceAliasItem2.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
            }
            if(folderItem2 != null) {
                response = getSourceEnvironment().processRequest("folders/" + folderItem2.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
            }
        }
    }

    @Test
    public void testImportNewMapAliasToBeInFolderWithService() throws Exception {
        mappingsToClean = null;

        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A service, folder, and service alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. 2 folders, a service, and a service alias", 4, bundleItem.getContent().getMappings().size());


        //map the alias folder to the root folder containing the alias service
        Mapping aliasFolderMapping = getMapping(bundleItem.getContent().getMappings(), folderItem.getId());
        aliasFolderMapping.setTargetId(Folder.ROOT_FOLDER_ID.toString());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertConflictResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping folderMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.FOLDER.toString(), folderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, folderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, folderMapping.getActionTaken());
        Assert.assertEquals(folderItem.getId(), folderMapping.getSrcId());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), folderMapping.getTargetId());

        Mapping serviceMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        Mapping serviceAliasMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceAliasMapping.getAction());
        Assert.assertEquals(Mapping.ErrorType.UniqueKeyConflict, serviceAliasMapping.getErrorType());
        Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());

    }
}
