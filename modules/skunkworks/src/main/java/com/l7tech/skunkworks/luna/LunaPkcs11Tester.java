package com.l7tech.skunkworks.luna;

import org.junit.*;
import sun.security.pkcs11.wrapper.PKCS11;
import sun.security.pkcs11.wrapper.PKCS11Exception;
import sun.security.pkcs11.wrapper.CK_ATTRIBUTE;
import sun.security.pkcs11.wrapper.PKCS11Constants;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_CLASS;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_TOKEN;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_EXTRACTABLE;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_DECRYPT;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_SIGN;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_UNWRAP;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_KEY_TYPE;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_MODULUS;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_PUBLIC_EXPONENT;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_PRIVATE_EXPONENT;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_PRIME_1;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_PRIME_2;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_EXPONENT_1;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_EXPONENT_2;
import static sun.security.pkcs11.wrapper.PKCS11Constants.CKA_COEFFICIENT;

import java.io.IOException;
import java.math.BigInteger;

/**
 *
 */
public class LunaPkcs11Tester {

    /** PKCS#11 wrapper */
    public static PKCS11 p;

    /** Our session handle */
    public static long s;

    private BigInteger hex(String hex) { return new BigInteger(hex, 16); }

    @BeforeClass
    public static void beforeClass() throws IOException, PKCS11Exception {
        LunaRawPkcs11 raw = new LunaRawPkcs11();
        p = raw.p;
        s = raw.s;
    }

    @AfterClass
    public static void afterClass() throws PKCS11Exception {
        if (p != null) {
            p.C_CloseSession(s);
            p = null;
            s = 0;
        }
    }

    @Test
    public void testGetInfo() throws PKCS11Exception {
        System.out.println("PKCS#11 info: " + p.C_GetInfo());
    }


    @Test
    public void testListObjects() throws PKCS11Exception {
        // List all objects
        CK_ATTRIBUTE[] template = new CK_ATTRIBUTE[] {
        };
        p.C_FindObjectsInit(s, template);

        long[] found = p.C_FindObjects(s, 100000);

        System.out.printf("Token contains %d objects\n", found.length);

        for (long handle : found) {
            template = new CK_ATTRIBUTE[] {
                    new CK_ATTRIBUTE(PKCS11Constants.CKA_ID),
                    new CK_ATTRIBUTE(PKCS11Constants.CKA_LABEL)
            };
            p.C_GetAttributeValue(s, handle, template);
            System.out.printf("handle %d %s / %s\n", handle, template[0].toString(), template[1].toString());
        }
    }

    @Test
    @Ignore("Not finished")
    public void testCreateObject_DES() throws Exception {
        CK_ATTRIBUTE[] template = new CK_ATTRIBUTE[] {
                // TODO
        };

        long objectHandle = p.C_CreateObject(s, template);
        System.out.println("Successfully created DES secret key: " + objectHandle);

        p.C_DestroyObject(s, objectHandle);
        System.out.println("Successfully destroyed DES secret key");
    }

    /*  The following failure prevented successful verification of an XML message signed with RSA+AES using SunPKCS11-Luna
16:44:23 01816-1676:STRTCreateObject {Sesn=2 AttrList={
CKA_TOKEN="01"
CKA_EXTRACTABLE="00"
CKA_DECRYPT="01"
CKA_SIGN="01"
CKA_UNWRAP="01"
CKA_CLASS="03000000"
CKA_KEY_TYPE="00000000"
CKA_MODULUS="c0aab8cbdae25143aefddf5d09ca2bc0a4494ec7bd5674512c89b652ffca19871ff1505c7c9863e7d3712c96608b5625889367df4f01b0fee8428119b75fb8935424219a4d67396435084aa33f486b5a7187bf3c3ff8fa0decc0d3f34d44002012d1635a64af6726dece4287f7fe99854d051e351e705e231c9bc36e6492833b"
CKA_PUBLIC_EXPONENT="010001"
CKA_PRIVATE_EXPONENT="bf4cc410255571baf295c0a27085fccb5a542f94c3ba83e3d585273362271911c1f5a9052bf163c15b8093f4fc075d9206f9d5b934894964d0d8b7b7010c5a06b48fd8ce24b9030e85dfa47987e47193bcd6bd9b6e353c643e933014745bc5cd1d2a5130e5063938a58a1eaa82414e03591aeddbdc11c46f3eceecf009510101"
CKA_PRIME_1="f11c2b7fbfe94a23c7da97d4e52b50efbfb552f6b9547a43a0a2c2e668008223ef426db20414979303f05c711e45810e7c7c9db42aee750637e9f54fe0ecb519"
CKA_PRIME_2="cc90b08e822231306734b52972dbfae9691418dc2bdf4fc9d0462b146e7bfa346690faf6c88000be18ab25e5e008b88441c77bb1717897b85b3a7a17403a9173"
CKA_EXPONENT_1="a90bc41fcc71074f30e73cfec2326d8bba90a9fb5c1333a9c8ad7512d026531858beed641b6248a785e2771dc4489ce4883962e498191a2962f880cb217b34b9"
CKA_EXPONENT_2="7cd46e6908104563fc16b5d62d38ffcc1f4b1c407ea52ed3403d115cce6ee927c0cf07d640539c20647d4c0b12c33f95b56e3d94f2615c742b776bb9748048f1"
CKA_COEFFICIENT="c285aaaba8bb65f160e0b51c28495986331e17cb4ab357d61e1ba73bcd2dea14148411da85ce3e0008285ce31ef8a0316be17ab9fb6333d7a0f2b00067eeaade" } }
16:44:23 01816-1676:FINICreateObject ***CKR_TEMPLATE_INCONSISTENT***(1828ms) {Obj=0 }
     */

    // Reproduces the above CKR_TEMPLATE_INCONSISTENT failure
    @Test
    @Ignore("Currently fails")
    public void testCreateObject_RSA() throws PKCS11Exception {
        CK_ATTRIBUTE[] template = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_CLASS, hex("03000000")),
                new CK_ATTRIBUTE(CKA_TOKEN, 1),
                new CK_ATTRIBUTE(CKA_EXTRACTABLE, 0),
                new CK_ATTRIBUTE(CKA_DECRYPT, 1),
                new CK_ATTRIBUTE(CKA_SIGN, 1),
                new CK_ATTRIBUTE(CKA_UNWRAP, 1),
                new CK_ATTRIBUTE(CKA_KEY_TYPE, hex("00000000")),
                new CK_ATTRIBUTE(CKA_MODULUS, hex("c0aab8cbdae25143aefddf5d09ca2bc0a4494ec7bd5674512c89b652ffca19871ff1505c7c9863e7d3712c96608b5625889367df4f01b0fee8428119b75fb8935424219a4d67396435084aa33f486b5a7187bf3c3ff8fa0decc0d3f34d44002012d1635a64af6726dece4287f7fe99854d051e351e705e231c9bc36e6492833b")),
                new CK_ATTRIBUTE(CKA_PUBLIC_EXPONENT, hex("010001")),
                new CK_ATTRIBUTE(CKA_PRIVATE_EXPONENT, hex("bf4cc410255571baf295c0a27085fccb5a542f94c3ba83e3d585273362271911c1f5a9052bf163c15b8093f4fc075d9206f9d5b934894964d0d8b7b7010c5a06b48fd8ce24b9030e85dfa47987e47193bcd6bd9b6e353c643e933014745bc5cd1d2a5130e5063938a58a1eaa82414e03591aeddbdc11c46f3eceecf009510101")),
                new CK_ATTRIBUTE(CKA_PRIME_1, hex("f11c2b7fbfe94a23c7da97d4e52b50efbfb552f6b9547a43a0a2c2e668008223ef426db20414979303f05c711e45810e7c7c9db42aee750637e9f54fe0ecb519")),
                new CK_ATTRIBUTE(CKA_PRIME_2, hex("cc90b08e822231306734b52972dbfae9691418dc2bdf4fc9d0462b146e7bfa346690faf6c88000be18ab25e5e008b88441c77bb1717897b85b3a7a17403a9173")),
                new CK_ATTRIBUTE(CKA_EXPONENT_1, hex("a90bc41fcc71074f30e73cfec2326d8bba90a9fb5c1333a9c8ad7512d026531858beed641b6248a785e2771dc4489ce4883962e498191a2962f880cb217b34b9")),
                new CK_ATTRIBUTE(CKA_EXPONENT_2, hex("7cd46e6908104563fc16b5d62d38ffcc1f4b1c407ea52ed3403d115cce6ee927c0cf07d640539c20647d4c0b12c33f95b56e3d94f2615c742b776bb9748048f1")),
                new CK_ATTRIBUTE(CKA_COEFFICIENT, hex("c285aaaba8bb65f160e0b51c28495986331e17cb4ab357d61e1ba73bcd2dea14148411da85ce3e0008285ce31ef8a0316be17ab9fb6333d7a0f2b00067eeaade")),
        };
        long objectHandle = p.C_CreateObject(s, template);
        System.out.println("Successfully created object #" + objectHandle);

        p.C_DestroyObject(s, objectHandle);
        System.out.println("Successfully destroyed object.");
    }
}
