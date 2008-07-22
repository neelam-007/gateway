/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.common.TestDocuments;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.wsp.WspWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.PolicyWriter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import java.io.*;
import java.util.logging.Logger;

/**
 * Test for WsspReader class, which converts WS-SecurityPolicy files into Layer 7 format.
 */
public class WsspReaderTest extends TestCase {
    private static Logger log = Logger.getLogger(WsspReaderTest.class.getName());

    public WsspReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WsspReaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    static void displayWispyPolicy(Policy blat, PrintStream out) throws IOException, SAXException {
        PolicyWriter writer = PolicyFactory.getPolicyWriter(PolicyFactory.StAX_POLICY_WRITER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.writePolicy((Policy)blat, baos);
        Document d = XmlUtil.parse(new ByteArrayInputStream(baos.toByteArray()));
        XmlUtil.nodeToFormattedOutputStream(d, out);
    }

    public void testGenerateSsbPolicyT1() throws Exception {
        final String bindingName = "T1Binding";
        testBinding(bindingName);
    }

    public void testGenerateSsbPolicyT3() throws Exception {
        final String bindingName = "T3Binding";
        testBinding(bindingName);
    }

    public void testGenerateSsbPolicyA11() throws Exception {
        final String bindingName = "A11Binding";
        testBinding(bindingName);
    }

    public void testGenerateSsbPolicyA12() throws Exception {
        final String bindingName = "A12Binding";
        testBinding(bindingName);
    }

    private void testBinding(String bindingName) throws WSDLException, IOException, SAXException, Wsdl.BadPolicyReferenceException, PolicyConversionException {
        // Get WSDL
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
        displayWispyPolicy(echoInPolicy, System.out);
        System.out.println("\n\n\nEffective policy for response:\n");
        displayWispyPolicy(echoOutPolicy, System.out);

        // Convert input policy to layer7 format
        WsspReader wsspReader = new WsspReader();
        Policy echoInNormalized = (Policy)echoInPolicy.normalize(wsdl.getPolicyRegistry());
        Policy echoOutNormalized = (Policy)echoOutPolicy.normalize(wsdl.getPolicyRegistry());
        com.l7tech.policy.assertion.Assertion ssbPolicy = wsspReader.convertFromWssp(echoInNormalized, echoOutNormalized);

        // Display converted policy
        log.info("\n\nConverted policy outline:\n" + ssbPolicy);
        log.info("\n\nConverted policy XML:\n" + WspWriter.getPolicyXml(ssbPolicy));
    }
}
