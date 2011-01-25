package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Exception used when a service conflicts with an existing services resolution parameters.
 */
public class NonUniqueServiceResolutionException extends ServiceResolutionException {

    //- PUBLIC

    public NonUniqueServiceResolutionException( final Collection<Pair<Map<String, Object>, PublishedService>> conflictingParameterCollection ) {
        super( "Resolution parameters conflict with one or more services" );
        this.conflictingParameterCollection = conflictingParameterCollection;
    }

    /**
     * Get the set of identifiers for conflicting services.
     *
     * @return The set of service ids (never null)
     */
    public Set<Long> getConflictingServices() {
        return Functions.reduce( conflictingParameterCollection, new HashSet<Long>(), new Functions.Binary<Set<Long>,Set<Long>,Pair<Map<String, Object>, PublishedService>>(){
            @Override
            public Set<Long> call( final Set<Long> serviceOids,
                                   final Pair<Map<String, Object>, PublishedService> servicePair ) {
                serviceOids.add( servicePair.right.getOid() );
                return serviceOids;
            }
        } );
    }

    /**
     * Get the name of a service by identifier.
     *
     * @param serviceOid The service id.
     * @param displayName True to return the display name.
     * @return The name or display name
     */
    public String getServiceName( final long serviceOid,
                                  final boolean displayName ) {
        return Functions.reduce( conflictingParameterCollection, null, new Functions.Binary<String,String,Pair<Map<String, Object>, PublishedService>>(){
            @Override
            public String call( final String value,
                                final Pair<Map<String, Object>, PublishedService> servicePair ) {
                return value != null ?
                        value :
                        servicePair.right.getOid()==serviceOid ?
                                (displayName ? servicePair.right.displayName() : servicePair.right.getName()) :
                                null;
            }
        } );
    }

    /**
     * Get the conflicting parameters for the service.
     *
     * @param serviceOid The service info to access.
     * @return A list of path/namespace/soapAction triples.
     */
    public Set<Triple<String,String,String>> getParameters( final long serviceOid ) {
        return Functions.reduce( conflictingParameterCollection, new HashSet<Triple<String,String,String>>(),
                new Functions.Binary<Set<Triple<String,String,String>>,Set<Triple<String,String,String>>,Pair<Map<String, Object>, PublishedService>>(){
            @Override
            public Set<Triple<String,String,String>> call( final Set<Triple<String,String,String>> values,
                                                           final Pair<Map<String, Object>, PublishedService> servicePair ) {
                if ( servicePair.right.getOid() == serviceOid ) {
                    values.add( new Triple<String,String,String>(
                        servicePair.right.getRoutingUri(),
                        (String)servicePair.left.get( UrnResolver.class.getName() + ServiceResolver.SUFFIX_VALUE ),
                        (String)servicePair.left.get( SoapActionResolver.class.getName() + ServiceResolver.SUFFIX_VALUE )
                    ) );
                }
                return values;
            }
        } );
    }

    //- PRIVATE

    private final Collection<Pair<Map<String, Object>, PublishedService>> conflictingParameterCollection;

}
