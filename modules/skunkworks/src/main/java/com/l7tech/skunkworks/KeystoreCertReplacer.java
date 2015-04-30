package com.l7tech.skunkworks;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Small command line utility to replace all instances of a cert in a keystore.
 * Throwaway command line code, no error checking -- do not reuse!
 */
public class KeystoreCertReplacer {

    public static void main( String[] args ) throws Exception {

        if ( args.length < 5 )
            usage();

        String kspath = args[0];
        char[] passphrase = args[1].toCharArray();
        String oldCertPemPath = args[2];
        String outkspath = args[3];
        String newCertPemPath = args[4];

        // Load old cert
        X509Certificate oldCert = loadCert( oldCertPemPath );
        X509Certificate newCert = loadCert( newCertPemPath );

        // TODO would be a good idea to check here to make sure that new cert
        // can sanely drop-in replace the old cert (check DN, SKI, and AKI at least) in a chain.
        // For now we just check DN.
        if ( !oldCert.getSubjectDN().equals( newCert.getSubjectDN() ) )
            throw new RuntimeException( "New cert subject DN differs from old cert subject DN" );

        // Load keystore
        KeyStore ks = KeyStore.getInstance( "PKCS12" );
        try ( InputStream is = new FileInputStream( kspath ) ) {
            ks.load( is, passphrase );
        }

        KeyStore out = KeyStore.getInstance( "PKCS12" );
        out.load( null, passphrase );

        // Scan all key entry cert chains for old cert, and replace with new cert, leaving rest of chain as-is
        int entriesModified = 0;
        Enumeration<String> aliases = ks.aliases();
        while ( aliases.hasMoreElements() ) {
            String alias = aliases.nextElement();
            if ( !ks.isKeyEntry( alias ) )
                continue;

            Key key = ks.getKey( alias, passphrase );
            Certificate[] chain = ks.getCertificateChain( alias );
            Certificate[] newChain = new Certificate[ chain.length ];
            System.arraycopy( chain, 0, newChain, 0, chain.length );
            boolean chainChanged = false;
            for ( int i = 0; i < chain.length; i++ ) {
                Certificate cert = chain[i];

                if ( cert.equals( oldCert ) ) {
                    System.err.println( "Replacing cert in chain for alias " + alias );
                    newChain[i] = newCert;
                    chainChanged = true;
                }
            }

            out.setKeyEntry( alias, key, passphrase, newChain );

            if ( chainChanged ) {
                entriesModified++;
            }
        }

        if ( entriesModified < 1 ) {
            System.err.println( "Keystore did not contain any matching certificate in a key entry cert chain.  Output file not created." );
            System.exit( 2 );
        }

        try ( OutputStream outstream = new FileOutputStream( outkspath ) ) {
           out.store( outstream, passphrase );
        }

        System.err.println( "Output file " + outkspath + " created.  " + entriesModified + " certificate chains were updated." );
    }

    private static void usage() {
        System.err.println( "Usage: KeystoreCertReplacer <inputKeystore> <passphrase> <oldCertPemPath> <outputKeystore> <newCertPemPath>" );
        System.exit( 1 );
    }

    private static X509Certificate loadCert( String pemPath ) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance( "X.509" );
        try ( InputStream is = new FileInputStream( pemPath ) ) {
            return (X509Certificate) cf.generateCertificate( is );
        }
    }

}
