package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.util.ConfiguredSessionFactoryBean;
import com.l7tech.skunkworks.rest.tools.RestResponse;
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
import java.io.StringReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PolicyBackedServiceMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(PolicyBackedServiceMigration.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<PolicyBackedServiceMO> policyBackedServiceItem;
    private Item<Mappings> mappingsToClean;

    private static ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator configuredGOIDGenerator = new ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator();

    @Before
    public void before() throws Exception {
        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("MyPolicy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.SERVICE_OPERATION);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("tag", "test.interface")
                .put("subtag", "myMethod")
                .map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);
        policyMO.setId(policyItem.getId());

        //create PBS;
        PolicyBackedServiceMO policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setName("Test PBS created");
        policyBackedServiceMO.setInterfaceName("test.interface");
        policyBackedServiceMO.setPolicyBackedServiceOperationPolicyIds(Arrays.asList(policyItem.getId()));
        response = getSourceEnvironment().processRequest("policyBackedServices", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyBackedServiceMO)));

        assertOkCreatedResponse(response);

        policyBackedServiceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyBackedServiceItem.setContent(policyBackedServiceMO);

    }

    @After
    public void after() throws Exception {
        if (mappingsToClean != null)
            cleanupAll(mappingsToClean);

        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("policyBackedServices/" + policyBackedServiceItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?policyBackedService=" + policyBackedServiceItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 2 items.", 2, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 3 items.", 3, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //test that the PBS is included
        Mapping pbsMapping = Functions.grepFirst(bundleItem.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
            @Override
            public Boolean call(Mapping mapping) {
                return EntityType.POLICY_BACKED_SERVICE.toString().equals(mapping.getType()) && policyBackedServiceItem.getId().equals(mapping.getSrcId());
            }
        });

        assertNotNull(pbsMapping);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        pbsMapping = Functions.grepFirst(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
            @Override
            public Boolean call(Mapping mapping) {
                return EntityType.POLICY_BACKED_SERVICE.toString().equals(mapping.getType()) && policyBackedServiceItem.getId().equals(mapping.getSrcId());
            }
        });

        Assert.assertEquals(EntityType.POLICY_BACKED_SERVICE.toString(), pbsMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, pbsMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, pbsMapping.getActionTaken());
        Assert.assertEquals(policyBackedServiceItem.getId(), pbsMapping.getSrcId());
        Assert.assertEquals(pbsMapping.getSrcId(), pbsMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testImportMapPolicy() throws Exception {

        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("MyPolicyTarget");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.SERVICE_OPERATION);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("tag", "test.interface.other")
                .put("subtag", "myMethodtarget")
                .map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);
        Item<PolicyMO> policyItemTarget = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try {

            response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //test that the PBS is included
            Mapping pbsMapping = Functions.grepFirst(bundleItem.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.POLICY_BACKED_SERVICE.toString().equals(mapping.getType()) && policyBackedServiceItem.getId().equals(mapping.getSrcId());
                }
            });

            assertNotNull(pbsMapping);

            //get policy mapping
            Mapping policyMapping = Functions.grepFirst(bundleItem.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.POLICY.toString().equals(mapping.getType()) && policyItem.getId().equals(mapping.getSrcId());
                }
            });
            policyMapping.setTargetId(policyItemTarget.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            pbsMapping = Functions.grepFirst(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.POLICY_BACKED_SERVICE.toString().equals(mapping.getType()) && policyBackedServiceItem.getId().equals(mapping.getSrcId());
                }
            });

            Assert.assertEquals(EntityType.POLICY_BACKED_SERVICE.toString(), pbsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, pbsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, pbsMapping.getActionTaken());
            Assert.assertEquals(policyBackedServiceItem.getId(), pbsMapping.getSrcId());
            Assert.assertEquals(pbsMapping.getSrcId(), pbsMapping.getTargetId());

            policyMapping = Functions.grepFirst(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.POLICY.toString().equals(mapping.getType()) && policyItem.getId().equals(mapping.getSrcId());
                }
            });

            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyItemTarget.getId(), policyMapping.getTargetId());

            //verify service
            response = getTargetEnvironment().processRequest("policyBackedServices/" + policyBackedServiceItem.getId(), HttpMethod.GET, null, "");
            Item<PolicyBackedServiceMO> ServiceDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            PolicyBackedServiceMO policyBackedServiceTarget = ServiceDependencies.getContent();
            Assert.assertNotNull(policyBackedServiceTarget);
            Assert.assertEquals(1, policyBackedServiceTarget.getPolicyBackedServiceOperationPolicyIds().size());
            Assert.assertNotNull(policyItemTarget.getId(), policyBackedServiceTarget.getPolicyBackedServiceOperationPolicyIds().get(0));

            mappingsToClean = mappings;

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("policies/" + policyItemTarget.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void testImportMapUpdateExisting() throws Exception {

        //create policy;
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("MyPolicyTarget");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.SERVICE_OPERATION);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("tag", "test.interface.other")
                .put("subtag", "myMethodtarget")
                .map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);
        Item<PolicyMO> policyItemTarget = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create PBS;
        PolicyBackedServiceMO policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setName("Test PBS created");
        policyBackedServiceMO.setInterfaceName("test.interface");
        policyBackedServiceMO.setPolicyBackedServiceOperationPolicyIds(Arrays.asList(policyItemTarget.getId()));
        response = getTargetEnvironment().processRequest("policyBackedServices", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyBackedServiceMO)));

        assertOkCreatedResponse(response);
        Item<PolicyMO> pbsItemTarget = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        try {

            response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //test that the PBS is included
            Mapping pbsMapping = Functions.grepFirst(bundleItem.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.POLICY_BACKED_SERVICE.toString().equals(mapping.getType()) && policyBackedServiceItem.getId().equals(mapping.getSrcId());
                }
            });

            assertNotNull(pbsMapping);

            //map the pbs to the one on the target
            pbsMapping.setTargetId(pbsItemTarget.getId());
            pbsMapping.setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            pbsMapping = Functions.grepFirst(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.POLICY_BACKED_SERVICE.toString().equals(mapping.getType()) && policyBackedServiceItem.getId().equals(mapping.getSrcId());
                }
            });

            Assert.assertEquals(EntityType.POLICY_BACKED_SERVICE.toString(), pbsMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, pbsMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, pbsMapping.getActionTaken());
            Assert.assertEquals(policyBackedServiceItem.getId(), pbsMapping.getSrcId());
            Assert.assertEquals(pbsItemTarget.getId(), pbsMapping.getTargetId());

            Mapping policyMapping = Functions.grepFirst(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.POLICY.toString().equals(mapping.getType()) && policyItem.getId().equals(mapping.getSrcId());
                }
            });

            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            //verify service
            response = getTargetEnvironment().processRequest("policyBackedServices/" + pbsItemTarget.getId(), HttpMethod.GET, null, "");
            Item<PolicyBackedServiceMO> serviceDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            PolicyBackedServiceMO policyBackedServiceTarget = serviceDependencies.getContent();
            Assert.assertNotNull(policyBackedServiceTarget);
            Assert.assertEquals(1, policyBackedServiceTarget.getPolicyBackedServiceOperationPolicyIds().size());
            Assert.assertNotNull(policyItem.getId(), policyBackedServiceTarget.getPolicyBackedServiceOperationPolicyIds().get(0));

            mappingsToClean = mappings;

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("policyBackedServices/" + pbsItemTarget.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/" + policyItemTarget.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    protected Goid getGoid() {
        return (Goid) configuredGOIDGenerator.generate(null, null);
    }
}
