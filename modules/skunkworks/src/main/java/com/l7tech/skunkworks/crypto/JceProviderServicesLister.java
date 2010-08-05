package com.l7tech.skunkworks.crypto;

import java.security.Provider;
import java.security.Security;
import java.util.Set;

/**
 * Lists JCE services.
 */
public class JceProviderServicesLister {
    public static void main(String[] args) {
        Provider[] provs = Security.getProviders();
        for (Provider prov : provs) {
            Set<Provider.Service> servs = prov.getServices();
            for (Provider.Service serv : servs) {
                System.out.println(serv.getType() + "." + serv.getAlgorithm() + " \t\t" + serv.getClassName());
            }
        }
    }
}
