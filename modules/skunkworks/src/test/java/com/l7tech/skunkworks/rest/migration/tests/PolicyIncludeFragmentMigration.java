package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class PolicyIncludeFragmentMigration extends MigrationTestBase {
    private static final Logger logger = Logger.getLogger(PolicyIncludeFragmentMigration.class.getName());

    private Item<ServiceMO> serviceItem;
    private Item<PolicyMO> policyItem1;
    private Item<PolicyMO> policyItem2;

    private Item<Mappings> mappingsToClean;

    @Before
    public void before() throws Exception {
        //create policy 1
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("PolicyFragment1");
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
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        resource.setContent(policyXml);

        RestResponse response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        policyItem1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = getSourceEnvironment().processRequest("policies/" + policyItem1.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkResponse(response);

        policyItem1 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create policy 2
        PolicyMO policyMO2 = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail2 = ManagedObjectFactory.createPolicyDetail();
        policyMO2.setPolicyDetail(policyDetail2);
        policyDetail2.setName("PolicyFragment2");
        policyDetail2.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail2.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail2.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet resourceSet2 = ManagedObjectFactory.createResourceSet();
        policyMO2.setResourceSets(Arrays.asList(resourceSet2));
        resourceSet2.setTag("policy");
        Resource resource2 = ManagedObjectFactory.createResource();
        resourceSet2.setResources(Arrays.asList(resource2));
        resource2.setType("policy");
        final String policyXml2 =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        resource2.setContent(policyXml2);

        response = getSourceEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO2)));

        assertOkCreatedResponse(response);

        policyItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = getSourceEnvironment().processRequest("policies/" + policyItem2.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkResponse(response);

        policyItem2 = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create service
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:Include>\n" +
                        "            <L7p:PolicyGuid stringValue=\"" + policyItem1.getContent().getGuid() + "\"/>\n" +
                        "        </L7p:Include>\n" +
                        "        <L7p:Include>\n" +
                        "            <L7p:PolicyGuid stringValue=\"" + policyItem2.getContent().getGuid() + "\"/>\n" +
                        "        </L7p:Include>\n" +
                        "    </wsp:All>" +
                        "</wsp:Policy>";

        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("Source Service");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        ServiceDetail.HttpMapping serviceMapping = ManagedObjectFactory.createHttpMapping();
        serviceMapping.setUrlPattern("/srcService");
        serviceMapping.setVerbs(Arrays.asList("GET"));
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent(assXml);
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet));

        response = getSourceEnvironment().processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);
    }

    @After
    public void after() throws Exception {

        if (mappingsToClean != null)
            cleanupAll(mappingsToClean);

        RestResponse response;


        response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + policyItem1.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);

        response = getSourceEnvironment().processRequest("policies/" + policyItem2.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @BugId("SSG-8584")
    @Test
    public void testImportNewServiceWithTwoPolicyIncludes() throws Exception {

        RestResponse response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 item. A service, and two policies", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals("The bundle should have 4 mappings. A folder, a service, and two policies", 4, bundleItem.getContent().getMappings().size());

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

        Mapping policyMapping1 = mappings.getContent().getMappings().get(1);
        Mapping policyMapping2 = mappings.getContent().getMappings().get(2);
        if(policyMapping1.getSrcId().equals(policyItem1.getId())) {
            compareMappingPolicy(policyMapping1, policyItem1);
            compareMappingPolicy(policyMapping2, policyItem2);
        } else {
            compareMappingPolicy(policyMapping1, policyItem2);
            compareMappingPolicy(policyMapping2, policyItem1);
        }

        Mapping serviceMapping = mappings.getContent().getMappings().get(3);
        Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
        Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
        Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

        //verify service
        response = getTargetEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.GET, null, "");
        Item<ServiceMO> targetService = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(1, targetService.getContent().getResourceSets().size());
        //service xml is equal ignoring whitespace.
        Assert.assertEquals(serviceItem.getContent().getResourceSets().get(0).getResources().get(0).getContent().replaceAll("\\s+", "").replaceAll("\\n+", ""), targetService.getContent().getResourceSets().get(0).getResources().get(0).getContent().replaceAll("\\s+", "").replaceAll("\\n+", ""));

        mappingsToClean = mappings;

        validate(mappings);
    }

    @BugId("SSG-9047")
    @Test
    public void testImportNewServiceWithPolicyIncludeAlwaysCreateNewMatchingGuid() throws Exception {
        //create policy with same id an guid on target
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyMO.setGuid(policyItem1.getContent().getGuid());
        policyDetail.setName("PolicyFragment1target");
        policyDetail.setGuid(policyItem1.getContent().getGuid());
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
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        resource.setContent(policyXml);

        RestResponse response = getTargetEnvironment().processRequest("policies/"+policyItem1.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        try {

            response = getSourceEnvironment().processRequest("bundle/service/" + serviceItem.getId(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Assert.assertEquals("The bundle should have 3 item. A service, and two policies", 3, bundleItem.getContent().getReferences().size());
            Assert.assertEquals("The bundle should have 4 mappings. A folder, a service, and two policies", 4, bundleItem.getContent().getMappings().size());

            getMapping(bundleItem.getContent().getMappings(), policyItem1.getId()).setAction(Mapping.Action.AlwaysCreateNew);

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


            Mapping policyMapping1 = getMapping(mappings.getContent().getMappings(), policyItem1.getId());
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping1.getType());
            Assert.assertEquals(Mapping.Action.AlwaysCreateNew, policyMapping1.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping1.getActionTaken());
            Assert.assertEquals(policyItem1.getId(), policyMapping1.getSrcId());
            Assert.assertFalse(policyMapping1.getSrcId().equals(policyMapping1.getTargetId()));

            Mapping policyMapping2 = getMapping(mappings.getContent().getMappings(), policyItem2.getId());
            Assert.assertEquals(EntityType.POLICY.toString(), policyMapping2.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping2.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping2.getActionTaken());
            Assert.assertEquals(policyItem2.getId(), policyMapping2.getSrcId());
            Assert.assertEquals(policyMapping2.getSrcId(), policyMapping2.getTargetId());

            Mapping serviceMapping = mappings.getContent().getMappings().get(3);
            Assert.assertEquals(EntityType.SERVICE.toString(), serviceMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, serviceMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, serviceMapping.getActionTaken());
            Assert.assertEquals(serviceItem.getId(), serviceMapping.getSrcId());
            Assert.assertEquals(serviceMapping.getSrcId(), serviceMapping.getTargetId());

            //verify service
            response = getTargetEnvironment().processRequest("services/" + serviceItem.getId() + "/dependencies", HttpMethod.GET, null, "");
            Item<DependencyListMO> ServiceDependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            List<DependencyMO> dependencies = ServiceDependencies.getContent().getDependencies();
            Assert.assertNotNull(dependencies);
            Assert.assertEquals(2, dependencies.size());
            Assert.assertNotNull(getDependency(dependencies, policyItem2.getId()));
            Assert.assertNotNull(getDependency(dependencies, policyMapping1.getTargetId()));
            mappingsToClean = mappings;

            validate(mappings);
        } finally {
            response = getTargetEnvironment().processRequest("policies/" + policyItem1.getId(), HttpMethod.DELETE, null, "");
            assertOkEmptyResponse(response);
        }
    }

    private static void compareMappingPolicy(Mapping policyMapping1, Item<PolicyMO> policyItem) {
        Assert.assertEquals(EntityType.POLICY.toString(), policyMapping1.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, policyMapping1.getAction());
        Assert.assertEquals(Mapping.ActionTaken.CreatedNew, policyMapping1.getActionTaken());
        Assert.assertEquals(policyItem.getId(), policyMapping1.getSrcId());
        Assert.assertEquals(policyMapping1.getSrcId(), policyMapping1.getTargetId());
    }
}
