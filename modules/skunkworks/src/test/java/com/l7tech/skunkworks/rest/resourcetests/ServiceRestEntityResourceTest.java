package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.xml.soap.SoapVersion;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ServiceRestEntityResourceTest extends RestEntityTests<PublishedService, ServiceMO> {
    private static final Logger logger = Logger.getLogger(ServiceRestEntityResourceTest.class.getName());

    private PolicyVersionManager policyVersionManager;
    private List<PublishedService> publishedServices = new ArrayList<>();
    private ServiceManager serviceManager;
    private Folder rootFolder;
    private static final String POLICY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "<wsp:All wsp:Usage=\"Required\">\n" +
            "<L7p:AuditAssertion/>\n" +
            "</wsp:All>\n" +
            "</wsp:Policy>\n";
    private static final String WSDL_XML = "<definitions name=\"HelloService\"\n" +
            "   targetNamespace=\"http://www.examples.com/wsdl/HelloService.wsdl\"\n" +
            "   xmlns=\"http://schemas.xmlsoap.org/wsdl/\"\n" +
            "   xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n" +
            "   xmlns:tns=\"http://www.examples.com/wsdl/HelloService.wsdl\"\n" +
            "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
            " \n" +
            "   <message name=\"SayHelloRequest\">\n" +
            "      <part name=\"firstName\" type=\"xsd:string\"/>\n" +
            "   </message>\n" +
            "   <message name=\"SayHelloResponse\">\n" +
            "      <part name=\"greeting\" type=\"xsd:string\"/>\n" +
            "   </message>\n" +
            "\n" +
            "   <portType name=\"Hello_PortType\">\n" +
            "      <operation name=\"sayHello\">\n" +
            "         <input message=\"tns:SayHelloRequest\"/>\n" +
            "         <output message=\"tns:SayHelloResponse\"/>\n" +
            "      </operation>\n" +
            "   </portType>\n" +
            "\n" +
            "   <binding name=\"Hello_Binding\" type=\"tns:Hello_PortType\">\n" +
            "   <soap:binding style=\"rpc\"\n" +
            "      transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "   <operation name=\"sayHello\">\n" +
            "      <soap:operation soapAction=\"sayHello\"/>\n" +
            "      <input>\n" +
            "         <soap:body\n" +
            "            encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"\n" +
            "            namespace=\"urn:examples:helloservice\"\n" +
            "            use=\"encoded\"/>\n" +
            "      </input>\n" +
            "      <output>\n" +
            "         <soap:body\n" +
            "            encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"\n" +
            "            namespace=\"urn:examples:helloservice\"\n" +
            "            use=\"encoded\"/>\n" +
            "      </output>\n" +
            "   </operation>\n" +
            "   </binding>\n" +
            "\n" +
            "   <service name=\"Hello_Service\">\n" +
            "      <documentation>WSDL File for HelloService</documentation>\n" +
            "      <port binding=\"tns:Hello_Binding\" name=\"Hello_Port\">\n" +
            "         <soap:address\n" +
            "            location=\"http://www.examples.com/SayHello/\"/>\n" +
            "      </port>\n" +
            "   </service>\n" +
            "</definitions>";

    private static final String comment = "MyComment1";
    private static final String basePath = "services/";

    @Before
    public void before() throws Exception {
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);
        serviceManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceManager", ServiceManager.class);

        FolderManager folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();

        PublishedService service = new PublishedService();
        service.setName("Service1");
        service.setRoutingUri("/test");
        service.getPolicy().setXml(POLICY);
        service.setFolder(rootFolder);
        service.setSoap(false);
        service.setDisabled(false);
        service.getPolicy().setGuid(UUID.randomUUID().toString());
        serviceManager.save(service);
        policyVersionManager.checkpointPolicy(service.getPolicy(), true, true);
        publishedServices.add(service);

        service = new PublishedService();
        service.setName("Service2");
        service.setRoutingUri("/test2");
        service.getPolicy().setXml(POLICY);
        service.setFolder(rootFolder);
        service.setSoap(true);
        service.setDisabled(true);
        service.getPolicy().setGuid(UUID.randomUUID().toString());
        service.putProperty("myProperty", "myPropertyValue");
        serviceManager.save(service);
        policyVersionManager.checkpointPolicy(service.getPolicy(), true, true);
        publishedServices.add(service);

        service = new PublishedService();
        service.setName("Service3");
        service.setRoutingUri("/test3");
        service.getPolicy().setXml(POLICY);
        service.setFolder(rootFolder);
        service.setSoap(true);
        service.setDisabled(false);
        service.setInternal(true);
        service.getPolicy().setGuid(UUID.randomUUID().toString());
        service.setSoapVersion(SoapVersion.SOAP_1_2);
        service.setWsdlUrl("http://wsdlUrl");
        service.setWsdlXml(WSDL_XML);
        service.putProperty("myProperty", "myPropertyValue");
        service.putProperty("myProperty2", "myPropertyValue2");
        serviceManager.save(service);
        policyVersionManager.checkpointPolicy(service.getPolicy(), true, true);
        publishedServices.add(service);
    }

    @After
    public void after() throws FindException, DeleteException {
        ArrayList<ServiceHeader> services = new ArrayList<>(serviceManager.findAllHeaders());
        for (EntityHeader service : services) {
            serviceManager.delete(service.getGoid());
        }

        ArrayList<EntityHeader> versions = new ArrayList<>(policyVersionManager.findAllHeaders());
        for (EntityHeader version : versions) {
            policyVersionManager.delete(version.getGoid());
        }
    }

    @Test
    public void createEntityWithCommentTest() throws Exception {

        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("Create Service Name");
        serviceDetail.setFolderId(rootFolder.getId());
        ServiceDetail.HttpMapping httpMapping = ManagedObjectFactory.createHttpMapping();
        httpMapping.setVerbs(CollectionUtils.list("GET", "POST"));
        ServiceDetail.SoapMapping soapMapping = ManagedObjectFactory.createSoapMapping();
        soapMapping.setLax(false);
        serviceDetail.setServiceMappings(CollectionUtils.list(httpMapping, soapMapping));
        serviceDetail.setProperties(CollectionUtils.<String, Object>mapBuilder().put("soap", false).map());
        serviceMO.setServiceDetail(serviceDetail);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:AuditDetailAssertion>\n" +
                "                <L7p:Detail stringValue=\"Policy Fragment: temp\"/>\n" +
                "            </L7p:AuditDetailAssertion>\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        Document request = ManagedObjectFactory.write(serviceMO);
        RestResponse response = processRequest(basePath + "?versionComment=" + comment, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        Goid serviceGoid = new Goid(getFirstReferencedGoid(response));
        PublishedService createdEntity = serviceManager.findByPrimaryKey(serviceGoid);

        assertEquals("Service name:", serviceMO.getServiceDetail().getName(), createdEntity.getName());
        assertEquals("Service folder:", serviceMO.getServiceDetail().getFolderId(), createdEntity.getFolder().getId());

        response = processRequest(basePath + serviceGoid.toString() + "/versions/1", HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        PolicyVersionMO version = (PolicyVersionMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        assertEquals("Comment:", comment, version.getComment());
    }

    @Test
    public void createEntityWithIdAndCommentTest() throws Exception {
        Goid id = new Goid(-123, 123L);
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(id.toString());
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("Create Service Name");
        serviceDetail.setFolderId(rootFolder.getId());
        ServiceDetail.HttpMapping httpMapping = ManagedObjectFactory.createHttpMapping();
        httpMapping.setVerbs(CollectionUtils.list("GET", "POST"));
        ServiceDetail.SoapMapping soapMapping = ManagedObjectFactory.createSoapMapping();
        soapMapping.setLax(false);
        serviceDetail.setServiceMappings(CollectionUtils.list(httpMapping, soapMapping));
        serviceDetail.setProperties(CollectionUtils.<String, Object>mapBuilder().put("soap", false).map());
        serviceMO.setServiceDetail(serviceDetail);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:AuditDetailAssertion>\n" +
                "                <L7p:Detail stringValue=\"Policy Fragment: temp\"/>\n" +
                "            </L7p:AuditDetailAssertion>\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        Document request = ManagedObjectFactory.write(serviceMO);
        RestResponse response = processRequest(basePath + id.toString() + "?versionComment=" + comment, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        Goid serviceGoid = new Goid(getFirstReferencedGoid(response));
        assertEquals("Service id:", id, serviceGoid);
        PublishedService createdEntity = serviceManager.findByPrimaryKey(serviceGoid);

        assertEquals("Service name:", serviceMO.getServiceDetail().getName(), createdEntity.getName());
        assertEquals("Service folder:", serviceMO.getServiceDetail().getFolderId(), createdEntity.getFolder().getId());

        response = processRequest(basePath + serviceGoid.toString() + "/versions/active", HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        PolicyVersionMO version = (PolicyVersionMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        assertEquals("Comment:", comment, version.getComment());
        assertEquals("Ordinal:", 1, version.getOrdinal());
    }

    @Test
    public void getInvalidPolicyVersionTest() throws Exception {
        RestResponse response = processRequest(basePath + new Goid(234, 234).toString() + "/versions/active", HttpMethod.GET, null, "");
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("Error type:", "ResourceNotFound", error.getType());
    }

    @Test
    public void updateEntityWithCommentTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(basePath + publishedServices.get(0).getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ServiceMO entityGot = (ServiceMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.getServiceDetail().setName("Updated Service Name");
        Resource policyResource = entityGot.getResourceSets().get(0).getResources().get(0);
        policyResource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:AuditDetailAssertion>\n" +
                "                <L7p:Detail stringValue=\"Policy Fragment: temp\"/>\n" +
                "            </L7p:AuditDetailAssertion>\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        RestResponse response = processRequest(basePath + entityGot.getId() + "?active=false&versionComment=" + comment, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        Goid serviceGoid = new Goid(entityGot.getId());
        PublishedService updatedEntity = serviceManager.findByPrimaryKey(serviceGoid);

        assertEquals("Service identifier:", updatedEntity.getId(), entityGot.getId());
        assertEquals("Service name:", updatedEntity.getName(), entityGot.getServiceDetail().getName());
        assertEquals("Service folder:", updatedEntity.getFolder().getId(), entityGot.getServiceDetail().getFolderId());

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(updatedEntity.getPolicy().getGoid(), 2);
        assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());
        assertEquals("Active:", false, version.isActive());

        PolicyVersion oldVersion = policyVersionManager.findPolicyVersionForPolicy(updatedEntity.getPolicy().getGoid(), 1);
        assertNotNull(oldVersion);
        assertEquals("Active:", true, oldVersion.isActive());
    }

    protected String getFirstReferencedGoid(RestResponse response) throws IOException {
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        List<Link> links = item.getLinks();

        for (Link link : links) {
            if ("self".equals(link.getRel())) {
                return link.getUri().substring(link.getUri().lastIndexOf('/') + 1);
            }
        }
        return null;
    }

    @Override
    public List<String> getRetrievableEntityIDs() throws FindException {
        return Functions.map(publishedServices, new Functions.Unary<String, PublishedService>() {
            @Override
            public String call(PublishedService publishedService) {
                return publishedService.getId();
            }
        });
    }

    @Override
    public List<ServiceMO> getCreatableManagedObjects() {
        ArrayList<ServiceMO> serviceMOs = new ArrayList<>();

        ServiceMO serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(getGoid().toString());
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("My New Service");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(rootFolder.getId());
        serviceMO.setServiceDetail(serviceDetail);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(POLICY);
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        serviceMOs.add(serviceMO);

        //create a service without specifying a folder. should use root by default SSG-8808
        serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(getGoid().toString());
        serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("My New Service");
        serviceDetail.setEnabled(false);
        serviceMO.setServiceDetail(serviceDetail);
        policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(POLICY);
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        serviceMOs.add(serviceMO);

        //create a service without specifying a folder. should use root by default SSG-8808
        serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(getGoid().toString());
        serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("My New Service");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(rootFolder.getId());
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String,Object>builder().put("internal", true).map());
        serviceMO.setServiceDetail(serviceDetail);
        policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(POLICY);
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        serviceMOs.add(serviceMO);

        //create a service with properties
        serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(getGoid().toString());
        serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("My New Service");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(rootFolder.getId());
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("createPropertyKey", "createPropertyValue").map());
        serviceMO.setServiceDetail(serviceDetail);
        policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(POLICY);
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        serviceMOs.add(serviceMO);


        return serviceMOs;
    }

    @Override
    public List<ServiceMO> getUpdateableManagedObjects() {
        ArrayList<ServiceMO> serviceMOs = new ArrayList<>();

        PublishedService publishedService = publishedServices.get(0);

        //update name
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(publishedService.getId());
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName(publishedService.getName() + "Updated");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(publishedService.getFolder().getId());
        serviceMO.setServiceDetail(serviceDetail);
        serviceMO.setVersion(publishedService.getVersion());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(publishedService.getPolicy().getXml());
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        serviceMOs.add(serviceMO);

        //update again SSG-8476
        serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(publishedService.getId());
        serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName(publishedService.getName() + "Updated");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(publishedService.getFolder().getId());
        serviceMO.setServiceDetail(serviceDetail);
        serviceMO.setVersion(publishedService.getVersion() + 1);
        policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(publishedService.getPolicy().getXml());
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        serviceMOs.add(serviceMO);

        //update property Value
        serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(publishedService.getId());
        serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName(publishedService.getName() + "Updated");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(publishedService.getFolder().getId());
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("createPropertyKey", "createPropertyValue").map());
        serviceMO.setServiceDetail(serviceDetail);
        serviceMO.setVersion(publishedService.getVersion() + 2);
        policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(publishedService.getPolicy().getXml());
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        serviceMOs.add(serviceMO);

        return serviceMOs;
    }

    @Override
    public Map<ServiceMO, Functions.BinaryVoid<ServiceMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<ServiceMO, Functions.BinaryVoid<ServiceMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //invalid folder id SSG-8455
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("My New Service");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(getGoid().toString());
        serviceMO.setServiceDetail(serviceDetail);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(POLICY);
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        builder.put(serviceMO, new Functions.BinaryVoid<ServiceMO, RestResponse>() {
            @Override
            public void call(ServiceMO serviceMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //long name id SSG-8453
        serviceMO = ManagedObjectFactory.createService();
        serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("My New Service with long name xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(rootFolder.getId());
        serviceMO.setServiceDetail(serviceDetail);
        policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(POLICY);
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        builder.put(serviceMO, new Functions.BinaryVoid<ServiceMO, RestResponse>() {
            @Override
            public void call(ServiceMO serviceMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<ServiceMO, Functions.BinaryVoid<ServiceMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<ServiceMO, Functions.BinaryVoid<ServiceMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        PublishedService publishedService = publishedServices.get(0);

        //invalid version number id SSG-8271
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(publishedService.getId());
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName(publishedService.getName() + "Updated");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(publishedService.getFolder().getId());
        serviceMO.setServiceDetail(serviceDetail);
        serviceMO.setVersion(publishedService.getVersion() + 100);
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(publishedService.getPolicy().getXml());
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        builder.put(serviceMO, new Functions.BinaryVoid<ServiceMO, RestResponse>() {
            @Override
            public void call(ServiceMO serviceMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //move to not existing folder. SSG-8477
        serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(publishedService.getId());
        serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName(publishedService.getName() + "Updated");
        serviceDetail.setEnabled(false);
        serviceDetail.setFolderId(getGoid().toString());
        serviceMO.setServiceDetail(serviceDetail);
        serviceMO.setVersion(publishedService.getVersion());
        policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setTag("policy");
        policyResource.setType("policy");
        policyResource.setContent(publishedService.getPolicy().getXml());
        policyResourceSet.setResources(Arrays.asList(policyResource));
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        builder.put(serviceMO, new Functions.BinaryVoid<ServiceMO, RestResponse>() {
            @Override
            public void call(ServiceMO serviceMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        return builder.map();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(publishedServices, new Functions.Unary<String, PublishedService>() {
            @Override
            public String call(PublishedService publishedService) {
                return publishedService.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "services";
    }

    @Override
    public String getType() {
        return EntityType.SERVICE.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        PublishedService service = serviceManager.findByPrimaryKey(Goid.parseGoid(id));
        return service.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        PublishedService service = serviceManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(service);
    }

    @Override
    public void verifyEntity(String id, ServiceMO managedObject) throws Exception {
        PublishedService entity = serviceManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getServiceDetail().getName());
            Assert.assertEquals(entity.getFolder().getId(), managedObject.getServiceDetail().getFolderId() != null ? managedObject.getServiceDetail().getFolderId() : Folder.ROOT_FOLDER_ID.toString());
            Assert.assertEquals("Policy xml's differ",
                    entity.getPolicy().getXml().trim().replaceAll("\\s+", " "),
                    managedObject.getResourceSets().get(0).getResources().get(0).getContent().trim().replaceAll("\\s+", " "));
            Assert.assertEquals(entity.isDisabled(), !managedObject.getServiceDetail().getEnabled());

            if(entity.isSoap() && entity.getWsdlXml() != null) {
                Assert.assertEquals(2, managedObject.getResourceSets().size());
                Assert.assertEquals(entity.getWsdlUrl(), managedObject.getResourceSets().get(1).getRootUrl());
                Assert.assertEquals(entity.getWsdlXml(), managedObject.getResourceSets().get(1).getResources().get(0).getContent());

            }

            if(managedObject.getServiceDetail().getProperties() != null) {
                Assert.assertEquals(entity.isSoap() ? SoapVersion.UNKNOWN.equals(entity.getSoapVersion()) ? "unspecified" : entity.getSoapVersion().getVersionNumber() : null, managedObject.getServiceDetail().getProperties().get("soapVersion"));
                Assert.assertEquals(entity.isInternal(), managedObject.getServiceDetail().getProperties().get("internal") == null ? false : managedObject.getServiceDetail().getProperties().get("internal"));
                Assert.assertEquals(entity.isSoap(), managedObject.getServiceDetail().getProperties().get("soap") == null ? false : managedObject.getServiceDetail().getProperties().get("soap"));
                Assert.assertEquals(entity.isWssProcessingEnabled(), managedObject.getServiceDetail().getProperties().get("wssProcessingEnabled") == null ? true : managedObject.getServiceDetail().getProperties().get("wssProcessingEnabled"));
                Assert.assertEquals(entity.isTracingEnabled(), managedObject.getServiceDetail().getProperties().get("tracingEnabled") == null ? false : managedObject.getServiceDetail().getProperties().get("tracingEnabled"));

                for(String key : entity.getPropertyNames()){
                    Assert.assertEquals(entity.getProperty(key), managedObject.getServiceDetail().getProperties().get("property." + key));
                }
            }

            if(managedObject.getServiceDetail().getServiceMappings() != null) {
                ServiceDetail.ServiceMapping serviceMapping = managedObject.getServiceDetail().getServiceMappings().get(0);
                Assert.assertNotNull(serviceMapping);
                Assert.assertTrue(serviceMapping instanceof ServiceDetail.HttpMapping);
                Assert.assertEquals(entity.getRoutingUri(), ((ServiceDetail.HttpMapping) serviceMapping).getUrlPattern());
                Assert.assertEquals(entity.getHttpMethodsReadOnly().size(), ((ServiceDetail.HttpMapping) serviceMapping).getVerbs().size());
                for (HttpMethod httpMethod : entity.getHttpMethodsReadOnly()) {
                    Assert.assertTrue(((ServiceDetail.HttpMapping) serviceMapping).getVerbs().contains(httpMethod.name()));
                }

                if (entity.isSoap()) {
                    serviceMapping = managedObject.getServiceDetail().getServiceMappings().get(1);
                    Assert.assertNotNull(serviceMapping);
                    Assert.assertTrue(serviceMapping instanceof ServiceDetail.SoapMapping);
                    Assert.assertEquals(entity.isLaxResolution(), ((ServiceDetail.SoapMapping) serviceMapping).isLax());
                }
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(publishedServices, new Functions.Unary<String, PublishedService>() {
                    @Override
                    public String call(PublishedService publishedService) {
                        return publishedService.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(publishedServices.get(0).getName()), Arrays.asList(publishedServices.get(0).getId()))
                .put("name=" + URLEncoder.encode(publishedServices.get(0).getName()) + "&name=" + URLEncoder.encode(publishedServices.get(1).getName()), Functions.map(publishedServices.subList(0, 2), new Functions.Unary<String, PublishedService>() {
                    @Override
                    public String call(PublishedService publishedService) {
                        return publishedService.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("enabled=false", Arrays.asList(publishedServices.get(1).getId()))
                .put("enabled=true", Arrays.asList(publishedServices.get(0).getId(), publishedServices.get(2).getId()))
                .put("name=" + URLEncoder.encode(publishedServices.get(0).getName()) + "&name=" + URLEncoder.encode(publishedServices.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(publishedServices.get(1).getId(), publishedServices.get(0).getId()))
                .map();
    }
}
