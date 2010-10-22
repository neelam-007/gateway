package com.l7tech.common.io;

import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.net.URI;

/**
 * A resource document resolved from a URI.
 */
public class URIResourceDocument implements ResourceDocument {

    //- PUBLIC

    public URIResourceDocument( final URI uri,
                                final ResourceDocumentResolver resolver ) {
        this.uri = uri;
        this.resolver = resolver;
    }

    public URIResourceDocument( final URI uri,
                                final String content,
                                final ResourceDocumentResolver resolver) {
        this.uri = uri;
        this.content = content;
        this.resolver = resolver;
    }

    @Override
    public boolean available() {
        return content != null;
    }

    @Override
    public boolean exists() throws IOException {
        getContent();
        return true;
    }

    @Override
    public ResourceDocument relative( final String path,
                                      final ResourceDocumentResolver resourceDocumentResolver ) throws IOException {
        final ResourceDocumentResolver resolver = resourceDocumentResolver != null ?
                resourceDocumentResolver :
                this. resolver;

        if ( resolver == null ) {
            throw new IOException( "Unable to resolve path '" + path + "', no resolver available.");
        }

        try {
            return new URIResourceDocument(uri.resolve( path ), resolver);
        } catch ( IllegalArgumentException e ) {
            throw new IOException( "Unable to resolve path '" + path + "', due to : " + ExceptionUtils.getMessage( e ));
        }
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public long getLastModified() {
        return -1;
    }

    @Override
    public String getContent() throws IOException {
        return doGetContent();
    }

    //- PRIVATE

    private final URI uri;
    private final ResourceDocumentResolver resolver;
    private String content;

    private String doGetContent() throws IOException {
        String content = this.content;

        if ( content == null ) {
            if ( resolver != null ) {
                ResourceDocument contentDoc = resolver.resolveByUri( uri.toString() );
                if ( contentDoc != null ) {
                    this.content = content = contentDoc.getContent();
                } else {
                    throw new IOException("Cannot resolve uri '"+uri+"'");
                }
            } else {
                throw new IOException("Cannot resolve uri '"+uri+"'");
            }
        }

        return content;
    }
}
