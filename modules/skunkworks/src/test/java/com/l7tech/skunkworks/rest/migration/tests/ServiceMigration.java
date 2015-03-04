package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
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
public class ServiceMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(ServiceMigration.class.getName());

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

        // create policy alias
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
        RestResponse response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A service", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 2 mappings. A folder, a service", 2, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping serviceMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        //verify service
        response = getTargetEnvironment().processRequest("services/"+serviceItem.getId(), HttpMethod.GET, null,"");
        Item<ServiceMO> targetService = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(2, targetService.getContent().getResourceSets().size());
        Assert.assertEquals("wsdl", targetService.getContent().getResourceSets().get(1).getTag());
        mappingsToClean = mappings;

        validate(mappings);
    }

    @Test
    public void testImportNewActivateWithComment() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A service", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 2 mappings. A folder, a service", 2, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle?activate=true&versionComment=Comment", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());

        validate(mappings);

        // verify that new policy is activated
        response = getTargetEnvironment().processRequest("services/"+ serviceItem.getId() + "/versions", "", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<PolicyVersionMO> policyVersionList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(1,policyVersionList.getContent().size());
        PolicyVersionMO version = policyVersionList.getContent().get(0).getContent();
        Assert.assertEquals(true,version.isActive());
        Assert.assertEquals("Comment",version.getComment());
    }

    @Test
    public void testImportNewDefaultSettings() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A service", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 2 mappings. A folder, a service", 2, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());

        validate(mappings);

        // verify that new policy is activated
        response = getTargetEnvironment().processRequest("services/"+ serviceItem.getId() + "/versions", "", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<PolicyVersionMO> policyVersionList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(1,policyVersionList.getContent().size());
        PolicyVersionMO version = policyVersionList.getContent().get(0).getContent();
        Assert.assertEquals(true,version.isActive());
    }

    @Test
    public void testImportNewFolder() throws Exception {
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        folderMO.setName("New Target folder");
        RestResponse response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<FolderMO> folderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderMO.setId(folderCreated.getId());
        folderCreated.setContent(folderMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 1 item. A service", 1, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 2 mappings. A folder, a service", 2, bundleItem.getContent().getMappings().size());

            // update mapping
            bundleItem.getContent().getMappings().get(0).setTargetId(folderCreated.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(folderCreated.getId(), rootFolderMapping.getTargetId());

            Mapping serviceMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            //verify service
            response = getTargetEnvironment().processRequest("services/"+serviceItem.getId(), HttpMethod.GET, null,"");
            Item<ServiceMO> targetService = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(2, targetService.getContent().getResourceSets().size());
            Assert.assertEquals("wsdl", targetService.getContent().getResourceSets().get(1).getTag());
            Assert.assertEquals(folderCreated.getId(), targetService.getContent().getServiceDetail().getFolderId());
            mappingsToClean = mappings;

            validate(mappings);
        }finally{

            response = getTargetEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("folders/"+folderCreated.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testImportServiceAliasNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A service, a folder, a service alias", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A service, 2 folders, a service alias", 4, bundleItem.getContent().getMappings().size());

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
    public void testImportNewServiceWithServiceDocuments() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A service", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 2 mappings. A policy, a service", 2, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping serviceMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testMappedService() throws Exception{
        ServiceMO serviceMO = ManagedObjectFactory.read(ManagedObjectFactory.write(serviceItem.getContent()),ServiceMO.class);
        serviceMO.setId(null);
        serviceMO.getServiceDetail().setName("Target Service");
        RestResponse response = getTargetEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<ServiceMO> serviceCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceMO.setId(serviceCreated.getId());
        serviceCreated.setContent(serviceMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A service, a folder, a service alias", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A service, 2 folders, a service alias", 4, bundleItem.getContent().getMappings().size());

            // map the service
            bundleItem.getContent().getMappings().get(2).setTargetId(serviceCreated.getId());
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
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceCreated.getId(), serviceMapping.getTargetId());

            Mapping serviceAliasMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceAliasMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceAliasMapping.getActionTaken());
            Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());
            Assert.assertEquals(serviceAliasMapping.getSrcId(), serviceAliasMapping.getTargetId());

            // validate service alias reference
            response = getTargetEnvironment().processRequest("serviceAliases/" + serviceAliasItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<ServiceAliasMO> newServiceAlias = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(serviceCreated.getId(), newServiceAlias.getContent().getServiceReference().getId());


            validate(mappings);
        }finally{
            response = getTargetEnvironment().processRequest("serviceAliases/" + serviceAliasItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("services/" + serviceCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("folders/" + folderItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testUpdateService() throws Exception{
        ServiceMO serviceMO = ManagedObjectFactory.read(ManagedObjectFactory.write(serviceItem.getContent()),ServiceMO.class);
        serviceMO.setId(null);
        serviceMO.getServiceDetail().setName("Target Service");
        serviceMO.getServiceDetail().setId(null);
        ResourceSet wsdlResourceSet = ManagedObjectFactory.createResourceSet();
        wsdlResourceSet.setTag("wsdl");
        wsdlResourceSet.setRootUrl("http://localhost:8080/targetTest.wsdl");
        Resource wsdlResource = ManagedObjectFactory.createResource();
        wsdlResourceSet.setResources(Arrays.asList(wsdlResource));
        wsdlResource.setType("wsdl");
        wsdlResource.setSourceUrl("http://localhost:8080/targetTest.wsdl");
        wsdlResource.setContent("<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>" );
        serviceMO.setResourceSets(Arrays.asList(serviceMO.getResourceSets().get(0),wsdlResourceSet));
        logger.info( XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));
        RestResponse response = getTargetEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));
        assertOkCreatedResponse(response);
        Item<ServiceMO> serviceCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceMO.setId(serviceCreated.getId());
        serviceCreated.setContent(serviceMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 1 item. A service", 1, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 2 mappings. A policy, a service", 2, bundleItem.getContent().getMappings().size());

            // map and update service
            bundleItem.getContent().getMappings().get(1).setTargetId(serviceCreated.getId());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(1).addProperty("FailOnNew",true);
            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping serviceMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceCreated.getId(), serviceMapping.getTargetId());

            // validate updated service
            response = getTargetEnvironment().processRequest("services/" + serviceCreated.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<ServiceMO> updatedService = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(serviceMO.getServiceDetail().getName(), updatedService.getContent().getServiceDetail().getName());
            Assert.assertEquals(2, updatedService.getContent().getResourceSets().size());
            Assert.assertEquals("http://localhost:8080/test.wsdl", updatedService.getContent().getResourceSets().get(1).getRootUrl());

            validate(mappings);

        }finally{
            response = getTargetEnvironment().processRequest("services/" + serviceCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @BugId("SSG-8716")
    @Test
    public void testMultiplePossibleMappingsService() throws Exception{
        ServiceMO serviceMO = ManagedObjectFactory.read(ManagedObjectFactory.write(serviceItem.getContent()),ServiceMO.class);
        serviceMO.setId(null);
        serviceMO.getServiceDetail().setName("Target Service");
        RestResponse response = getTargetEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<ServiceMO> serviceCreated1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceMO.setId(serviceCreated1.getId());
        serviceCreated1.setContent(serviceMO);

        serviceMO = ManagedObjectFactory.read(ManagedObjectFactory.write(serviceItem.getContent()),ServiceMO.class);
        serviceMO.setId(null);
        serviceMO.getServiceDetail().setName("Target Service");
        response = getTargetEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<ServiceMO> serviceCreated2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceMO.setId(serviceCreated2.getId());
        serviceCreated2.setContent(serviceMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A service, a folder, a service alias", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A service, 2 folders, a service alias", 4, bundleItem.getContent().getMappings().size());

            // map the service
            bundleItem.getContent().getMappings().get(2).addProperty("MapBy", "name");
            bundleItem.getContent().getMappings().get(2).addProperty("MapTo", "Target Service");
            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertConflictResponse(response);

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
            Assert.assertEquals(Mapping.ErrorType.ImproperMapping, serviceMapping.getErrorType());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());

            Mapping serviceAliasMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceAliasMapping.getAction());
            Assert.assertEquals(Mapping.ErrorType.InvalidResource, serviceAliasMapping.getErrorType());
            Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());
        }finally{
            mappingsToClean = null;
        }
    }

    /**
     * Source:
     * - Root
     *   - Service A
     *   - Folder A
     *     - ServiceAlias A (for service A)
     *
     * Target:
     * - Root
     *   - ServiceAlias B (for Service B)
     *   - Folder B
     *     - Service B
     *
     * Map the following:
     * Service A -> Service B
     * ServiceAlias A -> ServiceAlias B
     *
     * Expected Result:
     * - Root
     *   - ServiceAlias A (for Service A)
     *   - Folder B
     *     - Service A
     *   - Folder A
     *
     */
    @BugId("SSG-8792")
    @Test
    public void testUpdateServiceInDifferentFolder() throws Exception{
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setName("MyTargetServiceFolder");
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        RestResponse response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));
        assertOkCreatedResponse(response);
        Item<FolderMO> folderCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folderMO.setId(folderCreated.getId());
        folderCreated.setContent(folderMO);

        ServiceMO serviceMO = ManagedObjectFactory.read(ManagedObjectFactory.write(serviceItem.getContent()),ServiceMO.class);
        serviceMO.setId(null);
        serviceMO.getServiceDetail().setName("Target Service");
        serviceMO.getServiceDetail().setId(null);
        serviceMO.getServiceDetail().setFolderId(folderCreated.getId());
        ResourceSet wsdlResourceSet = ManagedObjectFactory.createResourceSet();
        wsdlResourceSet.setTag("wsdl");
        wsdlResourceSet.setRootUrl("http://localhost:8080/targetTest.wsdl");
        Resource wsdlResource = ManagedObjectFactory.createResource();
        wsdlResourceSet.setResources(Arrays.asList(wsdlResource));
        wsdlResource.setType("wsdl");
        wsdlResource.setSourceUrl("http://localhost:8080/targetTest.wsdl");
        wsdlResource.setContent("<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>" );
        serviceMO.setResourceSets(Arrays.asList(serviceMO.getResourceSets().get(0),wsdlResourceSet));
        logger.info( XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));
        response = getTargetEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));
        assertOkCreatedResponse(response);
        Item<ServiceMO> serviceCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceMO.setId(serviceCreated.getId());
        serviceCreated.setContent(serviceMO);

        // create policy alias
        ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, serviceCreated.getId()));
        response = getTargetEnvironment().processRequest("serviceAliases", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceAliasMO)));
        assertOkCreatedResponse(response);
        Item<ServiceAliasMO> serviceAliasCreated = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceAliasMO.setId(serviceAliasCreated.getId());
        serviceAliasCreated.setContent(serviceAliasMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/folder/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A service, folder and service alias", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. 2 folder, a service, and service alias", 4, bundleItem.getContent().getMappings().size());

            // map and update service
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), serviceItem.getId()).setTargetId(serviceCreated.getId());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), serviceItem.getId()).setAction(Mapping.Action.NewOrUpdate);
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), serviceItem.getId()).addProperty("FailOnNew", true);

            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), serviceAliasItem.getId()).setTargetId(serviceAliasCreated.getId());
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), serviceAliasItem.getId()).setAction(Mapping.Action.NewOrUpdate);
            MigrationTestBase.getMapping(bundleItem.getContent().getMappings(), serviceAliasItem.getId()).addProperty("FailOnNew", true);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
            Mapping rootFolderMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), Folder.ROOT_FOLDER_ID.toString());
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping serviceMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), serviceItem.getId());
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceCreated.getId(), serviceMapping.getTargetId());

            Mapping serviceAliasMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), serviceAliasItem.getId());
            Assert.assertEquals(EntityType.SERVICE_ALIAS.toString(), serviceAliasMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, serviceAliasMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, serviceAliasMapping.getActionTaken());
            Assert.assertEquals(serviceAliasItem.getId(), serviceAliasMapping.getSrcId());
            Assert.assertEquals(serviceAliasCreated.getId(), serviceAliasMapping.getTargetId());

            // validate updated service
            response = getTargetEnvironment().processRequest("services/" + serviceCreated.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<ServiceMO> updatedService = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(serviceMO.getServiceDetail().getName(), updatedService.getContent().getServiceDetail().getName());
            Assert.assertEquals("The service folder was updated but it shouldn't have been.", folderCreated.getId(), updatedService.getContent().getServiceDetail().getFolderId());
            Assert.assertEquals(2, updatedService.getContent().getResourceSets().size());
            Assert.assertEquals("http://localhost:8080/test.wsdl", updatedService.getContent().getResourceSets().get(1).getRootUrl());

            validate(mappings);

            //get the created folder dependencies. It should be empty
            response = getTargetEnvironment().processRequest("folders/" + folderItem.getId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(0, dependencies.getContent().getDependencies().size());

        }finally{
            response = getTargetEnvironment().processRequest("serviceAliases/" + serviceAliasCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("services/" + serviceCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("folders/" + folderCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

        }
    }

    @Test
    public void deleteMappingTest() throws Exception {
        ServiceMO serviceMO1 = ManagedObjectFactory.read(ManagedObjectFactory.write(serviceItem.getContent()),ServiceMO.class);
        serviceMO1.setId(null);
        serviceMO1.getServiceDetail().setName("Target Service");
        RestResponse response = getTargetEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO1)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<ServiceMO> serviceCreated1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceMO1.setId(serviceCreated1.getId());
        serviceCreated1.setContent(serviceMO1);

        ServiceMO serviceMO2 = ManagedObjectFactory.read(ManagedObjectFactory.write(serviceItem.getContent()),ServiceMO.class);
        serviceMO2.setId(null);
        serviceMO2.getServiceDetail().setName("Target Service By Name");
        response = getTargetEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO2)));
        logger.log(Level.INFO, response.toString());
        assertOkCreatedResponse(response);
        Item<ServiceMO> serviceCreated2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceMO2.setId(serviceCreated2.getId());
        serviceCreated2.setContent(serviceMO2);

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setTargetId(serviceCreated1.getId());
        mapping.setType(serviceCreated1.getType());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
        mappingNotExisting.setType(serviceCreated1.getType());

        Mapping mappingByName = ManagedObjectFactory.createMapping();
        mappingByName.setAction(Mapping.Action.Delete);
        mappingByName.setProperties(CollectionUtils.MapBuilder.<String,Object>builder()
                .put("MapBy", "name")
                .put("MapTo", serviceCreated2.getName()).map());
        mappingByName.setType(serviceCreated2.getType());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting, mappingByName));

        //import the bundle
        logger.log(Level.INFO, objectToString(bundle));
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mapping after the import", 3, mappings.getContent().getMappings().size());
        Mapping serviceMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceCreated1.getId(), serviceMapping.getTargetId());

        Mapping serviceMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, serviceMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, serviceMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, serviceMappingNotExisting.getTargetId());

        Mapping mappingByNameResult = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.SERVICE.toString(), mappingByNameResult.getType());
        Assert.assertEquals(Mapping.Action.Delete, mappingByNameResult.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, mappingByNameResult.getActionTaken());
        Assert.assertEquals(serviceCreated2.getId(), mappingByNameResult.getTargetId());

        response = getTargetEnvironment().processRequest("services/"+serviceCreated1.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);

        response = getTargetEnvironment().processRequest("services/"+serviceCreated2.getId(), HttpMethod.GET, null, "");
        assertNotFoundResponse(response);

        //check that all auto created roles where deleted
        response = getTargetEnvironment().processRequest("roles", HttpMethod.GET, null, "");
        assertOkResponse(response);

        ItemsList<RbacRoleMO> roles = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));

        for(Item<RbacRoleMO> role : roles.getContent()) {
            Assert.assertNotSame("Found the auto created role for the deleted entity: " + objectToString(role), serviceCreated1.getId(), role.getContent().getEntityID());
            Assert.assertNotSame("Found the auto created role for the deleted entity: " + objectToString(role), serviceCreated2.getId(), role.getContent().getEntityID());
        }
    }
}
