package com.l7tech.identity.cert;

import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.internal.InternalUserManagerServer;
import com.l7tech.logging.LogManager;
import com.l7tech.util.Locator;
import com.l7tech.util.KeystoreUtils;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.encoding.Base64;
import sun.security.x509.X500Name;


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
        logger = LogManager.getInstance().getSystemLogger();
        String tmp = getServletConfig().getInitParameter("RootKeyStore");
        if (tmp == null || tmp.length() < 1) tmp = "../../kstores/ssgroot";
        rootkstore = KeystoreUtils.getInstance().getRootKeystorePath();
        rootkstorepasswd = KeystoreUtils.getInstance().getRootKeystorePasswd();
        if (rootkstorepasswd == null || rootkstorepasswd.length() < 1) {
            logger.log(Level.SEVERE, "Key store password not found (root CA).");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // make sure we come in through ssl
        if (!request.isSecure()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR requests must come through ssl port");
            return;
        }

        // if the kstore cannot be found, try to proxy the request to the ssg that has the kstore
        if (!keystorePresent()) {
            proxyRequestToSsgThatHasRootKStore(request, response);
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
                throw new ServletException("cannot decode basic header");
            }
            try {
                authenticatedUser = authenticateBasicToken(decodedAuthValue);
            } catch (FindException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new IOException(e.getMessage());
            }
        }
        if (authenticatedUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must provide valid credentials");
            logger.log(Level.SEVERE, "failed authorization " + tmp);
            return;
        }
        logger.log(Level.INFO, "User " + authenticatedUser.getLogin() + " has authenticated for CSR");

        // check if user is allowed to generate a new cert
        InternalUserManagerServer userMan = (InternalUserManagerServer)getConfigManager().getInternalIdentityProvider().getUserManager();
        try {
            if (!userMan.userCanResetCert(Long.toString(authenticatedUser.getOid()))) {
                logger.log(Level.SEVERE, "user is refused csr: " + authenticatedUser.getLogin());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR Forbidden. Contact your administrator for more info.");
                return;
            }
        } catch (FindException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            logger.log(Level.SEVERE, e.getMessage(), e);
            return;
        }

        byte[] csr = readCSRFromRequest(request);
        Certificate cert = null;

        // sign request
        try {
            cert = sign(csr);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            logger.log(Level.SEVERE, e.getMessage(), e);
            return;
        }

        // record new cert
        try {
            userMan.recordNewCert(Long.toString(authenticatedUser.getOid()), cert);
        } catch (UpdateException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not record cert. " + e.getMessage());
            logger.log(Level.SEVERE, "Could not record cert. " + e.getMessage(), e);
            return;
        }

        // verify that the CN in the subject equals the login name
        X500Name x500name = new X500Name(((X509Certificate)(cert)).getSubjectX500Principal().getName());
        if (!x500name.getCommonName().equals(authenticatedUser.getLogin())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "You cannot scr for a subject different than " + authenticatedUser.getLogin());
            logger.log(Level.SEVERE, "User " + authenticatedUser.getLogin() + " tried to csr for subject other than self (" + x500name.getCommonName() + ")");
            return;
        }

        // send cert back
        try {
            byte[] certbytes = cert.getEncoded();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/x-x509-ca-cert");
            response.setContentLength(certbytes.length);
            response.getOutputStream().write(certbytes);
            response.flushBuffer();
            logger.log(Level.INFO, "sent new cert to user " + authenticatedUser.getLogin() + ". Subject DN=" + ((X509Certificate)(cert)).getSubjectDN().toString());
        } catch (CertificateEncodingException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            logger.log(Level.SEVERE, e.getMessage(), e);
            return;
        }
    }

    private byte[] readCSRFromRequest(HttpServletRequest request) throws IOException {
        // csr request might be based64 or not, we need to see what format we are getting
        byte[] contents = HexUtils.slurpStream(request.getInputStream(), 16384);
        String tmpStr = new String(contents);
        String beginKey = "-----BEGIN NEW CERTIFICATE REQUEST-----";
        int beggining = tmpStr.indexOf(beginKey);
        // the contents is not base64ed and contains the actual bytes
        if (beggining == -1) return contents;
        // otherwise, we need to extract section and unbase64
        beggining += beginKey.length();
        String endKey = "-----END NEW CERTIFICATE REQUEST-----";
        int end = tmpStr.indexOf(endKey);
        if (end == -1) {
            logger.log(Level.SEVERE, "Cannot read csr request (bad format?)");
            return new byte[0];
        }
        String b64str = tmpStr.substring(beggining, end);
        sun.misc.BASE64Decoder base64decoder = new sun.misc.BASE64Decoder();
        return base64decoder.decodeBuffer(b64str);
    }

    private Certificate sign(byte[] csr) throws Exception {
        RSASigner signer = getSigner();
        // todo, refactor RSASigner to throw more precise exceptions
        Certificate cert = signer.createCertificate(csr);
        return cert;
    }

    private User authenticateBasicToken(String value) throws FindException {
        String login = null;
        String clearTextPasswd = null;

        int i = value.indexOf( ':' );
        if (i == -1) {
            throw new FindException("invalid basic credentials " + value);
        }
        else {
            login = value.substring(0, i);
            clearTextPasswd = value.substring(i+1);
        }

        User tmpUser = new User();
        tmpUser.setLogin(login);
        PrincipalCredentials creds = new PrincipalCredentials(tmpUser, clearTextPasswd.getBytes());

        try {
            try {
                getConfigManager().getInternalIdentityProvider().authenticate(creds);
                return creds.getUser();
            } catch (AuthenticationException e) {
                logger.log(Level.SEVERE, "authentication failed for " + login, e);
                return null;
            }
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "error closing context", e);
                throw new FindException("cannot close context", e);
            }
        }
    }

    private synchronized IdentityProviderConfigManager getConfigManager() {
        if (identityProviderConfigManager == null) {
            identityProviderConfigManager = (IdentityProviderConfigManager)Locator.getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);
        }
        return identityProviderConfigManager;
    }

    private /*synchronized*/ RSASigner getSigner() {
        return new RSASigner(rootkstore, rootkstorepasswd, "ssgroot", rootkstorepasswd);
        /*if (rsasigner == null) {
            rsasigner = new RSASigner(rootkstore, rootkstorepasswd, "ssgroot", rootkstorepasswd);
        }
        return rsasigner;
        */
    }

    private boolean keystorePresent() {
        // todo, implement this for cluster configs
        return true;
    }

    private void proxyRequestToSsgThatHasRootKStore(HttpServletRequest request, HttpServletResponse response) {
        // todo, implement this for cluster configs
        return;
    }

    private IdentityProviderConfigManager identityProviderConfigManager = null;
    //private RSASigner rsasigner = null;
    private String rootkstore = null;
    private String rootkstorepasswd = null;
    private Logger logger = null;
}
