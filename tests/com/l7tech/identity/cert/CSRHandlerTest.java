package com.l7tech.identity.cert;

import com.l7tech.common.util.HexUtils;

import java.net.*;
import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;

/**
 * User: flascell
 * Date: Jul 29, 2003
 * Time: 3:35:27 PM
 *
 * test the csr handler servlet
 */
public class CSRHandlerTest {
    public static void main (String[] args) throws Exception {
        System.setProperty("javax.net.ssl.trustStore", System.getProperties().getProperty("user.home") + File.separator + ".l7tech" + File.separator + "trustStore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        URL url = new URL("https://riker:8443/csr");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        // authentication for ssgadmin/ssgadminpasswd
        connection.setRequestProperty("Authorization", "Basic c3NnYWRtaW46c3NnYWRtaW5wYXNzd2Q=");

        FileInputStream fis = new FileInputStream("/home/flascell/bogus.pkcs");
        byte[] buf = new byte[256];
        int read = fis.read(buf);
        OutputStream os = connection.getOutputStream();
        while (read > 0) {
            os.write(buf, 0, read);
            read = fis.read(buf);
        }
        os.close();

        InputStream response = null;

        try {
            response = connection.getInputStream();
            if (response != null) {
                byte[] responsecontents = HexUtils.slurpStream(response, 16384);
                ByteArrayInputStream bais = new ByteArrayInputStream(responsecontents);
                java.security.cert.Certificate dledcert = CertificateFactory.getInstance("X.509").generateCertificate(bais);
                System.out.println("Certificate downloaded successfully: " + dledcert.toString());
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        try {
            response = connection.getErrorStream();
            if (response == null) return;
            read = response.read(buf);
            System.out.println("ERROR STREAM:");
            while (read > 0) {
                System.out.print(new String(buf));
                read = response.read(buf);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
