package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.communityschemas.SchemaSourceResolver;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Schema source for global resources.
 */
public class ResourceEntrySchemaSourceResolver implements ApplicationListener, SchemaSourceResolver {

    //- PUBLIC

    public static String RESOLVER_ID = "Global Resource Resolver";

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
    public SchemaSource getSchemaByTargetNamespace( final String targetNamespace ) throws IOException {
        SchemaSource schema = null;

        try {
            final Collection<ResourceEntryHeader> resourceHeaders = resourceEntryManager.findHeadersByTNS( targetNamespace );
            if ( resourceHeaders.size() == 1 ) {
                final ResourceEntry entry = resourceEntryManager.findByPrimaryKey( resourceHeaders.iterator().next().getOid() );
                schema = resolved( entry );
            }
        } catch ( FindException e ) {
            // The schema could be from another source, but we cannot be sure it
            // is not a global resource so it seems safer to propagate the error
            throw new CausedIOException( e );
        }

        return schema;
    }

    @Override
    public SchemaSource getSchemaByUri( final String uri ) throws IOException {
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
    public void refreshSchemaByUri( final String uri ) throws IOException {
        // invalidation occurs at any time due to entity invalidation
    }

    @Override
    public void registerInvalidationListener( final SchemaInvalidationListener listener ) {
        listenerRef.set( listener );
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof EntityInvalidationEvent ) {
            final EntityInvalidationEvent invalidationEvent = (EntityInvalidationEvent) event;
            if ( ResourceEntry.class.isAssignableFrom(invalidationEvent.getEntityClass()) ) {
                invalidateSchemas( invalidationEvent.getEntityIds() );
            }
        }
    }

    //- PRIVATE

    private final ResourceEntryManager resourceEntryManager;
    private final Object oidToUriMapLock = new Object();
    private final Map<Long, Set<String>> oidToUriMap = new HashMap<Long,Set<String>>();
    private final AtomicReference<SchemaInvalidationListener> listenerRef = new AtomicReference<SchemaInvalidationListener>();

    /**
     *
     */
    private SchemaSource resolved( final ResourceEntry resourceEntry ) {
        SchemaSource schema = null;

        if ( resourceEntry != null ) {
            synchronized( oidToUriMapLock ) {
                Set<String> urisForOid = oidToUriMap.get( resourceEntry.getOid() );
                if ( urisForOid == null ) {
                    urisForOid = new HashSet<String>();
                    oidToUriMap.put( resourceEntry.getOid(), urisForOid );
                }
                urisForOid.add( resourceEntry.getUri() );
            }
            schema = new DefaultSchemaSource( resourceEntry.getUri(), resourceEntry.getContent(), this );
        }

        return schema;
    }

    /**
     *
     */
    private void invalidateSchemas( final long[] resourceEntryOids ) {
        final SchemaInvalidationListener listener = listenerRef.get();
        if ( listener != null ) {
            final Collection<Long> resourceEntryOidCollection = Arrays.asList(ArrayUtils.box(resourceEntryOids));
            final Map<Long, Set<String>> oidToUriMap;
            synchronized( oidToUriMapLock ) {
                oidToUriMap = new HashMap<Long,Set<String>>( this.oidToUriMap );
                this.oidToUriMap.keySet().removeAll( resourceEntryOidCollection );
            }

            oidToUriMap.keySet().retainAll(resourceEntryOidCollection);
            final Set<String> uris = Functions.reduce( oidToUriMap.values(), new HashSet<String>(), new Functions.Binary<Set<String>,Set<String>,Set<String>>(){
                @Override
                public Set<String> call( final Set<String> aggregated, final Set<String> urisForOid ) {
                    aggregated.addAll( urisForOid );
                    return aggregated;
                }
            } );

            for ( String uri : uris ) {
                listenerRef.get().invalidateSchemaByUri( uri, true );
            }
        }
    }

}
