package com.l7tech.skunkworks.wsp.pre32;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
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
class Pre32SerializedJavaClassMapping extends Pre32BeanTypeMapping {
    static final String ELEMENT_NAME = "base64SerializedValue";

    public Pre32SerializedJavaClassMapping(Class clazz, String externalName) {
        super(clazz, externalName);
    }

    protected void populateElement(Element element, Pre32TypedReference object) {
        if (!(object.target instanceof Serializable)) {
            throw new IllegalArgumentException("target not serializable");
        }
        try {
            Serializable se = (Serializable)object.target;
            Element entryElement = element.getOwnerDocument().createElementNS(getNsUri(), getNsPrefix() + ELEMENT_NAME);
            element.appendChild(entryElement);
            entryElement.appendChild(XmlUtil.createTextNode(element, objectToBase64(se)));
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
     * @param value   The simple string value represented by element, if meaningful for this Pre32TypeMapping; otherwise "included"
     * @return A Pre32TypedReference to the newly deserialized object
     * @throws Pre32InvalidPolicyStreamException if the element cannot be deserialized
     */
    protected Pre32TypedReference createObject(Element element, String value, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        if (value == null)
            throw new Pre32InvalidPolicyStreamException("Null values not supported");

        List entryElements = Pre32TypeMappingUtils.getChildElements(element, ELEMENT_NAME);
        if (entryElements.size() != 1) {
            throw new Pre32InvalidPolicyStreamException("Single child element expected with serialized Java mapping");
        }
        Element valueElement = (Element)entryElements.get(0);
        NodeList nl = valueElement.getChildNodes();
        if (nl.getLength() != 1) {
            throw new Pre32InvalidPolicyStreamException("Single text element expected with " + ELEMENT_NAME);
        }
        Node node = nl.item(0);
        if (node.getNodeType() != Node.TEXT_NODE) {
            throw new Pre32InvalidPolicyStreamException("Child text element expected with " + ELEMENT_NAME);
        }
        Text textNode = (Text)node;
        try {
            try {
                return new Pre32TypedReference(clazz, base64ToObject(textNode.getData()), element.getLocalName());
            } catch (ClassNotFoundException e) {
                UnknownAssertion ua = UnknownAssertion.create(element.getLocalName(),
                                                              XmlUtil.nodeToString(element),
                                                              e);
                return new Pre32TypedReference(UnknownAssertion.class, ua, element.getLocalName());
            }
        } catch (IOException e) {
            throw new Pre32InvalidPolicyStreamException(e);
        }
    }


    /**
     * convert a serializable object to a base64 String
     */
    private String objectToBase64(Serializable obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        byte[] bytes = bos.toByteArray();
        String encodedString = HexUtils.encodeBase64(bytes);
        oos.close();
        bos.close();
        return encodedString;
    }

    /**
     * Recreates the object from the base64 encoded String
     */
    private Object base64ToObject(String str) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(HexUtils.decodeBase64(str));
        ObjectInputStream ois = new ObjectInputStream(bis);

        Object obj = (Object)ois.readObject();
        ois.close();
        bis.close();
        return obj;
    }
}