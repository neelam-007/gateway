package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import org.apache.log4j.Category;

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
    private static final Category log = Category.getInstance(WspWriter.class);

    private OutputStream output;
    private int indent = 0;

    private WspWriter(OutputStream output) {
        this.output = output;
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
        writer.emitHeader();
        try {
            writer.emitNode(assertion);
        } catch (StackOverflowError e) {
            throw new InvalidPolicyTreeException("Policy is too deeply nested to be processed");
        } catch (Exception e) {
            if (e instanceof IOException)
                throw (IOException)e;
            throw new InvalidPolicyTreeException("Unable to serialize this policy tree", e);
        }
        writer.emitFooter();
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

    private void emitHeader() throws IOException {
        output.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                     "<Policy" +
                     " xmlns=\"" + WspConstants.POLICY_NS + "\">\n"
                     ).getBytes());
        ++indent;
    }

    private void emitIndentedString(String what) throws IOException {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < indent; ++i)
            sb.append(" ");
        sb.append(what);
        output.write(sb.toString().getBytes());
    }

    private void emitTag(String tag) throws IOException {
        emitIndentedString("<" + tag + ">\n");
        indent++;
    }

    private void emitEndTag(String tag) throws IOException {
        --indent;
        emitIndentedString("</" + tag + ">\n");
    }

    private void emitEmptyTag(String tag, String parm, String value) throws IOException {
        emitIndentedString("<" + tag + " " + parm + "=\"" + value + "\"/>\n");
    }

    private void emitCompositeAssertion(CompositeAssertion cass) throws IOException {
        WspConstants.AssertionMapping mapping = WspConstants.findAssertionMappingByAssertion(WspConstants.supportedCompositeAssertions, cass);
        if (mapping == null)
            throw new InvalidPolicyTreeException("Invalid policy: unknown CompositeAssertion: " + cass.getClass());
        String tag = mapping.tag;

        emitTag(tag);
        List kids = cass.getChildren();
        for (Iterator i = kids.iterator(); i.hasNext();) {
            Assertion kid = (Assertion) i.next();
            emitNode(kid);
        }
        emitEndTag(tag);
    }

    private void emitNode(Assertion assertion) throws IOException {
        if (assertion instanceof CompositeAssertion) {
            emitCompositeAssertion((CompositeAssertion) assertion);
            return;
        }

        WspConstants.AssertionMapping mapping = WspConstants.findAssertionMappingByAssertion(WspConstants.supportedLeafAssertions, assertion);
        if (mapping == null)
            throw new InvalidPolicyTreeException("Unrecognized policy assertion type: " + assertion.getClass());
        String tag = mapping.tag;

        emitTag(tag);
        ++indent;
        try {
            emitProperties(assertion);
        } catch (InvocationTargetException e) {
            throw new InvalidPolicyTreeException("Unable to serialize this policy", e);
        } catch (IllegalAccessException e) {
            throw new InvalidPolicyTreeException("Unable to serialize this policy", e);
        } finally {
            --indent;
        }
        emitEndTag(tag);
    }

    private void emitProperties(Assertion assertion)
            throws InvocationTargetException, IllegalAccessException, IOException
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
            if (setter == null) {
                log.warn("WspWriter: Warning: Assertion " + assertion.getClass() + ": no setter found for parameter " + parm);
                continue;
            }
            Class returnType = getter.getReturnType();
            if (!setter.getParameterTypes()[0].equals(returnType))
                throw new InvalidPolicyTreeException("Assertion has getter and setter for " + parm + " which disagree about its type");
            WspConstants.TypeMapping tm = WspConstants.findTypeMappingByClass(returnType);
            if (tm == null)
                throw new InvalidPolicyTreeException("Assertion " + assertion.getClass() + " has property \"" + parm + "\" with unsupported type " + returnType);
            String stype = tm.typeName;
            String svalue = tm.freezer.freeze(getter.invoke(assertion, new Object[0]));
            if (svalue == null) {
                if (!WspConstants.isNullableType(tm.type))
                    throw new InvalidPolicyTreeException("Assertion " + assertion.getClass() + " has property \"" + parm + "\" which must't be null yet is");
                emitEmptyTag(parm, stype + "Null", "null");
            } else
                emitEmptyTag(parm, stype, svalue);
        }
    }

    private void emitFooter() throws IOException {
        --indent;
        output.write("</Policy>\n".getBytes());
    }
}
