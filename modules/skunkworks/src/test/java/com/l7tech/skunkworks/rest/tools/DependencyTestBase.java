package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.RunOnNightly;
import com.l7tech.util.Functions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.http.entity.ContentType;
import org.junit.*;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

@Ignore
public abstract class DependencyTestBase {
    private static final Logger logger = Logger.getLogger(DependencyTestBase.class.getName());

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static DatabaseBasedRestManagementEnvironment environment;

    public static DatabaseBasedRestManagementEnvironment getEnvironment() {
        return environment;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (RunOnNightly.isNightly()) {
            environment = new DatabaseBasedRestManagementEnvironment();
        }
    }
    @Before
    public void before() throws Exception {

    }

    @AfterClass
    public static void afterClass() throws Exception {
    }

    @After
    public void after() throws Exception {
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

    protected void TestPolicyDependency(String policyXml, Functions.UnaryVoid<DependencyAnalysisMO> verify) throws Exception{
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

        RestResponse response = getEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //  get dependency
        RestResponse depResponse = getEnvironment().processRequest("policies/" + policyItem.getId() + "/dependencies", HttpMethod.GET, null, "");
        assertOkResponse(depResponse);

        // cleanup
        response = getEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

        //  verify
        final StreamSource source = new StreamSource(new StringReader(depResponse.getBody()));
        Item<DependencyAnalysisMO> item = MarshallingUtils.unmarshal(Item.class, source);
        assertNotNull(item.getContent().getDependencies());
        verify.call(item.getContent());

    }

    protected DependencyMO getDependency(List<DependencyMO> dependencies, final EntityType type){
        return (DependencyMO)CollectionUtils.find(dependencies, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return ((DependencyMO)o).getDependentObject().getType().equals(type.toString());
            }
        });
    }
}
