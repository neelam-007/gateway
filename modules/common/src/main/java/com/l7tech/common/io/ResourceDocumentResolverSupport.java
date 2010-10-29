package com.l7tech.common.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * Support class for resource document resolvers.
 *
 * <p>Subclasses should override one or more of the resolveBy... methods.</p>
 */
public class ResourceDocumentResolverSupport implements ResourceDocumentResolver {

    //- PUBLIC
    
    @Override
    public ResourceDocument resolveByUri( final String uri ) throws IOException {
        return null;
    }

    @Override
    public ResourceDocument resolveByTargetNamespace( final String uri, final String targetNamespace ) throws IOException {
        return uri==null ? null : resolveByUri( uri );
    }

    @Override
    public ResourceDocument resolveByPublicId( final String uri, final String publicId ) throws IOException {
        return uri==null ? null : resolveByUri( uri );
    }

    //- PROTECTED

    protected URI asUri( final String uri ) throws IOException {
        try {
            return new URI(uri);
        } catch ( URISyntaxException e ) {
            throw new IOException( e );
        }
    }

    protected boolean isScheme( final URI uri,
                                final Collection<String> schemes ) {
        return uri.getScheme()!=null && schemes.contains( uri.getScheme().toLowerCase() );
    }

    protected ResourceDocument newResourceDocument( final URI uri ) {
        return new URIResourceDocument( uri, getResolver() );
    }

    protected ResourceDocument newResourceDocument( final String uri,
                                                    final String content ) throws IOException {
        try {
            return newResourceDocument( new URI(uri), content );
        } catch ( URISyntaxException e ) {
            throw new IOException( e );
        }
    }

    protected ResourceDocument newResourceDocument( final URI uri,
                                                    final String content ) {
        return new URIResourceDocument( uri, content, getResolver() );        
    }

    protected ResourceDocument newResourceDocument( final File file ) {
        return new FileResourceDocument( file );
    }

    protected ResourceDocumentResolver getResolver() {
        return this;
    }
}
