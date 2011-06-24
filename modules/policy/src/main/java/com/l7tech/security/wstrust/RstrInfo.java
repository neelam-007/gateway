package com.l7tech.security.wstrust;

import com.l7tech.security.xml.KeyInfoElement;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.UnexpectedKeyInfoException;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.TooManyChildElementsException;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Holds info parsed out of a WS-Trust RequestSecurityTokenResponse element.
*/
public class RstrInfo {
    private static final Logger logger = Logger.getLogger(RstrInfo.class.getName());

    private final Element token;
    private final String wsTrustNamespace;
    private final String tokenType;
    private final int keySize;
    private final long created;
    private final String createdText;
    private final long expires;
    private final String expiresText;
    private final String keyComputationAlgorithm;
    private final BinaryValue entropy;
    private final BinaryValue secret;

    public RstrInfo( final String wsTrustNamespace,
                     final String tokenType,
                     final Element token,
                     final String keySize,
                     final String createdText,
                     final String expiresText,
                     final String keyComputationAlgorithm,
                     final BinaryValue entropy,
                     final BinaryValue secret ) throws InvalidDocumentFormatException {
        this.token = token;
        this.wsTrustNamespace = wsTrustNamespace;
        this.tokenType = tokenType;
        this.keySize = integer(keySize, "KeySize");
        this.created = date(createdText, "Lifetime/Created");
        this.createdText = createdText;
        this.expires = date(expiresText, "Lifetime/Expires");
        this.expiresText = expiresText;
        this.keyComputationAlgorithm = keyComputationAlgorithm;
        this.entropy = entropy;
        this.secret = secret;
    }

    private long date( final String dateText,
                       final String description ) throws InvalidDocumentFormatException {
        try {
            return dateText==null ? 0 : ISO8601Date.parse( dateText.trim() ).getTime();
        } catch ( ParseException e ) {
            throw new InvalidDocumentFormatException( "Invalid date for " + description + ": " + ExceptionUtils.getMessage(e));
        }
    }

    private int integer( final String keySizeText,
                         final String description ) throws InvalidDocumentFormatException {
        try {
            return keySizeText == null ? 0 : Integer.parseInt( keySizeText );
        } catch ( NumberFormatException nfe ) {
            throw new InvalidDocumentFormatException( "Invalid value for " + description);
        }
    }

    /**
     * Get the WS-Trust namespace used in the response.
     *
     * @return The namespace, never null.
     */
    public String getWsTrustNamespace() {
        return wsTrustNamespace;
    }

    /**
     * Get the token type URI.
     *
     * @return The URI or null.
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Get the returned token.
     *
     * @return The token element, never null.
     */
    public Element getToken() {
        return token;
    }

    /**
     * Was a key size present in the response?
     *
     * @return True if a key size was present.
     */
    public boolean isKeySizePresent() {
        return keySize > 0;
    }

    /**
     * Get the key size.
     *
     * @return The key size, zero if not present in the response.
     */
    public int getKeySize() {
        return keySize;
    }

    /**
     * Was a lifetime / created element present in the response.
     *
     * @return True if present.
     */
    public boolean isCreatedPresent() {
        return createdText != null;
    }

    /**
     * Get the created date from the response
     *
     * @return The date or zero if not present.
     */
    public long getCreated() {
        return created;
    }

    /**
     * Get the lifetime / created text from the response.
     *
     * @return The created text or null.
     */
    public String getCreatedText() {
        return createdText;
    }

    /**
     * Was a lifetime / expires element present in the response.
     *
     * @return True if present.
     */
    public boolean isExpiresPresent() {
        return expiresText != null;
    }

    /**
     * Get the expires date from the response
     *
     * @return The date or zero if not present.
     */
    public long getExpires() {
        return expires;
    }

    /**
     * Get the lifetime / expires text from the response.
     *
     * @return The expiry text or null.
     */
    public String getExpiresText() {
        return expiresText;
    }

    /**
     * Get the key computation algorithm from the proof token (if any)
     *
     * @return The key computation algorithm or null.
     */
    public String getKeyComputationAlgorithm() {
        return keyComputationAlgorithm;
    }

    /**
     * Get the entropy from the response.
     *
     * @return The entropy or null.
     */
    public byte[] getEntropy() {
        return entropy==null ? null : entropy.getValue();
    }

    /**
     * Get the entropy from the response as Base64 text.
     *
     * @return The entropy or null.
     */
    public String getEntropyBase64() {
        return entropy==null ? null : entropy.getValueBase64();
    }

    /**
     * Get the secret from the response.
     *
     * @return The secret or null.
     */
    public byte[] getSecret() {
        return secret==null ? null : secret.getValue();
    }

    /**
     * Get the secret from the response as Base64 text.
     *
     * @return The secret or null.
     */
    public String getSecretBase64() {
        return secret==null ? null : secret.getValueBase64();
    }

    @Override
    public String toString() {
        return "RstrInfo{" +
                "tokenType='" + tokenType + '\'' +
                ", keySize='" + keySize + '\'' +
                ", keyComputationAlgorithm='" + keyComputationAlgorithm + '\'' +
                ", created='" + createdText + '\'' +
                ", expires='" + expiresText + '\'' +
                '}';
    }

    /**
     * Parse the given RSTR collection.
     *
     * @param rstrCollectionElement The element to parse.
     * @param securityTokenResolver The resolver to use for decrypting keys (optional)
     * @return the RstrInfo
     * @throws InvalidDocumentFormatException If an error occurs.
     */
    public static RstrInfo parseRstrCollectionElement( final Element rstrCollectionElement,
                                                       final SecurityTokenResolver securityTokenResolver ) throws InvalidDocumentFormatException {
        verifyElement( rstrCollectionElement, SoapConstants.WST_NAMESPACE_ARRAY, "RequestSecurityTokenResponseCollection" );
        return parse( rstrCollectionElement, securityTokenResolver );
    }

    /**
     * Parse the given RSTR.
     *
     * @param rstrElement The element to parse.
     * @param securityTokenResolver The resolver to use for decrypting keys (optional)
     * @return the RstrInfo
     * @throws InvalidDocumentFormatException If an error occurs.
     */
    public static RstrInfo parseRstrElement( final Element rstrElement,
                                             final SecurityTokenResolver securityTokenResolver ) throws InvalidDocumentFormatException {
        verifyElement( rstrElement, SoapConstants.WST_NAMESPACE_ARRAY, "RequestSecurityTokenResponse" );
        return parse( rstrElement, securityTokenResolver );
    }

    /**
     * Parse the given response which may be an RSTR or an RSTR collection.
     *
     * @param responseElement The element to parse.
     * @param securityTokenResolver The resolver to use for decrypting keys (optional)
     * @return the RstrInfo
     * @throws InvalidDocumentFormatException If an error occurs.
     */
    public static RstrInfo parse( final Element responseElement,
                                  final SecurityTokenResolver securityTokenResolver ) throws InvalidDocumentFormatException {
        verifyElement( responseElement, SoapConstants.WST_NAMESPACE_ARRAY );

        final Element rstrElement = getRstrElement( responseElement );


        final String wsTrustNamespace = rstrElement.getNamespaceURI();

        final String tokenType;
        final String keySize;
        final Element tokenElement;
        String keyComputationAlgorithm = null;
        String lifetimeCreated = null;
        String lifetimeExpires = null;
        BinaryValue serverEntropy = null;
        BinaryValue serverKey = null;
        try {
            tokenType = getChildText( rstrElement, wsTrustNamespace, "TokenType" );
            keySize = getChildText( rstrElement, wsTrustNamespace, "KeySize" );

            final Element requestedTokenElement = DomUtils.findExactlyOneChildElementByName( rstrElement, wsTrustNamespace, "RequestedSecurityToken" );
            tokenElement = DomUtils.findExactlyOneChildElement( requestedTokenElement );

            final Element lifetime = DomUtils.findOnlyOneChildElementByName( rstrElement, wsTrustNamespace, "Lifetime" );
            if ( lifetime != null ) {
                lifetimeCreated = getChildText( lifetime, SoapConstants.WSU_NAMESPACE, "Created" );
                lifetimeExpires = getChildText( lifetime, SoapConstants.WSU_NAMESPACE, "Expires" );
            }

            final Element entropy = DomUtils.findOnlyOneChildElementByName( rstrElement, wsTrustNamespace, "Entropy" );
            if ( entropy != null ) {
                serverEntropy = getBinarySecret( securityTokenResolver, entropy, wsTrustNamespace );
            }

            final Element proof = DomUtils.findOnlyOneChildElementByName( rstrElement, wsTrustNamespace, "RequestedProofToken" );
            if ( proof != null ) {
                serverKey = getBinarySecret( securityTokenResolver, proof, wsTrustNamespace );
                keyComputationAlgorithm = getChildText( proof, wsTrustNamespace, "ComputedKey" );                
            }
        } catch ( TooManyChildElementsException e ) {
            throw new InvalidDocumentFormatException( ExceptionUtils.getMessage(e), e );
        } catch ( MissingRequiredElementException e ) {
            throw new InvalidDocumentFormatException( ExceptionUtils.getMessage(e), e );
        } catch ( GeneralSecurityException e ) {
            throw new InvalidDocumentFormatException( ExceptionUtils.getMessage(e), e );
        } catch ( UnexpectedKeyInfoException e ) {
            throw new InvalidDocumentFormatException( ExceptionUtils.getMessage(e), e );
        }

        final RstrInfo rstrInfo = new RstrInfo(
                wsTrustNamespace,
                tokenType,
                tokenElement,
                keySize,
                lifetimeCreated,
                lifetimeExpires,
                keyComputationAlgorithm,
                serverEntropy,
                serverKey );

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, rstrInfo.toString());

        return rstrInfo;
    }

    //- PRIVATE

    private static Element getRstrElement( final Element responseElement ) throws InvalidDocumentFormatException {
        final Element rstrElem;

        if ( "RequestSecurityTokenResponseCollection".equals(responseElement.getLocalName()) ) {
            final List<Element> requestSecurityTokenResponses = DomUtils.findChildElementsByName( responseElement, responseElement.getNamespaceURI(), "RequestSecurityTokenResponse" );

            if ( requestSecurityTokenResponses.size() != 1 ) {
                throw new InvalidDocumentFormatException( "Unexpected number of security token responses: " + requestSecurityTokenResponses.size() );
            } else {
                rstrElem = requestSecurityTokenResponses.get( 0 );
            }
        } else if ( "RequestSecurityTokenResponse".equals(responseElement.getLocalName()) ) {
            rstrElem = responseElement;
        } else {
            throw new InvalidDocumentFormatException( "Expected wst:RequestSecurityTokenResponse or wst:RequestSecurityTokenResponseCollection  but received " + qName(responseElement) );
        }

        return rstrElem;
    }

    private static BinaryValue getBinarySecret( final SecurityTokenResolver securityTokenResolver,
                                                final Element binarySecretHolder,
                                                final String wsTrustNamespace ) throws InvalidDocumentFormatException, UnexpectedKeyInfoException, GeneralSecurityException {
        final String binarySecretBase64 = getChildText( binarySecretHolder, wsTrustNamespace, "BinarySecret" );

        final Element encryptedKeyEl = DomUtils.findOnlyOneChildElementByName( binarySecretHolder, SoapConstants.XMLENC_NS, "EncryptedKey" );
        final byte[] encryptedKeySecret;

        if ( encryptedKeyEl != null ) {
            if ( binarySecretBase64 != null ) {
                throw new InvalidDocumentFormatException( "Only one of BinarySecret or EncryptedKey is permitted." );
            }
            if ( securityTokenResolver == null ) {
                throw new InvalidDocumentFormatException( "No resolver, cannot process EncryptedKey." );
            }

            final SignerInfo signerInfo = KeyInfoElement.getTargetPrivateKeyForEncryptedType(encryptedKeyEl, securityTokenResolver);

            // verify that the algo is supported
            XencUtil.checkEncryptionMethod(encryptedKeyEl);

            encryptedKeySecret = XencUtil.decryptKey(encryptedKeyEl, signerInfo.getPrivate());
        } else {
            encryptedKeySecret = null;
        }

        return binarySecretBase64!=null || encryptedKeySecret!=null ? new BinaryValue( binarySecretBase64, encryptedKeySecret ) : null;
    }

    private static String getChildText( final Element parent,
                                        final String namespace,
                                        final String localName ) throws TooManyChildElementsException {
        String text = null;

        final Element childElement = DomUtils.findOnlyOneChildElementByName( parent, namespace, localName );
        if ( childElement != null ) {
            text = DomUtils.getTextValue( childElement );
        }

        return text;
    }

    private static void verifyElement( final Element element,
                                       final String[] namespaces ) throws InvalidDocumentFormatException {
        verifyElement( element, namespaces, null );
    }

    private static void verifyElement( final Element element,
                                       final String[] namespaces,
                                       final String localName ) throws InvalidDocumentFormatException {
        if ( element == null ) throw new InvalidDocumentFormatException( localName + " element is required" );

        if ( ( localName != null && !localName.equals( element.getLocalName() ) ) ||
             !ArrayUtils.contains( namespaces, element.getNamespaceURI() )) {

            if ( localName == null ) {
                throw new InvalidDocumentFormatException( "Expected a ws-trust element but received " + qName(element) );
            } else {
                throw new InvalidDocumentFormatException( "Expected a wst:" + localName + " but received " + qName(element) );
            }
        }
    }

    private static String qName( final Element element ) {
        return new QName( element.getNamespaceURI(), element.getLocalName() ).toString();
    }

    private static final class BinaryValue {
        private String valueBase64;
        private byte[] value;

        private BinaryValue( final String valueBase64,
                             final byte[] value ) {
            this.valueBase64 = valueBase64;
            this.value = value;
        }

        public String getValueBase64() {
            if ( valueBase64 == null ) {
                valueBase64 = HexUtils.encodeBase64( value, true );
            }
            return valueBase64;
        }

        public byte[] getValue() {
            if ( value == null ) {
                value = HexUtils.decodeBase64( valueBase64, true );
            }
            return value;
        }
    }
}
