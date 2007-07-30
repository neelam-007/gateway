package com.l7tech.server.security.cert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.NoSuchProviderException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.Vector;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.OCSPException;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPReqGenerator;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.SingleResp;
import org.bouncycastle.ocsp.RevokedStatus;
import org.bouncycastle.ocsp.UnknownStatus;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.RerunnableHttpRequest;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.security.CertificateValidationResult;
import com.l7tech.common.util.CertUtils;

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
     * @param allowIssuer True to allow issuer or issuer designated signer for OCSP responses
     * @param signers Additional permitted signers (may be null)
     */
    public OCSPClient(final GenericHttpClient httpClient,
                      final String url,
                      final X509Certificate issuer,
                      final boolean allowIssuer,
                      final X509Certificate[] signers) {
        if (httpClient == null) throw new IllegalArgumentException("httpClient is required");
        if (url == null) throw new IllegalArgumentException("url is required");
        if (issuer == null) throw new IllegalArgumentException("issuer is required");

        this.httpClient = httpClient;
        this.url = url;
        this.issuerCertificate = issuer;
        this.allowIssuer = allowIssuer;
        this.signers = signers==null ? new X509Certificate[0] : signers;
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
         * <p>The result {@link CertificateValidationResult#UNKNOWN UNKNOWN} means that the responder is
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
    public static final class OCSPClientException extends Exception {
        public OCSPClientException(final String message) { super(message); }
        public OCSPClientException(final String message, Throwable cause) { super(message, cause); }
    }

    //- PRIVATE

    private static final String SYSPROP_PROVIDER = "com.l7tech.server.security.ocspProvider";
    private static final String DEFAULT_PROVIDER = "SUN";

    private static final long MAX_RESPONSE_AGE = 1000L * 60L * 60L * 24L * 31L;// around one month 

    private static final String CONTENT_TYPE_OCSP_REQUEST = "application/ocsp-request";
    private static final String CONTENT_TYPE_OCSP_RESPONSE = "application/ocsp-response";

    private static final String OID_EXT_KEY_USE_OCSPSIGN = "1.3.6.1.5.5.7.3.9";

    /**
     * Secure random used for nonce generation
     */
    private static final Random random = new SecureRandom();

    private static final Logger logger = Logger.getLogger(OCSPClient.class.getName());

    private final GenericHttpClient httpClient;
    private final String url;
    private final X509Certificate issuerCertificate;
    private final boolean allowIssuer;
    private final X509Certificate[] signers;

    /**
     * Check the revocation status of the given cert using the specified url 
     */
    private OCSPStatus doRevocationCheck(final X509Certificate certificate,
                                         final boolean nonce,
                                         final boolean signed) throws OCSPClientException {
        OCSPStatus status = null;

        try {
            CertificateID certId = new CertificateID(CertificateID.HASH_SHA1, issuerCertificate, certificate.getSerialNumber());
            OCSPReq request = generateOCSPRequest(certId,  nonce);
            status = sendRequest(url, request, certId, issuerCertificate, signed);
        } catch ( OCSPException oe ) {
            throw new OCSPClientException("Error generating OCSP request for certificate.", oe);
        }

        return status;
    }

    /**
     * Generate an OCSP request with the given info.
     */
    private static OCSPReq generateOCSPRequest(final CertificateID id,
                                               final boolean nonce) throws OCSPException {
        // create generator
        OCSPReqGenerator generator = new OCSPReqGenerator();

        // Add the ID for the certificate we are looking for
        generator.addRequest(id);

        // Extension holders
        Vector oids = new Vector();
        Vector values = new Vector();

        // Only support basic responses
        oids.add(OCSPObjectIdentifiers.id_pkix_ocsp_response);
        values.add(new X509Extension(false, new DEROctetString(new DERSequence(new ASN1Encodable[]{OCSPObjectIdentifiers.id_pkix_ocsp_basic}).getDEREncoded())));

        // Optionally add nonce extension
        if ( nonce ) {
            byte[] nonceBytes = new byte[8];
            random.nextBytes(nonceBytes);

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
                                   final X509Certificate issuerCert,
                                   final boolean signed) throws OCSPClientException {
        OCSPStatus status = new OCSPStatus(0, CertificateValidationResult.REVOKED);
        RerunnableHttpRequest httpRequest;

        try {
            // Encode request
            final byte[] array = request.getEncoded();

            // build request
            URL requestUrl = new URL((String) ocspResponderUrl);
            GenericHttpRequestParams params = new GenericHttpRequestParams(requestUrl);
            params.setContentType(ContentTypeHeader.parseValue(CONTENT_TYPE_OCSP_REQUEST));
            params.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_ACCEPT, CONTENT_TYPE_OCSP_RESPONSE));
            params.setFollowRedirects(false);
            params.setContentLength(Long.valueOf(array.length));
            httpRequest = (RerunnableHttpRequest) httpClient.createRequest(GenericHttpClient.POST, params);
            httpRequest.setInputStreamFactory(new RerunnableHttpRequest.InputStreamFactory(){
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(array);
                }
            });
        } catch (MalformedURLException murle) {
            throw new OCSPClientException("Invalid URL for OCSP responder '"+ocspResponderUrl+"'.", murle);
        } catch (IOException ioe) {
            throw new OCSPClientException("Error creating OCSP request for responder '"+ocspResponderUrl+"'.", ioe);
        }

        try {
            // get response
            GenericHttpResponse httpResponse = httpRequest.getResponse();
            int httpStatus = httpResponse.getStatus();
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Response HTTP status {0} for OCSP responder {1}",
                        new Object[]{httpStatus, ocspResponderUrl});
            }
            if (httpStatus != HttpConstants.STATUS_OK) {
                throw new OCSPClientException("Failing due to HTTP status code '" + httpStatus 
                        + "' from responder '" + ocspResponderUrl + "'.");
            }

            OCSPResp ocspResponse = new OCSPResp(httpResponse.getInputStream());
            status = handleResponse(ocspResponse, certId, issuerCert, signed);
            
        } catch (IOException ioe) {
            throw new OCSPClientException("HTTP error processing OCSP request for responder '"+ocspResponderUrl+"'.", ioe);
        }

        return status;
    }

    /**
     * Handle OCSP response
     */
    private OCSPStatus handleResponse(final OCSPResp ocspResponse,
                                      final CertificateID certId,
                                      final X509Certificate issuerCert,
                                      final boolean signed) throws OCSPClientException
    {
        OCSPStatus status = null;

        // process response
        int responseStatus = ocspResponse.getStatus();

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "OCSP response status is ''{0}''.", responseStatus);
        }

        // 
        switch ( responseStatus ) {
            case 0: // successful (so process to check revocation status for certificate)
                status = processOCSPResponse(ocspResponse, certId, issuerCert, signed);
                break;
            case 1: // bad request
                throw new OCSPClientException("OCSP Response Error [1]: Bad Request");
            case 2: // issuer error
                throw new OCSPClientException("OCSP Response Error [2]: Responder Error");
            case 3: // tryLater
                throw new OCSPClientException("OCSP Response Error [3]: Responder Error (Try Later)");
            // there is no status 4 ...
            case 5: // must sign request
                throw new OCSPClientException("OCSP Response Error [5]: Request Signature Required");
            case 6: // unauthorized
                throw new OCSPClientException("OCSP Response Error [6]: Unauthorized");
            default:
                throw new OCSPClientException("Unexpected OCSP Response Error: Status code " + responseStatus);
        }

        return status;
    }

    /**
     * Process the successful (as in valid) OCSP response.
     */
    private OCSPStatus processOCSPResponse(final OCSPResp ocspResponse,
                                           final CertificateID certId,
                                           final X509Certificate issuerCert,
                                           final boolean signed) throws OCSPClientException
    {
        OCSPStatus status = null;

        // get response data
        BasicOCSPResp basicOcspResp;
        try {
            Object responseObject = ocspResponse.getResponseObject();

            if (!(responseObject instanceof BasicOCSPResp)) {
                throw new OCSPClientException("Invalid OCSP response type: " +
                        responseObject == null ?  "NULL" : responseObject.getClass().getName());
            }

            basicOcspResp = (BasicOCSPResp) responseObject;
        } catch (OCSPException oe) {
            throw new OCSPClientException("Error processing response", oe);    
        }

        // validate the signature
        validateSignature(basicOcspResp, issuerCert, signed);

        // validate response time (producedAt appears to be infrormational, thisUpdate requires validation
        // to ensure it is "sufficiently recent" [RFC 2560 section 3.2])
        long timeNow = System.currentTimeMillis();

        for (SingleResp response : basicOcspResp.getResponses()) {
            if (response.getCertID().equals(certId)) {

                Date thisUpdate =  response.getThisUpdate();
                if ( thisUpdate == null || (timeNow - thisUpdate.getTime()) > MAX_RESPONSE_AGE ) {
                    throw new OCSPClientException("OCSP response is stale.");                    
                }

                Date nextUpdate = response.getNextUpdate();
                long nextUpdateTime;
                if (nextUpdate == null) {
                    nextUpdateTime = timeNow;
                } else {
                    nextUpdateTime = nextUpdate.getTime();
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
                                   final X509Certificate issuer,
                                   final boolean requireSignature) throws OCSPClientException {
        String sigAlg = basicOcspResp.getSignatureAlgName();
        X509Certificate signer = null;
        X509Certificate[] signerCerts;

        try {
            signerCerts = basicOcspResp.getCerts(System.getProperty(SYSPROP_PROVIDER, DEFAULT_PROVIDER));
        } catch (OCSPException oe) {
            throw new OCSPClientException("Error processing certificates in OCSP response.", oe);
        } catch (NoSuchProviderException nspe) {
            throw new OCSPClientException("Security provider error when processing certificates in OCSP response", nspe);
        }

        //
        if ( signerCerts.length == 0 ) {
            if ( allowIssuer ) {
                signer = issuer;
            }          
        } else {
            for ( X509Certificate signerCert : signerCerts ) {
                // check ok for signing
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Checking signer certificate ''{0}''.", signerCert.getSubjectDN());
                }

                // is this the issuer?
                if ( issuer.getSerialNumber().equals(signerCert.getSerialNumber()) &&
                     issuer.getIssuerDN().getName().equals(signerCert.getIssuerDN().getName())) {
                    if ( allowIssuer ) {
                        signer = issuer;
                        break;
                    }
                }

                // is this an allowed cert?
                for ( X509Certificate trustedSigner : signers ) {
                    if ( trustedSigner.getSerialNumber().equals(signerCert.getSerialNumber()) &&
                         trustedSigner.getIssuerDN().getName().equals(signerCert.getIssuerDN().getName())) {
                        signer = trustedSigner;
                        break;
                    }
                }

                // is this cert created by the issuer for OCSP responses?
                // check this last since the OCSP signer may be trusted, in which case we don't
                // need to check the signature
                try {
                    if ( signerCert.getExtendedKeyUsage().contains(OID_EXT_KEY_USE_OCSPSIGN) ) {
                        CertUtils.cachedVerify(signerCert, issuerCertificate.getPublicKey());
                        signer = signerCert;
                        break;
                    }
                } catch (GeneralSecurityException gse) {
                    throw new OCSPClientException("Error verifying OCSP response signer certificate.", gse);
                }
            }
        }

        //TODO support for nocheck
        // If nocheck is set on the cert then we don't need to check revocation
        //OCSPObjectIdentifiers.id_pkix_ocsp_nocheck

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
