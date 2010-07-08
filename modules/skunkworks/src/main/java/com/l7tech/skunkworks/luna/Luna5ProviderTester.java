package com.l7tech.skunkworks.luna;

import com.l7tech.util.Functions;
import com.safenetinc.luna.provider.LunaProvider;
import org.junit.Test;

import java.security.Provider;
import java.util.*;

/**
 * Standalone test for Luna 5.0.0 provider (with completely new package names from pre-5.0 Luna providers).
 */
public class Luna5ProviderTester {
    /*

     Notable service names (from output) for future reference:

Cipher.RSA//NoPadding
Cipher.RSA//OAEPWithSHA1AndMGF1Padding
Cipher.RSA//PKCS1v1_5

     */

    @Test
    public void testProvider() throws Exception {
        List<String> services = Functions.map(new LunaProvider().getServices(), new Functions.Unary<String, Provider.Service>() {
            @Override
            public String call(Provider.Service service) {
                return String.format("%s.%s", service.getType(), service.getAlgorithm());
            }
        });

        Collections.sort(services);

        for (String service : services) {
            System.out.println(service);
        }
    }
}
