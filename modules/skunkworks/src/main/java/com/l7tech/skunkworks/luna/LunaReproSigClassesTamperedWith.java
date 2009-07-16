package com.l7tech.skunkworks.luna;

import com.chrysalisits.crypto.LunaJCAProvider;
import com.chrysalisits.crypto.LunaTokenManager;
import com.chrysalisits.cryptox.LunaJCEProvider;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.Security;

/**
 *
 */
public class LunaReproSigClassesTamperedWith {
    public static void main(String[] args) throws Exception {
        Security.insertProviderAt(new LunaJCEProvider(), 1);
        Security.insertProviderAt(new LunaJCAProvider(), 1);
        LunaTokenManager.getInstance().Login("FGAA-3LJT-tsHW-NC3E");
        LunaTokenManager.getInstance().SetSecretKeysExtractable(true);

        KeyStore.getInstance("PKCS12").load(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(P12_B64)), "password".toCharArray());
    }

    private static final String P12_B64 =
            "MIACAQMwgAYJKoZIhvcNAQcBoIAkgASCA+gwgDCABgkqhkiG9w0BBwGggCSABIIDDTCCAwkwggMF\n" +
            "BgsqhkiG9w0BDAoBAqCCArIwggKuMCgGCiqGSIb3DQEMAQMwGgQULF6vVC6fCvwmsX6HaJ9yTpKT\n" +
            "BSACAgQABIICgKmfQn9YNsmdOseCLajJ52XlSmfRsiQIdRwsXuG/uL8x+sPWuVy+oLtu6SdHjrJz\n" +
            "0bFR+6eoO48NyGFFbgBX+22RBJG0HLZ6c351bRU6QiTifBpbucOjjwJYxATfI2OxeWPFjJGp8ApA\n" +
            "EF3Fa3q3TlxyBC1Jq6DSrUC78Og0Ei7ESoshiCJgH9QgzNgC/2UQQW87K4WrdIS59V4IdnPY4bsS\n" +
            "E/oKbexT2KIgobsnO2p0hSPZYEIV4rKQACWQ4oJCJCTBvzDm9Rs7o0rShMNxXm3pT5QLP00HN1So\n" +
            "YZtJIcnJ/vtqyELbpjuiVsGPFA8WIutYru34fTaw1ece69jaRpGUiBY2nXc2HAocJqJBwbfGTVCY\n" +
            "kUi9XtJuoXKvSSatHG/h6HHB69kwbAxXzMGMQGoWnIbWrcmNxZGmyhFn6mXLRxq5ziW7W4EelVl7\n" +
            "5ZTkFmYayJ4GHmC0O6IWczSAurYvYOMtT4A6GGie2+DHbgOWpFHZmn0cVeGKMb0dHm3CkfjsMfKy\n" +
            "98srSEwFRH68gUzvQiMb4mscygGSECmQVB81DSNZaxyX08lLjTXBQIaMgl1R10baYPiX+gqJ5xTQ\n" +
            "yJ32le5i2j3atoo2vb/wySFzqT1us8Oes0cUztIoP0Yb/G0I2EVFJxCL/s/OlDZRz4SxKcYmgfxw\n" +
            "6yHEflQ7+RrGco4saFz5VKhO9obG0Cl/jZ7Sy/NPR1VZ2JGmZP+XQbADz3+ZPidx7xVJOck4XegB\n" +
            "WtQccMl8Q3yHijsAkajbxs+qYWfdHiPodoNadZD4cct+58vVK3dWqPfHYwdu0WMBymbEdgaJaGII\n" +
            "Dm1NAdtZjXwMkVhqemTQMRz7ObSScooxQDAZBgkqhkiG9w0BCRQxDB4KAGUAbgB0AHIAeTAjBgkq\n" +
            "hkiG9w0BCRUxFgQUtasXfGTRK+rGIN8lSfQOkTVBfsQAAAAAAAAwgAYJKoZIhvcNAQcGoIAwgAIB\n" +
            "ADCABgkqhkiG9w0BBwEwKAYKKoZIhvcNAQwBBjAaBBRf0IAEGvw/tKT5wJi1LItAjyYOrQICBACg\n" +
            "gASCAiiY2pkOrtA+9AaszrYYg1RNH2+lYPQZpssR4I0DyvrEAcX7r7VgGl5gv9s9L1StQkGMv/Ld\n" +
            "pxJcy/G90ChV+hKz+z7G4gL6obk4N561+jUPf/+iGsxPonqaXDhDIrmw0Nt4JOf05JRoamKXASbU\n" +
            "BIIBxwJSxGwEQTqWDoMYNtDskHYVP+Khcn36q3MXCshJ2fU3csiZu6l7wZNqGcd23OQegbExtRmz\n" +
            "sQ7ZHfAOrdpLkaw1K3ijutucmgPCwdvWdGhTk1/hyyaPUPMll+Qxq5zPsKTYYPUlHtFHM6QR5ipo\n" +
            "fgBlGsq0SZLyXn6xEBNu4k8yxdxDmZ3Zg3mNhjqqSGixej6t3QUawLsiXiqivuYdUSy+6Eu7R2in\n" +
            "VmSd5lqfCxZVbsSRv8XXeC/pXLZAc/VCtyOsvjDQYVKiLMdfwLM6QpMJUTeyP87OkzgJ6tWwTejL\n" +
            "dYrY1R7k+8+FL1hE19/djwJB7M+Cbye7r+acmp96OwLZtuS8gNZkl1mOPWqMesD+SqgW0HX1lXve\n" +
            "1vu3galGkHYfSqEuHOvbCx+gvzHyWBqK7nw4/wuKVBNIbmSbRC2YtFPpDabbZ9BAG2n9LD50ukMu\n" +
            "DkggpAFl8lh8kNtBpoJ2w+B4zwVizTZ8IAtowDz8ezGaBTbLg7RFYcMTrHtOHgTu0PNLzIzbtLCl\n" +
            "8nlrGfZQkcQcVY0KKnrzjjBrH7JFhY3k2Lzk7P0fODMtkNJRXYZkC0ialCiCuwnyAAAAAAAAAAAA\n" +
            "AAAAAAAAAAAAMD0wITAJBgUrDgMCGgUABBSD0dLvfMNocQlwH9y5/8Vppp72kAQUZDwFIgF1UeBf\n" +
            "qw1384lyXXbpq5MCAgQAAAA=";
}
