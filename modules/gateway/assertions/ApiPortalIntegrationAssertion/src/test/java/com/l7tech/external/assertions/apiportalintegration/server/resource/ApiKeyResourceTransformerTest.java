package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ApiKeyResourceTransformerTest {
    private ApiKeyResourceTransformer transformer;

    @Before
    public void setup() {
        transformer = new ApiKeyResourceTransformer();
    }

    @Test
    public void resourceToEntity() {
        final ApiKeyResource resource = createDefaultResource();

        final ApiKey entity = transformer.resourceToEntity(resource);

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
        assertTrue(entity.getAccountPlanMappingName().isEmpty());
    }

    @Test
    public void resourceToEntityNullSecurity() {
        final ApiKeyResource resource = createDefaultResource();
        resource.setSecurity(null);

        final ApiKey entity = transformer.resourceToEntity(resource);

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

        final ApiKey entity = transformer.resourceToEntity(resource);

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
    public void resourceToEntityAccountPlanMappingName() {
        final ApiKeyResource resource = createDefaultResource();
        resource.setAccountPlanMappingName("Organization Name");
        final ApiKey entity = transformer.resourceToEntity(resource);
        assertEquals("Organization Name", entity.getAccountPlanMappingName());
    }

    @Test
    public void entityToResource() {
        final ApiKey entity = createDefaultEntity();

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
        assertTrue(resource.getAccountPlanMappingName().isEmpty());
    }

    @Test
    public void entityToResourceNullOAuthDetails() {
        final ApiKey entity = createDefaultEntity();
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

    @Test
    public void entityToResourceAccountPlanMappingName() throws Exception {
        final ApiKey entity = createDefaultEntity();
        entity.setAccountPlanMappingName("Organization Name");
        final ApiKeyResource resource = transformer.entityToResource(entity);
        assertEquals("Organization Name", resource.getAccountPlanMappingName());
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

    private ApiKey createDefaultEntity() {
        final ApiKey entity = new ApiKey();
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
