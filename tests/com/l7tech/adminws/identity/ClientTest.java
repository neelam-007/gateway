package com.l7tech.adminws.identity;

import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapConfigSettings;
import com.l7tech.common.util.Locator;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.net.PasswordAuthentication;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 19, 2003
 *
 * Tests WS for identity. Requires the actual WS to be deployed at http://localhost:8080/ssg/services/identityAdmin
 * and a valid admin account with the following credentials "ssgadmin", "ssgadminpasswd".
 *
 */
public class ClientTest extends TestCase {


    /**
     * create the <code>TestSuite</code> for the
     * LogCLientTest <code>TestCase</code>
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite(ClientTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            /**
             * test setup that deletes the stub data store; will trigger
             * store recreate
             * sets the environment
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                System.setProperty("com.l7tech.util.locator.properties",
                        "/com/l7tech/console/resources/services.properties");
            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }


    public void testFinds() throws Exception {
        // IdentityService me = new Client();
        IdentityService me =
                (IdentityService)Locator.getDefault().lookup(IdentityService.class);
        if (me == null) throw new IllegalStateException("cannot obtain identity service reference");
        // test echo
        System.out.println(me.echoVersion());

        // test findAllIdentityProviderConfig
        com.l7tech.objectmodel.EntityHeader[] res = me.findAllIdentityProviderConfig();

        // com.l7tech.objectmodel.EntityHeader[] res = me.findAllIdentityProviderConfigByOffset(0, 10);

        for (int i = 0; i < res.length; i++) {
            System.out.println(res[i].toString());
            com.l7tech.identity.IdentityProviderConfig ipc = me.findIdentityProviderConfigByPrimaryKey(res[i].getOid());
            System.out.println(ipc.toString());

            // skip ldap providers
            if (ipc.type() == IdentityProviderType.LDAP) continue;

            System.out.println("fetching groups");
            com.l7tech.objectmodel.EntityHeader[] res2 = me.findAllGroups(ipc.getOid());
            for (int j = 0; j < res2.length; j++) {
                System.out.println(res2[j].toString());
                System.out.println("group " + res2[j].getOid());
                com.l7tech.identity.Group group = me.findGroupByPrimaryKey(ipc.getOid(), res2[j].getStrId());
                System.out.println("group found " + group.toString());
                // System.out.println("save group" + me.saveGroup(ipc.getOid(), group));
                // System.out.println("delete group");
                // me.deleteGroup(ipc.getOid(), group.getOid());
            }

            System.out.println("fetching users");
            res2 = me.findAllUsers(ipc.getOid());
            for (int j = 0; j < res2.length; j++) {
                System.out.println(res2[j].toString());
                com.l7tech.identity.User user = me.findUserByPrimaryKey(ipc.getOid(), res2[j].getStrId());
                System.out.println("user found " + user.toString());
                // System.out.println("save user" + me.saveUser(ipc.getOid(), user));
                // System.out.println("delete user");
                // me.deleteUser(ipc.getOid(), user.getOid());
            }
            // System.out.println("re-saved" + me.saveIdentityProviderConfig(ipc));
            // me.deleteIdentityProviderConfig(ipc.getOid());
            // System.out.println("deleted");


        }
    }

    public void testCreateSpockLdapIDProviderConfig() throws Exception {
        // IdentityService me = new Client();
        IdentityService me =
                (IdentityService)Locator.getDefault().lookup(IdentityService.class);
        if (me == null) throw new IllegalStateException("cannot obtain identity service reference");
          

        // test echo
        System.out.println(me.echoVersion());

        // create a ldap id provider config
        com.l7tech.identity.IdentityProviderConfig cfg = new com.l7tech.identity.IdentityProviderConfig(IdentityProviderType.LDAP);

        cfg.setName("spock directory");
        cfg.setDescription("spock directory as seen from the kelowna office");

        cfg.putProperty(LdapConfigSettings.LDAP_HOST_URL, "ldap://localhost:3899");
        cfg.putProperty(LdapConfigSettings.LDAP_SEARCH_BASE, "dc=layer7-tech,dc=com");

        // save it
        long newcfgid = me.saveIdentityProviderConfig(cfg);

        System.out.println("new config saved successfuly with id:" + newcfgid);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.ssl.trustStore", System.getProperties().getProperty("user.home") + File.separator + ".l7tech" + File.separator + "trustStore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        ClientCredentialManager credsManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        PasswordAuthentication creds = new PasswordAuthentication("ssgadmin", "ssgadminpasswd".toCharArray());
        credsManager.login(creds);

        ClientTest toto = new ClientTest();
        toto.testFinds();
        // toto.testCreateSpockLdapIDProviderConfig();
        //junit.textui.TestRunner.run(suite());
    }
}
