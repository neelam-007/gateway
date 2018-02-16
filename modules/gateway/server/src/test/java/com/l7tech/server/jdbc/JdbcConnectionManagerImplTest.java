package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.test.BugId;
import com.l7tech.util.SyspropUtil;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import javax.inject.Inject;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/com/l7tech/server/resources/testEmbeddedDbContext.xml",
        "/com/l7tech/server/resources/testManagerContext.xml",
        "/com/l7tech/server/resources/dataAccessContext.xml"})
@TransactionConfiguration(transactionManager = "transactionManager")
public class JdbcConnectionManagerImplTest {

    static {
        //this is needed to
        SyspropUtil.setProperty("com.l7tech.server.dbScriptsDirectory", "etc/db/liquibase");
    }

    @Inject
    private JdbcConnectionManager jdbcConnectionManager;
    @Inject
    private SecurityZoneManager securityZoneManager;
    private Cache cache;

    @Before
    public void before() {
        cache = CacheManager.getInstance().getCache(JdbcConnection.class.getName());
    }

    @After
    public void after() {
        cache.removeAll();
    }

    @Test
    public void testDriverClassNotSupported() throws Exception {
        assertFalse(jdbcConnectionManager.isDriverClassSupported("test.driver"));
    }

    @Test
    public void testDriverClassSupported() throws Exception {
        assertTrue(jdbcConnectionManager.isDriverClassSupported("com.mysql.jdbc.Driver"));
    }

    @Test
    @BugId("DE332398")
    public void testCacheInvalidationAfterSecurityZoneDeletion() throws Exception {
        SecurityZone zone = new SecurityZone();
        zone.setName("zone");
        Goid zoneGoid = securityZoneManager.save(zone);
        zone.setGoid(zoneGoid);

        JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName("connection");
        jdbcConnection.setDriverClass("com.driver.Driver");
        jdbcConnection.setJdbcUrl("db://url");
        jdbcConnection.setUserName("user");
        jdbcConnection.setPassword("pass");
        jdbcConnection.setEnabled(true);
        jdbcConnection.setSecurityZone(zone);
        Goid connZoneGoid = jdbcConnectionManager.save(jdbcConnection);

        jdbcConnection = jdbcConnectionManager.findByPrimaryKey(connZoneGoid);
        assertNotNull(jdbcConnection.getSecurityZone());
        assertEquals(zoneGoid, jdbcConnection.getSecurityZone().getGoid());

        List keys = cache.getKeys();
        assertNotNull(keys);
        assertEquals(1, keys.size());

        securityZoneManager.delete(zoneGoid);

        keys = cache.getKeys();
        assertNotNull(keys);
        assertEquals(0, keys.size());

        jdbcConnection = jdbcConnectionManager.findByPrimaryKey(connZoneGoid);
        assertNull(jdbcConnection.getSecurityZone());

        keys = cache.getKeys();
        assertNotNull(keys);
        assertEquals(1, keys.size());
    }

    @Test
    @BugId("DE332398")
    public void testNoCacheChangeAfterUnrelatedSecurityZoneDeletion() throws Exception {
        SecurityZone zone = new SecurityZone();
        zone.setName("zone1");
        Goid zoneGoid = securityZoneManager.save(zone);
        zone.setGoid(zoneGoid);

        JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName("connection1");
        jdbcConnection.setDriverClass("com.driver.Driver");
        jdbcConnection.setJdbcUrl("db://url1");
        jdbcConnection.setUserName("user1");
        jdbcConnection.setPassword("pass1");
        jdbcConnection.setEnabled(false);
        jdbcConnection.setSecurityZone(null);
        Goid connNoZoneGoid = jdbcConnectionManager.save(jdbcConnection);

        jdbcConnection = jdbcConnectionManager.findByPrimaryKey(connNoZoneGoid);
        assertNull(jdbcConnection.getSecurityZone());

        List keys = cache.getKeys();
        assertNotNull(keys);
        assertEquals(1, keys.size());

        securityZoneManager.delete(zoneGoid);

        keys = cache.getKeys();
        assertNotNull(keys);
        assertEquals(1, keys.size());

    }

}
