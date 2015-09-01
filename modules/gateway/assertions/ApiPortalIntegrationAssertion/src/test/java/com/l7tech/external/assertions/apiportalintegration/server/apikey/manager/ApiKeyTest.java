package com.l7tech.external.assertions.apiportalintegration.server.apikey.manager;

import org.junit.Test;


import static org.junit.Assert.*;
/**
 * @author ALEE, 6/19/2015
 */
public class ApiKeyTest {
    @Test
    public void getReadOnlyCopyAccountPlanMappingName() {
        final ApiKey key = new ApiKey();
        key.setName("abc123");
        key.setSecret("secret");
        key.setLabel("label");
        key.setAccountPlanMappingName("org name");

        final ApiKey copy = key.getReadOnlyCopy();
        assertEquals("abc123", copy.getName());
        assertEquals("secret", copy.getSecret());
        assertEquals("org name", copy.getAccountPlanMappingName());
    }
}
