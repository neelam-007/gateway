package com.l7tech.server.security.cert;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.HexUtils;
import com.l7tech.util.TimeUnit;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.ocsp.CertID;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.ocsp.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OCSP client implementation using Bouncy Castle.
 *
 * @author Steve Jones
 * @see CertificateValidationResult
 */
public class OCSPClient {

    //- PUBLIC

    /**
     * Authority Information Access / OCSP access method OID
     */
    public static final String OID_AIA_OCSP_URL = "1.3.6.1.5.5.7.48.1";

    /**
     * Create an OCSP client.
     *
     * @param httpClient The HTTP client to use for making requests (required)
     * @param url The url to send requests to (required)
     * @param issuer The certificate of the CA (required)
     * @param certificateAuthorizer The authorizer for response signing
     */
    public OCSPClient(final GenericHttpClient httpClient,
                      final String url,
                      final X509Certificate issuer,
                      final OCSPCertificateAuthorizer certificateAuthorizer) {
        if (httpClient == null) throw new IllegalArgumentException("httpClient is required");
        if (url == null) throw new IllegalArgumentException("url is required");
        if (issuer == null) throw new IllegalArgumentException("issuer is required");

        this.httpClient = httpClient;
        this.url = url;
        this.issuerCertificate = issuer;
        this.certificateAuthorizer = certificateAuthorizer;
    }

    /**
     * Check the given certificate for revocation.
     *
     * <p>If this client has a specified url then that url will be used. If
     * not then the URL from the certificate will be used, if it matches the
     * pattern provided.</p>
     *
     * @param certificate The certificate to check
     * @param nonce True to include a nonce
     * @param signed True to require a signed response
     * @return The status for the certificate
     * @throws OCSPException if an error occurs
     */
    public OCSPStatus getRevocationStatus(final X509Certificate certificate,
                                          final boolean nonce,
                                          final boolean signed) throws OCSPClientException {
        OCSPStatus status;

        status = doRevocationCheck(certificate, nonce, signed);

        return status;
    }

    /**
     * Check if the given certificate is allowed to sign OCSP responses.
     *
     * <p>This checks if the issuer allows the given certificate to sign OCSP responses
     * on its behalf.</p>
     *
     * @param certificate The certificate to check.
     * @return true if permitted and certificate is valid.
     * @throws OCSPClientException if the certificate is invalid
     */
    public boolean isPermittedByIssuer(final X509Certificate certificate) throws OCSPClientException {
        boolean permitted = false;

        // Is this cert created by the issuer for OCSP responses?
        try {
            List<String> extendedUsages = certificate.getExtendedKeyUsage();
            if ( extendedUsages!=null && extendedUsages.contains(OID_EXT_KEY_USE_OCSPSIGN) ) {
                certificate.checkValidity();
                CertVerifier.cachedVerify(certificate, issuerCertificate);
                permitted = true;            
            }
        } catch (GeneralSecurityException gse) {
            throw new OCSPClientException("Error verifying OCSP response signer certificate.", gse);
        }

        return permitted;
    }

    /**
     * Should a revocation check be performed for the given certificate?
     *
     * <p>This is only applicable for certs that are {@link #isPermittedByIssuer permitted by issuer}.</p>
     *
     * @param certificate The certificate to check.
     * @return true if a revocation check should be performed.
     * @throws OCSPClientException if the certificate is invalid
     */
    public boolean shouldCheckRevocation(final X509Certificate certificate) throws OCSPClientException {
        // If nocheck is set on the cert then we don't need to check revocation
        final Set<String> extensions = certificate.getNonCriticalExtensionOIDs();
        if (extensions != null && extensions.contains(OID_EXT_KEY_USE_OCSP_NOCHECK)) {
            logger.log(Level.FINE, "OCSP signing certificate {0} has the id-pkix-ocsp-nocheck extension", certificate.getSubjectDN().getName());
            return false;
        }

        return true;
    }

    /**
     * Interface implemented to authorize certificates that sign OCSP responses.
     */
    public static interface OCSPCertificateAuthorizer {
        /**
         * Locate an authorized signer from the certificates listed in the OCSP response.
         *
         * @param client The OCSPClient making the check (not null)
         * @param certificates The certificates to check (may be empty but not null)
         * @return The authorized signer, or null if none
         */
        X509Certificate getAuthorizedSigner(OCSPClient client, X509Certificate[] certificates);
    }

    /**
     * Represents a status from the OCSP responder
     */
    public static final class OCSPStatus {
        private final long expires;
        private final CertificateValidationResult result;

        private OCSPStatus(long expires, CertificateValidationResult result) {
            this.expires = expires;
            this.result = result;
        }

        /**
         * Get the expiry time for the status.
         *
         * <p>This is only meaningful if the result is {@link CertificateValidationResult#OK OK}.</p>
         *
         * @return The expiry or -1 if not applicable.
         */
        public long getExpiry() {
            return expires;
        }

        /**
         * Get the result for this response.
         *
         * <p>This will never return {@link CertificateValidationResult#CANT_BUILD_PATH CANT_BUILD_PATH}.</p>
         *
         * <p>The result {@link com.l7tech.security.types.CertificateValidationResult#UNKNOWN UNKNOWN} means that the responder is
         * not able to give an authorative response for the certificate (i.e. you sent the request to the 
         * wrong responder, or there's no URL in the certificate being checked).</p>
         *
         * @return The CertificateValidationResult (never null)
         */
        public CertificateValidationResult getResult() {
            return result;
        }
    }

    /**
     * Exception class for OCSPClient
     */
    public static class OCSPClientException extends Exception {
        public OCSPClientException(final String message) { super(message); }
        public OCSPClientException(final String message, Throwable cause) { super(message, cause); }
    }

    /**
     * Exception class for OCSPClient bad response status
     */
    public static final class OCSPClientStatusException extends OCSPClientException {
        public OCSPClientStatusException(final String message) { super(message); }
        public OCSPClientStatusException(final String message, Throwable cause) { super(message, cause); }
    }

    //- PACKAGE

    static CertID buildCertID( final X509Certificate issuerCertificate, final BigInteger serialNumber ) throws OCSPException { 
        try {
            final AlgorithmIdentifier algorithmidentifier =
                    new AlgorithmIdentifier(new DERObjectIdentifier(CertificateID.HASH_SHA1), new DERNull());

            final MessageDigest messagedigest = JceProvider.getMessageDigest("SHA-1", null);
            final X509Principal x509principal = PrincipalUtil.getSubjectX509Principal(issuerCertificate);
            messagedigest.update(x509principal.getEncoded());
            final DEROctetString deroctetstring = new DEROctetString(messagedigest.digest());

            final PublicKey publickey = issuerCertificate.getPublicKey();
            final ASN1InputStream asn1inputstream = new ASN1InputStream(publickey.getEncoded());
            final SubjectPublicKeyInfo subjectpublickeyinfo =
                    SubjectPublicKeyInfo.getInstance(asn1inputstream.readObject());
            messagedigest.update(subjectpublickeyinfo.getPublicKeyData().getBytes());
            final DEROctetString deroctetstring1 = new DEROctetString(messagedigest.digest());

            final DERInteger derinteger = new DERInteger(serialNumber);

            return new CertID( algorithmidentifier, deroctetstring, deroctetstring1, derinteger );
        } catch( Exception exception ) {
            throw new OCSPException((new StringBuilder()).append("problem creating ID: ").append(exception).toString(), exception);
        }
    }

    byte[] generateNonce() {
        byte[] nonceBytes = new byte[8];
        random.nextBytes(nonceBytes);
        return nonceBytes;
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    //- PRIVATE

    private static final String SYSPROP_PROVIDER = "com.l7tech.server.security.ocspProvider";
    private static final String DEFAULT_PROVIDER = "SUN";

    private static final long MAX_RESPONSE_AGE = ConfigFactory.getLongProperty( "com.l7tech.server.security.ocspMaxResponseAge", TimeUnit.DAYS.toMillis( 31L ) );// default to around one month
    private static final long NEXT_UPDATE_OFFSET = ConfigFactory.getTimeUnitProperty( "com.l7tech.server.security.ocspNextUpdateOffset", 0L );

    private static final String CONTENT_TYPE_OCSP_REQUEST = "application/ocsp-request";
    private static final String CONTENT_TYPE_OCSP_RESPONSE = "application/ocsp-response";

    private static final String OID_EXT_KEY_USE_OCSPSIGN = "1.3.6.1.5.5.7.3.9";
    private static final String OID_EXT_KEY_USE_OCSP_NOCHECK = "1.3.6.1.5.5.7.48.1.5";

    /**
     * Secure random used for nonce generation
     */
    private static final Random random = new SecureRandom();

    private static final Logger logger = Logger.getLogger(OCSPClient.class.getName());

    private final GenericHttpClient httpClient;
    private final String url;
    private final X509Certificate issuerCertificate;
    private final OCSPCertificateAuthorizer certificateAuthorizer;

    /**
     * Check the revocation status of the given cert using the specified url 
     */
    private OCSPStatus doRevocationCheck(final X509Certificate certificate,
                                         final boolean nonce,
                                         final boolean signed) throws OCSPClientException {
        final OCSPStatus status;

        try {
            CertificateID certId = new CertificateID(buildCertID( issuerCertificate, certificate.getSerialNumber()));
            byte[] nonceBytes = nonce ? generateNonce() : null;
            OCSPReq request = generateOCSPRequest(certId,  nonceBytes);
            status = sendRequest(url, request, certId, signed, nonceBytes);
        } catch ( OCSPException oe ) {
            throw new OCSPClientException("Error generating OCSP request for certificate.", oe);
        }

        return status;
    }

    /**
     * Generate an OCSP request with the given info.
     */
    private static OCSPReq generateOCSPRequest( final CertificateID id,
                                                final byte[] nonceBytes ) throws OCSPException {
        // create generator
        final OCSPReqGenerator generator = new OCSPReqGenerator();

        // Add the ID for the certificate we are looking for
        generator.addRequest(id);

        // Extension holders
        final Vector<DERObjectIdentifier> oids = new Vector<DERObjectIdentifier>();
        final Vector<X509Extension> values = new Vector<X509Extension>();

        // Only support basic responses
        oids.add(OCSPObjectIdentifiers.id_pkix_ocsp_response);
        values.add(new X509Extension(false, new DEROctetString(new DERSequence(new ASN1Encodable[]{OCSPObjectIdentifiers.id_pkix_ocsp_basic}).getDEREncoded())));

        // Optionally add nonce extension
        if ( nonceBytes != null ) {
            oids.add(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
            values.add(new X509Extension(false, new DEROctetString(nonceBytes)));
        }

        // Add extensions
        if ( !oids.isEmpty() )
            generator.setRequestExtensions(new X509Extensions(oids, values));

        return generator.generate();
    }

    /**
     * Send the given request to the specified url 
     */
    private OCSPStatus sendRequest(final String ocspResponderUrl,
                                   final OCSPReq request,
                                   final CertificateID certId,
                                   final boolean signed,
                                   final byte[] nonceBytes) throws OCSPClientException {
        final URL requestUrl;
        try {
            requestUrl = new URL(ocspResponderUrl);
        } catch (MalformedURLException murle) {
            throw new OCSPClientException("Invalid URL for OCSP responder '" + ocspResponderUrl + "'.", murle);
        }

        final GenericHttpRequestParams requestParams;
        final byte[] requestEncodedBytes;
        try {
            // Encode request
            requestEncodedBytes = request.getEncoded();
            // build request params
            requestParams = new GenericHttpRequestParams(requestUrl);
            requestParams.setContentType(ContentTypeHeader.parseValue(CONTENT_TYPE_OCSP_REQUEST));
            requestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_ACCEPT, CONTENT_TYPE_OCSP_RESPONSE));
            requestParams.setFollowRedirects(false);
            requestParams.setContentLength((long) requestEncodedBytes.length);
        } catch (IOException ioe) {
            throw new OCSPClientException("Error creating OCSP request parameters", ioe);
        }

        try (final RerunnableHttpRequest httpRequest = (RerunnableHttpRequest) httpClient.createRequest(HttpMethod.POST, requestParams)) {
            httpRequest.setInputStreamFactory(() -> new ByteArrayInputStream(requestEncodedBytes));

            // get response
            try (final GenericHttpResponse httpResponse = httpRequest.getResponse()) {
                final int httpStatus = httpResponse.getStatus();
                logger.log(Level.FINER, "Response HTTP status {0} for OCSP responder {1}", new Object[]{httpStatus, ocspResponderUrl});

                if (httpStatus != HttpConstants.STATUS_OK) {
                    throw new OCSPClientException("Failing due to HTTP status code '" + httpStatus
                            + "' from responder '" + ocspResponderUrl + "'.");
                }

                final OCSPResp ocspResponse = new OCSPResp(httpResponse.getInputStream());
                return handleResponse(ocspResponse, certId, signed, nonceBytes);
            }
        } catch (IOException ioe) {
            throw new OCSPClientException("HTTP error during OCSP request.", ioe);
        }
    }

    /**
     * Handle OCSP response
     */
    private OCSPStatus handleResponse(final OCSPResp ocspResponse,
                                      final CertificateID certId,
                                      final boolean signed,
                                      final byte[] nonceBytes) throws OCSPClientException
    {
        final OCSPStatus status;

        // process response
        final int responseStatus = ocspResponse.getStatus();

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "OCSP response status is ''{0}''.", responseStatus);
        }

        // 
        switch ( responseStatus ) {
            case 0: // successful (so process to check revocation status for certificate)
                status = processOCSPResponse(ocspResponse, certId, signed, nonceBytes);
                break;
            case 1: // bad request
                throw new OCSPClientStatusException("OCSP Response Error [1]: Bad Request");
            case 2: // issuer error
                throw new OCSPClientStatusException("OCSP Response Error [2]: Responder Error");
            case 3: // tryLater
                throw new OCSPClientStatusException("OCSP Response Error [3]: Responder Error (Try Later)");
            // there is no status 4 ...
            case 5: // must sign request
                throw new OCSPClientStatusException("OCSP Response Error [5]: Request Signature Required");
            case 6: // unauthorized
                throw new OCSPClientStatusException("OCSP Response Error [6]: Unauthorized");
            default:
                throw new OCSPClientStatusException("Unexpected OCSP Response Error: Status code " + responseStatus);
        }

        return status;
    }

    /**
     * Process the successful (as in valid) OCSP response.
     */
    private OCSPStatus processOCSPResponse(final OCSPResp ocspResponse,
                                           final CertificateID certId,
                                           final boolean signed,
                                           final byte[] nonceBytes) throws OCSPClientException
    {
        OCSPStatus status = null;

        // get response data
        BasicOCSPResp basicOcspResp;
        try {
            Object responseObject = ocspResponse.getResponseObject();

            if (!(responseObject instanceof BasicOCSPResp)) {
                throw new OCSPClientException("Invalid OCSP response type: " +
                        (responseObject == null ?  "NULL" : responseObject.getClass().getName()));
            }

            basicOcspResp = (BasicOCSPResp) responseObject;
        } catch (OCSPException oe) {
            throw new OCSPClientException("Error processing response", oe);    
        }

        // validate the signature
        validateSignature(basicOcspResp, signed);

        // check the nonce
        if ( nonceBytes != null ) {
            byte[] responseNonce = null;
            byte[] value = basicOcspResp.getExtensionValue( OCSPObjectIdentifiers.id_pkix_ocsp_nonce.getId() );
            if ( value != null ) {
                try {
                    ASN1OctetString extensionOctetString = (ASN1OctetString) new ASN1InputStream( value ).readObject();
                    responseNonce = extensionOctetString.getOctets();
                } catch ( IOException e ) {
                    throw new OCSPClientException("Error processing response nonce", e);
                }
            }
            if ( responseNonce == null ) {
                throw new OCSPClientException("OCSP nonce was required but not present in response message");
            } else if ( !Arrays.equals( nonceBytes, responseNonce )) {
                throw new OCSPClientException("OCSP nonce mismatch expected " + HexUtils.hexDump(nonceBytes)+ " got " + HexUtils.hexDump(responseNonce));    
            }
        }

        // validate response time (producedAt appears to be infrormational, thisUpdate requires validation
        // to ensure it is "sufficiently recent" [RFC 2560 section 3.2])
        long timeNow = currentTimeMillis();

        for (SingleResp response : basicOcspResp.getResponses()) {
            if (response.getCertID().equals(certId)) {

                Date thisUpdate =  response.getThisUpdate();
                if ( thisUpdate == null || (timeNow - thisUpdate.getTime()) > MAX_RESPONSE_AGE ) {
                    throw new OCSPClientException("OCSP response is stale (beyond maximum permitted age)");
                }

                Date nextUpdate = response.getNextUpdate();
                long nextUpdateTime;
                if (nextUpdate == null) {
                    nextUpdateTime = timeNow;
                } else {
                    nextUpdateTime = nextUpdate.getTime();
                    if ( (nextUpdateTime + NEXT_UPDATE_OFFSET) <= timeNow ) {
                        throw new OCSPClientException("OCSP response is stale (next update due)");
                    }
                }

                Object certStatusObject = response.getCertStatus();
                if (certStatusObject instanceof RevokedStatus) {
                    status = new OCSPStatus(nextUpdateTime, CertificateValidationResult.REVOKED);
                } else if (certStatusObject instanceof UnknownStatus) {
                    status = new OCSPStatus(nextUpdateTime, CertificateValidationResult.UNKNOWN);
                } else {
                    status = new OCSPStatus(nextUpdateTime, CertificateValidationResult.OK);
                }

                break;
            }
        }

        if ( status == null ) {
            throw new OCSPClientException("OCSP response does not contain information for requested certificate.");
        }

        return status;
    }

    /**
     * Ensure that the signature on the OCSP response is valid and present (if required)
     */
    private void validateSignature(final BasicOCSPResp basicOcspResp,
                                   final boolean requireSignature) throws OCSPClientException {
        final String sigAlg = basicOcspResp.getSignatureAlgName();
        final X509Certificate signer;
        X509Certificate[] signerCerts;

        try {
            signerCerts = basicOcspResp.getCerts( ConfigFactory.getProperty( SYSPROP_PROVIDER, DEFAULT_PROVIDER ) );
        } catch (OCSPException oe) {
            throw new OCSPClientException("Error processing certificates in OCSP response.", oe);
        } catch (NoSuchProviderException nspe) {
            throw new OCSPClientException("Security provider error when processing certificates in OCSP response", nspe);
        }

        //
        if (signerCerts == null)
            signerCerts = new X509Certificate[0];
        signer = certificateAuthorizer.getAuthorizedSigner(this, signerCerts);

        // Perform signature validation
        try {
            byte[] signedData = basicOcspResp.getTBSResponseData();
            byte[] sig = basicOcspResp.getSignature();
            if (sig != null) {
                if (signer == null) {
                    throw new OCSPClientException("OCSP response is signed but the signer is unknown (or issuer is not permitted).");
                }

                Signature signature = Signature.getInstance(sigAlg);
                signature.initVerify(signer);
                signature.update(signedData);
                if (!signature.verify(sig))
                    throw new OCSPClientException("OCSP response signature was not valid.");
            } else if ( requireSignature ) {
                throw new OCSPClientException("OCSP response not signed and signature is required.");
            }
        } catch (OCSPException oe) {
            throw new OCSPClientException("Invalid OCSP response.", oe);    
        } catch (GeneralSecurityException gse) {
            throw new OCSPClientException("Error verifying OCSP response signature.", gse);    
        }
    }
}
