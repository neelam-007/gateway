/*
 * $Id$
 *
 * The contents of this file are subject to the Mozilla Public License 
 * Version 1.1 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at 
 * http://www.mozilla.org/MPL/ 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License 
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is eXchaNGeR browser code. (org.xngr.browser.*)
 *
 * The Initial Developer of the Original Code is Cladonia Ltd.. Portions created 
 * by the Initial Developer are Copyright (C) 2003 the Initial Developer. 
 * All Rights Reserved. 
 *
 * Contributor(s): Edwin Dankert <edankert@cladonia.com>
 */
package com.l7tech.console.xmlviewer.util;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.NamespaceStack;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * <p><code>XMLWriter</code> takes a DOM4J tree and formats it to a
 * stream as XML.
 * It can also take SAX events too so can be used by SAX clients as this object
 * implements the {@link ContentHandler} and {@link LexicalHandler} interfaces.
 * as well. This formatter performs typical document
 * formatting.  The XML declaration and processing instructions are
 * always on their own lines. An {@link OutputFormat} object can be
 * used to define how whitespace is handled when printing and allows various
 * configuration options, such as to allow suppression of the XML declaration,
 * the encoding declaration or whether empty documents are collapsed.</p>
 * <p/>
 * <p> There are <code>write(...)</code> methods to print any of the
 * standard DOM4J classes, including <code>Document</code> and
 * <code>Element</code>, to either a <code>Writer</code> or an
 * <code>OutputStream</code>.  Warning: using your own
 * <code>Writer</code> may cause the writer's preferred character
 * encoding to be ignored.  If you use encodings other than UTF-8, we
 * recommend using the method that takes an OutputStream instead.
 * </p>
 *
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @author Joseph Bowbeer
 * @version $Revision$
 */
public class XMLFormatter extends XMLWriter {

    /**
     * The Stack of namespaceStack written so far
     */
    private NamespaceStack namespaceStack = new NamespaceStack();
    private int indentLevel = 0;

    public XMLFormatter(Writer writer) {
        super(writer);

        namespaceStack.push(Namespace.NO_NAMESPACE);
    }

    public XMLFormatter(Writer writer, OutputFormat format) {
        super(writer, format);

        namespaceStack.push(Namespace.NO_NAMESPACE);
    }

    public XMLFormatter() {
        super();

        namespaceStack.push(Namespace.NO_NAMESPACE);
    }

    public XMLFormatter(OutputStream out) throws UnsupportedEncodingException {
        super(out);

        namespaceStack.push(Namespace.NO_NAMESPACE);
    }

    public XMLFormatter(OutputStream out, OutputFormat format) throws UnsupportedEncodingException {
        super(out, format);

        namespaceStack.push(Namespace.NO_NAMESPACE);
    }

    public XMLFormatter(OutputFormat format) throws UnsupportedEncodingException {
        super(format);

        namespaceStack.push(Namespace.NO_NAMESPACE);
    }

    /**
     * Set the initial indentation level.  This can be used to output
     * a document (or, more likely, an element) starting at a given
     * indent level, so it's not always flush against the left margin.
     * Default: 0
     *
     * @param indentLevel the number of indents to start with
     */
    public void setIndentLevel(int indentLevel) {
        this.indentLevel = indentLevel;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            writePrintln();
            indent();
            writer.write("<");
            writer.write(qName);
            writeNamespaces();
            writeAttributes(attributes);
            writer.write(">");
            ++indentLevel;
            lastOutputNodeType = Node.ELEMENT_NODE;

            super.startElement(namespaceURI, localName, qName, attributes);
        } catch (IOException e) {
            handleException(e);
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        try {
            --indentLevel;
            if (lastOutputNodeType == Node.ELEMENT_NODE) {
                writePrintln();
                indent();
            }

            // XXXX: need to determine this using a stack and checking for
            // content / children
            boolean hadContent = true;
            if (hadContent) {
                writeClose(qName);
            } else {
                writeEmptyElementClose(qName);
            }
            lastOutputNodeType = Node.ELEMENT_NODE;

            super.endElement(namespaceURI, localName, qName);
        } catch (IOException e) {
            handleException(e);
        }
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected void writeElement(Element element) throws IOException {
        int size = element.nodeCount();
        String qualifiedName = element.getQualifiedName();

        boolean hasElement = false;
        boolean hasText = false;

        // first test to see if this element has mixed content...
        for (int i = 0; i < size; i++) {
            Node node = element.node(i);

            if (node instanceof Element) {
                hasElement = true;
            } else if (node instanceof Text) {
                hasText = true;
            }
        }

        writePrintln();

        if (!(hasText && hasElement)) {
            indent();
        }

        writer.write("<");
        writer.write(qualifiedName);

        int previouslyDeclaredNamespaces = namespaceStack.size();

        Namespace ns = element.getNamespace();

        if (isNamespaceDeclaration(ns)) {
            namespaceStack.push(ns);
            writeNamespace(ns);
        }

        // Print out additional namespace declarations
        for (int i = 0; i < size; i++) {
            Node node = element.node(i);

            if (node instanceof Namespace) {
                Namespace additional = (Namespace)node;

                if (isNamespaceDeclaration(additional)) {
                    namespaceStack.push(additional);
                    writeNamespace(additional);
                }
            }
        }

        writeAttributes(element);

        lastOutputNodeType = Node.ELEMENT_NODE;

        if (size <= 0) {
            writeEmptyElementClose(qualifiedName);
        } else {
            writer.write(">");

            if (!hasElement) { // text only
                // we have at least one text node so lets assume
                // that its non-empty
                writeElementContent(element);
            } else if (hasElement && hasText) { // mixed
                // Mixed content
                Node lastNode = writeMixedElementContent(element);

                if (!((lastNode != null &&
                  lastNode.getNodeType() == Node.ELEMENT_NODE &&
                  ((Element)lastNode).nodeCount() == 0))) {
                    writePrintln();
                }
            } else { // hasElement && !hasText
                ++indentLevel;
                writeElementContent(element);
                --indentLevel;

                writePrintln();
                indent();
            }

            writer.write("</");
            writer.write(qualifiedName);
            writer.write(">");
//            writePrintln();

        }

        // remove declared namespaceStack from stack
        while (namespaceStack.size() > previouslyDeclaredNamespaces) {
            namespaceStack.pop();
        }

        lastOutputNodeType = Node.ELEMENT_NODE;
    }


    protected void writeMixedElement(Element element) throws IOException {
        int size = element.nodeCount();
        String qualifiedName = element.getQualifiedName();

        writer.write("<");
        writer.write(qualifiedName);

        int previouslyDeclaredNamespaces = namespaceStack.size();
        Namespace ns = element.getNamespace();

        if (isNamespaceDeclaration(ns)) {
            namespaceStack.push(ns);
            writeNamespace(ns);
        }

        // Print out additional namespace declarations
        boolean textOnly = true;
        for (int i = 0; i < size; i++) {
            Node node = element.node(i);
            if (node instanceof Namespace) {
                Namespace additional = (Namespace)node;
                if (isNamespaceDeclaration(additional)) {
                    namespaceStack.push(additional);
                    writeNamespace(additional);
                }
            } else if (node instanceof Element) {
                textOnly = false;
            }
        }

        writeAttributes(element);

        lastOutputNodeType = Node.ELEMENT_NODE;

        if (size <= 0) {
            writeEmptyElementClose(qualifiedName);
            writePrintln();
        } else {
            writer.write(">");
            writeMixedElementContent(element);
            writer.write("</");
            writer.write(qualifiedName);
            writer.write(">");
        }

        // remove declared namespaceStack from stack
        while (namespaceStack.size() > previouslyDeclaredNamespaces) {
            namespaceStack.pop();
        }

        lastOutputNodeType = Node.ELEMENT_NODE;
    }

    /**
     * Outputs the content of the given element. If whitespace trimming is
     * enabled then all adjacent text nodes are appended together before
     * the whitespace trimming occurs to avoid problems with multiple
     * text nodes being created due to text content that spans parser buffers
     * in a SAX parser.
     */
    protected Node writeMixedElementContent(Element element) throws IOException {
        Node previousNode = null;

        for (int i = 0, size = element.nodeCount(); i < size; i++) {
            Node node = element.node(i);
            Node nextNode = null;

            if (i + 1 < size) {
                nextNode = element.node(i + 1);
            }

            if (node instanceof Text) {
                String text = node.getText();

                // previous node was an empty element-node <element/>
                if (previousNode != null &&
                  previousNode.getNodeType() == Node.ELEMENT_NODE &&
                  ((Element)previousNode).nodeCount() == 0) {

                    text = trimStart(text); // trimStart???
                }

                // next node is an empty element-node <element/>
                if (nextNode != null &&
                  nextNode.getNodeType() == Node.ELEMENT_NODE &&
                  ((Element)nextNode).nodeCount() == 0) {

                    text = trimEnd(text);
                }

                text = trimSpaces(text);

                writeString(text);
            } else {
                writeMixedNode(node);
            }

            previousNode = node;
        }

        return previousNode;
    }

    protected void writeMixedNode(Node node) throws IOException {
        int nodeType = node.getNodeType();
        switch (nodeType) {
            case Node.ELEMENT_NODE:
                writeMixedElement((Element)node);
                break;
            case Node.ATTRIBUTE_NODE:
                writeAttribute((Attribute)node);
                break;
            case Node.TEXT_NODE:
                writeString(node.getText());
                //write((Text) node);
                break;
            case Node.CDATA_SECTION_NODE:
                writeCDATA(node.getText());
                break;
            case Node.ENTITY_REFERENCE_NODE:
                writeEntity((Entity)node);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                writeProcessingInstruction((ProcessingInstruction)node);
                break;
            case Node.COMMENT_NODE:
                writeComment(node.getText());
                break;
            case Node.DOCUMENT_NODE:
                write((Document)node);
                break;
            case Node.DOCUMENT_TYPE_NODE:
                writeDocType((DocumentType)node);
                break;
            case Node.NAMESPACE_NODE:
                // Will be output with attributes
                //write((Namespace) node);
                break;
            default:
                throw new IOException("Invalid node type: " + node);
        }
    }

    /**
     * Writes the attributes of the given element
     */
    protected void writeAttributes(Element element) throws IOException {

        // I do not yet handle the case where the same prefix maps to
        // two different URIs. For attributes on the same element
        // this is illegal; but as yet we don't throw an exception
        // if someone tries to do this
        for (int i = 0, size = element.attributeCount(); i < size; i++) {
            Attribute attribute = element.attribute(i);
            Namespace ns = attribute.getNamespace();
            if (ns != null && ns != Namespace.NO_NAMESPACE && ns != Namespace.XML_NAMESPACE) {
                String prefix = ns.getPrefix();
                String uri = namespaceStack.getURI(prefix);
                if (!ns.getURI().equals(uri)) { // output a new namespace declaration
                    writeNamespace(ns);
                    namespaceStack.push(ns);
                }
            }

            writer.write(" ");
            writer.write(attribute.getQualifiedName());
            writer.write("=\"");
            writeEscapeAttributeEntities(attribute.getValue());
            writer.write("\"");
        }
    }

    protected void indent() throws IOException {
        String indent = getOutputFormat().getIndent();
        if (indent != null && indent.length() > 0) {
            for (int i = 0; i < indentLevel; i++) {
                writer.write(indent);
            }
        }
    }

    protected boolean isNamespaceDeclaration(Namespace ns) {
        if (ns != null && ns != Namespace.XML_NAMESPACE) {
            String uri = ns.getURI();
            if (uri != null) {
                if (!namespaceStack.contains(ns)) {
                    return true;

                }
            }
        }
        return false;

    }

    private String trimStart(String string) {
        int len = string.length();
        char[] val = string.toCharArray();    /* avoid getfield opcode */
        int index = 0;

        while ((index < len) && (val[index] <= ' ')) {
            index++;
        }

        return (index > 0) ? string.substring(index, len) : string;
    }

    private String trimEnd(String string) {
        int len = string.length();
        char[] val = string.toCharArray();    /* avoid getfield opcode */

        while ((len > 0) && (val[len - 1] <= ' ')) {
            len--;
        }

        return ((len < string.length())) ? string.substring(0, len) : string;
    }

    private String trimSpaces(String string) {
        StringBuffer buffer = new StringBuffer();
        int len = string.length();
        char[] val = string.toCharArray();    /* avoid getfield opcode */

        // trim spaces till first normal character.
        for (int i = 0; i < val.length; i++) {
            if ((val[i] < ' ')) {
                buffer.append(val[i]);
            } else if (val[i] > ' ') {
                break;
            }
        }

        buffer.append(string.trim());

        // trim spaces till last normal character.
        for (int i = val.length; i > 0; i--) {
            if ((val[i - 1] < ' ')) {
                buffer.append(val[i - 1]);
            } else if (val[i - 1] > ' ') {
                break;
            }
        }

        return buffer.toString();
    }
}
