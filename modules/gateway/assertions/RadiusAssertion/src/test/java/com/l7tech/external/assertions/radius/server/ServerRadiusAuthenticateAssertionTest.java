package com.l7tech.external.assertions.radius.server;

import com.l7tech.external.assertions.radius.RadiusAuthenticateAssertion;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import net.jradius.client.RadiusClient;
import net.jradius.client.auth.MSCHAPv2Authenticator;
import net.jradius.client.auth.RadiusAuthenticator;
import net.jradius.dictionary.Attr_UserName;
import net.jradius.dictionary.Attr_UserPassword;
import net.jradius.packet.*;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.AttributeList;
import net.jradius.packet.attribute.RadiusAttribute;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.InetAddress;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Test the RadiusAuthenticateAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerRadiusAuthenticateAssertionTest {

    private static final Logger log = Logger.getLogger(ServerRadiusAuthenticateAssertionTest.class.getName());

    ServerRadiusAuthenticateAssertion partiallyMockedFixture;

    PolicyEnforcementContext pec;

    @Mock
    RadiusClient mockRadiusClient;
    private Message requestMsg;

    @Before
    public void setUp() throws Exception {
        //Setup Context
        requestMsg = new Message();
        Message responseMsg = new Message();
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);
    }

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

    @Test
    public void shouldSucceedAuthenticationWhenCredentialsProvided() throws Exception {
        RadiusAuthenticateAssertion assertion = new RadiusAuthenticateAssertion();
        assertion.setHost("localhost");
        assertion.setAuthPort("1812");
        assertion.setAcctPort("0");
        assertion.setTimeout("5");
        assertion.setAuthenticator("chap");
        assertion.setTarget(TargetMessageType.REQUEST);
        assertion.setPrefix("radius");
        assertion.setSecretGoid(Goid.DEFAULT_GOID);

        AuthenticationContext authContext = pec.getAuthenticationContext(requestMsg);
        HttpBasicToken httpBasicToken = new HttpBasicToken("user", "password".toCharArray());
        authContext.addCredentials(LoginCredentials.makeLoginCredentials(httpBasicToken,assertion.getClass()));

        RadiusResponse radiusPacket = new AccessAccept();
        radiusPacket.addAttributes(new AttributeList());
        when(mockRadiusClient.authenticate(any(AccessRequest.class), any(RadiusAuthenticator.class), anyInt())).thenReturn(radiusPacket);

        partiallyMockedFixture = spy(new ServerRadiusAuthenticateAssertion(assertion));
        InetAddress inetAddress = InetAddress.getByName(assertion.getHost());
        doReturn("shared secret").when(partiallyMockedFixture).getSharedSecret();
        doReturn(mockRadiusClient).when(partiallyMockedFixture).getRadiusClient(1812, 0, 5, inetAddress, "shared secret");

        assertEquals(AssertionStatus.NONE, partiallyMockedFixture.checkRequest(pec));
    }

    @Test
    public void shouldFailAuthenticationWhenCredentialsInvalid() throws Exception {
        RadiusAuthenticateAssertion assertion = new RadiusAuthenticateAssertion();
        assertion.setHost("localhost");
        assertion.setAuthPort("1812");
        assertion.setAcctPort("0");
        assertion.setTimeout("5");
        assertion.setAuthenticator("pap");
        assertion.setTarget(TargetMessageType.REQUEST);
        assertion.setPrefix("radius");
        assertion.setSecretGoid(Goid.DEFAULT_GOID);

        AuthenticationContext authContext = pec.getAuthenticationContext(requestMsg);
        HttpBasicToken httpBasicToken = new HttpBasicToken("user", "password".toCharArray());
        authContext.addCredentials(LoginCredentials.makeLoginCredentials(httpBasicToken,assertion.getClass()));

        RadiusResponse radiusPacket = new AccessReject();
        radiusPacket.addAttributes(new AttributeList());
        when(mockRadiusClient.authenticate(any(AccessRequest.class), any(RadiusAuthenticator.class), anyInt())).thenReturn(radiusPacket);

        partiallyMockedFixture = spy(new ServerRadiusAuthenticateAssertion(assertion));
        InetAddress inetAddress = InetAddress.getByName(assertion.getHost());
        doReturn("shared secret").when(partiallyMockedFixture).getSharedSecret();
        doReturn(mockRadiusClient).when(partiallyMockedFixture).getRadiusClient(1812, 0, 5, inetAddress, "shared secret");

        assertEquals(AssertionStatus.AUTH_FAILED, partiallyMockedFixture.checkRequest(pec));

    }

}
