package com.l7tech.xml.soap;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.TestDocuments;
import com.l7tech.xml.soap.SoapMessageGenerator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;


import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.io.InputStreamReader;

/**
 * Class SoapRequestGeneratorTest tests the {@link com.l7tech.xml.soap.SoapMessageGenerator}
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class SoapMessageGeneratorTest {
    private static final String WSDL = TestDocuments.WSDL;
    private static final String WSDL_DOC_LITERAL2 = TestDocuments.WSDL_DOC_LITERAL2;
    boolean messageGeneratorInvoked = false;

    @Before
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
    @Ignore("disabled")
    @Test
    public void testGenerateAndPrintSoapMessages() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();

        SoapMessageGenerator.Message[] requests = sg.generateRequests(TestDocuments.WSDL);

        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
            // request.getSOAPMessage().writeTo(System.out);
        }
    }


    /**
     * User supplied the values for messages
     * 
     * @throws Exception 
     */
    @Test
    public void testGenerateSoapMessagesWithUserParamValues() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            public String generate(String messagePartName, String operationName, Definition definition) {
                messageGeneratorInvoked = true;
                if ("symbol".equalsIgnoreCase(messagePartName)) {
                    return "CSCO";
                }
                return "NA";
            }
        }, null);
        SoapMessageGenerator.Message[] requests = sg.generateRequests(WSDL);
        assertTrue("Expected message input invoke. ", messageGeneratorInvoked);

        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
            // request.getSOAPMessage().writeTo(System.out);
        }
    }


    /**
     * validate that the messages created live in wsdl too
     * 
     * @throws Exception 
     */
    @Test
    public void testGenerateSoapMessagesValidateWithWsdl() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, new InputStreamReader(SoapMessageGeneratorTest.class.getClassLoader().getResourceAsStream( WSDL )) );
        SoapMessageGenerator sg = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sg.generateRequests(WSDL);
        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
            //request.getSOAPMessage().writeTo(System.out);
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
                    SoapMessageGenerator.Message request = requests[i];
                    SOAPBody sb = request.getSOAPMessage().getSOAPPart().getEnvelope().getBody();
                    Iterator elements = sb.getChildElements();
                    while (elements.hasNext() && !found) {
                        Object  se = elements.next();
                        if(se instanceof SOAPElement) {
                        if (bindingOperationName.equals(((SOAPElement)se).getElementName().getLocalName())) {
                            found = true;
                        }
                        }
                    }
                }
                assertTrue("The soap request was not found for " + bindingOperationName, found);
            }
        }
    }

    /**
     * just print the soap messages
     *
     * @throws Exception
     */
    @Test
    public void testGenerateAndPrintSoapMessagesDocumentStyle() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();

        SoapMessageGenerator.Message[] requests = sg.generateRequests(WSDL_DOC_LITERAL2);

        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
            request.getSOAPMessage().writeTo(System.out);
        }
    }
}
