package com.l7tech.identity.ldap;

import java.util.Collection;
import java.util.Iterator;
import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.User;

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
        // config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://localhost:3899");
        config.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://spock:389");
        // use this url when in the office
        config.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, "dc=layer7-tech,dc=com");
        LdapIdentityProviderServer provider = new LdapIdentityProviderServer();
        provider.initialize(config);
        GroupManager me = provider.getGroupManager();
        Collection res = me.findAll();
        Iterator i = res.iterator();
        while (i.hasNext()) {
            Group group = (Group)i.next();
            System.out.println(printGroup(group));
        }
    }

    private String printGroup(Group grp) {
        String out = "Group: " + grp.toString();
        Iterator i = grp.getMembers().iterator();
        while (i.hasNext()) {
            User u = (User)i.next();
            out += "\n\n\tmember:" + u.toString();
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
