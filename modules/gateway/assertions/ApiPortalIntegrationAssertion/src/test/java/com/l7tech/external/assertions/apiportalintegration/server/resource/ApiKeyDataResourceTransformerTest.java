package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ApiKeyDataResourceTransformerTest {
    private ApiKeyDataResourceTransformer transformer;

    @Before
    public void setup() {
        transformer = new ApiKeyDataResourceTransformer();
    }

    @Test
    public void resourceToEntity() {
        final ApiKeyResource resource = createDefaultResource();

        final ApiKeyData entity = transformer.resourceToEntity(resource);

        assertEquals("k1", entity.getName());
        assertEquals("label", entity.getLabel());
        assertEquals("callback", entity.getOauthCallbackUrl());
        assertEquals("scope", entity.getOauthScope());
        assertEquals("oauthType", entity.getOauthType());
        assertEquals("platform", entity.getPlatform());
        assertEquals("secret", entity.getSecret());
        assertEquals("active", entity.getStatus());
        assertEquals(2, entity.getServiceIds().size());
        assertEquals("p1", entity.getServiceIds().get("s1"));
        assertEquals("p2", entity.getServiceIds().get("s2"));
    }

    @Test
    public void resourceToEntityNullSecurity() {
        final ApiKeyResource resource = createDefaultResource();
        resource.setSecurity(null);

        final ApiKeyData entity = transformer.resourceToEntity(resource);

        assertEquals("k1", entity.getName());
        assertEquals("label", entity.getLabel());
        assertNull(entity.getOauthCallbackUrl());
        assertNull(entity.getOauthScope());
        assertNull(entity.getOauthType());
        assertEquals("platform", entity.getPlatform());
        assertEquals("secret", entity.getSecret());
        assertEquals("active", entity.getStatus());
        assertEquals(2, entity.getServiceIds().size());
        assertEquals("p1", entity.getServiceIds().get("s1"));
        assertEquals("p2", entity.getServiceIds().get("s2"));
    }

    @Test
    public void resourceToEntityNullOAuth() {
        final ApiKeyResource resource = createDefaultResource();
        resource.getSecurity().setOauth(null);

        final ApiKeyData entity = transformer.resourceToEntity(resource);

        assertEquals("k1", entity.getName());
        assertEquals("label", entity.getLabel());
        assertNull(entity.getOauthCallbackUrl());
        assertNull(entity.getOauthScope());
        assertNull(entity.getOauthType());
        assertEquals("platform", entity.getPlatform());
        assertEquals("secret", entity.getSecret());
        assertEquals("active", entity.getStatus());
        assertEquals(2, entity.getServiceIds().size());
        assertEquals("p1", entity.getServiceIds().get("s1"));
        assertEquals("p2", entity.getServiceIds().get("s2"));
    }

    @Test
    public void entityToResource() {
        final ApiKeyData entity = createDefaultEntity();

        final ApiKeyResource resource = transformer.entityToResource(entity);

        assertEquals("k1", resource.getKey());
        assertEquals("label", resource.getLabel());
        assertEquals("callback", resource.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", resource.getSecurity().getOauth().getScope());
        assertEquals("oauthType", resource.getSecurity().getOauth().getType());
        assertEquals("platform", resource.getPlatform());
        assertEquals("secret", resource.getSecret());
        assertEquals("active", resource.getStatus());
        assertEquals(2, resource.getApis().size());
        assertEquals("p1", resource.getApis().get("s1"));
        assertEquals("p2", resource.getApis().get("s2"));
    }

    @Test
    public void entityToResourceNullOAuthDetails() {
        final ApiKeyData entity = createDefaultEntity();
        entity.setOauthCallbackUrl(null);
        entity.setOauthScope(null);
        entity.setOauthType(null);

        final ApiKeyResource resource = transformer.entityToResource(entity);

        assertEquals("k1", resource.getKey());
        assertEquals("label", resource.getLabel());
        assertNull(resource.getSecurity().getOauth());
        assertEquals("platform", resource.getPlatform());
        assertEquals("secret", resource.getSecret());
        assertEquals("active", resource.getStatus());
        assertEquals(2, resource.getApis().size());
        assertEquals("p1", resource.getApis().get("s1"));
        assertEquals("p2", resource.getApis().get("s2"));
    }

    private ApiKeyResource createDefaultResource() {
        final ApiKeyResource resource = new ApiKeyResource();
        resource.setKey("k1");
        resource.setLabel("label");
        resource.setPlatform("platform");
        resource.setSecret("secret");
        resource.setStatus("active");
        resource.setSecurity(new SecurityDetails(new OAuthDetails("callback", "scope", "oauthType")));
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        resource.setApis(services);
        return resource;
    }

    private ApiKeyData createDefaultEntity() {
        final ApiKeyData entity = new ApiKeyData();
        entity.setName("k1");
        entity.setLabel("label");
        entity.setOauthCallbackUrl("callback");
        entity.setOauthScope("scope");
        entity.setOauthType("oauthType");
        entity.setPlatform("platform");
        entity.setSecret("secret");
        entity.setStatus("active");
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        entity.setServiceIds(services);
        return entity;
    }

}
