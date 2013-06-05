package com.l7tech.console.util;

import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityNameResolverTest {
    private static final long OID = 1234L;
    private static final String NAME = "test";
    private EntityNameResolver resolver;
    @Mock
    private ServiceAdmin serviceAdmin;
    @Mock
    private PolicyAdmin policyAdmin;
    @Mock
    private TrustedCertAdmin trustedCertAdmin;
    @Mock
    private ResourceAdmin resourceAdmin;

    @Before
    public void setup() {
        resolver = new EntityNameResolver(serviceAdmin, policyAdmin, trustedCertAdmin, resourceAdmin);
    }

    @Test
    public void getNameForHeaderUsesNameOnHeader() throws Exception {
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(null, null, "test", null)));
    }

    @Test
    public void getNameForServiceAlias() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(NAME);
        when(serviceAdmin.findByAlias(OID)).thenReturn(service);
        assertEquals(NAME + " alias", resolver.getNameForHeader(new EntityHeader(OID, EntityType.SERVICE_ALIAS, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForServiceAliasNotFound() throws Exception {
        when(serviceAdmin.findByAlias(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.SERVICE_ALIAS, null, null));
    }

    @Test
    public void getNameForPolicyAlias() throws Exception {
        when(policyAdmin.findByAlias(OID)).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, NAME, "xml", false));
        assertEquals(NAME + " alias", resolver.getNameForHeader(new EntityHeader(OID, EntityType.POLICY_ALIAS, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForPolicyAliasNotFound() throws Exception {
        when(policyAdmin.findByAlias(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.POLICY_ALIAS, null, null));
    }

    @Test
    public void getNameForKeyMetadata() throws Exception {
        when(trustedCertAdmin.findKeyMetadata(OID)).thenReturn(new SsgKeyMetadata(1L, NAME, null));
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(OID, EntityType.SSG_KEY_METADATA, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForKeyMetadataNotFound() throws Exception {
        when(trustedCertAdmin.findKeyMetadata(anyLong())).thenReturn(null);
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(OID, EntityType.SSG_KEY_METADATA, null, null)));
    }

    @Test
    public void getNameForResourceEntry() throws Exception {
        final String uri = "http://localhost:8080";
        final ResourceEntry resourceEntry = new ResourceEntry();
        resourceEntry.setUri(uri);
        when(resourceAdmin.findResourceEntryByPrimaryKey(OID)).thenReturn(resourceEntry);
        assertEquals(uri, resolver.getNameForHeader(new EntityHeader(OID, EntityType.RESOURCE_ENTRY, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForResourceEntryNotFound() throws Exception {
        when(resourceAdmin.findResourceEntryByPrimaryKey(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.RESOURCE_ENTRY, null, null));
    }

    @Test
    public void getNameForHttpConfiguration() throws Exception {
        final HttpConfiguration config = new HttpConfiguration();
        config.setProtocol(HttpConfiguration.Protocol.HTTP);
        config.setHost("localhost");
        config.setPort(8080);
        when(resourceAdmin.findHttpConfigurationByPrimaryKey(OID)).thenReturn(config);
        assertEquals("HTTP localhost 8080", resolver.getNameForHeader(new EntityHeader(OID, EntityType.HTTP_CONFIGURATION, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForHttpConfigurationNotFound() throws Exception {
        when(resourceAdmin.findHttpConfigurationByPrimaryKey(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.HTTP_CONFIGURATION, null, null));
    }

    @Test
    public void geNameForHeaderUnsupportedEntityType() throws Exception {
        assertTrue(resolver.getNameForHeader(new EntityHeader(OID, EntityType.ANY, null, null)).isEmpty());
    }
}
