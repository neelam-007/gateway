package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.RestResponse;
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
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
* This will test migration using the rest api from one gateway to another.
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class EncapsulatedAssertionMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionMigrationTest.class.getName());

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
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + encassPolicyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkDeleteResponse(response);
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
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdEncassPolicy.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

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
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdEncassPolicy.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

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
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ createdEncassPolicy.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ encassPolicyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            response = getTargetEnvironment().processRequest("policies/"+ policyItem.getId(), HttpMethod.DELETE, null, "");
            assertOkDeleteResponse(response);

            mappingsToClean = null;
        }
    }
}
