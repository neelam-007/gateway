package com.l7tech.util;

import com.l7tech.common.io.CertUtils;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertUtilsUtil {
    public static void main(String[] args) throws IOException, CertificateException, NoSuchAlgorithmException {

        if (args.length != 1) {
            printUsageAndExit();
        }

        String mycert = new String();
        String certfilename = args[0];
        final File certfile = new File(certfilename);
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(certfile));
            String readLine = reader.readLine();
            while (readLine != null) {
                mycert = mycert + readLine;
                readLine = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find file: " + certfile.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + certfile.getAbsolutePath(), e);
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not close stream", e);
            }
        }
        X509Certificate clientCert = CertUtils.decodeFromPEM(mycert);
        String Output = clientCert.getSubjectDN().toString();
        String ski = CertUtils.getSki(clientCert);
        String serial = HexUtils.encodeBase64(clientCert.getSerialNumber().toByteArray());

        System.out.println("Original File: " + certfilename);
        System.out.println("ski: " + ski);
        System.out.println("serial: " + serial);
        System.out.println("subject DN: " + CertUtils.getIssuerDN(clientCert));
        System.out.println("Issuer DN: " + CertUtils.getIssuerDN(clientCert));
        System.out.println("Thumbprint: " + CertUtils.getCertificateFingerprint(clientCert, CertUtils.ALG_SHA1, CertUtils.FINGERPRINT_BASE64));
        System.out.println("Cert: ");
        System.out.println(HexUtils.encodeBase64(clientCert.getEncoded()));

    }

    private static void printUsageAndExit() {
        System.err.println("com.l7tech.util.CertUtilUtils usage:");
        System.err.println("<jar path> <cert file location>");
        System.exit(0);
    }
}
