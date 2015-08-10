package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyBackedServiceMO;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.objectmodel.polback.PolicyBackedServiceOperation;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.polback.PolicyBackedServiceManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.net.URLEncoder;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PolicyBackedServiceRestEntityResourceTest extends RestEntityTests<PolicyBackedService, PolicyBackedServiceMO> {
    private PolicyBackedServiceManager policyBackedServiceManager;
    private SecurityZoneManager securityZoneManager;
    private List<PolicyBackedService> policyBackedServices = new ArrayList<>();
    private SecurityZone securityZone;
    private PolicyManager policyManager;
    private FolderManager folderManager;
    private Folder rootFolder;
    private Policy policy;

    @Before
    public void before() throws Exception {
        policyBackedServiceManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyBackedServiceManager", PolicyBackedServiceManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();


        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy 1",
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

        //Create new connections
        PolicyBackedService psb1 = new PolicyBackedService();
        psb1.setId(getGoid().toString());
        psb1.setName("Test PBS 1");
        psb1.setServiceInterfaceName("my.interface.pbsA");
        PolicyBackedServiceOperation policyBackedServiceOperation = new PolicyBackedServiceOperation();
        policyBackedServiceOperation.setPolicyGoid(policy.getGoid());
        policyBackedServiceOperation.setPolicyBackedService(psb1);
        psb1.setOperations(CollectionUtils.set(policyBackedServiceOperation));
        policyBackedServices.add(psb1);
        policyBackedServiceManager.save(psb1);

        securityZone = new SecurityZone();
        securityZone.setName("Zone1");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.ANY));
        securityZone.setGoid(securityZoneManager.save(securityZone));

        PolicyBackedService psb2 = new PolicyBackedService();
        psb2.setId(getGoid().toString());
        psb2.setName("Test PBS 2");
        psb2.setServiceInterfaceName("my.interface.pbsA");
        policyBackedServiceOperation = new PolicyBackedServiceOperation();
        policyBackedServiceOperation.setPolicyGoid(policy.getGoid());
        policyBackedServiceOperation.setPolicyBackedService(psb2);
        psb2.setOperations(CollectionUtils.set(policyBackedServiceOperation));
        psb2.setSecurityZone(securityZone);
        policyBackedServices.add(psb2);
        policyBackedServiceManager.save(psb2);

        PolicyBackedService psb3 = new PolicyBackedService();
        psb3.setId(getGoid().toString());
        psb3.setName("Test PBS 3");
        psb3.setServiceInterfaceName("my.interface.pbsB");
        policyBackedServiceOperation = new PolicyBackedServiceOperation();
        policyBackedServiceOperation.setPolicyGoid(policy.getGoid());
        policyBackedServiceOperation.setPolicyBackedService(psb3);
        psb3.setOperations(CollectionUtils.set(policyBackedServiceOperation));
        policyBackedServices.add(psb3);
        policyBackedServiceManager.save(psb3);
    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<PolicyBackedService> all = policyBackedServiceManager.findAll();
        for (PolicyBackedService policyBackedService : all) {
            policyBackedServiceManager.delete(policyBackedService.getGoid());
        }

        securityZoneManager.delete(securityZone);
        policyManager.delete(policy);
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(policyBackedServices, new Functions.Unary<String, PolicyBackedService>() {
            @Override
            public String call(PolicyBackedService policyBackedService) {
                return policyBackedService.getId();
            }
        });
    }

    @Override
    public List<PolicyBackedServiceMO> getCreatableManagedObjects() {
        List<PolicyBackedServiceMO> policyBackedServiceMOs = new ArrayList<>();

        PolicyBackedServiceMO policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setId(getGoid().toString());
        policyBackedServiceMO.setName("Test PBS created");
        policyBackedServiceMO.setInterfaceName("test.interface");
        policyBackedServiceMO.setPolicyBackedServiceOperationPolicyIds(Arrays.asList(policy.getId()));

        policyBackedServiceMOs.add(policyBackedServiceMO);

        return policyBackedServiceMOs;
    }

    @Override
    public List<PolicyBackedServiceMO> getUpdateableManagedObjects() {
        List<PolicyBackedServiceMO> policyBackedServiceMOs = new ArrayList<>();

        PolicyBackedService policyBackedService = this.policyBackedServices.get(0);
        PolicyBackedServiceMO policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setId(policyBackedService.getId());
        policyBackedServiceMO.setVersion(policyBackedService.getVersion());
        policyBackedServiceMO.setName(policyBackedService.getName() + " Updated 1");
        policyBackedServiceMO.setInterfaceName(policyBackedService.getServiceInterfaceName());
        policyBackedServiceMO.setPolicyBackedServiceOperationPolicyIds(Functions.map(policyBackedService.getOperations(), new Functions.Unary<String, PolicyBackedServiceOperation>() {
            @Override
            public String call(PolicyBackedServiceOperation policyBackedServiceOperation) {
                return policyBackedServiceOperation.getPolicyGoid().toString();
            }
        }));
        policyBackedServiceMOs.add(policyBackedServiceMO);

        //update twice
        policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setId(policyBackedService.getId());
        policyBackedServiceMO.setVersion(policyBackedService.getVersion());
        policyBackedServiceMO.setName(policyBackedService.getName() + " Updated 2");
        policyBackedServiceMO.setInterfaceName(policyBackedService.getServiceInterfaceName());
        policyBackedServiceMO.setPolicyBackedServiceOperationPolicyIds(Functions.map(policyBackedService.getOperations(), new Functions.Unary<String, PolicyBackedServiceOperation>() {
            @Override
            public String call(PolicyBackedServiceOperation policyBackedServiceOperation) {
                return policyBackedServiceOperation.getPolicyGoid().toString();
            }
        }));
        policyBackedServiceMOs.add(policyBackedServiceMO);

        return policyBackedServiceMOs;
    }

    @Override
    public Map<PolicyBackedServiceMO, Functions.BinaryVoid<PolicyBackedServiceMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<PolicyBackedServiceMO, Functions.BinaryVoid<PolicyBackedServiceMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        PolicyBackedServiceMO policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setName("UnCreatable PolicyBacked Service No Interface");
        policyBackedServiceMO.setInterfaceName(null);
        policyBackedServiceMO.setPolicyBackedServiceOperationPolicyIds(Arrays.asList(policy.getId()));

        builder.put(policyBackedServiceMO, new Functions.BinaryVoid<PolicyBackedServiceMO, RestResponse>() {
            @Override
            public void call(PolicyBackedServiceMO policyBackedServiceMO1, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setName("UnCreatable PolicyBackedService empty policyids");
        policyBackedServiceMO.setInterfaceName("my.interface");
        policyBackedServiceMO.setPolicyBackedServiceOperationPolicyIds(Collections.<String>emptyList());

        builder.put(policyBackedServiceMO, new Functions.BinaryVoid<PolicyBackedServiceMO, RestResponse>() {
            @Override
            public void call(PolicyBackedServiceMO policyBackedServiceMO1, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<PolicyBackedServiceMO, Functions.BinaryVoid<PolicyBackedServiceMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<PolicyBackedServiceMO, Functions.BinaryVoid<PolicyBackedServiceMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        PolicyBackedServiceMO policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setId(policyBackedServices.get(0).getId());
        policyBackedServiceMO.setVersion(policyBackedServices.get(0).getVersion());
        policyBackedServiceMO.setName(policyBackedServices.get(0).getName() + " Updated");
        policyBackedServiceMO.setInterfaceName(null);
        policyBackedServiceMO.setPolicyBackedServiceOperationPolicyIds(Arrays.asList(policy.getId()));

        builder.put(policyBackedServiceMO, new Functions.BinaryVoid<PolicyBackedServiceMO, RestResponse>() {
            @Override
            public void call(PolicyBackedServiceMO policyBackedServiceMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        builder.put("asdf" + getGoid().toString(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals("Expected successful response", 400, restResponse.getStatus());
            }
        });
        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(policyBackedServices, new Functions.Unary<String, PolicyBackedService>() {
            @Override
            public String call(PolicyBackedService cassandraConnection) {
                return cassandraConnection.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "policyBackedServices";
    }

    @Override
    public String getType() {
        return EntityType.POLICY_BACKED_SERVICE.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        PolicyBackedService entity = policyBackedServiceManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        PolicyBackedService entity = policyBackedServiceManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, PolicyBackedServiceMO managedObject) throws FindException {
        PolicyBackedService entity = policyBackedServiceManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getServiceInterfaceName(), managedObject.getInterfaceName());
            Assert.assertEquals(entity.getOperations().size(), managedObject.getPolicyBackedServiceOperationPolicyIds().size());
            for(PolicyBackedServiceOperation policyBackedServiceOperation : entity.getOperations()){
                Assert.assertTrue(managedObject.getPolicyBackedServiceOperationPolicyIds().contains(policyBackedServiceOperation.getPolicyGoid().toString()));
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(policyBackedServices, new Functions.Unary<String, PolicyBackedService>() {
                    @Override
                    public String call(PolicyBackedService message) {
                        return message.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(policyBackedServices.get(0).getName()),
                        Arrays.asList(policyBackedServices.get(0).getId()))
                .put("name=" + URLEncoder.encode(policyBackedServices.get(0).getName()) + "&name=" + URLEncoder.encode(policyBackedServices.get(1).getName()),
                        Functions.map(policyBackedServices.subList(0, 2), new Functions.Unary<String, PolicyBackedService>() {
                            @Override
                            public String call(PolicyBackedService policyBackedService) {
                                return policyBackedService.getId();
                            }
                        }))
                .put("name=banName", Collections.<String>emptyList())
                .put("name=" + URLEncoder.encode(policyBackedServices.get(0).getName()) + "&name=" + URLEncoder.encode(policyBackedServices.get(1).getName()) + "&sort=name&order=desc",
                        Arrays.asList(policyBackedServices.get(1).getId(), policyBackedServices.get(0).getId()))
                .put("interface=" + URLEncoder.encode(policyBackedServices.get(2).getServiceInterfaceName()), Arrays.asList(policyBackedServices.get(2).getId()))
                .put("interface=" + URLEncoder.encode(policyBackedServices.get(0).getServiceInterfaceName()),
                        Functions.map(policyBackedServices.subList(0, 2), new Functions.Unary<String, PolicyBackedService>() {
                            @Override
                            public String call(PolicyBackedService policyBackedService) {
                                return policyBackedService.getId();
                            }
                        }))
                .put("securityZone.id=" + securityZone.getId(), Arrays.asList(policyBackedServices.get(1).getId()))
                .map();
    }
}
