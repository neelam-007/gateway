package com.l7tech.policy.wsp;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Given a policy tree, emit an XML version of it.
 */
public class WspWriter {
    private Document document = null;
    private String targetVersion = null;
    private List<TypeMappingFinder> typeMappingFinders = new ArrayList<TypeMappingFinder>();
    private TypeMappingFinder metaTmf = new TypeMappingFinderWrapper(typeMappingFinders);

    private static ThreadLocal<WspWriter> currentWspWriter = new ThreadLocal<WspWriter>();


    static void setCurrent(WspWriter wspWriter) {
        currentWspWriter.set(wspWriter);
    }
    static WspWriter getCurrent() {
        WspWriter cur = currentWspWriter.get();
        if (cur == null)
            throw new IllegalStateException("No WspWriter currently defined for this thread");
        return cur;
    }

    /**
     * Create a new WspWriter prepared to emit a policy XML in the current (post-3.2, more WS-Policy-compliant) format.
     */
    public WspWriter() {
    }

    /**
     * Create a skeleton of a policy DOM tree.
     *
     * @return a new Document containing an empty Policy node.
     */
    Document createSkeleton() {
        try {
            return createSkeleton("wsp:Policy", null);
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Create a skeleton of a DOM tree, with the specified {@code rootElementName} and optional {@code xmlChildren}.
     *
     * @param rootElementName    the name of the root document element.  Required and cannot be {@code null}.
     * @param xmlChildren        the xml of the node children. Optional and can be {@code null} to indicate no children nodes.
     * @return a new {@code Document} with {@code rootElementName} node optionally having children node from the specified {@code xmlChildren}.
     * @throws SAXException if an error happens while creating document node and parsing children xml.
     */
    Document createSkeleton(
            @NotNull String rootElementName,
            @Nullable final String xmlChildren
    ) throws SAXException {
        final String l7p = ":L7p";
        final StringBuilder sb = new StringBuilder("<");
        sb.append(rootElementName).append(" ");
        sb.append("xmlns:wsp=\"").append(WspConstants.WSP_POLICY_NS).append("\" ");
        sb.append("xmlns").append(l7p).append("=\"").append(WspConstants.L7_POLICY_NS).append("\" ");
        if (xmlChildren != null) {
            sb.append(">");
            sb.append(xmlChildren);
            sb.append("</").append(rootElementName).append(">");
        } else {
            sb.append("/>");
        }
        return XmlUtil.stringToDocument(sb.toString());
    }

    /**
     * Convert the specified assertion into a DOM Element using the default serialization format.
     * This should not be used normally; it is only here for the benefit of the PermissiveWspVisitor, to help
     * it produce UnknownAssertion elements wrapping fragments of unrecognized XML.
     *
     * @param assertion the assertion to convert into an element.
     * @return the Element.  Never null.
     * @throws InvalidPolicyTreeException if an element cannot be created for this assertion
     */
    Element toElement(Assertion assertion) throws InvalidPolicyTreeException {
        if (assertion == null)
            return null;
        Document dom = createSkeleton();
        TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(assertion.getClass(), this);
        if (tm == null)
            throw new InvalidPolicyTreeException("No TypeMapping for assertion class " + assertion.getClass());
        TypedReference ref = new TypedReference(assertion.getClass(), assertion);
        try {
            Element policyElement = tm.freeze(this, ref, dom.getDocumentElement());
            if (policyElement == null)
                throw new InvalidPolicyTreeException("Assertion did not serialize to an element"); // can't happen
            return policyElement;
        } catch (StackOverflowError e) {
            throw new InvalidPolicyTreeException("Policy is too deeply nested to be processed");
        } catch (Exception e) {
            throw new InvalidPolicyTreeException("Unable to serialize this policy tree", e);
        }
    }

    /**
     * Write the policy tree rooted at assertion to the given output stream
     * as XML, using a default WspWriter.
     * @param assertion     the policy tree to write as XML
     * @param output        the OutputStream to send it to
     * @throws IOException  if there was a problem writing to the output stream
     * @throws InvalidPolicyTreeException if there was a problem with the policy being serialized
     */
    public static void writePolicy(Assertion assertion, OutputStream output) throws IOException {
        WspWriter writer = new WspWriter();
        writer.setPolicy(assertion);
        writer.writePolicyXmlToOutputStream(output);
    }

    /**
     * Set the policy tree that we will be serializing.
     * @param assertion  the assertion to serialize.  May be null, in which case the appropriate XML will be created to reflect this.
     * @throws InvalidPolicyTreeException if this policy cannot be serialized.
     */
    public void setPolicy( @Nullable Assertion assertion) {
        try {
            document = createSkeleton();
            if (assertion != null) {
                setCurrent(this);
                TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(assertion.getClass(), this);
                if (tm == null)
                    throw new InvalidPolicyTreeException("No TypeMapping for assertion class " + assertion.getClass());
                TypedReference ref = new TypedReference(assertion.getClass(), assertion);
                tm.freeze(this, ref, document.getDocumentElement());
            }
        } catch (StackOverflowError e) {
            throw new InvalidPolicyTreeException("Policy is too deeply nested to be processed");
        } catch (Exception e) {
            throw new InvalidPolicyTreeException("Unable to serialize this policy tree", e);
        } finally {
            setCurrent(null);
        }
    }

    /**
     * Write the current policy out to the specific OutputStream as XML.
     * @param output the outputstream to which the policy should be writted.  Must not be null.
     * @throws IOException if there is an IOException writing to the output stream or traversing the DOM Document
     * @throws IllegalStateException if {@link #setPolicy} has not yet been called
     */
    public void writePolicyXmlToOutputStream(OutputStream output) throws IOException {
        if (document == null) throw new IllegalStateException("No policy has been set yet");
        XmlUtil.nodeToFormattedOutputStream(document, output);
    }

    /**
     * Return the current policy XML as a string.
     * @return a String containing the policy.  Never null.
     * @throws IOException if there is an IOException traversing the DOM Document
     * @throws IllegalStateException if {@link #setPolicy} has not yet been called
     */
    public String getPolicyXmlAsString() throws IOException {
        if (document == null) throw new IllegalStateException("No policy has been set yet");
        return XmlUtil.nodeToFormattedString(document);
    }

    /**
     * Obtain the XML representation of the given policy tree, using the default policy format, and using UTF-8
     * as the character encoding format.
     * @param assertion     the policy tree to examine.  May be null in which case policy XML will be generated which represents this.
     * @return              a string containing XML
     */
    public static String getPolicyXml(@Nullable Assertion assertion) {
        WspWriter writer = new WspWriter();
        writer.setPolicy(assertion);
        try {
            return writer.getPolicyXmlAsString();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException while serializing policy XML", e); // shouldn't ever happen
        }
    }

    /**
     * Obtain the DOM representation of the given poicy tree, using the default policy format.
     *
     * @param assertion The policy to obtain
     */
    public static Document getPolicyDocument( final @Nullable Assertion assertion ) {
        final WspWriter writer = new WspWriter();
        writer.setPolicy( assertion );
        return writer.document;
    }

    public void addTypeMappingFinder(TypeMappingFinder typeMappingFinder) {
        if (typeMappingFinder == null)
            throw new NullPointerException("typeMappingFinder can't be null");
        typeMappingFinders.add(typeMappingFinder);
    }

    public void removeTypeMappingFinder(TypeMappingFinder typeMappingFinder) {
        typeMappingFinders.remove(typeMappingFinder);
    }

    public TypeMappingFinder getTypeMappingFinder() {
        return metaTmf;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    /**
     * Invoked by BeanTypeMapping to freeze each bean property, once the property has been located.
     * Subclasses can override this to hook in to monitor (or alter) the behavior recursively as each
     * nested bean field is serialized.
     * <p/>
     * This method just calls the {@link TypeMapping#freeze} method on the specifeid type mapping, passing
     * this WspWriter instance, the specified typed reference, and the specified container element.
     *
     * @param tm the already-looked-up TypeMapping.
     * @param tr the typed reference pointing at the property value.
     * @param element the container element to which the field XML should be added.
     * @param getter the get method of the property in question, that was already invoked to look up the typed referene, in case a subclass wishes to examine it for annotations.
     * @param targetObject the object instance whose field is being serialized.  (The getter has already been invoked on this instance to prepare the typted reference).
     */
    protected void freezeBeanProperty(TypeMapping tm, TypedReference tr, Element element, Method getter, Object targetObject) {
        tm.freeze(this, tr, element);
    }
}
