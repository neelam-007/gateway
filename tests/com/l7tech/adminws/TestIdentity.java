package com.l7tech.adminws;

import com.l7tech.adminws.identity.IdentityService;
import com.l7tech.adminws.identity.IdentityServiceLocator;
import com.l7tech.adminws.identity.Identity;
import com.l7tech.adminws.identity.Header;
import com.l7tech.identity.imp.IdentityProviderConfigManagerClient;
import com.l7tech.identity.IdentityProviderConfig;

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
        IdentityService service = new IdentityServiceLocator();
        Identity servicePort = service.getidentities(new java.net.URL("http://localhost:8080/UneasyRooster/services/identities"));
        //Identity servicePort = service.getidentities(new java.net.URL("http://localhost:8080/ssg/services/identities"));
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
}
