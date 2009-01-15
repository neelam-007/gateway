package com.l7tech.skunkworks;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;

/**
 * Test class that can generate Wssx Interop Test messages.
 */
public class WssxInteropMessageGenerator {
    private String scenario;
    private WssDecorator.DecorationResult dresult;
    private Message message;
    DecorationRequirements dreq = new DecorationRequirements();
    Document doc;

    public WssxInteropMessageGenerator() {
        reset();
    }

    public void reset() {
        scenario = "WS-SX Interop Test";
        message = null;
        dresult = null;
        dreq = new DecorationRequirements();
        dreq.setSecurityHeaderActor(null);
        dreq.setKeyEncryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p");
        try {
            doc = XmlUtil.stringToDocument(MessageFormat.format(PLAINTEXT_MESS, scenario));
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public Document generateRequest() throws IOException, SAXException, DecoratorException, InvalidDocumentFormatException, GeneralSecurityException {
        message = new Message(doc);
        WssDecoratorImpl decorator = new WssDecoratorImpl();
        dresult = decorator.decorateMessage(message, dreq);
        return message.getXmlKnob().getDocumentWritable();
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public DecorationRequirements dreq() {
        return dreq;
    }

    public static void main(String[] args) throws Exception {
        System.setProperty(WssDecoratorImpl.PROPERTY_SUPPRESS_NANOSECONDS, "true");
        WssxInteropMessageGenerator generator = new WssxInteropMessageGenerator();
        Document doc = generator.generateRequest();
        System.out.println(XmlUtil.nodeToFormattedString(doc));
    }

    private static final String PLAINTEXT_MESS = "" +
            "<s12:Envelope xmlns:s12=\"http://www.w3.org/2003/05/soap-envelope\">" +
            "    <s12:Body>\n" +
            "        <tns:EchoRequest xmlns:tns=\"http://example.com/ws/2008/09/securitypolicy\">{0}</tns:EchoRequest>\n" +
            "    </s12:Body>\n" +
            "</s12:Envelope>";


    public SignerInfo getAliceInfo() {
        return Base64Keystore.fromBase64(ALICE_PFX, "password");
    }

    public SignerInfo getBobInfo() {
        return Base64Keystore.fromBase64(BOB_PFX, "password");
    }

    private static final String ALICE_PFX =
            "MIIHCQIBAzCCBs8GCSqGSIb3DQEHAaCCBsAEgga8MIIGuDCCA7cGCSqGSIb3DQEHBqCCA6gwggOk\n" +
            "AgEAMIIDnQYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQINHgOt2kfZEECAggAgIIDcLWTu9wq\n" +
            "v9aA3uQrUVjbhNEm3xUd+nDmgNqSHvvCgCrxXCcNUZeZj6R2BaXI17AN/KGdGr9QupaRsbMJj0WD\n" +
            "/kFiEPAwBgDEc5qZHaU1qiQ+vi/tfi7ILANqLNqZbB58qUkokA2oi7TE4gVS6icdliBvC87tvvOh\n" +
            "IbZNglnKhc9kKoxfB4JYbmS3kfYW79wtrccfv1A6vhKvs1Lk6+OmvXgLH+34+D3CvU69QxGEhN1j\n" +
            "iNyboZDs8ggSFgbPMos/s/acQRZFJq1LHGoKwGFIdUccbQSHhnMvw15NqJqAJXUmvGtPTV7j5JQu\n" +
            "A1IcRPgEniteBuwuSIi95XT1c30UOwFQFNlR25y/uQX/qXZhsWmuuUAryxruH3JOvO0j3wrLpmo1\n" +
            "pb46YsUyf1gIUq2JE2nqlW4ZPpczERqwM3R8HdtMzc2ASue/2OwXZG8yEQfS9nsJi2ydVgpcoQcZ\n" +
            "aWp5V1RzSpGrjCAJ7h8v2WjtQG2FWS1PtpwtSmFYmLoHeFlGqGRKERlBnYHOpS6ZmdGJBQbrc1bn\n" +
            "LjO645ZWMASQ+jK053PddX9uXDllWSsFAnzXs92An7qFlttwMkc4CTBW291dqrrGtgokNx5OXDgA\n" +
            "fZEeIJDdWWpBdnkNHDpqqdhAnVErWk9Uyqg7ozRu+3TUyoxzOSwb4er5qUY7+XDJdzgxRlb4Kfhm\n" +
            "sfcOqF0gIy2KkFm/loK0mnWIHEE/ifRNJdpFajhdiME79NZidVtfCF6q7PGW9YjefKJlTdMCHP7j\n" +
            "uxDedtCLX5XOB+svI7yLdvAJc0xVHEQCBoUY1iOl+0VVoJq34y3vOrx1gnmnIzZMMjvbUeHgGK+1\n" +
            "MJTu4SDcyyBCmXgJ+OzSk6fwuoYVBr5Q+8D6tlCF+DVdhXak0bFO/nG1pzgqJLZFpYG+BmhyuZbR\n" +
            "kLCN9ztNGyph5dAEElR4PuwViCCIUeaVIXDdG5BZpBb6Ppsfgqcn0ZKOJUio7vAVlHhMV//VQiPJ\n" +
            "BMyzm85f1kHFtt75LiC+AsO7+FPuPAfZsLrtCvd15/qublEBjwKm17pcYcbT65NenBhxmydzytLv\n" +
            "pz5tXJmdoSBKYadPoOsW+VppeoNgWV6fH60klTC9H2P7EHRHSHwdr3UiumLMWAAFAUBg/b+uQW2Q\n" +
            "XUZxdsXTjs42p7uErCsvyX8vN70wggL5BgkqhkiG9w0BBwGgggLqBIIC5jCCAuIwggLeBgsqhkiG\n" +
            "9w0BDAoBAqCCAqYwggKiMBwGCiqGSIb3DQEMAQMwDgQIchZ5VKrvKvYCAggABIICgEiF2+GfTvxi\n" +
            "vnKZvHGE4FNAxPq3bykAB2+UUYmsFNOoX2pJjOuism2iOlJn7ATvKv9/3Ap1I3nuwh5CF3wNvwMT\n" +
            "A9q5ppoOEdXfjaBrQ06Tx/upgEfNhU6FLOEsYf8ltUubkz2cqyw/Z+PjhgDoYd789es4kRZiPAyw\n" +
            "zexLnQi5xr3IjWdP3wyqpI3CTV5wIBOFD8OROynmOhXA4OYBBnVj3Gmz5p3NRqiwgDq9pxBKby/i\n" +
            "yiZPFu4gv2d3l0a90jOfCOS0IP5Xtgu7GPyyEjdU3fYLiDexzxpTljtI5313pizjPHO/ouqbw6uY\n" +
            "knB5HiCq6bR645u+eFMQ9zJ3yftO+JX4IYPAnpjZeNPaI+BU4Oj7D7v18dfLehtF6vwPqOOLLjo4\n" +
            "ZuiPCXAzjH+R0gNdte800JGc547zO4JjCqgCkr2igziaKPPWieeAc9xGkBMl0qlI+lBPoOVuhnhY\n" +
            "4gVYRUsbkoY6OCechPeLkBhPHw7QGmkAMN/p+jRgAS6FBMbg3k40kvlZwFfuq0OQdCSMLi7nDUPl\n" +
            "1bDD58TeyI4gEhH1b+ZKg4b6RVJPXLZHE4FRSY+6zcGC36VDOxHlWB1RIHIEQIHDhV3W1JiDGIQT\n" +
            "6HqmuTBPvKBtKyPIgLDrm0lKO4uWN3fCq59eoSWoQ7Utgq4wf0psdzU8MN3i1w6D6pDCN1tHmUhJ\n" +
            "/CGY3lhlh/gJl3JWm2UhEiqqhzmHoQSEzL/PsR/60tWKb+/RSCeOUHaq/5+BcRoMiPxsvooKHfXM\n" +
            "ev7auYlJ3adfRangKidRdU7rhUjm91MJohv+y6tdbVbRZzLrgyZNvngniGcfpuPnvIoCmz0bF7/G\n" +
            "i8X9besxJTAjBgkqhkiG9w0BCRUxFgQUbg6I8267h0TUcPYvYE0D6k6+UJQwMTAhMAkGBSsOAwIa\n" +
            "BQAEFMHGVEb7ALktCwHTFrwUvM1WicllBAhBygPhEIcRvwICCAA=";

    private static final String BOB_PFX =
            "MIIHAQIBAzCCBscGCSqGSIb3DQEHAaCCBrgEgga0MIIGsDCCA68GCSqGSIb3DQEHBqCCA6AwggOc\n" +
            "AgEAMIIDlQYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQIUC1cWBDpTNMCAggAgIIDaHiCM8gU\n" +
            "ZAieHSLzVpzfv2LziT1+Gv1pmZ3Za7B0bOkMLsEf9xGQRzF73e3cqDd0KCS3O+L8yHoBnzF8eQ8l\n" +
            "tUoKPhD7/VIRnG5SSWsa0P5j7zTeWUIZ+uV0biWUMnxr1qpBm7n9FzOEldSbDQsisVIdyWgDkzsw\n" +
            "8WXrrbu4x8pHrDd8XDA+pIV7/SVxhos2W372omdw1DklJOxlrYREKDzXAqrmJ4wkm9T9CpE0r51i\n" +
            "y3Emp5AC5fMVtmWRoetbYfftYnBuRPLAz5nUUfYbKFAmlPCboKUfSrNKBT+dCNeIoYMR0PTn+tOm\n" +
            "Bzi2C72JqnxbAUn9OdPHC/lGnKW8OeF5zlo1P4WkYof+ZzQMJKwNlnveEi8QvP6AbRlAXGbmNEV/\n" +
            "+w3YcGHIvGEN0RSfhSzsDj/yWhrZRNzOEfm8ElVcseM84A6lmWH0TqhBBsXrlXYgqR1toxQirrLR\n" +
            "FoBjKdb2tLOnYkdZ4VJ5/6In7JahDvVvLIJaBc2Mj8L7MRxEco60LTbedaS6tIUlZee2khKwX0gi\n" +
            "0CdgFRfHa0hBExLlarNuiIlP/12XJGHIoNzgiI/EGZBv1+Rl+qWJSkvlyyDmTmdQXjlXu7KrlX4U\n" +
            "MSzxoQ20XQO8sKfyJFvkKBMteZ3ekM90YmDLpkJ37JDV+eVf0Rm/dUUdL+79m7JpZgZQoKP+ix1W\n" +
            "Tm3JZ2MYX0cwtN4I7eYUO/OD0OjsqomJU1u6e7UgScRDUAlMk3y5SCO4uI6X/U7QMfiX3SuPKVD1\n" +
            "wAYu1aj/BM+fZJ9b+/JZcYYpUNfb6JN2owxPO8ERIw7syyiOy4nMu0CPNvd5hFAzSAgojTKo9vqv\n" +
            "7xkR93us2Un96DUyug9eJ5aoLeQFTIlCoexeMh8sitaHJIs28IHz7V9QqnKgQOJrGatf+EHUdQw2\n" +
            "EcbfWTwrEANTXIqjrnyYx8BzgJ6UMTswLM9tHIlsFr4rWhVv/s4PJqiLoN10/K+qt4aMNWtWSCHc\n" +
            "PGL7FGEEM0qSq2Zn4Mv2RGmK6CsoxIC8qtdwSxB9JeTjkcrfG0weBE2NE6nmiG8cW94HqiTaSFoy\n" +
            "X2nzlbtCdAjYM16VusPXcBs4vHDrDBy9ICPizGQHp+fbSFsfoqcWs/XqxsYpb3X6BIPwEVV8Se2y\n" +
            "ZWtaTBzUPaSKCj2qMIIC+QYJKoZIhvcNAQcBoIIC6gSCAuYwggLiMIIC3gYLKoZIhvcNAQwKAQKg\n" +
            "ggKmMIICojAcBgoqhkiG9w0BDAEDMA4ECGC5jIdlTPLsAgIIAASCAoDNfPhGPpd4m9+uLNI9VCvK\n" +
            "gmXMm6gWrhA58ZOOz5HC4lLL0x2RNLJeJVnpZvpdt93etGUVHw+f7FFtNyLrckbInpgzAk4VVgQo\n" +
            "MVQr+CWhpGqVWCprbRYDofj1V9gdp+/TPy6G1/Z5mPCGHjGoFHroXEFJTfDc+3mPwNpbYyHuxI4I\n" +
            "KrnAicgEeyb5NjWzKuVn3ACZwLxz13TDScKWVoA3KQqM5/NesO60iBSobcqj7CHjcw4o9//EDyoB\n" +
            "KwJK/jwAFMTmu+iIhfuo/vasc3TDT8MZUEkyyIHYZ3pzwVuNrX3I8sDblZEakyw5Baix9gsPwTEo\n" +
            "Q5ao0Ms4cNHeyuJXjJdnyNvZLTlPNVUM4TloLhWcztVj/KhAvlw+IJ/4ObGa8vIy8xyETA//sVXt\n" +
            "Y4AxE+8hGDNel015T2CpdSut9jvmtqJaKRQzl92sHqpHtRZiJtF9vRPsuLX0JM1PlyLcCXevf8zH\n" +
            "j79JHz1U/ZKVyXGEfA0C3mJj3iBCWuQZ1Z48S7yowRVN72g5z0gkKG6I6oiuRvXXJkSvsdalB2gC\n" +
            "xsn0fQCxxkmz2n1ysvfKAj1ea7Gf7vmEabcCZyvAK2MvcsIvs14mkmiJfsXy7LsJQt5Yc3JKjnwP\n" +
            "00ebozvjFgRjh2dXAbnbKKGIMHASxZK94Fv3OtVnRd4GlCFZcqamBJVnsBbGdmUf703KV76zKhUI\n" +
            "EinDMsGINuGL0zrWn0uKArljLFJXZp+L+xT4DS7l3VVm1NdLxC7Emc2VDaSb1FlTwcvlw0cUaazo\n" +
            "0FfdqSOGtLbxM9wWFzNZ6BE6QICH7qRfcs8DyKE3WPc6b3DNJF1u9GkGLpbgqbym1Sz0ppJJMSUw\n" +
            "IwYJKoZIhvcNAQkVMRYEFDUDNCAb7qZQLRE0L5PuoJ/Atd8BMDEwITAJBgUrDgMCGgUABBQMCaJX\n" +
            "iW6GeTqVXvZ6x8x6v0cAOwQI8Ge9jbbDmVQCAggA";

}