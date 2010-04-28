package com.l7tech.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.StringReader;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.Functions;
import com.l7tech.util.CausedIOException;

/**
 * Utility class for working with XML documents with references.
 *
 * <p>This utility can be used to determine the dependencies of a given xml
 * document.</p>
 */
public class DocumentReferenceProcessor {

    //- PUBLIC

    /**
     * Create a DocumentReferenceProcessor with the default processors.
     *
     * <p>Default processors cover WSDL 1.1 and XML Schema resources.</p>
     */
    public DocumentReferenceProcessor() {
        typeRegistry.put( Wsdl11ReferenceTypeProcessor.WSDL11_NAMESPACE, new Wsdl11ReferenceTypeProcessor() );
        typeRegistry.put( SchemaReferenceTypeProcessor.SCHEMA_NAMESPACE, new SchemaReferenceTypeProcessor() );
    }

    /**
     * Create a DocumentReferenceProcessor for XML Schema resources only.
     *
     * @return the XML Schema aware DocumentReferenceProcessor.
     */
    public static DocumentReferenceProcessor schemaProcessor() {
        final DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
        processor.typeRegistry.clear();
        processor.typeRegistry.put( SchemaReferenceTypeProcessor.SCHEMA_NAMESPACE, new SchemaReferenceTypeProcessor() );
        return processor;
    }

    /**
     * Add a ReferenceTypeProcessor for the given namespace.
     *
     * @param namespace The namespace of the ReferenceTypeProcessor to add.
     * @param referenceTypeProcessor The ReferenceTypeProcessor
     * @return The replaced ReferenceTypeProcessor or null
     */
    public ReferenceTypeProcessor addReferenceTypeProcessor( final String namespace,
                                           final ReferenceTypeProcessor referenceTypeProcessor) {
        return typeRegistry.put( namespace, referenceTypeProcessor);
    }

    /**
     * Remove the ReferenceTypeProcessor for the given namespace.
     *
     * @param namespace The namespace of the ReferenceTypeProcessor to remove.
     * @return The removed ReferenceTypeProcessor or null
     */
    public ReferenceTypeProcessor removeReferenceTypeProcessor( final String namespace ) {
        return typeRegistry.remove( namespace );
    }

    /**
     * Retrieve the given document resources and all associated resources.
     *
     * <p>The result map will contain all resources, keyed by URL.</p>
     *
     * @param documentUrl The URL of the main document
     * @param resolver The resolver for resources by URI
     * @return The document resources
     */
    public Map<String,String> processDocument( final String documentUrl,
                                               final ResourceResolver resolver ) throws IOException {
        final Map<String,String> resources = new HashMap<String,String>(); 
        final List<String> referencedUrlQueue = new LinkedList<String>();
        referencedUrlQueue.add( documentUrl );

        while ( !referencedUrlQueue.isEmpty() ) {
            String referenceUrl = referencedUrlQueue.remove(0);
            if ( resources.containsKey(referenceUrl) ) {
                continue; // already processed this resource    
            }

            String documentSource;
            Document document;
            try {
                documentSource = resolver.resolve(referenceUrl);
                InputSource source = new InputSource();
                source.setSystemId(referenceUrl);
                source.setCharacterStream( new StringReader( documentSource ) );
                document = XmlUtil.parse( source, false );
            } catch ( SAXException se ) {
                throw new CausedIOException("Error parsing document '"+referenceUrl+"'.", se);
            }

            final List<String> newReferences = new ArrayList<String>();
            processDocumentReferences( document, new ReferenceCustomizer() {
                @Override
                public String customize(Document document, Node node, String documentUrl, ReferenceInfo referenceInfo) {
                    if ( referenceInfo.getReferenceUrl() != null ) {
                        newReferences.add( referenceInfo.getReferenceUrl() );
                    }
                    return null;
                }
            } );

            try {
                URI baseURI = new URI(referenceUrl);
                for ( String reference : newReferences ) {
                    URI uri = baseURI.resolve(new URI(reference));
                    String uriStr = uri.toString();

                    if (referenceUrl.startsWith("file:////")) { // This is for accessing a Network file.
                        uriStr = uriStr.replaceFirst("file:/", "file:////");
                    }
                    
                    referencedUrlQueue.add(uriStr);
                }
            } catch (URISyntaxException use) {
                throw new CausedIOException("Unable to resolve reference URI for base '"+referenceUrl+"'.", use);
            }

            resources.put( referenceUrl, documentSource );
        }

        return resources;
    }

    /**
     * Process references in the given document.
     *
     * <p>Note that the document must have been parsed in a manner that
     * preserved it source URL.</p>
     *
     * @param document The document to process
     * @param referenceCustomizer The customizer
     */
    public void processDocumentReferences( final Document document,
                                           final ReferenceCustomizer referenceCustomizer ) {
        final String documentUrl = document.getDocumentURI();
        processDocumentReferences(
                documentUrl,
                document.getDocumentElement(),
                referenceCustomizer );
    }

    /**
     * Process references in the given document.
     *
     * <p>Note that the document must have been parsed in a manner that
     * preserved it source URL.</p>
     *
     * @param element The element to process
     * @param referenceCustomizer The customizer
     */
    public void processDocumentReferences( final String url,
                                           final Element element,
                                           final ReferenceCustomizer referenceCustomizer ) {
        final Document document = element.getOwnerDocument();
        XmlUtil.visitNodes( element, new Functions.UnaryVoid<Node>(){
            @Override
            public void call( final Node node ) {
                switch ( node.getNodeType() ) {
                    case Node.ELEMENT_NODE:
                    case Node.ATTRIBUTE_NODE:
                        String namespace = node.getNamespaceURI();
                        if ( namespace != null ) {
                            ReferenceTypeProcessor processor = typeRegistry.get( namespace );
                            if ( processor != null ) {
                                ReferenceInfo referenceInfo = processor.extractReference( node );
                                if ( referenceInfo != null ) {
                                    String newUrl = referenceCustomizer.customize( document, node, url, referenceInfo );
                                    if ( newUrl != null ) {
                                        processor.replaceReference( node, referenceInfo.getReferenceUrl(), newUrl );
                                    }
                                }
                            }
                        }
                        break;
                }
            }
        } );
    }

    public interface ResourceResolver {
        String resolve( String resourceUrl ) throws IOException;
    }

    public static class ReferenceInfo {
        private final String url;
        private final String namespace;

        private ReferenceInfo( final String url ) {
            this(url, null);
        }

        private ReferenceInfo( final String url, final String namespace ) {
            this.url = url;
            this.namespace = namespace;
        }

        /**
         * Get the URL for the reference.
         *
         * @return The reference or null.
         */
        public String getReferenceUrl() {
            return url;
        }

        /**
         * Get the namespace for the reference.
         *
         * @return The namespace or null.
         */
        public String getReferenceNamespace() {
            return namespace;
        }
    }

    public interface ReferenceCustomizer {
        /**
         * Customize the given reference.
         *
         * <p>If null is returned the reference is unchanged.</p>
         *
         * @param document The document being processed
         * @param node The document node that contains the reference
         * @param documentUrl The URL of the document
         * @param referenceInfo The reference details
         * @return The customized reference or null
         */
        String customize( Document document, Node node, String documentUrl, ReferenceInfo referenceInfo );
    }

    public interface ReferenceTypeProcessor {
        /**
         * Extract the reference for the given node.
         *
         * @param node The node to check for references.
         * @return The reference info, or null for no reference.
         */
        ReferenceInfo extractReference( Node node );

        /**
         * Replace the reference URL with an alternative.
         *
         * @param node The node to process.
         * @param referenceUrl The URL to be replaced
         * @param replacementReferenceUrl The replacement URL
         */
        void replaceReference( Node node, String referenceUrl, String replacementReferenceUrl );
    }

    //- PRIVATE

    /**
     * Map of namespaces to processors
     */
    private final Map<String, ReferenceTypeProcessor> typeRegistry = new HashMap<String, ReferenceTypeProcessor>();

    private static abstract class ReferenceTypeProcessorSupport implements ReferenceTypeProcessor {
        private final String namespace;
        private final Set<String> elements;
        private final String attribute;

        private ReferenceTypeProcessorSupport( final String namespace,
                                      final String attribute,
                                      final String... elements ) {
            this.namespace = namespace;
            this.attribute = attribute;
            this.elements = new HashSet<String>( Arrays.asList(elements) );
        }

        @Override
        public ReferenceInfo extractReference( final Node node) {
            ReferenceInfo referenceInfo = null;

            if ( node.getNodeType() == Node.ELEMENT_NODE ) {
                Element referenceElement = (Element) node;
                if ( namespace.equals( referenceElement.getNamespaceURI() ) &&
                     elements.contains( referenceElement.getLocalName() ) ) {
                    if ( attribute==null || referenceElement.hasAttribute( attribute ) ) {
                        referenceInfo = processReference( referenceElement );
                    }
                }
            }

            return referenceInfo;
        }

        @Override
        public void replaceReference( final Node node,
                                      final String referenceUrl,
                                      final String replacementReferenceUrl ) {
            replaceReference( node, attribute, referenceUrl, replacementReferenceUrl );
        }

        protected void replaceReference( final Node node,
                                         final String attribute,
                                         final String referenceUrl,
                                         final String replacementReferenceUrl ) {
            if ( node.getNodeType() == Node.ELEMENT_NODE ) {
                Element referenceElement = (Element) node;
                if ( namespace.equals( referenceElement.getNamespaceURI() ) &&
                     elements.contains( referenceElement.getLocalName() ) ) {
                    referenceElement.setAttribute( attribute, replacementReferenceUrl );
                }
            }
        }

        protected ReferenceInfo processReference( final Node reference ) {
            ReferenceInfo referenceInfo = null;

            if ( reference instanceof Element && attribute != null ) {
                referenceInfo = new ReferenceInfo(((Element)reference).getAttribute( attribute ));    
            }

            return referenceInfo;
        }
    }

    private static class SchemaReferenceTypeProcessor extends ReferenceTypeProcessorSupport {
        private static final String SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
        private static final String ATTR_NAMESPACE = "namespace";
        private static final String ATTR_SCHEMA_LOCATION = "schemaLocation";
        private static final String ELE_IMPORT = "import";
        private static final String ELE_INCLUDE = "include";
        private static final String ELE_REDEFINE = "redefine";

        SchemaReferenceTypeProcessor() {
            super( SCHEMA_NAMESPACE, null, ELE_IMPORT, ELE_INCLUDE, ELE_REDEFINE );
        }

        @Override
        protected ReferenceInfo processReference( final Node reference ) {
            ReferenceInfo referenceInfo = null;

            if ( reference instanceof Element ) {
                final Element referenceElement = (Element) reference;
                final String schemaLocation = referenceElement.hasAttribute( ATTR_SCHEMA_LOCATION ) ?
                        referenceElement.getAttribute( ATTR_SCHEMA_LOCATION ) :
                        null;
                final String namespace = referenceElement.hasAttribute( ATTR_NAMESPACE ) ?
                        referenceElement.getAttribute( ATTR_NAMESPACE ) :
                        null;
                referenceInfo = new ReferenceInfo(schemaLocation, namespace);
            }

            return referenceInfo;
        }

        @Override
        protected void replaceReference( final Node node,
                                         final String attribute,
                                         final String referenceUrl,
                                         final String replacementReferenceUrl ) {
            if ( referenceUrl != null ) {
                super.replaceReference( node, ATTR_SCHEMA_LOCATION, referenceUrl, replacementReferenceUrl );
            }
        }
    }

    private static class Wsdl11ReferenceTypeProcessor extends ReferenceTypeProcessorSupport {
        private static final String WSDL11_NAMESPACE = "http://schemas.xmlsoap.org/wsdl/";
        private static final String ATTR_LOCATION = "location";
        private static final String ELE_IMPORT = "import";

        Wsdl11ReferenceTypeProcessor() {
            super( WSDL11_NAMESPACE, ATTR_LOCATION, ELE_IMPORT );
        }
    }
}
