package com.l7tech.adminws;

import com.l7tech.adminws.identity.IdentityService;
import com.l7tech.adminws.identity.IdentityServiceLocator;
import com.l7tech.adminws.identity.Identity;
import com.l7tech.adminws.identity.Header;
import com.l7tech.identity.imp.IdentityProviderConfigManagerClient;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
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

        IdentityService service = new IdentityServiceLocator();
        Identity servicePort = service.getidentities(new java.net.URL("http://localhost:8080/ssg/services/identities"));
        Header[] res = servicePort.findAlllIdentityProviderConfig();
        printres(res);

        /*
        IdentityProviderConfigManagerClient manager = new IdentityProviderConfigManagerClient();
        java.util.Collection col = manager.findAllHeaders();
        printCollection(col);
        */
    }

    public static void printres(Header[] res) {
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

    public static void testLocator2() throws Exception {
        /*
        com.l7tech.adminws.identity.IdentityProviderConfig svcConfig = new com.l7tech.adminws.identity.IdentityProviderConfig();
        svcConfig.setDescription("description");
        svcConfig.setName("name");
        svcConfig.setOid(123);
        svcConfig.setTypeClassName("type class name");
        svcConfig.setTypeDescription("type description");
        svcConfig.setTypeName("type name");
        svcConfig.setTypeOid(456);
        com.l7tech.adminws.identity.IdentitiesSoapBindingImpl service = new com.l7tech.adminws.identity.IdentitiesSoapBindingImpl();
        service.saveIdentityProviderConfig(svcConfig);
        */

        IdentityProviderConfigManagerClient manager = new IdentityProviderConfigManagerClient();

        com.l7tech.identity.IdentityProviderConfig genConfig = new com.l7tech.identity.imp.IdentityProviderConfigImp();
        genConfig.setDescription("description");
        genConfig.setName("name");
        genConfig.setOid(123);
        com.l7tech.identity.IdentityProviderType configType = new com.l7tech.identity.imp.IdentityProviderTypeImp();
        configType.setClassName("blah");
        configType.setDescription("description");
        configType.setName("name");
        configType.setOid(123);
        genConfig.setType(configType);

        manager.save(genConfig);
    }

}
