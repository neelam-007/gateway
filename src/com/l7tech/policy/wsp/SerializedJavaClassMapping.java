package com.l7tech.policy.wsp;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.apache.axis.encoding.Base64;

import java.io.*;
import java.util.List;

/**
 * The mapping that supports standard java serialization of a class.
 *
 * @author emil
 * @version 16-Feb-2004
 */
class SerializedJavaClassMapping extends WspConstants.BeanTypeMapping {
    static final String ELEMENT_NAME = "base64SerializedValue";

    public SerializedJavaClassMapping(Class clazz, String externalName) {
        super(clazz, externalName);
    }

    protected void populateElement(Element element, WspConstants.TypedReference object) {
        if (!(object.target instanceof Serializable)) {
            throw new IllegalArgumentException("target not serializable");
        }
        try {
            Serializable se = (Serializable)object.target;
            Element entryElement = element.getOwnerDocument().createElement(ELEMENT_NAME);
            element.appendChild(entryElement);
            entryElement.appendChild(element.getOwnerDocument().createTextNode(objectToBase64(se)));
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
    protected WspConstants.TypedReference createObject(Element element, String value) throws InvalidPolicyStreamException {
        if (value == null)
            throw new InvalidPolicyStreamException("Null values not supported");

        List entryElements = WspConstants.getChildElements(element, ELEMENT_NAME);
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
            return new WspConstants.TypedReference(clazz, base64ToObject(textNode.getData()), element.getNodeName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
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
        String encodedString = Base64.encode(bytes);
        oos.close();
        bos.close();
        return encodedString;
    }

    /**
     * Recreates the object from the base64 encoded String
     */
    private Object base64ToObject(String str) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(Base64.decode(str));
        ObjectInputStream ois = new ObjectInputStream(bis);

        Object obj = (Object)ois.readObject();
        ois.close();
        bis.close();
        return obj;
    }
}