package com.l7tech.console.panels;

import com.l7tech.common.io.ResourceDocument;
import com.l7tech.common.io.ResourceDocumentResolver;
import com.l7tech.common.io.ResourceDocumentResolverSupport;
import com.l7tech.common.io.URIResourceDocument;
import com.l7tech.common.mime.ContentTypeHeader;
import static com.l7tech.console.panels.GlobalResourceImportContext.ImportChoice.*;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import java.io.File;
import java.io.IOException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Context for global resource imports.
 */
class GlobalResourceImportContext {

    //- PUBLIC

    public static class ResourceInputSource {
        private final URI uri;
        private final File file;
        private final String content;
        private final ResourceDocumentResolver resolver;

        private ResourceInputSource( final File file,
                                     final URI uri,
                                     final String content,
                                     final ResourceDocumentResolver resolver ) {
            if ( file == null && uri == null) throw new IllegalArgumentException("file or url is required.");
            if ( content == null && resolver == null) throw new IllegalArgumentException("content or resolver is required.");
            this.file = file;
            this.content = content;
            this.resolver = resolver;
            this.uri = file!=null ? file.toURI() : uri;
        }

        public URI getUri() {
            return uri;
        }

        public String getUriString() {
            return uri.toString();
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
        private final ResourceDocument resourceDocument;
        private final ResourceType resourceType;
        private final String detail;
        private final Throwable error;
        private final Set<String> dependencies = new HashSet<String>();

        private ResourceHolder( final ResourceDocument resourceDocument,
                                final ResourceType resourceType,
                                final String detail,
                                final Throwable error ) {
            if ( resourceDocument == null ) throw new IllegalArgumentException( "resourceDocument is required" );
            this.resourceDocument = resourceDocument;
            this.resourceType = resourceType;
            this.detail = detail;
            this.error = error;
        }

        public boolean isError() {
            return error != null;
        }

        public Throwable getError() {
            return error;
        }

        /**
         * Should this resource holder be persist.
         *
         * @return
         */
        public boolean isPersist() {
            boolean persist = !isError();

            if ( persist && resourceDocument instanceof ResourceEntryResourceDocument ) {
                persist = ((ResourceEntryResourceDocument)resourceDocument).getUpdatedResourceEntry() != null;
            }

            return persist;
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

        public boolean isXml() {
            return resourceType != null && ContentTypeHeader.create( resourceType.getMimeType() ).isXml();
        }

        public void addDependency( final String uri ) {
            dependencies.add( uri );
        }

        public Set<String> getDependencies() {
            return Collections.unmodifiableSet( dependencies );
        }

        public ResourceDocument asResourceDocument() {
            return resourceDocument;
        }

        public ResourceEntry asResourceEntry() throws IOException {
            if ( resourceType == null || isError() ) throw new CausedIOException("Cannot create resource entry");

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

            return resourceEntry;
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

    Map<String, ResourceHolder> getProcessedResources() {
        return Collections.unmodifiableMap( processedResources );
    }

    void setProcessedResources( final Map<String, ResourceHolder> processedResources ) {
        this.processedResources = new HashMap<String, ResourceHolder>( processedResources );        
    }

    public Map<ImportOption, ImportChoice> getImportOptions() {
        return Collections.unmodifiableMap( importOptions );
    }

    public void setImportOptions( final Map<ImportOption, ImportChoice> importOptions ) {
        this.importOptions = new HashMap<ImportOption, ImportChoice>( importOptions );
    }

    ResourceDocument newResourceDocument( final String uri,
                                          final String content ) throws IOException {
        return new URIResourceDocument( asUri(uri), content, null );
    }

    ResourceInputSource newResourceInputSource( final File file ) {
        return new ResourceInputSource( file, null, null, getResourceDocumentResolverForType(null) );
    }

    ResourceInputSource newResourceInputSource( final URI uri, final ResourceType type ) {
        return new ResourceInputSource( null, uri, null, getResourceDocumentResolverForType(type) );
    }

    ResourceInputSource newResourceInputSource( final URI uri,
                                                final String content ) {
        return new ResourceInputSource( null, uri, content, null );
    }

    ResourceHolder newSchemaResourceHolder( final ResourceDocument resourceDocument,
                                            final String targetNamespace,
                                            final Throwable error ) {
        return new ResourceHolder( resourceDocument, ResourceType.XML_SCHEMA, targetNamespace, error );
    }

    ResourceHolder newDTDResourceHolder( final ResourceDocument resourceDocument,
                                         final String publicId,
                                         final Throwable error ) {
        return new ResourceHolder( resourceDocument, ResourceType.DTD, publicId, error );
    }

    ResourceHolder newResourceHolder( final ResourceDocument resourceDocument  ) {
        return new ResourceHolder( resourceDocument, null, null, null );
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

                        for ( final String dependencyUri : resourceHolder.getDependencies() ) {
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
                            for ( final String dependencyUri : holder.getDependencies() ) {
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

    static ResourceDocumentResolver buildResourceEntryResolver( final ResourceAdmin resourceAdmin ) {
        return new ResourceDocumentResolverSupport(){
            @Override
            public ResourceDocument resolveByUri( final String uri ) throws IOException {
                return resolveHeaders( new Functions.NullaryThrows<Collection<ResourceEntryHeader>, ObjectModelException>(){
                    @Override
                    public Collection<ResourceEntryHeader> call() throws ObjectModelException {
                        final ResourceEntryHeader header = resourceAdmin.findResourceHeaderByUriAndType( uri, null );
                        return header == null ? Collections.<ResourceEntryHeader>emptySet() : Collections.singleton( header );
                    }
                } );
            }

            @Override
            public ResourceDocument resolveByPublicId( final String publicId ) throws IOException {
                return resolveHeaders( new Functions.NullaryThrows<Collection<ResourceEntryHeader>, ObjectModelException>(){
                    @Override
                    public Collection<ResourceEntryHeader> call() throws ObjectModelException {
                        return resourceAdmin.findResourceHeadersByPublicIdentifier( publicId );
                    }
                } );
            }

            @Override
            public ResourceDocument resolveByTargetNamespace( final String targetNamespace ) throws IOException {
                return resolveHeaders( new Functions.NullaryThrows<Collection<ResourceEntryHeader>, ObjectModelException>(){
                    @Override
                    public Collection<ResourceEntryHeader> call() throws ObjectModelException {
                        return resourceAdmin.findResourceHeadersByTargetNamespace( targetNamespace ); 
                    }
                } );
            }

            private ResourceDocument resolveHeaders( final Functions.NullaryThrows<Collection<ResourceEntryHeader>, ObjectModelException> resolver ) throws IOException {
                ResourceDocument resourceDocument = null;

                try {
                    final Collection<ResourceEntryHeader> headers = resolver.call();
                    if ( headers != null ) {
                        if ( headers.size() == 1 ) {
                            resourceDocument = new ResourceEntryResourceDocument( headers.iterator().next(), resourceAdmin );
                        }
                    }
                } catch ( ObjectModelException e ) {
                    throw new IOException( e );
                }

                return resourceDocument;
            }
        };
    }

    ResourceDocumentResolver buildSmartResourceEntryResolver( final ResourceType type,
                                                              final ResourceAdmin resourceAdmin,
                                                              final Collection<ResourceDocumentResolver> externalResolvers,
                                                              final Functions.Ternary<ImportChoice,ImportOption,ImportChoice,String> choiceResolver ) {
        // Build an import option aware resolver
        return new ResourceDocumentResolverSupport(){
            private final ResourceType resourceType = type;
            private final ResourceDocumentResolver resourceEntityResolver = buildResourceEntryResolver( resourceAdmin );
            private final ResourceDocumentResolver externalResolver = GlobalResourceImportContext.this.getResolver( externalResolvers );

            @Override
            public ResourceDocument resolveByUri( final String uri ) throws IOException {
                ImportChoice choice = importOptions.get( ImportOption.CONFLICTING_URI );
                final ResourceDocument internal = resourceEntityResolver.resolveByUri( uri );

                ResourceDocument external = null;
                if ( internal == null || choice != ImportChoice.EXISTING ) {
                    try {
                        external = externalResolver.resolveByUri( uri );
                    } catch ( IOException ioe ) {
                        if ( internal == null ) throw ioe;
                        logger.log( Level.WARNING, "Error resolving external resource for URI '"+uri+"', using internal resource.", ExceptionUtils.getDebugException(ioe));
                    }
                }

                ResourceDocument resolved = resolveResource( ImportOption.CONFLICTING_URI, internal, external, uri );
                if ( resolved == null ) {
                    //TODO [steve] handle missing resource
                } else if ( resolved == external && resourceType == ResourceType.XML_SCHEMA ) {
                    //TODO [steve] check for and handle invalid resource
                }

                return resolved;
            }

            @Override
            public ResourceDocument resolveByPublicId( final String publicId ) throws IOException {
                ImportChoice choice = importOptions.get( ImportOption.CONFLICTING_PUBLIC_ID );
                final ResourceDocument internal = resourceEntityResolver.resolveByPublicId( publicId );

                ResourceDocument external = null;
                if ( internal == null || choice != ImportChoice.EXISTING ) {
                    try {
                        external = externalResolver.resolveByPublicId( publicId );
                    } catch ( IOException ioe ) {
                        if ( internal == null ) throw ioe;
                        logger.log( Level.WARNING, "Error resolving external resource for Public ID '"+publicId+"', using internal resource.", ExceptionUtils.getDebugException(ioe));
                    }
                }

                return resolveResource( ImportOption.CONFLICTING_PUBLIC_ID, internal, external, publicId );
            }

            @Override
            public ResourceDocument resolveByTargetNamespace( final String targetNamespace ) throws IOException {
                ImportChoice choice = importOptions.get( ImportOption.CONFLICTING_TARGET_NAMESPACE );
                final ResourceDocument internal = resourceEntityResolver.resolveByTargetNamespace( targetNamespace );

                ResourceDocument external = null;
                if ( internal == null || choice != ImportChoice.EXISTING ) {
                    try {
                        external = externalResolver.resolveByTargetNamespace( targetNamespace );
                    } catch ( IOException ioe ) {
                        if ( internal == null ) throw ioe;
                        logger.log( Level.WARNING, "Error resolving external resource for namespace '"+targetNamespace+"', using internal resource.", ExceptionUtils.getDebugException(ioe));
                    }
                }

                return resolveResource( ImportOption.CONFLICTING_TARGET_NAMESPACE, internal, external, targetNamespace );
            }

            private ResourceDocument resolveResource( final ImportOption option,
                                                      final ResourceDocument internal,
                                                      final ResourceDocument external,
                                                      final String resourceDetail ) throws IOException {
                final ImportChoice choice;
                final ResourceDocument resolved;
                if ( internal != null && external != null ) {
                    choice = getChoice( option, ImportChoice.EXISTING, resourceDetail );
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

                return resolved;
            }

            private ImportChoice getChoice( final ImportOption option,
                                            final ImportChoice defaultChoice,
                                            final String resourceDetail ) {
                ImportChoice choice = importOptions.get( option );

                if ( choice == NONE ) {
                    choice = choiceResolver.call( option, defaultChoice, resourceDetail );
                }

                return choice;
            }
        };
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
         * Use the existing dependency
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
         * Option for a dependency with a URI that matches an existing resource.
         */
        CONFLICTING_URI( EnumSet.of( EXISTING, UPDATE_ALL, SKIP ) ),

        /**
         * Option for a dependency with a target namespace that matches an existing resource.
         */
        CONFLICTING_TARGET_NAMESPACE( EnumSet.of( EXISTING, UPDATE_CONTENT, UPDATE_ALL, IMPORT, SKIP ) ),

        /**
         * Option for a dependency with a target namespace that matches an existing resource.
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

    private static final Logger logger = Logger.getLogger( GlobalResourceImportContext.class.getName() );

    private List<ResourceInputSource> resourceInputSources = Collections.emptyList();
    private Map<String, ResourceHolder> processedResources = Collections.emptyMap();
    private Map<ImportOption,ImportChoice> importOptions = buildImportOptionMap();
    private ResourceDocumentResolver schemaResolver;
    private ResourceDocumentResolver dtdResolver;

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

    private static URI asUri( final String uri ) throws IOException {
        try {
            return new URI(uri);
        } catch ( URISyntaxException e ) {
            throw new CausedIOException( e );
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
                    throw new CausedIOException(e);
                }

                if ( resourceEntry == null ) {
                    throw new CausedIOException("Global resource no longer available '"+resourceEntryHeader.getUri()+"'");
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
            this.updatedContent = resourceDocument.getContent();
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
        public ResourceDocument resolveByPublicId( final String publicId ) throws IOException {
            ResourceDocument resourceDocument = null;

            for ( final ResourceDocumentResolver resolver : resolvers ) {
                resourceDocument = resolver.resolveByPublicId( publicId );
                if ( resourceDocument != null ) break;
            }

            return resourceDocument;
        }

        @Override
        public ResourceDocument resolveByTargetNamespace( final String targetNamespace ) throws IOException {
            ResourceDocument resourceDocument = null;

            for ( final ResourceDocumentResolver resolver : resolvers ) {
                resourceDocument = resolver.resolveByTargetNamespace( targetNamespace );
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
