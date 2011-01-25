package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.server.audit.Auditor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A resolver that optionally filters out services without paths.
 */
public class RequirePathResolver extends ServiceResolver<Void> {

    //- PUBLIC

    public RequirePathResolver( final Auditor.AuditorFactory auditorFactory ) {
        super( auditorFactory );
    }

    @Override
    public void configure( final ResolutionConfiguration resolutionConfiguration ) {
        super.configure( resolutionConfiguration );
        requirePath.set( resolutionConfiguration.isPathRequired() );
    }

    @Override
    public Result resolve( final Map<String, Object> parameters,
                           final Collection<PublishedService> serviceSubset ) throws ServiceResolutionException {
        final Boolean applicable = (Boolean) parameters.get( PROP_APPLICABLE );
        if ( !requirePath.get() || (applicable!=null && !applicable) || !parameters.containsKey( PROP_VALUE )) return Result.NOT_APPLICABLE;

        final Set<PublishedService> output = new HashSet<PublishedService>();
        for ( final PublishedService service : serviceSubset ) {
            if ( service.getRoutingUri() != null ) {
                output.add( service );
            }
        }

        return new Result( output );
    }

    @Override
    public void populateResolutionParameters( final Message request,
                                              final Map<String, Object> parameters ) throws ServiceResolutionException {
        if ( !appliesToMessage(request) ) {
            parameters.put( PROP_APPLICABLE, false );
            return; // don't process request value
        }

        parameters.put( PROP_VALUE, null );
    }

    @Override
    public Collection<Map<String, Object>> generateResolutionParameters( final PublishedService service,
                                                                         final Collection<Map<String, Object>> parameterCollection ) throws ServiceResolutionException {
        return parameterCollection;
    }

    @Override
    public boolean usesMessageContent() {
        return false;
    }

    //- PROTECTED

    @Override
    protected List<Void> buildTargetValues( final PublishedService service ) throws ServiceResolutionException {
        return Collections.emptyList();
    }

    @Override
    protected void updateServiceValues( final PublishedService service, final List<Void> targetValues ) {
    }

    //- PRIVATE

    private final AtomicBoolean requirePath = new AtomicBoolean(false);

    private boolean appliesToMessage( final Message message ) {
        return (message.getKnob(HttpRequestKnob.class) != null) || (message.getKnob(FtpRequestKnob.class) != null);
    }
}
