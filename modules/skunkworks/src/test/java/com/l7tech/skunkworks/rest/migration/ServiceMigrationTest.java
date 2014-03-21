package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
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
public class ServiceMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(ServiceMigrationTest.class.getName());

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
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("folders/" + folderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getTargetEnvironment().processRequest("services/" , HttpMethod.GET, null, "");
        response = getTargetEnvironment().processRequest("serviceAliases/" , HttpMethod.GET, null, "");
        response = getTargetEnvironment().processRequest("folders/" , HttpMethod.GET, null, "");
    }

    @Test
    public void testImportNew() throws Exception {
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

        //verify service
        response = getTargetEnvironment().processRequest("services/"+serviceItem.getId(), HttpMethod.GET, null,"");
        Item<ServiceMO> targetService = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(2, targetService.getContent().getResourceSets().size());
        Assert.assertEquals("wsdl", targetService.getContent().getResourceSets().get(1).getTag());
        mappingsToClean = mappings;

        validate(mappings);
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
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("services/" + serviceCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("folders/" + folderItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

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
            Assert.assertEquals(serviceItem.getContent().getServiceDetail().getName(), updatedService.getContent().getServiceDetail().getName());
            Assert.assertEquals(2, updatedService.getContent().getResourceSets().size());
            Assert.assertEquals("http://localhost:8080/test.wsdl", updatedService.getContent().getResourceSets().get(1).getRootUrl());

            validate(mappings);

        }finally{
            response = getTargetEnvironment().processRequest("services/" + serviceCreated.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);
        }
    }
}
