package com.l7tech.common.io;

import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

/**
 * Resource document backed by a file.
 */
public class FileResourceDocument implements ResourceDocument {

    //- PUBLIC

    public FileResourceDocument( final File file ) {
        this.file = file;
    }

    @Override
    public ResourceDocument relative( final String path,
                                      final ResourceDocumentResolver resolver ) throws IOException {
        final ResourceDocument resolved;

        try {
            final URI resolvedUri = file.toURI().resolve(path);
            if ( resolver == null ) {
                resolved = new FileResourceDocument( new File( resolvedUri ) );
            } else {
                try {
                    resolved = resolver.resolveByUri( resolvedUri.toString() );
                    if ( resolved == null ) {
                        throw new IOException("Resource not found for URI '"+resolvedUri+"'");
                    }
                } catch ( IllegalArgumentException e ) {
                    throw new IOException( "Unable to resolve path '" + path + "', due to : " + ExceptionUtils.getMessage( e ));
                }
            }
        } catch ( IllegalArgumentException e ) {
            throw new IOException( "Unable to resolve path '" + path + "', due to : " + ExceptionUtils.getMessage( e ));
        }

        return resolved;
    }

    @Override
    public boolean available() {
        return content != null;
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public URI getUri() {
        return file.toURI();
    }

    @Override
    public long getLastModified() {
        return file.lastModified();
    }

    @Override
    public String getContent() throws IOException {
        String content = this.content;

        if ( content == null ) {
            final byte[] data = IOUtils.slurpFile( file );
            final String charset = XmlUtil.getEncoding( data );
            try {
                content = new String( data, charset );
            } catch ( UnsupportedEncodingException e ) {
                throw new CausedIOException(e);                
            }
            this.content = content;
        }
        return content;
    }

    //- PRIVATE

    private final File file;
    private String content;
}
