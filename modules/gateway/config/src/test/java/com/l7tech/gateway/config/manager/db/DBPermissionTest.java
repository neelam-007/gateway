package com.l7tech.gateway.config.manager.db;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.test.BugId;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class DBPermissionTest {
    @BugId("SSG-8848")
    @Test
    public void getPermissionStatementRevoke() {
        final DatabaseConfig config = new DatabaseConfig("localhost", 1234, "ssg", "root", null);
        final DBActions.DBPermission permission = new DBActions.DBPermission(config, Collections.singleton("localhost"), false);
        assertEquals("revoke all privileges on ssg.* from root@'localhost'", permission.getPermissionStatement("localhost"));
    }

    @Test
    public void getPermissionStatementGrant() {
        final DatabaseConfig config = new DatabaseConfig("localhost", 1234, "ssg", "root", "test");
        final DBActions.DBPermission permission = new DBActions.DBPermission(config, Collections.singleton("localhost"), true);
        assertEquals("grant all on ssg.* to root@'localhost' identified by 'test'", permission.getPermissionStatement("localhost"));
    }
}
