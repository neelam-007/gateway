package com.l7tech.adminws.identity;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 19, 2003
 *
 * Tests WS for identity. Requires the actual WS to be deployed at http://localhost:8080/ssg/services/identityAdmin
 * and a valid admin account with the following credentials "ssgadmin", "ssgadminpasswd".
 *
 */
public class ClientTest extends junit.framework.TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite(ClientTest.class);
        return suite;
    }

    public void testFinds() throws Exception {
        Client me = new Client("http://localhost:8080/ssg/services/identityAdmin", "ssgadmin", "ssgadminpasswd");

        // test echo
        System.out.println(me.echoVersion());

        // test findAllIdentityProviderConfig
        com.l7tech.objectmodel.EntityHeader[] res = me.findAllIdentityProviderConfig();

        // com.l7tech.objectmodel.EntityHeader[] res = me.findAllIdentityProviderConfigByOffset(0, 10);

        for (int i = 0; i < res.length; i++) {
            System.out.println(res[i].toString());
            com.l7tech.identity.IdentityProviderConfig ipc = me.findIdentityProviderConfigByPrimaryKey(res[i].getOid());
            System.out.println(ipc.toString());

            System.out.println("fetching groups");
            com.l7tech.objectmodel.EntityHeader[] res2 = me.findAllGroups(ipc.getOid());
            for (int j = 0; j < res2.length; j++) {
                System.out.println(res2[j].toString());
                System.out.println("group " + res2[j].getOid());
                com.l7tech.identity.Group group = me.findGroupByPrimaryKey(ipc.getOid(), res2[j].getOid());
                System.out.println("group found" + group.toString());
                // System.out.println("save group" + me.saveGroup(ipc.getOid(), group));
                // System.out.println("delete group");
                // me.deleteGroup(ipc.getOid(), group.getOid());
            }

            System.out.println("fetching users");
            res2 = me.findAllUsers(ipc.getOid());
            for (int j = 0; j < res2.length; j++) {
                System.out.println(res2[j].toString());
                com.l7tech.identity.User user = me.findUserByPrimaryKey(ipc.getOid(), res2[j].getStrId());
                System.out.println("user found" + user.toString());
                // System.out.println("save user" + me.saveUser(ipc.getOid(), user));
                // System.out.println("delete user");
                // me.deleteUser(ipc.getOid(), user.getOid());
            }
            // System.out.println("re-saved" + me.saveIdentityProviderConfig(ipc));
            // me.deleteIdentityProviderConfig(ipc.getOid());
            // System.out.println("deleted");


        }
    }

    public static void main(String[] args) throws Exception {
        ClientTest toto = new ClientTest();
        toto.testFinds();
        //junit.textui.TestRunner.run(suite());
    }
}
