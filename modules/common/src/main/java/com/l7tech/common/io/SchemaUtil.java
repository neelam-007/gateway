package com.l7tech.common.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.EntityResolver2;

/**
 * XML Schema utility methods.
 *
 * @author Steve Jones
 */
public class SchemaUtil {

    //- PUBLIC

    /**
     * Check if the given document is an XML Schema.
     *
     * @param document The document to check
     * @return true if the document is a schema.
     */
    public static boolean isSchema(final Document document) {
        boolean schema = false;

        if (document != null) {
            schema = isSchema(document.getDocumentElement());
        }

        return schema;
    }

    /**
     * Check if the given element is an XML Schema (document element).
     *
     * @param element The document to check (must be a "schema" element)
     * @return true if the element is a schema.
     */
    public static boolean isSchema(final Element element) {
        boolean schema = false;

        if (element != null) {
            schema = isSchema(new QName(element.getNamespaceURI(), element.getLocalName()));
        }

        return schema;
    }

    /**
     * Check if the given QName is an XML Schema (document element).
     *
     * @param name The qualified name to check (must be a "schema")
     * @return true if the qname is for a schema.
     */
    public static boolean isSchema(final QName name) {
        boolean schema = false;

        if (name != null) {
            schema = XMLSCHEMA_ELEMENTS.contains(name);
        }

        return schema;
    }

    /**
     * Get the dependency URIs for the given XML Schema document.
     *
     * <p>Each dependency is a pair with the left item being the URI as found
     * in the document and the right item being the resolved URI (or the
     * original URI if resolution is not possible)</p>
     *
     * @param document The XML Schema document to process
     * @return The set of dependency URIs
     */
    public static Collection<ResourceReference> getDependencies( final Document document ) {
        final Collection<ResourceReference> references = new ArrayList<ResourceReference>();
        processSchemaDependencies( document, references );
        return references;
    }

    /**
     * Get the dependency URIs for the given XML Schema source.
     *
     * <p>Each dependency is a pair with the left item being the URI as found
     * in the document and the right item being the resolved URI (or the
     * original URI if resolution is not possible)</p>
     *
     * <p>The dependency list will include DTD dependencies as well as XML Schema
     * dependencies.</p>
     *
     * @param inputSource The XML Schema source to process
     * @param entityResolver The entity resolver to use when parsing the source
     * @return The set of dependency URIs
     * @throws IOException if the source cannot be accessed
     * @throws SAXException if the source is not valid XML
     */
    public static Collection<ResourceReference> getDependencies( final InputSource inputSource,
                                                                 final EntityResolver entityResolver ) throws IOException, SAXException {
        final Collection<ResourceReference> references = new ArrayList<ResourceReference>();

        final Document document = XmlUtil.parse( inputSource, buildProcessingResolver(entityResolver, references) );
        processSchemaDependencies( document, references );

        return references;
    }

    //- PRIVATE

    private static final Collection<QName> XMLSCHEMA_ELEMENTS = Collections.unmodifiableList(Arrays.asList(
        new QName("http://www.w3.org/1999/XMLSchema", "schema"),
        new QName("http://www.w3.org/2000/10/XMLSchema", "schema"),
        new QName("http://www.w3.org/2001/XMLSchema", "schema")
    ));

    private static void processSchemaDependencies( final Document document,
                                                   final Collection<ResourceReference> references ) {
        final DocumentReferenceProcessor processor = DocumentReferenceProcessor.schemaProcessor();
        processor.processDocumentReferences( document, new DocumentReferenceProcessor.ReferenceCustomizer(){
            @Override
            public String customize( final Document document,
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                if ( !"import".equals(node.getLocalName()) || !(node instanceof Element)  ) {
                    if ( referenceInfo.getReferenceUrl()!=null ) {
                        references.add( new ResourceReference( documentUrl, referenceInfo.getReferenceUrl(), false, null ) );
                    }
                } else {
                    final String namespace;
                    final Element importElement = (Element) node;
                    final boolean hasNamespaceAttribute = importElement.hasAttributeNS( null, "namespace" );
                    namespace = hasNamespaceAttribute ? importElement.getAttributeNS( null, "namespace" ) : null;
                    references.add( new ResourceReference( documentUrl, referenceInfo.getReferenceUrl(), true, namespace ) );
                }

                return null;
            }
        } );
    }

    private static EntityResolver buildProcessingResolver( final EntityResolver entityResolver,
                                                           final Collection<ResourceReference> references ) {
        if ( entityResolver instanceof EntityResolver2 ) {
            final EntityResolver2 entityResolver2 = (EntityResolver2) entityResolver;
            return new DefaultHandler2(){
                @Override
                public InputSource resolveEntity( final String name, final String publicId, final String baseUri, final String systemId ) throws SAXException, IOException {
                    references.add( new ResourceReference( baseUri, systemId, publicId ) );
                    return entityResolver2.resolveEntity( name, publicId, baseUri, systemId );
                }
            };
        } else {
            return new EntityResolver(){
                @Override
                public InputSource resolveEntity( final String publicId, final String systemId ) throws SAXException, IOException {
                    references.add( new ResourceReference( null, systemId, publicId ) );       
                    return entityResolver.resolveEntity( publicId, systemId );
                }
            };
        }
    }
}
