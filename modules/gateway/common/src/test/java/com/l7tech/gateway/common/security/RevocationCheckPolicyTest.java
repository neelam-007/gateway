package com.l7tech.gateway.common.security;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RevocationCheckPolicyTest {
    private RevocationCheckPolicy p1;
    private RevocationCheckPolicy p2;
    private SecurityZone zone;

    private static final String POLICY_XML =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<java version=\"1.6.0_03\" class=\"java.beans.XMLDecoder\"> \n" +
            " <object class=\"java.util.ArrayList\"> \n" +
            "  <void method=\"add\"> \n" +
            "   <object class=\"com.l7tech.gateway.common.security.RevocationCheckPolicyItem\"> \n" +
            "    <void property=\"allowIssuerSignature\"> \n" +
            "     <boolean>true</boolean> \n" +
            "    </void> \n" +
            "    <void id=\"ArrayList0\" property=\"trustedSigners\"> \n" +
            "     <void method=\"add\"> \n" +
            "      <long>76742669</long> \n" +
            "     </void> \n" +
            "    </void> \n" +
            "    <void property=\"trustedSigners\"> \n" +
            "     <object idref=\"ArrayList0\"/> \n" +
            "    </void> \n" +
            "    <void property=\"type\"> \n" +
            "     <object class=\"com.l7tech.gateway.common.security.RevocationCheckPolicyItem$Type\" method=\"valueOf\"> \n" +
            "      <string>CRL_FROM_CERTIFICATE</string> \n" +
            "     </object> \n" +
            "    </void> \n" +
            "    <void property=\"url\"> \n" +
            "     <string>.*</string> \n" +
            "    </void> \n" +
            "   </object> \n" +
            "  </void> \n" +
            " </object> \n" +
            "</java> \n";

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        p1 = new RevocationCheckPolicy();
        p2 = new RevocationCheckPolicy();
    }

    @Test
    public void equalsDifferentSecurityZone() {
        p1.setSecurityZone(zone);
        p2.setSecurityZone(null);
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }

    @Test
    public void equalsSameSecurityZone() {
        p1.setSecurityZone(zone);
        p2.setSecurityZone(zone);
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    @Test
    public void testHashCodeDifferentSecurityZone() {
        p1.setSecurityZone(zone);
        p2.setSecurityZone(null);
        assertFalse(p1.hashCode() == p2.hashCode());
    }

    @Test
    public void testHashCodeSameSecurityZone() {
        p1.setSecurityZone(zone);
        p2.setSecurityZone(zone);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testLoadPolicyXml(){

        RevocationCheckPolicy r = new RevocationCheckPolicy();
        r.setRevocationCheckPolicyXml(POLICY_XML);
        assertEquals(1, r.getRevocationCheckItems().size());

        RevocationCheckPolicyItem item = r.getRevocationCheckItems().get(0);
        assertTrue(item.isAllowIssuerSignature());
        assertEquals(RevocationCheckPolicyItem.Type.CRL_FROM_CERTIFICATE, item.getType());
    }
}
