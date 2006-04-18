/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.Assertion;
import org.apache.ws.policy.util.PolicyWriter;
import org.apache.ws.policy.util.PolicyFactory;
import org.w3c.dom.Document;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOutput;
import javax.wsdl.extensions.ExtensibilityElement;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * test the ability to read wssp and emit layer 7 policy
 */
public class WsspParserTest extends TestCase {
    private static Logger log = Logger.getLogger(WsspParserTest.class.getName());

    public WsspParserTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WsspParserTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testParseSample1() throws Exception {
        InputStream is = TestDocuments.getSecurityPolicies().getRound3MsWsdl();
        Wsdl wsdl = Wsdl.newInstance("", XmlUtil.parse(is));
        assertNotNull(wsdl);

        Binding A11binding = null;
        BindingOperation A11echo = null;

        PrintStream out = System.out;
        out.println("Top-level policies:");
        List policies = wsdl.getPolicies();
        for (Iterator i = policies.iterator(); i.hasNext();) {
            Policy policy = (Policy)i.next();
            out.println("  Policy:" + policy.getPolicyURI());
        }

        out.println("Bindings:");
        Collection bindings = wsdl.getBindings();
        for (Iterator i = bindings.iterator(); i.hasNext();) {
            Binding binding = (Binding)i.next();
            out.println("  " + binding.getQName());
            if ("A11Binding".equals(binding.getQName().getLocalPart()))
                A11binding = binding;

            List bexts = binding.getExtensibilityElements();
            printPolicies(bexts, wsdl, out, "      ");

            out.println("      Operations:");
            Collection operations = binding.getBindingOperations();
            for (Iterator k = operations.iterator(); k.hasNext();) {
                BindingOperation operation = (BindingOperation)k.next();
                final String opname = operation.getName();
                out.println("          " + opname);
                if (A11binding == binding && "Echo".equals(opname)) A11echo = operation;

                final String indent = "              ";
                List opexts = operation.getExtensibilityElements();
                printPolicies(opexts, wsdl, out, indent);

                out.println(indent + "  input:");
                BindingInput input = operation.getBindingInput();
                List inexts = input.getExtensibilityElements();
                printPolicies(inexts, wsdl, out, indent + "    ");

                out.println(indent + "  output:");
                BindingOutput output = operation.getBindingOutput();
                List outexts = output.getExtensibilityElements();
                printPolicies(outexts, wsdl, out, indent + "    ");
            }
        }

        assertNotNull(A11binding);
        assertNotNull(A11echo);

        Assertion echoInputPolicy = wsdl.getEffectiveInputPolicy(A11binding, A11echo);
        Assertion blat = echoInputPolicy.normalize(wsdl.getPolicyRegistry());

        PolicyWriter writer = PolicyFactory.getPolicyWriter(PolicyFactory.StAX_POLICY_WRITER);

        out.println("\n\nEffective policy for echo input (binding A11):\n");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.writePolicy((Policy)blat, baos);
        Document d = XmlUtil.parse(new ByteArrayInputStream(baos.toByteArray()));
        XmlUtil.nodeToFormattedOutputStream(d, out);

    }

    private void printPolicies(List opexts, Wsdl wsdl, PrintStream out, String indent) throws Wsdl.BadPolicyReferenceException {
        for (Iterator j = opexts.iterator(); j.hasNext();) {
            ExtensibilityElement ee = (ExtensibilityElement)j.next();
            Policy policy = wsdl.toPolicy(ee);
            if (policy != null) {
                out.println(indent + "Policy:" + policy.getPolicyURI());
            }
        }
    }
}
