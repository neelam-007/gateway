package com.l7tech.common.io;

import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.transform.ExclusiveC11r;
import com.ibm.xml.dsig.transform.W3CCanonicalizer2WC;
import com.l7tech.util.*;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.*;
import org.xml.sax.ext.EntityResolver2;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XmlUtil extends DomUtils to provide parsing / io features.
 *
 * <p>Thread-local XML parsing and pretty-printing utilities.</p>
 */
public class XmlUtil extends DomUtils {
    private static final Logger logger = Logger.getLogger(XmlUtil.class.getName());

    private static final boolean DEFAULT_SERIALIZE_WITH_XSS4J = ConfigFactory.getBooleanProperty( "com.l7tech.common.serializeWithXss4j", false );

    public static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n";
    public static final String TEXT_XML = "text/xml";
    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String XERCES_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    public static final String XERCES_DEFER_NODE_EXPANSION = "http://apache.org/xml/features/dom/defer-node-expansion";
    public static final String XERCES_ATTR_SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";

    private static final EntityResolver SAFE_ENTITY_RESOLVER = new EntityResolver() {
        @Override
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
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            String msg = "Document referred to an external entity with system id '" + systemId + "'";
            logger.warning( msg );
            throw new IOException(msg);
        }
    };

    private static final LSResourceResolver SAFE_LS_RESOURCE_RESOLVER = new LSResourceResolver() {
        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            String msg = "Document referred to an external entity with system id '" + systemId + "' of type '" + type + "'";
            logger.warning( msg );
            return new LSInputImpl(); // resolve to nothing, causes error
        }
    };

    private static final URIResolver SAFE_URI_RESOLVER = new URIResolver(){
        @Override
        public Source resolve(final String href, final String base) throws TransformerException {
            throw new TransformerException("External entities are not supported '"+href+"'.");
        }
    };

    /**
     * Error handler without the console output.
     */
    private static final ErrorHandler QUIET_ERROR_HANDLER = new ErrorHandler() {
        @Override
        public void warning( SAXParseException exception) {}
        @Override
        public void error(SAXParseException exception) {}
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    };
    private static final ErrorHandler STRICT_ERROR_HANDLER =  new ErrorHandler() {
        @Override
        public void warning( final SAXParseException exception ) throws SAXException {}
        @Override
        public void error( final SAXParseException exception ) throws SAXException {
            throw exception;
        }
        @Override
        public void fatalError( final SAXParseException exception ) throws SAXException {
            throw exception;
        }
    };

    private static final XMLReporter SILENT_REPORTER = new XMLReporter() {
        @Override
        public void report( final String message, final String errorType, final Object relatedInformation, final Location location ) throws XMLStreamException {
            throw new XMLStreamException(message, location);
        }
    };

    private static final XMLResolver FAILING_RESOLVER = new XMLResolver() {
        @Override
        public Object resolveEntity( final String publicID, final String systemID, final String baseURI, final String namespace ) throws XMLStreamException {
            throw new XMLStreamException("External entity access forbidden '"+systemID+"' relative to '"+baseURI+"'.");
        }
    };

    private static final ThreadLocal<DocumentBuilder> documentBuilder = new ThreadLocal<DocumentBuilder>() {
        private final DocumentBuilderFactory dbf =
                configureDocumentBuilderFactory( DocumentBuilderFactory.newInstance(), true );

        @Override
        protected synchronized DocumentBuilder initialValue() {
            try {
                return configureDocumentBuilder( dbf.newDocumentBuilder() );
            } catch ( ParserConfigurationException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };
    private static final ThreadLocal<DocumentBuilder> documentBuilderAllowingDoctype = new ThreadLocal<DocumentBuilder>() {
        private final DocumentBuilderFactory dbfAllowingDoctype =
                configureDocumentBuilderFactory( DocumentBuilderFactory.newInstance(), false );

        @Override
        protected synchronized DocumentBuilder initialValue() {
            try {
                return configureDocumentBuilder( dbfAllowingDoctype.newDocumentBuilder() );
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    };
    private static final ThreadLocal<Canonicalizer> transparentXMLSerializer_XSS4J_W3C = new ThreadLocal<Canonicalizer>() {
        @Override
        protected Canonicalizer initialValue() {
            return new W3CCanonicalizer2WC();
        }
    };
    private static final ThreadLocal<Canonicalizer> exclusiveCanonicalizer = new ThreadLocal<Canonicalizer>() {
        @Override
        protected Canonicalizer initialValue() {
            return new ExclusiveC11r();
        }
    };
    private static Schema SCHEMA_SCHEMA;
    private static boolean serializeWithXss4j = DEFAULT_SERIALIZE_WITH_XSS4J;

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

    /**
     * Get a stateless, thread-safe ErrorHandler that throws a SAXException for errors.
     *
     * @return The ErrorHandler instance.
     */
    public static ErrorHandler getStrictErrorHandler() {
        return STRICT_ERROR_HANDLER;
    }

    private static DocumentBuilder getDocumentBuilder() {
        return documentBuilder.get();
    }

    private static DocumentBuilder getDocumentBuilderAllowingDoctype() {
        return getDocumentBuilderAllowingDoctype(XSS4J_SAFE_ENTITY_RESOLVER);
    }

    private static DocumentBuilder getDocumentBuilderAllowingDoctype( final EntityResolver entityResolver ) {
        final DocumentBuilder builder = documentBuilderAllowingDoctype.get();
        builder.setEntityResolver( entityResolver );
        return builder;
    }

    /**
     * Common settings for DocumentBuilderFactory
     */
    private static DocumentBuilderFactory configureDocumentBuilderFactory( final DocumentBuilderFactory dbf,
                                                                           final boolean disallowDoctype ) {
        dbf.setNamespaceAware(true);

        try {
            dbf.setFeature( XERCES_DISALLOW_DOCTYPE, disallowDoctype );
        } catch ( ParserConfigurationException pce ) {
            logger.log( Level.CONFIG,
                    "XML parser does not support disallow doctype.",
                    ExceptionUtils.getDebugException(pce));
        }

        if ( !ConfigFactory.getBooleanProperty( "com.l7tech.common.xmlDeferNodeExpansion", true ) ) {
            try {
                dbf.setFeature( XERCES_DEFER_NODE_EXPANSION, false );
            } catch ( ParserConfigurationException pce ) {
                logger.log( Level.CONFIG,
                        "XML parser does not support deferred node expansion.",
                        ExceptionUtils.getDebugException(pce));
            }
        }

        if ( ConfigFactory.getBooleanProperty( "com.l7tech.common.xmlSoftSymbolTable", false ) ) {
            try {
                dbf.setAttribute( XERCES_ATTR_SYMBOL_TABLE, new org.apache.xerces.util.SoftReferenceSymbolTable() );
            } catch ( IllegalArgumentException e ) {
                logger.log( Level.CONFIG,
                        "Error configuring XML parser symbol table.",
                        ExceptionUtils.getDebugException( e ) );
            }
        }


        return dbf;
    }

    private static DocumentBuilder configureDocumentBuilder( final DocumentBuilder builder ) {
        builder.setEntityResolver(XSS4J_SAFE_ENTITY_RESOLVER);
        builder.setErrorHandler(QUIET_ERROR_HANDLER);
        return builder;
    }

    /** @return a new, empty DOM document with absolutely nothing in it. */
    public static Document createEmptyDocument() {
        return getDocumentBuilder().newDocument();
    }

    /**
     * Create a new empty XML document with the specified document element.
     *
     * Favor this over using a builder factory to create a new Document yourself as this uses a thread local
     * DocumentBuilder.
     *
     * @param rootElementName the local name of the document element.  Required.
     * @param rootPrefix the namespace prefix to add to the document element, or null to leave it unprefixed.
     * @param rootNs the namespace URI in which to place the document element, or null to leave it in the empty namespace.
     *               If this is specified, a namespace declaration will be added for either the specified rootPrefix or for
     *               the default namespace if rootPrefix is null.
     * @return a new DOM document contianing only a single empty document element.  Never null.
     */
    public static Document createEmptyDocument(@NotNull String rootElementName, @Nullable String rootPrefix, @Nullable String rootNs) {
        if (rootElementName == null)
            throw new NullPointerException("rootElementName");
        Document doc = getDocumentBuilder().newDocument();
        final Element root;
        if (rootNs == null) {
            root = doc.createElement(rootElementName);
        } else {
            if (rootPrefix == null) {
                root = doc.createElementNS(rootNs, rootElementName);
                root.setAttributeNS(XMLNS_NS, "xmlns", rootNs);
            } else {
                root = doc.createElementNS(rootNs, rootPrefix + ":" + rootElementName);
                root.setAttributeNS(XMLNS_NS, "xmlns:" + rootPrefix, rootNs);
            }
        }
        doc.appendChild(root);
        return doc;
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
     * Same as "stringToDocument", parse the given text as XML.
     *
     * <p>This method exists because "parse" is easier to remember than
     * "stringToDocument"</p>
     *
     * @param inputXmlNotAUrl The XML content as a string.
     * @return The parsed document.
     * @throws SAXException If the input is not valid XML
     */
    public static Document parse( String inputXmlNotAUrl ) throws SAXException {
        return stringToDocument( inputXmlNotAUrl );
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

    /**
     * Create an XML document from the given SAX InputSource.
     *
     * <p>This method allows a DOCTYPE in the document and will resolve any
     * entities using the given resolver, but will not permit URL resolution by
     * the parser. This means that any entity not resolved by the given
     * resolver will cause a parse failure.</p>
     *
     * <p>WARNING: When checking that entities are resolved the character and
     * byte streams may be accessed from the InputSource. This method is
     * therefore not compatible with a resolver that opens new streams when
     * these methods are invoked.</p>
     *
     * @param source the input source (required)
     * @param entityResolver The entity resolver to use (required)
     * @return the parsed DOM Document
     * @throws java.io.IOException if an IO error occurs
     * @throws org.xml.sax.SAXException if there is a parsing error
     */
    public static Document parse( final InputSource source,
                                  final EntityResolver entityResolver ) throws IOException, SAXException {
        final DocumentBuilder parser = getDocumentBuilderAllowingDoctype( safeResolver(entityResolver) );
        return parser.parse(source);
    }

    @SuppressWarnings({"deprecation"})
    private static XMLSerializer getFormattedXmlSerializer() {
        XMLSerializer xmlSerializer = new XMLSerializer();
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        return xmlSerializer;
    }

    @SuppressWarnings({"deprecation"})
    private static XMLSerializer getEncodingXmlSerializer() {
        return new XMLSerializer();
    }

    private static Canonicalizer getTransparentXMLSerializer_XSS4J_W3C() {
        return transparentXMLSerializer_XSS4J_W3C.get();
    }

    @SuppressWarnings({"deprecation"})
    private static XMLSerializer getTransparentXMLSerializer( final boolean omitDocumentType ) {
        OutputFormat format = new OutputFormat();
        format.setLineWidth(0);
        format.setIndenting(false);
        format.setPreserveSpace(true);
        format.setOmitXMLDeclaration(true);
        format.setOmitComments(false);
        format.setOmitDocumentType(omitDocumentType);
        format.setPreserveEmptyAttributes(true);
        return new XMLSerializer(format);
    }

    private static Canonicalizer getExclusiveCanonicalizer() {
        return exclusiveCanonicalizer.get();
    }

    /**
     * Serialize the given node to the provided output stream.
     *
     * <p>Serialization will not include a DOCTYPE for Document nodes.</p>
     *
     * @param node The node to serialize (required)
     * @param os The output stream to use (required)
     * @throws IOException If an error occurs during serialization.
     */
    public static void nodeToOutputStream(Node node, OutputStream os) throws IOException {
        if (serializeWithXss4j)
            nodeToOutputStreamWithXss4j(node, os);
        else
            nodeToOutputStreamWithXMLSerializer(node, os, null);
    }

    public static void nodeToOutputStreamWithXss4j(Node node, final OutputStream os) throws IOException {
        Canonicalizer canon = getTransparentXMLSerializer_XSS4J_W3C();
        canon.canonicalize(node, os);
    }

    @SuppressWarnings({"deprecation"})
    static void nodeToOutputStreamWithXMLSerializer( final Node node, final OutputStream os, final XMLSerializer inSerializer) throws IOException {
        XMLSerializer serializer = inSerializer==null ? getTransparentXMLSerializer(true) : inSerializer;
        serializer.setOutputByteStream(os);
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                Element element = (Element) node;
                serializer.serialize(element);
                break;
            case Node.DOCUMENT_NODE:
                Document doc = (Document) node;
                serializer.serialize(doc);
                break;
            case Node.DOCUMENT_FRAGMENT_NODE:
                DocumentFragment fragment = (DocumentFragment) node;
                serializer.serialize(fragment);
                break;
            default:
                throw new IOException("Unsupported DOM Node type: " + node.getNodeType());
        }
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

    @SuppressWarnings({"deprecation"})
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

    @SuppressWarnings({"deprecation"})
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

    /**
     * Serialize the given node to a String.
     *
     * <p>For Document nodes any DOCTYPE will be omitted.</p>
     *
     * @param node The node to serialize (required)
     * @return The String for the node.
     * @throws IOException If and error occurs during serialization.
     */
    public static String nodeToString(Node node) throws IOException {
        return nodeToString( node, false );
    }

    /**
     * Quietly serialize the given node to a String.
     *
     * @param node The node to serialize(required)
     * @return The String for the node.
     * @see #nodeToString
     */
    public static String nodeToStringQuiet(final Node node) {
        try {
            return nodeToString(node);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error serializing XML: " + e.getMessage(), e);
        }
    }

    /**
     * Serialize the given node to a String.
     *
     * @param node The node to serialize (required)
     * @param doctype True to include the DOCTYPE for Document nodes if present.
     * @return The String for the node.
     * @throws IOException If and error occurs during serialization.
     */
    public static String nodeToString( final Node node, final boolean doctype ) throws IOException {
        final PoolByteArrayOutputStream out = new PoolByteArrayOutputStream(1024);
        try {
            if ( doctype ) {
                nodeToOutputStreamWithXMLSerializer( node, out, getTransparentXMLSerializer(false) );
            } else {
                nodeToOutputStream(node, out);
            }
            return out.toString(Charsets.UTF8);
        } finally {
            out.close();
        }
    }

    public static byte[] toByteArray(Node node) throws IOException {
        final PoolByteArrayOutputStream out = new PoolByteArrayOutputStream(1024);
        try {
            nodeToOutputStream(node, out);
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    /**
     * Convert the given node to UTF-8 bytes.
     *
     * <p>This method directly returns a byte[] buffer so the given offset
     * and length must be used.</p>
     *
     * @param node The node to convert.
     * @return The byte array, offset and length
     * @throws IOException If an error occurs
     */
    public static Triple<byte[],Integer,Integer> toRawByteArray( final Node node ) throws IOException {
        final PoolByteArrayOutputStream out = new PoolByteArrayOutputStream(1024);
        try {
            nodeToOutputStream(node, out);
            final int length = out.size();
            final byte[] data = out.detachPooledByteArray();
            return Triple.triple( data, 0, length );
        } finally {
            out.close();
        }
    }

    public static String nodeToFormattedString(Node node) throws IOException {
        final PoolByteArrayOutputStream out = new PoolByteArrayOutputStream(1024);
        try {
            nodeToFormattedOutputStream(node, out);
            return out.toString(Charsets.UTF8);
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
    @SuppressWarnings({"deprecation"})
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
                        newRootNode.setAttributeNS(DomUtils.XMLNS_NS, attrnode.getName(), attrnode.getValue());
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

    /**
     * Get the encoding for an input source
     *
     * <p>This method is likely to be slow so should be tested if used where
     * speed is important.</p>
     *
     * @param data The XML source
     * @return The (detected or default) encoding.
     */
    public static String getEncoding( final byte[] data ) {
        String xmlEncoding;
        try {
            final ByteOrderMarkInputStream byteOrderMarkIn = new ByteOrderMarkInputStream( new ByteArrayInputStream(data) );
            xmlEncoding = getEncoding( new StreamSource( byteOrderMarkIn ) );
            if ( byteOrderMarkIn.getEncoding() != null && "UTF-8".equalsIgnoreCase( xmlEncoding ) ) {
                xmlEncoding = byteOrderMarkIn.getEncoding().name();
            }
        } catch ( IOException e ) {
            // Try without BOM detection
            xmlEncoding = getEncoding( new StreamSource( new ByteArrayInputStream(data) ) );
        }

        return xmlEncoding;
    }

    /**
     * Get the encoding for an input source
     *
     * <p>This method is likely to be slow so should be tested if used where
     * speed is important.</p>
     *
     * @param source The XML source
     * @return The (detected or default) encoding.
     */
    public static String getEncoding( final Source source ) {
        String encoding = null;

        final XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setXMLReporter( SILENT_REPORTER );
        xif.setXMLResolver( FAILING_RESOLVER );
        XMLStreamReader reader = null;
        try {
            reader = xif.createXMLStreamReader( source );
            while( reader.hasNext() ) {
                final int eventType = reader.next();
                if ( eventType == XMLStreamReader.START_DOCUMENT ||
                     eventType == XMLStreamReader.START_ELEMENT ) {
                    encoding = reader.getCharacterEncodingScheme();
                    break;
                }
            }
        } catch ( XMLStreamException e ) {
            if ( reader != null ) {
                encoding = reader.getCharacterEncodingScheme();
            }
        } finally {
            ResourceUtils.closeQuietly( reader );
        }

        if ( encoding == null ) {
            encoding = "UTF-8";
        }

        return encoding;
    }

    /**
     * Does the given XML text have a document type declaration.
     *
     * <p>This method is likely to be slow so should be tested if used where
     * speed is important.</p>
     *
     * <p>If the given source is invalid then the result is not meaningful.</p>
     *
     * @param xml The XML text
     * @return True if there is a document type declaration.
     */
    public static boolean hasDoctype( final String xml ) {
        return hasDoctype( new StreamSource( new StringReader(xml) ) );
    }

    /**
     * Does the given source have a document type declaration.
     *
     * <p>This method is likely to be slow so should be tested if used where
     * speed is important.</p>
     *
     * <p>If the given source is invalid then the result is not meaningful.</p>
     *
     * @param source The XML source
     * @return True if there is a document type declaration.
     */
    public static boolean hasDoctype( final Source source ) {
        boolean doctype = false;

        final XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setXMLReporter( SILENT_REPORTER );
        xif.setXMLResolver( FAILING_RESOLVER );
        XMLStreamReader reader = null;
        try {
            reader = xif.createXMLStreamReader( source );
            while( reader.hasNext() ) {
                final int eventType = reader.next();
                if ( eventType == XMLStreamReader.DTD ) {
                    doctype = true;
                    break;
                } else if ( eventType == XMLStreamReader.START_DOCUMENT ||
                            eventType == XMLStreamReader.START_ELEMENT ) {
                    break;
                }
            }
        } catch ( XMLStreamException e ) {
            if ( ExceptionUtils.getMessage(e).contains( "External entity access forbidden '" )) {
                doctype = true;    
            }
        } finally {
            ResourceUtils.closeQuietly( reader );
        }

        return doctype;
    }

    /**
     * Get the QName of the document element.
     *
     * @param systemId The system identifier for the document (optional)
     * @param text The xml document as text
     * @param resolver The XML resolver to use for entity resolution (optional)
     * @return The QName
     * @throws SAXException if the document could not be parsed
     */
    public static QName getDocumentQName( final String systemId,
                                          final String text,
                                          final XMLResolver resolver ) throws SAXException {
        final StreamSource source = new StreamSource( new StringReader(text) );
        source.setSystemId( systemId );
        return getDocumentQName( source, resolver );
    }

    /**
     * Get the QName of the document element.
     *
     * @param source The xml document source
     * @param resolver The XML resolver to use for entity resolution (optional)
     * @return The QName
     * @throws SAXException if the document could not be parsed
     */
    public static QName getDocumentQName( final Source source,
                                          final XMLResolver resolver ) throws SAXException {
        QName name = null;

        final XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setXMLReporter( SILENT_REPORTER );
        xif.setXMLResolver( resolver == null ? FAILING_RESOLVER : resolver );
        XMLStreamReader reader = null;
        try {
            reader = xif.createXMLStreamReader( source );
            while( reader.hasNext() ) {
                final int eventType = reader.next();
                if ( eventType == XMLStreamReader.START_ELEMENT ) {
                    name = reader.getName();
                    break;
                }
            }
        } catch ( XMLStreamException e ) {
            throw new SAXException(e);
        } finally {
            ResourceUtils.closeQuietly( reader );
        }

        return name;
    }

    /**
     * Get the targetNamespace from a Schema without parsing the entire document.
     *
     * <p>This does not ensure that the schema is valid.</p>
     *
     * @param schema The Schema content
     * @return The targetNamespace (which may be null)
     * @throws BadSchemaException If an error occurs while accessing the target namespace
     */
    public static String getSchemaTNS( final String schema ) throws BadSchemaException {
        final StreamSource source = new StreamSource( new StringReader(schema) );
        final XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setXMLReporter( SILENT_REPORTER );
        xif.setXMLResolver( new XMLResolver(){
            @Override
            public Object resolveEntity( final String publicID,
                                         final String systemID,
                                         final String baseURI,
                                         final String namespace ) throws XMLStreamException {
                return new EmptyInputStream();
            }
        } );

        final QName schemaQName = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema");
        boolean sawSchemaElement = false;
        String targetNamespace = null;
        XMLStreamReader reader = null;
        try {
            reader = xif.createXMLStreamReader( source );
            while( reader.hasNext() ) {
                final int eventType = reader.next();
                if ( eventType == XMLStreamReader.START_ELEMENT ) {
                    if ( schemaQName.equals( reader.getName() ) ) {
                        sawSchemaElement = true;
                        targetNamespace = reader.getAttributeValue( null, "targetNamespace" );
                    } else {
                        break;
                    }
                }
            }
        } catch ( XMLStreamException e ) {
            throw new BadSchemaException(e);
        } finally {
            ResourceUtils.closeQuietly( reader );
        }

        if ( !sawSchemaElement ) {
            throw new BadSchemaException("Not an XML Schema.");   
        }

        return targetNamespace;
    }

    /**
     * Get the targetNamespace from a Schema. This also does some validation on the entire schema.
     *
     * @param schemaUri The uri for the schema (may be null)
     * @param schemaSrc String schema xml (required)
     * @param entityResolver the entity resolver to use (may be null)
     * @return String the target namespace, null if none declared
     * @throws BadSchemaException if the schema is not valid
     */
    @SuppressWarnings({"unchecked", "ForLoopReplaceableByForEach"})
    public static String getSchemaTNS( final String schemaUri,
                                       final String schemaSrc,
                                       final EntityResolver entityResolver ) throws BadSchemaException {
        if (schemaSrc == null) {
            throw new BadSchemaException("no xml");
        }

        try {
            final DocumentBuilderFactory dbfAllowingDoctype =
                configureDocumentBuilderFactory( DocumentBuilderFactory.newInstance(), false );
            dbfAllowingDoctype.setSchema( getSchemaSchema() );
            final DocumentBuilder builder = configureDocumentBuilder( dbfAllowingDoctype.newDocumentBuilder() );
            builder.setErrorHandler( STRICT_ERROR_HANDLER );
            builder.setEntityResolver( safeResolver( entityResolver ) );
            final InputSource schemaInput = new InputSource();
            schemaInput.setSystemId( schemaUri );
            schemaInput.setCharacterStream( new StringReader(schemaSrc) );
            final Document schemaDocument = builder.parse( schemaInput );

            String tns = schemaDocument.getDocumentElement().getAttribute("targetNamespace");

            if (tns.length() == 0) {
                tns = null;
            }
            else {
                // find imported namespaces
                Set<String> importedNamespaces = new HashSet<String>();
                NodeList importElements = schemaDocument.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "import");
                for (int n=0; n<importElements.getLength(); n++) {
                    Element importElement = (Element) importElements.item(n);
                    if (importElement.hasAttribute("namespace")) {
                        importedNamespaces.add(importElement.getAttribute("namespace"));
                    }else{
                        //null will never be added when the namespace attribute is defined on an element
                        importedNamespaces.add(null);
                    }
                }

                if(importedNamespaces.size() != importElements.getLength()){
                    logger.log(Level.INFO, "Schema imports more than a single schema with the same (or empty) namespace.");
                }
                
                //clean up added null if it was added
                if(importedNamespaces.contains(null)){
                    importedNamespaces.remove(null);
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

                        if (namespace != null && !importedNamespaces.contains(namespace)) {
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
        catch (ParserConfigurationException e) {
            throw new BadSchemaException(e);
        }
    }

    /**
     * Get the global (top-level) element names from the given schema.
     *
     * <p>WARNING: This does not process imported, included or redefined
     * schemas, only elements defined in the given schema document will be
     * found.</p>
     *
     * @param schemaElement The schema to process (required)
     * @return The element names (may be empty, never null)
     * @throws BadSchemaException If the schemaSrc is not valid
     */
    public static String[] getSchemaGlobalElements( final Element schemaElement ) throws BadSchemaException {
        List<String> elementNames = new ArrayList<String>();

        final Collection<Element> elementElements =
                XmlUtil.findChildElementsByName( schemaElement, XMLConstants.W3C_XML_SCHEMA_NS_URI, "element" );
        for ( Element elementElement : elementElements ) {
            final String name = elementElement.getAttribute( "name" );
            if ( name.isEmpty() ) {
                throw new BadSchemaException("Global element declaration missing (or empty) required name attribute");
            }
            elementNames.add( name );
        }

        return elementNames.toArray( new String[ elementNames.size() ] );
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

    /** @deprecated For unit tests only */
    @Deprecated
    public static void setSerializeWithXss4j(Boolean b) {
        serializeWithXss4j = b == null ? DEFAULT_SERIALIZE_WITH_XSS4J : b;
    }

    private static EntityResolver safeResolver( final EntityResolver resolver ) {
        if ( resolver == null ) {
            return XSS4J_SAFE_ENTITY_RESOLVER;
        } else if ( resolver instanceof EntityResolver2 ) {
            return new SafeEntityResolver2( (EntityResolver2) resolver );
        } else {
            return new SafeEntityResolver( resolver );
        }
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
                @Override
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

    private static class SafeEntityResolver implements EntityResolver {
        private final EntityResolver entityResolver;

        private SafeEntityResolver( final EntityResolver entityResolver ) {
            this.entityResolver = entityResolver;
        }

        protected final InputSource checkResolved( final InputSource inputSource, final boolean permitNull, final String desc ) throws IOException {
            if ( (!permitNull && inputSource == null) ||
                 (inputSource != null && inputSource.getByteStream() == null && inputSource.getCharacterStream() == null) ) {
                throw new IOException( "Could not resolve '" + desc + "'"  );
            }

            return inputSource;
        }

        @Override
        public final InputSource resolveEntity( final String publicId, final String systemId ) throws SAXException, IOException {
            return checkResolved( entityResolver.resolveEntity( publicId, systemId ), false, systemId );
        }
    }

    private static final class SafeEntityResolver2 extends SafeEntityResolver implements EntityResolver2 {
        private final EntityResolver2 entityResolver;

        private SafeEntityResolver2( final EntityResolver2 entityResolver ) {
            super( entityResolver );
            this.entityResolver = entityResolver;
        }

        @Override
        public final InputSource getExternalSubset( final String name, final String baseURI ) throws SAXException, IOException {
            return checkResolved( entityResolver.getExternalSubset( name, baseURI ), true, name );
        }

        @Override
        public final InputSource resolveEntity( final String name, final String publicId, final String baseURI, final String systemId ) throws SAXException, IOException {
            return checkResolved( entityResolver.resolveEntity( name, publicId, baseURI, systemId ), false, systemId );
        }
    }
}
