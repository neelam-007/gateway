package com.l7tech.identity.ldap;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Collection;
import java.util.Iterator;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.identity.*;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 19, 2003
 *
 * Tests LdapIdentityProviderServer, LdapGroupManagerServer and LdapUserManagerServer
 * requires to be able to resolve the hostname "spock" and the directory on spock to be on
 * and containing at least one user and one group.
 */
public class LdapIdentityProviderServerTest extends junit.framework.TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite(LdapIdentityProviderServerTest.class);
        return suite;
    }

    public void testFindAllUsers() throws Exception {
        IdentityProviderConfig config = new IdentityProviderConfig(IdentityProviderType.LDAP);
        // use this url when ssh forwarding locally
        // config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://localhost:3899");
        // use this url when in the office
        config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://spock:389");
        config.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, "dc=layer7-tech,dc=com");

        // create the provider
        LdapIdentityProviderServer provider = new LdapIdentityProviderServer();
        provider.initialize(config);


        // get the user manager
        UserManager me = provider.getUserManager();

        // use it
        Collection headers = me.findAllHeaders();
        Iterator i = headers.iterator();
        boolean usrsReceived = false;
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            User usr = me.findByPrimaryKey(header.getStrId());
            if (usr != null) usrsReceived = true;
            // uncomment if you want to see the content
            // System.out.println(usr);
        }
        assertTrue("receiving users", usrsReceived);
    }

    public void testFindAllGroups() throws Exception {
        IdentityProviderConfig config = new IdentityProviderConfig(IdentityProviderType.LDAP);
        // use this url when ssh forwarding locally
        // config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://localhost:3899");
        // use this url when in the office
        config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://spock:389");
        config.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, "dc=layer7-tech,dc=com");

        // create the provider
        LdapIdentityProviderServer provider = new LdapIdentityProviderServer();
        provider.initialize(config);


        // get the user manager
        GroupManager me = provider.getGroupManager();

        // use it
        Collection res = me.findAll();
        Iterator i = res.iterator();
        boolean grpsReceived = false;
        while (i.hasNext()) {
            Group group = (Group)i.next();
            // uncomment if you want to see the content
            // System.out.println(group);
            if (group != null) grpsReceived = true;
        }
        assertTrue("receiving groups", grpsReceived);
    }

    public void testAuthenticate() throws Exception {
        IdentityProviderConfig config = new IdentityProviderConfig(IdentityProviderType.LDAP);
        // use this url when ssh forwarding locally
        // config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://localhost:3899");
        // use this url when in the office
        config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://spock:389");
        config.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, "dc=layer7-tech,dc=com");

        // create the provider
        LdapIdentityProviderServer provider = new LdapIdentityProviderServer();
        provider.initialize(config);

        // get the user manager
        UserManager me = provider.getUserManager();
        System.out.println("retrieving user");
        User francois = me.findByLogin("flascelles");
        System.out.println(francois);
        System.out.println("authenticating");
        if (provider.authenticate(francois, "rockclimbing".getBytes())) System.out.println("OK");
        else System.out.println("not authenticated");
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}


