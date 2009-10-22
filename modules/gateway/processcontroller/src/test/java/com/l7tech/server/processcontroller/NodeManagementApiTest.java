/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

/** @author alex */
public class NodeManagementApiTest {
    
    @Test
    @Ignore("Destructive and unlikely to work on your computer anyway")
    public void testDeleteNode() throws Exception {
        NodeManagementApi api = new CxfUtils.ApiBuilder("https://localhost:8765/services/nodeManagementApi").build(NodeManagementApi.class);
        api.deleteNode("default", 36000);
    }

    @Test
    @Ignore("Change the mysql root password if you want to try it")
    public void testCreateDatabase() throws Exception {
        NodeManagementApi api = new CxfUtils.ApiBuilder("https://localhost:8765/services/nodeManagementApi").build(NodeManagementApi.class);
        final DatabaseConfig dbc = new DatabaseConfig("localhost", 3306, "ssgtemp", "gatewaytemp", "7layertemp");
        dbc.setDatabaseAdminUsername("root");
        dbc.setDatabaseAdminPassword("thisIsNotMyMysqlPassword");
        api.createDatabase("default", dbc, Collections.<String>emptySet(), "myadmin", "mypass", "hostname");
    }
}