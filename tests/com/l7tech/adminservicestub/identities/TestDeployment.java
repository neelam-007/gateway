package com.l7tech.adminservicestub.identities;

import com.l7tech.adminservicestub.ListResultEntry;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 7, 2003
 *
 * This class tests that the service is deployed locally and that the
 * stub successfuly consumes the service.
 */
public class TestDeployment {
    public static void main(String[] args) throws Exception {
        IdentityWSService service = new IdentityWSServiceLocator();
        IdentityWS servicePort = service.getidentities(new java.net.URL("http://localhost:8080/ssg/services/identities"));

        ListResultEntry[] res = servicePort.listProviders();
        printres(res);

        res = servicePort.listGroupsInProvider(654);
        printres(res);

        res = servicePort.listUsersInProvider(654);
        printres(res);
    }

    public static void printres(ListResultEntry[] res) {
        for (int i = 0; i < res.length; i++) {
            System.out.println(res[i].getName());
        }
    }
}
