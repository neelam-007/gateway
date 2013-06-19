package com.l7tech.console.panels;

import com.l7tech.common.io.ResourceDocument;
import com.l7tech.common.io.ResourceDocumentResolver;
import com.l7tech.common.io.ResourceDocumentResolverSupport;
import com.l7tech.common.io.URIResourceDocument;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import static com.l7tech.console.panels.GlobalResourceImportContext.ImportChoice.*;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.TextUtils;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Context for global resource imports.
 */
class GlobalResourceImportContext {

    //- PUBLIC

    public static class ResourceInputSource {
        private final URI uri;
        private final File file;
        private final String content;
        private final ResourceType type;
        private final ResourceDocumentResolver resolver;

        private ResourceInputSource( final File file,
                                     final URI uri,
                                     final String content,
                                     final ResourceType type,
                                     final ResourceDocumentResolver resolver ) {
            if ( file == null && uri == null) throw new IllegalArgumentException("file or url is required.");
            if ( content == null && resolver == null) throw new IllegalArgumentException("content or resolver is required.");
            this.file = file;
            this.content = content;
            this.type = type;
            this.resolver = resolver;
            this.uri = file!=null ? file.toURI() : uri;
        }

        public URI getUri() {
            return uri;
        }

        public String getUriString() {
            return uri.toString();
        }

        public ResourceType getType() {
            return type;
        }

        /**
         * Get the length if known (else null)
         *
         * @return The length
         */
        public Long getLength() {
            Long length = null;

            if ( file != null ) {
                length = file.length();
            }

            return length;
        }

        ResourceDocument asResourceDocument() throws IOException {
            ResourceDocument document;

            if ( content != null ) {
                document = new URIResourceDocument( uri, content, resolver );
            } else {
                document = resolver.resolveByUri( uri.toString() );
                if ( document == null ) {
                    throw new IOException("Resource not found for URI '"+uri+"'");                    
                }
            }

            return document;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final ResourceInputSource that = (ResourceInputSource) o;

            if ( file != null ? !file.equals( that.file ) : that.file != null ) return false;
            if ( uri != null ? !uri.equals( that.uri ) : that.uri != null ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = file != null ? file.hashCode() : 0;
            result = 31 * result + (uri != null ? uri.hashCode() : 0);
            return result;
        }
    }
    
    public static final class ResourceHolder {
        private ResourceDocument resourceDocument;
        private final ResourceType resourceType;
        private final String detail;
        private Throwable error;
        private final Set<Pair<String,String>> dependencies = new HashSet<Pair<String,String>>();
        // SecurityZone to set on the resource
        private final SecurityZone securityZone;

        private ResourceHolder( final ResourceDocument resourceDocument,
                                final ResourceType resourceType,
                                final String detail,
                                final Throwable error,
                                @Nullable final SecurityZone securityZone) {
            if ( resourceDocument == null ) throw new IllegalArgumentException( "resourceDocument is required" );
            this.resourceDocument = resourceDocument;
            this.resourceType = resourceType;
            this.detail = detail;
            this.error = error;
            this.securityZone = securityZone;
        }

        public boolean isError() {
            return error != null;
        }

        public Throwable getError() {
            return error;
        }

        public void setError( final Throwable error ){
            this.error = error;
        }

        /**
         * Should this resource holder be persisted.
         */
        public boolean isPersist() {
            boolean persist = !isError();

            if ( persist && resourceDocument instanceof ResourceEntryResourceDocument ) {
                persist = ((ResourceEntryResourceDocument)resourceDocument).getUpdatedResourceEntry() != null;
            }

            return persist;
        }

        /**
         * Is this a new resource.
         */
        public boolean isNew() {
            boolean newResource = true;

            if ( resourceDocument instanceof ResourceEntryResourceDocument ) {
                newResource = false;
            }

            return newResource;
        }

        public String getStatus() {
            return isError() ? "Failed" : "OK";
        }

        public String getAction() {
            String action = "Ignore";

            if ( !isError() ) {
                if (  resourceDocument instanceof ResourceEntryResourceDocument ) {
                    action = ((ResourceEntryResourceDocument)resourceDocument).getAction();
                } else {
                    action = "Create";
                }
            }

            return action;
        }

        public String getSystemId() {
            return resourceDocument.getUri().toString();
        }

        public void setSystemId( final URI systemId ) {
            if ( resourceDocument instanceof ResourceEntryResourceDocument ) {
                if ( isPersist() ) {
                    ((ResourceEntryResourceDocument)resourceDocument).updateUri( systemId );
                }
            } else if ( resourceDocument.available() ){
                try {
                    resourceDocument = new URIResourceDocument( systemId, resourceDocument.getContent(), null );
                } catch ( IOException ioe ) {
                    throw new IllegalStateException( "Content should be available", ioe );
                }
            } else {
                // fail later on access if the content is required
                resourceDocument = new URIResourceDocument( systemId, null, null );
            }
        }

        /**
         * Get the description for the resource.
         *
         * @return The description (may be empty, never null)
         */
        public String getDescription() {
            String description = null;
            
            if (  resourceDocument instanceof ResourceEntryResourceDocument ) {
                description = ((ResourceEntryResourceDocument)resourceDocument).resourceEntryHeader.getDescription();
            }

            if ( description == null ) {
                description = "";
            }

            return description;
        }

        public String getPublicId() {
            return getDetailForType( ResourceType.DTD );
        }

        public String getTargetNamespace() {
            return getDetailForType( ResourceType.XML_SCHEMA );
        }

        private String getDetailForType( final ResourceType type ) {
            String detail = null;

            if ( type == resourceType ) {
                detail = this.detail;                
            }

            return detail;
        }

        public String getDetails() {
            String detail = "";
            if ( this.detail != null ) {
                switch ( resourceType ) {
                    case XML_SCHEMA:
                        detail = "TNS: " + this.detail;
                        break;
                    case DTD:
                        detail = "Public ID: " + this.detail;
                        break;
                }
            }
            return detail;
        }

        public ResourceType getType() {
            return resourceType;
        }

        public String getContent() throws IOException {
            return resourceDocument.getContent();
        }

        public void setContent( final String content ) {
            if ( resourceDocument instanceof ResourceEntryResourceDocument ) {
                if ( isPersist() ) {
                    ((ResourceEntryResourceDocument)resourceDocument).updateContent( content );
                }
            } else {
                resourceDocument = new URIResourceDocument( resourceDocument.getUri(), content, null );
            }
        }

        public boolean isXml() {
            return resourceType != null && ContentTypeHeader.create( resourceType.getMimeType() ).isXml();
        }

        /**
         * Add a dependency.
         *
         * @param uri The uri reference in the format used by the resource (may be null)
         * @param absoluteUri The absolute URI for the dependency.
         */
        public void addDependency( final String uri, final String absoluteUri ) {
            dependencies.add( new Pair<String,String>( uri, absoluteUri ) );
        }

        /**
         * Get the dependencies for this resources.
         * 
         * @return The set of distinct uri (may be null) / absolute uri dependency pairs.
         */
        public Set<Pair<String,String>> getDependencies() {
            return Collections.unmodifiableSet( dependencies );
        }

        public void setDependencies( final Set<Pair<String,String>> dependencies ) {
            this.dependencies.clear();
            this.dependencies.addAll( dependencies );
        }

        public Set<String> getAbsoluteDependencies() {
            return Collections.unmodifiableSet( new HashSet<String>( Functions.map( dependencies, new Functions.Unary<String,Pair<String,String>>(){
                @Override
                public String call( final Pair<String, String> uriAndAbsoluteUri ) {
                    return uriAndAbsoluteUri.right;
                }
            } )) );
        }

        public ResourceDocument asResourceDocument() {
            return resourceDocument;
        }

        public ResourceEntry asResourceEntry() throws IOException {
            if ( resourceType == null || isError() ) throw new IOException("Cannot create resource entry");

            final ResourceEntry resourceEntry;
            if ( resourceDocument instanceof ResourceEntryResourceDocument ) {
                resourceEntry = ((ResourceEntryResourceDocument)resourceDocument).getUpdatedResourceEntry();
            } else { // create a new resource entry
                resourceEntry = new ResourceEntry();
                resourceEntry.setType( resourceType );
                resourceEntry.setUri( getSystemId() );
                resourceEntry.setContent( resourceDocument.getContent() );
                resourceEntry.setContentType( resourceType.getMimeType() );
                resourceEntry.setResourceKey1( detail );
            }
            if (resourceEntry != null) {
                resourceEntry.setSecurityZone(securityZone);
            }

            return resourceEntry;
        }

        public void updateContentFrom( final ResourceDocument update ) throws IOException {
            if ( resourceDocument instanceof ResourceEntryResourceDocument ) {
                ((ResourceEntryResourceDocument)resourceDocument).updateContentFrom( update );
            }
        }
    }

    //- PACKAGE

    GlobalResourceImportContext() {
    }

    void setResourceDocumentResolverForType( final ResourceType type,
                                             final ResourceDocumentResolver resourceDocumentResolver ){
        if ( type == ResourceType.XML_SCHEMA ) {
            schemaResolver = resourceDocumentResolver;
        } else if ( type == ResourceType.DTD ) {
            dtdResolver = resourceDocumentResolver;
        } else {
            schemaResolver = resourceDocumentResolver;
            dtdResolver = resourceDocumentResolver;
        }
    }

    ResourceDocumentResolver getResourceDocumentResolverForType( final ResourceType type ) {
        if ( type == ResourceType.XML_SCHEMA ) {
            return schemaResolver;
        } else {
            return dtdResolver;
        }
    }

    List<ResourceInputSource> getResourceInputSources() {
        return Collections.unmodifiableList( resourceInputSources );
    }

    void setResourceInputSources( final Collection<ResourceInputSource> resourceInputSources ) {
        this.resourceInputSources = new ArrayList<ResourceInputSource>( resourceInputSources );
    }

    Collection<ResourceHolder> getCurrentResourceHolders() {
        return Collections.unmodifiableCollection( currentResourceHolders );
    }

    void setCurrentResourceHolders( final Collection<ResourceHolder> currentResourceHolders ) {
        this.currentResourceHolders = currentResourceHolders;
    }

    Map<String, ResourceHolder> getProcessedResources() {
        return Collections.unmodifiableMap( processedResources );
    }

    void setProcessedResources( final Map<String, ResourceHolder> processedResources ) {
        this.processedResources = new HashMap<String, ResourceHolder>( processedResources );        
    }

    Map<ImportOption, ImportChoice> getImportOptions() {
        return Collections.unmodifiableMap( importOptions );
    }

    void setImportOptions( final Map<ImportOption, ImportChoice> importOptions ) {
        this.importOptions = new HashMap<ImportOption, ImportChoice>( importOptions );
    }

    SecurityZone getSecurityZone() {
        return securityZone;
    }

    /**
     * @param securityZone the SecurityZone to set on all imported resources.
     */
    void setSecurityZone(@Nullable final SecurityZone securityZone) {
        this.securityZone = securityZone;
    }

    ResourceDocument newResourceDocument( final String uri,
                                          final String content ) throws IOException {
        return new URIResourceDocument( asUri(uri), content, null );
    }

    static ResourceDocument newResourceDocument( final ResourceEntryHeader resourceEntryHeader,
                                                 final ResourceEntry resourceEntry ) throws IOException {
        return new ResourceEntryResourceDocument( resourceEntryHeader, resourceEntry );
    }

    ResourceInputSource newResourceInputSource( final File file ) {
        return new ResourceInputSource( file, null, null, null, getResourceDocumentResolverForType(null) );
    }

    ResourceInputSource newResourceInputSource( final URI uri, final ResourceType type ) {
        return new ResourceInputSource( null, uri, null, type, getResourceDocumentResolverForType(type) );
    }

    ResourceInputSource newResourceInputSource( final URI uri,
                                                final String content ) {
        return newResourceInputSource( uri, content, null );
    }

    ResourceInputSource newResourceInputSource( final URI uri,
                                                final String content,
                                                final ResourceType type ) {
        return new ResourceInputSource( null, uri, content, type, null );
    }

    ResourceHolder newSchemaResourceHolder( final ResourceDocument resourceDocument,
                                            final String targetNamespace,
                                            final Throwable error ) {
        return new ResourceHolder( resourceDocument, ResourceType.XML_SCHEMA, targetNamespace, error, securityZone );
    }

    ResourceHolder newDTDResourceHolder( final ResourceDocument resourceDocument,
                                         final String publicId,
                                         final Throwable error ) {
        String resourcePublicId = publicId;
        if ( resourceDocument instanceof ResourceEntryResourceDocument  ) {
            resourcePublicId = ((ResourceEntryResourceDocument)resourceDocument).resourceEntryHeader.getResourceKey1();
        }
        return new ResourceHolder( resourceDocument, ResourceType.DTD, resourcePublicId, error, securityZone );
    }

    ResourceHolder newResourceHolder( final ResourceDocument resourceDocument  ) {
        return new ResourceHolder( resourceDocument, null, null, null, securityZone );
    }

    static ResourceHolder newResourceHolder( final ResourceDocument resourceDocument, 
                                             final ResourceType resourceType ) {
        String detail = null;
        if ( resourceDocument instanceof ResourceEntryResourceDocument  ) {
            detail = ((ResourceEntryResourceDocument)resourceDocument).resourceEntryHeader.getResourceKey1();    
        }
        return new ResourceHolder( resourceDocument, resourceType, detail, null, null );
    }

    static Set<DependencySummary> getDependencies( final DependencyScope scope,
                                                   final ResourceHolder resourceHolder,
                                                   final Collection<ResourceHolder> resourceHolders ) {
        final Set<DependencySummary> dependencies = new TreeSet<DependencySummary>();

        final Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>> resolver =
                new Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>>(){
                    @Override
                    public Collection<ResourceHolder> call( final ResourceHolder resourceHolder, final Collection<ResourceHolder> resourceHolders ) {
                        Collection<ResourceHolder> dependencies = new ArrayList<ResourceHolder>();

                        for ( final String dependencyUri : resourceHolder.getAbsoluteDependencies() ) {
                            final ResourceHolder dependency = findResourceHolderByUri( resourceHolders, dependencyUri );
                            if ( dependency != null ) {
                                dependencies.add( dependency );
                            }
                        }

                        return dependencies;
                    }
                };

        resolveRecursive( dependencies, true, scope!=DependencyScope.DIRECT, resourceHolder, resourceHolders, resolver );
        filterByDependencyScope( scope, dependencies );

        return dependencies;
    }

    static Set<DependencySummary> getDependants( final DependencyScope scope,
                                                 final ResourceHolder resourceHolder,
                                                 final Collection<ResourceHolder> resourceHolders ) {
        final Set<DependencySummary> dependants = new TreeSet<DependencySummary>();

        final Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>> resolver =
                new Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>>(){
                    @Override
                    public Collection<ResourceHolder> call( final ResourceHolder resourceHolder, final Collection<ResourceHolder> resourceHolders ) {
                        Collection<ResourceHolder> dependants = new ArrayList<ResourceHolder>();

                        for ( final ResourceHolder holder : resourceHolders ) {
                            for ( final String dependencyUri : holder.getAbsoluteDependencies() ) {
                                if ( dependencyUri.equals( resourceHolder.getSystemId() ) ) {
                                    dependants.add( holder );
                                    break;
                                }
                            }
                        }

                        return dependants;
                    }
                };

        resolveRecursive( dependants, true, scope!=DependencyScope.DIRECT, resourceHolder, resourceHolders, resolver );
        filterByDependencyScope( scope, dependants );

        return dependants;
    }

    static void resolveRecursive( final Set<DependencySummary> values,
                                  final boolean isDirect,
                                  final boolean recursive,
                                  final ResourceHolder resourceHolder,
                                  final Collection<ResourceHolder> resourceHolders,
                                  final Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>> resolver ) {
        final Collection<ResourceHolder> toProcess = new ArrayList<ResourceHolder>();
        for ( final ResourceHolder resolved : resolver.call( resourceHolder, resourceHolders ) ) {
            if ( values.add( new DependencySummary(resolved.getSystemId(), !isDirect) ) && recursive ) {
                toProcess.add( resolved );
            }
        }

        // resolve transitive after direct dependencies in case any dependency
        // is both direct and transitive (in which case we currently show only
        // the direct dependency)
        for ( final ResourceHolder holderToProcess : toProcess ) {
            resolveRecursive( values, false, true, holderToProcess, resourceHolders, resolver );
        }
    }

    static void filterByDependencyScope( final DependencyScope scope,
                                         final Set<DependencySummary> dependencySummaries ) {
        if ( scope != DependencyScope.ALL ) {
            final boolean transitiveMatch = scope==DependencyScope.TRANSITIVE;

            for ( final Iterator<DependencySummary> summaryIterator = dependencySummaries.iterator() ; summaryIterator.hasNext(); ) {
                final DependencySummary summary = summaryIterator.next();
                if ( summary.isTransitive()!=transitiveMatch ) {
                    summaryIterator.remove();
                }
            }
        }
    }

    static ResourceHolder findResourceHolderByUri( final Collection<ResourceHolder> resourceHolders,
                                                   final String dependencyUri ) {
        ResourceHolder resourceHolder = null;

        for ( final ResourceHolder holder : resourceHolders ) {
            if ( dependencyUri.equals( holder.getSystemId() ) ) {
                resourceHolder = holder;
                break;
            }
        }

        return resourceHolder;
    }

    static ResourceDocumentResolver buildDownloadingResolver( final ResourceAdmin resourceAdmin ) {
        return new ResourceDocumentResolverSupport(){
            private final Collection<String> schemes = Collections.unmodifiableCollection( Arrays.asList( "http", "https") );

            @Override
            public ResourceDocument resolveByUri( final String uri ) throws IOException {
                if ( isScheme( asUri(uri), schemes ) ) {
                    final String content = resourceAdmin.resolveResource( uri );
                    return newResourceDocument( uri, content );
                } else {
                    return null;
                }
            }
        };
    }

    static ResourceDocumentResolver buildResourceEntryResolver( final ResourceAdmin resourceAdmin,
                                                                final boolean failOnDuplicateTns,
                                                                final Functions.UnaryThrows<ResourceEntryHeader,Collection<ResourceEntryHeader>,IOException> entrySelector ) {
        return new ResourceDocumentResolverSupport(){
            @Override
            public ResourceDocument resolveByUri( final String uri ) throws IOException {
                return resolveHeaders( uri, new Functions.NullaryThrows<Collection<ResourceEntryHeader>, ObjectModelException>(){
                    @Override
                    public Collection<ResourceEntryHeader> call() throws ObjectModelException {
                        final ResourceEntryHeader header = resourceAdmin.findResourceHeaderByUriAndType( uri, null );
                        return header == null ? Collections.<ResourceEntryHeader>emptySet() : Collections.singleton( header );
                    }
                } );
            }

            @Override
            public ResourceDocument resolveByPublicId( final String uri, final String publicId ) throws IOException {
                return resolveHeaders( uri, new Functions.NullaryThrows<Collection<ResourceEntryHeader>, ObjectModelException>(){
                    @Override
                    public Collection<ResourceEntryHeader> call() throws ObjectModelException {
                        return resourceAdmin.findResourceHeadersByPublicIdentifier( publicId );
                    }
                } );
            }

            @Override
            public ResourceDocument resolveByTargetNamespace( final String uri, final String targetNamespace ) throws IOException {
                return resolveHeaders( uri, new Functions.NullaryThrows<Collection<ResourceEntryHeader>, ObjectModelException>(){
                    @Override
                    public Collection<ResourceEntryHeader> call() throws ObjectModelException {
                        return resourceAdmin.findResourceHeadersByTargetNamespace( targetNamespace ); 
                    }
                } );
            }

            private ResourceDocument resolveHeaders( final String uri,
                                                     final Functions.NullaryThrows<Collection<ResourceEntryHeader>, ObjectModelException> resolver ) throws IOException {
                ResourceDocument resourceDocument = null;

                try {
                    final Collection<ResourceEntryHeader> headers = resolver.call();
                    if ( headers != null ) {
                        if ( headers.size() == 1 ) {
                            resourceDocument = new ResourceEntryResourceDocument( headers.iterator().next(), resourceAdmin );
                        } else if ( uri != null ) {
                            for ( final ResourceEntryHeader resourceEntryHeader : headers ) {
                                if ( uri.equals( resourceEntryHeader.getUri() ) ) {
                                    resourceDocument = new ResourceEntryResourceDocument( resourceEntryHeader, resourceAdmin );
                                    break;
                                }
                            }
                        }

                        if ( headers.size() > 1 && resourceDocument == null ) {
                            if ( entrySelector != null ) {
                                final ResourceEntryHeader selectedHeader = entrySelector.call( headers );
                                if ( selectedHeader != null ) {
                                    resourceDocument = new ResourceEntryResourceDocument( selectedHeader, resourceAdmin );
                                }
                            }

                            if ( resourceDocument == null && failOnDuplicateTns ) {
                                final Set<String> uris = Functions.reduce( headers, new TreeSet<String>(), new Functions.Binary<Set<String>,Set<String>,ResourceEntryHeader>(){
                                    @Override
                                    public Set<String> call( final Set<String> uris, final ResourceEntryHeader resourceEntryHeader ) {
                                        uris.add( resourceEntryHeader.getUri() );
                                        return uris;
                                    }
                                } );
                                throw new IOException( "Multiple schemas found for target namespace, system identifiers are " + uris );
                            }
                        }

                    }
                } catch ( ObjectModelException e ) {
                    throw new IOException( e );
                }

                return resourceDocument;
            }
        };
    }

    static ResourceDocumentResolver buildManualResolver( final ResourceTherapist manualResourceTherapist,
                                                         final ChoiceSelector choiceSelector ) {
        return new ResourceDocumentResolverSupport() {
            @Override
            public ResourceDocument resolveByPublicId( final String uri, final String publicId ) throws IOException {
                return resolveManually( ResourceType.DTD, uri, publicId, false,null );
            }

            @Override
            public ResourceDocument resolveByTargetNamespace( final String uri, final String targetNamespace ) throws IOException {
                return resolveManually( ResourceType.XML_SCHEMA, uri, null, true, targetNamespace );
            }

            @Override
            public ResourceDocument resolveByUri( final String uri ) throws IOException {
                ResourceType resolvedResourceType = null;
                for ( final ResourceType type : ResourceType.values() ) {
                    if ( uri.toLowerCase().endsWith( "." + type.getFilenameSuffix() )) {
                        resolvedResourceType = type;
                        break;
                    }
                }

                return resolveManually( resolvedResourceType, uri, null, false, null );
            }

            private ResourceDocument resolveManually( final ResourceType resourceType,
                                                      final String systemId,
                                                      final String publicId,
                                                      final boolean hasTargetNamespace,
                                                      final String targetNamespace ) throws IOException {
                final StringBuilder identificationBuilder = new StringBuilder();

                boolean first = true;
                if ( systemId != null && !systemId.isEmpty() ) {
                    identificationBuilder.append( "URI: " );
                    identificationBuilder.append( TextUtils.truncStringMiddleExact( systemId, 80 ) );
                    first = false;
                }

                if ( publicId != null && !publicId.isEmpty() ) {
                    if (!first) identificationBuilder.append("\n");
                    identificationBuilder.append( "Public ID: " );
                    identificationBuilder.append( TextUtils.truncStringMiddleExact( publicId, 80 ) );
                    first = false;
                }

                if ( hasTargetNamespace ) {
                    if (!first) identificationBuilder.append("\n");
                    identificationBuilder.append( "Target Namespace: " );
                    if ( targetNamespace != null ) {
                        identificationBuilder.append( TextUtils.truncStringMiddleExact( targetNamespace, 80 ) );
                    } else {
                        identificationBuilder.append( "<no namespace>" );
                    }
                }

                final String resourceIdentification = identificationBuilder.toString();
                final ImportChoice choice = choiceSelector.selectChoice( ImportOption.MISSING_RESOURCE, "missing", ImportChoice.SKIP, resourceIdentification, systemId, null );
                final ResourceDocument resourceDocument;

                switch ( choice ) {
                    case IMPORT:
                        resourceDocument = manualResourceTherapist.consult( resourceType, resourceIdentification, null, null );
                        break;
                    case SKIP:
                        throw new IOException("Missing resource skipped.");
                    default:
                        resourceDocument = null;
                        break;
                }

                return resourceDocument;
            }
        };        
    }

    ResourceDocumentResolver buildSmartResolver( final ResourceType type,
                                                 final ResourceAdmin resourceAdmin,
                                                 final Collection<ResourceDocumentResolver> externalResolvers,
                                                 final ChoiceSelector selector,
                                                 final Functions.UnaryThrows<ResourceEntryHeader,Collection<ResourceEntryHeader>,IOException> entitySelector,
                                                 final ResourceTherapist resourceTherapist ) {
        // Build an import option aware resolver
        return new ResourceDocumentResolverSupport(){
            private final ResourceType resourceType = type;
            private final ResourceDocumentResolver resourceEntityResolver = buildResourceEntryResolver( resourceAdmin, false, entitySelector );
            private final ResourceDocumentResolver externalResolver = GlobalResourceImportContext.this.getResolver( externalResolvers );
            private final ResourceDocumentResolver fullResolver = GlobalResourceImportContext.this.getResolver( Arrays.asList( resourceEntityResolver, externalResolver ) );
            private final ChoiceSelector choiceSelector = selector;
            private final ResourceTherapist manualResourceTherapist = resourceTherapist;

            @Override
            public ResourceDocument resolveByUri( final String uri ) throws IOException {
                ImportChoice choice = importOptions.get( ImportOption.CONFLICTING_URI );
                final ResourceDocument internal = resourceEntityResolver.resolveByUri( uri );

                ResourceDocument external = null;
                if ( internal == null || choice != ImportChoice.EXISTING ) {
                    try {
                        external = externalResolver.resolveByUri( uri );
                        if ( external != null ) external.getContent(); // to handle invalid resource
                    } catch ( IOException ioe ) {
                        external = doInvalid( null, null, uri, ioe );
                    }
                }

                return resolveResource( ImportOption.CONFLICTING_URI, internal, external, null );
            }

            @Override
            public ResourceDocument resolveByPublicId( final String uri, final String publicId ) throws IOException {
                ImportChoice choice = importOptions.get( ImportOption.CONFLICTING_PUBLIC_ID );
                final ResourceDocument internal = resourceEntityResolver.resolveByPublicId( uri, publicId );

                ResourceDocument external = null;
                if ( internal == null || choice != ImportChoice.EXISTING ) {
                    try {
                        external = externalResolver.resolveByPublicId( uri, publicId );
                        if ( external != null ) external.getContent(); // to handle invalid resource
                    } catch ( IOException ioe ) {
                        external = doInvalid( ResourceType.DTD, "Public ID: " + TextUtils.truncStringMiddleExact( publicId, 80 ), uri, ioe );
                    }
                }

                return resolveResource( ImportOption.CONFLICTING_PUBLIC_ID, internal, external, publicId );
            }

            @Override
            public ResourceDocument resolveByTargetNamespace( final String uri, final String targetNamespace ) throws IOException {
                ImportChoice choice = importOptions.get( ImportOption.CONFLICTING_TARGET_NAMESPACE );
                ResourceDocument internal = resourceEntityResolver.resolveByTargetNamespace( uri, targetNamespace );

                ResourceDocument external = null;
                if ( internal == null || choice != ImportChoice.EXISTING ) {
                    try {
                        external = externalResolver.resolveByTargetNamespace( uri, targetNamespace );
                        if ( external != null ) external.getContent(); // to handle invalid resource
                    } catch ( IOException ioe ) {
                        external = doInvalid( ResourceType.XML_SCHEMA, "Target Namespace: " + TextUtils.truncStringMiddleExact( targetNamespace, 80 ), uri, ioe );
                    }
                }

                if ( internal != null && external != null && !internal.getUri().equals( external.getUri() ) && targetNamespace==null ) {
                    // If the targetNamespace is null and it is not a URI conflict then ignore the internal resource
                    // it is expected that there may be many resources without namespaces (since included/redefined schemas
                    // can omit a targetNamespace)
                    internal = null;
                }

                return resolveResource( ImportOption.CONFLICTING_TARGET_NAMESPACE, internal, external, targetNamespace );
            }

            private ResourceDocument doInvalid( final ResourceType resourceType,
                                                final String detail,
                                                final String uri,
                                                final IOException error ) throws IOException {
                ResourceDocument resolved = null;

                final ImportChoice invalidResourceChoice = getChoice( ImportOption.MISSING_RESOURCE, "invalid", ImportChoice.SKIP, detail, uri, null );
                switch ( invalidResourceChoice ) {
                    case IMPORT:
                        String fullDetail = detail != null ? detail : "URI: " +  TextUtils.truncStringMiddleExact( uri, 80 );
                        resolved = manualResourceTherapist.consult(resourceType, fullDetail, resolved, ExceptionUtils.getMessage(error) );
                        break;
                    case SKIP:
                        throw new IOException("Invalid resource skipped ("+ExceptionUtils.getMessage( error )+")", error);
                }

                return resolved;
            }

            private ResourceDocument resolveResource( ImportOption option,
                                                      final ResourceDocument internal,
                                                      final ResourceDocument external,
                                                      final String conflictDetail ) throws IOException {
                final ImportChoice choice;
                ResourceDocument resolved;
                if ( internal != null && external != null ) {
                    if ( internal.getUri().equals( external.getUri() )) {
                        option = ImportOption.CONFLICTING_URI;    
                    }

                    choice = getChoice( option, null, ImportChoice.EXISTING, conflictDetail, internal.getUri().toString(), getDescription(internal) );
                    switch ( choice ) {
                        case UPDATE_ALL:
                            resolved = internal;
                            if ( internal instanceof ResourceEntryResourceDocument ) {
                                ((ResourceEntryResourceDocument)internal).updateFrom( external );
                            }
                            break;
                        case UPDATE_CONTENT:
                            resolved = internal;
                            if ( internal instanceof ResourceEntryResourceDocument ) {
                                ((ResourceEntryResourceDocument)internal).updateContentFrom( external );
                            }
                            break;
                        case IMPORT:
                            resolved = external;
                            break;
                        case SKIP:
                            throw new IOException("Conflicting resource skipped.");
                        default:
                            resolved = internal;
                            break;
                    }
                } else if ( internal != null ) {
                    resolved = internal;
                } else {
                    resolved = external;
                }

                if ( resolved != null && resolved == external && resourceType == ResourceType.XML_SCHEMA ) {
                    try {
                        XmlUtil.getSchemaTNS( 
                                resolved.getUri().toString(),
                                resolved.getContent(),
                                new ResourceHolderEntityResolver(GlobalResourceImportContext.this.getCurrentResourceHolders(),null,fullResolver) );
                    } catch ( XmlUtil.BadSchemaException e ) {
                        final String fullDetail;
                        switch ( option ) {
                            case CONFLICTING_URI:
                                fullDetail = "URI: " + TextUtils.truncStringMiddleExact( resolved.getUri().toString(), 80 );
                                break;
                            case CONFLICTING_TARGET_NAMESPACE:
                                fullDetail = "Target Namespace: " + TextUtils.truncStringMiddleExact( conflictDetail, 80 );
                                break;
                            default:
                                fullDetail = conflictDetail;
                                break;
                        }
                        final ImportChoice invalidResourceChoice = getChoice( ImportOption.MISSING_RESOURCE, "invalid", ImportChoice.SKIP, fullDetail, resolved.getUri().toString(), getDescription(resolved) );
                        switch ( invalidResourceChoice ) {
                            case IMPORT:
                                resolved = manualResourceTherapist.consult( ResourceType.XML_SCHEMA, fullDetail, resolved, ExceptionUtils.getMessage(e) );
                                break;
                            case SKIP:
                                throw new IOException("Invalid resource skipped.", e);
                        }
                    }
                }

                return resolved;
            }

            private ImportChoice getChoice( final ImportOption option,
                                            final String optionDetail,
                                            final ImportChoice defaultChoice,
                                            final String conflictDetail,
                                            final String resourceUri,
                                            final String resourceDescription ) {
                return choiceSelector.selectChoice( option, optionDetail, defaultChoice, conflictDetail, resourceUri, resourceDescription );
            }
        };
    }

    static class ResourceHolderEntityResolver implements EntityResolver {
        private final Collection<ResourceHolder> resourceHolders;
        private final EntityResolver parentEntityResolver;
        private final ResourceDocumentResolver parentDocumentResolver;

        ResourceHolderEntityResolver( final Collection<ResourceHolder> resourceHolders ) {
            this( resourceHolders, null, null );
        }

        ResourceHolderEntityResolver( final Collection<ResourceHolder> resourceHolders,
                                      final EntityResolver parentEntityResolver,
                                      final ResourceDocumentResolver parentDocumentResolver ) {
            this.resourceHolders = resourceHolders;
            this.parentEntityResolver = parentEntityResolver;
            this.parentDocumentResolver = parentDocumentResolver;
        }

        private InputSource asInputSource( final ResourceHolder resourceHolder ) throws IOException {
            final InputSource inputSource = new InputSource();
            inputSource.setSystemId( resourceHolder.getSystemId() );
            inputSource.setCharacterStream( new StringReader( resourceHolder.getContent() ) );
            return inputSource;
        }

        private InputSource asInputSource( final ResourceDocument resourceDocument ) throws IOException {
            final InputSource inputSource = new InputSource();
            inputSource.setSystemId( resourceDocument.getUri().toString() );
            inputSource.setCharacterStream( new StringReader( resourceDocument.getContent() ) );
            return inputSource;
        }

        @Override
        public InputSource resolveEntity( final String publicId, final String systemId ) throws SAXException, IOException {
            InputSource inputSource = null;

            if ( systemId != null ) {
                for ( final ResourceHolder resourceHolder : resourceHolders ) {
                    if ( systemId.equals(resourceHolder.getSystemId()) ) {
                        inputSource = asInputSource(resourceHolder);
                        break;
                    }
                }
            }

            if ( inputSource == null && publicId != null ) {
                for ( final ResourceHolder resourceHolder : resourceHolders ) {
                    if ( publicId.equals(resourceHolder.getPublicId()) ) {
                        inputSource = asInputSource(resourceHolder);
                        break;
                    }
                }
            }

            if ( inputSource == null && parentEntityResolver != null ) {
                inputSource = parentEntityResolver.resolveEntity( publicId, systemId );
            }

            if ( inputSource == null && parentDocumentResolver != null ) {
                final ResourceDocument resourceDocument = parentDocumentResolver.resolveByPublicId( systemId, publicId );
                if ( resourceDocument != null ) {
                    inputSource = asInputSource(resourceDocument);
                }
            }

            if ( inputSource == null ) {
                throw new IOException( "Resource not found '"+systemId+"', public identifier '"+publicId+"'" );
            }

            return inputSource;
        }
    }

    //- PROTECTED

    /**
     * Interface for fixing missing or invalid resources.
     */
    protected interface ResourceTherapist {

        /**
         * Find or fix the resource for the given details.
         *
         * @param resourceType The resource type if known (optional)
         * @param resourceDescription A description of the resource (required)
         * @param invalidResource The invalid resource if any (optional)
         * @param invalidDetail The reason the resource is invalid (optional)
         * @return The fixed/located document or null to fail
         */
        ResourceDocument consult( ResourceType resourceType,
                                  String resourceDescription,
                                  ResourceDocument invalidResource,
                                  String invalidDetail );
    }

    /**
     * Interface for selection of import choices.
     */
    protected interface ChoiceSelector {

        /**
         * Select an import choice for the given option.
         *
         * @param option The option for which a choice is required (required)
         * @param optionDetail The detail for the option (optional)
         * @param defaultChoice The default choice to use (required)
         * @param conflictDetail Identifying information for the resource (optional)
         * @param resourceUri Identifying information for the resource (optional)
         * @param resourceDescription A description of the resource (optional)
         * @return The selected choice (not null)
         */
        ImportChoice selectChoice( ImportOption option,
                                   String optionDetail,
                                   ImportChoice defaultChoice,
                                   String conflictDetail,
                                   String resourceUri,
                                   String resourceDescription );
    }

    /**
     * The current choice for an import option, not all choices are applicable for all options.
     */
    protected enum ImportChoice {
        /**
         * No current selection
         */
        NONE,

        /**
         * Use an existing dependency
         */
        EXISTING,

        /**
         * Update the content of the existing dependency
         */
        UPDATE_CONTENT,

        /**
         * Update the existing dependency and it's metadata (such as URI)
         */
        UPDATE_ALL,

        /**
         * Automatically or manually import the new dependency.
         */
        IMPORT,

        /**
         * Skip the dependency an all resources that depend on it.
         */
        SKIP
    }

    protected enum ImportOption {
        /**
         * Option for a dependency with a target namespace that matches multiple existing resources.
         */
        AMBIGUOUS_TARGET_NAMESPACE( EnumSet.of( EXISTING, IMPORT, SKIP ) ),

        /**
         * Option for a dependency with a public identifier that matches multiple existing resources.
         */
        AMBIGUOUS_PUBLIC_ID( EnumSet.of( EXISTING, IMPORT, SKIP ) ),

        /**
         * Option for a dependency with a URI that matches an existing resource.
         */
        CONFLICTING_URI( EnumSet.of( EXISTING, UPDATE_ALL, SKIP ) ),

        /**
         * Option for a dependency with a target namespace that matches an existing resource.
         */
        CONFLICTING_TARGET_NAMESPACE( EnumSet.of( EXISTING, UPDATE_CONTENT, UPDATE_ALL, IMPORT, SKIP ) ),

        /**
         * Option for a dependency with a public identifier that matches an existing resource.
         */
        CONFLICTING_PUBLIC_ID( EnumSet.of( EXISTING, UPDATE_CONTENT, UPDATE_ALL, IMPORT, SKIP ) ),

        /**
         * Option for a missing dependency.
         */
        MISSING_RESOURCE( EnumSet.of( IMPORT, SKIP ) );

        private final EnumSet<ImportChoice> choices;

        private ImportOption( final EnumSet<ImportChoice> choices ) {
            this.choices = choices;
        }

        public EnumSet<ImportChoice> getImportChoices() {
            return choices;
        }
    }

    protected static class DependencySummary implements Comparable<DependencySummary> {
        private final String uri;
        private final boolean transitive;

        protected DependencySummary( final String uri,
                                     final boolean transitive ) {
            if ( uri == null ) throw new IllegalArgumentException( "uri is required" );
            this.uri = uri;
            this.transitive = transitive;
        }

        public String getUri() {
            return uri;
        }

        public boolean isTransitive() {
            return transitive;
        }

        public String toString() {
            return uri;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final DependencySummary that = (DependencySummary) o;

            if ( !uri.equals( that.uri ) ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return uri.hashCode();
        }

        @Override
        public int compareTo( final DependencySummary other ) {
            return uri.compareTo( other.uri );
        }
    }

    protected enum DependencyScope {
        ALL,
        DIRECT,
        TRANSITIVE
    }

    //- PRIVATE

    private List<ResourceInputSource> resourceInputSources = Collections.emptyList();
    private Collection<ResourceHolder> currentResourceHolders = Collections.emptyList();
    private Map<String, ResourceHolder> processedResources = Collections.emptyMap();
    private Map<ImportOption,ImportChoice> importOptions = buildImportOptionMap();
    private ResourceDocumentResolver schemaResolver;
    private ResourceDocumentResolver dtdResolver;
    private SecurityZone securityZone;

    private ResourceDocumentResolver getResolver( final Collection<ResourceDocumentResolver> resolvers ) {
        final ResourceDocumentResolver resolver;
        if ( resolvers.isEmpty() ) {
            resolver = new ResourceDocumentResolverSupport();
        } else if ( resolvers.size() == 1 ) {
            resolver = resolvers.iterator().next();
        } else {
            resolver = new CompositeResourceDocumentResolver( resolvers );
        }

        return resolver;
    }

    private static String getDescription( final ResourceDocument resourceDocument ) {
        String description = null;

        if ( resourceDocument instanceof ResourceEntryResourceDocument ) {
            final ResourceEntryResourceDocument resourceEntryResourceDocument =
                    (ResourceEntryResourceDocument) resourceDocument;
            description = resourceEntryResourceDocument.resourceEntryHeader.getDescription();
        }

        return description;
    }

    private static URI asUri( final String uri ) throws IOException {
        try {
            return new URI(uri);
        } catch ( URISyntaxException e ) {
            throw new IOException( e );
        }
    }

    private static Map<ImportOption,ImportChoice> buildImportOptionMap() {
        final Map<ImportOption,ImportChoice> importOptions = new HashMap<ImportOption,ImportChoice>();

        for ( final ImportOption option : ImportOption.values() ) {
            importOptions.put( option, ImportChoice.NONE );
        }

        return importOptions;
    }

    private static class ResourceEntryResourceDocument implements ResourceDocument {
        private URI uri;
        private final ResourceAdmin resourceAdmin;
        private final ResourceEntryHeader resourceEntryHeader;
        private ResourceEntry resourceEntry;
        private String updatedContent;

        private ResourceEntryResourceDocument( final ResourceEntryHeader resourceEntryHeader,
                                               final ResourceAdmin resourceAdmin ) throws IOException {
            this.uri = asUri(resourceEntryHeader.getUri());
            this.resourceEntryHeader = resourceEntryHeader;
            this.resourceAdmin = resourceAdmin;
        }

        private ResourceEntryResourceDocument( final ResourceEntryHeader resourceEntryHeader,
                                               final ResourceEntry resourceEntry ) throws IOException {
            this.uri = asUri(resourceEntryHeader.getUri());
            this.resourceEntryHeader = resourceEntryHeader;
            this.resourceEntry = resourceEntry;
            this.resourceAdmin = null;
        }

        @Override
        public boolean available() {
            return resourceEntry != null;
        }

        @Override
        public URI getUri() {
            return uri;
        }

        @Override
        public String getContent() throws IOException {
            if ( resourceEntry == null ) {
                try {
                    resourceEntry = resourceAdmin.findResourceEntryByPrimaryKey( resourceEntryHeader.getOid() );
                } catch ( FindException e ) {
                    throw new IOException(e);
                }

                if ( resourceEntry == null ) {
                    throw new IOException("Global resource no longer available '"+resourceEntryHeader.getUri()+"'");
                }

                updateResourceEntry();
            }

            return resourceEntry.getContent();
        }

        @Override
        public ResourceDocument relative( final String path,
                                          final ResourceDocumentResolver resolver ) throws IOException {
            ResourceDocument resolved;

            if ( resolver == null ) {
                throw new IOException( "Unable to resolve path '" + path + "', no resolver available.");
            }

            try {
                final String resolvedUri = uri.resolve( path ).toString();
                resolved = resolver.resolveByUri( resolvedUri );
                if ( resolved == null ) {
                    throw new IOException("Resource not found for URI '"+resolvedUri+"'");
                }
            } catch ( IllegalArgumentException e ) {
                throw new IOException( "Unable to resolve path '" + path + "', due to : " + ExceptionUtils.getMessage( e ));
            }

            return resolved;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public long getLastModified() {
            return 0;
        }

        public void updateFrom( final ResourceDocument resourceDocument ) throws IOException {
            this.uri = resourceDocument.getUri();
            this.updatedContent = resourceDocument.getContent();
            updateResourceEntry();
        }

        public void updateContentFrom( final ResourceDocument resourceDocument ) throws IOException {
            updateContent( resourceDocument.getContent() );
        }

        public void updateUri( final URI uri ) {
            this.uri = uri;
            updateResourceEntry();
        }

        public void updateContent( final String content ) {
            this.updatedContent = content;
            updateResourceEntry();
        }

        /**
         * Get the updated resource entry (if any)
         *
         * @return The updated entry or null.
         */
        public ResourceEntry getUpdatedResourceEntry() {
            return updatedContent!=null ? resourceEntry : null;
        }

        public String getAction() {
            return updatedContent!=null ? "Update" : "Ignore";
        }

        private void updateResourceEntry() {
            if ( resourceEntry != null && updatedContent != null) {
                resourceEntry.setUri( uri.toString() );
                resourceEntry.setContent( updatedContent );
            }
        }
    }

    /**
     * Resolver that delegates to an underlying collection of resolvers.
     *
     * <p>This resolver does not (and should not) fail over, a resolver must
     * return null to allow the next resolver to be used.<p>
     */
    private static class CompositeResourceDocumentResolver extends ResourceDocumentResolverSupport {
        private final Collection<ResourceDocumentResolver> resolvers;


        private CompositeResourceDocumentResolver( final Collection<ResourceDocumentResolver> resolvers ) {
            this.resolvers = Collections.unmodifiableCollection( new ArrayList<ResourceDocumentResolver>( resolvers ) );   
        }

        @Override
        public ResourceDocument resolveByPublicId( final String uri, final String publicId ) throws IOException {
            ResourceDocument resourceDocument = null;

            for ( final ResourceDocumentResolver resolver : resolvers ) {
                resourceDocument = resolver.resolveByPublicId( uri, publicId );
                if ( resourceDocument != null ) break;
            }

            return resourceDocument;
        }

        @Override
        public ResourceDocument resolveByTargetNamespace( final String uri, final String targetNamespace ) throws IOException {
            ResourceDocument resourceDocument = null;

            for ( final ResourceDocumentResolver resolver : resolvers ) {
                resourceDocument = resolver.resolveByTargetNamespace( uri, targetNamespace );
                if ( resourceDocument != null ) break;
            }

            return resourceDocument;
        }

        @Override
        public ResourceDocument resolveByUri( final String uri ) throws IOException {
            ResourceDocument resourceDocument = null;

            for ( final ResourceDocumentResolver resolver : resolvers ) {
                resourceDocument = resolver.resolveByUri( uri ); 
                if ( resourceDocument != null ) break;
            }

            return resourceDocument;
        }
    }
}
