package com.l7tech.identity.ldap;

import java.util.Collection;
import java.util.Iterator;
import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.identity.IdentityProviderConfig;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 19, 2003
 *
 */
public class LdapGroupManagerServerTest extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite(LdapGroupManagerServerTest.class);
        return suite;
    }

    public void testFindAll() throws Exception {
        IdentityProviderConfig config = new IdentityProviderConfig();
        // use this url when ssh forwarding locally
        config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://localhost:3899");
        // use this url when in the office
        //config.setLdapHostURL("ldap://spock:389");
        config.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, "dc=layer7-tech,dc=com");
        LdapGroupManagerServer me = new LdapGroupManagerServer(config);
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
