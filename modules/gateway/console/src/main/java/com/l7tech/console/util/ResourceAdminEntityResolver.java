package com.l7tech.console.util;

import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.CausedIOException;
import org.xml.sax.InputSource;
import org.xml.sax.ext.EntityResolver2;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * Entity resolver that uses the resource admin API.
 */
public class ResourceAdminEntityResolver implements EntityResolver2 {

    //- PUBLIC

    public ResourceAdminEntityResolver( final ResourceAdmin resourceAdmin ) {
        this( resourceAdmin, false );
    }

    public ResourceAdminEntityResolver( final ResourceAdmin resourceAdmin,
                                        final boolean allowRemote ) {
        this.resourceAdmin = resourceAdmin;
        this.allowRemote = allowRemote;
    }

    @Override
    public InputSource getExternalSubset( final String name, final String baseURI ) throws IOException {
        return null;
    }

    @Override
    public InputSource resolveEntity( final String name, final String publicId, final String baseURI, final String systemId ) throws IOException {
        return doResolve( publicId, baseURI, systemId );
    }

    @Override
    public InputSource resolveEntity( final String publicId,
                                      final String systemId ) throws IOException {
        return doResolve( publicId, systemId, systemId );
    }

    //- PRIVATE

    private final ResourceAdmin resourceAdmin;
    private final boolean allowRemote;

    private InputSource doResolve( final String publicId, final String baseURI, final String systemId ) throws IOException {
        InputSource inputSource = null;

        try {
            ResourceEntry entry = findByPublicIdentifier( publicId );

            if ( entry == null ) {
                // try by unresolved URI
                entry = resourceAdmin.findResourceEntryByUriAndType( systemId, ResourceType.DTD );
            }

            String absoluteUri = null;
            if ( entry == null && baseURI != null ) {
                // try by resolved URI
                final URI uri = new URI( systemId );
                if ( !uri.isAbsolute() ) {
                    final URI base = new URI(baseURI);
                    if ( base.isAbsolute() ) {
                        absoluteUri = base.resolve( uri ).toString();
                        entry = resourceAdmin.findResourceEntryByUriAndType( absoluteUri, ResourceType.DTD );
                    }
                } else {
                    absoluteUri = systemId;
                }
            } else {
                absoluteUri = systemId;
            }

            if ( entry != null ) {
                inputSource = new InputSource();
                inputSource.setSystemId( entry.getUri() );
                inputSource.setCharacterStream( new StringReader( entry.getContent() ) );
            } else if ( allowRemote && absoluteUri != null && (absoluteUri.toLowerCase().startsWith("http:") || absoluteUri.toLowerCase().startsWith("https:")) ) {
                final String content = resourceAdmin.resolveResource( absoluteUri );
                inputSource = new InputSource();
                inputSource.setSystemId( absoluteUri );
                inputSource.setCharacterStream( new StringReader( content ) );
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

    private ResourceEntry findByPublicIdentifier( final String publicId ) throws FindException {
        ResourceEntry resourceEntry = null;

        if ( publicId != null ) {
            final Collection<ResourceEntryHeader> headers = resourceAdmin.findResourceHeadersByPublicIdentifier( publicId );
            if ( headers.size() == 1 ) {
                resourceEntry = resourceAdmin.findResourceEntryByPrimaryKey( headers.iterator().next().getGoid() );
            }
        }

        return resourceEntry;
    }
}
