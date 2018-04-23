package com.l7tech.kerberos;

import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;
import com.sun.corba.se.spi.ior.ObjectId;
import junit.framework.TestCase;
import org.bouncycastle.asn1.*;
import org.bouncycastle.util.encoders.Hex;
import org.ietf.jgss.GSSException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sun.security.jgss.GSSHeader;
import sun.security.jgss.krb5.Krb5AcceptCredential;
import sun.security.krb5.KrbApReq;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

import java.io.ByteArrayInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by balro10 on 2016-08-11.
 */
public class KerberosTicketDecodeTest extends TestCase {

    public static final String TICKET1 =
            "YIIFiQYJKoZIhvcSAQICAQBuggV4MIIFdKADAgEFoQMCAQ6iBwMFAAAAAACjggSCYYIEfjCCBHqgAwIBBaE" +
                    "QGw5OQVZZLUxNREVWLk1JTKIjMCGgAwIBAKEaMBgbBmtyYnRndBsObmF2eS1sbWRldi5taWyjgg" +
                    "Q6MIIENqADAgEXoQMCAQKiggQoBIIEJHztdhWmw4kNH0UpXOsy/yOmIphIHN3XR2/v3VaP/Zo0M" +
                    "++AjAoD+IC6L1+BEq6RbgJ1Dri9NcaxEOZ45H8GIw9vox0a3vKAelMarbLC13sMX9w2940ut5cx" +
                    "oNw+H+eZKnAPJuChc9Z50d0g6WVWQKD7aNPLN8hwEshUPVZbr/zg0+iQ7YZAIyCJ33IeXk/1vAZ" +
                    "V7Ziwn747BPjMRBcFIE0RU9iqZWBr3nbL+GInHVejQRAv1Kmb7Y9SF7WisdPxnBWPKqAlIo1kpE" +
                    "7aE1JZCb0+Q9PAZW4yvjq9oD0UXH/ujxPNlQaaGAj6ZAqv7qmaQoXv3D/ycudN2RD3hpdxv1gK9" +
                    "dcHsu1td6wQqldgbwfUy9f2qkc5Lokdr233ST6LzOJH9R2C++igsZZvvGkcpsWKoH13Gov8l0JW" +
                    "JeYsOdg/zQiW0jAX7Vpig7pMiClFmj7Lttc/EjavR3ZRkhYM/hkzndmWr+Mqy1rL1BgBZEipaUf" +
                    "wzgXP41ig5q49HHqBgvmfYvES5tIcDT2GjyfdCC80pEQk8acep0KPAw2r1bRhQZJ7O/d+X3DpDk" +
                    "WEGS6e/1hBHQ9O3Og8Zpv5KiFW1sYIbhW5n1EmFaNSZjslI3zO6xAoouRM1ud7JKqDOk7kylpHD" +
                    "OAgp79SPkLVUOVws25YhJYi7mvrQ/TLgXJGyG5tFXwEeN42jcMtkhdoSlmeCnpOXJ1I+dJPiI2b" +
                    "3RIRdh5+dKE/XBmN6Atl4v2rqvdMgw/gkk+Twj3VJBQ0+5A6BVp0nnTHauF6JRtvhAbjH0uRXeQ" +
                    "NPsWQXsJb0xsOwaElIZAHhAohd1huF2W3qasosSjQbVF37m5mL5Zj5XtmOC9Qu8U0XebVxGqy8h" +
                    "ghGF4ei5NCUvaI3Eb9ras5jwW1ckFux+GcjBsVXDeX4aNeKuw8XJZ5fgkYDQq/Bbu9vjryTUsVS" +
                    "ydJZirMq8mjMwyB8PZ9WjJyCT9MWn3L6OlSzpbU3h1+amhRr8cyQcM8a4kBWhz2qr1c4kOzaiWq" +
                    "OH86PpW0RiUaxq7YLRdlm+xDMY9bd06ByXxbkD6qKFz2Aclxw0UNKqt88VI15j8DOlkJ7++ymzB" +
                    "XFCWjTFWWVzkKXHTprzYhWrHFKyBvTN/rVEuLvvEF0dOAkDWDUF9vgwXd/5G8NLUZ8N8ZRC4W6i" +
                    "LyG0wowfPfmEJS4wywVxoNK+RqbX4Di3fR0bud7jDqhP4HOJWl+IHEv5LjfIPNZ28M71uEpAV9/" +
                    "bLlPzOpT7Hp4MO9+0NU+fZ6ndymQyO7nBf43E9qqtGCWJeTjGtmjqK3UIlNofOWBBWNVvD4ozlF" +
                    "fCP6GNpm9GnTv8MRo2Pq7iqetSvfCaZ5fewpJ1G6KQuM3OXm86iaCnyE1wZ+XprQDiyW0qpFIYO" +
                    "DIiSe5Hx0R4roXKanGPOkgdgwgdWgAwIBF6KBzQSBypwuQ0XeBaN94f9QN57nhhke4d/wdfaRDr" +
                    "CBPa2KpYCtQc4czTUOlu6ZaLJ0oqS2O8Xryaxi7NfujTErO9gTbc102giV4/UvgeYbsnwzC2TaY" +
                    "5gohfoGQ/YHonplstBr244qN15sLqgEWwwCWVrSA2l9fMOO4OkyBR4V9DVpVOKSu/+4/3xQdvfR" +
                    "oyB32UjoRPgETYpX8E9m1EaPaLio56tKtLx+mNmFz85+6e61896mrE2ttg9UXDhSeMbw2GoGADg" +
                    "6W23wqYzMeLw=";

    public static final String TICKET2 =
            "YIIFdQYJKoZIhvcSAQICAQBuggVkMIIFYKADAgEFoQMCAQ6iBwMFAAAAAACjggSCYYIEfjCCBHqgAwIB" +
                    "BaEPGw1SRURNT05ELkxPQ0FMoigwJqADAgEAoR8wHRsEaHR0cBsVYmFscm8xMG1hYzEyMzcu" +
                    "Y2EuY29to4IENjCCBDKgAwIBF6EDAgEDooIEJASCBCCUz/oiqi65Zfaxp3hmOa4G7NFYu8Y3" +
                    "BFCQt/MLAxG7XYWZLG7JvhOts48TCISkDjDABC6skP9pcVHL4uhhctIuauD7MH/A0Swf2b7Z" +
                    "WEA/LCJgAFjbt4wNXCsf7Mg7BbXcnMIsBd+5KeyyvyTDtUaYFxN4tCAYfZp36KeelMDydhd2" +
                    "hxUxYvtNT9dgosO/7YVsmJAXNaI1PQhkwWBq0ns0K8DbGMqGvXgyZl+vxMSvpFP6gY0v+F72" +
                    "uqC8fd9jgxdhFUBXLNUAaE+LRVdtgscXyPJ2U/h8GU4I/vTL/PbtPEm2MqfJkURIWwJPHrAy" +
                    "xSCflfJH+jYPHlGT4omXyY8enBvZccOoHEl/chiH4SIyjQP0O2ZoaM/QVLU+9b71TrlsDtqV" +
                    "cpfa3rGx8yKDfR2UsDCv9zn1CLlbV4GTX6C4GTuXA0QkruTu8j32TbN68+i355D3uKhw84Za" +
                    "gvYXosYRF6LG1g2C4eQcDiWosojgWOwwyoeaQlLe2U8XLZ8YWdG/4BlXi/sdn8yx6y0H5XcL" +
                    "IfMw1nwEMgjc+lcRUUyiyyfFzSKr0vmVflMM2EnbqqO8GQ1A9B/oFgpOwlg6I83DFSQvT4au" +
                    "IBGTq3RML9coXN1b3+YJ9Lb8/Pd7SX5X9Xzj6ZB4yANWIcu7t4xGect6Xy5XUeI4PUDzv+7v" +
                    "tLFuP50nwvF5/pJ7J4pxuUG5sCglwS/TmsuH2x1L6cKrNaPTxqssyaq0xdgWikfCYH/xuYxZ" +
                    "z2i/MW3nr7M6wP1/oMpCgzJboewGGGNDBGZiPZ4gkRRbUcunHXFKlnnqwi74P6d/qkssXnSn" +
                    "8FGeXzcxuz9gktriQEpIwq++DZ16jxwAgQ+RQU1piMzB9wwTl5aMaAOR7VtkqHwwHWu/8tes" +
                    "+5IXz4UjdLtLfmV9L0F6/yGsULlXPeIVhVsUUblLcO1XoYRzd5zdE3PFJ/U3jWAXlOWEuJBI" +
                    "Vgl7364a/Ar1XzrMMdSH+n2WRasmpUSytGBBrQvTkxJXR3NPIUwAU+t8YAXPKLG9fzbmAnVw" +
                    "MiV9oz9L4qX0zYrKIqithe0DSb36zXyoToBCEh9jRPRTKqzUy2pfQfWO3yXIJfqeorW2Kt+Q" +
                    "jf44gIxy4lK0UaS4jb4/6/F5qCqSoZ/g/g/rv1c4XEkC9pjT8hGIGaAWCHXyMvUQFIlx/vhb" +
                    "if5KlimWwIm5hZ0Cz710NueOjjJuyfK5lUQlB19Se45+eqz3xGnK3W6q3mR+0Ovz3qz8srKv" +
                    "MhYNuqUN1CeXfYFjJ408oLSgupltuoZltVnOndTL1Z7xC4JcUXP0ukmps1Ybk1zxX6n5nx4C" +
                    "rHIRSv+RCESzkGc98cni3loIuXPmaohRUOR50XeIKnMYEKPcJi6UGgv75/k1W9N0RVgP2By1" +
                    "Ffcf0n3IKGikgcQwgcGgAwIBF6KBuQSBtjow7DUad+JmSQHtMdpk4yDlGnb212WF16XEKfc0" +
                    "1fgtCJEStBMG9xJCBpLGSKPnIL+A8SW9Dc1bwU9m28aAqzV82KS3JcIGat3oRjkrufbNpzbF" +
                    "PjLwLOk7luh9BxYXIHaZkguUFWo0hK3lWmTBknqnbT/8RrB+UfZauvTL/u9wEtxWODd5rFaO" +
                    "GwYM3/bZOqcZtfr4jFr5rnyYgx4cwqoIrvhi64LgjvZbBjW2tJPEc0pTl7kp";

    public static final String CORRUPT =
            "YIIFdQYJKoZIhvcSAQICAQBzzzzzzzzzzzzDAgEFoQMCAQ6iBwMFAAAAAACjggSCYYIEfjCCBHqgAwIB" +
                    "BaEPGw1SRURNT05ELkxPQ0FMoigwJqADAgEAoR8wHRsEaHR0cBsVYmFscm8xMG1hYzEyMzcu" +
                    "Y2EuY29to4IENjCCBDKgAwIBF6EDAgEDooIEJASCBCCUz/oiqi65Zfaxp3hmOa4G7NFYu8Y3" +
                    "BFCQt/MLAxG7XYWZLG7JvhOts48TCISkDjDABC6skP9pcVHL4uhhctIuauD7MH/A0Swf2b7Z" +
                    "WEA/LCJgAFjbt4wNXCsf7Mg7BbXcnMIsBd+5KeyyvyTDtUaYFxN4tCAYfZp36KeelMDydhd2" +
                    "hxUxYvtNT9dgosO/7YVsmJAXNaI1PQhkwWBq0ns0K8DbGMqGvXgyZl+vxMSvpFP6gY0v+F72" +
                    "uqC8fd9jgxdhFUBXLNUAaE+LRVdtgscXyPJ2U/h8GU4I/vTL/PbtPEm2MqfJkURIWwJPHrAy" +
                    "xSCflfJH+jYPHlGT4omXyY8enBvZccOoHEl/chiH4SIyjQP0O2ZoaM/QVLU+9b71TrlsDtqV" +
                    "cpfa3rGx8yKDfR2UsDCv9zn1CLlbV4GTX6C4GTuXA0QkruTu8j32TbN68+i355D3uKhw84Za" +
                    "gvYXosYRF6LG1g2C4eQcDiWosojgWOwwyoeaQlLe2U8XLZ8YWdG/4BlXi/sdn8yx6y0H5XcL" +
                    "IfMw1nwEMgjc+lcRUUyiyyfFzSKr0vmVflMM2EnbqqO8GQ1A9B/oFgpOwlg6I83DFSQvT4au" +
                    "IBGTq3RML9coXN1b3+YJ9Lb8/Pd7SX5X9Xzj6ZB4yANWIcu7t4xGect6Xy5XUeI4PUDzv+7v" +
                    "tLFuP50nwvF5/pJ7J4pxuUG5sCglwS/TmsuH2x1L6cKrNaPTxqssyaq0xdgWikfCYH/xuYxZ" +
                    "z2i/MW3nr7M6wP1/oMpCgzJboewGGGNDBGZiPZ4gkRRbUcunHXFKlnnqwi74P6d/qkssXnSn" +
                    "8FGeXzcxuz9gktriQEpIwq++DZ16jxwAgQ+RQU1piMzB9wwTl5aMaAOR7VtkqHwwHWu/8tes" +
                    "+5IXz4UjdLtLfmV9L0F6/yGsULlXPeIVhVsUUblLcO1XoYRzd5zdE3PFJ/U3jWAXlOWEuJBI" +
                    "Vgl7364a/Ar1XzrMMdSH+n2WRasmpUSytGBBrQvTkxJXR3NPIUwAU+t8YAXPKLG9fzbmAnVw" +
                    "MiV9oz9L4qX0zYrKIqithe0DSb36zXyoToBCEh9jRPRTKqzUy2pfQfWO3yXIJfqeorW2Kt+Q" +
                    "jf44gIxy4lK0UaS4jb4/6/F5qCqSoZ/g/g/rv1c4XEkC9pjT8hGIGaAWCHXyMvUQFIlx/vhb" +
                    "if5KlimWwIm5hZ0Cz710NueOjjJuyfK5lUQlB19Se45+eqz3xGnK3W6q3mR+0Ovz3qz8srKv" +
                    "MhYNuqUN1CeXfYFjJ408oLSgupltuoZltVnOndTL1Z7xC4JcUXP0ukmps1Ybk1zxX6n5nx4C" +
                    "rHIRSv+RCESzkGc98cni3loIuXPmaohRUOR50XeIKnMYEKPcJi6UGgv75/k1W9N0RVgP2By1" +
                    "Ffcf0n3IKGikgcQwgcGgAwIBF6KBuQSBtjow7DUad+JmSQHtMdpk4yDlGnb212WF16XEKfc0" +
                    "1fgtCJEStBMG9xJCBpLGSKPnIL+A8SW9Dc1bwU9m28aAqzV82KS3JcIGat3oRjkrufbNpzbF" +
                    "PjLwLOk7luh9BxYXIHaZkguUFWo0hK3lWmTBknqnbT/8RrB+UfZauvTL/u9wEtxWODd5rFaO" +
                    "GwYM3/bZOqcZtfr4jFr5rnyYgx4cwqoIrvhi64LgjvZbBjW2tJPEc0pTl7kp";

    public static final String SPNEGO_TICKET =
            "YIIE+wYGKwYBBQUCoIIE7zCCBOugDTALBgkqhkiG9xIBAgKhBAMCATaiggTSBIIEzmCCBMoGCSqGSIb3\n" +
            "EgECAgEAboIEuTCCBLWgAwIBBaEDAgEOogcDBQAAAAAAo4ID3mGCA9owggPWoAMCAQWhDxsNU0VBVFRM\n" +
            "RS5MT0NBTKIjMCGgAwIBAKEaMBgbBEhUVFAbEHNwbmVnbzEudGVzdC5jb22jggOXMIIDk6ADAgEXoQMC\n" +
            "AQSiggOFBIIDgcvuUIfyYa5i71sngE0PEfuzn1NBJxXdEEgQJugcvXGJVDlB4fC4AYE5P4tXLsx6ImBp\n" +
            "E9zaju6cA+/YyQ7R4bW6Dc59KEvivszKrg+Cei7vEBJYPnIh72M3Fv3rJEQ6+dArn7edyV8FueLBmWje\n" +
            "cWCHne02GBtRtxxpKd+erEwSgWqIdhj4CyB8iqwjOF9iUYgGQjtcWRg8a64WuoPeYsCgJvl+yN1Kzlte\n" +
            "PV222w19SeHnFYwsWpnBky89c2rl2ibbxaRdkstGfYnPeBYS/+jC8yRporQz58SOf51svDo6t34JqQsp\n" +
            "0imV/keCl6+QXsg7mUCOijxSleq9XaPzv7epsJaGV44n6oxF3WtR6DrDtL5QSqT4m6bYfzh4OPX34yN4\n" +
            "ATE5mYZhjpZ658p9aICL9/P2UbMBZnfZ9/Mk96SUYQVfXuXvmxgfSpSZELy/q3zvWErIap9LU21W6Ftr\n" +
            "PtQtpRMA0Bzk/ANhE7XiNWBNnJ0Qhol3s3dhJ7UevrdqgfkyNdOkIsXgwwUKvypXh3WOTQC3CEa86+jG\n" +
            "Pxr6FcMjaxdVCn+KAdvcxTGq3LsgtnG/Lh1rfeW4RHTVuufN1BeWJhvR+Thoa/AIKMQsni4OdkEjfuBT\n" +
            "X8lVAaYDXKDf+eiEkCFTbJo4HhbUGrM9XHlKsboC9TwPCLVlWnfsXyTAyE7sU5G1c2VVS9X8FlXym5oV\n" +
            "nU9vvRUgC59OdTBdOvoyloAcCc4CuGF6+f4+eUYalI/VXrSin2dANOQwxqBJiS2qZlaDt3+M2lOhlmG9\n" +
            "xMhrqzwXdakThN16zUr3OcXlW4BoJVOLXTq/xsw4yzJ+PuUzM3++CTiCW6mnTxt6kQSMpWPHtisvuufN\n" +
            "XOQftbFiq4htSNwTkZzyJVAI9WxrA/cIXTqJlM+NJ+fdRPemrzFlxVilDmE0oh3qMv+gRQGRgj7H2AXd\n" +
            "co4HEV0U6Hqwav+NR+9wmsHNjr3lo+lpFq7HqtVHMMuFAP2GIkUguXvJVe6Y28nu+X/x7szMEYJqfyOk\n" +
            "N0b9bMNMrWo1oxXV1lGV8bVmLfRLXlh/VvlvsIz5NDFpg2kPxn8VfAPKpRouVtmXs5LoncUcAcwEREKw\n" +
            "PqpFW6kNJ5i0nrrwoTaF9y0FoUeBesIB2QQCrOx/kY4Di5eogqAupdim3mK0eKAlhVijraUc7aV4oF30\n" +
            "MQhVsoU6x6SBvTCBuqADAgEXooGyBIGvFYt1hPLfKqJtgk2tMrCEkUy/e3YJcKhG0XozVtq/Cj1TE96h\n" +
            "BuwT+MBHuyNpeizxgqCYn249ldkosAsb3DQ8PBLwEBG7nv9ZnbUUaEMgKH5K4gwI+45OM+419RL7Q3yk\n" +
            "cjk790jdlPuV/NZdig3KKATF2wj0gAdVyPTm/ZaZoUxgKjdJYnGtW3AakVLS1S0fM5UTwizR8hjUO1aE\n" +
            "sUh34wCNaDjbqJR5ibTxQ+HvUg==";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testTicketParse() {
        Krb5ApReq k1 = new Krb5ApReq(HexUtils.decodeBase64(TICKET1));
        assertEquals(k1.getHost(),"navy-lmdev.mil");
        assertEquals(k1.getProtocol(),"krbtgt");
        assertEquals(k1.getRealm(),"NAVY-LMDEV.MIL");

        Krb5ApReq k2 = new Krb5ApReq(HexUtils.decodeBase64(TICKET2));
        assertEquals(k2.getHost(),"balro10mac1237.ca.com");
        assertEquals(k2.getProtocol(),"http");
        assertEquals(k2.getRealm(),"REDMOND.LOCAL");

    }

    @Test
    public void testCorruptParse() {
        boolean success = false;
        try {
            Krb5ApReq k = new Krb5ApReq(HexUtils.decodeBase64(CORRUPT));
        } catch ( Krb5ApReqException e ) {
            assertTrue(true);
            success = true;
        } catch ( Exception e ) {
            assertTrue(false);
        }

        assertTrue(success);
    }

    @Test
    public void testSpnegoTicketParsing() {
        try {
            Krb5ApReq krb5ApReq = new Krb5ApReq(HexUtils.decodeBase64(SPNEGO_TICKET));
            assertEquals(krb5ApReq.getSpn(), "HTTP/spnego1.test.com@SEATTLE.LOCAL");
        } catch (Krb5ApReqException e) {
            fail("The SPNEGO token must have been successfully parsed.");
        }
    }
//    @Test
//    public void doSpeedTest() {
//        long start = System.currentTimeMillis();
//        for ( int i = 0; i < 500000; i++ ) {
//            Krb5ApReq k = new Krb5ApReq(HexUtils.decodeBase64(TICKET1));
//
//        }
//        System.out.println("Time: " + (System.currentTimeMillis() - start) );
//    }

}
