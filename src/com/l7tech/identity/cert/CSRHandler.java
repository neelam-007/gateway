package com.l7tech.identity.cert;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.AuthenticatableHttpServlet;
import sun.security.x509.X500Name;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;


/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jul 25, 2003
 *
 * Servlet which handles the CSR requests coming from the Client Proxy. Must come
 * through ssl and must contain valid credentials embedded in basic auth header.
 *
 */
public class CSRHandler extends AuthenticatableHttpServlet {

    public void init( ServletConfig config ) throws ServletException {
        super.init(config);

        String tmp = getServletConfig().getInitParameter("RootKeyStore");
        if (tmp == null || tmp.length() < 1) tmp = "../../kstores/ssgroot";
        rootkstore = KeystoreUtils.getInstance().getRootKeystorePath();
        rootkstorepasswd = KeystoreUtils.getInstance().getRootKeystorePasswd();
        if (rootkstorepasswd == null || rootkstorepasswd.length() < 1) {
            String msg = "Key store password not found (root CA).";
            logger.log(Level.SEVERE, msg);
            throw new ServletException(msg);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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

        //PersistenceContext pc = null;

        try {
            // Authentication
            List users = null;
            try {
                //pc = PersistenceContext.getCurrent();
                users = authenticateRequestBasic(request);
            } catch (IOException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "authentication error");
                logger.log(Level.SEVERE, "Failed authentication", e);
                return;
            } catch (BadCredentialsException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must provide valid credentials");
                logger.log(Level.SEVERE, "Failed authentication", e);
                return;
            }

            if (users == null || users.size() < 1) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must provide valid credentials");
                logger.warning("CSR Handler called without credentials");
                return;
            } else if (users.size() > 1) {
                String msg = "Ambiguous authentication - credentials valid in more than one identity provider.";
                response.sendError(HttpServletResponse.SC_CONFLICT, msg);
                logger.warning(msg);
                return;
            }

            User authenticatedUser = null;
            authenticatedUser = (User)users.get(0);

            ClientCertManager man = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);
            if (!man.userCanGenCert(authenticatedUser)) {
                logger.log(Level.SEVERE, "user is refused csr: " + authenticatedUser.getLogin());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSR Forbidden." +
                                            " Contact your administrator for more info.");
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
                PersistenceContext.getCurrent().beginTransaction();
                man.recordNewUserCert(authenticatedUser, cert);
            } catch (UpdateException e) {
                String msg = "Could not record cert. " + e.getMessage();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                logger.log(Level.SEVERE, msg, e);
                return;
            } catch (TransactionException e) {
                String msg = "Could not record cert. " + e.getMessage();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                logger.log(Level.SEVERE, msg, e);
                return;
            } catch ( SQLException e ) {
                String msg = "Could not record cert. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                return;
            } finally {
                try {
                    PersistenceContext.getCurrent().commitTransaction();
                } catch (TransactionException te) {
                    logger.log(Level.WARNING, "exception committing new cert update", te);
                } catch (ObjectModelException e) {
                    logger.log(Level.WARNING, "exception committing cert update", e);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "exception committing cert update", e);
                }
            }

            // verify that the CN in the subject equals the login name
            X500Name x500name = new X500Name(((X509Certificate)(cert)).getSubjectX500Principal().getName());
            if (!x500name.getCommonName().equals(authenticatedUser.getLogin())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                   "You cannot scr for a subject different than " +
                                    authenticatedUser.getLogin());
                logger.log(Level.SEVERE, "User " +
                                         authenticatedUser.getLogin() +
                                         " tried to csr for subject other than self (" +
                                         x500name.getCommonName() + ")");
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
                logger.fine("sent new cert to user " + authenticatedUser.getLogin() +
                            ". Subject DN=" + ((X509Certificate)(cert)).getSubjectDN().toString());
            } catch (CertificateEncodingException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                logger.log(Level.SEVERE, e.getMessage(), e);
                return;
            }
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            }
            catch (SQLException e) {
                logger.log(Level.WARNING, "exception closing context", e);
            }
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

    private RSASigner getSigner() {
        return new RSASigner(rootkstore, rootkstorepasswd, "ssgroot", rootkstorepasswd);
    }

    private boolean keystorePresent() {
        // todo, implement this for cluster configs
        return true;
    }

    private void proxyRequestToSsgThatHasRootKStore(HttpServletRequest request, HttpServletResponse response) {
        // todo, implement this for cluster configs
        return;
    }

    private String rootkstore = null;
    private String rootkstorepasswd = null;
}
