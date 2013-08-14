package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.communityschemas.SchemaSourceResolver;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationEvent;
import org.xml.sax.InputSource;
import org.xml.sax.ext.EntityResolver2;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Schema source for global resources.
 */
public class ResourceEntrySchemaSourceResolver implements PostStartupApplicationListener, EntityResolver2, SchemaSourceResolver {

    //- PUBLIC

    public static final String RESOLVER_ID = "Global Resource Resolver";

    public ResourceEntrySchemaSourceResolver( final ResourceEntryManager resourceEntryManager ) {
        this.resourceEntryManager = resourceEntryManager;
    }

    @Override
    public String getId() {
        return RESOLVER_ID;
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public SchemaSource getSchemaByTargetNamespace( final Audit audit,
                                                    final String targetNamespace ) throws IOException {
        SchemaSource schema = null;

        try {
            final Collection<ResourceEntryHeader> resourceHeaders = resourceEntryManager.findHeadersByTNS( targetNamespace );
            if ( resourceHeaders.size() == 1 ) {
                final ResourceEntry entry = resourceEntryManager.findByPrimaryKey( resourceHeaders.iterator().next().getGoid() );
                schema = resolved( entry );
            } else if ( !resourceHeaders.isEmpty() ) {
                final Set<String> uris = Functions.reduce( resourceHeaders, new TreeSet<String>(), new Functions.Binary<Set<String>,Set<String>,ResourceEntryHeader>(){
                    @Override
                    public Set<String> call( final Set<String> uris, final ResourceEntryHeader resourceEntryHeader ) {
                        uris.add( resourceEntryHeader.getUri() );
                        return uris;
                    }
                } );
                throw new IOException( "Multiple schemas found for target namespace, system identifiers are " + uris );
            }
        } catch ( FindException e ) {
            // The schema could be from another source, but we cannot be sure it
            // is not a global resource so it seems safer to propagate the error
            throw new CausedIOException( e );
        }

        return schema;
    }

    @Override
    public SchemaSource getSchemaByUri( final Audit audit,
                                        final String uri ) throws IOException {
        SchemaSource schema;

        try {
            final ResourceEntry entry = resourceEntryManager.findResourceByUriAndType( uri, ResourceType.XML_SCHEMA );
            schema = resolved( entry );
        } catch ( FindException e ) {
            // The schema could be from another source, but we cannot be sure it
            // is not a global resource so it seems safer to propagate the error
            throw new CausedIOException( e );
        }

        return schema;
    }

    @Override
    public void refreshSchemaByUri( final Audit audit,
                                    final String uri ) throws IOException {
        // invalidation occurs at any time due to entity invalidation
    }

    @Override
    public void registerInvalidationListener( final SchemaInvalidationListener listener ) {
        listenerRef.set( listener );
    }

    @Override
    public InputSource getExternalSubset( final String name,
                                          final String baseURI ) throws IOException {
        return null;
    }

    @Override
    public InputSource resolveEntity( final String name,
                                      final String publicId,
                                      final String baseURI,
                                      final String systemId ) throws IOException {
        InputSource inputSource = null;

        try {
            ResourceEntry entry = findByPublicIdentifier( publicId );

            if ( entry == null ) {
                // try by unresolved URI
                entry = resourceEntryManager.findResourceByUriAndType( systemId, ResourceType.DTD );
            }

            if ( entry == null && baseURI != null ) {
                // try by resolved URI
                final URI uri = new URI( systemId );
                if ( !uri.isAbsolute() ) {
                    final URI base = new URI(baseURI);
                    if ( base.isAbsolute() ) {
                        entry = resourceEntryManager.findResourceByUriAndType( base.resolve( uri ).toString(), ResourceType.DTD );
                    }
                }
            }

            if ( entry != null ) {
                inputSource = new InputSource();
                inputSource.setSystemId( entry.getUri() );
                inputSource.setCharacterStream( new StringReader( entry.getContent() ) );
            }
        } catch ( FindException e ) {
            // The schema could be from another source, but we cannot be sure it
            // is not a global resource so it seems safer to propagate the error
            throw new CausedIOException( e );
        } catch ( URISyntaxException e ) {
            throw new CausedIOException( e );
        }

        return inputSource;
    }

    /**
     * Entity resolver (should not be used)
     */
    @Override
    public InputSource resolveEntity( final String publicId,
                                      final String systemId ) throws IOException {
        return resolveEntity("entity", publicId, systemId, systemId );
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof GoidEntityInvalidationEvent) {
            final GoidEntityInvalidationEvent invalidationEvent = (GoidEntityInvalidationEvent) event;
            if ( ResourceEntry.class.isAssignableFrom(invalidationEvent.getEntityClass()) ) {
                invalidateSchemas( invalidationEvent.getEntityIds() );
            }
        }
    }

    //- PRIVATE

    private final ResourceEntryManager resourceEntryManager;
    private final Object goidToUriMapLock = new Object();
    private final Map<Goid, Set<String>> goidToUriMap = new HashMap<Goid,Set<String>>();
    private final AtomicReference<SchemaInvalidationListener> listenerRef = new AtomicReference<SchemaInvalidationListener>();

    /**
     *
     */
    private SchemaSource resolved( final ResourceEntry resourceEntry ) {
        SchemaSource schema = null;

        if ( resourceEntry != null ) {
            synchronized( goidToUriMapLock ) {
                Set<String> urisForGoid = goidToUriMap.get( resourceEntry.getGoid() );
                if ( urisForGoid == null ) {
                    urisForGoid = new HashSet<String>();
                    goidToUriMap.put( resourceEntry.getGoid(), urisForGoid );
                }
                urisForGoid.add( resourceEntry.getUri() );
            }
            schema = new DefaultSchemaSource( resourceEntry.getUri(), resourceEntry.getContent(), this );
        }

        return schema;
    }

    private ResourceEntry findByPublicIdentifier( final String publicId ) throws FindException {
        ResourceEntry resourceEntry = null;

        if ( publicId != null ) {
            final Collection<ResourceEntryHeader> headers = resourceEntryManager.findHeadersByPublicIdentifier( publicId );
            if ( headers.size() == 1 ) {
                resourceEntry = resourceEntryManager.findByHeader( headers.iterator().next() );
            }
        }

        return resourceEntry;
    }

    /**
     *
     */
    private void invalidateSchemas( final Goid[] resourceEntryGoids ) {
        final SchemaInvalidationListener listener = listenerRef.get();
        if ( listener != null ) {
            final Collection<Goid> resourceEntryGoidCollection = Arrays.asList(resourceEntryGoids);
            final Map<Goid, Set<String>> goidToUriMap;
            synchronized( goidToUriMapLock ) {
                goidToUriMap = new HashMap<Goid,Set<String>>( this.goidToUriMap );
                this.goidToUriMap.keySet().removeAll( resourceEntryGoidCollection );
            }

            goidToUriMap.keySet().retainAll(resourceEntryGoidCollection);
            final Set<String> uris = Functions.reduce( goidToUriMap.values(), new HashSet<String>(), new Functions.Binary<Set<String>,Set<String>,Set<String>>(){
                @Override
                public Set<String> call( final Set<String> aggregated, final Set<String> urisForGoid ) {
                    aggregated.addAll( urisForGoid );
                    return aggregated;
                }
            } );

            for ( String uri : uris ) {
                listenerRef.get().invalidateSchemaByUri( uri, true );
            }
        }
    }

}
