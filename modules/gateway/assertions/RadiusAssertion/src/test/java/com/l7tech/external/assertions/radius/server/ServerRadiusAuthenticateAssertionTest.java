package com.l7tech.external.assertions.radius.server;

import net.jradius.client.RadiusClient;
import net.jradius.client.auth.MSCHAPv2Authenticator;
import net.jradius.dictionary.Attr_UserName;
import net.jradius.dictionary.Attr_UserPassword;
import net.jradius.packet.AccessRequest;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.AttributeList;
import net.jradius.packet.attribute.RadiusAttribute;
import org.junit.Test;
import org.junit.Ignore;

import java.net.InetAddress;
import java.util.List;
import java.util.logging.Logger;

/**
 * Test the RadiusAuthenticateAssertion.
 */
public class ServerRadiusAuthenticateAssertionTest {

    private static final Logger log = Logger.getLogger(ServerRadiusAuthenticateAssertionTest.class.getName());

    @Ignore("Require Radius Server")
    @Test
    public void testAuthenticate() throws Exception {
        AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");

        InetAddress inetAddress = InetAddress.getByName("localhost");

        RadiusClient radiusClient = new RadiusClient(inetAddress, "testing123", 1812, 1813, 30);
        AttributeList attributeList = new AttributeList();

        attributeList.add(new Attr_UserName("andy"));
        attributeList.add(new Attr_UserPassword("password"));

        AccessRequest request = new AccessRequest(radiusClient, attributeList);

        RadiusPacket reply = radiusClient.authenticate(request, new MSCHAPv2Authenticator(), 1);

        if (reply == null) return; // Request Timed-out

        AttributeList list = reply.getAttributes();
        System.out.println("TTT" + reply.getAttributeValue("MS-CHAP2-Success"));
        for (RadiusAttribute a :list.getAttributeList()) {
            System.out.println("Attribute Name: " + a.getAttributeName());
            System.out.println("Attribute Value: " + a.getValue().getBytes());

        }


        System.out.println("Received:\n" + reply.toString());
        radiusClient.close();

    }
}
