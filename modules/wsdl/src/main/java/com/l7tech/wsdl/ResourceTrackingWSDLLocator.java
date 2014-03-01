package com.l7tech.wsdl;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.l7tech.common.io.IOExceptionThrowingReader;
import com.l7tech.common.io.SchemaUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.*;
import com.l7tech.util.Functions.Binary;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.*;

import javax.wsdl.xml.WSDLLocator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.reduce;

/**
 * WSDLLocator that keeps track of imported documents.
 *
 * @author Steve Jones
 */
public class ResourceTrackingWSDLLocator implements WSDLLocator {

    /**
     * Create a tracking locator that wraps the given WSDLLocator.
     *
     * @param actualWSDLLocator The wsdl locator
     * @param includeBase Include the base document in the tracked set
     * @param stripSchemas True to replace any "wsdl:import"'d schema xmls with dummy WSDL documents.
     * @param stripDoctypes Strip the document type
     */
    public ResourceTrackingWSDLLocator(WSDLLocator actualWSDLLocator, boolean includeBase, boolean stripSchemas, boolean stripDoctypes) {
        this.delegate = actualWSDLLocator;
        this.resources = new ArrayList<WSDLResource>();
        this.includeBase = includeBase;
        this.stripSchemas = stripSchemas;
        this.stripDoctypes = stripDoctypes;
    }

    /**
     * Produce output collection from document set.
     *
     * @param baseUri The URI of the base WSDL
     * @param uriToContent map of URIs to resources
     * @param includeBase Include the base document in the tracked set
     * @param stripSchemas True to replace any "wsdl:import"'d schema xmls with dummy WSDL documents.
     * @param stripDoctypes Strip the document type
     * @return The collection of resources (base first if included)
     * @throws IOException If an IO error occurs
     */
    public static Collection<WSDLResource> toWSDLResources(String baseUri,
                                                           Map<String,String> uriToContent,
                                                           boolean includeBase,
                                                           boolean stripSchemas,
                                                           boolean stripDoctypes) throws IOException {
        List<WSDLResource> resources = new ArrayList<WSDLResource>();

        if ( includeBase ) {
            String content = uriToContent.get( baseUri );
            if ( content == null ) {
                throw new IOException("Missing resource '"+baseUri+"'.");
            }

            InputSource source = new InputSource();
            source.setSystemId( baseUri );
            source.setCharacterStream( new StringReader(content) );
            resources.add( buildResource( source, null, stripDoctypes, stripSchemas ) );
        }

        for ( Map.Entry<String,String> resourceEntry : uriToContent.entrySet() ) {
            String uri = resourceEntry.getKey();
            if ( !baseUri.equals(uri) ) {
                String content = resourceEntry.getValue();

                InputSource source = new InputSource();
                source.setSystemId( uri );
                source.setCharacterStream( new StringReader(content) );
                resources.add( buildResource( source, null, stripDoctypes, stripSchemas ) );
            }
        }

        return Collections.unmodifiableCollection(resources);
    }

    /**
     * Process the given resource according to the passed flags.
     *
     * @param resourceUri The URI of the resource
     * @param resourceContent The content of the resource
     * @param stripSchemas True to replace any "wsdl:import"'d schema xmls with dummy WSDL documents.
     * @param stripDoctypes Strip the document type
     * @return The processed resource
     * @throws IOException If an IO error occurs
     */
    public static String processResource( final String resourceUri,
                                          final String resourceContent,
                                          boolean stripSchemas,
                                          boolean stripDoctypes ) throws IOException {
        return processResource( resourceUri, resourceContent, null, stripSchemas, stripDoctypes );
    }

    /**
     * Process the given resource according to the passed flags.
     *
     * @param resourceUri The URI of the resource
     * @param resourceContent The content of the resource
     * @param resolver The entity resolver to use (may be null)
     * @param stripSchemas True to replace any "wsdl:import"'d schema xmls with dummy WSDL documents.
     * @param stripDoctypes Strip the document type
     * @return The processed resource
     * @throws IOException If an IO error occurs
     */
    public static String processResource( final String resourceUri,
                                          final String resourceContent,
                                          final EntityResolver resolver,
                                          boolean stripSchemas,
                                          boolean stripDoctypes ) throws IOException {
        InputSource source = new InputSource();
        source.setSystemId( resourceUri );
        source.setCharacterStream( new StringReader(resourceContent) );
        WSDLResource resource = buildResource( source, resolver, stripDoctypes, stripSchemas );
        return resource.getWsdl();
    }

    /**
     * Get the collection of retrieved documents.
     *
     * <p>The order is arbitrary.</p>
     *
     * @return The collection of WSDLResources
     */
    public Collection<WSDLResource> getWSDLResources() {
        return Collections.unmodifiableCollection(resources);
    }

    /**
     * Get a map of retrieved documents ( uri -> content )
     *
     * @return The unordered Map of resources.
     */
    public Map<String,String> getResourceMap() {
        return reduce( resources, new HashMap<String,String>(), new Binary<Map<String,String>,Map<String,String>,WSDLResource>(){
            @Override
            public Map<String, String> call( final Map<String, String> resourceMap, final WSDLResource wsdlResource ) {
                resourceMap.put( wsdlResource.getUri(), wsdlResource.getWsdl() );
                return resourceMap;
            }
        } );
    }

    /**
     * WSDLLocator
     */
    @Override
    public InputSource getBaseInputSource() {
        InputSource baseInputSource = null;
        if (includeBase) {
            String uri = null;
            try {
                InputSource is = delegate.getBaseInputSource();
                if (is != null) {
                    uri = is.getSystemId();
                    WSDLResource resource = buildResource(is, null, stripDoctypes, stripSchemas);
                    baseInputSource = resource.toInputSource();
                    resources.add(resource);
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Error getting base input source '"+uri+"'.", ioe);
                InputSource inputSource = new InputSource();
                inputSource.setSystemId( uri );
                inputSource.setCharacterStream( new IOExceptionThrowingReader(ioe, false) );
                baseInputSource = inputSource;
            }
        } else {
            baseInputSource = delegate.getBaseInputSource();
        }
        return baseInputSource;
    }

    /**
     * WSDLLocator
     */
    @Override
    public String getBaseURI() {
        return delegate.getBaseURI();
    }

    /**
     * WSDLLocator
     */
    @Override
    public InputSource getImportInputSource(final String parentLocation, final String importLocation) {
        InputSource inputSource = null;

        String uri = null;
        try {
            InputSource is = delegate.getImportInputSource(parentLocation, importLocation);
            if (is != null) {
                uri = is.getSystemId();
                WSDLResource resource = buildResource(is, null, stripDoctypes, stripSchemas);
                inputSource = resource.toInputSource();
                resources.add(resource);
            }
        } catch (IOException ioe) {
            //should not log the stack trace here. SSG-7734
            logger.log(Level.WARNING, "Error getting import input source '"+uri+"'. Message: " + ioe.getMessage());
            InputSource errorSource = new InputSource();
            errorSource.setSystemId( uri );
            errorSource.setCharacterStream( new IOExceptionThrowingReader(ioe, false) );
            inputSource = errorSource;
        }

        return inputSource;
    }

    /**
     * WSDLLocator
     */
    @Override
    public String getLatestImportURI() {
        return delegate.getLatestImportURI();
    }

    @Override
    public void close() {
        delegate.close();
    }

    /**
     * WSDL document data and metadata
     */
    public static class WSDLResource {
        private final String uri;
        private final String contentType;
        private final String wsdl;

        public WSDLResource(final String uri, final String contentType, final String wsdl) {
            this.uri = uri;
            this.contentType = contentType;
            this.wsdl = wsdl;            
        }

        public String getContentType() {
            return contentType;
        }

        public String getUri() {
            return uri;
        }

        public String getWsdl() {
            return wsdl;
        }

        private InputSource toInputSource() {
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(wsdl));
            is.setSystemId(uri);
            return is;
        }
    }

    //- PRIVATE

    private static final int MAX_DOCUMENT_SIZE = ConfigFactory.getIntProperty( "com.l7tech.wsdl.defaultMaxSize", 1024 * 1024 * 10 ); //10MB
    private static final String NOOP_WSDL = "<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"/>";

    private static final Logger logger = Logger.getLogger(ResourceTrackingWSDLLocator.class.getName());

    private final WSDLLocator delegate;
    private final Collection<WSDLResource> resources;
    private final boolean includeBase;
    private final boolean stripSchemas;
    private final boolean stripDoctypes;

    /**
     * Build a WSDL resource for the given input
     */
    private static WSDLResource buildResource( final InputSource inputSource,
                                               final EntityResolver resolver,
                                               final boolean stripDoctypes,
                                               final boolean stripSchemas) throws IOException {
        String uri = inputSource.getSystemId();
        String wsdl = null;

        // check byte stream and url
        InputStream in = null;
        try {
            in = inputSource.getByteStream();
            if (in != null) {
                wsdl = loadFromStream(inputSource, in);
            }
        } finally {
            ResourceUtils.closeQuietly(in);
        }

        // check reader
        if (wsdl == null) {
            Reader reader = null;
            try {
                reader = inputSource.getCharacterStream();
                if (reader != null) {
                    wsdl = loadFromReader(inputSource, reader);
                }
            } finally {
                ResourceUtils.closeQuietly(reader);
            }
        }

        // finally try
        if (wsdl == null && uri != null) {
            InputStream urlIn = null;
            try {
                URL url = new URL(uri);
                URLConnection connection = url.openConnection();
                urlIn = connection.getInputStream();
                wsdl = loadFromUrl(inputSource, connection, urlIn);
            } finally {
                ResourceUtils.closeQuietly(urlIn);
            }
        }

        if (wsdl == null) {
            throw new IOException("No data");
        }

        if (stripDoctypes || stripSchemas) {
            try {
                DocumentBuilder db = getDocumentBuilder();
                db.setEntityResolver( resolver );
                db.setErrorHandler( new ErrorHandler(){
                    @Override
                    public void warning( SAXParseException exception) {}
                    @Override
                    public void error(SAXParseException exception) {}
                    @Override
                    public void fatalError(SAXParseException exception) throws SAXException {
                        throw exception;
                    }
                } );
                InputSource source = new InputSource();
                source.setSystemId(uri);
                source.setCharacterStream(new StringReader(wsdl));
                Document document = db.parse(source);
                if (stripSchemas && SchemaUtil.isSchema(document)) {
                    wsdl = NOOP_WSDL;
                } else if (stripDoctypes && document.getDoctype() != null) {
                    wsdl = stripExternalEntities(document);
                }
            }
            catch(SAXException se) {
                throw new IOException("Error parsing XML with URI " + uri + ": " + ExceptionUtils.getMessage(se), se);
            }
        }

        return new WSDLResource(uri, "text/xml", wsdl);
    }

    /**
     * Get a DOM level 3 builder
     */
    private static DocumentBuilder getDocumentBuilder() {
        try {
            DocumentBuilderFactory dbf = new com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl();
            dbf.setValidating(false);
            dbf.setNamespaceAware(true);
            dbf.setExpandEntityReferences(true);
            return dbf.newDocumentBuilder();
        }
        catch(ParserConfigurationException pce) {
            throw new IllegalStateException("Error with XML parser configuration.", pce);
        }
    }

    /**
     * Load a WSDL string from the given URL
     */
    private static String loadFromUrl(InputSource inputSource, URLConnection connection, InputStream in) throws IOException {
        Charset encoding = (inputSource.getEncoding() != null && Charset.isSupported(inputSource.getEncoding())) ? Charset.forName(inputSource.getEncoding()) : null;
        in = new ByteOrderMarkInputStream(in);
        if (encoding == null) {
            encoding = ((ByteOrderMarkInputStream)in).getEncoding();
            if (encoding == null) {
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConn = (HttpURLConnection) connection;
                    encoding = ContentTypeHeader.parseValue(httpConn.getContentType()).getEncoding();
                } else {
                    encoding = Charset.defaultCharset();
                }
            } else if (ByteOrderMarkInputStream.UTF8.equals(encoding)) {
                // If BOM says UTF-8 it could be iso-8859-1 so check other info if available
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConn = (HttpURLConnection) connection;
                    encoding = ContentTypeHeader.parseValue(httpConn.getContentType()).getEncoding();
                } else {
                    encoding = Charsets.UTF8;
                }
            }
        }
        byte[] data = IOUtils.slurpStream(new ByteLimitInputStream(in, 1024, MAX_DOCUMENT_SIZE));
        return new String(data, encoding);
    }

    /**
     * Load a WSDL string from the given reader
     */
    private static String loadFromReader(InputSource inputSource, Reader reader) throws IOException {
        String uri = inputSource.getSystemId();
        StringBuffer buffer = new StringBuffer(4096);
        char[] charbuffer = new char[4096];

        int read;
        int total = 0;
        while((read=reader.read(charbuffer)) >= 0) {
            total += read;
            if (total > MAX_DOCUMENT_SIZE) {
                throw new IOException("Document too large '"+uri+"'.");
            }
            buffer.append(charbuffer, 0, read);
        }
        return buffer.toString();
    }

    /**
     * Load a WSDL string from the given input stream
     */
    private static String loadFromStream(InputSource inputSource, InputStream in) throws IOException {
        in = new ByteOrderMarkInputStream(in);
        Charset encoding = inputSource.getEncoding()!=null && Charset.isSupported(inputSource.getEncoding()) ?
                Charset.forName(inputSource.getEncoding()) : null;
        if (encoding == null) {
            encoding = ((ByteOrderMarkInputStream)in).getEncoding();
            if (encoding == null) {
                encoding = Charset.defaultCharset();
            }
        }
        byte[] data = IOUtils.slurpStream(new ByteLimitInputStream(in, 1024, MAX_DOCUMENT_SIZE));
        return new String(data, encoding);
    }

    /**
     * Convert the given Document to a String, add any DTD defined attributes
     * to the DOM and expand any entities.
     *
     * Probably should have just used a canonicalizer ...
     */
    private static String stripExternalEntities(Document document) throws SAXException, IOException {
        DOMImplementationLS dils = (DOMImplementationLS) document.getImplementation();
        LSSerializer lsser = dils.createLSSerializer();
        DOMConfiguration lsserDOMConfig = lsser.getDomConfig();
        lsserDOMConfig.setParameter("discard-default-content", Boolean.FALSE);
        lsserDOMConfig.setParameter("xml-declaration", Boolean.FALSE);
        return lsser.writeToString(document.getDocumentElement());
    }
}
