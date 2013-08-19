package com.l7tech.gateway.common.security.keystore;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.security.cert.TestCertificateGenerator;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import static org.junit.Assert.*;

public class SsgKeyEntryTest {
    private static final Goid KEYSTORE_ID = new Goid(0,1234L);
    private static final String ALIAS = "alias";
    private SsgKeyEntry keyEntry;
    private X509Certificate[] chain;
    private PrivateKey privateKey;
    private SecurityZone zone;

    @Before
    public void setup() throws Exception {
        privateKey = new TestCertificateGenerator().getPrivateKey();
        chain = new X509Certificate[]{new TestCertificateGenerator().subject("CN=test").generate()};
        keyEntry = new SsgKeyEntry(KEYSTORE_ID, ALIAS, chain, privateKey);
        zone = new SecurityZone();
    }

    @Test
    public void setSecurityZoneMetadataNotYetAttached() {
        keyEntry.setSecurityZone(zone);
        assertEquals(zone, keyEntry.getSecurityZone());
        assertEquals(KEYSTORE_ID, keyEntry.getKeyMetadata().getKeystoreGoid());
        assertEquals(ALIAS, keyEntry.getKeyMetadata().getAlias());
    }

    @Test
    public void setSecurityZoneMetadataAlreadyAttached() {
        keyEntry.attachMetadata(new SsgKeyMetadata(KEYSTORE_ID, ALIAS, null));

        keyEntry.setSecurityZone(zone);
        assertEquals(zone, keyEntry.getSecurityZone());
        assertEquals(KEYSTORE_ID, keyEntry.getKeyMetadata().getKeystoreGoid());
        assertEquals(ALIAS, keyEntry.getKeyMetadata().getAlias());
    }
}
