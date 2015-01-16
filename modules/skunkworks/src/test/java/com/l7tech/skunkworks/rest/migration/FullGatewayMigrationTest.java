package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.JVMDatabaseBasedRestManagementEnvironment;
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
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class FullGatewayMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(FullGatewayMigrationTest.class.getName());

    private Item<IdentityProviderMO> internalIDProviderItem;

    @Before
    public void before() throws Exception {
        // get internal provider item
        RestResponse response = getSourceEnvironment().processRequest("identityProviders/" + IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        internalIDProviderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

    }

    @After
    public void after() throws Exception {
    }

    /**
     * Test that an empty gateway can be exported and imported without needing to update the bundle
     */
    @Test
    public void testEmptyFullGatewayExportImport() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);
    }

    /**
     * Test that an empty gateway can be exported and imported without needing to update the bundle
     */
    @Test
    public void testEmptyFullGatewayExportImportNewOrUpdate() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true&defaultAction=NewOrUpdate", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);
    }

    /**
     * Test migrating a full gateway with one created policy. Test that auto generated roles get created correctly.
     */
    @Test
    public void testFullGatewayExportImportOnePolicy() throws Exception {
        Item<PolicyMO> policyItem = null;
        try {
            policyItem = createPolicy(getSourceEnvironment());
            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //find the policy auto generated role mappings
            final Item<PolicyMO> finalPolicyItem = policyItem;
            List<Mapping> policyRoleMappings = Functions.grep(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.RBAC_ROLE.name().equals(mapping.getType()) && ((RbacRoleMO) getBundleReference(bundleItem.getContent(), mapping.getSrcId())).getName().contains(finalPolicyItem.getId());
                }
            });

            Assert.assertFalse(policyRoleMappings.isEmpty());
            for (Mapping mapping : policyRoleMappings) {
                //assert that the source and the target id are not the same on the auto generated mapping to validate that it got mapped to the auto generated mapping on the target gateway
                Assert.assertNotSame(mapping.getSrcId(), mapping.getTargetId());
                RbacRoleMO sourceRole = getRole(getSourceEnvironment(), mapping.getSrcId());
                RbacRoleMO targetRole = getRole(getTargetEnvironment(), mapping.getTargetId());
                Assert.assertEquals(sourceRole.getName(), targetRole.getName());
            }

        } finally {
            if (policyItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
            }
        }

    }

    /**
     * Test migrating a full gateway with one created policy. Map to a policy with a different name and id on the target gateway. Test that the roles get created correctly
     */
    @Test
    public void testFullGatewayExportImportOnePolicyMapToDifferentPolicyName() throws Exception {
        Item<PolicyMO> policyItem = null;
        try {
            policyItem = createPolicy(getSourceEnvironment());
            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            getMapping(bundleItem.getContent().getMappings(), policyItem.getId()).addProperty("MapBy", "name");
            getMapping(bundleItem.getContent().getMappings(), policyItem.getId()).addProperty("MapTo", "My Target Policy");

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //find the policy auto generated role mappings
            final Item<PolicyMO> finalPolicyItem = policyItem;
            List<Mapping> policyRoleMappings = Functions.grep(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.RBAC_ROLE.name().equals(mapping.getType()) && ((RbacRoleMO) getBundleReference(bundleItem.getContent(), mapping.getSrcId())).getName().contains(finalPolicyItem.getId());
                }
            });

            Assert.assertFalse(policyRoleMappings.isEmpty());
            for (Mapping mapping : policyRoleMappings) {
                //assert that the source and the target id are not the same on the auto generated mapping to validate that it got mapped to the auto generated mapping on the target gateway
                Assert.assertNotSame(mapping.getSrcId(), mapping.getTargetId());
                RbacRoleMO sourceRole = getRole(getSourceEnvironment(), mapping.getSrcId());
                RbacRoleMO targetRole = getRole(getTargetEnvironment(), mapping.getTargetId());
                Assert.assertEquals(sourceRole.getEntityID(), targetRole.getEntityID());
                Assert.assertNotSame(sourceRole.getName(), targetRole.getName());
            }

        } finally {
            if (policyItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
            }
        }
    }

    /**
     * This will create a policy on the source gateway, which generated an auto generated role for the policy. A user
     * is then assigned to that role.
     * That will be migrated. (with the user mapped to another user on the target gateway.)
     *
     * We validate that the user is correctly mapped in the role on the target gateway
     *
     * @throws Exception
     */
    @Test
    public void testFullGatewayExportImportOnePolicyMapAssignmentOnAutoGeneratedRole() throws Exception {
        Item<PolicyMO> policyItem = null;
        Item<UserMO> userItem = null;
        try {
            policyItem = createPolicy(getSourceEnvironment());
            userItem = createUser(getSourceEnvironment(), "srcUser", internalIDProviderItem.getId());

            final Item<PolicyMO> finalPolicyItem = policyItem;
            //get the bundle to find the auto generated role for the policy
            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItemBeforeRoleUpdate = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Mapping policyRoleMapping = Functions.grepFirst(bundleItemBeforeRoleUpdate.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.RBAC_ROLE.name().equals(mapping.getType()) && ((RbacRoleMO) getBundleReference(bundleItemBeforeRoleUpdate.getContent(), mapping.getSrcId())).getName().contains(finalPolicyItem.getId());
                }
            });

            //assign a user to the policy's auto generated role
            RbacRoleMO roleMO = getBundleReference(bundleItemBeforeRoleUpdate.getContent(), policyRoleMapping.getSrcId());
            RbacRoleAssignmentMO rbacRoleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
            rbacRoleAssignmentMO.setEntityType("User");
            rbacRoleAssignmentMO.setIdentityId(userItem.getId());
            rbacRoleAssignmentMO.setProviderId(userItem.getContent().getProviderId());
            AddAssignmentsContext addAssignmentsContext = new AddAssignmentsContext();
            addAssignmentsContext.setAssignments(Arrays.asList(rbacRoleAssignmentMO));
            response = getSourceEnvironment().processRequest("roles/" + roleMO.getId() + "/assignments", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(addAssignmentsContext));
            logger.log(Level.INFO, response.toString());
            assertOkEmptyResponse(response);

            //get the bundle again with the updated role assignment
            response = getSourceEnvironment().processRequest("bundle", "all=true&defaultAction=NewOrUpdate", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);
            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            getMapping(bundleItem.getContent().getMappings(), userItem.getId()).setTargetId("00000000000000000000000000000003");

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //get the policy role mappings
            List<Mapping> policyRoleMappings = Functions.grep(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.RBAC_ROLE.name().equals(mapping.getType()) && ((RbacRoleMO) getBundleReference(bundleItem.getContent(), mapping.getSrcId())).getName().contains(finalPolicyItem.getId());
                }
            });

            Assert.assertFalse(policyRoleMappings.isEmpty());
            for (Mapping mapping : policyRoleMappings) {
                //assert that the source and the target id are not the same on the auto generated mapping to validate that it got mapped to the auto generated mapping on the target gateway
                Assert.assertNotSame(mapping.getSrcId(), mapping.getTargetId());
                RbacRoleMO sourceRole = getRole(getSourceEnvironment(), mapping.getSrcId());
                RbacRoleMO targetRole = getRole(getTargetEnvironment(), mapping.getTargetId());
                Assert.assertEquals(sourceRole.getName(), targetRole.getName());
            }

            Mapping mappedRole = getMapping(mappings.getContent().getMappings(), policyRoleMapping.getSrcId());
            // get the auto generated role for the policy from the target gateway
            response = getTargetEnvironment().processRequest("roles/" + mappedRole.getTargetId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
            assertOkResponse(response);

            // validate that the user got mapped correctly.
            Item<RbacRoleMO> targetRole = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Assert.assertEquals(1, targetRole.getContent().getAssignments().size());
            Assert.assertEquals("00000000000000000000000000000003", targetRole.getContent().getAssignments().get(0).getIdentityId());


        } finally {
            if (policyItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("policies/" + policyItem.getId(), HttpMethod.DELETE, null, "");
            }
            if (userItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("identityProviders/" + internalIDProviderItem.getId() + "/users/" + userItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
            }
        }
    }

    /**
     * Test migrating a full gateway with one created id provider. Map the id provider and make sure the auto generated
     * roles got mapped.
     */
    @Test
    public void testFullGatewayExportImportOneIDProvider() throws Exception {
        Item<IdentityProviderMO> idProviderItemSrc = null;
        Item<IdentityProviderMO> idProviderItemTrgt = null;
        try {
            idProviderItemSrc = createIdProviderLdap(getSourceEnvironment());
            idProviderItemTrgt = createIdProviderLdap(getTargetEnvironment());

            //get the bundle on the target ssg. This will create the default ssl key (when search for dependencies on the id provider)
            getTargetEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");

            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //map the id provider
            getMapping(bundleItem.getContent().getMappings(), idProviderItemSrc.getId()).setTargetId(idProviderItemTrgt.getId());

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //find the id provider auto generated role mappings
            final Item<IdentityProviderMO> finalIdProviderItemSrc = idProviderItemSrc;
            List<Mapping> idProviderRoleMappings = Functions.grep(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.RBAC_ROLE.name().equals(mapping.getType()) && ((RbacRoleMO) getBundleReference(bundleItem.getContent(), mapping.getSrcId())).getName().contains(finalIdProviderItemSrc.getId());
                }
            });

            Assert.assertFalse(idProviderRoleMappings.isEmpty());
            for (Mapping mapping : idProviderRoleMappings) {
                //assert that the source and the target id are not the same on the auto generated mapping to validate that it got mapped to the auto generated mapping on the target gateway
                Assert.assertNotSame(mapping.getSrcId(), mapping.getTargetId());
                RbacRoleMO sourceRole = getRole(getSourceEnvironment(), mapping.getSrcId());
                RbacRoleMO targetRole = getRole(getTargetEnvironment(), mapping.getTargetId());
                Assert.assertNotSame(sourceRole.getName(), targetRole.getName());
            }

        } finally {
            if (idProviderItemSrc != null) {
                RestResponse response = getSourceEnvironment().processRequest("identityProviders/" + idProviderItemSrc.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
            }
            if (idProviderItemTrgt != null) {
                RestResponse response = getTargetEnvironment().processRequest("identityProviders/" + idProviderItemTrgt.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
            }
        }

    }

    /**
     * Test migrating a full gateway with one created folder. Test that auto generated roles get created correctly.
     */
    @Test
    public void testFullGatewayExportImportOneFolder() throws Exception {
        Item<FolderMO> folderItem = null;
        try {
            folderItem = createFolder(getSourceEnvironment());
            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //find the folder auto generated role mappings
            final Item<FolderMO> finalFolderItem = folderItem;
            List<Mapping> folderRoleMappings = Functions.grep(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.RBAC_ROLE.name().equals(mapping.getType()) && ((RbacRoleMO) getBundleReference(bundleItem.getContent(), mapping.getSrcId())).getName().contains(finalFolderItem.getId());
                }
            });

            Assert.assertFalse(folderRoleMappings.isEmpty());
            for (Mapping mapping : folderRoleMappings) {
                //assert that the source and the target id are not the same on the auto generated mapping to validate that it got mapped to the auto generated mapping on the target gateway
                Assert.assertNotSame(mapping.getSrcId(), mapping.getTargetId());
                RbacRoleMO sourceRole = getRole(getSourceEnvironment(), mapping.getSrcId());
                RbacRoleMO targetRole = getRole(getTargetEnvironment(), mapping.getTargetId());
                Assert.assertEquals(sourceRole.getName(), targetRole.getName());
            }

        } finally {
            if (folderItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("folders/" + folderItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("folders/" + folderItem.getId(), HttpMethod.DELETE, null, "");
            }
        }
    }

    /**
     * Test migrating a full gateway with one created securityZone. Test that auto generated roles get created correctly.
     */
    @Test
    public void testFullGatewayExportImportOneSecurityZone() throws Exception {
        Item<SecurityZoneMO> securityZoneItem = null;
        try {
            securityZoneItem = createSecurityZone(getSourceEnvironment());
            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //find the security zone auto generated role mappings
            final Item<SecurityZoneMO> finalSecurityZoneItem = securityZoneItem;
            List<Mapping> folderRoleMappings = Functions.grep(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.RBAC_ROLE.name().equals(mapping.getType()) && ((RbacRoleMO) getBundleReference(bundleItem.getContent(), mapping.getSrcId())).getName().contains(finalSecurityZoneItem.getId());
                }
            });

            Assert.assertFalse(folderRoleMappings.isEmpty());
            for (Mapping mapping : folderRoleMappings) {
                //assert that the source and the target id are not the same on the auto generated mapping to validate that it got mapped to the auto generated mapping on the target gateway
                Assert.assertNotSame(mapping.getSrcId(), mapping.getTargetId());
                RbacRoleMO sourceRole = getRole(getSourceEnvironment(), mapping.getSrcId());
                RbacRoleMO targetRole = getRole(getTargetEnvironment(), mapping.getTargetId());
                Assert.assertEquals(sourceRole.getName(), targetRole.getName());
            }

        } finally {
            if (securityZoneItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("securityZones/" + securityZoneItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("securityZones/" + securityZoneItem.getId(), HttpMethod.DELETE, null, "");
            }
        }
    }

    /**
     * Test migrating a full gateway with one created service. Test that auto generated roles get created correctly.
     */
    @Test
    public void testFullGatewayExportImportOneService() throws Exception {
        Item<ServiceMO> serviceItem = null;
        try {
            serviceItem = createService(getSourceEnvironment());
            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //find the service auto generated role mappings
            final Item<ServiceMO> finalServiceItem = serviceItem;
            List<Mapping> folderRoleMappings = Functions.grep(mappings.getContent().getMappings(), new Functions.Unary<Boolean, Mapping>() {
                @Override
                public Boolean call(Mapping mapping) {
                    return EntityType.RBAC_ROLE.name().equals(mapping.getType()) && ((RbacRoleMO) getBundleReference(bundleItem.getContent(), mapping.getSrcId())).getName().contains(finalServiceItem.getId());
                }
            });

            Assert.assertFalse(folderRoleMappings.isEmpty());
            for (Mapping mapping : folderRoleMappings) {
                //assert that the source and the target id are not the same on the auto generated mapping to validate that it got mapped to the auto generated mapping on the target gateway
                Assert.assertNotSame(mapping.getSrcId(), mapping.getTargetId());
                RbacRoleMO sourceRole = getRole(getSourceEnvironment(), mapping.getSrcId());
                RbacRoleMO targetRole = getRole(getTargetEnvironment(), mapping.getTargetId());
                Assert.assertEquals(sourceRole.getName(), targetRole.getName());
            }

        } finally {
            if (serviceItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("services/" + serviceItem.getId(), HttpMethod.DELETE, null, "");
            }
        }
    }

    /**
     * Test migrating a full gateway with one created http configuration.
     */
    @Test
    @BugId("SSG-10177")
    public void testFullGatewayExportImportOneHttpConfiguration() throws Exception {
        Item<HttpConfigurationMO> httpConfigurationItem = null;
        try {
            httpConfigurationItem = createHttpConfiguration(getSourceEnvironment());
            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

        } finally {
            if (httpConfigurationItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("httpConfigurations/" + httpConfigurationItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("httpConfigurations/" + httpConfigurationItem.getId(), HttpMethod.DELETE, null, "");
            }
        }
    }

    /**
     * Test migrating a full gateway with one created service. Test that auto generated roles get created correctly.
     */
    @Test
    public void testFullGatewayExportImportInternalUser() throws Exception {
        Item<UserMO> userItem = null;
        try {
            userItem = createUser(getSourceEnvironment(), "srcUser", internalIDProviderItem.getId());
            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true&encryptSecrets=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Mapping userMappingIn = getMapping(bundleItem.getContent().getMappings(), userItem.getId());
            userMappingIn.getProperties().remove("FailOnNew");

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //verify the mappings
            Mapping userMapping = getMapping(mappings.getContent().getMappings(), userItem.getId());
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, userMapping.getActionTaken());
            Assert.assertEquals(userItem.getId(), userMapping.getSrcId());

        } finally {
            if (userItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("identityProviders/" + internalIDProviderItem.getId() + "/users/" + userItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("identityProviders/" + internalIDProviderItem.getId() + "/users/" + userItem.getId(), HttpMethod.DELETE, null, "");
            }
        }
    }

    /**
     * Test migrating a full gateway with one created service. Test that auto generated roles get created correctly.
     */
    @Test
    public void testFullGatewayExportImportFederatedUser() throws Exception {
        Item<UserMO> userItem = null;
        Item<IdentityProviderMO> federatedIdentityProviderItem = null;
        try {
            federatedIdentityProviderItem = createIdProviderFederated(getSourceEnvironment());
            userItem = createUser(getSourceEnvironment(), "srcUser", federatedIdentityProviderItem.getId());
            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
            Mapping userMappingIn = getMapping(bundleItem.getContent().getMappings(), userItem.getId());
            userMappingIn.getProperties().remove("FailOnNew");
            Mapping fipMappingIn = getMapping(bundleItem.getContent().getMappings(), federatedIdentityProviderItem.getId());
            fipMappingIn.getProperties().remove("FailOnNew");

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", "encryptPassword=true", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //verify the mappings
            Mapping userMapping = getMapping(mappings.getContent().getMappings(), userItem.getId());
            Assert.assertEquals(EntityType.USER.toString(), userMapping.getType());
            Assert.assertEquals(Mapping.Action.NewOrExisting, userMapping.getAction());
            Assert.assertEquals(Mapping.ActionTaken.CreatedNew, userMapping.getActionTaken());
            Assert.assertEquals(userItem.getId(), userMapping.getSrcId());

        } finally {
            if (userItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("identityProviders/" + federatedIdentityProviderItem.getId() + "/users/" + userItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("identityProviders/" + federatedIdentityProviderItem.getId() + "/users/" + userItem.getId(), HttpMethod.DELETE, null, "");
            }
            if (federatedIdentityProviderItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("identityProviders/" + federatedIdentityProviderItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("identityProviders/" + federatedIdentityProviderItem.getId(), HttpMethod.DELETE, null, "");

            }
        }
    }

    /**
     * Test migrating a full gateway with one assertion security zone.
     */
    @Test
    public void testFullGatewayExportImportOneAssertionSecurityZone() throws Exception {
        Item<SecurityZoneMO> securityZoneItem = null;
        Item<AssertionSecurityZoneMO> assertionSecurityZoneItem = null;
        try {
            securityZoneItem = createSecurityZone(getSourceEnvironment());
            assertionSecurityZoneItem = createAssertionSecurityZoneItem("com.l7tech.policy.assertion.FalseAssertion", securityZoneItem.getId(), getSourceEnvironment());

            RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());
            assertOkResponse(response);

            final Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            //import the bundle
            response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                    objectToString(bundleItem.getContent()));
            assertOkResponse(response);

            Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            response = getTargetEnvironment().processRequest("assertionSecurityZones/com.l7tech.policy.assertion.FalseAssertion", HttpMethod.GET, null, "");
            assertOkResponse(response);
            Item<AssertionSecurityZoneMO> targetAssertionSecurityZone = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

            Mapping securityZoneMapping = getMapping(mappings.getContent().getMappings(), securityZoneItem.getId());

            Assert.assertEquals("Assertion security zone id is not expected", securityZoneMapping.getTargetId(), targetAssertionSecurityZone.getContent().getSecurityZoneId());

        } finally {
            if (assertionSecurityZoneItem != null) {
                createAssertionSecurityZoneItem("com.l7tech.policy.assertion.FalseAssertion", null, getSourceEnvironment());
            }
            if (securityZoneItem != null) {
                RestResponse response = getSourceEnvironment().processRequest("securityZones/" + securityZoneItem.getId(), HttpMethod.DELETE, null, "");
                assertOkEmptyResponse(response);
                getTargetEnvironment().processRequest("securityZones/" + securityZoneItem.getId(), HttpMethod.DELETE, null, "");
            }
        }
    }

    private Item<HttpConfigurationMO> createHttpConfiguration(JVMDatabaseBasedRestManagementEnvironment environment) throws Exception {
        HttpConfigurationMO httpConfiguration = ManagedObjectFactory.createHttpConfiguration();
        httpConfiguration.setUsername("userNew");
        httpConfiguration.setPort(333);
        httpConfiguration.setHost("newHost");
        httpConfiguration.setProtocol(HttpConfigurationMO.Protocol.HTTP);
        httpConfiguration.setPath("path");
        httpConfiguration.setTlsKeyUse(HttpConfigurationMO.Option.DEFAULT);

        RestResponse response = environment.processRequest("httpConfigurations", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(httpConfiguration)));

        assertOkCreatedResponse(response);

        Item<HttpConfigurationMO> httpConfigurationItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        httpConfigurationItem.setContent(httpConfiguration);
        return httpConfigurationItem;

    }

    private Item<ServiceMO> createService(JVMDatabaseBasedRestManagementEnvironment environment) throws Exception {
        //create service
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        ServiceMO serviceMO = ManagedObjectFactory.createService();
        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceMO.setServiceDetail(serviceDetail);
        serviceDetail.setName("Source Service");
        serviceDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        ServiceDetail.HttpMapping serviceMapping = ManagedObjectFactory.createHttpMapping();
        serviceMapping.setUrlPattern("/srcService");
        serviceMapping.setVerbs(Arrays.asList("POST"));
        ServiceDetail.SoapMapping soapMapping = ManagedObjectFactory.createSoapMapping();
        soapMapping.setLax(false);
        serviceDetail.setServiceMappings(Arrays.asList(serviceMapping,soapMapping));
        serviceDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", true)
                .put("soapVersion", "1.2")
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent(assXml );
        ResourceSet wsdlResourceSet = ManagedObjectFactory.createResourceSet();
        wsdlResourceSet.setTag("wsdl");
        wsdlResourceSet.setRootUrl("http://localhost:8080/test.wsdl");
        Resource wsdlResource = ManagedObjectFactory.createResource();
        wsdlResourceSet.setResources(Arrays.asList(wsdlResource));
        wsdlResource.setType("wsdl");
        wsdlResource.setSourceUrl("http://localhost:8080/test.wsdl");
        wsdlResource.setContent("<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>" );
        serviceMO.setResourceSets(Arrays.asList(policyResourceSet,wsdlResourceSet));

        RestResponse response = environment.processRequest("services", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(serviceMO)));

        assertOkCreatedResponse(response);

        Item<ServiceMO> serviceItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        serviceItem.setContent(serviceMO);
        return serviceItem;
    }

    private Item<SecurityZoneMO> createSecurityZone(JVMDatabaseBasedRestManagementEnvironment environment) throws Exception {
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName("MySecurityZoneTarget");
        securityZoneMO.setDescription("MySecurityZone description");
        securityZoneMO.setPermittedEntityTypes(CollectionUtils.list(EntityType.ANY.toString()));
        RestResponse response = environment.processRequest("securityZones", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(securityZoneMO)));

        assertOkCreatedResponse(response);

        return MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }

    private Item<FolderMO> createFolder(JVMDatabaseBasedRestManagementEnvironment environment) throws Exception {
        FolderMO parentFolderMO = ManagedObjectFactory.createFolder();
        parentFolderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        parentFolderMO.setName("Source parent folder");
        RestResponse response = environment.processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(parentFolderMO)));
        assertOkCreatedResponse(response);
        return MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }

    private RbacRoleMO getRole(JVMDatabaseBasedRestManagementEnvironment environment, String id) throws Exception {
        // get the auto generated role for the policy from the target gateway
        RestResponse response = environment.processRequest("roles/" + id, HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        // validate that the user got mapped correctly.
        Item<RbacRoleMO> targetRole = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        return targetRole.getContent();
    }

    private Item<IdentityProviderMO> createIdProviderLdap(JVMDatabaseBasedRestManagementEnvironment environment) throws Exception {
        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setName("My New ID Provider");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.BIND_ONLY_LDAP);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .map());
        IdentityProviderMO.BindOnlyLdapIdentityProviderDetail detailsBindOnly = identityProviderMO.getBindOnlyLdapIdentityProviderDetail();
        detailsBindOnly.setServerUrls(Arrays.asList("server1", "server2"));
        detailsBindOnly.setUseSslClientAuthentication(true);
        detailsBindOnly.setBindPatternPrefix("prefix Pattern");
        detailsBindOnly.setBindPatternSuffix("suffix Pattern");

        RestResponse response = environment.processRequest("identityProviders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(identityProviderMO)));
        assertOkCreatedResponse(response);
        Item<IdentityProviderMO> idProviderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        identityProviderMO.setId(idProviderItem.getId());
        idProviderItem.setContent(identityProviderMO);
        return idProviderItem;
    }

    private Item<IdentityProviderMO> createIdProviderFederated(JVMDatabaseBasedRestManagementEnvironment environment) throws Exception {
        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setName("My Federated IDP");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.FEDERATED);
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("certificateValidation", "Validate Certificate Path")
                .put("enableCredentialType.x509", true)
                .put("enableCredentialType.saml", true)
                .map());
        IdentityProviderMO.FederatedIdentityProviderDetail federatedDetails = identityProviderMO.getFederatedIdentityProviderDetail();
        federatedDetails.setCertificateReferences(Collections.<String>emptyList());

        RestResponse response = environment.processRequest("identityProviders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(identityProviderMO)));
        assertOkCreatedResponse(response);
        Item<IdentityProviderMO> idProviderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        identityProviderMO.setId(idProviderItem.getId());
        idProviderItem.setContent(identityProviderMO);
        return idProviderItem;
    }

    private Item<UserMO> createUser(JVMDatabaseBasedRestManagementEnvironment environment, String login, String identityProviderId) throws Exception {
        //create user
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(identityProviderId);
        userMO.setLogin(login);
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        RestResponse response = environment.processRequest("identityProviders/" + identityProviderId + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        Item<UserMO> userItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        userItem.setContent(userMO);
        return userItem;
    }

    private Item<PolicyMO> createPolicy(JVMDatabaseBasedRestManagementEnvironment environment) throws Exception {
        //create policy
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Source Policy");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("soap", false)
                .map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent(assXml);
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));

        RestResponse response = environment.processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));

        assertOkCreatedResponse(response);

        Item<PolicyMO> policyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = environment.processRequest("policies/" + policyItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");

        return MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }

    private Item<AssertionSecurityZoneMO> createAssertionSecurityZoneItem(String assertionName, String securityZoneId, JVMDatabaseBasedRestManagementEnvironment environment) throws Exception {
        AssertionSecurityZoneMO assertionAccessMo = ManagedObjectFactory.createAssertionAccess();
        assertionAccessMo.setName(assertionName);
        assertionAccessMo.setSecurityZoneId(securityZoneId);
        RestResponse response = environment.processRequest("assertionSecurityZones/"+assertionName, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                XmlUtil.nodeToString(ManagedObjectFactory.write(assertionAccessMo)));

        assertOkResponse(response);

        Item<AssertionSecurityZoneMO> assertionSecurityZoneItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = environment.processRequest("assertionSecurityZones/" + assertionSecurityZoneItem.getId(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");

        return MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }
}
