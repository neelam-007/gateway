package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.skunkworks.rest.tools.JVMDatabaseBasedRestManagementEnvironment;
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
import java.util.logging.Logger;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class AssertionAccessMigrationTest extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(AssertionAccessMigrationTest.class.getName());

    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
        //delete all assertion accesses
        RestResponse response = getSourceEnvironment().processRequest("assertionSecurityZones", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        ItemsList<AssertionSecurityZoneMO> assZones = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        if (assZones.getContent() != null) {
            for (Item<AssertionSecurityZoneMO> assZone : assZones.getContent()) {
                if (assZone.getContent().getSecurityZoneId() != null && !Goid.DEFAULT_GOID.toString().equals(assZone.getContent().getSecurityZoneId())) {
                    updateAssZone(getSourceEnvironment(), assZone.getName(), null);
                }
            }
        }

        response = getTargetEnvironment().processRequest("assertionSecurityZones", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        assZones = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        if (assZones.getContent() != null) {
            for (Item<AssertionSecurityZoneMO> assZone : assZones.getContent()) {
                if (assZone.getContent().getSecurityZoneId() != null && !Goid.DEFAULT_GOID.toString().equals(assZone.getContent().getSecurityZoneId())) {
                    updateAssZone(getTargetEnvironment(), assZone.getName(), null);
                }
            }
        }

        // Delete security zones

        response = getSourceEnvironment().processRequest("securityZones", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        ItemsList<SecurityZoneMO> zones = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        if (zones.getContent() != null) {
            for (Item<SecurityZoneMO> zone : zones.getContent()) {
                response = getSourceEnvironment().processRequest("securityZones/" + zone.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
                assertOkEmptyResponse(response);
            }
        }

        response = getTargetEnvironment().processRequest("securityZones", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);

        zones = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(response.getBody())));
        if (zones.getContent() != null) {
            for (Item<SecurityZoneMO> zone : zones.getContent()) {
                response = getTargetEnvironment().processRequest("securityZones/" + zone.getId(), HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), "");
                assertOkEmptyResponse(response);
            }
        }
    }

    @Test
    public void testImportNewOrExisting() throws Exception {
        Item<SecurityZoneMO> sourceSecurityZone = createSecurityZone(getSourceEnvironment(), "MyZone");
        Item<AssertionSecurityZoneMO> gmaAccessZone = updateAssZone(getSourceEnvironment(), GatewayManagementAssertion.class.getName(), sourceSecurityZone.getId());

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the assertion accesses are exported
        Mapping assertionZoneMapping = getMapping(bundleItem.getContent().getMappings(), gmaAccessZone.getName());
        Assert.assertNotNull(assertionZoneMapping);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertionZoneMapping = getMapping(mappings.getContent().getMappings(), gmaAccessZone.getId());
        Assert.assertNotNull(assertionZoneMapping);
        Assert.assertEquals(EntityType.ASSERTION_ACCESS.toString(), assertionZoneMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, assertionZoneMapping.getAction());
        //depending on the order tests are run this could be either one.
        Assert.assertTrue(Mapping.ActionTaken.CreatedNew.equals(assertionZoneMapping.getActionTaken()) || Mapping.ActionTaken.UsedExisting.equals(assertionZoneMapping.getActionTaken()));
        Assert.assertEquals(gmaAccessZone.getId(), assertionZoneMapping.getSrcId());
        Assert.assertEquals(assertionZoneMapping.getSrcId(), assertionZoneMapping.getTargetId());

        //validate that the security zone is correct
        response = getTargetEnvironment().processRequest("assertionSecurityZones/" + gmaAccessZone.getName(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);
        Item<AssertionSecurityZoneMO> updatedAssZone = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(sourceSecurityZone.getId(), updatedAssZone.getContent().getSecurityZoneId());

        validate(mappings);
    }

    @Test
    public void testImportUpdateExisting() throws Exception {
        Item<SecurityZoneMO> sourceSecurityZone = createSecurityZone(getSourceEnvironment(), "MyZone");
        Item<AssertionSecurityZoneMO> gmaAccessZone = updateAssZone(getSourceEnvironment(), GatewayManagementAssertion.class.getName(), sourceSecurityZone.getId());

        Item<SecurityZoneMO> sourceSecurityZoneTrgt = createSecurityZone(getTargetEnvironment(), "MyZoneTrgt");
        Item<AssertionSecurityZoneMO> gmaAccessZoneTrgt = updateAssZone(getTargetEnvironment(), GatewayManagementAssertion.class.getName(), sourceSecurityZoneTrgt.getId());

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the assertion accesses are exported
        Mapping assertionZoneMapping = getMapping(bundleItem.getContent().getMappings(), gmaAccessZone.getName());
        Assert.assertNotNull(assertionZoneMapping);
        assertionZoneMapping.setAction(Mapping.Action.NewOrUpdate);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertionZoneMapping = getMapping(mappings.getContent().getMappings(), gmaAccessZone.getId());
        Assert.assertNotNull(assertionZoneMapping);
        Assert.assertEquals(EntityType.ASSERTION_ACCESS.toString(), assertionZoneMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrUpdate, assertionZoneMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UpdatedExisting, assertionZoneMapping.getActionTaken());
        Assert.assertEquals(gmaAccessZone.getId(), assertionZoneMapping.getSrcId());
        Assert.assertEquals(gmaAccessZoneTrgt.getId(), assertionZoneMapping.getTargetId());

        //validate that the security zone is correct
        response = getTargetEnvironment().processRequest("assertionSecurityZones/" + gmaAccessZone.getName(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);
        Item<AssertionSecurityZoneMO> updatedAssZone = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(sourceSecurityZone.getId(), updatedAssZone.getContent().getSecurityZoneId());

        validate(mappings);
    }

    @Test
    public void testImportMapExisting() throws Exception {
        Item<SecurityZoneMO> sourceSecurityZone = createSecurityZone(getSourceEnvironment(), "MyZone");
        Item<AssertionSecurityZoneMO> gmaAccessZone = updateAssZone(getSourceEnvironment(), GatewayManagementAssertion.class.getName(), sourceSecurityZone.getId());

        Item<SecurityZoneMO> sourceSecurityZoneTrgt = createSecurityZone(getTargetEnvironment(), "MyZoneTrgt");
        Item<AssertionSecurityZoneMO> gmaAccessZoneTrgt = updateAssZone(getTargetEnvironment(), GatewayManagementAssertion.class.getName(), sourceSecurityZoneTrgt.getId());

        RestResponse response = getSourceEnvironment().processRequest("bundle", "all=true", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //validate the the assertion accesses are exported
        Mapping assertionZoneMapping = getMapping(bundleItem.getContent().getMappings(), gmaAccessZone.getName());
        Assert.assertNotNull(assertionZoneMapping);

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertionZoneMapping = getMapping(mappings.getContent().getMappings(), gmaAccessZone.getId());
        Assert.assertNotNull(assertionZoneMapping);
        Assert.assertEquals(EntityType.ASSERTION_ACCESS.toString(), assertionZoneMapping.getType());
        Assert.assertEquals(Mapping.Action.NewOrExisting, assertionZoneMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.UsedExisting, assertionZoneMapping.getActionTaken());
        Assert.assertEquals(gmaAccessZone.getId(), assertionZoneMapping.getSrcId());
        Assert.assertEquals(gmaAccessZoneTrgt.getId(), assertionZoneMapping.getTargetId());

        //validate that the security zone is correct
        response = getTargetEnvironment().processRequest("assertionSecurityZones/" + gmaAccessZoneTrgt.getName(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);
        Item<AssertionSecurityZoneMO> updatedAssZone = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(sourceSecurityZoneTrgt.getId(), updatedAssZone.getContent().getSecurityZoneId());

        validate(mappings);
    }

    @Test
    public void testImportDelete() throws Exception {

        Item<SecurityZoneMO> sourceSecurityZoneTrgt = createSecurityZone(getTargetEnvironment(), "MyZoneTrgt");
        Item<AssertionSecurityZoneMO> gmaAccessZoneTrgt = updateAssZone(getTargetEnvironment(), GatewayManagementAssertion.class.getName(), sourceSecurityZoneTrgt.getId());

        Bundle bundle = ManagedObjectFactory.createBundle();

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(Mapping.Action.Delete);
        mapping.setSrcId(gmaAccessZoneTrgt.getName());
        mapping.setType(gmaAccessZoneTrgt.getType());
        mapping.setProperties(CollectionUtils.<String, Object>mapBuilder().put("MapBy", "name").map());

        Mapping mappingNotExisting = ManagedObjectFactory.createMapping();
        mappingNotExisting.setAction(Mapping.Action.Delete);
        mappingNotExisting.setSrcId("bad.assertion.name");
        mappingNotExisting.setType(gmaAccessZoneTrgt.getType());
        mappingNotExisting.setProperties(CollectionUtils.<String, Object>mapBuilder().put("MapBy", "name").map());

        bundle.setMappings(Arrays.asList(mapping, mappingNotExisting));

        //import the bundle
        RestResponse response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(),
                objectToString(bundle));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Mapping assertionZoneMapping = mappings.getContent().getMappings().get(0);
        Assert.assertNotNull(assertionZoneMapping);
        Assert.assertEquals(EntityType.ASSERTION_ACCESS.toString(), assertionZoneMapping.getType());
        Assert.assertEquals(Mapping.Action.Delete, assertionZoneMapping.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Deleted, assertionZoneMapping.getActionTaken());
        Assert.assertEquals(gmaAccessZoneTrgt.getId(), assertionZoneMapping.getSrcId());
        Assert.assertEquals(gmaAccessZoneTrgt.getId(), assertionZoneMapping.getTargetId());

        Mapping activeConnectorMappingNotExisting = mappings.getContent().getMappings().get(1);
        Assert.assertEquals(EntityType.ASSERTION_ACCESS.toString(), activeConnectorMappingNotExisting.getType());
        Assert.assertEquals(Mapping.Action.Delete, activeConnectorMappingNotExisting.getAction());
        Assert.assertEquals(Mapping.ActionTaken.Ignored, activeConnectorMappingNotExisting.getActionTaken());
        Assert.assertEquals(null, activeConnectorMappingNotExisting.getTargetId());

        //validate that the security zone is correct
        response = getTargetEnvironment().processRequest("assertionSecurityZones/"+gmaAccessZoneTrgt.getName(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        Item<AssertionSecurityZoneMO> updatedAssZone = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertEquals(null, updatedAssZone.getContent().getSecurityZoneId());

        validate(mappings);
    }

    private Item<SecurityZoneMO> createSecurityZone(JVMDatabaseBasedRestManagementEnvironment environment, String name) throws Exception {
        SecurityZoneMO securityZoneMO = ManagedObjectFactory.createSecurityZone();
        securityZoneMO.setName(name);
        securityZoneMO.setDescription("MySecurityZone description");
        securityZoneMO.setPermittedEntityTypes(CollectionUtils.list(EntityType.ANY.toString()));
        RestResponse response = environment.processRequest("securityZones", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(securityZoneMO)));

        assertOkCreatedResponse(response);

        return MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }

    private Item<AssertionSecurityZoneMO> updateAssZone(JVMDatabaseBasedRestManagementEnvironment environment, String name, String securityZoneId) throws Exception {
        AssertionSecurityZoneMO assertionAccess = ManagedObjectFactory.createAssertionAccess();
        assertionAccess.setName(name);
        assertionAccess.setSecurityZoneId(securityZoneId);

        RestResponse response = environment.processRequest("assertionSecurityZones/" + name, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(assertionAccess)));
        assertOkResponse(response);
        Item createdItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        response = environment.processRequest("assertionSecurityZones/" + createdItem.getName(), HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
        assertOkResponse(response);
        return MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
    }
}
