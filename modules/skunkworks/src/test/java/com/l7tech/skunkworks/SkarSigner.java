package com.l7tech.skunkworks;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.io.TeeInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Experimenting with signing .zip files (such as .skar files) in a way that signs the entire raw bytes of the
 * target file, rather than just certain components, and which does not require a detached signature file.
 */
public class SkarSigner {

    /**
     * Sign an arbitrary binary input stream using the specified signing key and cert.
     *
     * @param signingCert cert to sign with.  Subject DN is used as key name, so should be unique per trusted cert.  Required.
     * @param signingKey  key to sign with. Required.
     * @param fileToSign inputstream of bytes to sign.  Required.
     * @param outputZip  outputstream to which .ZIP will be written.  Required.
     * @throws Exception if a problem occurs
     */
    public static void signZip( X509Certificate signingCert, PrivateKey signingKey, InputStream fileToSign, OutputStream outputZip ) throws Exception {
        try ( ZipOutputStream zos = new ZipOutputStream( outputZip ) ) {
            zos.putNextEntry( new ZipEntry( "signed.dat" ) );
            TeeInputStream tis = new TeeInputStream( fileToSign, zos );
            DigestInputStream dis = new DigestInputStream( tis, MessageDigest.getInstance( "SHA-256" ) );
            IOUtils.copyStream( dis, new NullOutputStream() );
            byte[] digestBytes = dis.getMessageDigest().digest();
            String digest = HexUtils.hexDump( digestBytes );

            zos.putNextEntry( new ZipEntry( "signature.xml" ) );
            Document xml = XmlUtil.stringAsDocument( "<sig><digest>" + digest + "</digest></sig>" );
            Element doc = xml.getDocumentElement();

            Element sig = DsigUtil.createEnvelopedSignature( doc, signingCert, signingKey, null, signingCert.getSubjectDN().getName(), "SHA-1" );
            doc.appendChild( sig );
            XmlUtil.nodeToOutputStream( doc, zos );
        }
    }

    /**
     * Verify a signature created by signZip.
     *
     * @param trustedSigners certs trusted to create signatures.  All must have different subject DNs.
     * @param zipToVerify input stream containing .ZIP file as produced by signZip.
     * @throws Exception if a problem occurs or if signature cannot be verified
     */
    public static void verifyZip( X509Certificate[] trustedSigners, InputStream zipToVerify ) throws Exception {
        ZipInputStream zis = new ZipInputStream( zipToVerify );

        ZipEntry signedEntry = zis.getNextEntry();
        if ( signedEntry.isDirectory() || !"signed.dat".equals( signedEntry.getName() ) )
            throw new IOException( "First zip entry is not a plain file named signed.dat" );
        DigestInputStream dis = new DigestInputStream( zis, MessageDigest.getInstance( "SHA-256" ) );
        IOUtils.copyStream( dis, new NullOutputStream() );
        byte[] computedDigest = dis.getMessageDigest().digest();

        ZipEntry sigEntry = zis.getNextEntry();
        if ( sigEntry.isDirectory() || !"signature.xml".equals( sigEntry.getName() ) )
            throw new IOException( "Second zip entry is not a plain file named signature.xml" );

        Document xml = XmlUtil.parse( zis );
        Element doc = xml.getDocumentElement();
        Element digestEl = DomUtils.findExactlyOneChildElementByName( doc, "digest" );
        byte[] signedDigest = HexUtils.unHexDump( digestEl.getTextContent() );
        if ( !Arrays.equals( computedDigest, signedDigest ) )
            throw new IOException( "Digest of signed.dat does not match value from signature.xml" );

        Element sig = DomUtils.findExactlyOneChildElementByName( doc, SoapConstants.DIGSIG_URI, "Signature" );
        X509Certificate signerCert = DsigUtil.checkSimpleEnvelopedSignature( sig, new SimpleSecurityTokenResolver( trustedSigners ) );
        boolean foundMatch = false;
        for ( X509Certificate certificate : trustedSigners ) {
            if ( CertUtils.certsAreEqual( certificate, signerCert ) ) {
                foundMatch = true;
                break;
            }
        }
        if ( !foundMatch )
            throw new IOException( "Signer certificate not recognized" );

        // Ok
    }

    public static void main( String[] args ) throws Exception {
        Pair<X509Certificate, PrivateKey> key = TestKeys.getCertAndKey( "RSA_2048" );
        byte[] fileToSign = "blah blah blah blah".getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        signZip( key.left, key.right, new ByteArrayInputStream( fileToSign ), baos );

        new FileOutputStream( "SkarSigner_test.zip" ).write( baos.toByteArray() );

        verifyZip( new X509Certificate[] { key.left }, new FileInputStream( "SkarSigner_test.zip" ) );

        System.out.println( "Signature verified successfully" );
    }

}
