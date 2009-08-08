/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.wsdl.Wsdl;
import org.apache.ws.policy.Policy;
import org.junit.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import static org.junit.Assert.*;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Try converted a policy from WSSP -> Layer 7 -> WSSP  and from Layer 7 -> WSSP -> Layer 7
 */
public class WsspRoundTripTest {
    private static Logger log = Logger.getLogger(WsspRoundTripTest.class.getName());

    static {
        System.setProperty("com.l7tech.policy.wssp.useNewWsspNs", "false");
        System.setProperty("com.l7tech.policy.wssp.useNewWspNs", "false");
    }

    @Test
    public void test_A11_L7WsspL7() throws Exception {
        String l7Xml = WsspWriterTest.L7_POLICY_A11;
        Assertion l7root = WspReader.getDefault().parsePermissively(l7Xml, WspReader.INCLUDE_DISABLED);
        System.out.println("Starting L7 policy:\n" + l7root + "\n\n");

        WsspWriter writer = new WsspWriter();
        Policy bindingPolicy = writer.convertFromLayer7(l7root, false);
        Policy requestPolicy = writer.convertFromLayer7(l7root, false, true);
        Policy responsePolicy = writer.convertFromLayer7(l7root, false, false);

        System.out.println("WSP Binding policy:\n");
        WsspReaderTest.displayWispyPolicy(bindingPolicy, System.out);

        System.out.println("\nWSP Request (input) policy:\n");
        WsspReaderTest.displayWispyPolicy(requestPolicy, System.out);

        System.out.println("\nWSP Response (output) policy:\n");
        WsspReaderTest.displayWispyPolicy(responsePolicy, System.out);
        System.out.println();

        WsspReader reader = new WsspReader();
        Assertion backToL7 = reader.convertFromWssp((Policy)(bindingPolicy.merge(requestPolicy)).normalize(),
                                                    (Policy)(bindingPolicy.merge(responsePolicy)).normalize());

        System.out.println("\n\nConverted back to Layer 7 form:\n" + backToL7 + "\n");
    }

    @Test
    public void test_A11_L7WsdlL7() throws Exception {
        test_L7WsspL7(WsspWriterTest.L7_POLICY_A11);
    }

    @Test
    public void test_A12_L7WsdlL7() throws Exception {
        test_L7WsspL7(WsspWriterTest.L7_POLICY_A12);
    }

    @Test
    public void test_T1_L7WsdlL7() throws Exception {
        test_L7WsspL7(WsspWriterTest.L7_POLICY_T1);
    }

    @Test
    public void test_T3_L7WsdlL7() throws Exception {
        test_L7WsspL7(WsspWriterTest.L7_POLICY_T3);
    }

    private void test_L7WsspL7(String l7Xml) throws IOException, SAXException, PolicyAssertionException, WSDLException, Wsdl.BadPolicyReferenceException, PolicyConversionException {
        Assertion l7root = WspReader.getDefault().parsePermissively(l7Xml, WspReader.INCLUDE_DISABLED);
        System.out.println("Starting L7 policy:\n" + l7root + "\n\n");

        // Get an undecorated WSDL
        Document wsdlDoc = TestDocuments.getTestDocument(TestDocuments.WSDL);

        // Decorate it with the layer 7 policy
        WsspWriter.decorate(wsdlDoc, l7root, false, null, null, null);

        // Send it to the SSB
        // ...
        // ... (slow modem)
        // ...
        // Parse the WSDL
        Wsdl wsdl = Wsdl.newInstance(wsdlDoc.getDocumentElement().getBaseURI(), wsdlDoc);

        // Get the effective policy

        Binding binding = wsdl.getBinding("GetQuoteSoapBinding");
        BindingOperation getQuote = binding.getBindingOperation("getQuote", null, null);
        assertNotNull(getQuote);

        Policy inputPolicy = (Policy)wsdl.getEffectiveInputPolicy(binding, getQuote);
        Policy outputPolicy = (Policy)wsdl.getEffectiveOutputPolicy(binding, getQuote);

        System.out.println("WSP Request (input) effective policy:\n");
        WsspReaderTest.displayWispyPolicy(inputPolicy, System.out);

        System.out.println("WSP Response (output) effective policy:\n");
        WsspReaderTest.displayWispyPolicy(outputPolicy, System.out);
        System.out.println();

        // Convert back into layer 7 format
        WsspReader reader = new WsspReader();
        Assertion backToL7 = reader.convertFromWssp((Policy)inputPolicy.normalize(),
                                                    (Policy)outputPolicy.normalize());

        System.out.println("\n\nConverted back to Layer 7 form:\n" + backToL7 + "\n");
    }

    @Test
    public void test_A11_WsspL7Wssp() throws Exception {
        final String bindingName = "A11Binding";
        testWsspL7Wssp(bindingName);
    }

    @Test
    public void test_A12_WsspL7Wssp() throws Exception {
        final String bindingName = "A12Binding";
        testWsspL7Wssp(bindingName);
    }

    @Test
    public void test_T1_WsspL7Wssp() throws Exception {
        final String bindingName = "T1Binding";
        testWsspL7Wssp(bindingName);
    }

    @Test
    public void test_T3_WsspL7Wssp() throws Exception {
        final String bindingName = "T3Binding";
        testWsspL7Wssp(bindingName);
    }

    private void testWsspL7Wssp(String bindingName) throws WSDLException, IOException, SAXException, Wsdl.BadPolicyReferenceException, PolicyConversionException, PolicyAssertionException {
        InputStream is = TestDocuments.getSecurityPolicies().getRound3MsWsdl();
        Wsdl wsdl = Wsdl.newInstance("", XmlUtil.parse(is));
        assertNotNull(wsdl);

        // Get effective policies for the binding's Echo operation
        Binding binding = wsdl.getBinding(bindingName);
        assertNotNull(binding);
        BindingOperation echoOp = binding.getBindingOperation("Echo", null, null);
        Policy echoInPolicy = (Policy)wsdl.getEffectiveInputPolicy(binding, echoOp);
        Policy echoOutPolicy = (Policy)wsdl.getEffectiveOutputPolicy(binding, echoOp);

        // Display effective policies
        System.out.println("\nEffective policy for request:\n");
        WsspReaderTest.displayWispyPolicy(echoInPolicy, System.out);
        System.out.println("\n\n\nEffective policy for response:\n");
        WsspReaderTest.displayWispyPolicy(echoOutPolicy, System.out);

        // Convert input policy to layer7 format
        WsspReader wsspReader = new WsspReader();
        Policy echoInNormalized = (Policy)echoInPolicy.normalize(wsdl.getPolicyRegistry());
        Policy echoOutNormalized = (Policy)echoOutPolicy.normalize(wsdl.getPolicyRegistry());
        Assertion ssbPolicy = wsspReader.convertFromWssp(echoInNormalized, echoOutNormalized);

        // Display converted policy
        log.info("\n\nConverted policy outline:\n" + ssbPolicy);
        log.info("\n\nConverted policy XML:\n" + WspWriter.getPolicyXml(ssbPolicy));

        // Convert back into WSSP format
        WsspWriter writer = new WsspWriter();
        Policy bindingPolicy = writer.convertFromLayer7(ssbPolicy, false);
        Policy requestPolicy = writer.convertFromLayer7(ssbPolicy, false, true);
        Policy responsePolicy = writer.convertFromLayer7(ssbPolicy, false, false);

        System.out.println("WSP Binding policy:\n");
        WsspReaderTest.displayWispyPolicy(bindingPolicy, System.out);

        System.out.println("\nWSP Request (input) policy:\n");
        WsspReaderTest.displayWispyPolicy(requestPolicy, System.out);

        System.out.println("\nWSP Response (output) policy:\n");
        WsspReaderTest.displayWispyPolicy(responsePolicy, System.out);
        System.out.println();
    }
}
