package com.l7tech.server.identity.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.PermissiveHostnameVerifier;
import com.l7tech.common.io.PermissiveSSLSocketFactory;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import org.junit.Ignore;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyPair;

/**
 * User: flascell
 * Date: Jul 29, 2003
 * Time: 3:35:27 PM
 *
 * test the csr handler servlet
 */
@Ignore("Developer test")
public class CSRHandlerTest {
    public static void main (String[] args) throws Exception {
        URL url = new URL("https://localhost:8443/ssg/csr");
        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setHostnameVerifier( new PermissiveHostnameVerifier() );
        connection.setSSLSocketFactory( new PermissiveSSLSocketFactory() );

        String auth = "Basic " + HexUtils.encodeBase64("admin:password".getBytes());
        connection.setRequestProperty("Authorization", auth);


        final KeyPair keyPair = JceProvider.getInstance().generateRsaKeyPair();
        CertificateRequest csr = JceProvider.getInstance().makeCsr("user", keyPair);

        byte[] buf = csr.getEncoded();
        OutputStream os = connection.getOutputStream();
        os.write(buf);
        os.close();

        InputStream response;

        try {
            response = connection.getInputStream();
            if (response != null) {
                byte[] responsecontents = IOUtils.slurpStream(response, 16384);
                java.security.cert.Certificate dledcert = CertUtils.decodeCert(responsecontents);
                System.out.println("Certificate downloaded successfully: " + dledcert.toString());
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        try {
            response = connection.getErrorStream();
            if (response == null) return;
            final byte[] buffer = new byte[1024];
            int read = response.read(buffer);
            System.out.println("ERROR STREAM:");
            while (read > 0) {
                System.out.print(new String(buf, 0, read));
                read = response.read(buf);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
