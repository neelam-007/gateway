package com.l7tech.kerberos;

import org.apache.commons.codec.binary.Base64;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.security.Principal;
import java.util.Map;

/**
 * User: awitrisna
 */
public class MockKrb5LoginModule implements LoginModule {

    private Subject subject;

    /**
     * Kerberos principal object captured by real KDC Connection
     */
    private static final String KERBEROS_PRINCIPAL =
            "rO0ABXNyAC5qYXZheC5zZWN1cml0eS5hdXRoLmtlcmJlcm9zLktlcmJlcm9zUHJpbmNpcGFsmad9X" +
                    "Q8eMykDAAB4cHVyAAJbQqzzF/gGCFTgAgAAeHAAAAAkMCKgAwIBAaEbMBkbBGh0dHAbEWF3aXRya" +
                    "XNuYS1kZXNrdG9wdXEAfgACAAAADxsNUUFXSU4yMDAzLkNPTXg=";

    /**
     * TGT object captured by real KDC Connection
     */
    private static final String KERBEROS_TICKET =
            "rO0ABXNyACtqYXZheC5zZWN1cml0eS5hdXRoLmtlcmJlcm9zLktlcmJlcm9zVGlja2V0ZqGBbXB3" +
                    "w7sCAApbAAxhc24xRW5jb2Rpbmd0AAJbQkwACGF1dGhUaW1ldAAQTGphdmEvdXRpbC9EYXRlO0wA" +
                    "BmNsaWVudHQAMExqYXZheC9zZWN1cml0eS9hdXRoL2tlcmJlcm9zL0tlcmJlcm9zUHJpbmNpcGFs" +
                    "O1sAD2NsaWVudEFkZHJlc3Nlc3QAF1tMamF2YS9uZXQvSW5ldEFkZHJlc3M7TAAHZW5kVGltZXEA" +
                    "fgACWwAFZmxhZ3N0AAJbWkwACXJlbmV3VGlsbHEAfgACTAAGc2VydmVycQB+AANMAApzZXNzaW9u" +
                    "S2V5dAAmTGphdmF4L3NlY3VyaXR5L2F1dGgva2VyYmVyb3MvS2V5SW1wbDtMAAlzdGFydFRpbWVx" +
                    "AH4AAnhwdXIAAltCrPMX+AYIVOACAAB4cAAAA75hggO6MIIDtqADAgEFoQ8bDVFBV0lOMjAwMy5D" +
                    "T02iIjAgoAMCAQKhGTAXGwZrcmJ0Z3QbDVFBV0lOMjAwMy5DT02jggN4MIIDdKADAgEXoQMCAQKi" +
                    "ggNmBIIDYsa4fpG7g216nAfnYaJyLjGQDd2AWUbdY6J2nGuUa1zbMktiQXF4cYgPp7s5qchnBx+1" +
                    "2zgMUrsFhhz26qA6ApJXmOUAGeJrTvpQRkrZwaByTmXPaCY28tyYf4+Q3EGaKh0G3J0YH/z/2I1E" +
                    "Clm/HhHSFGLVStAyMpOUFNYn9CqOWBtv2ZYHRzEqzmmBeBshbKUdMLiK9VVfFZIk+zcoaNbtl5NS" +
                    "cjpQon+Ifv6J3DbgAHLJK11TqNnwonI7r9cEt6eXV0fvHhei/rj/JX9nEIlJ3FC8WCOKT5i5VQJ6" +
                    "A68EfWgpmFb9B18zwsyZB6qtHBxEWqAuexJ9n3OZbOWHVZuhvfm6e985agSzO3aik9UbGkkboQww" +
                    "OssVg+0lcv+tFdbOpLqO8uL2IN9Ol9RiodhZyz4KZrLbjC1OZzh9ujBZHVqqW2vFowRcHSujtzJs" +
                    "rFRNKYSzRBKm70GmzyF0EvkwFFFqUqE4dVI8YKXzNPFZKXxS7i3czK40m17XjAfDZExAgpgaQXZW" +
                    "FK9KsCY81JaIASrdzxJVwSfLiuJsfCB9gcTD2uK887gbbG3D4SgCh4nqzyFoJijDNXNSdxdE11T3" +
                    "h7MJ3pnmdj/L3D2Jc0WPhn3lVSRblBHWMp3tMIFmZXhMDcVKZcyqdCANE0mEmBEGjfDix7XpYNQD" +
                    "Qbf5atzVxkD8+AkZ3eZXGvceM+1XP3vFzoXFAmnK/fXEZKjuyuEq2dMvWyGqZX2SBXcgnDAvFP1f" +
                    "O0Uk2TEgLHnGnl6EV1A/urRDIHf+0qQp5n8q87kNmLLpjAUPzJP0sl5Z71jy3Pb5aqCo1i6HjCu9" +
                    "TyqkEFgZ2++OrF7/nVnAg7fUJ61lPNOLMTHPAEL7WDpFo7uic/Llyo5D7cI5CnYZ3p25Zxi4cKfI" +
                    "cMXDbhuheYRWA6LsIcIz70bHIW8zEjilCLMpoHQIC9L+Ab/ABh5VwLHA5jeEgNY/9Py/sB8aNoOk" +
                    "fLI5ShlfY2enGGnpV47InphRJaCLeySg/sEzD6zdZMrCetxnSevEBzJJbL4gnaaZs9SnPhttn6ZT" +
                    "JqUE4kefMBeEFbukdxBGfZnvuANLrjWoPoK206RSI8bj/WSuYe24KMa/AHDL9Yy9XZBU890ypklr" +
                    "Drdw1rUpxQzDQP9a0GbfuTrOc3IADmphdmEudXRpbC5EYXRlaGqBAUtZdBkDAAB4cHcIAAABNxPh" +
                    "HEh4c3IALmphdmF4LnNlY3VyaXR5LmF1dGgua2VyYmVyb3MuS2VyYmVyb3NQcmluY2lwYWyZp31d" +
                    "Dx4zKQMAAHhwdXEAfgAIAAAAJDAioAMCAQGhGzAZGwRodHRwGxFhd2l0cmlzbmEtZGVza3RvcHVx" +
                    "AH4ACAAAAA8bDVFBV0lOMjAwMy5DT014cHNxAH4ACncIAAABNxYGbUh4dXIAAltaV48gORS4XeIC" +
                    "AAB4cAAAACAAAAAAAAAAAAABAQAAAAAAAAAAAAAAAAAAAAAAAAAAAHBzcQB+AAx1cQB+AAgAAAAi" +
                    "MCCgAwIBAqEZMBcbBmtyYnRndBsNUUFXSU4yMDAzLkNPTXVxAH4ACAAAAA8bDVFBV0lOMjAwMy5D" +
                    "T014c3IAJGphdmF4LnNlY3VyaXR5LmF1dGgua2VyYmVyb3MuS2V5SW1wbJKDhug8r0vXAwAAeHB1" +
                    "cQB+AAgAAAAbMBmgAwIBF6ESBBDYvHuPpXmxKditbIuFp3noeHNxAH4ACncIAAABNxPhHEh4";

    private static final String KEY_BYTES = "iEb36u6PsRetBr3YMLdYbA==";
    private static final int KEY_TYPE = 23;
    private static final int VERSION_NUMBER = 2;


    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
    }

    @Override
    public boolean login() throws LoginException {
        try {
            subject.getPrincipals().add((Principal) KerberosClientTest.decode(KERBEROS_PRINCIPAL));
            subject.getPrivateCredentials().add(KerberosClientTest.decode(KERBEROS_TICKET));
            //subject.getPrivateCredentials().add(new Keytab(tmp));
            KerberosKey key = new KerberosKey((KerberosPrincipal) KerberosClientTest.decode(KERBEROS_PRINCIPAL), Base64.decodeBase64(KEY_BYTES), KEY_TYPE, VERSION_NUMBER);
            subject.getPrivateCredentials().add(key);
            /* Required JDK1.7
            Krb5Util.KeysFromKeyTab keysFromKeyTab = new Krb5Util.KeysFromKeyTab(key);
            subject.getPrivateCredentials().add(keysFromKeyTab);
            */
            return true;
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return true;
    }

}
