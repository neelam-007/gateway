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
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.isSecure()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR requests must come through ssl port");
            return;
        }

        // Process the Auth stuff in the headers
        String tmp = request.getHeader(HTTPConstants.HEADER_AUTHORIZATION);
        if (tmp != null ) tmp = tmp.trim();
        User authenticatedUser = null;
        // TRY BASIC AUTHENTICATION
        if (tmp != null && tmp.startsWith("Basic ")) {
            String decodedAuthValue = new String(Base64.decode(tmp.substring(6)));
            if (decodedAuthValue == null) {
                throw new AxisFault("cannot decode basic header");
            }
            authenticatedUser = authenticateBasicToken(decodedAuthValue);
        }
        if (authenticatedUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must provide valid credentials");
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "failed authorization " + tmp);
        }

        // check if user is allowed to generate a new cert
        // todo
        // ClientCertManager.userCanGenCert ...

        byte[] csr = readCSRFromRequest(request);
        Certificate cert = null;
        try {
            cert = sign(csr);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
        }

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

    private Certificate sign(byte[] csr) throws Exception {
        RSASigner signer = getSigner();
        // todo, refactor RSASigner to throw more precise exceptions
        return signer.createCertificate(csr);
    }

    private User authenticateBasicToken(String value) throws AxisFault {
        String login = null;
        int i = value.indexOf( ':' );
        if (i == -1) login = value;
        else login = value.substring( 0, i);

        // MD5 IT
        java.security.MessageDigest md5Helper = null;
        try {
            md5Helper = java.security.MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AxisFault(e.getMessage());
        }
        byte[] digest = md5Helper.digest(value.getBytes());
        String md5edDecodedAuthValue = HexUtils.encodeMd5Digest(digest);

        // COMPARE TO THE VALUE IN INTERNAL DB
        com.l7tech.identity.User internalUser = findUserByLogin(login);
        if (internalUser == null) {
            throw new AxisFault("User " + login + " not registered in internal id provider");
        }
        if (internalUser.getPassword() == null) {
            throw new AxisFault("User " + login + "does not have a password");
        }
        if (internalUser.getPassword().equals(md5edDecodedAuthValue)) {
            // AUTHENTICATION SUCCESS, move on to authorization
            return internalUser;
        } else {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "authentication failure for user " + login + " with credentials " + md5edDecodedAuthValue);
        }
        return null;
    }

    /**
     * returns null if user does not exist
     */
    private User findUserByLogin(String login) {
        try {
            User output = getConfigManager().getInternalIdentityProvider().getUserManager().findByLogin(login);
            PersistenceContext.getCurrent().close();
            return output;
        } catch (FindException e) {
            // not throwing this on purpose, a FindException might just mean that the user does not exist,
            // in which case null is a valid answer
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "exception finding user by login", e);
            return null;
        } catch (SQLException e) {
            // not throwing this on purpose, a FindException might just mean that the user does not exist,
            // in which case null is a valid answer
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "exception closing context", e);
            return null;
        } catch (ObjectModelException e) {
            // not throwing this on purpose, a FindException might just mean that the user does not exist,
            // in which case null is a valid answer
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "exception closing context", e);
            return null;
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
            rsasigner = new RSASigner("/usr/java/jakarta-tomcat-4.1.24/kstores/ssgroot", "password", "ssgroot", "password");
        }
        return rsasigner;
    }

    private IdentityProviderConfigManager identityProviderConfigManager = null;
    private RSASigner rsasigner = null;
}
