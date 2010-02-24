package com.l7tech.gateway.api;

import com.sun.ws.management.client.exceptions.FaultException;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class AccessorTest {

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    @Test
    public void testSOAPFaults() throws Exception {
        {
            final FaultException fe = buildSOAPFaultException( FAULT_1 );
            final Accessor.AccessorSOAPFaultException accessorSOAPFaultException = new Accessor.AccessorSOAPFaultException( fe );
            assertEquals("Fault reason", "Policy Falsified", accessorSOAPFaultException.getFault());
            assertEquals("Fault role", "http://localhost:8080/wsman", accessorSOAPFaultException.getRole());
            assertEquals("Fault code", "soapenv:Receiver", accessorSOAPFaultException.getCode());
            assertEquals("Fault subcodes", Collections.<String>emptyList(), accessorSOAPFaultException.getSubcodes());
            assertEquals("Fault details", Collections.<String>emptyList(), accessorSOAPFaultException.getDetails());
        }
        {
            final FaultException fe = buildSOAPFaultException( FAULT_2 );
            final Accessor.AccessorSOAPFaultException accessorSOAPFaultException = new Accessor.AccessorSOAPFaultException( fe );
            assertEquals("Fault reason", "Policy Falsified", accessorSOAPFaultException.getFault());
            assertEquals("Fault role", "http://localhost:8080/wsman", accessorSOAPFaultException.getRole());
            assertEquals("Fault code", "soapenv:Receiver", accessorSOAPFaultException.getCode());
            assertEquals("Fault subcodes", Arrays.asList("m:MessageTimeout", "m:Recipient"), accessorSOAPFaultException.getSubcodes());
            assertEquals("Fault details", Collections.<String>emptyList(), accessorSOAPFaultException.getDetails());
        }
        {
            final FaultException fe = buildSOAPFaultException( FAULT_3 );
            final Accessor.AccessorSOAPFaultException accessorSOAPFaultException = new Accessor.AccessorSOAPFaultException( fe );
            assertEquals("Fault reason", "Policy Falsified", accessorSOAPFaultException.getFault());
            assertEquals("Fault role", "http://localhost:8080/wsman", accessorSOAPFaultException.getRole());
            assertEquals("Fault code", "soapenv:Receiver", accessorSOAPFaultException.getCode());
            assertEquals("Fault subcodes", Collections.<String>emptyList(), accessorSOAPFaultException.getSubcodes());
            assertEquals("Fault details", Arrays.asList("Found user: admin", "Authentication failed for identity provider ID -2"), accessorSOAPFaultException.getDetails());
        }
    }                                                   

    @Test
    public void testSOAPFaultParsing() throws Exception {
        String faultText1 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n" +
                "  Subcodes:\n" +
                "    Detail: ";

        String faultText2 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n" +
                "    Detail: ";

        String faultText3 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n" +
                "  Subcodes:\n";

        String faultText4 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n";

        String faultText5 =
                "SOAP Fault: Policy Falsified\n" +
                "     Actor: http://localhost:8080/wsman\n" +
                "      Code: soapenv:Receiver\n" +
                "  Subcodes:\n" +
                "    Detail: test fault details\nerserwser\nasdfe ";

        Pattern faultPattern = Accessor.AccessorSOAPFaultException.SOAP_FAULT_PATTERN;
        Matcher matcher1 = faultPattern.matcher( faultText1 );
        if ( matcher1.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher1.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher1.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher1.group(3) );
            assertEquals("Fault subcodes", "", matcher1.group(4) );
            assertEquals("Fault details", "", matcher1.group(5) );
        }

        Matcher matcher2 = faultPattern.matcher( faultText2 );
        if ( matcher2.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher2.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher2.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher2.group(3) );
            assertNull("Fault subcodes", matcher2.group(4) );
            assertEquals("Fault details", "", matcher2.group(5) );
        }

        Matcher matcher3 = faultPattern.matcher( faultText3 );
        if ( matcher3.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher3.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher3.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher3.group(3) );
            assertEquals("Fault subcodes", "", matcher3.group(4) );
            assertNull("Fault details", matcher3.group(5) );
        }

        Matcher matcher4 = faultPattern.matcher( faultText4 );
        if ( matcher4.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher4.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher4.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher4.group(3) );
            assertNull("Fault subcodes", matcher2.group(4) );
            assertNull("Fault details", matcher3.group(5) );
        }

        Matcher matcher5 = faultPattern.matcher( faultText5 );
        if ( matcher5.matches() ) {
            assertEquals("Fault reason", "Policy Falsified", matcher5.group(1) );
            assertEquals("Fault role", "http://localhost:8080/wsman", matcher5.group(2) );
            assertEquals("Fault code", "soapenv:Receiver", matcher5.group(3) );
            assertEquals("Fault subcodes", "", matcher5.group(4) );
            assertEquals("Fault details", "test fault details\nerserwser\nasdfe ", matcher5.group(5) );
        }
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    private FaultException buildSOAPFaultException( final String soapFaultMessage ) throws Exception {
        MessageFactory factory = MessageFactory.newInstance( SOAPConstants.SOAP_1_2_PROTOCOL);
        SOAPMessage message = factory.createMessage( null, new ByteArrayInputStream(soapFaultMessage.getBytes()) );
        SOAPFault fault = message.getSOAPBody().getFault();
        return new FaultException(fault);
    }

    private static final String FAULT_1 =
            "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
            "    <soapenv:Body>\n" +
            "        <soapenv:Fault>\n" +
            "            <soapenv:Code>\n" +
            "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
            "            </soapenv:Code>\n" +
            "            <soapenv:Reason>\n" +
            "                <soapenv:Text xml:lang=\"en-US\">Policy Falsified</soapenv:Text>\n" +
            "            </soapenv:Reason>\n" +
            "            <soapenv:Role>http://localhost:8080/wsman</soapenv:Role>\n" +
            "            <soapenv:Detail>\n" +
            "                <l7:policyResult status=\"Something went wrong\" xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
            "            </soapenv:Detail>\n" +
            "        </soapenv:Fault>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String FAULT_2 = // fault with subcodes
            "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:m=\"http://www.example.org/timeouts\">\n" +
            "    <soapenv:Body>\n" +
            "        <soapenv:Fault>\n" +
            "            <soapenv:Code>\n" +
            "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
            "                <soapenv:Subcode>\n" +
            "                    <soapenv:Value>m:MessageTimeout</soapenv:Value>\n" +
            "                    <soapenv:Subcode>\n" +
            "                        <soapenv:Value>m:Recipient</soapenv:Value>\n" +
            "                    </soapenv:Subcode>" +
            "                </soapenv:Subcode>" +
            "            </soapenv:Code>\n" +
            "            <soapenv:Reason>\n" +
            "                <soapenv:Text xml:lang=\"en-US\">Policy Falsified</soapenv:Text>\n" +
            "            </soapenv:Reason>\n" +
            "            <soapenv:Role>http://localhost:8080/wsman</soapenv:Role>\n" +
            "            <soapenv:Detail>\n" +
            "                <l7:policyResult status=\"Something went wrong\" xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
            "            </soapenv:Detail>\n" +
            "        </soapenv:Fault>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String FAULT_3 = // fault with details
            "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
            "    <soapenv:Body>\n" +
            "        <soapenv:Fault>\n" +
            "            <soapenv:Code>\n" +
            "                <soapenv:Value>soapenv:Receiver</soapenv:Value>\n" +
            "            </soapenv:Code>\n" +
            "            <soapenv:Reason>\n" +
            "                <soapenv:Text xml:lang=\"en-US\">Policy Falsified</soapenv:Text>\n" +
            "            </soapenv:Reason>\n" +
            "            <soapenv:Role>http://localhost:8080/wsman</soapenv:Role>\n" +
            "            <soapenv:Detail>\n" +
            "                <l7:policyResult status=\"Authentication Failed\"\n" +
            "                    xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\" xmlns:l7p=\"http://www.layer7tech.com/ws/policy\">\n" +
            "                    <l7:assertionResult assertion=\"l7p:AuditAssertion\" status=\"No Error\"/>\n" +
            "                    <l7:assertionResult assertion=\"l7p:FaultLevel\" status=\"No Error\"/>\n" +
            "                    <l7:assertionResult assertion=\"l7p:HttpBasic\" status=\"No Error\">\n" +
            "                        <l7:detailMessage id=\"4104\">Found user: admin</l7:detailMessage>\n" +
            "                    </l7:assertionResult>\n" +
            "                    <l7:assertionResult assertion=\"l7p:Authentication\" status=\"Authentication Failed\">\n" +
            "                        <l7:detailMessage id=\"4208\">Authentication failed for identity provider ID -2</l7:detailMessage>\n" +
            "                    </l7:assertionResult>\n" +
            "                </l7:policyResult>\n" +
            "            </soapenv:Detail>\n" +
            "        </soapenv:Fault>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";
}
