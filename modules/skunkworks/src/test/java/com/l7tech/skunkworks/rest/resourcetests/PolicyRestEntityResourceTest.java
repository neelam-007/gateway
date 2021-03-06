package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceManager;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PolicyRestEntityResourceTest extends RestEntityTests<Policy, PolicyMO> {
    private static final Logger logger = Logger.getLogger(PolicyRestEntityResourceTest.class.getName());
    private final String POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\"/>\n" +
            "</wsp:Policy>";

    private PolicyManager policyManager;
    private List<Policy> policies = new ArrayList<>();
    private Folder rootFolder;
    private PolicyVersionManager policyVersionManager;
    private SecurityZoneManager securityZoneManager;
    private SecurityZone securityZone;

    private static final String comment = "MyComment1";
    private FolderManager folderManager;
    private Folder myFolder;
    private ServiceManager serviceManager;
    private ClusterPropertyManager clusterPropertyManager;
    private PublishedService service;

    @Before
    public void before() throws ObjectModelException {
        serviceManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceManager", ServiceManager.class);
        clusterPropertyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("clusterPropertyManager", ClusterPropertyManager.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);

        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();

        myFolder = new Folder("MyFolder", rootFolder);
        folderManager.save(myFolder);

        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        securityZone = new SecurityZone();
        securityZone.setName("Zone");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.POLICY));
        securityZoneManager.save(securityZone);

        //Create the policies
        Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy 1",
                POLICY_XML,
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
                POLICY_XML,
                false
        );
        policy.setFolder(myFolder);
        policy.setSoap(true);
        policy.setGuid(UUID.randomUUID().toString());

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,comment,true);
        policies.add(policy);

        policy = new Policy(PolicyType.GLOBAL_FRAGMENT, "Policy 3",
                POLICY_XML,
                true
        );
        policy.setFolder(rootFolder);
        policy.setSoap(false);
        policy.setGuid(UUID.randomUUID().toString());
        policy.setInternalTag("Test");

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,comment,true);
        policies.add(policy);

        policy = new Policy(PolicyType.PRIVATE_SERVICE, "Policy 4",
                POLICY_XML,
                false
        );
        policy.setFolder(rootFolder);
        policy.setGuid(UUID.randomUUID().toString());

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,comment,true);
        policies.add(policy);

        policy = new Policy(PolicyType.IDENTITY_PROVIDER_POLICY, "Policy 5",
                POLICY_XML,
                false
        );
        policy.setFolder(rootFolder);
        policy.setGuid(UUID.randomUUID().toString());

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,comment,true);
        policies.add(policy);

        service = new PublishedService();
        service.setName("Service1");
        service.setRoutingUri("/test");
        service.getPolicy().setXml(POLICY_XML);
        service.setFolder(rootFolder);
        service.setSoap(false);
        service.setDisabled(false);
        service.getPolicy().setGuid(UUID.randomUUID().toString());
        serviceManager.save(service);
        policyVersionManager.checkpointPolicy(service.getPolicy(), true, true);
    }

    @After
    public void after() throws FindException, DeleteException {
        serviceManager.delete(service);
        Collection<Policy> all = policyManager.findAll();
        for (Policy policy : all) {
            policyManager.delete(policy.getGoid());
        }
        folderManager.delete(myFolder);
        securityZoneManager.delete(securityZone);
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(Functions.grep(policies, new Functions.Unary<Boolean, Policy>() {
            @Override
            public Boolean call(Policy policy) {
                return PolicyType.INCLUDE_FRAGMENT.equals(policy.getType()) ||
                        PolicyType.INTERNAL.equals(policy.getType()) ||
                        PolicyType.GLOBAL_FRAGMENT.equals(policy.getType()) ||
                        PolicyType.IDENTITY_PROVIDER_POLICY.equals(policy.getType());
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
        resource.setContent(POLICY_XML);
        resourceSet.setResources(Arrays.asList(resource));
        policy.setResourceSets(Arrays.asList(resourceSet));
        policies.add(policy);

        //create a policy without specifying a folder. should use root by default SSG-8808
        policy = ManagedObjectFactory.createPolicy();
        policy.setId(getGoid().toString());
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("New Policy 2");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policy.setPolicyDetail(policyDetail);
        resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent(POLICY_XML);
        resourceSet.setResources(Arrays.asList(resource));
        policy.setResourceSets(Arrays.asList(resourceSet));
        policies.add(policy);

        return policies;
    }

    @Override
    public List<PolicyMO> getUpdateableManagedObjects() {
        List<PolicyMO> policyMOs = new ArrayList<>();

        Policy policy = this.policies.get(0);

        //update name
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setId(policy.getId());
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(policy.getName() + "Updated");
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        policyMO.setVersion(policy.getVersion());
        policyMO.setGuid(policy.getGuid());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent(policy.getXml());
        resourceSet.setResources(Arrays.asList(resource));
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        policyMOs.add(policyMO);

        //update again SSG-8476
        policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setId(policy.getId());
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(policy.getName() + "Updated");
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        policyMO.setVersion(policy.getVersion() + 1);
        policyMO.setGuid(policy.getGuid());
        resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        resource = ManagedObjectFactory.createResource();
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

        //same name
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

        //null policy type
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

        //empty name SSG-8463
        policyMO = ManagedObjectFactory.createPolicy();
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("");
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        policyMO.setVersion(policy.getVersion());
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

        //existing guid
        policyMO = ManagedObjectFactory.createPolicy();
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("test");
        policyDetail.setGuid(policy.getGuid());
        policyMO.setGuid(policy.getGuid());
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        policyMO.setVersion(policy.getVersion());
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
        //same name as other policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setId(policy.getId());
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(this.policies.get(1).getName());
        policyDetail.setGuid(policy.getGuid());
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        policyMO.setGuid(policy.getGuid());
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

        //move to not existing folder. SSG-8477
        policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setId(policy.getId());
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(policy.getName());
        policyDetail.setGuid(policy.getGuid());
        policyDetail.setFolderId(getGoid().toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        policyMO.setGuid(policy.getGuid());

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

        //bad version SSG-8281
        policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setId(policy.getId());
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(policy.getName());
        policyDetail.setGuid(policy.getGuid());
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        policyMO.setVersion(policy.getVersion() + 100);
        policyMO.setGuid(policy.getGuid());
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

        //change guid SSG-8280
        policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setId(policy.getId());
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName(policy.getName());
        policyDetail.setFolderId(policy.getFolder().getId());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyMO.setPolicyDetail(policyDetail);
        policyMO.setVersion(policy.getVersion());
        policyMO.setGuid(UUID.randomUUID().toString());
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
            Assert.assertEquals("Policy xml's differ",
                    entity.getXml().trim().replaceAll("\\s+", " "),
                    managedObject.getResourceSets().get(0).getResources().get(0).getContent().trim().replaceAll("\\s+", " "));
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
                case IDENTITY_PROVIDER_POLICY:
                    Assert.assertEquals(PolicyDetail.PolicyType.ID_PROVIDER, managedObject.getPolicyDetail().getPolicyType());
                    break;
                default:
                    Assert.fail("should not be able to get policies of this type: " + entity.getType());
            }
            Assert.assertEquals(entity.getFolder().getId(), managedObject.getPolicyDetail().getFolderId() != null ? managedObject.getPolicyDetail().getFolderId() : Folder.ROOT_FOLDER_ID.toString());
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
                                PolicyType.GLOBAL_FRAGMENT.equals(policy.getType())||
                                PolicyType.IDENTITY_PROVIDER_POLICY.equals(policy.getType());
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
                .put("guid="+policies.get(0).getGuid()+"&guid="+policies.get(2).getGuid() + "&sort=id", Arrays.asList(policies.get(0).getId(), policies.get(2).getId()))
                .put("type=Include", Arrays.asList(policies.get(0).getId()))
                .put("type=Internal", Arrays.asList(policies.get(1).getId()))
                .put("type=Global", Arrays.asList(policies.get(2).getId()))
                .put("soap=true", Arrays.asList(policies.get(0).getId(), policies.get(1).getId()))
                .put("parentFolder.id=" + rootFolder.getId(), Arrays.asList(policies.get(0).getId(), policies.get(2).getId(), policies.get(4).getId()))
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
    public void updateIdentityProviderPolicyWithSecurityZoneTest() throws Exception {
        RestResponse response = processRequest(getResourceUri() + "/" + policies.get(4).getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        PolicyMO policyReturned = (PolicyMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // double check its the identity provider policy
        assertEquals(PolicyDetail.PolicyType.ID_PROVIDER, policyReturned.getPolicyDetail().getPolicyType());

        policyReturned.getPolicyDetail().setName("ID Provider Policy Updated");
        policyReturned.setSecurityZoneId(securityZone.getId());

        String policyMOString = writeMOToString(policyReturned);

        logger.info(policyMOString.toString());
        response = processRequest(getResourceUri() + "/" + policies.get(4).getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), policyMOString);
        logger.info(response.toString());

        assertEquals(400, response.getStatus());
        source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse returnedError = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("InvalidResource", returnedError.getType());
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

        Assert.assertEquals(200, response.getStatus());

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

    @Test
    public void testCreateDeleteDebugTracePolicy() throws Exception {
        PolicyMO policy = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("[Internal Debug Trace Policy]");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INTERNAL);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("tag", "debug-trace").map());
        policy.setPolicyDetail(policyDetail);
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent(POLICY_XML);
        resourceSet.setResources(Arrays.asList(resource));
        policy.setResourceSets(Arrays.asList(resourceSet));

        RestResponse response = processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(policy)));
        logger.log(Level.FINE, response.toString());
        Assert.assertEquals(201, response.getStatus());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PolicyMO> policyReturned = MarshallingUtils.unmarshal(Item.class, source);

        response = processRequest(getResourceUri() + "/" + policyReturned.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        source = new StreamSource(new StringReader(response.getBody()));
        policyReturned = MarshallingUtils.unmarshal(Item.class, source);

        ClusterProperty traceProp = clusterPropertyManager.findByUniqueName(ServerConfigParams.PARAM_TRACE_POLICY_GUID);
        Assert.assertNotNull(traceProp);
        assertEquals(traceProp.getValue(), policyReturned.getContent().getGuid());

        response = processRequest(getResourceUri() + "/" + policyReturned.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
        logger.info(response.toString());

        assertEquals(204, response.getStatus());

        traceProp = clusterPropertyManager.findByUniqueName(ServerConfigParams.PARAM_TRACE_POLICY_GUID);
        Assert.assertNull(traceProp);
    }

    @Test
    public void testCreateDeleteFailedDebugTracePolicy() throws Exception {
        PolicyMO policy = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setName("[Internal Debug Trace Policy]");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INTERNAL);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("tag", "debug-trace").map());
        policy.setPolicyDetail(policyDetail);
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resource.setContent(POLICY_XML);
        resourceSet.setResources(Arrays.asList(resource));
        policy.setResourceSets(Arrays.asList(resourceSet));

        RestResponse response = processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(policy)));
        logger.log(Level.FINE, response.toString());
        Assert.assertEquals(201, response.getStatus());
        StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item<PolicyMO> policyReturned = MarshallingUtils.unmarshal(Item.class, source);

        response = processRequest(getResourceUri() + "/" + policyReturned.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        source = new StreamSource(new StringReader(response.getBody()));
        policyReturned = MarshallingUtils.unmarshal(Item.class, source);

        ClusterProperty traceProp = clusterPropertyManager.findByUniqueName(ServerConfigParams.PARAM_TRACE_POLICY_GUID);
        Assert.assertNotNull(traceProp);
        assertEquals(traceProp.getValue(), policyReturned.getContent().getGuid());

        service = serviceManager.findByPrimaryKey(service.getGoid());
        service.setTracingEnabled(true);
        serviceManager.update(service);
        service = serviceManager.findByPrimaryKey(service.getGoid());

        response = processRequest(getResourceUri() + "/" + policyReturned.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
        logger.info(response.toString());

        assertEquals(403, response.getStatus());
        source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse returnedError = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceAccess", returnedError.getType());

        traceProp = clusterPropertyManager.findByUniqueName(ServerConfigParams.PARAM_TRACE_POLICY_GUID);
        Assert.assertNotNull(traceProp);
        assertEquals(traceProp.getValue(), policyReturned.getContent().getGuid());

        service.setTracingEnabled(false);
        serviceManager.update(service);

        response = processRequest(getResourceUri() + "/" + policyReturned.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
        logger.info(response.toString());

        assertEquals(204, response.getStatus());

        traceProp = clusterPropertyManager.findByUniqueName(ServerConfigParams.PARAM_TRACE_POLICY_GUID);
        Assert.assertNull(traceProp);
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
