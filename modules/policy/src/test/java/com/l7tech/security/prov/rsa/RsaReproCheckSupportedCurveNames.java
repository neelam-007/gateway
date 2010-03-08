package com.l7tech.security.prov.rsa;

import com.l7tech.util.ExceptionUtils;
import com.rsa.jsafe.provider.JsafeJCE;

import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

/**
 *
 */
public class RsaReproCheckSupportedCurveNames {
    public static void main(String[] args) throws Exception {
        Security.insertProviderAt(new JsafeJCE(), 1);

        String[] curves = {
                curvename("sect163k1"),
                //curvename("sect163r1"),
                curvename("sect163r2"),
                //curvename("sect193r1"),
                //curvename("sect193r2"),
                curvename("sect233k1"),
                curvename("sect233r1"),
                //curvename("sect239k1"),
                curvename("sect283k1"),
                curvename("sect283r1"),
                curvename("sect409k1"),
                curvename("sect409r1"),
                curvename("sect571k1"),
                curvename("sect571r1"),
                //curvename("secp160k1"),
                //curvename("secp160r1"),
                //curvename("secp160r2"),
                //curvename("secp192k1"),
                curvename("secp192r1"),
                //curvename("secp224k1"),
                curvename("secp224r1"),
                //curvename("secp256k1"),
                curvename("secp256r1"),
                curvename("secp384r1"),
                curvename("secp521r1"),
                curvename("prime192v1"),
                curvename("prime256v1"),
                curvename("K-163"),
                curvename("B-163"),
                curvename("K-233"),
                curvename("B-233"),
                curvename("K-283"),
                curvename("B-283"),
                curvename("K-409"),
                curvename("B-409"),
                curvename("K-571"),
                curvename("B-571"),
                curvename("P-192"),
                curvename("P-224"),
                curvename("P-256"),
                curvename("P-384"),
                curvename("P-521")
        };

        for (String curve : curves) {
            checkCurve(curve);
        }
    }

    static String curvename(String curvename) { return curvename; }

    static boolean checkCurve(String curveName) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec(curveName));
            kpg.generateKeyPair();
            System.out.println("  " + curveName);
            return true;
        } catch (Exception e) {
            System.out.println("!-" + curveName + "\t\t" + ExceptionUtils.getMessage(e));
            return false;
        }
    }

}
