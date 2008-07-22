package com.l7tech.policy.wsp;

import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.UnknownAssertion;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.*;
import java.util.List;

/**
 * The mapping that supports standard java serialization of a class.
 *
 * @author emil
 * @version 16-Feb-2004
 */
class SerializedJavaClassMapping extends BeanTypeMapping {
    private static final String ELEMENT_NAME = "base64SerializedValue";
    private static ClassLoader classLoader = null;

    public SerializedJavaClassMapping(Class clazz, String externalName) {
        super(clazz, externalName);
    }

    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) {
        if (!(object.target instanceof Serializable)) {
            throw new IllegalArgumentException("target not serializable");
        }
        try {
            Serializable se = (Serializable)object.target;
            Element entryElement = element.getOwnerDocument().createElementNS(getNsUri(), getNsPrefix(wspWriter) + ELEMENT_NAME);
            element.appendChild(entryElement);
            entryElement.appendChild(DomUtils.createTextNode(element, objectToBase64(se)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inspect the DOM element and construct the actual object, which at this point is known to be non-null.
     * The default implementation calls stringToObject(value) to create the object, and populateObject() to fill
     * out its fields.
     *
     * @param element The element being deserialized
     * @param value   The simple string value represented by element, if meaningful for this TypeMapping; otherwise "included"
     * @return A TypedReference to the newly deserialized object
     * @throws InvalidPolicyStreamException if the element cannot be deserialized
     */
    protected TypedReference createObject(Element element, String value, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (value == null)
            throw new InvalidPolicyStreamException("Null values not supported");

        List entryElements = TypeMappingUtils.getChildElements(element, ELEMENT_NAME);
        if (entryElements.size() != 1) {
            throw new InvalidPolicyStreamException("Single child element expected with serialized Java mapping");
        }
        Element valueElement = (Element)entryElements.get(0);
        NodeList nl = valueElement.getChildNodes();
        if (nl.getLength() != 1) {
            throw new InvalidPolicyStreamException("Single text element expected with " + ELEMENT_NAME);
        }
        Node node = nl.item(0);
        if (node.getNodeType() != Node.TEXT_NODE) {
            throw new InvalidPolicyStreamException("Child text element expected with " + ELEMENT_NAME);
        }
        Text textNode = (Text)node;
        try {
            try {
                return new TypedReference(clazz, base64ToObject(textNode.getData()), element.getLocalName());
            } catch (ClassNotFoundException e) {
                UnknownAssertion ua = UnknownAssertion.create(element.getLocalName(),
                                                              XmlUtil.nodeToString(element),
                                                              e);
                return new TypedReference(UnknownAssertion.class, ua, element.getLocalName());
            }
        } catch (IOException e) {
            throw new InvalidPolicyStreamException(e);
        }
    }

    /**
     * Set the ClassLoader to use when loading classes.
     *
     * @param classLoader The classloader to use
     */
    static void setClassloader(ClassLoader classLoader) {
        SerializedJavaClassMapping.classLoader = classLoader;
    }

    /**
     * convert a serializable object to a base64 String
     */
    private String objectToBase64(Serializable obj) throws IOException {
        BufferPoolByteArrayOutputStream bos = new BufferPoolByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            String encodedString = HexUtils.encodeBase64(bos.toByteArray(), true);
            oos.close();
            return encodedString;
        } finally {
            bos.close();
        }
    }

    /**
     * Recreates the object from the base64 encoded String
     */
    private Object base64ToObject(String str) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(HexUtils.decodeBase64(str, true));
        ObjectInputStream ois = new ClassLoaderObjectInputStream(bis);

        Object obj = (Object)ois.readObject();
        ois.close();
        bis.close();
        return obj;
    }

    private static final class ClassLoaderObjectInputStream extends ObjectInputStream {
        protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            Class clazz = null;
            ClassNotFoundException recnfe = null;
            try {
                //Note that the classes version is checked later so no need to do so here
                clazz = classLoader!=null ? classLoader.loadClass(desc.getName()) : null;
            }
            catch(ClassNotFoundException cnfe) {
                recnfe = cnfe;
            }

            if (clazz == null) {
                try {
                    clazz = super.resolveClass(desc);
                }
                catch(ClassNotFoundException cnfe) {
                    // ignore
                }
            }

            if (clazz == null) {
                if (recnfe != null)
                    throw recnfe;
                else
                    throw new ClassNotFoundException(desc.getName());
            }

            return clazz;
        }

        public ClassLoaderObjectInputStream(InputStream in) throws IOException {
            super(in);
        }
    }
}