package com.l7tech.common.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

/**
 * A resource document resolver for local files.
 */
public class FileResourceDocumentResolver extends ResourceDocumentResolverSupport {

    //- PUBLIC

    @Override
    public ResourceDocument resolveByUri( final String uriString ) throws IOException {
        ResourceDocument resourceDocument = null;

        final URI uri = asUri(uriString);
        if ( isScheme( uri, schemes ) ) {
            try {
                final File file = new File( uri );
                if ( file.exists() ) { // don't check read permission / type here, we want to fail later for that
                    resourceDocument = new FileResourceDocument( file );
                }
            } catch ( IllegalArgumentException e ) {
                throw new IOException(e);
            }
        }

        return resourceDocument;
    }

    //- PRIVATE

    private static final Collection<String> schemes = Collections.singleton( "file");

}
