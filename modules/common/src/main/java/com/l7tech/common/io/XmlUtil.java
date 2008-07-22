package com.l7tech.common.io;

import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import javax.xml.transform.URIResolver;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;
import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Stack;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.logging.Logger;

import com.l7tech.util.*;
import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.transform.W3CCanonicalizer2WC;
import com.ibm.xml.dsig.transform.ExclusiveC11r;

/**
 * XmlUtil extends DomUtils to provide parsing / io features.
 *
 * <p>Thread-local XML parsing and pretty-printing utilities.</p>
 */
public class XmlUtil extends DomUtils {
    private static final Logger logger = Logger.getLogger(XmlUtil.class.getName());

    public static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n";
    public static final String TEXT_XML = "text/xml";
    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String XERCES_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    private static final EntityResolver SAFE_ENTITY_RESOLVER = new EntityResolver() {
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
            String msg = "Document referred to an external entity with system id '" + systemId + "'";
            logger.warning( msg );
            throw new SAXException(msg);
        }
    };

    /**
     * Different from {@link #SAFE_ENTITY_RESOLVER} in that it throws {@link IOException} rather than {@link SAXException}.
     */
    private static final EntityResolver XSS4J_SAFE_ENTITY_RESOLVER = new EntityResolver() {
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            String msg = "Document referred to an external entity with system id '" + systemId + "'";
            logger.warning( msg );
            throw new IOException(msg);
        }
    };

    private static final LSResourceResolver SAFE_LS_RESOURCE_RESOLVER = new LSResourceResolver() {
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            String msg = "Document referred to an external entity with system id '" + systemId + "' of type '" + type + "'";
            logger.warning( msg );
            return new LSInputImpl(); // resolve to nothing, causes error
        }
    };

    private static final URIResolver SAFE_URI_RESOLVER = new URIResolver(){
        public Source resolve(final String href, final String base) throws TransformerException {
            throw new TransformerException("External entities are not supported '"+href+"'.");
        }
    };

    /**
     * Error handler without the console output.
     */
    private static final ErrorHandler QUIET_ERROR_HANDLER = new ErrorHandler() {
        public void warning( SAXParseException exception) {}
        public void error(SAXParseException exception) {}
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    };
    private static ThreadLocal documentBuilder = new ThreadLocal() {
        protected synchronized Object initialValue() {
            try {
                DocumentBuilder builder = dbf.newDocumentBuilder();
                builder.setEntityResolver(XSS4J_SAFE_ENTITY_RESOLVER);
                builder.setErrorHandler(QUIET_ERROR_HANDLER);
                return builder;
            } catch ( ParserConfigurationException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };
    private static ThreadLocal documentBuilderAllowingDoctype = new ThreadLocal() {
        protected synchronized Object initialValue() {
            try {
                DocumentBuilder builder = dbfAllowingDoctype.newDocumentBuilder();
                builder.setEntityResolver(XSS4J_SAFE_ENTITY_RESOLVER);
                builder.setErrorHandler(QUIET_ERROR_HANDLER);
                return builder;
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };
    private static ThreadLocal formattedXMLSerializer = new ThreadLocal() {
        protected synchronized Object initialValue() {
            XMLSerializer xmlSerializer = new XMLSerializer();
            OutputFormat of = new OutputFormat();
            of.setIndent(4);
            xmlSerializer.setOutputFormat(of);
            return xmlSerializer;
        }
    };
    private static ThreadLocal encodingXMLSerializer = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new XMLSerializer();
        }
    };
    private static ThreadLocal transparentXMLSerializer = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new W3CCanonicalizer2WC();
        }
    };
    private static ThreadLocal exclusiveCanonicalizer = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new ExclusiveC11r();
        }
    };
    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static Schema SCHEMA_SCHEMA;
    private static DocumentBuilderFactory dbfAllowingDoctype = DocumentBuilderFactory.newInstance();
    static {
        dbf.setNamespaceAware(true);
        dbf.setAttribute(XERCES_DISALLOW_DOCTYPE, Boolean.TRUE);
    }

    static {
        dbfAllowingDoctype.setNamespaceAware(true);
    }


    /**
     * Returns a stateless, thread-safe {@link org.xml.sax.EntityResolver} that throws a SAXException upon encountering
     * external entity references but otherwise works as expected.
     *
     * This EntityResolver should be used in ALL XML parsing to avoid DOS attacks.
     * @return a safe {@link org.xml.sax.EntityResolver} instance.
     */
    public static EntityResolver getSafeEntityResolver() {
        return SAFE_ENTITY_RESOLVER;
    }

    /**
     * Returns a stateless, thread-safe {@link org.xml.sax.EntityResolver} that throws a SAXException upon encountering
     * external entity references but otherwise works as expected.
     *
     * This EntityResolver should be used in ALL XML parsing to avoid DOS attacks.
     * @return a safe {@link org.xml.sax.EntityResolver} instance.
     */
    public static EntityResolver getXss4jEntityResolver() {
        return XSS4J_SAFE_ENTITY_RESOLVER;
    }

    /**
     * Returns a stateless, thread-safe {@link org.w3c.dom.ls.LSResourceResolver} that resolves all schema and entity
     * references to an uninitialized LSInput implementation.
     *
     * @return a safe {@link org.w3c.dom.ls.LSResourceResolver} instance.
     */
    public static LSResourceResolver getSafeLSResourceResolver() {
        return SAFE_LS_RESOURCE_RESOLVER;
    }

    /**
     * Returns a stateless, thread-safe {@link javax.xml.transform.URIResolver} that throws a TransformerException on
     * encountering any uri.
     *
     * This URIResolver should be used in ALL TrAX processing for security.
     *
     * @return a safe {@link javax.xml.transform.URIResolver} instance.
     * @see javax.xml.transform.TransformerFactory#setURIResolver TransformerFactory.setURIResolver
     * @see javax.xml.transform.Transformer#setURIResolver Transformer.setURIResolver
     */
    public static URIResolver getSafeURIResolver() {
        return SAFE_URI_RESOLVER;
    }

    private static DocumentBuilder getDocumentBuilder() {
        return (DocumentBuilder)documentBuilder.get();
    }

    private static DocumentBuilder getDocumentBuilderAllowingDoctype() {
        return (DocumentBuilder)documentBuilderAllowingDoctype.get();
    }

    /** @return a new, empty DOM document with absolutely nothing in it. */
    public static Document createEmptyDocument() {
        return getDocumentBuilder().newDocument();
    }

    /** @return a new DOM document contianing only a single empty element. */
    public static Document createEmptyDocument(String rootElementName, String rootPrefix, String rootNs) {
        // TODO make a better-performing version of this
        try {
            if (rootElementName == null || rootElementName.length() < 1) throw new IllegalArgumentException();
            final String xml;
            if (rootNs != null) {
                if (rootPrefix != null) {
                    final String el = rootPrefix + ":" + rootElementName;
                    xml = "<" + el + " xmlns:" + rootPrefix + "=\"" + rootNs + "\"/>";
                } else {
                    xml = "<" + rootElementName + " xmlns=\"" + rootNs + "\"/>";
                }
            } else {
                xml = "<" + rootElementName + "/>";
            }
            return stringToDocument(xml);
        } catch ( SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public static Document stringToDocument(String inputXmlNotAUrl) throws SAXException {
        Reader reader = new StringReader(inputXmlNotAUrl);
        try {
            return parse(reader, false);
        } catch ( IOException e) {  // can't happen
            throw new SAXException("Unable to parse this XML string: " + e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Version of stringToDocument for use when you know that the XML is well-formed.
     *
     * @param inputXmlNotAUrl A known well-formed XML string.
     * @return the Document
     * @throws IllegalArgumentException if the given string is not XML
     */
    public static Document stringAsDocument(String inputXmlNotAUrl) {
        try {
            return stringToDocument(inputXmlNotAUrl);
        } catch (SAXException se) {
            throw new IllegalArgumentException("Unable to parse '"+inputXmlNotAUrl+"' as XML.", se);
        }
    }

    /**
     * Create an XML document from the given InputStream. If the InputStream
     * data does NOT contain charset information and you know the charset you
     * should call the parse method that takes an encoding argument.
     *
     * @param input the InputStream that will produce the document's bytes
     * @return the parsed document
     * @throws java.io.IOException if an IO error occurs
     * @throws org.xml.sax.SAXException if there is a parsing error
     */
    public static Document parse( InputStream input) throws IOException, SAXException {
        return parse(new InputSource(input), false);
    }

    /**
     * Create an XML document from the given InputStream. If the InputStream
     * data contains charset information this SHOULD be ignored by the parser
     * and the specified one used.
     *
     * @param input the InputStream that will produce the document's bytes
     * @return the parsed document
     * @throws java.io.IOException if an IO error occurs
     * @throws org.xml.sax.SAXException if there is a parsing error
     */
    public static Document parse(InputStream input, String encoding) throws IOException, SAXException {
        InputSource is = new InputSource(input);
        is.setEncoding(encoding);
        return parse(is, false);
    }

    /**
     * Create an XML document from the given InputStream. If the InputStream
     * data does NOT contain charset information and you know the charset you
     * should create a Reader and pass that instead.
     *
     * @param input the InputStream that will produce the document's bytes
     * @param allowDoctype true to allow DOCTYPE processing instructions. <b>NOTE</b>: Don't allow DOCTYPEs on "foreign" documents (e.g. any document received over a network interface).
     * @return the parsed DOM Document
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public static Document parse(InputStream input, boolean allowDoctype) throws IOException, SAXException {
        return parse(new InputSource(input), allowDoctype);
    }

    /**
     * Create an XML document from the given Reader.
     *
     * <p>This method is intended to be used when you have character data to
     * parse. If you know the encoding but have binary data, call the method
     * that takes an InputStream and an encoding.</p>
     *
     * @param input the Reader that will produce the document's characters
     * @param allowDoctype true to allow DOCTYPE processing instructions. <b>NOTE</b>: Don't allow DOCTYPEs on "foreign" documents (e.g. any document received over a network interface).
     * @return the parsed document
     * @throws java.io.IOException if an IO error occurs
     * @throws org.xml.sax.SAXException if there is a parsing error
     */
    public static Document parse(Reader input, boolean allowDoctype) throws IOException, SAXException {
        return parse(new InputSource(input), allowDoctype);
    }

    /**
     * Create an XML document from the given SAX InputSource.
     *
     * @param source the input source
     * @param allowDoctype true to allow DOCTYPE processing instructions. <b>NOTE</b>: Don't allow DOCTYPEs on "foreign" documents (e.g. any document received over a network interface).
     * @return the parsed DOM Document
     * @throws java.io.IOException if an IO error occurs
     * @throws org.xml.sax.SAXException if there is a parsing error
     */
    public static Document parse(InputSource source, boolean allowDoctype) throws IOException, SAXException {
        DocumentBuilder parser = allowDoctype
                ? getDocumentBuilderAllowingDoctype()
                : getDocumentBuilder();
        return parser.parse(source);
    }

    private static XMLSerializer getFormattedXmlSerializer() {
        return (XMLSerializer) formattedXMLSerializer.get();
    }

    private static XMLSerializer getEncodingXmlSerializer() {
        return (XMLSerializer) encodingXMLSerializer.get();
    }

    private static Canonicalizer getTransparentXMLSerializer() {
        return (Canonicalizer)transparentXMLSerializer.get();
    }

    private static Canonicalizer getExclusiveCanonicalizer() {
        return (Canonicalizer)exclusiveCanonicalizer.get();
    }

    public static void nodeToOutputStream( Node node, OutputStream os) throws IOException {
        Canonicalizer canon = getTransparentXMLSerializer();
        canon.canonicalize(node, os);
    }

    /**
     * Serializes the specified node using an exclusive canonicalizer.
     * @param node
     * @param os
     * @throws java.io.IOException
     */
    public static void canonicalize(Node node, OutputStream os) throws IOException {
        // TODO inclusive namespaces?
        Canonicalizer canon = getExclusiveCanonicalizer();
        canon.canonicalize(node, os);
    }

    /**
     * Add whitespace to a document for readability.
     *
     * @param document The document to modify
     * @param indent True to indent each nested element
     */
    public static void format(Document document, boolean indent) {
        format(document.getDocumentElement(), 0, indent ? 2 : 0);
    }

    /**
     * Add whitespace to an element for readability.
     *
     * @param element The element to modify
     * @param initialIndent the initial indentation
     * @param levelIndent the indentation per level/depth
     */
    @SuppressWarnings({"unchecked"})
    public static void format( Element element, int initialIndent, int levelIndent) {
        Document document = element.getOwnerDocument();

        PaddingCharSequence pcs = new PaddingCharSequence(' ', initialIndent);
        if (element.hasChildNodes()) element.appendChild(document.createTextNode("\n"+pcs.toString()));
        pcs.addLength(levelIndent);
        Node currentNode = element.getFirstChild();
        Stack elementStack = new Stack();

        while (currentNode != null) {
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                currentNode.getParentNode().insertBefore(document.createTextNode("\n"+pcs.toString()),currentNode);
                if (hasChildNodesOfType(currentNode, Node.ELEMENT_NODE)) currentNode.appendChild(document.createTextNode("\n"+pcs.toString()));
            }
            if (currentNode.hasChildNodes()) {
                elementStack.push(currentNode);
                pcs.addLength(levelIndent);
                currentNode = currentNode.getFirstChild();
            }
            else {
                Node nextNode = currentNode.getNextSibling();
                if (nextNode == null && !elementStack.isEmpty()) {
                    nextNode = (Node) elementStack.pop();
                    pcs.addLength(-1*levelIndent);
                    currentNode = nextNode.getNextSibling();
                }
                else {
                    currentNode = nextNode;
                }
            }
        }
    }

    public static void nodeToOutputStream(Node node, OutputStream os, String encoding) throws IOException {
        OutputFormat of = new OutputFormat();
        of.setEncoding(encoding);

        XMLSerializer ser = getEncodingXmlSerializer();
        ser.setOutputFormat(of);
        ser.setOutputByteStream(os);
        if (node instanceof Document)
            ser.serialize((Document)node);
        else if (node instanceof Element)
            ser.serialize((Element)node);
        else
            throw new IllegalArgumentException("Node must be either a Document or an Element");
    }

    public static void nodeToFormattedOutputStream(Node node, OutputStream os) throws IOException {
        XMLSerializer ser = getFormattedXmlSerializer();
        ser.setOutputByteStream(os);
        if (node instanceof Document)
            ser.serialize((Document)node);
        else if (node instanceof Element)
            ser.serialize((Element)node);
        else
            throw new IllegalArgumentException("Node must be either a Document or an Element");
    }

    public static String nodeToString(Node node) throws IOException {
        final BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(1024);
        try {
            nodeToOutputStream(node, out);
            return out.toString("UTF-8");
        } finally {
            out.close();
        }
    }

    public static byte[] toByteArray(Node node) throws IOException {
        final BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(1024);
        try {
            nodeToOutputStream(node, out);
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    public static byte[] toByteArray(Node node, String encoding) throws IOException {
        final BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(1024);
        try {
            nodeToOutputStream(node, out, encoding);
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    public static String nodeToFormattedString(Node node) throws IOException {
        final BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(1024);
        try {
            nodeToFormattedOutputStream(node, out);
            return out.toString("UTF-8");
        } finally {
            out.close();
        }
    }

    /**
     * Create a document with an element of another document while maintaining the
     * namespace declarations of the parent elements.
     * @param schema the Element on which to base the new XML.
     * @return the new XML as a String.  Never null.
     * @throws IOException if the serializer has a problem reading the source DOM.
     */
    public static String elementToXml(Element schema) throws IOException {
        DocumentBuilder builder = getDocumentBuilder();
        Document schemadoc = builder.newDocument();
        Element newRootNode = (Element)schemadoc.importNode(schema, true);
        schemadoc.appendChild(newRootNode);
        // remember all namespace declarations of parent elements
        Node node = schema.getParentNode();
        while (node != null) {
            if (node instanceof Element) {
                Element el = (Element)node;
                NamedNodeMap attrsmap = el.getAttributes();
                for (int i = 0; i < attrsmap.getLength(); i++) {
                    Attr attrnode = (Attr)attrsmap.item(i);
                    if (attrnode.getName().startsWith("xmlns:")) {
                        newRootNode.setAttribute(attrnode.getName(), attrnode.getValue());
                    }
                }

            }
            node = node.getParentNode();
        }
        // output to string
        final StringWriter sw = new StringWriter(512);
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        xmlSerializer.serialize(schemadoc);
        return sw.toString();
    }

    @SuppressWarnings({"unchecked", "ForLoopReplaceableByForEach"})
    public static String getSchemaTNS(String schemaSrc) throws BadSchemaException {
        if (schemaSrc == null) {
            throw new BadSchemaException("no xml");
        }
        /* can't do this as it ends up chocking on unresolved imports which is beyond the scope here
        // 1. pass through the javax.xml.validation.SchemaFactory
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        if (resolver != null) {
            factory.setResourceResolver(resolver);
        }
        try {
            factory.newSchema(new StreamSource(new ByteArrayInputStream(schemaSrc.getBytes())));
        } catch (Exception e) {
            throw new BadSchemaException(e);
        }
        */
        /* Replacing with non-xmlbeans version, this is slower but acceptable (means manager does not need XML Beans jar).
        // 2. pass through SchemaDocument
        SchemaDocument sdoc;
        try {
            sdoc = SchemaDocument.Factory.parse(new StringReader(schemaSrc));
        } catch (Exception e) {
            throw new BadSchemaException(e);
        }
        return sdoc.getSchema().getTargetNamespace();
        */
        try {
            Document schemaDocument = parse(new StringReader(schemaSrc), true);
            Schema schemaSchema = getSchemaSchema();
            Validator validator = schemaSchema.newValidator();
            // use a string reader since the DOMSource seems to have problems with type prefixes
            validator.validate(new StreamSource(new StringReader(schemaSrc)));

            String tns = schemaDocument.getDocumentElement().getAttribute("targetNamespace");

            if (tns.length() == 0) {
                tns = null;
            }
            else {
                // find imported namespaces
                Set importedNamespaces = new HashSet();
                NodeList importElements = schemaDocument.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "import");
                for (int n=0; n<importElements.getLength(); n++) {
                    Element importElement = (Element) importElements.item(n);
                    if (importElement.hasAttribute("namespace")) {
                        importedNamespaces.add(importElement.getAttribute("namespace"));
                    }
                }
                // add import for default ns and tns
                importedNamespaces.add(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                importedNamespaces.add(tns);

                // ensure that all "type" attributes are using valid (declared) prefixes and are imported
                NodeList elementElements = schemaDocument.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "element");
                for (int n=0; n<elementElements.getLength(); n++) {
                    Element eleElement = (Element) elementElements.item(n);

                    String [][] qnameAttributes = new String[][] {
                            {"type", null, null},
                            {"ref", null, null},
                            {"substitutionGroup", null, null},
                    };

                    boolean doCheck = false;
                    for (int i=0; i<qnameAttributes.length; i++) {
                        if (eleElement.hasAttribute(qnameAttributes[i][0])) {
                            doCheck = true;
                            qnameAttributes[i][1] = eleElement.getAttribute(qnameAttributes[i][0]);
                            qnameAttributes[i][2] = getNamespacePrefix(qnameAttributes[i][1]);
                        }
                    }
                    if (!doCheck) continue;

                    Map namespaces = DomUtils.getNamespaceMap(eleElement);
                    for (int i=0; i<qnameAttributes.length; i++) {
                        if (qnameAttributes[i][1] == null) continue;

                        String namespace = (String) namespaces.get(qnameAttributes[i][2]);

                        if (namespace == null) {
                            throw new BadSchemaException("Undeclared namespace prefix '"+qnameAttributes[i][2]+"' for "+qnameAttributes[i][0]+" '"+qnameAttributes[i][1]+"'.");
                        }

                        if (!importedNamespaces.contains(namespace)) {
                            throw new BadSchemaException("Unimported namespace prefix '"+qnameAttributes[i][2]+"' for "+qnameAttributes[i][0]+" '"+qnameAttributes[i][1]+"'.");
                        }
                    }
                }
            }

            return tns;
        }
        catch(SAXException se) {
            throw new BadSchemaException(se);
        }
        catch(IOException ioe) {
            throw new BadSchemaException(ioe);
        }
    }

    @SuppressWarnings({"ForLoopReplaceableByForEach"})
    public static void softXSLTransform(Document source, Result result, Transformer transformer, Map params) throws TransformerException {
        if (params != null && !params.isEmpty()) {
            for ( Iterator i = params.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                if (name == null) continue;
                Object value = params.get(name);
                if (value == null) continue;
                transformer.setParameter(name, value);
            }
        }
        transformer.transform(new DOMSource(source), result);
    }

    /**
     * Get a Schema for validating Schema documents.
     *
     * <p>The XSD and DTD files for validating schemas are stored on the classpath.</p>
     *
     * @return The Schema for validating XML Schema documents.
     * @throws org.xml.sax.SAXException if there's something wrong with the build ...
     */
    private static Schema getSchemaSchema() throws SAXException {
        Schema schemaSchema = SCHEMA_SCHEMA;
        if (schemaSchema == null) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(new LSResourceResolver(){
                public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                    LSInputImpl input = new LSInputImpl();
                    // map this to the resource name
                    if ("http://www.w3.org/2001/xml.xsd".equals(systemId))
                        systemId = "xml.xsd";
                    // resolve imports from the classpath
                    input.setByteStream(DomUtils.class.getResourceAsStream("/com/l7tech/common/resources/" + systemId));
                    return input;
                }
            });
            // load the Schema schema resource
            schemaSchema = factory.newSchema(new StreamSource(DomUtils.class.getResourceAsStream("/com/l7tech/common/resources/XMLSchema.xsd")));
            SCHEMA_SCHEMA = schemaSchema;
        }

        return schemaSchema;
    }

    public static class BadSchemaException extends Exception {
        public BadSchemaException(String s){super(s);}
        public BadSchemaException(Throwable e){super(e.getMessage(), e);}
    }
}
