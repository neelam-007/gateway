package com.l7tech.adminws.identity;

import com.l7tech.identity.*;
import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.util.Locator;
import com.l7tech.objectmodel.DeleteException;

import java.net.PasswordAuthentication;

/**
 * User: flascell
 * Date: Jul 1, 2003
 * Time: 10:59:50 AM
 *
 */
public class IdentityProviderClientTest {
    public static void testCreateDeleteUser(IdentityProviderClient testee) throws Exception {
        System.out.println("creating user");
        User newuser = new User();
        newuser.setName("test user");
        newuser.setLogin("tuser");
        newuser.setPassword("i am the grand cornolio");
        newuser.setEmail("blah@foo.bar");
        System.out.println("getting user manager");
        UserManager userManager = testee.getUserManager();
        System.out.println("saving user");
        long userId = userManager.save(newuser);
        System.out.println("user saved, oid=" + userId);
        newuser.setOid(userId);
        System.out.println("deleting user");
        userManager.delete(newuser);
        System.out.println("done testCreateDeleteUser");
    }

    public static void testDeleteAdminUser(IdentityProviderClient testee) throws Exception {
        UserManager userMan = testee.getUserManager();
        System.out.println("getting admin dude");
        User administrator = userMan.findByLogin("ssgadmin");
        if (administrator != null) {
            System.out.println("we got him, let's try to delete him");
            try {
                userMan.delete(administrator);
            } catch (DeleteException e) {
                System.out.println("deletion failed. that is good. " + e.getMessage());
                System.out.println("done testDeleteAdminUser");
                return;
            }
            System.out.println("oops, that seemed successful. let's save him again");
            administrator.setOid(0);
            userMan.save(administrator);
        }
        System.out.println("done testDeleteAdminUser");
    }

    public static void main(String[] args) throws Exception {
        ClientCredentialManager credsManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        PasswordAuthentication creds = new PasswordAuthentication("ssgadmin", "ssgadminpasswd".toCharArray());
        credsManager.login(creds);

        // construct the provider to test
        IdentityProviderClient internalProvider = new IdentityProviderClient();
        IdentityProviderConfig cfg = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        cfg.setOid(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
        cfg.setDescription("Internal identity provider");
        internalProvider.initialize(cfg);

        // test create and delete
        testCreateDeleteUser(internalProvider);

        // test DeleteAdminUser
        testDeleteAdminUser(internalProvider);

        System.out.println("done");
    }
}
