package com.l7tech.service;

import com.l7tech.common.xml.Wsdl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Class WsdlTest tests the {@link com.l7tech.common.xml.Wsdl}
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class SoapRequestGeneratorTest extends TestCase {
    public static final String WSDL = "com/l7tech/service/resources/StockQuoteService.wsdl";
    public static final String WSDL2PORTS = "com/l7tech/service/resources/xmltoday-delayed-quotes-2ports.wsdl";
    public static final String WSDL2SERVICES = "com/l7tech/service/resources/xmltoday-delayed-quotes-2services.wsdl";
    boolean messageGeneratorInvoked = false;


    /**
     * test <code>AbstractLocatorTest</code> constructor
     */
    public SoapRequestGeneratorTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * AbstractLocatorTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SoapRequestGeneratorTest.class);
        return suite;
    }

    public void setUp() throws Exception {
        messageGeneratorInvoked = false;
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * just print the soap messages
     * 
     * @throws Exception 
     */
    public void testGenerateAndPrintSoapMessages() throws Exception {
        SoapRequestGenerator sg = new SoapRequestGenerator();

        SoapRequestGenerator.SOAPRequest[] requests = sg.generate(WSDL);

        for (int i = 0; i < requests.length; i++) {
            SoapRequestGenerator.SOAPRequest request = requests[i];
            request.getSOAPMessage().writeTo(System.out);
        }
    }


    /**
     * User supplied the values for messages
     * 
     * @throws Exception 
     */
    public void testGenerateSoapMessagesWithUserParamValues() throws Exception {
        SoapRequestGenerator sg = new SoapRequestGenerator(new SoapRequestGenerator.MessageInputGenerator() {
            public String generate(String messagePartName, String operationName, Definition definition) {
                messageGeneratorInvoked = true;
                if ("symbol".equalsIgnoreCase(messagePartName)) {
                    return "CSCO";
                }
                return "NA";
            }
        });
        SoapRequestGenerator.SOAPRequest[] requests = sg.generate(WSDL);
        assertTrue("Expected message input invoke. ", messageGeneratorInvoked);

        for (int i = 0; i < requests.length; i++) {
            SoapRequestGenerator.SOAPRequest request = requests[i];
            request.getSOAPMessage().writeTo(System.out);
        }
    }


    /**
     * validate that the messages created live in wsdl too
     * 
     * @throws Exception 
     */
    public void testGenerateSoapMessagesValidateWithWsdl() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, new WsdlTest("blah").getWsdlReader(WSDL));
        SoapRequestGenerator sg = new SoapRequestGenerator();
        SoapRequestGenerator.SOAPRequest[] requests = sg.generate(WSDL);
        for (int i = 0; i < requests.length; i++) {
            SoapRequestGenerator.SOAPRequest request = requests[i];
            request.getSOAPMessage().writeTo(System.out);
        }

        Collection bindings = wsdl.getBindings();

        for (Iterator iterator = bindings.iterator(); iterator.hasNext();) {
            Binding binding = (Binding)iterator.next();
            List bindingOperations = binding.getBindingOperations();
            for (Iterator iterator1 = bindingOperations.iterator(); iterator1.hasNext();) {
                BindingOperation bindingOperation = (BindingOperation)iterator1.next();
                final String bindingOperationName = bindingOperation.getName();

                boolean found = false;
                for (int i = 0; i < requests.length && !found; i++) {
                    SoapRequestGenerator.SOAPRequest request = requests[i];
                    SOAPBody sb = request.getSOAPMessage().getSOAPPart().getEnvelope().getBody();
                    Iterator elements = sb.getChildElements();
                    while (elements.hasNext() && !found) {
                        SOAPElement se = (SOAPElement)elements.next();
                        if (bindingOperationName.equals(se.getElementName().getLocalName())) {
                            found = true;
                        }
                    }
                }
                assertTrue("The soap request was not found for " + bindingOperationName, found);
            }
        }
    }

    /**
     * Test <code>SoapRequestGeneratorTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
