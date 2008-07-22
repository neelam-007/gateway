package com.l7tech.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Initial attempt to create a DOM from an XMLStreamWriter.
 *
 * <p>Should work fine for elements and attributes, nothing else is implemented (e.g. text nodes).</p>
 *
 * TODO find a StAX implementation that supports writing to DOMResults
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class DOMResultXMLStreamWriter implements XMLStreamWriter {

    //- PUBLIC

    /**
     * Create a writer that writes to the given DOMResult.
     *
     * <p>The DOMResult must have a Node that is an Element, Document or
     * DocumentFragment.</p>
     *
     * @param result The DOMResult target.
     */
    public DOMResultXMLStreamWriter(DOMResult result) {
        if (result == null) throw new IllegalArgumentException("result must not be null");
        if (result.getNode() == null) throw new IllegalArgumentException("result must have a node");

        rootNode = result.getNode();
        factory = rootNode instanceof Document ? (Document) rootNode : rootNode.getOwnerDocument();
        currentNode = rootNode;
        closed = false;
        prefixStack = new Stack();
        prefixStack.push(new HashMap());
        namespaceStack = new Stack();
        namespaceStack.push(new HashMap());
    }

    public void close() throws XMLStreamException {
        ensureNotClosed();
        closed = true;
    }

    public void flush() throws XMLStreamException {
        ensureNotClosed();
    }

    public NamespaceContext getNamespaceContext() {
        final Node context = currentNode;

        return new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    prefix = XMLConstants.DEFAULT_NS_PREFIX;
                }

                String uri = null;
                Node current = context;
                String attrName = XMLConstants.DEFAULT_NS_PREFIX.equals(prefix) ?
                        XMLConstants.XMLNS_ATTRIBUTE :
                        XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix;

                while (uri == null && current != null) {
                    NamedNodeMap nnm = current.getAttributes();
                    if (nnm != null) {
                        Attr attribute = (Attr) nnm.getNamedItem(attrName);
                        if (attribute != null) {
                            uri = attribute.getValue();
                        }
                    }
                    current = current.getParentNode();
                }

                return uri;
            }

            public String getPrefix(String namespaceURI) {
                String prefix = null;
                Node current = context;

                while (prefix == null && current != null) {
                    NamedNodeMap nnm = current.getAttributes();
                    if (nnm != null) {
                        for (int n=0; n<nnm.getLength(); n++) {
                            Attr attribute = (Attr) nnm.item(n);
                            if (attribute.getValue().equals(namespaceURI) &&
                                attribute.getName().startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                                if (attribute.getName().equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                                    prefix = XMLConstants.DEFAULT_NS_PREFIX;
                                    break;
                                }
                                else if (attribute.getName().indexOf(':')>0){
                                    prefix = attribute.getName().substring(attribute.getName().indexOf(':')+1);
                                    break;
                                }
                            }
                        }
                    }
                    current = current.getParentNode();
                }

                return prefix;
            }

            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }
        };
    }

    public String getPrefix(String uri) throws XMLStreamException {
        String prefix = (String)((Map) namespaceStack.peek()).get(uri);

        if (prefix == null) {
            prefix = getNamespaceContext().getPrefix(uri);
        }

        return prefix;
    }

    public Object getProperty(String string) throws IllegalArgumentException {
        return null;
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        setPrefix(XMLConstants.DEFAULT_NS_PREFIX, uri);
    }

    public void setNamespaceContext(NamespaceContext namespaceContext) throws XMLStreamException {
        throw new XMLStreamException("Not supported");
    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        ensureNotClosed();
        ((Map)prefixStack.peek()).put(prefix, uri);
        ((Map)namespaceStack.peek()).put(uri, prefix);
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        ensureNotClosed();
        if (currentNode == rootNode) throw new XMLStreamException("Cannot close element (no current element)");
        if (localName.indexOf(':') >= 0) throw new XMLStreamException("Invalid localName '"+localName+"'.");
        Element element = (Element) currentNode;
        element.setAttribute(localName, value);
    }

    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        ensureNotClosed();
        if (currentNode == rootNode) throw new XMLStreamException("Cannot close element (no current element)");
        if (localName.indexOf(':') >= 0) throw new XMLStreamException("Invalid localName '"+localName+"'.");

        String prefix = getNamespaceContext().getPrefix(namespaceURI);
        if (prefix == null || prefix.trim().length()==0) {
            prefix = findAvailablePrefix();
        }
        writeNamespace(prefix, namespaceURI);
        Element element = (Element) currentNode;
        element.setAttributeNS(namespaceURI, prefix + ":" + localName, value);
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        ensureNotClosed();
        if (currentNode == rootNode) throw new XMLStreamException("Cannot close element (no current element)");
        if (localName.indexOf(':') >= 0) throw new XMLStreamException("Invalid localName '"+localName+"'.");

        if ((prefix == null || prefix.length()==0) &&
            (namespaceURI == null || namespaceURI.length()==0)) {
            writeAttribute(localName, value);
        }
        else {
            NamespaceContext nc = getNamespaceContext();
            String uriForPrefix = nc.getNamespaceURI(prefix);
            if (uriForPrefix != null && !uriForPrefix.equals(namespaceURI)) {
                throw new XMLStreamException("Attribute prefix already bound to namespace '"+uriForPrefix+"'.");
            }
            writeNamespace(prefix, namespaceURI);
            Element element = (Element) currentNode;
            element.setAttributeNS(namespaceURI, prefix + ":" + localName, value);
        }
    }

    public void writeCData(String string) throws XMLStreamException {
        ensureNotClosed();
        throw new XMLStreamException("CData not yet supported");
    }

    public void writeCharacters(char[] chars, int i, int i1) throws XMLStreamException {
        ensureNotClosed();
        throw new XMLStreamException("Text not yet supported");
    }

    public void writeCharacters(String string) throws XMLStreamException {
        ensureNotClosed();
        throw new XMLStreamException("Text not yet supported");
    }

    public void writeComment(String string) throws XMLStreamException {
        ensureNotClosed();
        throw new XMLStreamException("Comments not yet supported");
    }

    public void writeDefaultNamespace(String namespaceUri) throws XMLStreamException {
        ensureNotClosed();
        writeNamespace(XMLConstants.DEFAULT_NS_PREFIX, namespaceUri);
    }

    public void writeDTD(String string) throws XMLStreamException {
        ensureNotClosed();
        throw new XMLStreamException("DTD not yet supported");
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        ensureNotClosed();
        if (localName.indexOf(':') >= 0) throw new XMLStreamException("Invalid localName '"+localName+"'.");
        Node newNode = factory.createElement(localName);
        currentNode.appendChild(newNode);
    }

    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        ensureNotClosed();

        if (localName.indexOf(':') >= 0) throw new XMLStreamException("Invalid localName '"+localName+"'.");
        String prefix = getPrefix(namespaceURI);

        Element newElement = null;
        if (prefix == null || XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            newElement = factory.createElementNS(namespaceURI, localName);
            if (!namespaceURI.equals(getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX))) {
                newElement.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, namespaceURI);
            }
        }
        else {
            newElement = factory.createElementNS(namespaceURI, prefix + ":" + localName);
            if (!namespaceURI.equals(getNamespaceContext().getNamespaceURI(prefix))) {
                newElement.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, namespaceURI);
            }
        }

        currentNode.appendChild(newElement);
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        ensureNotClosed();
        Element newElement = factory.createElementNS(namespaceURI, prefix + ":" + localName);

        if (!namespaceURI.equals(getNamespaceContext().getNamespaceURI(prefix))) {
            newElement.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, namespaceURI);
        }

        currentNode.appendChild(newElement);
    }

    public void writeEndDocument() throws XMLStreamException {
        ensureNotClosed();
    }

    public void writeEndElement() throws XMLStreamException {
        ensureNotClosed();
        popPrefixes();
        if (currentNode == rootNode) throw new XMLStreamException("Cannot close element (no current element)");
        currentNode = currentNode.getParentNode();
    }

    public void writeEntityRef(String string) throws XMLStreamException {
        ensureNotClosed();
        throw new XMLStreamException("Entity references not yet supported");
    }

    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        ensureNotClosed();
        if (currentNode.getNodeType() != Node.ELEMENT_NODE) {
            throw new XMLStreamException("Cannot add namespace (no current element)");
        }
        Element element = (Element) currentNode;
        if (prefix == null ||
            XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
            prefix = XMLConstants.DEFAULT_NS_PREFIX;
        }
        String prefixCurrentUri = getNamespaceContext().getNamespaceURI(prefix);
        if (!namespaceURI.equals(prefixCurrentUri)) {
            String attrName = XMLConstants.DEFAULT_NS_PREFIX.equals(prefix) ?
                    XMLConstants.XMLNS_ATTRIBUTE :
                    XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix;

            element.setAttribute(attrName, namespaceURI);
        }
    }

    public void writeProcessingInstruction(String string) throws XMLStreamException {
        ensureNotClosed();
        throw new XMLStreamException("Processing instructions not yet supported");
    }

    public void writeProcessingInstruction(String string, String string1) throws XMLStreamException {
        ensureNotClosed();
        throw new XMLStreamException("Processing instructions not yet supported");
    }

    public void writeStartDocument() throws XMLStreamException {
        ensureNotClosed();
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        ensureNotClosed();
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        ensureNotClosed();
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        ensureNotClosed();
        pushPrefixes();
        if (localName.indexOf(':') >= 0) throw new XMLStreamException("Invalid localName '"+localName+"'.");
        Node newNode = factory.createElement(localName);
        currentNode.appendChild(newNode);
        currentNode = newNode;
    }

    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        ensureNotClosed();
        pushPrefixes();
        if (localName.indexOf(':') >= 0) throw new XMLStreamException("Invalid localName '"+localName+"'.");
        String prefix = getPrefix(namespaceURI);

        Element newElement = null;
        if (prefix == null || XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            newElement = factory.createElementNS(namespaceURI, localName);
            if (!namespaceURI.equals(getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX))) {
                newElement.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, namespaceURI);
            }
        }
        else {
            newElement = factory.createElementNS(namespaceURI, prefix + ":" + localName);
            if (!namespaceURI.equals(getNamespaceContext().getNamespaceURI(prefix))) {
                newElement.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, namespaceURI);
            }
        }

        currentNode.appendChild(newElement);
        currentNode = newElement;
    }

    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        ensureNotClosed();
        pushPrefixes();
        Element newElement = factory.createElementNS(namespaceURI, prefix + ":" + localName);

        if (!namespaceURI.equals(getNamespaceContext().getNamespaceURI(prefix))) {
            newElement.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, namespaceURI);
        }

        currentNode.appendChild(newElement);
        currentNode = newElement;
    }

    //- PRIVATE

    private final Document factory;
    private final Node rootNode;
    private final Stack prefixStack;
    private final Stack namespaceStack;

    private Node currentNode;
    private boolean closed;

    private void ensureNotClosed() throws XMLStreamException {
        if (closed) {
            throw new XMLStreamException("Stream is closed.");
        }
    }

    private void pushPrefixes() {
        prefixStack.push(new HashMap((Map)prefixStack.peek()));
        namespaceStack.push(new HashMap((Map)namespaceStack.peek()));
    }

    private void popPrefixes() {
        prefixStack.pop();
        namespaceStack.pop();
    }

    private String findAvailablePrefix() {
        NamespaceContext nc = getNamespaceContext();
        Map prefixMap = (Map) prefixStack.peek();
        for (int p=0; p<10000; p++) {
            String prefix = "p" + Integer.toString(p);
            if (nc.getNamespaceURI(prefix) == null &&
                prefixMap.get(prefix) == null) {
                return prefix;
            }
        }
        throw new IllegalStateException("Cannot find available prefix!");
    }
}
