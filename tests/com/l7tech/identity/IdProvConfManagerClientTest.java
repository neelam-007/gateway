package com.l7tech.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.console.security.ClientCredentialManager;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;

import java.util.Collection;
import java.util.Iterator;
import java.net.PasswordAuthentication;
import java.io.File;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 23, 2003
 *
 */
public class IdProvConfManagerClientTest {

    public static void testListContentOfInternalIDProvider(IdProvConfManagerClient testee) throws Exception {
        IdentityProvider provider = testee.getInternalIdentityProvider();
        UserManager usermanager = provider.getUserManager();
        Collection userheaders = usermanager.findAllHeaders();
        Iterator i = userheaders.iterator();
        while (i.hasNext()) {
            EntityHeader obj = (EntityHeader)i.next();
            System.out.println(obj);
        }
        GroupManager groupManager = provider.getGroupManager();
        Collection headers = groupManager.findAllHeaders();
        i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader obj = (EntityHeader)i.next();
            System.out.println(obj);
        }
    }

    public static String testCreateGroup(IdProvConfManagerClient testee) throws Exception {
        IdentityProvider provider = testee.getInternalIdentityProvider();
        GroupManager groupMan = provider.getGroupManager();
        InternalGroup newgrp = new InternalGroup();
        System.out.println("creating group");
        newgrp.setName("thepolice");
        newgrp.setDescription("70s and early 80s rock");
        System.out.println("saving group");
        String grpId = groupMan.save(newgrp);
        System.out.println("group saved oid=" + grpId);
        return grpId;
    }

    public static void testAssignUserToGroup(IdProvConfManagerClient testee, String groupid) throws Exception {
        IdentityProvider provider = testee.getInternalIdentityProvider();
        GroupManager groupMan = provider.getGroupManager();
        System.out.println("get existing group");
        Group thepolice = groupMan.findByPrimaryKey(groupid);
        System.out.println("create new user");
        UserManager userMan = provider.getUserManager();
        InternalUser newUser = new InternalUser();
        newUser.setName("stewart");
        newUser.setLogin("scopeland");
        newUser.setPassword("drumer");
        System.out.println("saving new user");
        String usrid = userMan.save(newUser);
        System.out.println("add user to group");
        //thepolice.getMembers().add(newUser);
        System.out.println("update the group");
        groupMan.update(thepolice);
        System.out.println("done");
    }

    public static void updateGroup(IdProvConfManagerClient testee) throws Exception {
        IdentityProvider provider = testee.getInternalIdentityProvider();
        GroupManager groupMan = provider.getGroupManager();
        System.out.println("get existing group");
        InternalGroup thepolice = (InternalGroup)groupMan.findByPrimaryKey("4718592");

        System.out.println(thepolice);
        thepolice.setDescription("completly different description");

        System.out.println("update group");
        groupMan.update(thepolice);
        System.out.println("done");
    }

    public static void updateUser(IdProvConfManagerClient testee) throws Exception {
        IdentityProvider provider = testee.getInternalIdentityProvider();
        UserManager userMan = provider.getUserManager();
        System.out.println("retrieving existing user");
        InternalUser stewart = (InternalUser)userMan.findByPrimaryKey("5046272");
        System.out.println("change it");
        stewart.setEmail("completly different email address");
        System.out.println("update it");
        userMan.update(stewart);
        System.out.println("done");
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.ssl.trustStore", System.getProperties().getProperty("user.home") + File.separator + ".l7tech" + File.separator + "trustStore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        ClientCredentialManager credsManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        PasswordAuthentication creds = new PasswordAuthentication("ssgadmin", "ssgadminpasswd".toCharArray());
        credsManager.login(creds);

        IdProvConfManagerClient manager = new IdProvConfManagerClient();
        testListContentOfInternalIDProvider(manager);
        String grp = testCreateGroup(manager);
        testAssignUserToGroup(manager, grp);
        //updateGroup(manager);
        //updateUser(manager);

        System.exit(0);
    }
}
