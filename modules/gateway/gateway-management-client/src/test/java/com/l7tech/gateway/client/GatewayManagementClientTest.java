package com.l7tech.gateway.client;

import com.l7tech.gateway.api.impl.TransportFactory;
import com.l7tech.test.BugId;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ResourceUtils;
import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.client.impl.TransportClient;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.*;
import java.net.ConnectException;
import java.net.PasswordAuthentication;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Gateway Management Client
 */
public class GatewayManagementClientTest {

    //- PUBLIC

    @Test
    public void testUsage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{}, System.in, out, out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertTrue( "Expected usage:\n"+output, output.startsWith( "Usage:" ));
    }

    @Test
    public void testHelp() throws Exception {
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ "-help", "enumerate" }, System.in, out, out );
            int exitCode = gmc.run();
            String output = out.toString();
            assertEquals( "Exit code", 0L, (long) exitCode );
            assertTrue( "Expected help for enumerate command:\n"+output, output.startsWith( "enumerate:" ));
        }
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ "enumerate", "-help" }, System.in, out, out );
            int exitCode = gmc.run();
            String output = out.toString();
            assertEquals( "Exit code", 0L, (long) exitCode );
            assertTrue( "Expected help for enumerate command:\n"+output, output.startsWith( "enumerate:" ));
        }
    }

    @Test
    public void testVersion() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{"-version"}, System.in, out, out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected usage:\n"+output, output.startsWith( "Version " ));
    }

    @Test
    public void testInvalidCommand() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "unknown-command" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertTrue( "Expected unknown command:\n" + output , output.startsWith("Command 'unknown-command' not recognized." ));
    }

    @Test
    public void testConnectionError() {
        setResponseObject( new ConnectException("Connection refused") );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertEquals( "Expected network error", "Network error 'Connection refused'\n", output);
    }

    @Test
    public void testServerNotTrusted() {
        setResponseObject( new SSLHandshakeException("some handshake error") );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertEquals( "Expected trust error", "Server TLS/SSL certificate is not trusted.\n", output);
    }

    @Test
    public void testServiceNotFound() {
        setResponse( "ServiceNotFoundResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
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
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertEquals( "Expected not authorized", "Not authorized to access Gateway Management service.\n", output);
    }

    @Test
    public void testAuthenticationFailed() {
        setResponse( "AuthenticationFailedResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertEquals( "Expected soap fault",
                "SOAP Fault from service:\n" +
                "  Fault: Policy Falsified\n" +
                "  Role: http://localhost:8080/wsman\n" +
                "  Details: [Found user: admin, Authentication failed for identity provider ID -2]\n", output);
    }

    @Test
    public void testResourceNotFound() {
        setResponse( "ResourceNotFoundResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertEquals( "Expected resource not found message", "No resource matched the specified name or identifier.\n", output);
    }

    @Test
    public void testPolicyMethodNonPolicyResource() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "validate", "-type", "clusterProperty", "-name", "soap.roles" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertEquals( "Expected not applicable message", "Command not applicable for type 'clusterProperty'\n", output);
    }

    @Test
    public void testInvalidInput() throws Exception {
        final String property =
                "<ns9:ClusterP  roperty xmlns:ns9=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "  <ns9:Name>test.property</ns9:Name>\n" +
                "  <ns9:Value>test value</ns9:Value>\n" +
                "</ns9:ClusterProperty>";
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "create", "-type", "clusterProperty", "-in", property },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertTrue( "Expected input error:\n" + output , output.contains("Error processing input") );
    }

    @Test
    public void testActionNotSupported() throws Exception {
        final String folder =
                "<Folder xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\" version=\"0\" id=\"256704512\">\n" +
                "    <Name>Folder</Name>\n" +
                "</Folder>";
        setResponse( "ActionNotSupportedResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "put", "-type", "folder", "-in", folder },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertTrue( "Expected not supported:\n" + output , output.equals("Command not supported for type 'folder'.\n") );
    }
    
    @Test
    public void testPermissionDenied() {
        final String property =
                "<ns9:ClusterProperty xmlns:ns9=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"264372224\" version=\"0\">\n" +
                "  <ns9:Name>test.property</ns9:Name>\n" +
                "  <ns9:Value>test value2</ns9:Value>\n" +
                "</ns9:ClusterProperty>";
        setResponse( "AccessDeniedResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "put", "-type", "clusterProperty", "-in", property },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertTrue( "Expected permission denied:\n" + output , output.equals("Permission denied when accessing resource.\n") );
    }

    @Test
    public void testEnumerate() throws Exception {
        setResponse(
                "ClusterProperty_Enumerate_Response1.xml",
                "ClusterProperty_Enumerate_Response2.xml", 
                "ClusterProperty_Enumerate_Response3.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ "gateway", "enumerate", "-type", "clusterProperty" }, System.in, out, out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected cluster property enumeration:\n" + output , output.contains("ClusterProperty>") && output.contains( "<enumeration>" ) && output.endsWith( "</enumeration>\n" ));
    }

    @Test
    public void testEmptyEnumerate() throws Exception {
        setResponse(
                "ClusterProperty_Enumerate_Response1.xml",
                "ClusterProperty_Enumerate_Response4.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        GatewayManagementClient gmc = new GatewayManagementClient( new String[]{ "gateway", "enumerate", "-type", "clusterProperty" }, System.in, out, out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected empty enumeration:\n" + output , output.contains( "<enumeration/>" ));
    }

    @Test
    public void testCreate() throws Exception {
        final String property =
                "<ns9:ClusterProperty xmlns:ns9=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "  <ns9:Name>test.property</ns9:Name>\n" +
                "  <ns9:Value>test value</ns9:Value>\n" +
                "</ns9:ClusterProperty>";
        setResponse( "ClusterProperty_Create_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "create", "-type", "clusterProperty", "-in", property },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected cluster property:\n" + output , output.contains("ClusterProperty>") && output.contains( "test value" ));
        assertTrue( "Expected ID in output:\n" + output , output.contains( "id=\"264372224\"" ));
    }

    @Test
    public void testGet() throws Exception {
        setResponse( "ClusterProperty_Get_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "clusterProperty", "-name", "soap.roles" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected cluster property:\n" + output , output.contains("ClusterProperty>") && output.contains( "secure_span" ));
    }

    @BugId("SSG-5551")
    @Test
    public void testGet_Guid_Supported() throws Exception {
        setResponse( "Policy_Get_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "policy", "-guid", "a6d0de8b-3c96-4fca-867e-c673a5083341" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        System.out.println(output);
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected policy :\n" + output , output.contains("Policy>") && output.contains( "Create Policy Test" ));
    }

    @BugId("SSG-5551")
    @Test
    public void testGet_Guid_NotSupported() throws Exception {
        setResponse( "InvalidSelectorsResponse.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "get", "-type", "service", "-guid", "a6d0de8b-3c96-4fca-867e-c673a5083341" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        System.out.println(output);
        assertEquals( "Exit code", 1L, (long) exitCode );
        assertTrue( "Expected invalid selectors. Service does not support -guid:\n" + output , output.contains("The Selectors for the resource were not valid"));
    }

    @Test
    public void testPut() throws Exception {
        final String property =
                "<ns9:ClusterProperty xmlns:ns9=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"264372224\" version=\"0\">\n" +
                "  <ns9:Name>test.property</ns9:Name>\n" +
                "  <ns9:Value>test value2</ns9:Value>\n" +
                "</ns9:ClusterProperty>";
        setResponse( "ClusterProperty_Put_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "put", "-type", "clusterProperty", "-in", property },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected cluster property:\n" + output , output.contains("ClusterProperty>") && output.contains( "test value2" ));
        assertTrue( "Expected ID in output:\n" + output , output.contains( "id=\"264372224\"" ));
        assertTrue( "Expected version in output:\n" + output , output.contains( "version=\"1\"" ));
    }

    @Test
    public void testDelete() throws Exception {
        setResponse( "ClusterProperty_Delete_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "delete", "-type", "clusterProperty", "-name", "test.property" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected no output:\n" + output , output.isEmpty());
    }

    @Test
    public void testValidatePolicy() throws Exception {
        setResponse( "Service_ValidatePolicy_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "validate", "-type", "service", "-id", "17268736" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected validation result:\n" + output , output.contains("PolicyValidationResult>") && output.contains( "Credentials are collected but not authenticated." ));
    }

    @Test
    public void testValidatePolicyInput() throws Exception {
        setResponse( "Service_ValidatePolicy_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "validate", "-type", "service", "-in", "<Service xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\" version=\"21\" id=\"17268736\"><ServiceDetail version=\"21\" id=\"17268736\"><Name>Warehouse</Name><Enabled>true</Enabled><ServiceMappings><HttpMapping><UrlPattern>/wex</UrlPattern><Verbs><Verb>POST</Verb></Verbs></HttpMapping><SoapMapping><Lax>true</Lax></SoapMapping></ServiceMappings><Properties><Property key=\"wssProcessingEnabled\"><BooleanValue>true</BooleanValue></Property><Property key=\"soap\"><BooleanValue>true</BooleanValue></Property><Property key=\"soapVersion\"><StringValue>1.1</StringValue></Property><Property key=\"internal\"><BooleanValue>false</BooleanValue></Property></Properties></ServiceDetail><Resources><ResourceSet tag=\"policy\"><Resource type=\"policy\">&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;&lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;&lt;L7p:EchoRoutingAssertion/&gt;&lt;/wsp:All&gt;&lt;/wsp:Policy&gt;</Resource></ResourceSet></Resources></Service>" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected validation result:\n" + output , output.contains("PolicyValidationResult>") && output.contains( "Credentials are collected but not authenticated." ));
    }

    @Test
    public void testValidatePolicyInputNoSoapVersion() throws Exception {
        setResponse( "Service_ValidatePolicy_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "validate", "-type", "service", "-in", "<Service xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\" version=\"21\" id=\"17268736\"><ServiceDetail version=\"21\" id=\"17268736\"><Name>Warehouse</Name><Enabled>true</Enabled><ServiceMappings><HttpMapping><UrlPattern>/wex</UrlPattern><Verbs><Verb>POST</Verb></Verbs></HttpMapping><SoapMapping><Lax>true</Lax></SoapMapping></ServiceMappings><Properties><Property key=\"wssProcessingEnabled\"><BooleanValue>true</BooleanValue></Property><Property key=\"soap\"><BooleanValue>true</BooleanValue></Property><Property key=\"internal\"><BooleanValue>false</BooleanValue></Property></Properties></ServiceDetail><Resources><ResourceSet tag=\"policy\"><Resource type=\"policy\">&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;&lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;&lt;L7p:EchoRoutingAssertion/&gt;&lt;/wsp:All&gt;&lt;/wsp:Policy&gt;</Resource></ResourceSet></Resources></Service>" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected validation result:\n" + output , output.contains("PolicyValidationResult>") && output.contains( "Credentials are collected but not authenticated." ));
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
                new String[]{ "gateway", "import", "-type", "service", "-id", "17268736", "-in", policy,
                    // Add one of each type of import instruction
                    "-import", "accept", "IdProviderReference", "10231",               // Import assertions referencing the provider as-is
                    "-import", "remove", "IdProviderReference", "10231",               // Do not import assertions referencing the provider
                    "-import", "replace", "IdProviderReference", "10231", "23111",     // Replace references to the provider with the given id
                    "-import", "rename", "IncludedPolicyReference", "13214", "NewName" // Rename the included policy fragment when importing
                },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected policy import result:\n" + output , output.contains("PolicyImportResult") );
    }

    @Test
    public void testExportPolicy() throws Exception {
        setResponse( "Service_ExportPolicy_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "export", "-type", "service", "-id", "17268736" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected policy export:\n" + output , output.contains("<exp:Export") && output.contains( "<exp:References/>" ));
    }

    @Test
    public void testCreateKey() throws Exception {
        String createkey =
                "<PrivateKeyCreationContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Dn>CN=MyKey</Dn>\n" +
                "</PrivateKeyCreationContext>";
        setResponse( "PrivateKey_CreateKey_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "createkey", "-type", "privateKey", "-id", "2:mykey", "-in", createkey},
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        assertEquals( "Exit code", 0L, (long) exitCode );
    }

    @Test
    public void testExportKey() throws Exception {
        setResponse( "PrivateKey_ExportKey_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "exportkey", "-type", "privateKey", "-id", "2:ssl", "-keyAlias", "alias", "-keyPassword", "password", "-outFile", "-" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        assertEquals( "Exit code", 0L, (long) exitCode );
    }

    @Test
    public void testGenerateCsr() throws Exception {
        setResponse( "PrivateKey_GenerateCsr_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "generatecsr", "-type", "privateKey", "-id", "2:ssl", "-outFile", "-" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        assertEquals( "Exit code", 0L, (long) exitCode );
    }

    @Test
    public void testImportKey() throws Exception {
        setResponse( "PrivateKey_ImportKey_Response.xml" );
        final byte[] fakeKeystore = new byte[32];
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "importkey", "-type", "privateKey", "-id", "2:ssl2", "-inFile", "-", "-keyPassword", "password" },
                new ByteArrayInputStream( fakeKeystore ),
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected private key resource:\n" + output , output.contains("PrivateKey>") && output.contains( "Encoded>MII" ));
    }

    @Test
    public void testKeyPurposes() throws Exception {
        setResponse( "PrivateKey_KeyPurposes_Response.xml" );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final GatewayManagementClient gmc = new GatewayManagementClient(
                new String[]{ "gateway", "keypurposes", "-type", "privateKey", "-id", "2:ssl", "-keyPurpose", "Default SSL Key" },
                System.in,
                out,
                out );
        int exitCode = gmc.run();
        String output = out.toString();
        assertEquals( "Exit code", 0L, (long) exitCode );
        assertTrue( "Expected private key resource:\n" + output , output.contains("PrivateKey>") && output.contains( "Default SSL Key" ));
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
    private static final boolean logMessages = ConfigFactory.getBooleanProperty( "com.l7tech.gateway.client.logTestMessages", true );

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
