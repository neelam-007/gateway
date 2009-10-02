package com.l7tech.common.io;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * EntityResolver that maps system or public IDs to classpath resources.
 */
public class ResourceMapEntityResolver implements EntityResolver {

    //- PUBLIC

    /**
     * Create an entity resolver that maps the given IDs to classpath resources.
     *
     * <p>If no ClassLoader is given then the ClassLoader for this class is used
     * to find resources.</p>
     *
     * @param publicIdsToResources The Public ID to resource map (may be null)
     * @param systemIdsToResources The System ID to resource map (may be null)
     * @param loader The ClassLoader to use (may be null)
     */
    public ResourceMapEntityResolver( final Map<String,String> publicIdsToResources,
                                      final Map<String,String> systemIdsToResources,
                                      final ClassLoader loader ) {
        this( publicIdsToResources,
              systemIdsToResources,
              loader,
              false );
    }

    /**
     * Create an entity resolver that maps the given IDs to classpath resources.
     *
     * <p>If no ClassLoader is given then the ClassLoader for this class is used
     * to find resources.</p>
     *
     * @param publicIdsToResources The Public ID to resource map (may be null)
     * @param systemIdsToResources The System ID to resource map (may be null)
     * @param loader The ClassLoader to use (may be null)
     * @param allowMissingResource True to return null on missing resource (else will throw)
     */
    public ResourceMapEntityResolver( final Map<String,String> publicIdsToResources,
                                      final Map<String,String> systemIdsToResources,
                                      final ClassLoader loader,
                                      final boolean allowMissingResource ) {
        this.systemIdsToResources = new HashMap<String,String>();
        this.publicIdsToResources = new HashMap<String,String>();

        if ( publicIdsToResources != null ) {
            this.publicIdsToResources.putAll( publicIdsToResources) ;
        }

        if ( systemIdsToResources != null ) {
            this.systemIdsToResources.putAll( systemIdsToResources) ;
        }

        this.loader = loader != null ? loader : ResourceMapEntityResolver.class.getClassLoader();

        this.allowMissingResource = allowMissingResource;
    }

    @Override
    public InputSource resolveEntity( final String publicId, final String systemId ) throws SAXException, IOException {
        InputSource inputSource = null;
        String resource = publicIdsToResources.get( publicId );

        if ( resource == null ) {
            resource = systemIdsToResources.get( systemId );
        }

        if ( resource != null ) {
            inputSource = new InputSource();
            inputSource.setPublicId( publicId );
            inputSource.setSystemId( systemId );
            inputSource.setByteStream( loader.getResourceAsStream( resource ) );
            if ( inputSource.getByteStream() == null ) {
                throw new IOException("Entity resolved to missing resource '"+publicId+"', '"+systemId+"', resource is '"+resource+"'.");
            }
        } else if ( !allowMissingResource ) {
            throw new IOException("Entity not resolved '"+publicId+"', '"+systemId+"'.");
        }

        return inputSource;
    }

    //- PRIVATE

    private final Map<String,String> systemIdsToResources;
    private final Map<String,String> publicIdsToResources;
    private final ClassLoader loader;
    private final boolean allowMissingResource;
}
