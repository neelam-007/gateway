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

            Document document;
            try {
                InputSource source = new InputSource();
                source.setSystemId(referenceUrl);
                source.setCharacterStream( new StringReader( resolver.resolve(referenceUrl) ) );
                document = XmlUtil.parse( source, false );
            } catch ( SAXException se ) {
                throw new CausedIOException("Error parsing document '"+referenceUrl+"'.", se);
            }

            final List<String> newReferences = new ArrayList<String>();
            processDocumentReferences( document, new ReferenceCustomizer() {
                public String customize(Document document, Node node, String documentUrl, String referenceUrl) {
                    newReferences.add( referenceUrl );
                    return null;
                }
            } );

            try {
                URI baseURI = new URI(referenceUrl);
                for ( String reference : newReferences ) {
                    referencedUrlQueue.add( baseURI.resolve(new URI(reference)).toString() );
                }
            } catch (URISyntaxException use) {
                throw new CausedIOException("Unable to resolve reference URI for base '"+referenceUrl+"'.", use);
            }

            resources.put( referenceUrl, XmlUtil.nodeToString(document) );
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
        XmlUtil.visitNodes( document.getDocumentElement(), new Functions.UnaryVoid<Node>(){
            public void call( final Node node ) {
                switch ( node.getNodeType() ) {
                    case Node.ELEMENT_NODE:
                    case Node.ATTRIBUTE_NODE:
                        String namespace = node.getNamespaceURI();
                        if ( namespace != null ) {
                            ReferenceTypeProcessor processor = typeRegistry.get( namespace );
                            if ( processor != null ) {
                                String referenceUrl = processor.extractReference( node );
                                if ( referenceUrl != null ) {
                                    String newUrl = referenceCustomizer.customize( document, node, documentUrl, referenceUrl );
                                    if ( newUrl != null ) {
                                        processor.replaceReference( node, referenceUrl, newUrl );
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

    public interface ReferenceCustomizer {
        /**
         * Customize the given reference.
         *
         * <p>If null is returned the reference is unchanged.</p>
         *
         * @param document The document being processed
         * @param node The document node that contains the reference
         * @param documentUrl The URL of the document
         * @param referenceUrl The URL of the reference
         * @return The customized reference or null
         */
        String customize( Document document, Node node, String documentUrl, String referenceUrl );
    }

    public interface ReferenceTypeProcessor {
        String extractReference( Node node );
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

        public String extractReference( final Node node) {
            String referenceUrl = null;

            if ( node.getNodeType() == Node.ELEMENT_NODE ) {
                Element referenceElement = (Element) node;
                if ( namespace.equals( referenceElement.getNamespaceURI() ) &&
                     elements.contains( referenceElement.getLocalName() ) ) {
                    if ( referenceElement.hasAttribute( attribute ) ) {
                        referenceUrl = referenceElement.getAttribute( attribute );
                    }
                }
            }

            return referenceUrl;
        }

        public void replaceReference( final Node node,
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
    }

    private static class SchemaReferenceTypeProcessor extends ReferenceTypeProcessorSupport {
        private static final String SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
        private static final String ATTR_SCHEMA_LOCATION = "schemaLocation";
        private static final String ELE_IMPORT = "import";
        private static final String ELE_INCLUDE = "include";
        private static final String ELE_REDEFINE = "redefine";

        SchemaReferenceTypeProcessor() {
            super( SCHEMA_NAMESPACE, ATTR_SCHEMA_LOCATION, ELE_IMPORT, ELE_INCLUDE, ELE_REDEFINE );
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
