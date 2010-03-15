package com.l7tech.gateway.client;

import com.l7tech.gateway.api.impl.TransportFactory;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.client.impl.TransportClient;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.PasswordAuthentication;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Tests for Gateway Management Client
 */
@SuppressWarnings({ "ThrowableInstanceNeverThrown" })
public class GatewayManagementClientTest {

    //- PUBLIC

    @Test
    public void testUsage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{}, out, out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertTrue( "Expected usage:\n"+output, output.startsWith( "Usage:" ));
    }

    @Test
    public void testHelp() throws Exception {
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ "-help", "enumerate" }, out, out );
            int exitcode = gmc.run();
            String output = out.toString();
            assertEquals( "Exit code", 0, exitcode );
            assertTrue( "Expected help for enumerate command:\n"+output, output.startsWith( "enumerate:" ));
        }
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ "enumerate", "-help" }, out, out );
            int exitcode = gmc.run();
            String output = out.toString();
            assertEquals( "Exit code", 0, exitcode );
            assertTrue( "Expected help for enumerate command:\n"+output, output.startsWith( "enumerate:" ));
        }
    }

    @Test
    public void testInvalidCommand() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "unknowncommand" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertTrue( "Expected unknown command:\n" + output , output.startsWith("Command 'unknowncommand' not recognized." ));
    }

    @Test
    public void testConnectionError() {
        setResponseObject( new ConnectException("Connection refused") );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertEquals( "Expected network error", "Network error 'Connection refused'\n", output);
    }

    @Test
    public void testServerNotTrusted() {
        setResponseObject( new SSLHandshakeException("some handshake error") );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertEquals( "Expected trust error", "Server TLS/SSL certificate is not trusted.\n", output);
    }

    @Test
    public void testServiceNotFound() {
        setResponse( "ServiceNotFoundResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertEquals( "Expected soap fault",
                "SOAP Fault from service:\n" +
                "  Fault: Policy Falsified\n" +
                "  Role: http://localhost:8080/wsmanee\n" +
                "  Details: []\n", output);
    }

    @Test
    public void testAuthenticationRequired() {
        setResponseObject( new IOException("Unauthorized") );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertEquals( "Expected not authorized", "Not authorized to access Gateway Management service.\n", output);
    }

    @Test
    public void testAuthenticationFailed() {
        setResponse( "AuthenticationFailedResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertEquals( "Expected soap fault",
                "SOAP Fault from service:\n" +
                "  Fault: Policy Falsified\n" +
                "  Role: http://localhost:8080/wsman\n" +
                "  Details: [Found user: admine, Authentication failed for identity provider ID -2]\n", output);
    }

    @Test
    public void testResourceNotFound() {
        setResponse( "ResourceNotFoundResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertEquals( "Expected resource not found message", "No resource matched the specified name or identifier.\n", output);
    }

    @Test
    public void testPolicyMethodNonPolicyResource() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "validate", "-type", "clusterProperty", "-name", "soap.roles" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertEquals( "Expected not applicable message", "Command not applicable for type 'clusterProperty'\n", output);
    }

    @Test
    public void testInvalidInput() throws Exception {
        final String property =
                "<ns9:ClusterP  roperty xmlns:ns9=\"http://ns.l7tech.com/2010/01/gateway-management\">\n" +
                "  <ns9:Name>testproperty</ns9:Name>\n" +
                "  <ns9:Value>testvalue</ns9:Value>\n" +
                "</ns9:ClusterProperty>";
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "create", "-type", "clusterProperty", "-in", property },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertTrue( "Expected input error:\n" + output , output.contains("Error processing input") );
    }

    @Test
    public void testActionNotSupported() throws Exception {
        final String folder =
                "<Folder xmlns=\"http://ns.l7tech.com/2010/01/gateway-management\" version=\"0\" id=\"256704512\">\n" +
                "    <Name>Foldere</Name>\n" +
                "</Folder>";
        setResponse( "ActionNotSupportedResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "put", "-type", "folder", "-in", folder },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertTrue( "Expected not supported:\n" + output , output.equals("Command not supported for type 'folder'.\n") );
    }
    
    @Test
    public void testPermissionDenied() {
        final String property =
                "<ns9:ClusterProperty xmlns:ns9=\"http://ns.l7tech.com/2010/01/gateway-management\" id=\"264372224\" version=\"0\">\n" +
                "  <ns9:Name>testproperty</ns9:Name>\n" +
                "  <ns9:Value>testvalue2</ns9:Value>\n" +
                "</ns9:ClusterProperty>";
        setResponse( "AccessDeniedResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "put", "-type", "clusterProperty", "-in", property },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1, exitcode );
        assertTrue( "Expected permission denied:\n" + output , output.equals("Permission denied when accessing resource.\n") );
    }

    @Test
    public void testEnumerate() throws Exception {
        setResponse(
                "ClusterProperty_Enumerate_Response1.xml",
                "ClusterProperty_Enumerate_Response2.xml", 
                "ClusterProperty_Enumerate_Response3.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ "gateway", "enumerate", "-type", "clusterProperty" }, out, out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0, exitcode );
        assertTrue( "Expected cluster property enumeration:\n" + output , output.contains("<ClusterProperty") && output.contains( "<enumeration>" ) && output.endsWith( "</enumeration>\n" ));
    }

    @Test
    public void testEmptyEnumerate() throws Exception {
        setResponse(
                "ClusterProperty_Enumerate_Response1.xml",
                "ClusterProperty_Enumerate_Response4.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ "gateway", "enumerate", "-type", "clusterProperty" }, out, out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0, exitcode );
        assertTrue( "Expected empty enumeration:\n" + output , output.contains( "<enumeration/>" ));
    }

    @Test
    public void testCreate() throws Exception {
        final String property =
                "<ns9:ClusterProperty xmlns:ns9=\"http://ns.l7tech.com/2010/01/gateway-management\">\n" +
                "  <ns9:Name>testproperty</ns9:Name>\n" +
                "  <ns9:Value>testvalue</ns9:Value>\n" +
                "</ns9:ClusterProperty>";
        setResponse( "ClusterProperty_Create_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "create", "-type", "clusterProperty", "-in", property },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0, exitcode );
        assertTrue( "Expected cluster property:\n" + output , output.contains("<ClusterProperty") && output.contains( "testvalue" ));
        assertTrue( "Expected ID in output:\n" + output , output.contains( "id=\"264372224\"" ));
    }

    @Test
    public void testGet() throws Exception {
        setResponse( "ClusterProperty_Get_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0, exitcode );
        assertTrue( "Expected cluster property:\n" + output , output.contains("<ClusterProperty") && output.contains( "secure_span" ));
    }

    @Test
    public void testPut() throws Exception {
        final String property =
                "<ns9:ClusterProperty xmlns:ns9=\"http://ns.l7tech.com/2010/01/gateway-management\" id=\"264372224\" version=\"0\">\n" +
                "  <ns9:Name>testproperty</ns9:Name>\n" +
                "  <ns9:Value>testvalue2</ns9:Value>\n" +
                "</ns9:ClusterProperty>";
        setResponse( "ClusterProperty_Put_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "put", "-type", "clusterProperty", "-in", property },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0, exitcode );
        assertTrue( "Expected cluster property:\n" + output , output.contains("<ClusterProperty") && output.contains( "testvalue2" ));
        assertTrue( "Expected ID in output:\n" + output , output.contains( "id=\"264372224\"" ));
        assertTrue( "Expected version in output:\n" + output , output.contains( "version=\"1\"" ));
    }

    @Test
    public void testDelete() throws Exception {
        setResponse( "ClusterProperty_Delete_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "delete", "-type", "clusterProperty", "-name", "testproperty" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0, exitcode );
        assertTrue( "Expected no output:\n" + output , output.isEmpty());
    }

    @Test
    public void testValidatePolicy() throws Exception {
        setResponse( "Service_ValidatePolicy_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "validate", "-type", "service", "-id", "17268736" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0, exitcode );
        assertTrue( "Expected validation result:\n" + output , output.contains("<PolicyValidationResult") && output.contains( "Credentials are collected but not authenticated." ));
    }

    @Test
    public void testImportPolicy() throws Exception {
        String policy =
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AuditAssertion/>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        setResponse( "Service_ImportPolicy_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "import", "-type", "service", "-id", "17268736", "-in", policy },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0, exitcode );
        assertTrue( "Expected policy import result:\n" + output , output.contains("<PolicyImportResult") );
    }

    @Test
    public void testExportPolicy() throws Exception {
        setResponse( "Service_ExportPolicy_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "export", "-type", "service", "-id", "17268736" },
                out,
                out );
        int exitcode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0, exitcode );
        assertTrue( "Expected policy export:\n" + output , output.contains("<exp:Export") && output.contains( "<exp:References/>" ));
    }

    @BeforeClass
    public static void setup() {
        TransportFactory.setTransportStrategy( new TransportFactory.TransportStrategy(){
            @Override
            public TransportClient newTransportClient( final int connectTimeout,
                                                       final int readTimeout,
                                                       final PasswordAuthentication passwordAuthentication,
                                                       final HostnameVerifier hostnameVerifier,
                                                       final SSLSocketFactory sslSocketFactory ) {
                return new TransportClient(){
                    @Override
                    public Addressing sendRequest( final Addressing addressing,
                                                   final Map.Entry<String, String>... entries ) throws IOException, SOAPException, JAXBException {
                        logMessage( addressing );
                        Addressing response = getResponseMessage();
                        logMessage( response );
                        return response;
                    }

                    @Override
                    public Addressing sendRequest( final SOAPMessage soapMessage,
                                                   final String s,
                                                   final Map.Entry<String, String>... entries ) throws IOException, SOAPException, JAXBException {
                        return getResponseMessage();
                    }

                    private void logMessage( final Addressing addressing ) {
                        if ( logMessages ) {
                            try {
                                addressing.writeTo( System.out );
                                System.out.println();
                            } catch ( Exception e ) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
            }
        } );
    }

    //- PRIVATE

    private static final Queue<Object> responseObjects = new ArrayDeque<Object>();
    private static final boolean logMessages = SyspropUtil.getBoolean( "com.l7tech.gateway.client.logTestMessages", true );

    private static void setResponse( final String... responseFileNames ) {
        responseObjects.clear();
        for ( String responseFileName : responseFileNames ) {
            responseObjects.add( "testMessages/" + responseFileName );
        }
    }

    private static void setResponseObject( final Object responseObject ) {
        responseObjects.clear();
        responseObjects.add( responseObject );
    }

    private static Management getResponseMessage() throws IOException, SOAPException {
        InputStream messageIn = null;
        try {
            String responseResourceName;
            Object responseObject = responseObjects.remove();
            if ( responseObject instanceof IOException ) {
                throw (IOException) responseObject;
            } else if ( responseObject instanceof SOAPException ) {
                throw (SOAPException) responseObject;
            } else if ( responseObject instanceof RuntimeException ) {
                throw (RuntimeException) responseObject;
            } else if ( responseObject instanceof String ) {
                responseResourceName = (String) responseObject;
            } else {
                throw new IOException("Unexpected response object type");
            }

            messageIn = GatewayManagementClientTest.class.getResourceAsStream(responseResourceName);
            if ( messageIn == null ) {
                throw new FileNotFoundException(responseResourceName);
            }
            return new Management( messageIn );
        } catch ( NoSuchElementException e ) {
            throw new IOException("No message queued for request.");
        } finally {
            ResourceUtils.closeQuietly( messageIn );
        }

    }
}
