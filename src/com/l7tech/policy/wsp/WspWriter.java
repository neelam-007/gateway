package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Given a policy tree, emit an XML version of it.
 * User: mike
 * Date: Jun 11, 2003
 * Time: 4:06:17 PM
 */
public class WspWriter {
    private OutputStream output;
    Document document;

    private WspWriter(OutputStream output) {
        try {
            this.output = output;
            this.document = XmlUtil.stringToDocument("<Policy xmlns=\"" + WspConstants.POLICY_NS + "\"/>");
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Write the policy tree rooted at assertion to the given output stream
     * as XML.
     * @param assertion     the policy tree to write as XML
     * @param output        the OutputStream to send it to
     * @throws IOException  if there was a problem writing to the output stream
     * @throws IllegalArgumentException if there was a problem with the policy being serialized
     */
    public static void writePolicy(Assertion assertion, OutputStream output) throws IOException {
        WspWriter writer = new WspWriter(output);
        try {
            writer.emitNode(assertion, writer.document.getDocumentElement());
        } catch (StackOverflowError e) {
            throw new InvalidPolicyTreeException("Policy is too deeply nested to be processed");
        } catch (Exception e) {
            if (e instanceof IOException)
                throw (IOException)e;
            throw new InvalidPolicyTreeException("Unable to serialize this policy tree", e);
        }
        writer.writeToOutputStream();
    }

    private void writeToOutputStream() throws IOException {
        XmlUtil.documentToOutputStream(document, output);
    }

    /**
     * Obtain the XML representation of the given policy tree.
     * @param assertion     the policy tree to examine
     * @return              a string containing XML
     */
    public static String getPolicyXml(Assertion assertion) {
        final StringWriter sw = new StringWriter();
        OutputStream swo = new OutputStream() {
            public void write(int b) throws IOException {
                sw.write(b);
            }
        };
        try {
            writePolicy(assertion, swo);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException writing to StringWriter", e); // shouldn't ever happen
        }
        return sw.toString();
    }


    private void emitCompositeAssertion(CompositeAssertion cass, Node container) throws IOException {
        WspConstants.AssertionMapping mapping = WspConstants.findAssertionMappingByAssertion(WspConstants.supportedCompositeAssertions, cass);
        if (mapping == null)
            throw new InvalidPolicyTreeException("Invalid policy: unknown CompositeAssertion: " + cass.getClass());
        String tag = mapping.tag;

        Element element = document.createElement(tag);
        container.appendChild(element);
        List kids = cass.getChildren();
        for (Iterator i = kids.iterator(); i.hasNext();) {
            Assertion kid = (Assertion) i.next();
            emitNode(kid, element);
        }
    }

    private void emitNode(Assertion assertion, Node container) throws IOException {
        if (assertion instanceof CompositeAssertion) {
            emitCompositeAssertion((CompositeAssertion) assertion, container);
            return;
        }

        WspConstants.AssertionMapping mapping = WspConstants.findAssertionMappingByAssertion(WspConstants.supportedLeafAssertions, assertion);
        if (mapping == null)
            throw new InvalidPolicyTreeException("Unrecognized policy assertion type: " + assertion.getClass());
        String tag = mapping.tag;

        Element element = document.createElement(tag);
        container.appendChild(element);
        try {
            emitProperties(assertion, element);
        } catch (InvocationTargetException e) {
            throw new InvalidPolicyTreeException("Unable to serialize this policy", e);
        } catch (IllegalAccessException e) {
            throw new InvalidPolicyTreeException("Unable to serialize this policy", e);
        }
    }

    private void emitProperties(Assertion assertion, Element element)
            throws InvocationTargetException, IllegalAccessException
    {
        Class ac = assertion.getClass();
        Map setters = new HashMap();
        Map getters = new HashMap();
        Method[] methods = ac.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String name = method.getName();
            if (name.startsWith("is") && name.length() > 2 && method.getReturnType().equals(boolean.class))
                getters.put(name.substring(2), method);
            else if (name.startsWith("get") && name.length() > 3)
                getters.put(name.substring(3), method);
            else if (name.startsWith("set") && name.length() > 3)
                setters.put(name.substring(3) + ":" + method.getParameterTypes()[0], method);
        }
        for (Iterator i = getters.keySet().iterator(); i.hasNext();) {
            String parm = (String) i.next();
            if (WspConstants.isIgnorableProperty(parm))
                continue;
            Method getter = (Method) getters.get(parm);
            if (getter == null)
                throw new InvalidPolicyTreeException("Assertion failed"); // can't happen
            Method setter = (Method) setters.get(parm + ":" + getter.getReturnType());
            if (setter == null)
                throw new InvalidPolicyTreeException("WspWriter: Warning: Assertion " + assertion.getClass() + ": no setter found for parameter " + parm);
            Class returnType = getter.getReturnType();
            if (!setter.getParameterTypes()[0].equals(returnType))
                throw new InvalidPolicyTreeException("Assertion has getter and setter for " + parm + " which disagree about its type");
            WspConstants.TypeMapping tm = WspConstants.findTypeMappingByClass(returnType);
            if (tm == null)
                throw new InvalidPolicyTreeException("Assertion " + assertion.getClass() + " has property \"" + parm + "\" with unsupported type " + returnType);
            element.appendChild(tm.freeze(document, parm, getter.invoke(assertion, new Object[0])));
        }
    }
}
