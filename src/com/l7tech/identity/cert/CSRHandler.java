package com.l7tech.identity.cert;

import com.l7tech.common.util.HexUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;


/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jul 25, 2003
 *
 * Servlet which handles the CSR requests coming from the Client Proxy. Must come
 * through ssl and must contain valid credentials embedded in basic auth header.
 *
 */
public class CSRHandler extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.isSecure()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR requests must come through ssl port");
            return;
        }
        // todo, authenticate user
        byte[] csr = readCSRFromRequest(request);
        Certificate cert = sign(csr);
        try {
            byte[] certbytes = cert.getEncoded();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/x-x509-ca-cert");
            response.setContentLength(certbytes.length);
            response.getOutputStream().write(certbytes);
            response.flushBuffer();
        } catch (CertificateEncodingException e) {
            // todo, send 500
        }
    }

    private byte[] readCSRFromRequest(HttpServletRequest request) throws IOException {
        byte[] b64Encoded = HexUtils.slurpStream(request.getInputStream(), 16384);
        String tmpStr = new String(b64Encoded);
        String beginKey = "-----BEGIN CERTIFICATE REQUEST-----";
        String endKey = "-----END CERTIFICATE REQUEST-----";
        int beggining = tmpStr.indexOf(beginKey) + beginKey.length();
        int end = tmpStr.indexOf(endKey);
        return tmpStr.substring(beggining, end).getBytes();
    }

    private Certificate sign(byte[] csr) {
        // todo, user RSA signer
        return null;
    }
}
