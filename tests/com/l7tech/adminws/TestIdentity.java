package com.l7tech.adminws;

import com.l7tech.identity.imp.IdentityProviderConfigManagerClient;
import com.l7tech.identity.*;
import com.l7tech.util.Locator;

import java.util.Collection;
import java.util.Iterator;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 * Test to see if the admin service is up and running and that the stub works
 */
public class TestIdentity {
    public static void main(String[] args) throws Exception {

        testGetProviders();

        /*
        testLocator2();

        IdentityService service = new IdentityServiceLocator();
        Identity servicePort = service.getidentities(new java.net.URL("http://localhost:8080/ssg/services/identities"));
        com.l7tech.adminws.identity.IdentityProviderConfig svcConfig = new com.l7tech.adminws.identity.IdentityProviderConfig();
        svcConfig.setDescription("description");
        svcConfig.setName("name");
        svcConfig.setOid(123);
        svcConfig.setTypeClassName("type class name");
        svcConfig.setTypeDescription("type description");
        svcConfig.setTypeName("type name");
        svcConfig.setTypeOid(456);
        long res = servicePort.saveIdentityProviderConfig(svcConfig);
        System.out.println(res);
        */

        /*
        IdentityService service = new IdentityServiceLocator();
        Identity servicePort = service.getidentities(new java.net.URL("http://localhost:8080/ssg/services/identities"));
        Header[] res = servicePort.findAlllIdentityProviderConfig();
        printres(res);
        for (int i = 0; i < res.length; i++) {
            com.l7tech.adminws.identity.IdentityProviderConfig ipc = servicePort.findIdentityProviderConfigByPrimaryKey(res[i].getOid());
            System.out.println("IPC" + ipc.getDescription());
            System.out.println(ipc.toString());
        }

        */

        /*

        IdentityProviderConfigManagerClient manager = new IdentityProviderConfigManagerClient();
        java.util.Collection col = manager.findAllHeaders();
        printCollection(col);
        */

    }

    public static void printres(com.l7tech.objectmodel.EntityHeader[] res) {
        for (int i = 0; i < res.length; i++) {
            System.out.println(res[i].getName() + " - " + res[i].getType());
        }
    }
    public static void printCollection(Collection col) {
        Iterator iter = col.iterator();
        while(iter.hasNext()){
            System.out.print(iter.next());
        }
    }

    public static void testLocator() throws Exception {
        //if (identityProviderConfigManager == null){
            // instantiate the server-side manager
            IdentityProviderConfigManager identityProviderConfigManager = (IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
            if (identityProviderConfigManager == null) throw new java.rmi.RemoteException("Cannot instantiate the IdentityProviderConfigManager");
        //}
        //return identityProviderConfigManager;
    }

    public static void testGetProviders() throws Exception {
        IdentityProviderConfigManagerClient manager = new IdentityProviderConfigManagerClient();
        Collection res = manager.findAllIdentityProviders();
        Iterator it = res.iterator();
        while (it.hasNext()) {
            System.out.println("Provider found");
            System.out.println(it.next());
            IdentityProvider provider = (IdentityProvider)it.next();
            UserManager users = provider.getUserManager();
            Collection res2 = users.findAllHeaders();
            Iterator it2 = res2.iterator();
            while (it2.hasNext()) {
                System.out.println("User found");
                System.out.println(it2.next());
            }
            GroupManager groups = provider.getGroupManager();
            res2 = groups.findAllHeaders();
            it2 = res2.iterator();
            while (it2.hasNext()) {
                System.out.println("Group found");
                System.out.println(it2.next());
            }
        }
    }

}
