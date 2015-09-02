package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class IdentityProviderMigration extends com.l7tech.skunkworks.rest.tools.MigrationTestBase {
    private static final Logger logger = Logger.getLogger(IdentityProviderMigration.class.getName());
    private Item<IdentityProviderMO> identityProviderItem;

    @Before
    public void before() throws Exception {
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

        RestResponse response = getSourceEnvironment().processRequest("identityProviders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(identityProviderMO)));
        assertOkCreatedResponse(response);

        identityProviderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        identityProviderItem.setContent(identityProviderMO);
    }

    @After
    public void after() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("identityProviders/" + identityProviderItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExportSingle() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?identityProvider=" + identityProviderItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A identityProvider", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 2 items. A identityProvider and a private key", 2, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testIgnoreIdentityProviderDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?identityProvider=" + identityProviderItem.getId() + "&requireIdentityProvider=" + identityProviderItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items. A identityProvider", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 mapping. A identityProvider", 1, bundleItem.getContent().getMappings().size());
        assertTrue((Boolean) bundleItem.getContent().getMappings().get(0).getProperties().get("FailOnNew"));
    }
}
