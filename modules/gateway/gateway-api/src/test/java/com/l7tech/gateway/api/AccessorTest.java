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
        {
            final FaultException fe = buildSOAPFaultException( FAULT_4 );
            System.out.println(fe.getMessage());
            final Accessor.AccessorSOAPFaultException accessorSOAPFaultException = new Accessor.AccessorSOAPFaultException( fe );
            assertEquals("Fault reason", "The service cannot comply with the request due to internal processing errors.", accessorSOAPFaultException.getFault());
            assertEquals("Fault role", "", accessorSOAPFaultException.getRole());
            assertEquals("Fault code", "env:Receiver", accessorSOAPFaultException.getCode());
            assertEquals("Fault subcodes", Arrays.asList("wsman:InternalError"), accessorSOAPFaultException.getSubcodes());
            assertEquals("Fault details", Arrays.asList("java.lang.NullPointerException", "java.lang.NullPointerException", "WsdlOperationServiceResolver:68ReolutionManagerImpl:171ReolutionManagerImpl:78NativeMethodAccessorImpl:-2NativeMethodAccessorImpl:39DelegatingMethodAccessorImpl:25Method:597AopUtils:307ReflectiveMethodInvocation:182ReflectiveMethodInvocation:149TransactionInterceptor:106ReflectiveMethodInvocation:171JdkDynamicAopProxy:204ServiceManagerImp:146ServiceManagerImp:44NativeMethodAccessorImpl:-2NativeMethodAccessorImpl:39DelegatingMethodAccessorImpl:25Method:597AopUtils:307ReflectiveMethodInvocation:182ReflectiveMethodInvocation:149TransactionInterceptor:106ReflectiveMethodInvocation:171JdkDynamicAopProxy:204PolicyVersioningServiceManager:152PolicyVersioningServiceManager:29EntityManagerResourceFactory:171EntityManagerResourceFactory:140ResourceFactorySupport:305TransactionTemplate:128ResourceFactorySupport:301EntityManagerResourceFactory:140ResourceHandler:167DefaultHandler:65ResourceHandler:72ResourceHandler:101RemoteUtils:32ResourceHandler:97ResourceHandler:93AccessController:-2Subject:396ResourceHandler:93NativeMethodAccessorImpl:-2NativeMethodAccessorImpl:39DelegatingMethodAccessorImpl:25Method:597ReflectiveRequestDispatcher:109ReflectiveRequestDispatcher:51FutureTask:303FutureTask:138Executors:441FutureTask:303FutureTask:138ThreadPoolExecutor:886ThreadPoolExecutor:908Thread:619"), accessorSOAPFaultException.getDetails());
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

    private static final String FAULT_4 = // fault with stack
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\" xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action xmlns:ns11=\"http://ns.l7tech.com/2010/01/gateway-management\" env:mustUnderstand=\"true\">http://schemas.dmtf.org/wbem/wsman/1/wsman/fault</wsa:Action><wsa:MessageID xmlns:ns11=\"http://ns.l7tech.com/2010/01/gateway-management\" env:mustUnderstand=\"true\">uuid:657e926d-ba01-4477-b80e-1a5e90daa07d</wsa:MessageID><wsa:RelatesTo xmlns:ns11=\"http://ns.l7tech.com/2010/01/gateway-management\">uuid:cd5208f3-e1f0-4dd1-86fd-715c54ffbd69</wsa:RelatesTo><wsa:To xmlns:ns11=\"http://ns.l7tech.com/2010/01/gateway-management\" env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To></env:Header><env:Body><env:Fault xmlns:ns11=\"http://ns.l7tech.com/2010/01/gateway-management\"><env:Code><env:Value>env:Receiver</env:Value><env:Subcode><env:Value>wsman:InternalError</env:Value></env:Subcode></env:Code><env:Reason><env:Text xml:lang=\"en-US\">The service cannot comply with the request due to internal processing errors.</env:Text></env:Reason><env:Detail><env:Text xml:lang=\"en-US\">java.lang.NullPointerException</env:Text><ex:Exception xmlns:ex=\"http://schemas.sun.com/ws/java/exception\">java.lang.NullPointerException<ex:StackTrace><ex:t>WsdlOperationServiceResolver:68</ex:t><ex:t>ReolutionManagerImpl:171</ex:t><ex:t>ReolutionManagerImpl:78</ex:t><ex:t>NativeMethodAccessorImpl:-2</ex:t><ex:t>NativeMethodAccessorImpl:39</ex:t><ex:t>DelegatingMethodAccessorImpl:25</ex:t><ex:t>Method:597</ex:t><ex:t>AopUtils:307</ex:t><ex:t>ReflectiveMethodInvocation:182</ex:t><ex:t>ReflectiveMethodInvocation:149</ex:t><ex:t>TransactionInterceptor:106</ex:t><ex:t>ReflectiveMethodInvocation:171</ex:t><ex:t>JdkDynamicAopProxy:204</ex:t><ex:t>ServiceManagerImp:146</ex:t><ex:t>ServiceManagerImp:44</ex:t><ex:t>NativeMethodAccessorImpl:-2</ex:t><ex:t>NativeMethodAccessorImpl:39</ex:t><ex:t>DelegatingMethodAccessorImpl:25</ex:t><ex:t>Method:597</ex:t><ex:t>AopUtils:307</ex:t><ex:t>ReflectiveMethodInvocation:182</ex:t><ex:t>ReflectiveMethodInvocation:149</ex:t><ex:t>TransactionInterceptor:106</ex:t><ex:t>ReflectiveMethodInvocation:171</ex:t><ex:t>JdkDynamicAopProxy:204</ex:t><ex:t>PolicyVersioningServiceManager:152</ex:t><ex:t>PolicyVersioningServiceManager:29</ex:t><ex:t>EntityManagerResourceFactory:171</ex:t><ex:t>EntityManagerResourceFactory:140</ex:t><ex:t>ResourceFactorySupport:305</ex:t><ex:t>TransactionTemplate:128</ex:t><ex:t>ResourceFactorySupport:301</ex:t><ex:t>EntityManagerResourceFactory:140</ex:t><ex:t>ResourceHandler:167</ex:t><ex:t>DefaultHandler:65</ex:t><ex:t>ResourceHandler:72</ex:t><ex:t>ResourceHandler:101</ex:t><ex:t>RemoteUtils:32</ex:t><ex:t>ResourceHandler:97</ex:t><ex:t>ResourceHandler:93</ex:t><ex:t>AccessController:-2</ex:t><ex:t>Subject:396</ex:t><ex:t>ResourceHandler:93</ex:t><ex:t>NativeMethodAccessorImpl:-2</ex:t><ex:t>NativeMethodAccessorImpl:39</ex:t><ex:t>DelegatingMethodAccessorImpl:25</ex:t><ex:t>Method:597</ex:t><ex:t>ReflectiveRequestDispatcher:109</ex:t><ex:t>ReflectiveRequestDispatcher:51</ex:t><ex:t>FutureTask:303</ex:t><ex:t>FutureTask:138</ex:t><ex:t>Executors:441</ex:t><ex:t>FutureTask:303</ex:t><ex:t>FutureTask:138</ex:t><ex:t>ThreadPoolExecutor:886</ex:t><ex:t>ThreadPoolExecutor:908</ex:t><ex:t>Thread:619</ex:t></ex:StackTrace></ex:Exception></env:Detail></env:Fault></env:Body></env:Envelope>";
}
