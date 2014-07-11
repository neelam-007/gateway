package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.*;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: rseminoff
 * Date: 23/05/12
 *
 * This tests the methods in the XMPPConnectionManager class, which is the heavy lifter of this assertion.
 *
 */
public class XMPPConnectionManagerTest {

    private XMPPConnectionManager manager;
    private final Goid MESSAGE_SERVICE_GOID = new Goid(0x0123456789ABCDEFL, 0xFEDCBA9876543210L);
    private final Goid TERMINATE_SERVICE_GOID = new Goid(0xFEDCBA9876543210L, 0x0123456789ABCDEFL);
    private final String SERVER_HOSTNAME = "ext-dep-tactical-teamcity";
    private ApplicationContext appContext;
    private PolicyEnforcementContext policyContext;
    private XMPPConnectionEntityAdminImpl xmppAdmin;
    private XMPPConnectionManager connManager;

    @Before
    public void setup() {
        XMPPClassHelperFactory.setInstance(new MockXMPPClassHelper.MockClassHelperFactory());
        appContext = new ClassPathXmlApplicationContext(new String[] {"/com/l7tech/external/assertions/xmppassertion/server/extra-beans.xml"});
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        GenericEntityManager gem = appContext.getBean("genericEntityManager", GenericEntityManager.class);
        gem.registerClass(XMPPConnectionEntity.class);
        xmppAdmin = new XMPPConnectionEntityAdminImpl(gem.getEntityManager(XMPPConnectionEntity.class), appContext.getBean("stashManagerFactory", StashManagerFactory.class),
                appContext.getBean("messageProcessor", MessageProcessor.class), appContext.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class),
                appContext.getBean("routingTrustManager", TrustManager.class), appContext.getBean("secureRandom", SecureRandom.class),
                appContext.getBean("defaultKey", DefaultKey.class), appContext.getBean("ssgFirewallManager", FirewallRulesManager.class));
        connManager = XMPPConnectionManager.getInstance();
        connManager.start();
    }

    @After
    public void tearDown() {
        appContext = null;
        policyContext = null;
        connManager.stop();
        connManager = null;
        xmppAdmin = null;
        // Destroy the connection manager so we start with a clean instance each time.
        XMPPConnectionManager.destroyInstance();
    }

    @Test
    @Ignore( "Relies on missing Tactical mock classes" )
    public void testAddRemoveServerConnections() {

        ArrayList<Goid> entityTestList = new ArrayList<>();
        try {
            for (int i = 0; i < 5; i++) {
                assertTrue("Unable to save a server socket in the list", entityTestList.add(registerTestServer()));
                assertTrue("Unable to save a client socket in the list", entityTestList.add(registerLocalClientSocket()));
            }
        } catch (Exception e) {
            fail("Unable to save connection entity in the state for the test. (" + e.getMessage() + ")");
        }

        // They've all been added, let's confirm we can go through and get each one.
        try {
            for (Goid currentGOID : entityTestList) {
                assertTrue(Goid.equals(currentGOID, xmppAdmin.find(currentGOID).getGoid()));
            }
        } catch (Exception e) {
            fail("Unable to locate items that were inserted in the entity list");
        }
    }


    /**
     * This begins a convo with the XMPP service on the ext-dep server.
     */
    @Test
    @Ignore( "Relies on missing Tactical mock classes" )
    public void testStartServerConnection() {

        Goid serverID = PersistentEntity.DEFAULT_GOID;
        try {
            serverID = registerTestServer();
        } catch (Exception e) {
            fail("Unable to save connection entity in the state for the test. (" +e.getMessage() + ")");
        }

        XMPPOpenServerSessionAssertion assertion = new XMPPOpenServerSessionAssertion();
        assertion.setEnabled(true);
        assertion.setXMPPConnectionId(serverID);

        ServerXMPPOpenServerSessionAssertion serverAssertion = null;
        try {
            serverAssertion = new ServerXMPPOpenServerSessionAssertion(assertion, appContext);
        } catch (PolicyAssertionException e) {
            fail("Unable to set up server assertion to test. (" + e.getMessage() + ")");
        }

        AssertionStatus status = AssertionStatus.NONE;
        try {
            /**
             * This exposes a bug in the XMPPConnectionManager class.  When the start() method is called after the
             * singleton is instantiated, it will create an internal list of previously defined connections
             * provided by XMPPConnectionEntityAdminImpl.
             * Later additions to the list using XMPPConnectionEntityAdminImpl.save() are not seen by
             * XMPPConnectionManager, and attempting to connect to OIDs returned by save() when the connection was
             * added will result in a failure.
             * This second connManager.start() exposes and mitigates the bug, as the first connManager.start() is in
             * the @Before test setup above, and is performed by JUnit4 before executing each test.
             *
             * Reported as Bug 12413 on Tactical Bugzilla.
             */
            connManager.start();
            status = serverAssertion.checkRequest(policyContext);
            connManager.start();
        } catch (Exception e) {
            fail("The server assertion failed on the XMPP server connection attempt. (" + e.getMessage() + ")");
        }

        assertTrue("The assertion returns an error status.  (Status == " + status + ")", status == AssertionStatus.NONE);

    }

    @Test
    @Ignore //functional test disabling as per TAC-167
    public void testSetGetSessionAttributes(){

        final String attributeName = "policy";
        final String attributeValue = "something";
        final String variableName = "attribute.policy";

        Goid serverID = PersistentEntity.DEFAULT_GOID;
        try {
            serverID = registerTestServer();
        } catch (Exception e) {
            fail("Unable to save connection entity in the state for the test. (" +e.getMessage() + ")");
        }

        long serverSessionId = createServerSession(serverID);

        XMPPSetSessionAttributeAssertion assertion = new XMPPSetSessionAttributeAssertion();
        assertion.setAttributeName(attributeName);
        assertion.setInbound(false);
        assertion.setEnabled(true);
        assertion.setValue(attributeValue);
        assertion.setSessionId(Long.toString(serverSessionId));

        try{
            ServerXMPPSetSessionAttributeAssertion sXmppSSAA = new ServerXMPPSetSessionAttributeAssertion(assertion, appContext);
            AssertionStatus result = sXmppSSAA.checkRequest(policyContext);
            Assert.assertEquals(result, AssertionStatus.NONE);
        }
        catch (PolicyAssertionException e){
            fail("The server assertion failed on the XMPP server set session attribute assertion (" + e.getMessage() + ")");
        }
        catch (IOException io){
            fail("The server assertion failed on the ServerXMPPSetSessionAttributeAssertion checkRequest (" + io.getMessage() + ")");
        }

        XMPPGetSessionAttributeAssertion getAssertion =  new XMPPGetSessionAttributeAssertion();

        getAssertion.setAttributeName(attributeName);
        getAssertion.setInbound(false);
        getAssertion.setSessionId(Long.toString(serverSessionId));
        getAssertion.setVariableName(variableName);

        try{
            ServerXMPPGetSessionAttributeAssertion sXmppGSAA = new ServerXMPPGetSessionAttributeAssertion(getAssertion, appContext);
            AssertionStatus result = sXmppGSAA.checkRequest(policyContext);
            Assert.assertEquals(result, AssertionStatus.NONE);
        }
        catch (PolicyAssertionException e){
            fail("The server assertion failed on the XMPP server get session attribute assertion (" + e.getMessage() + ")");
        }
        catch (IOException io){
            fail("The server assertion failed on the ServerXMPPGetSessionAttributeAssertion checkRequest (" + io.getMessage() + ")");
        }

        try{
            Assert.assertEquals(attributeValue, policyContext.getVariable(variableName));
        }
        catch(NoSuchVariableException nov){
            fail("There is no variable with name " + variableName + "(" + nov.getMessage() + ")");
        }
    }

//    @Test
//    public void testServerClientAssociation() {
//        /**
//         * This test sets up a client and server connection, then sets up the communication between them.
//         */
//        // Tell the administrator to save these entities, keep the OIDs as we need them to associate the connection.
//        long serverID = XMPPConnectionEntity.DEFAULT_OID, socketID = XMPPConnectionEntity.DEFAULT_OID;
//        try {
//            serverID = registerTestServer();
//            socketID = registerLocalClientSocket();
//        } catch (Exception e) {
//            fail("Unable to create/save the server and client connections (" + e.getMessage() + ")");
//        }
//
//        long serverSessionID = createServerSession(serverID);
//        long clientSessionID = connectClientToServer(socketID);
//
//        // Attempt to connect the server and client IDs
//        XMPPAssociateSessionsAssertion associationAssertion = new XMPPAssociateSessionsAssertion();
//        associationAssertion.setInboundSessionId(Long.toString(clientSessionID));
//        associationAssertion.setOutboundSessionId(Long.toString(serverSessionID));
//
//        ServerXMPPAssociateSessionsAssertion serverAssociationAssertion = null;
//        AssertionStatus status = AssertionStatus.NONE;
//        try {
//            serverAssociationAssertion = new ServerXMPPAssociateSessionsAssertion(associationAssertion, appContext);
//            status = serverAssociationAssertion.checkRequest(policyContext);
//        } catch (Exception e) {
//            fail("An error occurred while processing the server assertion. ("+e.getMessage()+")");
//        }
//
//        assertTrue("The server assertion returned an abnormal status. (" + status +")", status == AssertionStatus.NONE);
//
//        // While we're at it, and we have two associated connections, let's check both sides of the association and
//        // ensure it's returning the associated half.
//        // Start with the client side, find the server we set up earlier.
//        XMPPGetAssociatedSessionIdAssertion getAssociationAssertion = new XMPPGetAssociatedSessionIdAssertion();
//        getAssociationAssertion.setInbound(true);
//        getAssociationAssertion.setSessionId(Long.toString(clientSessionID));
//        getAssociationAssertion.setVariableName("otherSide");   // This is where the other associated OID goes.
//
//        status = AssertionStatus.NONE;
//        ServerXMPPGetAssociatedSessionIdAssertion serverGetAssociationAssertion;
//        try {
//            serverGetAssociationAssertion = new ServerXMPPGetAssociatedSessionIdAssertion(getAssociationAssertion, appContext);
//            status = serverGetAssociationAssertion.checkRequest(policyContext);
//        } catch (Exception e) {
//            fail("Unable to process server assertion. ("+e.getMessage()+")");
//        }
//
//        assertTrue("The server assertion returned an abnormal status. (" + status +")", status == AssertionStatus.NONE);
//
//        // Now we get the variable.
//        Object checkObject = null;
//        try {
//            checkObject = policyContext.getVariable("otherSide");
//        } catch (Exception e) {
//            fail("Unable to get the variable containing the client side OID. (" + e.getMessage() + ")");
//        }
//
//        if (checkObject != null && checkObject instanceof Long) {
//            assertTrue("The associated server to the socketID was NOT the serverID (" + checkObject + ")", (Long)checkObject == serverSessionID);
//        }
//
//        // Check the other side -- server to client.
//        // Create a new assertion and server assertion to check the ConnectionManager singleton with.
//        getAssociationAssertion = new XMPPGetAssociatedSessionIdAssertion();
//        getAssociationAssertion.setInbound(false);
//        getAssociationAssertion.setSessionId(Long.toString(serverSessionID));
//        getAssociationAssertion.setVariableName("otherSide");   // This is where the other associated OID goes.
//
//        status = AssertionStatus.NONE;
//        try {
//            serverGetAssociationAssertion = new ServerXMPPGetAssociatedSessionIdAssertion(getAssociationAssertion, appContext);
//            status = serverGetAssociationAssertion.checkRequest(policyContext);
//        } catch (Exception e) {
//            fail("Unable to process server assertion. ("+e.getMessage()+")");
//        }
//
//        assertTrue("The server assertion returned an abnormal status. (" + status +")", status == AssertionStatus.NONE);
//
//        // Now we get the variable -- reuse the existing checkObject object.
//        checkObject = null;
//        try {
//            checkObject = policyContext.getVariable("otherSide");
//        } catch (Exception e) {
//            fail("Unable to get the variable containing the client side OID. (" + e.getMessage() +")");
//        }
//
//        if (checkObject != null && checkObject instanceof Long) {
//            assertTrue("The associated server to the serverID was NOT the socketID (" + checkObject + ")", (Long)checkObject == clientSessionID);
//        }
//
//        // Tear down the connections, as we have established them.
//        XMPPCloseSessionAssertion closeAssertion = new XMPPCloseSessionAssertion();
//        closeAssertion.setInbound(true);
//        closeAssertion.setSessionId(Long.toString(clientSessionID));
//        status = AssertionStatus.NONE;
//
//        ServerXMPPCloseSessionAssertion serverCloseAssertion = null;
//        try {
//            serverCloseAssertion = new ServerXMPPCloseSessionAssertion(closeAssertion, appContext);
//        } catch (Exception e) {
//            fail("Unable to create Server Close Session Assertion ("+e.getMessage()+")");
//        }
//
//        try {
//            status = serverCloseAssertion.checkRequest(policyContext);
//        } catch (Exception e) {
//            fail("CloseSessionAssertion failed on client session close. (" + e.getMessage() + ")");
//        }
//
//        assertTrue("CloseSessionAssertion returned an abnormal value closing the client session. (" + status + ")", status == AssertionStatus.NONE);
//
//        // This means the session is closed.  Close the server session.
//        closeAssertion = new XMPPCloseSessionAssertion();
//        closeAssertion.setInbound(false);
//        closeAssertion.setSessionId(Long.toString(serverSessionID));
//        status = AssertionStatus.NONE;
//        serverCloseAssertion = null;
//
//        try {
//            serverCloseAssertion = new ServerXMPPCloseSessionAssertion(closeAssertion, appContext);
//        } catch (Exception e) {
//            fail("Unable to create Server Close Session Assertion ("+e.getMessage()+")");
//        }
//
//        try {
//            status = serverCloseAssertion.checkRequest(policyContext);
//        } catch (Exception e) {
//            fail("CloseSessionAssertion failed on server session close. (" + e.getMessage() + ")");
//        }
//
//        assertTrue("CloseSessionAssertion returned an abnormal value closing the server session. (" + status + ")", status == AssertionStatus.NONE);
//
//
//    }

//
//    // NOTE: Also check the following conditions:
//    // * Getting a remote certificate from a server.
//    // * Starting a TLS Session, if possible, using remote certificate
//    @Test
//    public void testStartTLSGetRemoteCertificate() {
//
//        final String CERT_VARIABLE = "certVariable";
//
//        // This will establish a connection to a server and ask for its certificate.
//        // How we will check to see if its the right cert....?
//        // Create the server session.
//        long sessionID = createServerSession(registerTestServer());
//
//        // Get the TLS Session started first.
//        XMPPStartTLSAssertion tlsAssertion = new XMPPStartTLSAssertion();
//        tlsAssertion.setToServer(true);
//        tlsAssertion.setClientAuthType(XMPPStartTLSAssertion.ClientAuthType.NONE);
//        tlsAssertion.setSessionId(Long.toString(sessionID));
//
//        AssertionStatus status = AssertionStatus.NONE;
//        ServerXMPPStartTLSAssertion serverTlsAssertion = null;
//
//        try {
//            serverTlsAssertion = new ServerXMPPStartTLSAssertion(tlsAssertion, appContext);
//        } catch (Exception e) {
//            fail("Unable to create a server start TLS assertion (" +e.getMessage()+")");
//        }
//
//        // Now try to start the TLS with the server.
//        try {
//            status = serverTlsAssertion.checkRequest(policyContext);
//        } catch (Exception e) {
//            fail("The server startTLS assertion failed. ("+e.getMessage()+")");
//        }
//
//        assertTrue("The server startTLS assertion returned an abnormal status. (" + status + ")", status == AssertionStatus.NONE);
//
//
//
//        // Prepare to get the remote cert from the server
//        XMPPGetRemoteCertificateAssertion assertion = new XMPPGetRemoteCertificateAssertion();
//        assertion.setSessionId(Long.toString(sessionID));   // Get cert from the server.
//        assertion.setInbound(false);    // Outbound - to server.
//        assertion.setVariableName(CERT_VARIABLE);  // The variable to place the cert.
//
//        status = AssertionStatus.NONE;
//        ServerXMPPGetRemoteCertificateAssertion serverAssertion = null;
//
//        try {
//            serverAssertion = new ServerXMPPGetRemoteCertificateAssertion(assertion, appContext);
//        } catch (Exception e) {
//            fail("Unable to create the server get remote cert assertion. (" + e.getMessage() + ")");
//        }
//
//        // Get the cert
//        try {
//            status = serverAssertion.checkRequest(policyContext);
//        } catch (Exception e) {
//            fail("The server get remote cert assertion failed. (" + e.getMessage() + ")");
//        }
//
//        assertTrue("Server assertion returned an abnormal status. (" + status + ")", status == AssertionStatus.NONE);
//
//        // We should have a cert in a variable.
//        Object certObject = null;
//        try {
//            certObject = policyContext.getVariable(CERT_VARIABLE);
//        } catch (Exception e) {
//            fail("Unable to get the variable 'certVariable' from policyContext. (" + e.getMessage() +")");
//        }
//
//        if (certObject != null && certObject instanceof String) {
//            // We have our certificate.
//            fail();   // For the moment.
//        }
//
//        // We have a session ID, let's close it.
//        XMPPCloseSessionAssertion closeAssertion = new XMPPCloseSessionAssertion();
//        ServerXMPPCloseSessionAssertion serverCloseAssertion;
//        status = AssertionStatus.NONE;
//
//        closeAssertion.setSessionId(Long.toString(sessionID));
//        closeAssertion.setInbound(false);  // Shut down the server session.
//
//        try {
//            serverCloseAssertion = new ServerXMPPCloseSessionAssertion(closeAssertion, appContext);
//            status = serverCloseAssertion.checkRequest(policyContext);
//        } catch (Exception e) {
//            fail("Unable to close the socket session ("+e.getMessage()+")");
//        }
//
//        assertTrue("Closing the socket session returned an abnormal status", status == AssertionStatus.NONE);
//
//    }

    /**
     * Creates and registers a connection to the Test XMPP Server.
     */
    private Goid registerTestServer() {

        XMPPConnectionEntity serverConnection = new XMPPConnectionEntity();
        serverConnection.setHostname(SERVER_HOSTNAME);
        serverConnection.setInbound(false);
        serverConnection.setMessageReceivedServiceOid(MESSAGE_SERVICE_GOID);
        serverConnection.setSessionTerminatedServiceOid(TERMINATE_SERVICE_GOID);
        serverConnection.setEnabled(true);

        Goid serverGOID = XMPPConnectionEntity.DEFAULT_GOID;
        try {
            serverGOID = xmppAdmin.save(serverConnection);
        } catch (Exception e) {
            fail("Unable to save test server registration. ("+e.getMessage()+")");
        }
        return serverGOID;
    }

    private Goid registerLocalClientSocket() {
        XMPPConnectionEntity clientConnection = new XMPPConnectionEntity();
        //clientConnection.setHostname("localhost");
        //clientConnection.setBindAddress("127.0.0.1");
        clientConnection.setBindAddress("0.0.0.0");
        clientConnection.setPort(60010); // Use a local port for the connection.
        clientConnection.setInbound(true);
        clientConnection.setMessageReceivedServiceOid(MESSAGE_SERVICE_GOID);
        clientConnection.setSessionTerminatedServiceOid(TERMINATE_SERVICE_GOID);
        clientConnection.setEnabled(true);
        clientConnection.setGoid(XMPPConnectionEntity.DEFAULT_GOID);

        Goid clientGOID = XMPPConnectionEntity.DEFAULT_GOID;

        try {
            clientGOID = xmppAdmin.save(clientConnection);
        } catch (Exception e) {
            fail("Unable to register a local client socket  ("+e.getMessage()+")");
        }
        return clientGOID;
    }

    private long createServerSession(Goid serverGOID) {

        // Mitigates the bug mentioned above (#12413)
        connManager.start();

        // Connect to the Server with XMPPOpenServerSessionAssertion
        XMPPOpenServerSessionAssertion assertion = new XMPPOpenServerSessionAssertion();
        assertion.setXMPPConnectionId(serverGOID);

        ServerXMPPOpenServerSessionAssertion serverAssertion;
        AssertionStatus status = AssertionStatus.NONE;

        try {
            serverAssertion = new ServerXMPPOpenServerSessionAssertion(assertion, appContext);
            status = serverAssertion.checkRequest(policyContext);
        } catch (Exception e) {
            fail("Unable to open a server session. ("+e.getMessage()+")");
        }

        assertTrue("Open Server Session Assertion returned an abnormal status (" + status + ")", status == AssertionStatus.NONE);

        // we have a session ID passed back to us to use.
        Object sessionID = null;
        try {
            sessionID = policyContext.getVariable(XMPPOpenServerSessionAssertion.OUTBOUND_SESSION_ID_VAR_NAME);
        } catch (Exception e) {
            fail("Unable to locate the returned session ID during server session creation. (" +e.getMessage()+")");
        }

        long parsedSessionID = XMPPConnectionEntity.DEFAULT_GOID.getLow();

        if (sessionID != null && sessionID instanceof String) {
            parsedSessionID = Long.parseLong((String) sessionID);
        }
        else {
            fail("The returned session ID during server session creation was null.  Failed.");
        }
        return parsedSessionID;
    }

    private long connectClientToServer(Goid clientGOID) {

        long serverSessionID = XMPPConnectionEntity.DEFAULT_GOID.getLow();

        try {
            serverSessionID = connManager.connectToServer(clientGOID);
        } catch (Exception e) {
            fail("Unable to connect client to server session");
        }

        return serverSessionID;
    }

}


