package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PolicyRestEntityResourceTest extends RestEntityTests<Policy, PolicyMO> {
    private static final Logger logger = Logger.getLogger(PolicyRestEntityResourceTest.class.getName());

    private PolicyManager policyManager;
    private List<Policy> policies = new ArrayList<>();
    private Folder rootFolder;
    private PolicyVersionManager policyVersionManager;

    private static final String comment = "MyComment1";
    private FolderManager folderManager;
    private Folder myFolder;

    @Before
    public void before() throws ObjectModelException {
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);

        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();

        myFolder = new Folder("MyFolder", rootFolder);
        folderManager.save(myFolder);

        //Create the policies
        Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy 1",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <exp:References/>\n" +
                        "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "        <wsp:All wsp:Usage=\"Required\">\n" +
                        "        </wsp:All>\n" +
                        "    </wsp:Policy>\n" +
                        "</exp:Export>\n",
                false
        );
        policy.setFolder(rootFolder);
        policy.setGuid(UUID.randomUUID().toString());
        policy.setSoap(true);
        policy.disable();

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,comment,true);
        policies.add(policy);

        policy = new Policy(PolicyType.INTERNAL, "Policy 2",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <exp:References/>\n" +
                        "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "        <wsp:All wsp:Usage=\"Required\">\n" +
                        "        </wsp:All>\n" +
                        "    </wsp:Policy>\n" +
                        "</exp:Export>\n",
                false
        );
        policy.setFolder(myFolder);
        policy.setSoap(true);
        policy.setGuid(UUID.randomUUID().toString());

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,comment,true);
        policies.add(policy);

        policy = new Policy(PolicyType.GLOBAL_FRAGMENT, "Policy 3",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <exp:References/>\n" +
                        "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "        <wsp:All wsp:Usage=\"Required\">\n" +
                        "        </wsp:All>\n" +
                        "    </wsp:Policy>\n" +
                        "</exp:Export>\n",
                true
        );
        policy.setFolder(rootFolder);
        policy.setSoap(false);
        policy.setGuid(UUID.randomUUID().toString());

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,comment,true);
        policies.add(policy);

        policy = new Policy(PolicyType.PRIVATE_SERVICE, "Policy 4",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <exp:References/>\n" +
                        "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "        <wsp:All wsp:Usage=\"Required\">\n" +
                        "        </wsp:All>\n" +
                        "    </wsp:Policy>\n" +
                        "</exp:Export>\n",
                false
        );
        policy.setFolder(rootFolder);
        policy.setGuid(UUID.randomUUID().toString());

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,comment,true);
        policies.add(policy);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<Policy> all = policyManager.findAll();
        for (Policy policy : all) {
            policyManager.delete(policy.getGoid());
        }
        folderManager.delete(myFolder);
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(Functions.grep(policies, new Functions.Unary<Boolean, Policy>() {
            @Override
            public Boolean call(Policy policy) {
                return PolicyType.INCLUDE_FRAGMENT.equals(policy.getType()) ||
                        PolicyType.INTERNAL.equals(policy.getType()) ||
                        PolicyType.GLOBAL_FRAGMENT.equals(policy.getType());
            }
        }), new Functions.Unary<String, Policy>() {
            @Override
            public String call(Policy policy) {
                return policy.getId();
            }
        });
    }

    @Override
    public List<PolicyMO> getCreatableManagedObjects() {
        List<PolicyMO> policies = new ArrayList<>();

        PolicyMO policy = ManagedObjectFactory.createPolicy();
        policy.setId(getGoid().toString());
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("New Policy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policy.setPolicyDetail(policyDetail);
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        resourceSet.setResources(Arrays.asList(resource));
        policy.setResourceSets(Arrays.asList(resourceSet));
        policies.add(policy);

        return policies;
    }

    @Override
    public List<PolicyMO> getUpdateableManagedObjects() {
        List<PolicyMO> policyMOs = new ArrayList<>();

        Policy policy = this.policies.get(0);
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setId(policy.getId());
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(policy.getName() + "Updated");
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent(policy.getXml());
        resourceSet.setResources(Arrays.asList(resource));
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        policyMOs.add(policyMO);
        return policyMOs;
    }

    @Override
    public Map<PolicyMO, Functions.BinaryVoid<PolicyMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<PolicyMO, Functions.BinaryVoid<PolicyMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        Policy policy = this.policies.get(0);
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(policy.getName());
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent(policy.getXml());
        resourceSet.setResources(Arrays.asList(resource));
        policyMO.setResourceSets(Arrays.asList(resourceSet));

        builder.put(policyMO, new Functions.BinaryVoid<PolicyMO, RestResponse>() {
            @Override
            public void call(PolicyMO policyMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        policyMO = ManagedObjectFactory.createPolicy();
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(policy.getName() + "Updated");
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(null);
        policyMO.setPolicyDetail(policyDetail);
        resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent(policy.getXml());
        resourceSet.setResources(Arrays.asList(resource));
        policyMO.setResourceSets(Arrays.asList(resourceSet));

        builder.put(policyMO, new Functions.BinaryVoid<PolicyMO, RestResponse>() {
            @Override
            public void call(PolicyMO policyMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @BugId("SSG-8254")
    @Test
    public void createPolicyWithBadTypeTest() throws Exception {

        Policy policy = this.policies.get(0);
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(policy.getName() + "Updated");
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent(policy.getXml());
        resourceSet.setResources(Arrays.asList(resource));
        policyMO.setResourceSets(Arrays.asList(resourceSet));

        String policyMOString = writeMOToString(policyMO);
        policyMOString = policyMOString.replace("<l7:PolicyType>Include</l7:PolicyType>", "<l7:PolicyType></l7:PolicyType>");

        RestResponse response = processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Assert.assertEquals(400, response.getStatus());

        policyMOString = writeMOToString(policyMO);
        policyMOString = policyMOString.replace("<l7:PolicyType>Include</l7:PolicyType>", "<l7:PolicyType>BLAHBLAHBLAH</l7:PolicyType>");

        response = processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Assert.assertEquals(400, response.getStatus());
    }

    @Override
    public Map<PolicyMO, Functions.BinaryVoid<PolicyMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<PolicyMO, Functions.BinaryVoid<PolicyMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        Policy policy = this.policies.get(0);
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setId(policy.getId());
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(this.policies.get(1).getName());
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent(policy.getXml());
        resourceSet.setResources(Arrays.asList(resource));
        policyMO.setResourceSets(Arrays.asList(resourceSet));

        builder.put(policyMO, new Functions.BinaryVoid<PolicyMO, RestResponse>() {
            @Override
            public void call(PolicyMO policyMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        builder.put("type=badType", new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        builder.put("soap=notboolean", new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        builder.put("parentFolder.id=notAGoid", new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(Functions.grep(policies, new Functions.Unary<Boolean, Policy>() {
            @Override
            public Boolean call(Policy policy) {
                return PolicyType.INCLUDE_FRAGMENT.equals(policy.getType()) ||
                        PolicyType.INTERNAL.equals(policy.getType()) ||
                        PolicyType.GLOBAL_FRAGMENT.equals(policy.getType());
            }
        }), new Functions.Unary<String, Policy>() {
            @Override
            public String call(Policy policy) {
                return policy.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "policies";
    }

    @Override
    public String getType() {
        return EntityType.POLICY.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        Policy entity = policyManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        Policy entity = policyManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, PolicyMO managedObject) throws FindException {
        Policy entity = policyManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getPolicyDetail().getName());
            Assert.assertEquals(entity.getXml(), managedObject.getResourceSets().get(0).getResources().get(0).getContent());
            switch (entity.getType()) {
                case INTERNAL:
                    Assert.assertEquals(PolicyDetail.PolicyType.INTERNAL, managedObject.getPolicyDetail().getPolicyType());
                    break;
                case GLOBAL_FRAGMENT:
                    Assert.assertEquals(PolicyDetail.PolicyType.GLOBAL, managedObject.getPolicyDetail().getPolicyType());
                    break;
                case INCLUDE_FRAGMENT:
                    Assert.assertEquals(PolicyDetail.PolicyType.INCLUDE, managedObject.getPolicyDetail().getPolicyType());
                    break;
                default:
                    Assert.fail("should not be able to get policies of this type: " + entity.getType());
            }
            Assert.assertEquals(entity.getFolder().getId(), managedObject.getPolicyDetail().getFolderId());
            if (managedObject.getPolicyDetail().getGuid() != null) {
                Assert.assertEquals(entity.getGuid(), managedObject.getPolicyDetail().getGuid());
            }

        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(Functions.grep(policies, new Functions.Unary<Boolean, Policy>() {
                    @Override
                    public Boolean call(Policy policy) {
                        return PolicyType.INCLUDE_FRAGMENT.equals(policy.getType()) ||
                                PolicyType.INTERNAL.equals(policy.getType()) ||
                                PolicyType.GLOBAL_FRAGMENT.equals(policy.getType());
                    }
                }), new Functions.Unary<String, Policy>() {
                    @Override
                    public String call(Policy policy) {
                        return policy.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(policies.get(0).getName()), Arrays.asList(policies.get(0).getId()))
                .put("name=" + URLEncoder.encode(policies.get(0).getName()) + "&name=" + URLEncoder.encode(policies.get(1).getName()), Functions.map(policies.subList(0, 2), new Functions.Unary<String, Policy>() {
                    @Override
                    public String call(Policy policy) {
                        return policy.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("guid="+policies.get(1).getGuid(), Arrays.asList(policies.get(1).getId()))
                .put("guid="+policies.get(0).getGuid()+"&guid="+policies.get(2).getGuid(), Arrays.asList(policies.get(0).getId(), policies.get(2).getId()))
                .put("type=Include", Arrays.asList(policies.get(0).getId()))
                .put("type=Internal", Arrays.asList(policies.get(1).getId()))
                .put("type=Global", Arrays.asList(policies.get(2).getId()))
                .put("soap=true", Arrays.asList(policies.get(0).getId(), policies.get(1).getId()))
                .put("parentFolder.id=" + rootFolder.getId(), Arrays.asList(policies.get(0).getId(), policies.get(2).getId()))
                .put("parentFolder.id=" + myFolder.getId(), Arrays.asList(policies.get(1).getId()))
                .map();
    }

    @Test
    public void createPolicyWithCommentTest() throws Exception {
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("My New Policy");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(rootFolder.getId());
        policyMO.setPolicyDetail(policyDetail);
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
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));

        String policyMOString = writeMOToString(policyMO);

        RestResponse response = processRequest(getResourceUri() +"?versionComment="+comment, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Goid policyGoid = new Goid(getFirstReferencedGoid(response));
        Policy policySaved = policyManager.findByPrimaryKey(policyGoid);
        assertNotNull(policySaved);
        assertEquals(policyDetail.getName(), policySaved.getName());
        assertEquals(AuditDetailAssertion.class, ((AllAssertion) policySaved.getAssertion()).getChildren().get(0).getClass());

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(policyGoid, 1);
        assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());

    }

    @Test
    public void createPolicyWithIDAndCommentTest() throws Exception {
        Goid id = new Goid(124124124, 1);

        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("My New Policy");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(rootFolder.getId());
        policyMO.setPolicyDetail(policyDetail);
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
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));

        String policyMOString = writeMOToString(policyMO);

        RestResponse response = processRequest(getResourceUri() + "/" + id  +"?versionComment="+comment, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Policy policySaved = policyManager.findByPrimaryKey(id);
        assertNotNull(policySaved);
        assertEquals(policyDetail.getName(), policySaved.getName());
        assertEquals(AuditDetailAssertion.class, ((AllAssertion) policySaved.getAssertion()).getChildren().get(0).getClass());

        final Goid goidReturned = new Goid(getFirstReferencedGoid(response));
        assertEquals(id, goidReturned);

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(goidReturned, 1);
        assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());
    }

    @Test
    public void updatePolicyTest() throws Exception {
        RestResponse response = processRequest(getResourceUri() + "/" + policies.get(0).getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        PolicyMO policyReturned = (PolicyMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        policyReturned.getPolicyDetail().setName("Policy Updated");
        Resource policyResource = policyReturned.getResourceSets().get(0).getResources().get(0);
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

        String policyMOString = writeMOToString(policyReturned);

        logger.info(policyMOString.toString());
        response = processRequest(getResourceUri() + "/" + policies.get(0).getId() + "?active=false&versionComment="+comment, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        Policy policySaved = policyManager.findByPrimaryKey(policies.get(0).getGoid());
        assertEquals("Policy Updated",policySaved.getName());

        PolicyVersion version = policyVersionManager.findPolicyVersionForPolicy(policies.get(0).getGoid(), 2);
        assertNotNull(version);
        assertEquals("Comment:", comment, version.getName());
        assertEquals("Active:", false, version.isActive());

        PolicyVersion oldVersion = policyVersionManager.findPolicyVersionForPolicy(policies.get(0).getGoid(), 1);
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

        for(Link link : links){
            if("self".equals(link.getRel())){
                return link.getUri().substring(link.getUri().lastIndexOf('/') + 1);
            }
        }
        return null;
    }
}
