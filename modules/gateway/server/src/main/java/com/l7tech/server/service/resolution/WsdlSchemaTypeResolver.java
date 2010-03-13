package com.l7tech.server.service.resolution;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.Resolver;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.DocumentReferenceProcessor;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * SchemaTypeResolver that uses WSDL / Schema data.
 */
class WsdlSchemaTypeResolver implements SoapUtil.SchemaTypeResolver {

    //- PUBLIC

    @Override
    public Collection<QName> resolveType( final QName type ) {
        Collection<QName> resolved = resolvedTypesMap.get( type );
        if ( resolved != null ) {
            return resolved;
        }

        final String namespace = type.getNamespaceURI();
        final Collection<Types> typesCollection = wsdl.getTypes();
        Element typeElement = null;
        for ( final Types types : typesCollection ) {
            typeElement = getTypeElement( namespace, type.getLocalPart(), types );
            if ( typeElement != null ) {
                break;
            }
        }

        if ( typeElement != null ) {
            Element seqElement = XmlUtil.findFirstChildElementByName( typeElement, XMLConstants.W3C_XML_SCHEMA_NS_URI, "sequence" );
            if ( seqElement != null ) {
                Element anyElement = XmlUtil.findFirstChildElementByName( seqElement, XMLConstants.W3C_XML_SCHEMA_NS_URI, "any" );
                if ( XmlUtil.findNextElementSibling( anyElement ) == null ) {
                    Collection<QName> names = findAllElementQNames( typesCollection, new HashSet<Element>() );
                    if ( "0".equals( anyElement.getAttribute( "minOccurs" ) ) ) {
                        names.add( null ); // no payload is permitted
                    }
                    resolved = names;
                }
            }
        }

        if ( resolved == null ) {
            resolved = Collections.singleton( new QName( null, type.getLocalPart() ) );
        }

        resolvedTypesMap.put( type, resolved );

        return resolved;
    }

    //- PACKAGE

    WsdlSchemaTypeResolver( final Wsdl wsdl ) {
        this( wsdl, null );
    }

    WsdlSchemaTypeResolver( final Wsdl wsdl,
                                   final Resolver<String,String> schemaResolver ) {
        this.wsdl = wsdl;
        this.schemaResolver = schemaResolver;
    }

    /**
     * This method can be overridden to resolve schemas (instead of a resolver).
     *
     * @return The schemaXml or null.
     */
    String resolveSchema( final String uri ) {
        return schemaResolver == null ? null : schemaResolver.resolve( uri );
    }

    //- PRIVATE

    private static final QName SCHEMA_QNAME = new QName( XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema" );

    private final Wsdl wsdl;
    private final Resolver<String,String> schemaResolver;
    private final Map<QName,Collection<QName>> resolvedTypesMap = new HashMap<QName,Collection<QName>>();
    private final Map<String,Element> schemasBySystemId = new HashMap<String,Element>();

    private Collection<QName> findAllElementQNames( final Collection<Types> typesCollection,
                                                    final HashSet<Element> schemas ) {
        final Collection<QName> elements = new ArrayList<QName>();

        for ( final Types types : typesCollection ) {
            collectElementQNames( elements, types, schemas );
        }

        return elements;
    }

    @SuppressWarnings({ "unchecked" })
    private void collectElementQNames( final Collection<QName> elements,
                                       final Types types,
                                       final HashSet<Element> schemas ) {
        for ( final ExtensibilityElement ee : (Collection<ExtensibilityElement>) types.getExtensibilityElements()  ) {
            if ( SCHEMA_QNAME.equals(ee.getElementType()) ) {
                Element schemaElement = null;
                if ( ee instanceof Schema ) {
                    schemaElement = ((Schema)ee).getElement();
                } else if ( ee instanceof UnknownExtensibilityElement ) {
                    schemaElement = ((UnknownExtensibilityElement)ee).getElement();
                }

                if ( schemaElement != null ) {
                    collectElementQNames( elements, schemaElement, schemas );
                }
            }
        }
    }

    private void collectElementQNames( final Collection<QName> elements,
                                       final Element schemaElement,
                                       final HashSet<Element> schemas ) {
        // Check for recursion
        if ( schemas.contains( schemaElement )) {
            return;
        } else {
            schemas.add( schemaElement );
        }

        String namespace = schemaElement.getAttribute( "targetNamespace" );
        if ( namespace.isEmpty() ) {
            namespace = null;
        }
        for ( Element child : XmlUtil.findChildElementsByName( schemaElement, XMLConstants.W3C_XML_SCHEMA_NS_URI, "element" ) ) {
            elements.add( new QName( namespace, child.getAttribute( "name" )) );
        }

        final Collection<Element> referencedSchemas = findReferencedSchemas( schemaElement );

        for ( final Element referencedSchemaElement : referencedSchemas ) {
            collectElementQNames( elements, referencedSchemaElement, schemas );
        }
    }

    @SuppressWarnings({ "unchecked" })
    private Element getTypeElement( final String namespace,
                                    final String localPart,
                                    final Types types ) {
        Element typeElement = null;

        for ( final ExtensibilityElement ee : (Collection<ExtensibilityElement>) types.getExtensibilityElements()  ) {
            if ( SCHEMA_QNAME.equals(ee.getElementType()) ) {
                Element schemaElement = null;
                if ( ee instanceof Schema ) {
                    schemaElement = ((Schema)ee).getElement();
                } else if ( ee instanceof UnknownExtensibilityElement ) {
                    schemaElement = ((UnknownExtensibilityElement)ee).getElement();
                }

                if ( schemaElement != null ) {
                    typeElement = getTypeElement( namespace, localPart, schemaElement, new HashSet<Element>() );
                    if ( typeElement != null ) {
                        break;
                    }
                }
            }
        }

        return typeElement;
    }

    private Element getTypeElement( final String namespace,
                                    final String localPart,
                                    final Element schemaElement,
                                    final Set<Element> schemas ) {
        Element typeElement = null;

        // Check for recursion
        if ( schemas.contains( schemaElement )) {
            return null;
        } else {
            schemas.add( schemaElement );
        }

        if ( namespace.equals( schemaElement.getAttribute( "targetNamespace" ) ) ) {
            for ( Element child : XmlUtil.findChildElementsByName( schemaElement, XMLConstants.W3C_XML_SCHEMA_NS_URI, "complexType" ) ) {
                if ( localPart.equals( child.getAttribute( "name" ) ) ) {
                    typeElement = child;
                    break;
                }
            }
        } else {
            final Collection<Element> referencedSchemas = findReferencedSchemas( schemaElement );

            for ( Element referencedSchemaElement : referencedSchemas ) {
                typeElement = getTypeElement( namespace, localPart, referencedSchemaElement, schemas );
                if ( typeElement != null ) {
                    break;
                }
            }
        }

        return typeElement;
    }

    private Collection<Element> findReferencedSchemas( final Element schemaElement ) {
        final Collection<Element> referencedSchemaElements = new ArrayList<Element>();

        final DocumentReferenceProcessor drp = new DocumentReferenceProcessor();
        drp.processDocumentReferences(
                schemaElement.getOwnerDocument().getDocumentURI(),
                schemaElement,
                new DocumentReferenceProcessor.ReferenceCustomizer(){
                    @Override
                    public String customize( final Document document, final Node node, final String documentUrl, final String referenceUrl ) {
                        try {
                            final URI base = new URI(documentUrl);
                            final String docUrl = base.resolve(new URI(referenceUrl)).toString();
                            final String schemaXml = resolveSchema( docUrl );
                            if ( schemaXml != null ) {
                                referencedSchemaElements.add( compileAndCache( docUrl, schemaXml ) );
                            }
                        } catch ( IOException ioe ) {
                            // skip
                        } catch ( URISyntaxException e ) {
                            // skip
                        } catch ( SAXException e ) {
                            // skip
                        }
                        return null;
                    }
                } );

        return referencedSchemaElements;
    }

    private Element compileAndCache( final String uri, final String schemaXml ) throws IOException, SAXException {
        Element schemaElement = schemasBySystemId.get( uri );

        if ( schemaElement == null ) {
            final InputSource in = new InputSource( uri );
            in.setCharacterStream( new StringReader( schemaXml ) );
            schemaElement = XmlUtil.parse( in, false ).getDocumentElement();
            schemasBySystemId.put( uri, schemaElement );
        }

        return schemaElement;
    }

}
