package com.l7tech.identity;

import com.l7tech.common.util.Locator;
import com.l7tech.console.security.ClientCredentialManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.identity.IdProvConfManagerServer;
import org.apache.axis.client.Call;

import javax.xml.namespace.QName;
import java.io.File;
import java.net.PasswordAuthentication;
import java.util.Collection;
import java.util.Iterator;

/**
 * User: flascell
 * Date: Jul 1, 2003
 * Time: 10:59:50 AM
 *
 */
public class IdentityProviderClientTest {

    public IdentityProviderClientTest() throws Exception {
        System.setProperty("javax.net.ssl.trustStore", System.getProperties().getProperty("user.home") + File.separator + ".l7tech" + File.separator + "trustStore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        ClientCredentialManager credsManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        PasswordAuthentication creds = new PasswordAuthentication("ssgadmin", "ssgadminpasswd".toCharArray());
        credsManager.login(creds);

        // construct the provider to test
        IdentityProviderConfig cfg = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        cfg.setOid(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
        cfg.setDescription("Internal identity provider");

        IdentityProviderClient internalProvider = new IdentityProviderClient(cfg);

        idProvider = internalProvider;
    }

    public void testListAll() throws Exception {
        System.out.println("READING ALL USERS");
        Collection headers = idProvider.getUserManager().findAllHeaders();
        for (Iterator i = headers.iterator(); i.hasNext();) {
            EntityHeader header = (EntityHeader)i.next();
            System.out.println(header);
            User usr = idProvider.getUserManager().findByPrimaryKey(header.getStrId());
            System.out.println(usr);
        }
        System.out.println("DONE READING USERS");
    }

    public void testCreateDeleteUser() throws Exception {
        System.out.println("creating user");
        InternalUser newuser = new InternalUser();
        newuser.setName("test user");
        newuser.setLogin("tuser");
        newuser.setPassword("i am the grand cornolio");
        newuser.setEmail("blah@foo.bar");
        System.out.println("getting user manager");
        UserManager userManager = idProvider.getUserManager();
        System.out.println("saving user");
        String userId = userManager.save(newuser);
        System.out.println("user saved, oid=" + userId);
        newuser.setOid( new Long( userId ).longValue() );
        System.out.println("deleting user");
        userManager.delete(newuser);
        System.out.println("done testCreateDeleteUser");
    }

    public void testDeleteAdminUser() throws Exception {
        UserManager userMan = idProvider.getUserManager();
        System.out.println("getting admin dude");
        InternalUser administrator = (InternalUser)userMan.findByLogin("ssgadmin");
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

    public void testSearchIdentities() throws Exception {
        String pattern = "j*";
        System.out.println("SEARCHING USERS " + pattern);
        Collection headers = idProvider.search(new EntityType[] {EntityType.GROUP, EntityType.USER}, pattern);
        for (Iterator i = headers.iterator(); i.hasNext();) {
            EntityHeader header = (EntityHeader)i.next();
            System.out.println(header);
            User usr = idProvider.getUserManager().findByPrimaryKey(header.getStrId());
            System.out.println(usr);
        }
        System.out.println("DONE SEARCHING USERS");
    }

    public void testAxisVersion() throws Exception {
        Call call = (Call)(new org.apache.axis.client.Service()).createCall();
        call.setTargetEndpointAddress(new java.net.URL("https://localhost:8443/ssg/services/Version"));
        call.setUsername("ssgadmin");
        call.setPassword("ssgadminpasswd");
        call.setOperationName(new QName("", "getVersion"));
        call.setReturnClass(String.class);
        call.setMaintainSession(true);
        for (int i = 0; i < (10*6); i++) {
            call.invoke(new Object[]{});
            //System.out.println((String)call.invoke(new Object[]{}));
        }
    }

    public static void main(String[] args) throws Exception {
        IdentityProviderClientTest testee = new IdentityProviderClientTest();
        testee.testSearchIdentities();
        System.exit(0);
    }

    IdentityProviderClient idProvider;
}
