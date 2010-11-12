package com.l7tech.common.io;

import com.l7tech.util.ExceptionUtils;
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
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                                          final Resolver resolver ) throws IOException, SAXException {
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

    /**
     * Update the external identifier system identifier if present.
     *
     * <p>If the given document does not contain a system identifier then one
     * will not be added.</p>
     *
     * <p>If the given system identifier contains both single and double quotes
     * then it cannot be used and the original document is returned.</p>
     *
     * @param document The XML document to update (must not be null)
     * @param systemId The system identifier to use (must not be null)
     * @return The (possibly updated) document (never null)
     */
    public static String updateExternalSystemId( final String document,
                                                 final String systemId ) {
        String updatedDocument = document;

        final boolean hasQuote = systemId.contains( "'" );
        final boolean hasDoubleQuote = systemId.contains( "\"" );

        if ( !hasQuote || !hasDoubleQuote ) { // if the system id contains both quote types it can't be used
            final Matcher matcher = systemIdSearchPattern.matcher( document );
            if ( matcher.find() ) {
                String quote;
                if ( hasQuote ) {
                    quote = "\"";
                } else if ( hasDoubleQuote ) {
                    quote = "'";
                } else {
                    quote = matcher.group( 0 ).substring( 0, 1 );
                }

                final String updatedSystemId = quote + systemId + quote;
                if ( !updatedSystemId.equals( matcher.group( 0 ) ) ) {
                    updatedDocument = document.substring( 0, matcher.start()) + updatedSystemId + document.substring( matcher.end() );
                }
            }
        }

        return updatedDocument;
    }

    public interface Resolver {

        /**
         * Resolve a resource by system identifier.
         *
         * @param publicId The public identifier to resolve
         * @param baseUri The baseUri of the parent document (may be null if system id is absolute)
         * @param systemId The system identifier to resolve
         * @return A pair of systemId/content for the resolved resource
         * @throws IOException If the identifier cannot be resolved
         */
        Pair<String,String> call( String publicId, String baseUri, String systemId ) throws IOException;
    }

    //- PRIVATE

    /**
     * Regular expression for matching the system identifier in a DOCTYPE declaration.
     *
     * '<!DOCTYPE' S Name (S ExternalID)? S? ('[' intSubset ']' S?)? '>'
     * S               ::=   (#x20 | #x9 | #xD | #xA)+
     * ExternalID      ::=   'SYSTEM' S SystemLiteral | 'PUBLIC' S PubidLiteral S SystemLiteral
     * SystemLiteral   ::=   ('"' [^"]* '"') | ("'" [^']* "'")
     * PubidLiteral    ::=   '"' PubidChar* '"' | "'" (PubidChar - "'")* "'"
     * PubidChar       ::=   #x20 | #xD | #xA | [a-zA-Z0-9] | [-'()+,./:=?;!*#@$_%]
     */
    private static final Pattern systemIdSearchPattern = Pattern.compile( "(?s)(?<=<!DOCTYPE\\s{1,128}[^\\s]{1,1024}\\s{1,128}(?:SYSTEM\\s{1,128}|PUBLIC\\s{1,128}(?:\"[\\sa-zA-Z0-9\\-'()+,\\./:=\\?;!*#@$_%]{0,1024}\"|'[\\sa-zA-Z0-9\\-()+,\\./:=\\?;!*#@$_%]{0,1024}')\\s{1,128}))(?:'[^']+'|\"[^\"]+\")" );

    private static void processReferencesRecursively(
            final String dtdSystemId,
            final String dtdContent,
            final Resolver resolver,
            final Map<EntityKey,EntityValue> resolvedEntities ) throws IOException, SAXException {
        final Set<EntityKey> references = new LinkedHashSet<EntityKey>();
        final XMLReader reader = getXMLReaderForDTD( references );
        reader.setEntityResolver( new EntityResolver2(){
            @Override
            public InputSource getExternalSubset( final String name, final String baseURI ) throws SAXException, IOException {
                return null;
            }

            @Override
            public InputSource resolveEntity( final String name, final String publicId, final String baseUri, final String systemId ) throws SAXException, IOException {
                final String entitySystemId;
                final String entityContent;

                if ( !dtdSystemId.equals( systemId )) {
                    final EntityValue entityValue = resolve( publicId, baseUri, systemId, resolver );
                    final EntityKey resolverKey = new EntityKey(publicId, entityValue.getSystemId());
                    resolvedEntities.put( resolverKey, entityValue );
                    entitySystemId = entityValue.getSystemId();
                    entityContent = entityValue.getContent();
                } else {
                    entitySystemId = dtdSystemId;
                    entityContent = dtdContent;
                }

                final InputSource inputSource = new InputSource( entitySystemId );
                inputSource.setCharacterStream( new StringReader(entityContent) );

                return inputSource;
            }

            @Override
            public InputSource resolveEntity( final String publicId,
                                              final String systemId ) throws SAXException, IOException {
                return resolveEntity( null, publicId, null, systemId );
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
                final EntityValue entityValue = resolve( reference.getPublicId(), reference.getSystemId(), reference.getSystemId(), resolver );
                resolvedEntities.put( reference, entityValue );
                processReferencesRecursively(
                        entityValue.getSystemId(),
                        entityValue.getContent(),
                        resolver,
                        resolvedEntities );
            }
        }
    }

    private static EntityValue resolve( final String publicId,
                                        final String baseUri,
                                        final String systemId,
                                        final Resolver resolver ) throws IOException {
        final Pair<String,String> resolvedEntity = resolver.call( publicId,  baseUri, systemId );
        if ( resolvedEntity == null ) {
            throw new IOException("Unable to resolve entity '"+publicId+"'/'"+systemId+"'");
        }
        return new EntityValue(resolvedEntity.left, resolvedEntity.right);
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
