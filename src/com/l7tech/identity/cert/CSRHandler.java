package com.l7tech.identity.cert;

import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.logging.LogManager;
import com.l7tech.util.Locator;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.sql.SQLException;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.encoding.Base64;
import org.apache.axis.AxisFault;


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

    public void init( ServletConfig config ) throws ServletException {
        super.init(config);
        rootkstore = getServletConfig().getInitParameter("RootKeyStore");
        rootkstorepasswd = getServletConfig().getInitParameter("RootKeyStorePasswd");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.isSecure()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR requests must come through ssl port");
            return;
        }

        // Process the Auth stuff in the headers
        /*String tmp = request.getHeader(HTTPConstants.HEADER_AUTHORIZATION);
        if (tmp != null ) tmp = tmp.trim();
        User authenticatedUser = null;
        // TRY BASIC AUTHENTICATION
        if (tmp != null && tmp.startsWith("Basic ")) {
            String decodedAuthValue = new String(Base64.decode(tmp.substring(6)));
            if (decodedAuthValue == null) {
                throw new AxisFault("cannot decode basic header");
            }
            try {
                authenticatedUser = authenticateBasicToken(decodedAuthValue);
            } catch (FindException e) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
                throw new IOException(e.getMessage());
            }
        }
        if (authenticatedUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must provide valid credentials");
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "failed authorization " + tmp);
        }*/

        // check if user is allowed to generate a new cert
        // todo
        // ClientCertManager.userCanGenCert ...

        byte[] csr = readCSRFromRequest(request);
        Certificate cert = null;

        // sign request
        try {
            cert = sign(csr);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            return;
        }

        // record cert
        // todo
        // ClientCertManager.recordCert

        // send cert back
        try {
            byte[] certbytes = cert.getEncoded();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/x-x509-ca-cert");
            response.setContentLength(certbytes.length);
            response.getOutputStream().write(certbytes);
            response.flushBuffer();
        } catch (CertificateEncodingException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            return;
        }
    }

    private byte[] readCSRFromRequest(HttpServletRequest request) throws IOException {
        byte[] b64Encoded = HexUtils.slurpStream(request.getInputStream(), 16384);
        String tmpStr = new String(b64Encoded);
        String beginKey = "-----BEGIN CERTIFICATE REQUEST-----";
        String endKey = "-----END CERTIFICATE REQUEST-----";
        int beggining = tmpStr.indexOf(beginKey) + beginKey.length();
        int end = tmpStr.indexOf(endKey);
        // return tmpStr.substring(beggining, end).getBytes();
        // test unbased 64 the string
        String b64str = tmpStr.substring(beggining, end);
        sun.misc.BASE64Decoder base64decoder = new sun.misc.BASE64Decoder();
        return base64decoder.decodeBuffer(b64str);
    }

    private Certificate sign(byte[] csr) throws Exception {
        RSASigner signer = getSigner();
        // todo, refactor RSASigner to throw more precise exceptions
        return signer.createCertificate(csr);
    }

    private User authenticateBasicToken(String value) throws FindException {
        String login = null;
        int i = value.indexOf( ':' );
        if (i == -1) login = value;
        else login = value.substring( 0, i);

        // MD5 IT
        java.security.MessageDigest md5Helper = null;
        try {
            md5Helper = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Exception in java.security.MessageDigest.getInstance", e);
            throw new FindException(e.getMessage(), e);
        }
        byte[] digest = md5Helper.digest(value.getBytes());
        String md5edDecodedAuthValue = HexUtils.encodeMd5Digest(digest);

        // COMPARE TO THE VALUE IN INTERNAL DB
        com.l7tech.identity.User internalUser = findUserByLogin(login);
        if (internalUser == null) throw new FindException("User " + login + " not found in id provider");
        if (internalUser.getPassword() == null) throw new FindException("User " + login + "does not have a password");
        if (internalUser.getPassword().equals(md5edDecodedAuthValue)) {
            // AUTHENTICATION SUCCESS, move on to authorization
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "User " + login + " authenticated successfully");
            return internalUser;
        } else {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "authentication failure for user " + login + " with credentials " + md5edDecodedAuthValue);
        }
        return null;
    }

    /**
     * returns null if user does not exist
     */
    private User findUserByLogin(String login) throws FindException {
        try {
            User output = getConfigManager().getInternalIdentityProvider().getUserManager().findByLogin(login);
            PersistenceContext.getCurrent().close();
            return output;
        } catch (SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "exception closing context", e);
            throw new FindException(e.getMessage(), e);
        } catch (ObjectModelException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "exception closing context", e);
            throw new FindException(e.getMessage(), e);
        }
    }

    private synchronized IdentityProviderConfigManager getConfigManager() {
        if (identityProviderConfigManager == null) {
            identityProviderConfigManager = (IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
        }
        return identityProviderConfigManager;
    }

    private synchronized RSASigner getSigner() {
        if (rsasigner == null) {
            rsasigner = new RSASigner(rootkstore, rootkstorepasswd, "ssgroot", rootkstorepasswd);
        }
        return rsasigner;
    }

    private IdentityProviderConfigManager identityProviderConfigManager = null;
    private RSASigner rsasigner = null;
    private String rootkstore = null;
    private String rootkstorepasswd = null;
}
