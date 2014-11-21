package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.Functions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.http.entity.ContentType;
import org.junit.*;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

@Ignore
public abstract class DependencyTestBase extends RestEntityTestBase{
    private static final Logger logger = Logger.getLogger(DependencyTestBase.class.getName());

    protected PolicyManager policyManager;
    protected FolderManager folderManager;
    protected Folder rootFolder;
    protected List<Goid> policyGoids = new ArrayList<Goid>();
    protected PolicyVersionManager policyVersionManager;

    @Before
    public void before() throws Exception {
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);

        rootFolder = folderManager.findRootFolder();
    }

    @AfterClass
    public static void afterClass() throws Exception {
    }

    @After
    public void after() throws Exception {
        for(Goid policyGoid : policyGoids){
            if(policyManager.findByPrimaryKey(policyGoid)!=null)
                policyManager.delete(policyGoid);
        }
    }

    protected void assertOkCreatedResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(201, response.getStatus());
        assertNotNull(response.getBody());
    }

    protected void assertOkResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());
    }

    protected void assertOKDeleteResponse(RestResponse response) {
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(204, response.getStatus());
    }

    protected void TestPolicyDependency(String policyXml, Functions.UnaryVoid<Item<DependencyListMO>> verify) throws Exception{
        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Test Policy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(com.l7tech.util.CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent(policyXml);

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyGoids.add(Goid.parseGoid(policyItem.getId()));

        //sleep for a second so the the dependency cache has time to properly build up
        Thread.sleep(1000);

        //  get dependency
        RestResponse depResponse = getDatabaseBasedRestManagementEnvironment().processRequest("policies/" + policyItem.getId() + "/dependencies", HttpMethod.GET, null, "");
        assertOkResponse(depResponse);

        // cleanup
        response = getDatabaseBasedRestManagementEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

        //  verify
        final StreamSource source = new StreamSource(new StringReader(depResponse.getBody()));
        Item<DependencyListMO> item = MarshallingUtils.unmarshal(Item.class, source);
        verify.call(item);

    }

    protected void TestDependency(String resourceURI, String id, Functions.UnaryVoid<Item<DependencyListMO>> verify) throws Exception{

         //  get dependency
        RestResponse depResponse = getDatabaseBasedRestManagementEnvironment().processRequest( resourceURI + id + "/dependencies", HttpMethod.GET, null, "");
        assertOkResponse(depResponse);

        //  verify
        final StreamSource source = new StreamSource(new StringReader(depResponse.getBody()));
        Item<DependencyListMO> item = MarshallingUtils.unmarshal(Item.class, source);
        verify.call(item);
    }

    protected DependencyMO getDependency(DependencyListMO dependencies, final EntityType type){
        return (DependencyMO)CollectionUtils.find(dependencies.getDependencies(), new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return ((DependencyMO)o).getType().equals(type.toString());
            }
        });
    }

    public static DependencyMO getDependency(List<DependencyMO> dependencies, final String id){
        return (DependencyMO)CollectionUtils.find(dependencies, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return ((DependencyMO)o).getId().equals(id);
            }
        });
    }
}
