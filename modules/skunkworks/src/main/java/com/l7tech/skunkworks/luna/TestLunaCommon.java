package com.l7tech.skunkworks.luna;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility methods for Luna test code.
 */
public class TestLunaCommon {
    public static char[] getTokenPin() {
        final String tokenpin = System.getProperty("com.l7tech.lunaPin", null);
        if (tokenpin == null) throw new IllegalStateException("Please set system property com.l7tech.lunaPin to the token PIN (ie, ///6-6KWT-SCMH-N3FE)");
        return tokenpin.toCharArray();
    }

    public static void showKey(String label, Key key) {
        String alg = key.getAlgorithm();
        String kind;
        if (key instanceof PublicKey) {
            kind = "public key";
        } else if (key instanceof PrivateKey) {
            kind = "private key";
        } else if (key instanceof SecretKey) {
            kind = "secret key";
        } else {
            kind = "unknown key type";
        }
        final String format = key.getFormat();
        String encoded = getEncoded(key);
        System.out.printf("%s %s %s: %s / %s / %s\n", label, alg, kind, key.getClass().getName(), format, encoded);
    }

    static String getEncoded(Key key) {
        try {
            return HexUtils.hexDump(key.getEncoded());
        } catch (Exception e) {
            final Throwable root = ExceptionUtils.unnestToRoot(e);
            String name = root.getClass().getName();
            String msg = ExceptionUtils.getMessage(root);
            return "<getEncoded() failed: " + name + ": " + msg + ">";
        }
    }

    public static void showServices(Provider prov) {
        System.out.println("Services available from provider " + prov.getName() + ": ");
        List<Provider.Service> servs = new ArrayList<Provider.Service>(prov.getServices());
        Collections.sort(servs, new Comparator<Provider.Service>() {
            @Override
            public int compare(Provider.Service a, Provider.Service b) {
                int r = a.getType().compareTo(b.getType());
                return r != 0 ? r : a.getAlgorithm().compareTo(b.getAlgorithm());
            }
        });
        for (Provider.Service serv : servs) {
            System.out.printf("   %-17s\t%-37s\t%-80s\n", serv.getType(), serv.getAlgorithm(), serv.getClassName());
        }
    }
}
