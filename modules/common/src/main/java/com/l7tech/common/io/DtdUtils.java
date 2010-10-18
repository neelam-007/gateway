package com.l7tech.common.io;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.SAXParsingCompleteException;
import com.l7tech.util.ValidationUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for working with DTDs.
 */
public class DtdUtils {

    //- PUBLIC

    /**
     * String of all characters that are valid in a (normalized) public identifier.
     */
    public static final String PUBLIC_ID_CHARACTERS = ValidationUtils.ALPHA_NUMERIC + " -'()+,./:=?;!*#@$_%";

    /**
     * Process references for the given DTD.
     *
     * @param dtdSystemId The system identifier for the DTD
     * @param dtdContent The content for the DTD
     * @param resolver The resolver to use to access dependencies
     * @throws IOException If an error occurs.
     * @throws SAXException If an error occurs.
     * @see Resolver for a convenience interface that extends the resolver interface
     */
    public static void processReferences( final String dtdSystemId,
                                          final String dtdContent,
                                          final Functions.BinaryThrows<Pair<String,String>,String,String,IOException> resolver ) throws IOException, SAXException {
        final Map<EntityKey,EntityValue> resolvedEntities = new HashMap<EntityKey,EntityValue>();
        resolvedEntities.put( new EntityKey(null, dtdSystemId), new EntityValue(dtdSystemId, dtdContent) );
        processReferencesRecursively( dtdSystemId, dtdContent, resolver, resolvedEntities );
    }

    /**
     * Perform as much syntactic validation as is possible.
     *
     * <p>This will not detect all possible errors in the DTD but provides
     * basic validation.</p>
     *
     * @param dtdSystemId The (absolute) system identifier for the DTD (required)
     * @param dtdContent The content for the DTD (required)
     * @throws SAXException If the DTD is invalid
     */
    public static void validate( final String dtdSystemId,
                                 final String dtdContent ) throws SAXException {
        final XMLReader reader = getXMLReaderForDTD( null );
        reader.setEntityResolver( new EntityResolver(){
            @Override
            public InputSource resolveEntity( final String resolvePublicId,
                                              final String resolveSystemId ) throws SAXException, IOException {
                if ( !dtdSystemId.equals( resolveSystemId )) {
                    // Validation of a document type definition that references external
                    // entities is beyond what we want to do here.
                    // We cannot continue validation without resolving the external
                    // entity so we abort.
                    throw new SAXParsingCompleteException();
                }

                final InputSource inputSource = new InputSource();
                inputSource.setSystemId( resolveSystemId );
                inputSource.setCharacterStream( new StringReader(dtdContent) );
                return inputSource;
            }
        } );

        try {
            reader.parse( getInputSourceForDtd(dtdSystemId) );
        } catch (SAXParsingCompleteException e) {
            // success, DTD parsed without error
        } catch ( IOException e ) {
            throw new SAXException( ExceptionUtils.getMessage(e), e); // this shouldn't occur
        }
    }

    /**
     * Normalize the given public identifier.
     *
     * <p>This will not validate the give public identifier.</p>
     *
     * <p>See http://www.w3.org/TR/REC-xml/#sec-external-ent</p>
     *
     * @param publicId The public identifier to normalize.
     * @return The normalized public identifier.
     */
    public static String normalizePublicId( final String publicId ) {
        String normalized = publicId.trim();

        normalized = normalized.replaceAll( "\\s{2,}", " " );

        return normalized;
    }

    public interface Resolver extends Functions.BinaryThrows<Pair<String,String>,String,String,IOException>{

        /**
         * Resolve a resource by system identifier.
         *
         * @param publicId The public identifier to resolve
         * @param systemId The system identifier to resolve
         * @return A pair of systemId/content for the resolved resource
         * @throws IOException If the identifier cannot be resolved
         */
        @Override
        Pair<String,String> call( String publicId, String systemId ) throws IOException;
    }

    //- PRIVATE

    private static void processReferencesRecursively(
            final String dtdSystemId,
            final String dtdContent,
            final Functions.BinaryThrows<Pair<String,String>,String,String,IOException> resolver,
            final Map<EntityKey,EntityValue> resolvedEntities ) throws IOException, SAXException {
        final Set<EntityKey> references = new LinkedHashSet<EntityKey>();
        final XMLReader reader = getXMLReaderForDTD( references );
        reader.setEntityResolver( new EntityResolver(){
            @Override
            public InputSource resolveEntity( final String resolvePublicId,
                                              final String resolveSystemId ) throws SAXException, IOException {
                final String entitySystemId;
                final String entityContent;

                if ( !dtdSystemId.equals( resolveSystemId )) {
                    final EntityKey resolverKey = new EntityKey(resolvePublicId, resolveSystemId);
                    final EntityValue entityValue = resolve( resolverKey, resolver, resolvedEntities );
                    entitySystemId = entityValue.getSystemId();
                    entityContent = entityValue.getContent();
                } else {
                    entitySystemId = dtdSystemId;
                    entityContent = dtdContent;
                }

                final InputSource inputSource = new InputSource();
                inputSource.setSystemId( entitySystemId );
                inputSource.setCharacterStream( new StringReader(entityContent) );
                return inputSource;
            }
        } );

        try {
            reader.parse( getInputSourceForDtd(dtdSystemId) );
        } catch (SAXParsingCompleteException e) {
            // success, DTD parsed without error
        }

        for ( final EntityKey reference : references ) {
            if ( !resolvedEntities.containsKey( reference ) ) {
                // The entity was not resolve due to parsing so resolve manually
                final EntityValue entityValue = resolve( reference, resolver, resolvedEntities );
                processReferencesRecursively(
                        entityValue.getSystemId(),
                        entityValue.getContent(),
                        resolver,
                        resolvedEntities );
            }
        }
    }

    private static EntityValue resolve( final EntityKey entityKey,
                                        final Functions.BinaryThrows<Pair<String, String>, String, String, IOException> resolver,
                                        final Map<EntityKey, EntityValue> resolvedEntities ) throws IOException {
        final Pair<String,String> resolvedEntity = resolver.call( entityKey.getPublicId(),  entityKey.getSystemId() );
        if ( resolvedEntity == null ) {
            throw new IOException("Unable to resolve entity '"+entityKey.getPublicId()+"'/'"+entityKey.getSystemId()+"'");
        }
        final EntityValue entityValue = new EntityValue(resolvedEntity.left, resolvedEntity.right);
        resolvedEntities.put( entityKey, entityValue );
        return entityValue;
    }

    private static XMLReader getXMLReaderForDTD( final Set<EntityKey> references ) throws SAXException {
        final XMLReader reader = XMLReaderFactory.createXMLReader();

        reader.setErrorHandler( new StrictErrorHandler() );
        final DefaultHandler2 handler = new DefaultHandler2(){
            @Override
            public void externalEntityDecl( final String name, final String publicId, final String systemId ) throws SAXException {
                if ( references != null ) {
                    references.add( new EntityKey(publicId, systemId) );
                }
            }

            @Override
            public void notationDecl( final String name, final String publicId, final String systemId ) throws SAXException {
                if ( references != null ) {
                    references.add( new EntityKey(publicId, systemId) );
                }
            }

            @Override
            public void endDTD() throws SAXException {
                throw new SAXParsingCompleteException();
            }
        };
        reader.setContentHandler( handler );

        try {
            reader.setProperty( "http://xml.org/sax/properties/lexical-handler", handler );
            reader.setProperty( "http://xml.org/sax/properties/declaration-handler", handler );

        } catch ( SAXNotSupportedException e ) {
            throw new RuntimeException(e);
        } catch ( SAXNotRecognizedException e ) {
            throw new RuntimeException(e);
        }

        return reader;
    }

    private static InputSource getInputSourceForDtd( final String systemId ) {
        // Parse a dummy document using the DTD
        final InputSource inputSource = new InputSource();
        inputSource.setCharacterStream( new StringReader(
                "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE test SYSTEM \""+systemId.trim()+"\">\n" +
                "<test/> ") );
        return inputSource;
    }

    /**
     * 
     */
    private static class EntityKey extends Pair<String,String> {
        private EntityKey( final String publicId, final String systemId ) {
            super( publicId, systemId );
        }

        public String getPublicId() {
            return left;
        }

        public String getSystemId() {
            return right;
        }
    }

    private static class EntityValue extends Pair<String,String> {
        private EntityValue( final String systemId, final String content ) {
            super( systemId, content );
        }

        public String getSystemId() {
            return left;
        }

        public String getContent() {
            return right;
        }
    }


    /**
     * Error handler that always throws
     */
    private static class StrictErrorHandler implements ErrorHandler {
        @Override
        public void warning( final SAXParseException exception ) throws SAXException {
            throw exception;
        }

        @Override
        public void error( final SAXParseException exception ) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError( final SAXParseException exception ) throws SAXException {
            throw exception;
        }
    }
}
