package com.l7tech.policy.exporter;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.GlobalResourceInfo;
import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.ResourceUtils;
import com.l7tech.wsdl.WsdlEntityResolver;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reference to a Global Resource (Resource Entry)
 */
public class GlobalResourceReference extends ExternalReference {

    //- PUBLIC

    public GlobalResourceReference( final ExternalReferenceFinder finder,
                                    final EntityResolver entityResolver,
                                    final String systemIdentifier,
                                    final ResourceType type,
                                    final String resourceKey1,
                                    final String resourceKey2,
                                    final String resourceKey3 ) {
        super(finder);
        if ( type == null ) throw new IllegalArgumentException("type is required");
        this.entityResolver = entityResolver;
        this.systemIdentifier = systemIdentifier;
        this.type = type;
        this.resourceKey1 = resourceKey1;
        this.resourceKey2 = resourceKey2;
        this.resourceKey3 = resourceKey3;
    }

    public static GlobalResourceReference parseFromElement( final ExternalReferenceFinder finder,
                                                            final EntityResolver entityResolver,
                                                            final Element el ) throws InvalidDocumentFormatException {
        if ( !el.getNodeName().equals(TOPEL_NAME) ) {
            throw new InvalidDocumentFormatException("Expecting element of name " + TOPEL_NAME);
        }

        return new GlobalResourceReference(
                finder,
                entityResolver,
                getParamFromEl( el, SID_EL_NAME ),
                toResourceType( getRequiredParamFromEl( el, TYPE_EL_NAME ) ),
                getParamFromEl( el, RESKEY1_EL_NAME ),
                getParamFromEl( el, RESKEY2_EL_NAME ),
                getParamFromEl( el, RESKEY3_EL_NAME )
                    );
    }

    public String getSystemIdentifier() {
        return systemIdentifier;
    }

    public ResourceType getType() {
        return type;
    }

    public String getResourceKey1() {
        return resourceKey1;
    }

    public String getResourceKey2() {
        return resourceKey2;
    }

    public String getResourceKey3() {
        return resourceKey3;
    }

    @Override
    public boolean setLocalizeReplace( final String identifier ) {
        boolean replaced = false;
        if ( type == ResourceType.XML_SCHEMA ) {
            localizeType = LocalizeAction.REPLACE;
            localSystemIdentifier = identifier;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof GlobalResourceReference) ) return false;

        final GlobalResourceReference that = (GlobalResourceReference) o;

        if ( resourceKey1 != null ? !resourceKey1.equals( that.resourceKey1 ) : that.resourceKey1 != null )
            return false;
        if ( resourceKey2 != null ? !resourceKey2.equals( that.resourceKey2 ) : that.resourceKey2 != null )
            return false;
        if ( resourceKey3 != null ? !resourceKey3.equals( that.resourceKey3 ) : that.resourceKey3 != null )
            return false;
        if ( systemIdentifier != null ? !systemIdentifier.equals( that.systemIdentifier ) : that.systemIdentifier != null )
            return false;
        if ( type != that.type ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = systemIdentifier != null ? systemIdentifier.hashCode() : 0;
        result = 31 * result + type.hashCode();
        result = 31 * result + (resourceKey1 != null ? resourceKey1.hashCode() : 0);
        result = 31 * result + (resourceKey2 != null ? resourceKey2.hashCode() : 0);
        result = 31 * result + (resourceKey3 != null ? resourceKey3.hashCode() : 0);
        return result;
    }

    //- PROTECTED

    @Override
    protected void serializeToRefElement( final Element referencesParentElement ) {
        final Element refEl = referencesParentElement.getOwnerDocument().createElementNS(null, TOPEL_NAME);
        setTypeAttribute( refEl );
        addParamEl( refEl, SID_EL_NAME, systemIdentifier, false );
        addParamEl( refEl, TYPE_EL_NAME, type.name(), true );
        addParamEl( refEl, RESKEY1_EL_NAME, resourceKey1, false );
        addParamEl( refEl, RESKEY2_EL_NAME, resourceKey2, false );
        addParamEl( refEl, RESKEY3_EL_NAME, resourceKey3, false );
        referencesParentElement.appendChild(refEl);
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        // check that the resource entry is present on the target system
        try {
            if (systemIdentifier == null || getFinder().findResourceEntryByUriAndType( systemIdentifier, type )==null) {
                return resourceKey1 != null && !getFinder().findResourceEntryByKeyAndType( resourceKey1, type ).isEmpty();
            }
        } catch (RuntimeException e) {
            logger.log( Level.SEVERE, "Error using resource entry admin layer", e);
            throw e;
        } catch ( FindException e) {
            logger.log( Level.SEVERE, "Error using resource entry admin layer", e);
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    protected boolean localizeAssertion( final @Nullable Assertion assertionToLocalize ) {
        if ( assertionToLocalize instanceof SchemaValidation ) {
            final SchemaValidation schemaVal = (SchemaValidation) assertionToLocalize;
            final AssertionResourceInfo schemaResource = schemaVal.getResourceInfo();

            if ( localizeType == LocalizeAction.DELETE ) {
                // check if this assertion indeed refers to this external schema
                if ( schemaResource instanceof StaticResourceInfo ) {
                    if ( systemIdentifier != null || resourceKey1 != null ) {
                        final Collection<GlobalResourceReference> schemaReferences =
                                buildResourceEntryReferences( null, entityResolver, schemaVal );

                        for ( final GlobalResourceReference schemaReference : schemaReferences ) {
                            if ( schemaReference.getType() != type ) continue;
                            if ( schemaReference.getSystemIdentifier()!=null &&
                                    schemaReference.getSystemIdentifier().equals(systemIdentifier) ) return false;
                            if ( schemaReference.getResourceKey1()!=null &&
                                    schemaReference.getResourceKey1().equals(resourceKey1) ) return false;
                        }
                    }
                } else if ( schemaResource instanceof GlobalResourceInfo && systemIdentifier != null && type==ResourceType.XML_SCHEMA ) {
                    String globalSchemaName = ((GlobalResourceInfo) schemaResource).getId();
                    if ( globalSchemaName.equals(systemIdentifier) )
                        return false;
                }
            } else if ( localizeType == LocalizeAction.REPLACE && systemIdentifier != null && localSystemIdentifier != null ) {
                if ( schemaResource instanceof GlobalResourceInfo && type==ResourceType.XML_SCHEMA ) {
                    final GlobalResourceInfo resourceInfo = (GlobalResourceInfo) schemaResource;
                    if (resourceInfo.getId()!=null && resourceInfo.getId().equals(systemIdentifier)) {
                        resourceInfo.setId( localSystemIdentifier );
                    }
                } else if ( schemaResource instanceof StaticResourceInfo ) {
                    final StaticResourceInfo resourceInfo = (StaticResourceInfo) schemaResource;
                    try {
                        final Document schema = XmlUtil.parse(asInputSource(resourceInfo), entityResolver);
                        final boolean[] updated = new boolean[]{false};
                        final DocumentReferenceProcessor processor = DocumentReferenceProcessor.schemaProcessor();
                        processor.processDocumentReferences( schema, new DocumentReferenceProcessor.ReferenceCustomizer(){
                            @Override
                            public String customize( final Document document,
                                                     final Node node,
                                                     final String documentUrl,
                                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                                String uri = null;
                                if ( systemIdentifier.equals( referenceInfo.getReferenceUrl() ) ||
                                     systemIdentifier.equals( resolve( documentUrl, referenceInfo.getReferenceUrl() ) )) {
                                    updated[0] = true;
                                    uri = relativize(documentUrl, localSystemIdentifier);
                                }
                                return uri;
                            }
                        } );
                        if ( updated[0] ) {
                            resourceInfo.setDocument( XmlUtil.nodeToString(schema) );
                        }
                    } catch (SAXException e) {
                        logger.log(Level.WARNING, "Cannot parse schema: " + ExceptionUtils.getMessage( e ));
                    } catch ( IOException e) {
                        logger.log(Level.WARNING, "Cannot update schema: " + ExceptionUtils.getMessage( e ));
                    }
                }
            }
        }

        return true;
    }

    //- PACKAGE

    static Collection<GlobalResourceReference> buildResourceEntryReferences( final @Nullable ExternalReferenceFinder finder,
                                                                             final EntityResolver entityResolver,
                                                                             final UsesResourceInfo usesResourceInfo ) {
        final Collection<GlobalResourceReference> references = new ArrayList<GlobalResourceReference>();

        final ResourceType mainType = deriveType( usesResourceInfo );
        final AssertionResourceInfo resourceInfo = usesResourceInfo.getResourceInfo();

        if ( mainType != null ) {
            if ( resourceInfo instanceof StaticResourceInfo ) {
                if ( mainType == ResourceType.XML_SCHEMA ) {
                    try {
                        final Document schema = XmlUtil.parse( // DTD references constructed while parsing
                                asInputSource(((StaticResourceInfo) resourceInfo)),
                                new ReferenceBuildingEntityResolver( finder, entityResolver, references) );
                        final ArrayList<ListedImport> listOfImports = listImports(schema);
                        for ( final ListedImport unresolvedImport : listOfImports ) {
                            // create old format schema reference so previous releases can read the export 
                            references.add( new ExternalSchemaReference( finder, entityResolver, unresolvedImport.name, unresolvedImport.tns ) );
                        }
                    } catch ( SAXException e) {
                        logger.log(Level.WARNING, "Error parsing schema: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        // fallthrough since it's possible this assertion is just badly configured in which case we wont care
                        // about external references
                    } catch ( IOException e ) {
                        logger.log(Level.WARNING, "Error parsing schema: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        // fallthrough
                    }
                }
            } else if ( resourceInfo instanceof GlobalResourceInfo ) {
                final String systemIdentifier = ((GlobalResourceInfo) resourceInfo).getId();
                if ( mainType == ResourceType.XML_SCHEMA ) { // create old format schema reference so previous releases can read the export
                    references.add( new ExternalSchemaReference( finder, entityResolver, systemIdentifier, null ) );
                } else {
                    references.add( new GlobalResourceReference( finder, entityResolver, systemIdentifier, mainType, null, null, null) );
                }
            }
        }

        return references;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( GlobalResourceReference.class.getName());

    private final EntityResolver entityResolver;
    private final String systemIdentifier;
    private final ResourceType type;
    private final String resourceKey1;
    private final String resourceKey2;
    private final String resourceKey3;
    private String localSystemIdentifier;
    private LocalizeAction localizeType = LocalizeAction.IGNORE;
    private static final String TOPEL_NAME = "GlobalResourceReference";
    private static final String SID_EL_NAME = "SystemIdentifier";
    private static final String TYPE_EL_NAME = "Type";
    private static final String RESKEY1_EL_NAME = "ResourceKey1";
    private static final String RESKEY2_EL_NAME = "ResourceKey2";
    private static final String RESKEY3_EL_NAME = "ResourceKey3";

    private static ResourceType toResourceType( final String value ) throws InvalidDocumentFormatException {
        try {
            return ResourceType.valueOf( value.trim() );
        } catch ( final IllegalArgumentException e ) {
            throw new InvalidDocumentFormatException( "Unknown type for " + TOPEL_NAME + "/" + TYPE_EL_NAME + ": " + value );
        }
    }

    private static InputSource asInputSource( final StaticResourceInfo staticResourceInfo ) {
        final InputSource inputSource = new InputSource();
        if ( staticResourceInfo.getOriginalUrl() != null ) {
            inputSource.setSystemId( staticResourceInfo.getOriginalUrl() );
        }
        inputSource.setCharacterStream( new StringReader( staticResourceInfo.getDocument() ) );
        return inputSource;
    }

    private static ResourceType deriveType( final Object source ) {
        final ResourceType type;

        if ( source instanceof SchemaValidation ) {
            type = ResourceType.XML_SCHEMA;
        } else {
            type = null;
        }

        return type;
    }

    private static String resolve( final String baseUri,
                                   final String uri ) {
        String resolvedUri = uri;

        if ( baseUri != null ) {
            try {
                final URI base = new URI( baseUri );
                resolvedUri = base.resolve( uri ).toString();
            } catch ( URISyntaxException e ) {
                logger.log(Level.WARNING, "Error resolving URL for base '"+baseUri+"', url '"+uri+"' :" + ExceptionUtils.getMessage( e ));
            } catch ( IllegalArgumentException e ) {
                logger.log(Level.WARNING, "Error resolving URL for base '"+baseUri+"', url '"+uri+"' :" + ExceptionUtils.getMessage( e ));
            }
        }

        return resolvedUri;
    }

    private static String relativize( final String baseUri,
                                      final String uri ) {
        String relativeUri = uri;

        if ( baseUri != null ) {
            try {
                final URI base = new URI( baseUri );
                final URI refUri = new URI( uri );
                relativeUri = ResourceUtils.relativizeUri( base, refUri ).toString();
            } catch ( URISyntaxException e ) {
                logger.log(Level.WARNING, "Error relativizing URL for base '"+baseUri+"', url '"+uri+"' :" + ExceptionUtils.getMessage( e ));
            }
        }

        return relativeUri;
    }

    /**
     * The document should have been parsed in a way that preserves the source URL.
     *
     * @return An array list of ListedImport objects
     */
    private static ArrayList<ListedImport> listImports( final Document schemaDoc ) {
        final ArrayList<ListedImport> output = new ArrayList<ListedImport>();
        final List<Element> dependencyElements = new ArrayList<Element>();
        final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
        schemaReferenceProcessor.processDocumentReferences( schemaDoc, new DocumentReferenceProcessor.ReferenceCustomizer(){
            @Override
            public String customize( final Document document,
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                if ( node instanceof Element ) dependencyElements.add( (Element)node );
                return null;
            }
        } );

        for ( final Element dependencyElement : dependencyElements ) {
            final String schemaNamespace = "import".equals(dependencyElement.getLocalName())?
                    dependencyElement.getAttribute("namespace") : // we want empty, not null for a reference to a schema with no TNS
                    null;
            final String schemaUrl = dependencyElement.hasAttribute("schemaLocation") ?
                    dependencyElement.getAttribute("schemaLocation") :
                    null;

            String resolvedSchemaUrl;
            if ( schemaUrl != null ) {
                resolvedSchemaUrl = resolve( schemaDoc.getDocumentURI(), schemaUrl );
            } else {
                resolvedSchemaUrl = null;
            }

            if ( resolvedSchemaUrl != null || schemaNamespace != null ) {
                output.add(new ListedImport(resolvedSchemaUrl, schemaNamespace));
            }
        }

        return output;
    }

    private static class ReferenceBuildingEntityResolver implements EntityResolver {
        private final ExternalReferenceFinder finder;
        private final EntityResolver entityResolver;
        private final EntityResolver backupEntityResolver = new WsdlEntityResolver();
        private final Collection<GlobalResourceReference> references;

        private ReferenceBuildingEntityResolver( final ExternalReferenceFinder finder,
                                                 final EntityResolver entityResolver,
                                                 final Collection<GlobalResourceReference> references ) {
            this.finder = finder;
            this.entityResolver = entityResolver;
            this.references = references;
        }

        @Override
        public InputSource resolveEntity( final String publicId,
                                          final String systemId ) throws SAXException, IOException {
            references.add( new GlobalResourceReference( finder, entityResolver, systemId, ResourceType.DTD, publicId, null, null ) );
            InputSource resolved = entityResolver.resolveEntity( publicId, systemId );
            if ( resolved == null ) {
                // fall back to well known default resources
                resolved = backupEntityResolver.resolveEntity( publicId, systemId );
            }
            return resolved;
        }
    }

    private static class ListedImport {
        ListedImport(final String name, final String tns) {
            this.name = name;
            this.tns = tns;
        }
        public String name;
        public String tns;
    }
}