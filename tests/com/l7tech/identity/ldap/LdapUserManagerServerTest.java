package com.l7tech.identity.ldap;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Collection;
import java.util.Iterator;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 19, 2003
 *
 */
public class LdapUserManagerServerTest extends junit.framework.TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite(LdapUserManagerServerTest.class);
        return suite;
    }

    public void testFindByPrimaryKey() throws Exception {
        IdentityProviderConfig config = new IdentityProviderConfig(IdentityProviderType.LDAP);
        // use this url when ssh forwarding locally
        //config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://localhost:3899");
        // use this url when in the office
        config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://spock:389");
        config.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, "dc=layer7-tech,dc=com");
        LdapUserManagerServer me = new LdapUserManagerServer(config);

        Collection headers = me.findAllHeaders();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            User usr = me.findByPrimaryKey(header.getStrId());
            System.out.println(usr);
        }
    }

    public void testFindByLogin() throws Exception {
        IdentityProviderConfig config = new IdentityProviderConfig(IdentityProviderType.LDAP);
        // use this url when ssh forwarding locally
        //config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://localhost:3899");
        // use this url when in the office
        config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://spock:389");
        config.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, "dc=layer7-tech,dc=com");
        LdapUserManagerServer me = new LdapUserManagerServer(config);

        User usr = me.findByLogin("flascelles");
        System.out.println(usr);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
