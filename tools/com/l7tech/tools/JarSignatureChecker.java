package com.l7tech.tools;

import java.io.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Check Jar files under a given directory for expired signatures.
 */
public class JarSignatureChecker {

    public static void main( final String[] args ) {
        if ( args.length < 1 ) {
            System.out.println( "Use: JarSignatureChecker <directory>" );
        } else {
            JarSignatureChecker checker = new JarSignatureChecker();
            checker.checkDirectory( new File(args[0]) );
            checker.dumpCerts();
            System.exit( checker.getExitCode() );
        }
    }

    private void dumpCerts() {
        System.out.println( "\nJars by signing certificate:" );
        for ( X509Certificate certificate : expiryMap.values() ) {
            Collection<String> jars =  jarMap.get( certificate );
            System.out.println( certificate.getSubjectDN().getName() + ": " + jars );            
        }

        System.out.println( "\nCertificates by expiry date:" );
        for ( X509Certificate certificate : expiryMap.values() ) {
            System.out.println( certificate.getSubjectDN().getName() + " " + certificate.getNotAfter() );
        }
    }

    private int getExitCode() {
        int code = 0;

        // error if expires in the next 6 months (ish)
        Date errorExpiry = new Date( System.currentTimeMillis() + ( 1000L*60L*60L*24L*180L ) );
        if ( !expiryMap.isEmpty() && expiryMap.keySet().iterator().next().before(errorExpiry) ) {
            code = 1;
        }

        return code;
    }

    private void checkDirectory(final File file) {
        File[] jarFiles = file.listFiles(jarFileFilter);
        if ( jarFiles != null ) {
            for ( File jarFile : jarFiles ) {
                checkFile( jarFile );
            }
        }

        File[] directories = file.listFiles(directoryFileFilter);
        if ( directories != null ) {
            for ( File direcotory : directories ) {
                checkDirectory( direcotory );   
            }
        }
    }

    private void checkFile( final File file ) {
        System.out.println( "Checking JAR file : " + file );
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(file);
            for ( JarEntry entry : Collections.list(jarFile.entries()) ) {
                // have to read the entry before you can get the certs
                readStream( jarFile.getInputStream(entry) );
                Certificate[] certificates = entry.getCertificates();
                if ( certificates != null ) {
                    for ( Certificate certificate : certificates ) {
                        if ( certificate instanceof X509Certificate) {
                            X509Certificate x509Certificate = (X509Certificate) certificate;
                            expiryMap.put( x509Certificate.getNotAfter(), x509Certificate );
                            Collection<String> jars = jarMap.get( x509Certificate );
                            if ( jars == null ) {
                                jars = new TreeSet<String>();
                                jarMap.put( x509Certificate,  jars );
                            }
                            jars.add( file.getName() );
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( jarFile != null ) try {
                jarFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final Map<X509Certificate, Collection<String>> jarMap = new HashMap<X509Certificate, Collection<String>>();
    private final Map<Date, X509Certificate> expiryMap = new TreeMap<Date,X509Certificate>();
    private final byte[] block = new byte[4096];

    private final FileFilter jarFileFilter = new FileFilter() {
        @Override
        public boolean accept( final File pathname ) {
            return pathname.getName().endsWith(".jar");
        }
    };

    private final FileFilter directoryFileFilter = new FileFilter() {
        @Override
        public boolean accept( final File pathname ) {
            return pathname.isDirectory();
        }
    };

    @SuppressWarnings({"StatementWithEmptyBody"})
    private void readStream( final InputStream in ) {
        try {
            while (in.read(block) >= 0) ;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

