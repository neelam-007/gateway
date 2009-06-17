package com.l7tech.server.util;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.HexUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Certificate utility methods that require server only classes.
 *
 * @author Steve Jones
 */
public class ServerCertUtils {

    /**
     * @return an array of zero or more CRL URLs from the certificate
     */
    public static String[] getCrlUrls(X509Certificate cert) throws IOException {
        Set<String> urls = new LinkedHashSet<String>();
        byte[] distibutionPointBytes = cert.getExtensionValue(CertUtils.X509_OID_CRL_DISTRIBUTION_POINTS);
        if (distibutionPointBytes != null && distibutionPointBytes.length > 0) {
            ASN1Encodable asn1 = X509ExtensionUtil.fromExtensionValue(distibutionPointBytes);
            DERObject obj = asn1.getDERObject();
            CRLDistPoint distPoint = CRLDistPoint.getInstance(obj);
            DistributionPoint[] points = distPoint.getDistributionPoints();
            for (DistributionPoint point : points) {
                DistributionPointName dpn = point.getDistributionPoint();
                obj = dpn.getName().toASN1Object();
                ASN1Sequence seq = ASN1Sequence.getInstance(obj);
                DEREncodable first = seq.getObjectAt(0);
                if (first instanceof GeneralName) {
                    GeneralName generalName = (GeneralName) first;
                    urls.add(generalName.getName().toString());
                } else if (first instanceof ASN1Encodable) {
                    ASN1Encodable tag = (ASN1Encodable) first;
                    DERObject foo = tag.getDERObject().getDERObject();
                    if (foo instanceof DEROctetString) {
                        DEROctetString derOctetString = (DEROctetString) foo;
                        distibutionPointBytes = derOctetString.getOctets();
                        urls.add(new String(distibutionPointBytes, "ISO8859-1"));
                    }
                }
            }
        }

        byte[] netscapeCrlUrlBytes = cert.getExtensionValue( CertUtils.X509_OID_NETSCAPE_CRL_URL);
        if (netscapeCrlUrlBytes != null && netscapeCrlUrlBytes.length > 0) {
            ASN1Encodable asn1 = X509ExtensionUtil.fromExtensionValue(netscapeCrlUrlBytes);
            if (asn1 instanceof DERString) {
                urls.add(((DERString) asn1).getString());
            } else {
                throw new IOException("Netscape CRL URL extension value is not a String");
            }
        }
        return urls.toArray(new String[urls.size()]);
    }

    /**
     * Get the URIs from the certificates authority information access extension for the given access method.
     *
     * <p>Possible values for the accessmethodOid are:</p>
     *
     * <ul>
     *   <li>OCSP       - 1.3.6.1.5.5.7.48.1</li>
     *   <li>CA Issuers - 1.3.6.1.5.5.7.48.2</li>
     * </ul>
     *
     * <p>Note that this method will only return values with the URI name type.</p>
     *
     * @param certificate The certificate to examine
     * @param accessMethodOid The OID of the desired access method
     * @return The array of uris (may be empty but not null)
     * @throws java.security.cert.CertificateException if the certificates authority information access extension is invalid
     */
    public static String[] getAuthorityInformationAccessUris(final X509Certificate certificate,
                                                             final String accessMethodOid)
            throws CertificateException {
        Set<String> uris = new LinkedHashSet<String>();

        byte[] aiaBytes = certificate.getExtensionValue(CertUtils.X509_OID_AUTHORITY_INFORMATION_ACCESS);
        if (aiaBytes != null) {
            try {
                // Process AIA extension
                ASN1Object extensionObject = ASN1Object.fromByteArray(aiaBytes);
                if (!(extensionObject instanceof DEROctetString))
                    throw new CertificateException("Certificate authority information access extension is not of the expected type: " +
                            extensionObject.getClass().getName());

                DEROctetString derOS = (DEROctetString) extensionObject;
                ASN1Object extensionSequenceObject =  ASN1Object.fromByteArray(derOS.getOctets());
                if (!(extensionSequenceObject instanceof DERSequence))
                    throw new CertificateException("Certificate authority information access extension content is not of the expected type: " +
                            extensionSequenceObject.getClass().getName());

                // Create AIA from sequence
                DERSequence sequence = (DERSequence) extensionSequenceObject;
                AuthorityInformationAccess aia = new AuthorityInformationAccess(sequence);
                AccessDescription[] accessDescriptions = aia.getAccessDescriptions();

                if (accessDescriptions.length == 0)
                    throw new CertificateException("Certificate authority information access extension is empty.");

                for (AccessDescription accessDescription : accessDescriptions) {
                    if(accessMethodOid.equals(accessDescription.getAccessMethod().getId())) {
                        GeneralName name = accessDescription.getAccessLocation();
                        // GeneralName ::= CHOICE { ... uniformResourceIdentifier       [6]     IA5String,
                        if (name.getTagNo() == 6) {
                            DEREncodable nameObject = name.getName();
                            if (!(nameObject instanceof DERIA5String))
                                throw new CertificateException("Certificate authority information access extension has access description location with incorrect name type " +
                                    nameObject.getClass().getName());

                            DERIA5String urlDer = (DERIA5String) nameObject;
                            uris.add(urlDer.getString());
                        }
                    }
                }
            }
            catch(IllegalArgumentException iae) { // can be thrown from AuthorityInformationAccess constructor
                throw new CertificateException("Error processing certificate authority information access extension.", iae);
            }
            catch(IOException ioe) {
                throw new CertificateException("Error processing certificate authority information access extension.", ioe);
            }
        }

        return uris.toArray(new String[uris.size()]);
    }

    public static AuthorityKeyIdentifierStructure getAKIStructure(X509Certificate cert) throws IOException {
        if (cert.getVersion() < 3) return null;
        return doGetAKIStructure(cert);
    }

    public static AuthorityKeyIdentifierStructure getAKIStructure(X509CRL crl) throws IOException {
        if (crl.getVersion() < 2) return null;
        return doGetAKIStructure(crl);
    }

    /**
     * Get the Base64 encoded key identifier for the authorities certificate.
     *
     * @param akis The structure from which to get the key identifier
     * @return The key identifier or null if there is none
     */
    public static String getAKIKeyIdentifier(AuthorityKeyIdentifierStructure akis) {
        String ski = null;

        byte[] skiBytes =  akis.getKeyIdentifier();
        if (skiBytes != null) {
            ski = HexUtils.encodeBase64(skiBytes, true);
        }

        return ski;
    }

    /**
     * Get the serial number for the authorities certificate.
     *
     * @param akis The structure from which to get the serial number
     * @return The serial number or null if there is none
     */
    public static BigInteger getAKIAuthorityCertSerialNumber(AuthorityKeyIdentifierStructure akis) {
        return akis.getAuthorityCertSerialNumber();
    }

    /**
     * Get the Issuer DN for the authorities certificate (issuer of the authorities certificate).
     *
     * @param akis The structure from which to get the serial number
     * @return The Issuer DN or null if there is none
     * @throws CertificateException if the issuerDn is present but invalid.
     */
    public static String getAKIAuthorityCertIssuer(AuthorityKeyIdentifierStructure akis) throws CertificateException {
        String issuerDn = null;

        GeneralNames names = akis.getAuthorityCertIssuer();
        if (names != null) {
            for ( GeneralName name : names.getNames() ) {
                if (name.getTagNo()==4) { // need a directory name
                    try {
                        X500Principal x500Name = new X500Principal(name.getName().getDERObject().getDEREncoded());
                        issuerDn = x500Name.toString();
                    }
                    catch(IllegalArgumentException iae) {
                        throw new CertificateException("Could not parse issuer as directory name.", iae);
                    }
                }
            }

            if ( issuerDn == null ) {
                throw new CertificateException("Could not find issuer as directory name.");
            }
        }

        return issuerDn;
    }

    private static AuthorityKeyIdentifierStructure doGetAKIStructure(X509Extension x509Extendable) throws IOException {
        byte[] aki = x509Extendable.getExtensionValue(X509Extensions.AuthorityKeyIdentifier.getId());
        if (aki == null) return null;
        return new AuthorityKeyIdentifierStructure(aki);
    }

}
