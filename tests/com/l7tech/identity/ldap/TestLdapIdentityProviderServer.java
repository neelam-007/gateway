package com.l7tech.identity.ldap;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Collection;
import java.util.Iterator;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.GroupManager;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 19, 2003
 *
 */
public class TestLdapIdentityProviderServer extends junit.framework.TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite(TestLdapIdentityProviderServer.class);
        return suite;
    }

    public void testFindAllUsers() throws Exception {
        LdapIdentityProviderConfig config = new LdapIdentityProviderConfig();
        // use this url when ssh forwarding locally
        config.setLdapHostURL("ldap://localhost:3899");
        // use this url when in the office
        //config.setLdapHostURL("ldap://spock:389");
        config.setSearchBase("dc=layer7-tech,dc=com");

        // create the provider
        LdapIdentityProviderServer provider = new LdapIdentityProviderServer();
        provider.initialize(config);


        // get the user manager
        UserManager me = provider.getUserManager();

        // use it
        Collection headers = me.findAllHeaders();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            User usr = me.findByPrimaryKey(header.getStrId());
            System.out.println(usr);
        }
    }

    public void testFindAllGroups() throws Exception {
        LdapIdentityProviderConfig config = new LdapIdentityProviderConfig();
        // use this url when ssh forwarding locally
        config.setLdapHostURL("ldap://localhost:3899");
        // use this url when in the office
        //config.setLdapHostURL("ldap://spock:389");
        config.setSearchBase("dc=layer7-tech,dc=com");

        // create the provider
        LdapIdentityProviderServer provider = new LdapIdentityProviderServer();
        provider.initialize(config);


        // get the user manager
        GroupManager me = provider.getGroupManager();

        // use it
        Collection res = me.findAll();
        Iterator i = res.iterator();
        while (i.hasNext()) {
            LdapGroup group = (LdapGroup)i.next();
            System.out.println(group);
        }
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}


