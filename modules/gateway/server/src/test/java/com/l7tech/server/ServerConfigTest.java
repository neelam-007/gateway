package com.l7tech.server;

import static org.junit.Assert.*;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ServerConfigTest {
    @Test
    public void checkPropertyDescriptions() {
        ServerConfig sc = ServerConfig.getInstance();

        Map<String,String> descToName = new HashMap<String,String>();
        Map<String,String> namesAndDescs = sc.getClusterPropertyNames();

        for (Map.Entry<String, String> entry : namesAndDescs.entrySet()) {
            String name = entry.getKey();
            String desc = entry.getValue();
            assertNotNull("Cluster property name must be non-null", name);
            final String trimmedName = name.trim().toLowerCase();
            assertTrue("Cluster property name must be non-empty", trimmedName.length() > 0);
            assertNotNull("Cluster property description for " + name + " must be non-null", desc);
            final String trimmedDesc = desc.trim().toLowerCase();
            assertTrue("Cluster property description for " + name + " must be non-empty", trimmedDesc.length() > 0);

            final String prevName = descToName.put(trimmedDesc, trimmedName);
            if (prevName != null)
                fail("Cluster property description for " + name + " duplicates description for " + prevName);
        }
    }
}
