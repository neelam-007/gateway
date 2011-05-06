package com.l7tech.skunkworks.crypto;

import java.security.Provider;
import java.security.Security;
import java.util.*;

/**
 * Lists JCE services.
 */
public class JceProviderServicesLister {
    public static void main(String[] args) {
        Provider[] provs = Security.getProviders();
        for (Provider prov : provs) {
            System.out.println("\nProvider: " + prov.getName() + " (" + prov + ")");
            List<Provider.Service> servs = new ArrayList<Provider.Service>(prov.getServices());
            Collections.sort(servs, new Comparator<Provider.Service>() {
                @Override
                public int compare(Provider.Service a, Provider.Service b) {
                    int r = a.getType().compareTo(b.getType());
                    return r != 0 ? r : a.getAlgorithm().compareTo(b.getAlgorithm());
                }
            });
            for (Provider.Service serv : servs) {
                System.out.printf("   %-28s\t%-37s\t%-80s\n", serv.getType(), serv.getAlgorithm(), serv.getClassName());
            }
        }
    }
}
