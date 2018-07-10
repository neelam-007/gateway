package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class EncapsulatedAssertionMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionMigration.class.getName());

    private Item<PolicyMO> policyItem;
    private Item<PolicyMO> encassPolicyItem;
    private Item<EncapsulatedAssertionMO> encassItem;
    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {
        RestResponse response;

        //create encass policy
        PolicyMO encassPolicyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        encassPolicyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Src Encass Policy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        encassPolicyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(encassPolicyMO)));
        assertOkCreatedResponse(response);
        encassPolicyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        encassPolicyItem.setContent(encassPolicyMO);

        // create encass
        EncapsulatedAssertionMO encassMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encassMO.setName("Src Encass");
        encassMO.setGuid(UUID.randomUUID().toString());
        encassMO.setProperties(new HashMap<String,String>());
        encassMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, encassPolicyItem.getId()));
        response = getSourceEnvironment().processRequest("encapsulatedAssertions", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(encassMO)));
        assertOkCreatedResponse(response);
        encassItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        encassItem.setContent(encassMO);

        //create policy
        PolicyMO policyMO = ManagedObjectFactory.read(ManagedObjectFactory.write(encassPolicyMO),PolicyMO.class);
        policyMO.setId(null);
        policyMO.getPolicyDetail().setId(null);
        policyMO.getPolicyDetail().setGuid(null);
        policyMO.getPolicyDetail().setName("Src Policy");
        policyMO.getResourceSets().get(0).getResources().get(0).setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Encapsulated>\n" +
                "            <L7p:Enabled booleanValue=\"true\"/>\n" +
                "            <L7p:EncapsulatedAssertionConfigGuid stringValue=\"" + encassMO.getGuid() + "\"/>\n" +
                "            <L7p:Detail EncapsulatedAssertionConfigName=\"" + encassMO.getName() + "\"/>\n" +
                "        </L7p:Encapsulated>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");
        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        policyItem.setContent(policyMO);
    }

    @After
    public void after() throws Exception {
        if(mappingsToClean!= null)
            cleanupAll(mappingsToClean);

        RestResponse response;
        response = getSourceEnvironment().processRequest("encapsulatedAssertions/" + encassItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + encassPolicyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?encapsulatedAssertion=" + encassItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 2 items. An encass and policy", 2, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 3 items. An encass and policy and a folder", 3, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnoreEncapsulatedAssertionDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?encapsulatedAssertion=" + encassItem.getId() + "&requireEncapsulatedAssertion=" + encassItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A encapsulatedAssertion", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. A encapsulatedAssertion", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
    }

    @Test
    public void testImportNew() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, active connection and secure password", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. Root folder,a policy, active connection and secure password", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping encassPolicyMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.POLICY.toString(), encassPolicyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, encassPolicyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, encassPolicyMapping.getActionTaken());
        Assert.assertEquals(encassPolicyItem.getId(), encassPolicyMapping.getSrcId());
        Assert.assertEquals(encassPolicyMapping.getSrcId(), encassPolicyMapping.getTargetId());

        Mapping encassMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), encassMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, encassMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, encassMapping.getActionTaken());
        Assert.assertEquals(encassItem.getId(), encassMapping.getSrcId());
        Assert.assertEquals(encassMapping.getSrcId(), encassMapping.getTargetId());


        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        validate(mappings);
    }

    @Test
    public void testCircularEncassCreateNew() throws Exception {
        // update policy
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:Encapsulated>\n" +
                        "            <L7p:EncapsulatedAssertionConfigGuid stringValue=\""+ encassItem.getContent().getGuid() +"\"/>\n" +
                        "            <L7p:EncapsulatedAssertionConfigName stringValue=\""+ encassItem.getName() +"\"/>\n" +
                        "        </L7p:Encapsulated>>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";
        RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.GET,null, "");
        Item<PolicyMO> updatedPolicyMO = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        updatedPolicyMO.getContent().getResourceSets().get(0).getResources().get(0).setContent(assXml);
        response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(updatedPolicyMO.getContent()));
        assertOkResponse(response);

        response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A policy, active connection and secure password", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. Root folder,a policy, active connection and secure password", 4, bundleItem.getContent().getMappings().size());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

        Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
        Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
        Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
        Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

        Mapping encassPolicyMapping = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.POLICY.toString(), encassPolicyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, encassPolicyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, encassPolicyMapping.getActionTaken());
        Assert.assertEquals(encassPolicyItem.getId(), encassPolicyMapping.getSrcId());
        Assert.assertEquals(encassPolicyMapping.getSrcId(), encassPolicyMapping.getTargetId());

        Mapping encassMapping = mappings.getContent().getMappings().get(2);
        Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), encassMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, encassMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, encassMapping.getActionTaken());
        Assert.assertEquals(encassItem.getId(), encassMapping.getSrcId());
        Assert.assertEquals(encassMapping.getSrcId(), encassMapping.getTargetId());


        Mapping policyMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
        Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

        validate(mappings);

    }

    @Test
    public void testMapEncassPolicy() throws Exception {
        //create encass policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Target Encass Policy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
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
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> createdEncassPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncassPolicy.setContent(policyMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connection and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. Root folder,a policy, active connection and secure password", 4, bundleItem.getContent().getMappings().size());

            // map the encass policy
            bundleItem.getContent().getMappings().get(1).setTargetId(createdEncassPolicy.getId());
            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping encassPolicyMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), encassPolicyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, encassPolicyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, encassPolicyMapping.getActionTaken());
            Assert.assertEquals(encassPolicyItem.getId(), encassPolicyMapping.getSrcId());
            Assert.assertEquals(createdEncassPolicy.getId(), encassPolicyMapping.getTargetId());

            Mapping encassMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), encassMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, encassMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, encassMapping.getActionTaken());
            Assert.assertEquals(encassItem.getId(), encassMapping.getSrcId());
            Assert.assertEquals(encassMapping.getSrcId(), encassMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> dependencies = policyCreatedDependencies.getContent().getDependencies();
            Assert.assertNotNull(dependencies);
            Assert.assertEquals(2, dependencies.size());
            Assert.assertNotNull(getDependency(dependencies, createdEncassPolicy.getId()));

        }finally{

            response = getTargetEnvironment().processRequest("encapsulatedAssertions/"+ encassItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdEncassPolicy.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testMapEncassConfig() throws Exception {
        //create encass policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Target Encass Policy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
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
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> createdEncassPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncassPolicy.setContent(policyMO);

        // create encass config
        EncapsulatedAssertionMO encassMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encassMO.setName("Target Encass");
        encassMO.setGuid(UUID.randomUUID().toString());
        encassMO.setProperties(new HashMap<String,String>());
        encassMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, createdEncassPolicy.getId()));
        response = getTargetEnvironment().processRequest("encapsulatedAssertions", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(encassMO)));
        assertOkCreatedResponse(response);
        Item<EncapsulatedAssertionMO> createdEncass = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncass.setContent(encassMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connection and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. Root folder,a policy, active connection and secure password", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.Ignore);
            bundleItem.getContent().getMappings().get(2).setTargetId(createdEncass.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping encassPolicyMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), encassPolicyMapping.getType());
            Assert.assertEquals(Mapping.Action.Ignore, encassPolicyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Ignored, encassPolicyMapping.getActionTaken());
            Assert.assertEquals(encassPolicyItem.getId(), encassPolicyMapping.getSrcId());

            Mapping encassMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), encassMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, encassMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, encassMapping.getActionTaken());
            Assert.assertEquals(encassItem.getId(), encassMapping.getSrcId());
            Assert.assertEquals(createdEncass.getId(), encassMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> dependencies = policyCreatedDependencies.getContent().getDependencies();
            Assert.assertNotNull(dependencies);
            Assert.assertEquals(2, dependencies.size());
            Assert.assertNotNull(getDependency(dependencies, createdEncassPolicy.getId()));
            Assert.assertNotNull(getDependency(dependencies, createdEncass.getId()));

        }finally{

            response = getTargetEnvironment().processRequest("encapsulatedAssertions/"+ createdEncass.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdEncassPolicy.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testMapUpdateEncassConfig() throws Exception {
        //create encass policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Target Encass Policy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
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
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> createdEncassPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncassPolicy.setContent(policyMO);

        // create encass config
        EncapsulatedAssertionMO encassMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encassMO.setName("Target Encass");
        encassMO.setGuid(UUID.randomUUID().toString());
        encassMO.setProperties(new HashMap<String,String>());
        encassMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, createdEncassPolicy.getId()));
        response = getTargetEnvironment().processRequest("encapsulatedAssertions", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(encassMO)));
        assertOkCreatedResponse(response);
        Item<EncapsulatedAssertionMO> createdEncass = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncass.setContent(encassMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connection and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. Root folder,a policy, active connection and secure password", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(2).setTargetId(createdEncass.getId());
            bundleItem.getContent().getMappings().get(2).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping encassPolicyMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), encassPolicyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, encassPolicyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, encassPolicyMapping.getActionTaken());
            Assert.assertEquals(encassPolicyItem.getId(), encassPolicyMapping.getSrcId());

            Mapping encassMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), encassMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, encassMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, encassMapping.getActionTaken());
            Assert.assertEquals(encassItem.getId(), encassMapping.getSrcId());
            Assert.assertEquals(createdEncass.getId(), encassMapping.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> dependencies = policyCreatedDependencies.getContent().getDependencies();
            Assert.assertNotNull(dependencies);
            Assert.assertEquals(2, dependencies.size());
            Assert.assertNull(getDependency(dependencies, createdEncassPolicy.getId()));
            DependencyMO encassDep = getDependency(dependencies, createdEncass.getId());
            Assert.assertNotNull(encassDep);
            assertEquals(encassItem.getName(),encassDep.getName());

        }finally{

            response = getTargetEnvironment().processRequest("encapsulatedAssertions/"+ createdEncass.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdEncassPolicy.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ encassPolicyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testMapUpdateSingleCircularEncassConfig() throws Exception {
        // create circular encass

        // update source encass policy
        RestResponse response = getSourceEnvironment().processRequest("policies/"+encassPolicyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),"");
        assertOkResponse(response);
        Item<PolicyMO> srcPolicyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        PolicyMO policyMO = srcPolicyItem.getContent();
        policyMO.getResourceSets().get(0).getResources().get(0).setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Encapsulated>\n" +
                "            <L7p:EncapsulatedAssertionConfigGuid stringValue=\""+ encassItem.getContent().getGuid() +"\"/>\n" +
                "            <L7p:EncapsulatedAssertionConfigName stringValue=\""+ encassItem.getName() +"\"/>\n" +
                "        </L7p:Encapsulated>>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");
        response = getSourceEnvironment().processRequest("policies/"+encassPolicyItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkResponse(response);
        encassPolicyItem.setContent(policyMO);

        // create target encass1
        //create encass policy
        policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Target Encass Policy 1");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
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
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> createdTargetEncassPolicy1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdTargetEncassPolicy1.setContent(policyMO);

        // create encass config
        EncapsulatedAssertionMO encassMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encassMO.setName("Target Encass 1");
        encassMO.setGuid(UUID.randomUUID().toString());
        encassMO.setProperties(new HashMap<String,String>());
        encassMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, createdTargetEncassPolicy1.getId()));
        response = getTargetEnvironment().processRequest("encapsulatedAssertions", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(encassMO)));
        assertOkCreatedResponse(response);
        Item<EncapsulatedAssertionMO> createdTargetEncass1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdTargetEncass1.setContent(encassMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 5 items. 3 policy, 2 encass configs", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 6 mappings. Root folder, 3 policy, 2 encass configs", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(1).setTargetId(createdTargetEncassPolicy1.getId());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(2).setTargetId(createdTargetEncass1.getId());
            bundleItem.getContent().getMappings().get(2).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping encassPolicyMapping2 = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), encassPolicyMapping2.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, encassPolicyMapping2.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, encassPolicyMapping2.getActionTaken());
            Assert.assertEquals(encassPolicyItem.getId(), encassPolicyMapping2.getSrcId());
            Assert.assertEquals(createdTargetEncassPolicy1.getId(), encassPolicyMapping2.getTargetId());

            Mapping encassMapping1 = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), encassMapping1.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, encassMapping1.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, encassMapping1.getActionTaken());
            Assert.assertEquals(encassItem.getId(), encassMapping1.getSrcId());
            Assert.assertEquals(createdTargetEncass1.getId(), encassMapping1.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> dependencies = policyCreatedDependencies.getContent().getDependencies();
            Assert.assertNotNull(dependencies);
            Assert.assertEquals(2, dependencies.size());

            response = getTargetEnvironment().processRequest("policies/"+encassPolicyMapping2.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> encassPolicyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> encassPolicyDependencies = encassPolicyCreatedDependencies.getContent().getDependencies();
            Assert.assertNotNull(encassPolicyDependencies);
            Assert.assertEquals(2, encassPolicyDependencies.size());

        }finally{

            response = getTargetEnvironment().processRequest("encapsulatedAssertions/"+ createdTargetEncass1.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdTargetEncassPolicy1.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testMapUpdateEncassConfigNotActivate() throws Exception {

        // create target encass1
        //create encass policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Target Encass Policy 1");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
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
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> createdTargetEncassPolicy1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdTargetEncassPolicy1.setContent(policyMO);

        // create encass config
        EncapsulatedAssertionMO encassMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encassMO.setName("Target Encass 1");
        encassMO.setGuid(UUID.randomUUID().toString());
        encassMO.setProperties(new HashMap<String,String>());
        encassMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, createdTargetEncassPolicy1.getId()));
        response = getTargetEnvironment().processRequest("encapsulatedAssertions", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(encassMO)));
        assertOkCreatedResponse(response);
        Item<EncapsulatedAssertionMO> createdTargetEncass1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdTargetEncass1.setContent(encassMO);

        //create target policy
        policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Target Policy 1");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> createdTargetPolicy1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdTargetEncassPolicy1.setContent(policyMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 5 items. 3 policy, 2 encass configs", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 6 mappings. Root folder, 3 policy, 2 encass configs", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(1).setTargetId(createdTargetEncassPolicy1.getId());
            bundleItem.getContent().getMappings().get(1).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(2).setTargetId(createdTargetEncass1.getId());
            bundleItem.getContent().getMappings().get(2).setAction(Mapping.Action.NewOrUpdate);
            bundleItem.getContent().getMappings().get(3).setTargetId(createdTargetPolicy1.getId());
            bundleItem.getContent().getMappings().get(3).setAction(Mapping.Action.NewOrUpdate);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle?activate=false", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping encassPolicyMapping2 = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), encassPolicyMapping2.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, encassPolicyMapping2.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, encassPolicyMapping2.getActionTaken());
            Assert.assertEquals(encassPolicyItem.getId(), encassPolicyMapping2.getSrcId());
            Assert.assertEquals(createdTargetEncassPolicy1.getId(), encassPolicyMapping2.getTargetId());

            Mapping encassMapping1 = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), encassMapping1.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, encassMapping1.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, encassMapping1.getActionTaken());
            Assert.assertEquals(encassItem.getId(), encassMapping1.getSrcId());
            Assert.assertEquals(createdTargetEncass1.getId(), encassMapping1.getTargetId());

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrUpdate, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(createdTargetPolicy1.getId(), policyMapping.getTargetId());

            validate(mappings);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> dependencies = policyCreatedDependencies.getContent().getDependencies();
            Assert.assertNotNull(dependencies);
            Assert.assertEquals(0, dependencies.size());

            // activate latest policy
            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/versions", HttpMethod.GET, null, "");
            assertOkResponse(response);
            ItemsList<PolicyVersionMO> policyVersions = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(2, policyVersions.getContent().size());

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/versions/2/activate/", HttpMethod.POST, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<DependencyListMO> policyLatestDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertNotNull(policyLatestDependencies.getContent().getDependencies());
            Assert.assertEquals(2, policyLatestDependencies.getContent().getDependencies().size());


        }finally{

            response = getTargetEnvironment().processRequest("encapsulatedAssertions/"+ createdTargetEncass1.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdTargetEncassPolicy1.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdTargetPolicy1.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            mappingsToClean = null;
        }
    }

    @Test
    public void testAlwaysCreateNewGuidConflict() throws Exception {
        //create encass policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Target Encass Policy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
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
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> createdEncassPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncassPolicy.setContent(policyMO);

        // create encass config with same id and guid
        EncapsulatedAssertionMO encassMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encassMO.setName("Target Encass");
        encassMO.setGuid(encassItem.getContent().getGuid());
        encassMO.setProperties(new HashMap<String,String>());
        encassMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, createdEncassPolicy.getId()));
        response = getTargetEnvironment().processRequest("encapsulatedAssertions/" + encassItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(encassMO)));
        assertOkCreatedResponse(response);
        Item<EncapsulatedAssertionMO> createdEncass = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncass.setContent(encassMO);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + policyItem.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 items. A policy, active connection and secure password", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. Root folder,a policy, active connection and secure password", 4, bundleItem.getContent().getMappings().size());

            // map
            bundleItem.getContent().getMappings().get(2).setAction(Mapping.Action.AlwaysCreateNew);

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());

            Mapping rootFolderMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, rootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());
            Assert.assertEquals(rootFolderMapping.getSrcId(), rootFolderMapping.getTargetId());

            Mapping encassPolicyMapping = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.POLICY.toString(), encassPolicyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, encassPolicyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, encassPolicyMapping.getActionTaken());
            Assert.assertEquals(encassPolicyItem.getId(), encassPolicyMapping.getSrcId());

            Mapping encassMapping = mappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), encassMapping.getType());
            Assert.assertEquals(Mapping.Action.AlwaysCreateNew, encassMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, encassMapping.getActionTaken());
            Assert.assertEquals(encassItem.getId(), encassMapping.getSrcId());
            Assert.assertFalse(createdEncass.getId().equals(encassMapping.getTargetId()));

            Mapping policyMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping.getActionTaken());
            Assert.assertEquals(policyItem.getId(), policyMapping.getSrcId());
            Assert.assertEquals(policyMapping.getSrcId(), policyMapping.getTargetId());

            validate(mappings);

            response = getTargetEnvironment().processRequest("policies/"+policyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> dependencies = policyCreatedDependencies.getContent().getDependencies();
            Assert.assertNotNull(dependencies);
            Assert.assertEquals(2, dependencies.size());
            Assert.assertNotNull(getDependency(dependencies, encassPolicyItem.getId()));
            Assert.assertNotNull(getDependency(dependencies, encassMapping.getTargetId()));

            mappingsToClean = mappings;

        }finally{

            response = getTargetEnvironment().processRequest("encapsulatedAssertions/"+ createdEncass.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdEncassPolicy.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @BugId("SSG-10399")
    @Test
    public void testBrokenEncapsulatedPolicyReferenceExportImport() throws Exception {

        //create encass policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Encass Policy 1");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
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
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> createdEncassPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncassPolicy.setContent(policyMO);

        // create encass config
        EncapsulatedAssertionMO encassMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encassMO.setName("Encass 1");
        encassMO.setGuid(UUID.randomUUID().toString());
        encassMO.setProperties(new HashMap<String,String>());
        encassMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, createdEncassPolicy.getId()));
        response = getSourceEnvironment().processRequest("encapsulatedAssertions/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(encassMO)));
        assertOkCreatedResponse(response);
        Item<EncapsulatedAssertionMO> createdEncass = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncass.setContent(encassMO);

        //create policy to use encass
        policyMO = ManagedObjectFactory.createPolicy();
        policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Policy 2");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        resourceSet = ManagedObjectFactory.createResourceSet();
        policyMO.setResourceSets(Arrays.asList(resourceSet));
        resourceSet.setTag("policy");
        resource = ManagedObjectFactory.createResource();
        resourceSet.setResources(Arrays.asList(resource));
        resource.setType("policy");
        resource.setContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Encapsulated>\n" +
                "            <L7p:Enabled booleanValue=\"true\"/>\n" +
                "            <L7p:EncapsulatedAssertionConfigGuid stringValue=\"" + encassMO.getGuid() + "\"/>\n" +
                "            <L7p:Detail EncapsulatedAssertionConfigName=\"" + encassMO.getName() + "\"/>\n" +
                "        </L7p:Encapsulated>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO>  createdPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdPolicy.setContent(policyMO);

        // delete encass
        response = getSourceEnvironment().processRequest("encapsulatedAssertions/" + createdEncass.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
        assertOkEmptyResponse(response);
        // delete backing policy
        response = getSourceEnvironment().processRequest("policies/" + createdEncassPolicy.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(),"");
        assertOkEmptyResponse(response);

        try{
            response = getSourceEnvironment().processRequest("bundle/policy/" + createdPolicy.getId(), HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 1 items. A policy", 1, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 3 mappings. Root folder,a policy,an encass", 3, bundleItem.getContent().getMappings().size());

            List<Item> references = bundleItem.getContent().getReferences();
            Item policyReference = references.get(0);
            Assert.assertEquals(EntityType.POLICY.toString(), policyReference.getType());
            Assert.assertEquals(createdPolicy.getId(), policyReference.getId());

            List<Mapping> mappings = bundleItem.getContent().getMappings();

            Mapping encassMapping = mappings.get(0);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), encassMapping.getType());
            Assert.assertEquals(Mapping.Action.Ignore, encassMapping.getAction());
            Assert.assertNull( encassMapping.getSrcId());
            Assert.assertNull( encassMapping.getSrcUri());
            Assert.assertEquals("guid", encassMapping.getProperty("MapBy"));
            Assert.assertEquals(encassMO.getGuid(), encassMapping.getProperty("MapTo"));

            Mapping rootFolderMapping = mappings.get(1);
            Assert.assertEquals(EntityType.FOLDER.toString(), rootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, rootFolderMapping.getAction());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), rootFolderMapping.getSrcId());

            Mapping encassPolicyMapping = mappings.get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), encassPolicyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, encassPolicyMapping.getAction());
            Assert.assertEquals(createdPolicy.getId(), encassPolicyMapping.getSrcId());

            //Import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> targetMappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = targetMappings;

            //verify the mappings
            Assert.assertEquals("There should be 3 mappings after the import", 3, targetMappings.getContent().getMappings().size());

            Mapping targetEncassMapping = targetMappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), targetEncassMapping.getType());
            Assert.assertEquals(Mapping.Action.Ignore, targetEncassMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Ignored, targetEncassMapping.getActionTaken());
            Assert.assertNull( targetEncassMapping.getSrcId());
            Assert.assertNull( targetEncassMapping.getSrcUri());
            Assert.assertEquals("guid", targetEncassMapping.getProperty("MapBy"));
            Assert.assertEquals(encassMO.getGuid(), targetEncassMapping.getProperty("MapTo"));

            Mapping targetRootFolderMapping = targetMappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.FOLDER.toString(), targetRootFolderMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, targetRootFolderMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.UsedExisting, targetRootFolderMapping.getActionTaken());
            Assert.assertEquals(Folder.ROOT_FOLDER_ID.toString(), targetRootFolderMapping.getSrcId());
            Assert.assertEquals(targetRootFolderMapping.getSrcId(), targetRootFolderMapping.getTargetId());

            Mapping targetPolicyMapping = targetMappings.getContent().getMappings().get(2);
            Assert.assertEquals(EntityType.POLICY.toString(), targetPolicyMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, targetPolicyMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, targetPolicyMapping.getActionTaken());
            Assert.assertEquals(createdPolicy.getId(), targetPolicyMapping.getSrcId());
            Assert.assertEquals(targetPolicyMapping.getSrcId(), targetPolicyMapping.getTargetId());

            validate(targetMappings);

            response = getTargetEnvironment().processRequest("policies/"+targetPolicyMapping.getTargetId() + "/dependencies", HttpMethod.GET, null, "");
            assertOkResponse(response);

            Item<DependencyListMO> policyCreatedDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> dependencies = policyCreatedDependencies.getContent().getDependencies();
            Assert.assertNotNull(dependencies);
            Assert.assertEquals(0, dependencies.size());

        }finally{

            // clean up
            response = getSourceEnvironment().processRequest("policies/"+ createdPolicy.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    @Test
    public void deleteMappingTest() throws Exception {
//create encass policy
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Target Encass Policy");
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
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
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"HI\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n");

        RestResponse response = getTargetEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        Item<PolicyMO> createdEncassPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncassPolicy.setContent(policyMO);

        // create encass config
        EncapsulatedAssertionMO encassMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encassMO.setName("Target Encass");
        encassMO.setGuid(UUID.randomUUID().toString());
        encassMO.setProperties(new HashMap<String,String>());
        encassMO.setPolicyReference(new ManagedObjectReference(PolicyMO.class, createdEncassPolicy.getId()));
        response = getTargetEnvironment().processRequest("encapsulatedAssertions", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(encassMO)));
        assertOkCreatedResponse(response);
        Item<EncapsulatedAssertionMO> createdEncass = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        createdEncass.setContent(encassMO);

        try {
            Bundle bundle = ManagedObjectFactory.createBundle();

            Mapping mapping = ManagedObjectFactory.createMapping();
            mapping.setAction(Mapping.Action.Delete);
            mapping.setTargetId(createdEncass.getId());
            mapping.setSrcId(Goid.DEFAULT_GOID.toString());
            mapping.setType(createdEncass.getType());

            Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
            mappingNotExisting.setAction(Mapping.Action.Delete);
            mappingNotExisting.setSrcId(Goid.DEFAULT_GOID.toString());
            mappingNotExisting.setType(createdEncass.getType());

            bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));
            bundle.setReferences(Arrays.<Item>asList(createdEncass));

            //import the bundle
            logger.log(Level.INFO, objectToString(bundle));
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundle));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            mappingsToClean = mappings;

            //verify the mappings
            Assert.assertEquals("There should be 2 mapping after the import", 2, mappings.getContent().getMappings().size());
            Mapping activeConnectorMapping = mappings.getContent().getMappings().get(0);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), activeConnectorMapping.getType());
            Assert.assertEquals(Mapping.Action.Delete, activeConnectorMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Deleted, activeConnectorMapping.getActionTaken());
            Assert.assertEquals(createdEncass.getId(), activeConnectorMapping.getTargetId());

            Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
            Assert.assertEquals(EntityType.ENCAPSULATED_ASSERTION.toString(), activeConnectorMappingNotExisting.getType());
            Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
            Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
            Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

            response = getTargetEnvironment().processRequest("encapsulatedAssertions/" + createdEncass.getId(), HttpMethod.GET, null, "");
            assertNotFoundResponse(response);
        } finally {
            // clean up
            response = getTargetEnvironment().processRequest("policies/"+ createdEncassPolicy.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }
}
