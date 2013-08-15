package com.l7tech.server.identity.ldap;

//import org.apache.directory.server.core.integ.annotations.CleanupLevel;
//import org.apache.directory.server.core.integ.Level;
//import org.apache.directory.server.core.integ.IntegrationUtils;
//import org.apache.directory.server.integ.SiRunner;
//import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
//import org.apache.directory.server.ldap.LdapServer;
//import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Ignore;

import javax.naming.directory.DirContext;
import javax.naming.NamingException;

import com.l7tech.util.MockConfig;
import com.l7tech.util.IOUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ResourceUtils;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.test.BugNumber;

import java.util.Properties;
import java.util.NoSuchElementException;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * LDAP Identity Provider tests that use an embedded Apache Directory Server
 *
 * <p>To use this test you'll need to install the Apache Directory Server and
 * uncomment the dependency in modules/gateway/server/ivy.xml</p>
 *
 * <p><code>./build.sh repository-install-remote -Dorg=org.apache.directory.server -Dmod=apacheds-server-integ -Drev=1.5.5</code></p>
 *
 * <p>NOTE: If this doesn't work (after rebuilding your idea project ...) check
 * the file "lib/repository/org.apache.mina/mina-core-2.0.0-M6.jar" is present,
 * if not this should fix it (rebuild idea project):</p>
 *
 * <p><code>wget http://mirrors.ibiblio.org/pub/mirrors/maven2/org/apache/mina/mina-core/2.0.0-M6/mina-core-2.0.0-M6.jar -O lib/repository/org.apache.mina/mina-core-2.0.0-M6.jar</code></p>
 */
//@RunWith( SiRunner.class )
//@CleanupLevel( Level.CLASS )
@Ignore
public class LdapIdentityProviderTest {
//    public static LdapServer ldapServer;
    private boolean initialized = false;

    /**
     * Test user lookup / finding / searching
     */
    @Test
    public void testFindUser() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider();
        LdapUserManager ldapUserManager = ldapIdentityProvider.getUserManager();

        {
            LdapUser user1 = ldapUserManager.findByLogin("test1");
            Assert.assertNotNull( "Test user1 found by login", user1 );
        }
        {
            LdapUser user1 = ldapUserManager.findByPrimaryKey("cn=test1, ou=users, ou=system");
            Assert.assertNotNull( "Test user1 found by dn", user1 );
        }
        {
            LdapUser user1 = ldapUserManager.findByPrimaryKey("cn=testnouserhere, ou=users, ou=system");
            Assert.assertNull( "Test testnouserhere found", user1 );
        }
        EntityHeaderSet<IdentityHeader> expectedUsers = new EntityHeaderSet<IdentityHeader>();
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test1, ou=users, ou=system", "test1", EntityType.USER ) );
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test2, ou=users, ou=system", "test2", EntityType.USER ) );
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test3, ou=users, ou=system", "test3", EntityType.USER ) );
        Assert.assertTrue( "Found user headers", ldapUserManager.findAllHeaders().containsAll( expectedUsers ));
        Assert.assertTrue( "Searched user headers", ldapUserManager.search("*").containsAll( expectedUsers ));
    }

    /**
     * Test group lookup / finding / searching
     */
    @Test
    public void testFindGroups() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider();
        LdapGroupManager ldapGroupManager = ldapIdentityProvider.getGroupManager();

        {
            LdapGroup group1 = ldapGroupManager.findByName("group1");
            Assert.assertNotNull( "Test group1 found by login", group1 );
        }
        {
            LdapGroup group1 = ldapGroupManager.findByPrimaryKey("cn=group1, ou=users, ou=system");
            Assert.assertNotNull( "Test group1 found by dn", group1 );
        }
        {
            LdapGroup group1 = ldapGroupManager.findByPrimaryKey("cn=testnogrouphere, ou=users, ou=system");
            Assert.assertNull( "Test testnogrouphere found", group1 );
        }
        EntityHeaderSet<IdentityHeader> expectedGroups = new EntityHeaderSet<IdentityHeader>();
        expectedGroups.add( idh( ldapIdentityProvider, "cn=pgroup1, ou=users, ou=system", "pgroup1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=pgroup2, ou=users, ou=system", "pgroup2", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=pgroup3, ou=users, ou=system", "pgroup3", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=pgroup4, ou=users, ou=system", "pgroup4", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=group1, ou=users, ou=system", "group1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=group2, ou=users, ou=system", "group2", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=ugroup1, ou=users, ou=system", "ugroup1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=ugroup2, ou=users, ou=system", "ugroup2", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=sougroup1, ou=groups1, ou=system", "sougroup1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=sougroup2, ou=groups2, ou=system", "sougroup2", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "ou=users, ou=system", "users", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "ou=groups1, ou=system", "groups1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "ou=groups2, ou=system", "groups2", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "ou=system", "system", EntityType.GROUP ) );
        Assert.assertTrue( "Found group headers", ldapGroupManager.findAllHeaders().containsAll( expectedGroups ));
        Assert.assertTrue( "Searched group headers", ldapGroupManager.search("*").containsAll( expectedGroups ));
    }

    /**
     * Basic group membership tests with no nesting limits
     */
    @Test
    public void testGroupIsMember() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider();
        LdapUserManager ldapUserManager = ldapIdentityProvider.getUserManager();
        LdapGroupManager ldapGroupManager = ldapIdentityProvider.getGroupManager();

        LdapUser user1 = ldapUserManager.findByLogin("test1");
        Assert.assertNotNull( "Test user1 found", user1 );

        LdapUser user2 = ldapUserManager.findByLogin("test2");
        Assert.assertNotNull( "Test user2 found", user2 );

        LdapGroup group = ldapGroupManager.findByName("group1");
        Assert.assertNotNull( "Test group found", group );

        LdapGroup group2 = ldapGroupManager.findByName("group2");
        Assert.assertNotNull( "Test group found", group2 );

        LdapGroup ugroup = ldapGroupManager.findByName("ugroup1");
        Assert.assertNotNull( "Test group found", ugroup );

        LdapGroup ugroup2 = ldapGroupManager.findByName("ugroup2");
        Assert.assertNotNull( "Test group found", ugroup2 );

        LdapGroup pgroup = ldapGroupManager.findByName("pgroup1");
        Assert.assertNotNull( "Test posix group found", pgroup );

        LdapGroup pgroup4 = ldapGroupManager.findByName("pgroup4");
        Assert.assertNotNull( "Test posix group4 found", pgroup4 );

        LdapGroup ougroup = ldapGroupManager.findByName("users");
        Assert.assertNotNull( "Test ou group found", ougroup );

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, group ));
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, group ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, group2 ));
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, group2 ));

        Assert.assertTrue( "Member of ugroup", ldapGroupManager.isMember( user1, ugroup ));
        Assert.assertFalse( "Not member of ugroup", ldapGroupManager.isMember( user2, ugroup ));

        Assert.assertTrue( "Member of ugroup", ldapGroupManager.isMember( user1, ugroup2 ));
        Assert.assertFalse( "Not member of ugroup", ldapGroupManager.isMember( user2, ugroup2 ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, pgroup ));
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, pgroup ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, pgroup4 ));
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, pgroup4 ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, ougroup ));
        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user2, ougroup ));
    }

    /**
     * Basic group membership tests for case insensitive matching
     */
    @BugNumber(8703)
    @Test
    public void testGroupIsMemberBug8703() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider(true);
        LdapUserManager ldapUserManager = ldapIdentityProvider.getUserManager();
        LdapGroupManager ldapGroupManager = ldapIdentityProvider.getGroupManager();

        LdapUser user1 = ldapUserManager.findByLogin("jdawson");
        Assert.assertNotNull( "Test jdawson found", user1 );

        LdapUser user2 = ldapUserManager.findByLogin("jjensen"); // user with mismatched case in group membership
        Assert.assertNotNull( "Test jjensen found", user2 );

        LdapGroup group = ldapGroupManager.findByName("CORESALES_ECS");
        Assert.assertNotNull( "Test group found", group );

        Assert.assertTrue( "Member of group (jdawson)", ldapGroupManager.isMember( user1, group ));
        Assert.assertTrue( "Member of group (jjensen)", ldapGroupManager.isMember( user2, group ));
    }

    /**
     * Negative group membership test for case sensitive matching
     */
    @BugNumber(8703)
    @Test
    public void testGroupIsMemberBug8703Negative() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider();
        LdapUserManager ldapUserManager = ldapIdentityProvider.getUserManager();
        LdapGroupManager ldapGroupManager = ldapIdentityProvider.getGroupManager();

        LdapUser user2 = ldapUserManager.findByLogin("jjensen"); // user with mismatched case in group membership
        Assert.assertNotNull( "Test jjensen found", user2 );

        LdapGroup group = ldapGroupManager.findByName("CORESALES_ECS");
        Assert.assertNotNull( "Test group found", group );

        // Should fail due to case sensitive check
        Assert.assertFalse( "Member of group (jjensen)", ldapGroupManager.isMember( user2, group ));
    }

    /**
     * Test user listings for groups
     */
    @Test
    public void testGroupMembership() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider();
        LdapGroupManager ldapGroupManager = ldapIdentityProvider.getGroupManager();

        LdapGroup group = ldapGroupManager.findByName("group1");
        Assert.assertNotNull( "Test group found", group );

        LdapGroup group2 = ldapGroupManager.findByName("group2");
        Assert.assertNotNull( "Test group found", group2 );

        LdapGroup ugroup = ldapGroupManager.findByName("ugroup1");
        Assert.assertNotNull( "Test group found", ugroup );

        LdapGroup ugroup2 = ldapGroupManager.findByName("ugroup2");
        Assert.assertNotNull( "Test group found", ugroup2 );

        LdapGroup pgroup = ldapGroupManager.findByName("pgroup1");
        Assert.assertNotNull( "Test posix group found", pgroup );

        LdapGroup pgroup4 = ldapGroupManager.findByName("pgroup4");
        Assert.assertNotNull( "Test posix group4 found", pgroup4 );

        LdapGroup ougroup = ldapGroupManager.findByName("users");
        Assert.assertNotNull( "Test ou group found", ougroup );

        LdapGroup ougroup2 = ldapGroupManager.findByName("system");
        Assert.assertNotNull( "Test ou group2 found", ougroup2 );

        EntityHeaderSet<IdentityHeader> expectedUsers = new EntityHeaderSet<IdentityHeader>();
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test1, ou=users, ou=system", "test1", EntityType.USER ) );

        Assert.assertTrue( "group membership", ldapGroupManager.getUserHeaders(group).containsAll( expectedUsers ));
        Assert.assertTrue( "posix group membership", ldapGroupManager.getUserHeaders(pgroup).containsAll( expectedUsers ));
        Assert.assertTrue( "ugroup membership", ldapGroupManager.getUserHeaders(ugroup).containsAll( expectedUsers ));

        Assert.assertTrue( "group membership nested", ldapGroupManager.getUserHeaders(group2).containsAll( expectedUsers ));
        Assert.assertTrue( "posix group membership nested", ldapGroupManager.getUserHeaders(pgroup4).containsAll( expectedUsers ));
        Assert.assertTrue( "ugroup membership nested", ldapGroupManager.getUserHeaders(ugroup2).containsAll( expectedUsers ));

        expectedUsers.add( idh( ldapIdentityProvider, "cn=test2, ou=users, ou=system", "test2", EntityType.USER ) );
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test3, ou=users, ou=system", "test3", EntityType.USER ) );

        Assert.assertTrue( "Ou group membership", ldapGroupManager.getUserHeaders(ougroup).containsAll( expectedUsers ));
        Assert.assertTrue( "Ou group membership by id", ldapGroupManager.getUserHeaders("ou=users, ou=system").containsAll( expectedUsers ));

        Assert.assertTrue( "Ou group membership nested", ldapGroupManager.getUserHeaders(ougroup2).containsAll( expectedUsers ));
        Assert.assertTrue( "Ou group membership nested by id", ldapGroupManager.getUserHeaders("ou=system").containsAll( expectedUsers ));
    }

    /**
     * Test group listings for users 
     */
    @Test
    public void testContainingGroups() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider();
        LdapUserManager ldapUserManager = ldapIdentityProvider.getUserManager();
        LdapGroupManager ldapGroupManager = ldapIdentityProvider.getGroupManager();
        
        LdapUser user1 = ldapUserManager.findByLogin("test1");
        Assert.assertNotNull( "Test user1 found", user1 );

        LdapUser user2 = ldapUserManager.findByLogin("test2");
        Assert.assertNotNull( "Test user2 found", user2 );

        EntityHeaderSet<IdentityHeader> expectedGroups = new EntityHeaderSet<IdentityHeader>();
        expectedGroups.add( idh( ldapIdentityProvider, "ou=users, ou=system", "users", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "ou=system", "system", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=sougroup2, ou=groups2, ou=system", "sougroup1", EntityType.GROUP ) );

        Assert.assertTrue( "Group listing user2", ldapGroupManager.getGroupHeaders( user2 ).containsAll( expectedGroups ));

        expectedGroups.add( idh( ldapIdentityProvider, "cn=pgroup1, ou=users, ou=system", "pgroup1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=pgroup2, ou=users, ou=system", "pgroup2", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=pgroup3, ou=users, ou=system", "pgroup3", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=pgroup4, ou=users, ou=system", "pgroup4", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=group1, ou=users, ou=system", "group1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=group2, ou=users, ou=system", "group2", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=ugroup1, ou=users, ou=system", "ugroup1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=ugroup2, ou=users, ou=system", "ugroup2", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "cn=sougroup1, ou=groups1, ou=system", "sougroup1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "ou=groups1, ou=system", "groups1", EntityType.GROUP ) );
        expectedGroups.add( idh( ldapIdentityProvider, "ou=groups2, ou=system", "groups2", EntityType.GROUP ) );

        Set<IdentityHeader> groups = ldapGroupManager.getGroupHeaders( user1 );
        Assert.assertTrue( "Group listing user1", groups.containsAll( expectedGroups ));
    }

    /**
     * Basic group hierarchy test with nesting limit of 2
     */
    @Test
    public void testGroupNestingLimit() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider(2);
        LdapUserManager ldapUserManager = ldapIdentityProvider.getUserManager();
        LdapGroupManager ldapGroupManager = ldapIdentityProvider.getGroupManager();

        LdapUser user1 = ldapUserManager.findByLogin("test1");
        Assert.assertNotNull( "Test user1 found", user1 );

        LdapUser user2 = ldapUserManager.findByLogin("test2");
        Assert.assertNotNull( "Test user2 found", user2 );

        LdapGroup group = ldapGroupManager.findByName("group1");
        Assert.assertNotNull( "Test group found", group );

        LdapGroup group2 = ldapGroupManager.findByName("group2");
        Assert.assertNotNull( "Test group found", group2 );

        LdapGroup ugroup = ldapGroupManager.findByName("ugroup1");
        Assert.assertNotNull( "Test group found", ugroup );

        LdapGroup ugroup2 = ldapGroupManager.findByName("ugroup2");
        Assert.assertNotNull( "Test group found", ugroup2 );

        LdapGroup pgroup = ldapGroupManager.findByName("pgroup1");
        Assert.assertNotNull( "Test posix group found", pgroup );

        LdapGroup pgroup2 = ldapGroupManager.findByName("pgroup2");
        Assert.assertNotNull( "Test posix group2 found", pgroup2 );

        LdapGroup pgroup4 = ldapGroupManager.findByName("pgroup4");
        Assert.assertNotNull( "Test posix group4 found", pgroup4 );

        LdapGroup ougroup = ldapGroupManager.findByName("users");
        Assert.assertNotNull( "Test ou group found", ougroup );

        LdapGroup ougroup2 = ldapGroupManager.findByName("system");
        Assert.assertNotNull( "Test ou group2 found", ougroup2 );

        // Test isMember nesting
        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, group ));
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, group ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, group2 ));
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, group2 ));

        Assert.assertTrue( "Member of ugroup", ldapGroupManager.isMember( user1, ugroup ));
        Assert.assertFalse( "Not member of ugroup", ldapGroupManager.isMember( user2, ugroup ));

        Assert.assertTrue( "Member of ugroup", ldapGroupManager.isMember( user1, ugroup2 ));
        Assert.assertFalse( "Not member of ugroup", ldapGroupManager.isMember( user2, ugroup2 ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, pgroup ));
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, pgroup ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, pgroup2 ));
        Assert.assertFalse( "Not member of group2", ldapGroupManager.isMember( user2, pgroup2 ));

        Assert.assertFalse( "Not Member of group", ldapGroupManager.isMember( user1, pgroup4 ));  // Not member due to nesting limit
        Assert.assertFalse( "Not member of group2", ldapGroupManager.isMember( user2, pgroup4 ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, ougroup ));
        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user2, ougroup ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, ougroup2 ));
        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user2, ougroup2 ));

        EntityHeaderSet<IdentityHeader> expectedUsers = new EntityHeaderSet<IdentityHeader>();
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test1, ou=users, ou=system", "test1", EntityType.USER ) );

        Assert.assertTrue( "group membership", ldapGroupManager.getUserHeaders(group).containsAll( expectedUsers ));
        Assert.assertTrue( "posix group membership", ldapGroupManager.getUserHeaders(pgroup).containsAll( expectedUsers ));
        Assert.assertTrue( "ugroup membership", ldapGroupManager.getUserHeaders(ugroup).containsAll( expectedUsers ));

        Assert.assertTrue( "group membership nested", ldapGroupManager.getUserHeaders(group2).containsAll( expectedUsers ));
        Assert.assertTrue( "posix group membership nested", ldapGroupManager.getUserHeaders(pgroup2).containsAll( expectedUsers ));
        Assert.assertFalse( "posix group membership nested", ldapGroupManager.getUserHeaders(pgroup4).containsAll( expectedUsers ));  // Due to nesting limit
        Assert.assertTrue( "ugroup membership nested", ldapGroupManager.getUserHeaders(ugroup2).containsAll( expectedUsers ));

        expectedUsers.add( idh( ldapIdentityProvider, "cn=test2, ou=users, ou=system", "test2", EntityType.USER ) );
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test3, ou=users, ou=system", "test3", EntityType.USER ) );

        Assert.assertTrue( "Ou group membership", ldapGroupManager.getUserHeaders(ougroup).containsAll( expectedUsers ));
        Assert.assertTrue( "Ou group membership by id", ldapGroupManager.getUserHeaders("ou=users, ou=system").containsAll( expectedUsers ));

        Assert.assertTrue( "Ou group membership nested", ldapGroupManager.getUserHeaders(ougroup2).containsAll( expectedUsers ));
        Assert.assertTrue( "Ou group membership nested by id", ldapGroupManager.getUserHeaders("ou=system").containsAll( expectedUsers ));
    }

    /**
     * Basic group hierarchy test with no nesting
     */
    @Test
    public void testGroupsNoNesting() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider(1);
        LdapUserManager ldapUserManager = ldapIdentityProvider.getUserManager();
        LdapGroupManager ldapGroupManager = ldapIdentityProvider.getGroupManager();

        LdapUser user1 = ldapUserManager.findByLogin("test1");
        Assert.assertNotNull( "Test user1 found", user1 );

        LdapUser user2 = ldapUserManager.findByLogin("test2");
        Assert.assertNotNull( "Test user2 found", user2 );

        LdapGroup group = ldapGroupManager.findByName("group1");
        Assert.assertNotNull( "Test group found", group );

        LdapGroup group2 = ldapGroupManager.findByName("group2");
        Assert.assertNotNull( "Test group found", group2 );

        LdapGroup ugroup = ldapGroupManager.findByName("ugroup1");
        Assert.assertNotNull( "Test group found", ugroup );

        LdapGroup ugroup2 = ldapGroupManager.findByName("ugroup2");
        Assert.assertNotNull( "Test group found", ugroup2 );

        LdapGroup pgroup = ldapGroupManager.findByName("pgroup1");
        Assert.assertNotNull( "Test posix group found", pgroup );

        LdapGroup pgroup4 = ldapGroupManager.findByName("pgroup4");
        Assert.assertNotNull( "Test posix group4 found", pgroup4 );

        LdapGroup ougroup = ldapGroupManager.findByName("users");
        Assert.assertNotNull( "Test ou group found", ougroup );

        LdapGroup ougroup2 = ldapGroupManager.findByName("system");
        Assert.assertNotNull( "Test ou group2 found", ougroup2 );

        // Test isMember nesting
        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, group ));
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, group ));

        Assert.assertFalse( "not Member of group", ldapGroupManager.isMember( user1, group2 ));  // Not member due to nesting limit
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, group2 ));

        Assert.assertTrue( "Member of ugroup", ldapGroupManager.isMember( user1, ugroup ));
        Assert.assertFalse( "Not member of ugroup", ldapGroupManager.isMember( user2, ugroup ));

        Assert.assertFalse( "Not Member of ugroup", ldapGroupManager.isMember( user1, ugroup2 ));  // Not member due to nesting limit
        Assert.assertFalse( "Not member of ugroup", ldapGroupManager.isMember( user2, ugroup2 ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, pgroup ));
        Assert.assertFalse( "Not member of group", ldapGroupManager.isMember( user2, pgroup ));

        Assert.assertFalse( "Not Member of group", ldapGroupManager.isMember( user1, pgroup4 ));  // Not member due to nesting limit
        Assert.assertFalse( "Not member of group2", ldapGroupManager.isMember( user2, pgroup4 ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, ougroup ));
        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user2, ougroup ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, ougroup2 ));
        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user2, ougroup2 ));

        EntityHeaderSet<IdentityHeader> expectedUsers = new EntityHeaderSet<IdentityHeader>();
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test1, ou=users, ou=system", "test1", EntityType.USER ) );

        Assert.assertTrue( "group membership", ldapGroupManager.getUserHeaders(group).containsAll( expectedUsers ));
        Assert.assertTrue( "posix group membership", ldapGroupManager.getUserHeaders(pgroup).containsAll( expectedUsers ));
        Assert.assertTrue( "ugroup membership", ldapGroupManager.getUserHeaders(ugroup).containsAll( expectedUsers ));

        Assert.assertFalse( "group membership nested", ldapGroupManager.getUserHeaders(group2).containsAll( expectedUsers ));        // Not member due to nesting limit
        Assert.assertFalse( "posix group membership nested", ldapGroupManager.getUserHeaders(pgroup4).containsAll( expectedUsers )); // Not member due to nesting limit
        Assert.assertFalse( "ugroup membership nested", ldapGroupManager.getUserHeaders(ugroup2).containsAll( expectedUsers ));      // Not member due to nesting limit

        expectedUsers.add( idh( ldapIdentityProvider, "cn=test2, ou=users, ou=system", "test2", EntityType.USER ) );
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test3, ou=users, ou=system", "test3", EntityType.USER ) );

        Assert.assertTrue( "Ou group membership", ldapGroupManager.getUserHeaders(ougroup).containsAll( expectedUsers ));
        Assert.assertTrue( "Ou group membership by id", ldapGroupManager.getUserHeaders("ou=users, ou=system").containsAll( expectedUsers ));

        Assert.assertTrue( "Ou group membership nested", ldapGroupManager.getUserHeaders(ougroup2).containsAll( expectedUsers ));
        Assert.assertTrue( "Ou group membership nested by id", ldapGroupManager.getUserHeaders("ou=system").containsAll( expectedUsers ));
    }

    /**
     * Group hierarchy test for groups in other types of group (regular group in ou group and vice versa)
     */
    @Test
    public void testHeterogenousGroupNesting() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider(0);
        LdapUserManager ldapUserManager = ldapIdentityProvider.getUserManager();
        LdapGroupManager ldapGroupManager = ldapIdentityProvider.getGroupManager();

        LdapUser user1 = ldapUserManager.findByLogin("test1");
        Assert.assertNotNull( "Test user1 found", user1 );

        LdapUser user2 = ldapUserManager.findByLogin("test2");
        Assert.assertNotNull( "Test user2 found", user2 );

        LdapGroup sougroup1 = ldapGroupManager.findByName("sougroup1");
        Assert.assertNotNull( "Test group found", sougroup1 );

        LdapGroup sougroup2 = ldapGroupManager.findByName("sougroup2");
        Assert.assertNotNull( "Test group found", sougroup2 );

        LdapGroup ougroup1 = ldapGroupManager.findByName("groups1");
        Assert.assertNotNull( "Test ou group found", ougroup1 );

        LdapGroup ougroup2 = ldapGroupManager.findByName("groups2");
        Assert.assertNotNull( "Test ou group found", ougroup2 );

        // Test isMember nesting
        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, ougroup1 )); // user is member of nested group sougroup1
        Assert.assertFalse( "Not Member of group", ldapGroupManager.isMember( user2, ougroup1 ));

        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user1, sougroup2 )); // user is member of nested ou group sougroup2
        Assert.assertTrue( "Member of group", ldapGroupManager.isMember( user2, sougroup2 )); // user is member of nested ou group sougroup2

        EntityHeaderSet<IdentityHeader> expectedUsers = new EntityHeaderSet<IdentityHeader>();
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test1, ou=users, ou=system", "test1", EntityType.USER ) );

        Assert.assertTrue( "group membership", ldapGroupManager.getUserHeaders(ougroup1).containsAll( expectedUsers ));

        expectedUsers.add( idh( ldapIdentityProvider, "cn=test2, ou=users, ou=system", "test2", EntityType.USER ) );
        expectedUsers.add( idh( ldapIdentityProvider, "cn=test3, ou=users, ou=system", "test3", EntityType.USER ) );

        Assert.assertTrue( "group membership", ldapGroupManager.getUserHeaders(sougroup2).containsAll( expectedUsers ));
    }

    @Test
    public void testProvider() throws Exception {
        init();

        LdapIdentityProvider ldapIdentityProvider = getLdapIdentityProvider();

        EntityHeaderSet<IdentityHeader> foundHeaders = ldapIdentityProvider.search( new EntityType[]{ EntityType.USER, EntityType.GROUP}, "*" );
        EntityHeaderSet<IdentityHeader> expectedHeaders = new EntityHeaderSet<IdentityHeader>();
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=test1, ou=users, ou=system", "test1", EntityType.USER ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=test2, ou=users, ou=system", "test2", EntityType.USER ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=test3, ou=users, ou=system", "test3", EntityType.USER ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=pgroup1, ou=users, ou=system", "pgroup1", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=pgroup2, ou=users, ou=system", "pgroup2", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=pgroup3, ou=users, ou=system", "pgroup3", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=pgroup4, ou=users, ou=system", "pgroup4", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=group1, ou=users, ou=system", "group1", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=group2, ou=users, ou=system", "group2", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=ugroup1, ou=users, ou=system", "ugroup1", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=ugroup2, ou=users, ou=system", "ugroup2", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=sougroup1, ou=groups1, ou=system", "sougroup1", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "cn=sougroup2, ou=groups2, ou=system", "sougroup2", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "ou=users, ou=system", "users", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "ou=groups1, ou=system", "groups1", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "ou=groups2, ou=system", "groups2", EntityType.GROUP ) );
        expectedHeaders.add( idh( ldapIdentityProvider, "ou=system", "system", EntityType.GROUP ) );
        Assert.assertTrue( "Search found users/groups", foundHeaders.containsAll( expectedHeaders ));

        LdapUser user3 = ldapIdentityProvider.findUserByCredential( LoginCredentials.makeLoginCredentials(new HttpBasicToken("test3", "".toCharArray()), HttpBasic.class) );
        Assert.assertNotNull( "Test user3 found", user3 );
    }

    private IdentityHeader idh( LdapIdentityProvider ldapIdentityProvider, String dn, String cn, EntityType type ) {
        // long providerGoid, String identityId, EntityType type, String loginName, String description, String commonName, Integer version
        return new IdentityHeader( ldapIdentityProvider.getConfig().getGoid(), dn, type, cn, "", cn, -1 );
    }

    private LdapIdentityProvider getLdapIdentityProvider() throws Exception {
        return getLdapIdentityProvider(0, false);
    }

    private LdapIdentityProvider getLdapIdentityProvider( boolean caseInsensitiveGroupMembership ) throws Exception {
        return getLdapIdentityProvider(0, caseInsensitiveGroupMembership);
    }

    private LdapIdentityProvider getLdapIdentityProvider( int groupNestingLimit ) throws Exception {
        return getLdapIdentityProvider(groupNestingLimit, false);
    }
    
    private LdapIdentityProvider getLdapIdentityProvider( int groupNestingLimit,
                                                          boolean caseInsensitiveGroupMembership ) throws Exception {
        LdapRuntimeConfig ldapRuntimeConfig = new LdapRuntimeConfig( new MockConfig( new Properties() ) );
        LdapIdentityProviderImpl ldapProvider = new LdapIdentityProviderImpl(){
            @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
            @Override
            public DirContext getBrowseContext() throws NamingException {
                try {
                    DirContext context = null;
//                    context = getWiredContext( ldapServer );
                    return context;
                } catch (Exception e) {
                    throw (NamingException) new NamingException().initCause( e );
                }
            }
        };
        LdapUserManagerImpl ldapUserManager = new LdapUserManagerImpl();
        LdapGroupManagerImpl ldapGroupManager = new LdapGroupManagerImpl();

        ldapProvider.setLdapRuntimeConfig( ldapRuntimeConfig );
        ldapUserManager.setLdapRuntimeConfig( ldapRuntimeConfig );
        ldapGroupManager.setLdapRuntimeConfig( ldapRuntimeConfig );

        LdapIdentityProviderConfig config = loadTemplate( getClass().getResource( "ldapTemplates/TestLDAP.xml" ), "TestLDAP" );
        config.setSearchBase( "ou=system" );
        config.setGroupMaxNesting( groupNestingLimit );
        config.setGroupMembershipCaseInsensitive( caseInsensitiveGroupMembership );
        ldapProvider.setUserManager( ldapUserManager );
        ldapProvider.setGroupManager( ldapGroupManager );
        ldapProvider.setIdentityProviderConfig( config );

        return ldapProvider;
    }

    private LdapIdentityProviderConfig loadTemplate( final URL templateUrl, String name ) throws IOException {
        InputStream is = null;
        try {
            is = templateUrl.openStream();
            byte[] data = IOUtils.slurpStream(is);
            String properties = new String(data);

            LdapIdentityProviderConfig template = new LdapIdentityProviderConfig();
            template.setName(name);
            template.setSerializedProps(properties);
            template.setTemplateName(template.getName());

            return template;
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new CausedIOException("Error reading object", aioobe);
        } catch (NoSuchElementException nsee) {
            throw new CausedIOException("Error reading object", nsee);
        } finally {
            ResourceUtils.closeQuietly( is );
        }
    }

    private void init() throws Exception {
        if ( !initialized ) {
//            IntegrationUtils.injectEntries( ldapServer.getDirectoryService(), new String(IOUtils.slurpUrl(getClass().getResource( "ldap.schema.nis.ldif" ))));
//            IntegrationUtils.injectEntries( ldapServer.getDirectoryService(), new String(IOUtils.slurpUrl(getClass().getResource( "ldap.ldif" ))));
            initialized = true;
        }
    }
}
