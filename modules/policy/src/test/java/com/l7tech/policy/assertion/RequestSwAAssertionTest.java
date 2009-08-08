package com.l7tech.policy.assertion;

import java.util.Iterator;
import java.util.Set;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import com.l7tech.policy.wsp.WspReader;
import com.l7tech.wsdl.BindingInfo;
import com.l7tech.wsdl.BindingOperationInfo;
import com.l7tech.wsdl.MimePartInfo;

/**
 * Unit tests for SOAP with Attachments assertion.
 *
 * @author Steve Jones
 */
public class RequestSwAAssertionTest extends TestCase {

    public static Test suite() {
        return new TestSuite(RequestSwAAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Test 4.2 SOAP with Attachments assertion has expected defaults.
     */
    public void test42PolicyDefaults() throws Exception {
        testPolicyValues(POLICY_42, false);
    }

    /**
     * Test 4.3 SOAP with Attachments assertion with required signature.
     */
    public void test43PolicyAll() throws Exception {
        testPolicyValues(POLICY_43_SIGNED, true);
    }

    /**
     * Test 4.3 SOAP with Attachments assertion without required signature.
     */
    public void test43PolicyNone() throws Exception {
        testPolicyValues(POLICY_43_UNSIGNED, false);
    }

    /**
     * Test 4.3 SOAP with Attachments assertion with required signature.
     *
     * This tests the requiresSignature method.
     */
    public void testSignatureRequired() throws Exception {
        testSignatureRequired(POLICY_43_SIGNED, true);
    }

    /**
     * Test 4.3 SOAP with Attachments assertion without required signature.
     *
     * This tests the requiresSignature method.
     */
    public void testSignatureNotRequired() throws Exception {
        testSignatureRequired(POLICY_43_UNSIGNED, false);        
    }

    /**
     * Testflags on the mime parts 
     */
    private void testPolicyValues(final String policyXml,
                                  final boolean signatureRequired) throws Exception {
        Assertion policy = WspReader.getDefault().parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);

        boolean hasPart = false;

        Iterator<Assertion> assertions = policy.preorderIterator();
        while ( assertions.hasNext() ) {
            Assertion assertion = assertions.next();

            if (assertion instanceof RequestSwAAssertion) {
                RequestSwAAssertion swaAssertion = (RequestSwAAssertion) assertion;

                Map bindings = swaAssertion.getBindings();
                for (String bindingName : (Set<String>)bindings.keySet()) {
                    BindingInfo binding = (BindingInfo)bindings.get(bindingName);

                    // for each operation of the binding found in assertion
                    for (String boName : (Set<String>) binding.getBindingOperations().keySet()) {
                        BindingOperationInfo bo = (BindingOperationInfo)binding.getBindingOperations().get(boName);

                        for (String parameterName : (Set<String>) bo.getMultipart().keySet()) {
                            MimePartInfo part = (MimePartInfo)bo.getMultipart().get(parameterName);
                            hasPart = true;

                            if ( !signatureRequired && part.isRequireSignature() ) {
                                fail("Part requires signature");
                            } else if ( signatureRequired && !part.isRequireSignature() ) {
                                fail("Part does not require signature");
                            }
                        }
                    }
                }
            }
        }

        if ( !hasPart ) {
            fail("No parts found for assertion");
        }
    }

    /**
     * Test assertion requireSignature method 
     */
    public void testSignatureRequired(String policyXml, boolean signatureRequired) throws Exception {
        Assertion policy = WspReader.getDefault().parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);

        boolean hasAssertion = false;

        Iterator<Assertion> assertions = policy.preorderIterator();
        while ( assertions.hasNext() ) {
            Assertion assertion = assertions.next();

            if (assertion instanceof RequestSwAAssertion) {
                RequestSwAAssertion swaAssertion = (RequestSwAAssertion) assertion;
                hasAssertion = true;

                assertEquals("Signature requirement", signatureRequired, swaAssertion.requiresSignature());
            }
        }

        if ( !hasAssertion ) {
            fail("No SwA assertion found");
        }
    }    

    private static final String POLICY_42 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n"+
            "    <wsp:All wsp:Usage=\"Required\">\n"+
            "        <L7p:RequestSwAAssertion>\n"+
            "            <L7p:Bindings mapValue=\"included\">\n"+
            "                <L7p:entry>\n"+
            "                    <L7p:key stringValue=\"EchoAttachmentsServiceSoapBinding2\"/>\n"+
            "                    <L7p:value wsdlBindingInfo=\"included\">\n"+
            "                        <L7p:BindingName stringValue=\"EchoAttachmentsServiceSoapBinding2\"/>\n"+
            "                        <L7p:BindingOperations mapValue=\"included\">\n"+
            "                            <L7p:entry>\n"+
            "                                <L7p:key stringValue=\"echoDir\"/>\n"+
            "                                <L7p:value wsdlBindingOperationInfo=\"included\">\n"+
            "                                    <L7p:Multipart mapValue=\"included\">\n"+
            "                                    <L7p:entry>\n"+
            "                                    <L7p:key stringValue=\"source\"/>\n"+
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n"+
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n"+
            "                                    <L7p:item stringValue=\"*/*\"/>\n"+
            "                                    </L7p:ContentTypes>\n"+
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n"+
            "                                    <L7p:Name stringValue=\"source\"/>\n"+
            "                                    </L7p:value>\n"+
            "                                    </L7p:entry>\n"+
            "                                    </L7p:Multipart>\n"+
            "                                    <L7p:Name stringValue=\"echoDir\"/>\n"+
            "                                    <L7p:Xpath stringValue=\"/SOAP-ENV:Envelope/SOAP-ENV:Body/impl:echoDir\"/>\n"+
            "                                </L7p:value>\n"+
            "                            </L7p:entry>\n"+
            "                            <L7p:entry>\n"+
            "                                <L7p:key stringValue=\"echoTwo\"/>\n"+
            "                                <L7p:value wsdlBindingOperationInfo=\"included\">\n"+
            "                                    <L7p:Multipart mapValue=\"included\">\n"+
            "                                    <L7p:entry>\n"+
            "                                    <L7p:key stringValue=\"source2\"/>\n"+
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n"+
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n"+
            "                                    <L7p:item stringValue=\"*/*\"/>\n"+
            "                                    </L7p:ContentTypes>\n"+
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n"+
            "                                    <L7p:Name stringValue=\"source2\"/>\n"+
            "                                    </L7p:value>\n"+
            "                                    </L7p:entry>\n"+
            "                                    <L7p:entry>\n"+
            "                                    <L7p:key stringValue=\"source1\"/>\n"+
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n"+
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n"+
            "                                    <L7p:item stringValue=\"*/*\"/>\n"+
            "                                    </L7p:ContentTypes>\n"+
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n"+
            "                                    <L7p:Name stringValue=\"source1\"/>\n"+
            "                                    </L7p:value>\n"+
            "                                    </L7p:entry>\n"+
            "                                    </L7p:Multipart>\n"+
            "                                    <L7p:Name stringValue=\"echoTwo\"/>\n"+
            "                                    <L7p:Xpath stringValue=\"/SOAP-ENV:Envelope/SOAP-ENV:Body/impl:echoTwo\"/>\n"+
            "                                </L7p:value>\n"+
            "                            </L7p:entry>\n"+
            "                        </L7p:BindingOperations>\n"+
            "                    </L7p:value>\n"+
            "                </L7p:entry>\n"+
            "                <L7p:entry>\n"+
            "                    <L7p:key stringValue=\"EchoAttachmentsServiceSoapBinding1\"/>\n"+
            "                    <L7p:value wsdlBindingInfo=\"included\">\n"+
            "                        <L7p:BindingName stringValue=\"EchoAttachmentsServiceSoapBinding1\"/>\n"+
            "                        <L7p:BindingOperations mapValue=\"included\">\n"+
            "                            <L7p:entry>\n"+
            "                                <L7p:key stringValue=\"echoOne\"/>\n"+
            "                                <L7p:value wsdlBindingOperationInfo=\"included\">\n"+
            "                                    <L7p:Multipart mapValue=\"included\">\n"+
            "                                    <L7p:entry>\n"+
            "                                    <L7p:key stringValue=\"source\"/>\n"+
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n"+
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n"+
            "                                    <L7p:item stringValue=\"*/*\"/>\n"+
            "                                    </L7p:ContentTypes>\n"+
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n"+
            "                                    <L7p:Name stringValue=\"source\"/>\n"+
            "                                    </L7p:value>\n"+
            "                                    </L7p:entry>\n"+
            "                                    </L7p:Multipart>\n"+
            "                                    <L7p:Name stringValue=\"echoOne\"/>\n"+
            "                                    <L7p:Xpath stringValue=\"/SOAP-ENV:Envelope/SOAP-ENV:Body/impl:echoOne\"/>\n"+
            "                                </L7p:value>\n"+
            "                            </L7p:entry>\n"+
            "                        </L7p:BindingOperations>\n"+
            "                    </L7p:value>\n"+
            "                </L7p:entry>\n"+
            "            </L7p:Bindings>\n"+
            "            <L7p:NamespaceMap mapValue=\"included\">\n"+
            "                <L7p:entry>\n"+
            "                    <L7p:key stringValue=\"SOAP-ENV\"/>\n"+
            "                    <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n"+
            "                </L7p:entry>\n"+
            "                <L7p:entry>\n"+
            "                    <L7p:key stringValue=\"soapenv\"/>\n"+
            "                    <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n"+
            "                </L7p:entry>\n"+
            "                <L7p:entry>\n"+
            "                    <L7p:key stringValue=\"impl\"/>\n"+
            "                    <L7p:value stringValue=\"urn:EchoAttachmentsService\"/>\n"+
            "                </L7p:entry>\n"+
            "                <L7p:entry>\n"+
            "                    <L7p:key stringValue=\"xsd\"/>\n"+
            "                    <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema\"/>\n"+
            "                </L7p:entry>\n"+
            "                <L7p:entry>\n"+
            "                    <L7p:key stringValue=\"xsi\"/>\n"+
            "                    <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n"+
            "                </L7p:entry>\n"+
            "            </L7p:NamespaceMap>\n"+
            "        </L7p:RequestSwAAssertion>\n"+
            "    </wsp:All>\n"+
            "</wsp:Policy>";

    private static final String POLICY_43_SIGNED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:RequestSwAAssertion>\n" +
            "            <L7p:Bindings mapValue=\"included\">\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"EchoAttachmentsServiceSoapBinding2\"/>\n" +
            "                    <L7p:value wsdlBindingInfo=\"included\">\n" +
            "                        <L7p:BindingName stringValue=\"EchoAttachmentsServiceSoapBinding2\"/>\n" +
            "                        <L7p:BindingOperations mapValue=\"included\">\n" +
            "                            <L7p:entry>\n" +
            "                                <L7p:key stringValue=\"echoDir\"/>\n" +
            "                                <L7p:value wsdlBindingOperationInfo=\"included\">\n" +
            "                                    <L7p:Multipart mapValue=\"included\">\n" +
            "                                    <L7p:entry>\n" +
            "                                    <L7p:key stringValue=\"source\"/>\n" +
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n" +
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n" +
            "                                    <L7p:item stringValue=\"*/*\"/>\n" +
            "                                    </L7p:ContentTypes>\n" +
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n" +
            "                                    <L7p:Name stringValue=\"source\"/>\n" +
            "                                    <L7p:RequireSignature booleanValue=\"true\"/>\n" +
            "                                    </L7p:value>\n" +
            "                                    </L7p:entry>\n" +
            "                                    </L7p:Multipart>\n" +
            "                                    <L7p:Name stringValue=\"echoDir\"/>\n" +
            "                                    <L7p:Xpath stringValue=\"/SOAP-ENV:Envelope/SOAP-ENV:Body/impl:echoDir\"/>\n" +
            "                                </L7p:value>\n" +
            "                            </L7p:entry>\n" +
            "                            <L7p:entry>\n" +
            "                                <L7p:key stringValue=\"echoTwo\"/>\n" +
            "                                <L7p:value wsdlBindingOperationInfo=\"included\">\n" +
            "                                    <L7p:Multipart mapValue=\"included\">\n" +
            "                                    <L7p:entry>\n" +
            "                                    <L7p:key stringValue=\"source2\"/>\n" +
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n" +
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n" +
            "                                    <L7p:item stringValue=\"*/*\"/>\n" +
            "                                    </L7p:ContentTypes>\n" +
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n" +
            "                                    <L7p:Name stringValue=\"source2\"/>\n" +
            "                                    <L7p:RequireSignature booleanValue=\"true\"/>\n" +
            "                                    </L7p:value>\n" +
            "                                    </L7p:entry>\n" +
            "                                    <L7p:entry>\n" +
            "                                    <L7p:key stringValue=\"source1\"/>\n" +
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n" +
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n" +
            "                                    <L7p:item stringValue=\"*/*\"/>\n" +
            "                                    </L7p:ContentTypes>\n" +
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n" +
            "                                    <L7p:Name stringValue=\"source1\"/>\n" +
            "                                    <L7p:RequireSignature booleanValue=\"true\"/>\n" +
            "                                    </L7p:value>\n" +
            "                                    </L7p:entry>\n" +
            "                                    </L7p:Multipart>\n" +
            "                                    <L7p:Name stringValue=\"echoTwo\"/>\n" +
            "                                    <L7p:Xpath stringValue=\"/SOAP-ENV:Envelope/SOAP-ENV:Body/impl:echoTwo\"/>\n" +
            "                                </L7p:value>\n" +
            "                            </L7p:entry>\n" +
            "                        </L7p:BindingOperations>\n" +
            "                    </L7p:value>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"EchoAttachmentsServiceSoapBinding1\"/>\n" +
            "                    <L7p:value wsdlBindingInfo=\"included\">\n" +
            "                        <L7p:BindingName stringValue=\"EchoAttachmentsServiceSoapBinding1\"/>\n" +
            "                        <L7p:BindingOperations mapValue=\"included\">\n" +
            "                            <L7p:entry>\n" +
            "                                <L7p:key stringValue=\"echoOne\"/>\n" +
            "                                <L7p:value wsdlBindingOperationInfo=\"included\">\n" +
            "                                    <L7p:Multipart mapValue=\"included\">\n" +
            "                                    <L7p:entry>\n" +
            "                                    <L7p:key stringValue=\"source\"/>\n" +
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n" +
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n" +
            "                                    <L7p:item stringValue=\"*/*\"/>\n" +
            "                                    </L7p:ContentTypes>\n" +
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n" +
            "                                    <L7p:Name stringValue=\"source\"/>\n" +
            "                                    <L7p:RequireSignature booleanValue=\"true\"/>\n" +
            "                                    </L7p:value>\n" +
            "                                    </L7p:entry>\n" +
            "                                    </L7p:Multipart>\n" +
            "                                    <L7p:Name stringValue=\"echoOne\"/>\n" +
            "                                    <L7p:Xpath stringValue=\"/SOAP-ENV:Envelope/SOAP-ENV:Body/impl:echoOne\"/>\n" +
            "                                </L7p:value>\n" +
            "                            </L7p:entry>\n" +
            "                        </L7p:BindingOperations>\n" +
            "                    </L7p:value>\n" +
            "                </L7p:entry>\n" +
            "            </L7p:Bindings>\n" +
            "            <L7p:NamespaceMap mapValue=\"included\">\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"SOAP-ENV\"/>\n" +
            "                    <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"soapenv\"/>\n" +
            "                    <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"impl\"/>\n" +
            "                    <L7p:value stringValue=\"urn:EchoAttachmentsService\"/>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"xsd\"/>\n" +
            "                    <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema\"/>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"xsi\"/>\n" +
            "                    <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n" +
            "                </L7p:entry>\n" +
            "            </L7p:NamespaceMap>\n" +
            "        </L7p:RequestSwAAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";
    
    private static final String POLICY_43_UNSIGNED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:RequestSwAAssertion>\n" +
            "            <L7p:Bindings mapValue=\"included\">\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"EchoAttachmentsServiceSoapBinding2\"/>\n" +
            "                    <L7p:value wsdlBindingInfo=\"included\">\n" +
            "                        <L7p:BindingName stringValue=\"EchoAttachmentsServiceSoapBinding2\"/>\n" +
            "                        <L7p:BindingOperations mapValue=\"included\">\n" +
            "                            <L7p:entry>\n" +
            "                                <L7p:key stringValue=\"echoDir\"/>\n" +
            "                                <L7p:value wsdlBindingOperationInfo=\"included\">\n" +
            "                                    <L7p:Multipart mapValue=\"included\">\n" +
            "                                    <L7p:entry>\n" +
            "                                    <L7p:key stringValue=\"source\"/>\n" +
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n" +
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n" +
            "                                    <L7p:item stringValue=\"*/*\"/>\n" +
            "                                    </L7p:ContentTypes>\n" +
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n" +
            "                                    <L7p:Name stringValue=\"source\"/>\n" +
            "                                    <L7p:RequireSignature booleanValue=\"false\"/>\n" +
            "                                    </L7p:value>\n" +
            "                                    </L7p:entry>\n" +
            "                                    </L7p:Multipart>\n" +
            "                                    <L7p:Name stringValue=\"echoDir\"/>\n" +
            "                                    <L7p:Xpath stringValue=\"/SOAP-ENV:Envelope/SOAP-ENV:Body/impl:echoDir\"/>\n" +
            "                                </L7p:value>\n" +
            "                            </L7p:entry>\n" +
            "                            <L7p:entry>\n" +
            "                                <L7p:key stringValue=\"echoTwo\"/>\n" +
            "                                <L7p:value wsdlBindingOperationInfo=\"included\">\n" +
            "                                    <L7p:Multipart mapValue=\"included\">\n" +
            "                                    <L7p:entry>\n" +
            "                                    <L7p:key stringValue=\"source2\"/>\n" +
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n" +
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n" +
            "                                    <L7p:item stringValue=\"*/*\"/>\n" +
            "                                    </L7p:ContentTypes>\n" +
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n" +
            "                                    <L7p:Name stringValue=\"source2\"/>\n" +
            "                                    </L7p:value>\n" +
            "                                    </L7p:entry>\n" +
            "                                    <L7p:entry>\n" +
            "                                    <L7p:key stringValue=\"source1\"/>\n" +
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n" +
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n" +
            "                                    <L7p:item stringValue=\"*/*\"/>\n" +
            "                                    </L7p:ContentTypes>\n" +
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n" +
            "                                    <L7p:Name stringValue=\"source1\"/>\n" +
            "                                    </L7p:value>\n" +
            "                                    </L7p:entry>\n" +
            "                                    </L7p:Multipart>\n" +
            "                                    <L7p:Name stringValue=\"echoTwo\"/>\n" +
            "                                    <L7p:Xpath stringValue=\"/SOAP-ENV:Envelope/SOAP-ENV:Body/impl:echoTwo\"/>\n" +
            "                                </L7p:value>\n" +
            "                            </L7p:entry>\n" +
            "                        </L7p:BindingOperations>\n" +
            "                    </L7p:value>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"EchoAttachmentsServiceSoapBinding1\"/>\n" +
            "                    <L7p:value wsdlBindingInfo=\"included\">\n" +
            "                        <L7p:BindingName stringValue=\"EchoAttachmentsServiceSoapBinding1\"/>\n" +
            "                        <L7p:BindingOperations mapValue=\"included\">\n" +
            "                            <L7p:entry>\n" +
            "                                <L7p:key stringValue=\"echoOne\"/>\n" +
            "                                <L7p:value wsdlBindingOperationInfo=\"included\">\n" +
            "                                    <L7p:Multipart mapValue=\"included\">\n" +
            "                                    <L7p:entry>\n" +
            "                                    <L7p:key stringValue=\"source\"/>\n" +
            "                                    <L7p:value wsdlMimePartInfo=\"included\">\n" +
            "                                    <L7p:ContentTypes stringArrayValue=\"included\">\n" +
            "                                    <L7p:item stringValue=\"*/*\"/>\n" +
            "                                    </L7p:ContentTypes>\n" +
            "                                    <L7p:MaxLength intValue=\"1000\"/>\n" +
            "                                    <L7p:Name stringValue=\"source\"/>\n" +
            "                                    </L7p:value>\n" +
            "                                    </L7p:entry>\n" +
            "                                    </L7p:Multipart>\n" +
            "                                    <L7p:Name stringValue=\"echoOne\"/>\n" +
            "                                    <L7p:Xpath stringValue=\"/SOAP-ENV:Envelope/SOAP-ENV:Body/impl:echoOne\"/>\n" +
            "                                </L7p:value>\n" +
            "                            </L7p:entry>\n" +
            "                        </L7p:BindingOperations>\n" +
            "                    </L7p:value>\n" +
            "                </L7p:entry>\n" +
            "            </L7p:Bindings>\n" +
            "            <L7p:NamespaceMap mapValue=\"included\">\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"SOAP-ENV\"/>\n" +
            "                    <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"soapenv\"/>\n" +
            "                    <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"impl\"/>\n" +
            "                    <L7p:value stringValue=\"urn:EchoAttachmentsService\"/>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"xsd\"/>\n" +
            "                    <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema\"/>\n" +
            "                </L7p:entry>\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"xsi\"/>\n" +
            "                    <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n" +
            "                </L7p:entry>\n" +
            "            </L7p:NamespaceMap>\n" +
            "        </L7p:RequestSwAAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";
}
