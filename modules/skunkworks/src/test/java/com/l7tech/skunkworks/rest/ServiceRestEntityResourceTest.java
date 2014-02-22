package com.l7tech.skunkworks.rest;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ServiceRestEntityResourceTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(ServiceRestEntityResourceTest.class.getName());

    private PolicyVersionManager policyVersionManager;
    private static final PublishedService service = new PublishedService();
    private static ServiceManager serviceManager;
    private Folder rootFolder;
    private static final String POLICY = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>";

    private static final String comment = "MyComment1";
    private static final String basePath = "services/";

    @Before
    public void before() throws Exception {
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);
        serviceManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceManager", ServiceManager.class);

        FolderManager folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();

        service.setName("Service1");
        service.setRoutingUri("/test");
        service.setPolicy(new Policy(PolicyType.INCLUDE_FRAGMENT, "Service1 Policy", POLICY, false));
        service.setFolder(rootFolder);
        service.setSoap(false);
        service.getPolicy().setGuid(UUID.randomUUID().toString());
        serviceManager.save(service);
        policyVersionManager.checkpointPolicy(service.getPolicy(), true, true);
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
        httpMapping.setVerbs(CollectionUtils.list("GET","POST"));
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

        response = processRequest(basePath + serviceGoid.toString() + "/versions/1", HttpMethod.GET, null,"");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        PolicyVersionMO version = (PolicyVersionMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        assertEquals("Comment:", comment, version.getComment());
    }

    @Test
    public void createEntityWithIdAndCommentTest() throws Exception {
        Goid id = new Goid(-123,123L);
        ServiceMO serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(id.toString());
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setName("Create Service Name");
        serviceDetail.setFolderId(rootFolder.getId());
        ServiceDetail.HttpMapping httpMapping = ManagedObjectFactory.createHttpMapping();
        httpMapping.setVerbs(CollectionUtils.list("GET","POST"));
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

        response = processRequest(basePath + serviceGoid.toString() + "/versions/active", HttpMethod.GET, null,"");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        PolicyVersionMO version = (PolicyVersionMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        assertEquals("Comment:", comment, version.getComment());
        assertEquals("Ordinal:", 1, version.getOrdinal());
    }

    @Test
    public void getInvalidPolicyVersionTest() throws Exception {
        RestResponse  response = processRequest(basePath + new Goid(234,234).toString() + "/versions/active", HttpMethod.GET, null,"");
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse error = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("Error type:", "ResourceNotFound", error.getType());
    }

    @Test
    public void updateEntityWithCommentTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(basePath + service.getId(), HttpMethod.GET, null, "");
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


    protected String writeMOToString(ManagedObject mo) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ManagedObjectFactory.write(mo, bout);
        return bout.toString();
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
}
