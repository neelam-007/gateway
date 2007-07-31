package com.l7tech.server.util;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Extension;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

import com.l7tech.common.util.CertUtils;

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
        Set urls = new HashSet();
        byte[] distibutionPointBytes = cert.getExtensionValue(CertUtils.X509_OID_CRL_DISTRIBUTION_POINTS);
        if (distibutionPointBytes != null && distibutionPointBytes.length > 0) {
            ASN1Encodable asn1 = X509ExtensionUtil.fromExtensionValue(distibutionPointBytes);
            DERObject obj = asn1.getDERObject();
            CRLDistPoint distPoint = CRLDistPoint.getInstance(obj);
            DistributionPoint[] points = distPoint.getDistributionPoints();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < points.length; i++) {
                DistributionPoint point = points[i];
                DistributionPointName dpn = point.getDistributionPoint();
                obj = dpn.getName().toASN1Object();
                org.bouncycastle.asn1.ASN1Sequence seq = org.bouncycastle.asn1.ASN1Sequence.getInstance(obj);
                DERTaggedObject tag = (DERTaggedObject) seq.getObjectAt(0);
                DERObject foo = tag.getObject();
                if (foo instanceof DEROctetString) {
                    DEROctetString derOctetString = (DEROctetString) foo;
                    distibutionPointBytes = derOctetString.getOctets();
                    //noinspection unchecked
                    urls.add(new String(distibutionPointBytes, "ISO8859-1"));
                }
            }
        }

        byte[] netscapeCrlUrlBytes = cert.getExtensionValue(CertUtils.X509_OID_NETSCAPE_CRL_URL);
        if (netscapeCrlUrlBytes != null && netscapeCrlUrlBytes.length > 0) {
            ASN1Encodable asn1 = X509ExtensionUtil.fromExtensionValue(netscapeCrlUrlBytes);
            if (asn1 instanceof DERString) {
                //noinspection unchecked
                urls.add(((DERString) asn1).getString());
            } else {
                throw new IOException("Netscape CRL URL extension value is not a String");
            }
        }
        //noinspection unchecked
        return (String[])urls.toArray(new String[0]);
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
        Set<String> uris = new LinkedHashSet();

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
                AuthorityInformationAccess aia =(AuthorityInformationAccess) new AuthorityInformationAccess(sequence);
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

    private static AuthorityKeyIdentifierStructure doGetAKIStructure(X509Extension x509Extendable) throws IOException {
        byte[] aki = x509Extendable.getExtensionValue(X509Extensions.AuthorityKeyIdentifier.getId());
        if (aki == null) return null;
        return new AuthorityKeyIdentifierStructure(aki);
    }

}
