package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyAliasManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.*;

import static junit.framework.Assert.assertEquals;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PolicyAliasRestEntityResourceTest extends RestEntityTests<PolicyAlias, PolicyAliasMO> {
    private PolicyAliasManager policyAliasManager;
    private PolicyVersionManager policyVersionManager;
    private PolicyManager policyManager;
    private FolderManager folderManager;

    private List<PolicyAlias> policyAliases = new ArrayList<>();
    private Folder rootFolder;
    private Folder myPolicyFolder;
    private Folder myAliasFolder;
    private Folder myEmptyFolder;
    private List<Policy> policies = new ArrayList<>();

    private static final String POLICY = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>";

    @Before
    public void before() throws ObjectModelException {

        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        policyAliasManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyAliasManager", PolicyAliasManager.class);

        //create a test folder
        rootFolder = folderManager.findRootFolder();
        myPolicyFolder = new Folder("MyPolicyFolder", rootFolder);
        folderManager.save(myPolicyFolder);
        myAliasFolder = new Folder("MyAliasFolder", rootFolder);
        folderManager.save(myAliasFolder);
        myEmptyFolder = new Folder("MyEmptyFolder", rootFolder);
        folderManager.save(myEmptyFolder);

        //create the published service
        for (int i = 0; i < 3; i++) {
            Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy " + i, POLICY, false);
            policy.setFolder(myPolicyFolder);
            policy.setGuid(UUID.randomUUID().toString());
            policy.setSoap(true);
            policy.disable();
            policyManager.save(policy);
            policyVersionManager.checkpointPolicy(policy, true, true);
            policies.add(policy);
        }


        //Create the PublishedServiceAliases
        PolicyAlias policyAlias = new PolicyAlias(policies.get(0), myAliasFolder);
        policyAliasManager.save(policyAlias);
        policyAliases.add(policyAlias);

        policyAlias = new PolicyAlias(policies.get(0), rootFolder);
        policyAliasManager.save(policyAlias);
        policyAliases.add(policyAlias);

        policyAlias = new PolicyAlias(policies.get(1), myAliasFolder);
        policyAliasManager.save(policyAlias);
        policyAliases.add(policyAlias);

        policyAlias = new PolicyAlias(policies.get(1), rootFolder);
        policyAliasManager.save(policyAlias);
        policyAliases.add(policyAlias);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<PolicyAlias> all = policyAliasManager.findAll();
        for (PolicyAlias policyAlias : all) {
            policyAliasManager.delete(policyAlias.getGoid());
        }

        for (Policy publishedService : policies) {
            policyManager.delete(publishedService.getGoid());
        }

        Collection<Folder> folders = folderManager.findAll();
        for (Folder folder : folders) {
            if (!Folder.ROOT_FOLDER_ID.equals(folder.getGoid())) {
                folderManager.delete(folder);
            }
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(policyAliases, new Functions.Unary<String, PolicyAlias>() {
            @Override
            public String call(PolicyAlias policyAlias) {
                return policyAlias.getId();
            }
        });
    }

    @Override
    public List<PolicyAliasMO> getCreatableManagedObjects() {
        List<PolicyAliasMO> policyAliasMOs = new ArrayList<>();

        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setId(getGoid().toString());
        policyAliasMO.setFolderId(myAliasFolder.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policies.get(2).getId()));
        policyAliasMOs.add(policyAliasMO);

        policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setId(getGoid().toString());
        policyAliasMO.setFolderId(rootFolder.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policies.get(2).getId()));
        policyAliasMOs.add(policyAliasMO);

        return policyAliasMOs;
    }

    @Override
    public List<PolicyAliasMO> getUpdateableManagedObjects() {
        List<PolicyAliasMO> policyAliasMOs = new ArrayList<>();

        PolicyAlias policyAlias = policyAliases.get(0);

        //change folder
        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setId(policyAlias.getId());
        policyAliasMO.setFolderId(myEmptyFolder.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policyAlias.getEntityGoid().toString()));
        policyAliasMOs.add(policyAliasMO);

        //update twice
        policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setId(policyAlias.getId());
        policyAliasMO.setFolderId(myAliasFolder.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policyAlias.getEntityGoid().toString()));
        policyAliasMOs.add(policyAliasMO);

        return policyAliasMOs;
    }

    @Override
    public Map<PolicyAliasMO, Functions.BinaryVoid<PolicyAliasMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<PolicyAliasMO, Functions.BinaryVoid<PolicyAliasMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //already existing alias in folder
        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId(myAliasFolder.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policies.get(0).getId()));
        builder.put(policyAliasMO, new Functions.BinaryVoid<PolicyAliasMO, RestResponse>() {
            @Override
            public void call(PolicyAliasMO policyAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //service in same folder
        policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId(myPolicyFolder.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policies.get(0).getId()));
        builder.put(policyAliasMO, new Functions.BinaryVoid<PolicyAliasMO, RestResponse>() {
            @Override
            public void call(PolicyAliasMO policyAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //bad service
        policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId(myAliasFolder.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, new Goid(123, 456).toString()));
        builder.put(policyAliasMO, new Functions.BinaryVoid<PolicyAliasMO, RestResponse>() {
            @Override
            public void call(PolicyAliasMO policyAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //bad folder
        policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId(new Goid(123, 456).toString());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policies.get(0).getId()));
        builder.put(policyAliasMO, new Functions.BinaryVoid<PolicyAliasMO, RestResponse>() {
            @Override
            public void call(PolicyAliasMO policyAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<PolicyAliasMO, Functions.BinaryVoid<PolicyAliasMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<PolicyAliasMO, Functions.BinaryVoid<PolicyAliasMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //move to service folder
        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setId(policyAliases.get(0).getId());
        policyAliasMO.setFolderId(myPolicyFolder.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policyAliases.get(0).getEntityGoid().toString()));
        builder.put(policyAliasMO, new Functions.BinaryVoid<PolicyAliasMO, RestResponse>() {
            @Override
            public void call(PolicyAliasMO policyAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //move to folder with alias already there
        policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setId(policyAliases.get(0).getId());
        policyAliasMO.setFolderId(rootFolder.getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policyAliases.get(0).getEntityGoid().toString()));
        builder.put(policyAliasMO, new Functions.BinaryVoid<PolicyAliasMO, RestResponse>() {
            @Override
            public void call(PolicyAliasMO policyAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //try to change backing service
        policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setId(policyAliases.get(0).getId());
        policyAliasMO.setFolderId(policyAliases.get(0).getFolder().getId());
        policyAliasMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policies.get(2).getId()));
        builder.put(policyAliasMO, new Functions.BinaryVoid<PolicyAliasMO, RestResponse>() {
            @Override
            public void call(PolicyAliasMO policyAliasMO, RestResponse restResponse) {
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
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(policyAliases, new Functions.Unary<String, PolicyAlias>() {
            @Override
            public String call(PolicyAlias policyAlias) {
                return policyAlias.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "policyAliases";
    }

    @Override
    public String getType() {
        return EntityType.POLICY_ALIAS.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        PolicyAlias entity = policyAliasManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        Policy publishedService = policyManager.findByPrimaryKey(entity.getEntityGoid());
        Assert.assertNotNull(publishedService);
        return publishedService.getName() + " alias";
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        PolicyAlias entity = policyAliasManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, PolicyAliasMO managedObject) throws FindException {
        PolicyAlias entity = policyAliasManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getEntityGoid().toString(), managedObject.getPolicyReference().getId());
            Assert.assertEquals(entity.getFolder().getId(), managedObject.getFolderId());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(policyAliases, new Functions.Unary<String, PolicyAlias>() {
                    @Override
                    public String call(PolicyAlias policyAlias) {
                        return policyAlias.getId();
                    }
                }))
                .put("policy.id=" + policies.get(0).getId(), Arrays.asList(policyAliases.get(0).getId(), policyAliases.get(1).getId()))
                .put("policy.id=" + policies.get(0).getId() + "&policy.id=" + policies.get(1).getId(), Functions.map(policyAliases.subList(0, 4), new Functions.Unary<String, PolicyAlias>() {
                    @Override
                    public String call(PolicyAlias policyAlias) {
                        return policyAlias.getId();
                    }
                }))
                .put("policy.id=" + new Goid(0, 0).toString(), Collections.<String>emptyList())
                .put("folder.id=" + myAliasFolder.getId(), Arrays.asList(policyAliases.get(0).getId(), policyAliases.get(2).getId()))
                .put("folder.id=" + rootFolder.getId(), Arrays.asList(policyAliases.get(1).getId(), policyAliases.get(3).getId()))
                .put("policy.id=" + policies.get(0).getId() + "&policy.id=" + policies.get(1).getId() + "&sort=policy.id&order=desc", Arrays.asList(policyAliases.get(3).getId(), policyAliases.get(2).getId(), policyAliases.get(1).getId(), policyAliases.get(0).getId()))
                .map();
    }


    @Test
    public void deleteNonEmptyFolderTest() throws Exception {
        RestResponse response = processRequest("folders/" + myPolicyFolder.getId(), HttpMethod.DELETE, null, "");

        assertEquals(403, response.getStatus());
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ErrorResponse returnedError = MarshallingUtils.unmarshal(ErrorResponse.class, source);
        assertEquals("ResourceAccess", returnedError.getType());
        assertEquals("Folder is not empty", returnedError.getDetail());
    }
}
