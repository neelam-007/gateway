package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManager;
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
public class EncapsulatedAssertionRestEntityResourceTest extends RestEntityTests<EncapsulatedAssertionConfig, EncapsulatedAssertionMO> {
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;
    private List<EncapsulatedAssertionConfig> encapsulatedAssertionConfigs = new ArrayList<>();
    private PolicyManager policyManager;
    private FolderManager folderManager;
    private Folder rootFolder;
    private Policy policy;

    @Before
    public void before() throws SaveException, FindException {
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

        encapsulatedAssertionConfigManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class);

        EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setName("Encass 1");
        encapsulatedAssertionConfig.setGuid(UUID.randomUUID().toString());
        encapsulatedAssertionConfig.setPolicy(policy);

        encapsulatedAssertionConfigManager.save(encapsulatedAssertionConfig);
        encapsulatedAssertionConfigs.add(encapsulatedAssertionConfig);

        encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setName("Encass 2");
        encapsulatedAssertionConfig.setGuid(UUID.randomUUID().toString());
        encapsulatedAssertionConfig.setPolicy(policy);
        EncapsulatedAssertionArgumentDescriptor encapsulatedAssertionArgumentDescriptor = new EncapsulatedAssertionArgumentDescriptor();
        encapsulatedAssertionArgumentDescriptor.setArgumentName("Arg1");
        encapsulatedAssertionArgumentDescriptor.setArgumentType("String");
        encapsulatedAssertionArgumentDescriptor.setGuiLabel("Arg1Label");
        encapsulatedAssertionArgumentDescriptor.setOrdinal(1);
        encapsulatedAssertionArgumentDescriptor.setGuiPrompt(false);
        encapsulatedAssertionArgumentDescriptor.setEncapsulatedAssertionConfig(encapsulatedAssertionConfig);
        encapsulatedAssertionConfig.setArgumentDescriptors(CollectionUtils.set(encapsulatedAssertionArgumentDescriptor));
        EncapsulatedAssertionResultDescriptor encapsulatedAssertionResultDescriptor = new EncapsulatedAssertionResultDescriptor();
        encapsulatedAssertionResultDescriptor.setResultName("resultName");
        encapsulatedAssertionResultDescriptor.setResultType("Integer");
        encapsulatedAssertionResultDescriptor.setEncapsulatedAssertionConfig(encapsulatedAssertionConfig);
        encapsulatedAssertionConfig.setResultDescriptors(CollectionUtils.set(encapsulatedAssertionResultDescriptor));
        encapsulatedAssertionConfig.putProperty(EncapsulatedAssertionConfig.PROP_PALETTE_FOLDER, "palette folder");
        encapsulatedAssertionConfig.putProperty(EncapsulatedAssertionConfig.PROP_DESCRIPTION, "Some description");
        encapsulatedAssertionConfig.putProperty(EncapsulatedAssertionConfig.PROP_ICON_BASE64, "icon!");
        encapsulatedAssertionConfig.putProperty(EncapsulatedAssertionConfig.PROP_ALLOW_TRACING, "true");

        encapsulatedAssertionConfigManager.save(encapsulatedAssertionConfig);
        encapsulatedAssertionConfigs.add(encapsulatedAssertionConfig);

        encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setName("Encass 3");
        encapsulatedAssertionConfig.setGuid(UUID.randomUUID().toString());
        encapsulatedAssertionConfig.setPolicy(policy);

        encapsulatedAssertionConfigManager.save(encapsulatedAssertionConfig);
        encapsulatedAssertionConfigs.add(encapsulatedAssertionConfig);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<EncapsulatedAssertionConfig> all = encapsulatedAssertionConfigManager.findAll();
        for (EncapsulatedAssertionConfig encapsulatedAssertionConfig : all) {
            encapsulatedAssertionConfigManager.delete(encapsulatedAssertionConfig.getGoid());
        }
        policyManager.delete(policy);
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(encapsulatedAssertionConfigs, new Functions.Unary<String, EncapsulatedAssertionConfig>() {
            @Override
            public String call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                return encapsulatedAssertionConfig.getId();
            }
        });
    }

    @Override
    public List<EncapsulatedAssertionMO> getCreatableManagedObjects() {
        List<EncapsulatedAssertionMO> encapsulatedAssertionMOs = new ArrayList<>();

        EncapsulatedAssertionMO encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setId(getGoid().toString());
        encapsulatedAssertionMO.setName("My New Encass");
        encapsulatedAssertionMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policy.getId()));
        encapsulatedAssertionMOs.add(encapsulatedAssertionMO);

        return encapsulatedAssertionMOs;
    }

    @Override
    public List<EncapsulatedAssertionMO> getUpdateableManagedObjects() {
        List<EncapsulatedAssertionMO> encapsulatedAssertionMOs = new ArrayList<>();

        EncapsulatedAssertionConfig encapsulatedAssertionConfig = encapsulatedAssertionConfigs.get(0);
        EncapsulatedAssertionMO encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setId(encapsulatedAssertionConfig.getId());
        encapsulatedAssertionMO.setGuid(encapsulatedAssertionConfig.getGuid());
        encapsulatedAssertionMO.setName(encapsulatedAssertionConfig.getName() + "Updated");
        encapsulatedAssertionMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, encapsulatedAssertionConfig.getPolicy().getId()));
        encapsulatedAssertionMOs.add(encapsulatedAssertionMO);

        //update twice
        encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setId(encapsulatedAssertionConfig.getId());
        encapsulatedAssertionMO.setGuid(encapsulatedAssertionConfig.getGuid());
        encapsulatedAssertionMO.setName(encapsulatedAssertionConfig.getName() + "Updated");
        encapsulatedAssertionMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, encapsulatedAssertionConfig.getPolicy().getId()));
        encapsulatedAssertionMOs.add(encapsulatedAssertionMO);

        return encapsulatedAssertionMOs;
    }

    @Override
    public Map<EncapsulatedAssertionMO, Functions.BinaryVoid<EncapsulatedAssertionMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<EncapsulatedAssertionMO, Functions.BinaryVoid<EncapsulatedAssertionMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //same name
        EncapsulatedAssertionMO encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setName(encapsulatedAssertionConfigs.get(0).getName());
        encapsulatedAssertionMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policy.getId()));
        builder.put(encapsulatedAssertionMO, new Functions.BinaryVoid<EncapsulatedAssertionMO, RestResponse>() {
            @Override
            public void call(EncapsulatedAssertionMO encapsulatedAssertionMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //same guid
        encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setName("My New Encass");
        encapsulatedAssertionMO.setGuid(encapsulatedAssertionConfigs.get(0).getGuid());
        encapsulatedAssertionMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policy.getId()));
        builder.put(encapsulatedAssertionMO, new Functions.BinaryVoid<EncapsulatedAssertionMO, RestResponse>() {
            @Override
            public void call(EncapsulatedAssertionMO encapsulatedAssertionMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //long name SSG-8204
        encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setName("aaaaabbbbbcccccdddzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddaaaaabbbbbcccccdddddzzzzzzzzzz");
        encapsulatedAssertionMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, policy.getId()));
        builder.put(encapsulatedAssertionMO, new Functions.BinaryVoid<EncapsulatedAssertionMO, RestResponse>() {
            @Override
            public void call(EncapsulatedAssertionMO encapsulatedAssertionMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<EncapsulatedAssertionMO, Functions.BinaryVoid<EncapsulatedAssertionMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<EncapsulatedAssertionMO, Functions.BinaryVoid<EncapsulatedAssertionMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //same name
        EncapsulatedAssertionMO encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setId(encapsulatedAssertionConfigs.get(0).getId());
        encapsulatedAssertionMO.setName(encapsulatedAssertionConfigs.get(1).getName());
        encapsulatedAssertionMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, encapsulatedAssertionConfigs.get(0).getPolicy().getId()));
        builder.put(encapsulatedAssertionMO, new Functions.BinaryVoid<EncapsulatedAssertionMO, RestResponse>() {
            @Override
            public void call(EncapsulatedAssertionMO encapsulatedAssertionMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //same guid
        encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setId(encapsulatedAssertionConfigs.get(0).getId());
        encapsulatedAssertionMO.setName(encapsulatedAssertionConfigs.get(0).getName());
        encapsulatedAssertionMO.setGuid(encapsulatedAssertionConfigs.get(1).getGuid());
        encapsulatedAssertionMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, encapsulatedAssertionConfigs.get(0).getPolicy().getId()));
        builder.put(encapsulatedAssertionMO, new Functions.BinaryVoid<EncapsulatedAssertionMO, RestResponse>() {
            @Override
            public void call(EncapsulatedAssertionMO encapsulatedAssertionMO, RestResponse restResponse) {
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
        return Functions.map(encapsulatedAssertionConfigs, new Functions.Unary<String, EncapsulatedAssertionConfig>() {
            @Override
            public String call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                return encapsulatedAssertionConfig.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "encapsulatedAssertions";
    }

    @Override
    public String getType() {
        return EntityType.ENCAPSULATED_ASSERTION.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        EncapsulatedAssertionConfig entity = encapsulatedAssertionConfigManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        EncapsulatedAssertionConfig entity = encapsulatedAssertionConfigManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, EncapsulatedAssertionMO managedObject) throws FindException {
        EncapsulatedAssertionConfig entity = encapsulatedAssertionConfigManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            if (managedObject.getGuid() != null) {
                Assert.assertEquals(entity.getGuid(), managedObject.getGuid());
            }
            Assert.assertEquals(entity.getPolicy().getId(), managedObject.getPolicyReference().getId());
            Assert.assertEquals(entity.getArgumentDescriptors().size(), managedObject.getEncapsulatedArguments().size());
            for (final EncapsulatedAssertionArgumentDescriptor encapsulatedAssertionArgumentDescriptor : entity.getArgumentDescriptors()) {
                EncapsulatedAssertionMO.EncapsulatedArgument encapsulatedArgument = Functions.grepFirst(managedObject.getEncapsulatedArguments(), new Functions.Unary<Boolean, EncapsulatedAssertionMO.EncapsulatedArgument>() {
                    @Override
                    public Boolean call(EncapsulatedAssertionMO.EncapsulatedArgument encapsulatedArgument) {
                        return encapsulatedArgument.getOrdinal() == encapsulatedAssertionArgumentDescriptor.getOrdinal();
                    }
                });
                Assert.assertNotNull("Could not find encass arguement: " + encapsulatedAssertionArgumentDescriptor.getArgumentName(), encapsulatedArgument);
                Assert.assertEquals(encapsulatedAssertionArgumentDescriptor.getArgumentName(), encapsulatedArgument.getArgumentName());
                Assert.assertEquals(encapsulatedAssertionArgumentDescriptor.getArgumentType(), encapsulatedArgument.getArgumentType());
                Assert.assertEquals(encapsulatedAssertionArgumentDescriptor.getGuiLabel(), encapsulatedArgument.getGuiLabel());
                Assert.assertEquals(encapsulatedAssertionArgumentDescriptor.getOrdinal(), encapsulatedArgument.getOrdinal());
            }
            Assert.assertEquals(entity.getResultDescriptors().size(), managedObject.getEncapsulatedResults().size());
            for (final EncapsulatedAssertionResultDescriptor encapsulatedAssertionResultDescriptor : entity.getResultDescriptors()) {
                EncapsulatedAssertionMO.EncapsulatedResult encapsulatedResult = Functions.grepFirst(managedObject.getEncapsulatedResults(), new Functions.Unary<Boolean, EncapsulatedAssertionMO.EncapsulatedResult>() {
                    @Override
                    public Boolean call(EncapsulatedAssertionMO.EncapsulatedResult encapsulatedResult) {
                        return encapsulatedResult.getResultName().equals(encapsulatedAssertionResultDescriptor.getResultName()) && encapsulatedResult.getResultType().equals(encapsulatedAssertionResultDescriptor.getResultType());
                    }
                });
                Assert.assertNotNull("Could not find encass result: " + encapsulatedAssertionResultDescriptor.getResultName(), encapsulatedResult);
            }
            if (managedObject.getProperties() != null) {
                Assert.assertEquals(entity.getProperties().size(), managedObject.getProperties().size());
                for (final String propKey : entity.getProperties().keySet()) {
                    Assert.assertNotNull("Could not find encass property: " + propKey, managedObject.getProperties().get(propKey));
                    Assert.assertEquals(entity.getProperties().get(propKey), managedObject.getProperties().get(propKey));
                }
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(encapsulatedAssertionConfigs, new Functions.Unary<String, EncapsulatedAssertionConfig>() {
                    @Override
                    public String call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                        return encapsulatedAssertionConfig.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(encapsulatedAssertionConfigs.get(0).getName()), Arrays.asList(encapsulatedAssertionConfigs.get(0).getId()))
                .put("name=" + URLEncoder.encode(encapsulatedAssertionConfigs.get(0).getName()) + "&name=" + URLEncoder.encode(encapsulatedAssertionConfigs.get(1).getName()), Functions.map(encapsulatedAssertionConfigs.subList(0, 2), new Functions.Unary<String, EncapsulatedAssertionConfig>() {
                    @Override
                    public String call(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
                        return encapsulatedAssertionConfig.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("policy.id=" + getGoid().toString(), Collections.<String>emptyList())
                .put("policy.id=" + policy.getGoid(), Arrays.asList(encapsulatedAssertionConfigs.get(0).getId(), encapsulatedAssertionConfigs.get(1).getId(), encapsulatedAssertionConfigs.get(2).getId()))
                .put("name=" + URLEncoder.encode(encapsulatedAssertionConfigs.get(0).getName()) + "&name=" + URLEncoder.encode(encapsulatedAssertionConfigs.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(encapsulatedAssertionConfigs.get(1).getId(), encapsulatedAssertionConfigs.get(0).getId()))
                .map();
    }
}
