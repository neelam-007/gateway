package com.l7tech.identity.cert;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.AuthenticatableHttpServlet;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;


/**
 * Servlet which handles the CSR requests coming from the Client Proxy. Must come
 * through ssl and must contain valid credentials embedded in basic auth header.
 *
 * When the node handling the csr request does not have access to the master key, it tries
 * to forward the request to the master node. In order for this to work, the root cert of the
 * cluster must be previously inserted in the trust store of this node
 * ($JAVA_HOME/jre/lib/security/cacerts).
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jul 25, 2003
 */
public class CSRHandler extends AuthenticatableHttpServlet {

    public void init( ServletConfig config ) throws ServletException {
        super.init(config);

        String tmp = getServletConfig().getInitParameter("RootKeyStore");
        if (tmp == null || tmp.length() < 1) tmp = "../../kstores/ssgroot";
        rootkstore = KeystoreUtils.getInstance().getRootKeystorePath();
        rootkstorepasswd = KeystoreUtils.getInstance().getRootKeystorePasswd();
        rootkstoretype = KeystoreUtils.getInstance().getKeyStoreType();
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
            proxyReqToSsgWithRootKStore(request, response);
            return;
        }

        try {
            // Authentication
            List users = null;
            try {
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
        return HexUtils.decodeBase64(b64str);
    }

    private Certificate sign(byte[] csr) throws Exception {
        RsaCertificateSigner signer = getSigner();
        // todo, refactor RsaCertificateSigner to throw more precise exceptions
        Certificate cert = signer.createCertificate(csr);
        return cert;
    }

    private RsaCertificateSigner getSigner() {
        return new RsaCertificateSigner(rootkstore, rootkstorepasswd, "ssgroot", rootkstorepasswd, rootkstoretype);
    }

    private boolean keystorePresent() {
        if (rootkstore == null) return false;
        // check that the file in rootkstore exists
        return (new File(rootkstore)).exists();
    }

    private boolean isRequestAlreadyRoutedFromOtherPeer(HttpServletRequest req) {
        String isalreadyrouted = req.getHeader(ROUTED_FROM_PEER);
        if (isalreadyrouted != null && isalreadyrouted.length() > 0) {
            return true;
        }
        return false;
    }

    private void proxyReqToSsgWithRootKStore(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // these requests should not be routed more than once!
        if (isRequestAlreadyRoutedFromOtherPeer(req)) {
            String msg = "could not get root key for signing";
            logger.warning(msg + ". request re-routed!");
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        // look for ip address of master server
        ClusterInfoManager manager = ClusterInfoManager.getInstance();
        Collection clusterNodes = null;
        try {
            clusterNodes = manager.retrieveClusterStatus();
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot get cluster info", e);
            clusterNodes = null;
        }
        if (clusterNodes == null) {
            String msg = "could not get root key for signing";
            logger.warning(msg);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }
        String masterhostname = null;
        for (Iterator i = clusterNodes.iterator(); i.hasNext();) {
            ClusterNodeInfo node = (ClusterNodeInfo)i.next();
            if (node.getIsMaster()) {
                masterhostname = node.getAddress();
                break;
            }
        }
        if (masterhostname == null) {
            String msg = "could not get root key for signing";
            logger.warning(msg);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        logger.finest("redirecting request to master " + masterhostname);
        // reconstruct url with master server host name
        int pork = req.getServerPort();
        String protocol = "https";
        if (pork == 8080 || pork == 80) protocol = "http";
        String url = protocol + "://" + masterhostname + ":" + pork + req.getRequestURI();
        if (req.getQueryString() != null && req.getQueryString().length() > 0) {
            url += "?" + req.getQueryString();
        }
        logger.finest("using url " + url);

        // using HttpURLConnection
        HttpURLConnection connection = (HttpURLConnection)(new URL(url)).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        try {
            // loose hostname verifier
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection)connection).setHostnameVerifier(
                  new HostnameVerifier() {
                      public boolean verify(String s, SSLSession sslSession) {
                          return true;
                      }
                  });
            } else {
                // this should not happen
                logger.severe("non https connection(?): " + connection.getClass().getName());
            }

            connection.setRequestProperty(CONTENT_TYPE, req.getContentType());
            connection.setRequestProperty(ROUTED_FROM_PEER, "Yes");
            connection.setRequestProperty(AUTH_HEADER_NAME, req.getHeader(AUTH_HEADER_NAME));
            OutputStream outputstream = connection.getOutputStream();
            InputStream inputSream = req.getInputStream();
            byte[] buff = new byte[256];
            // send the actual csr request
            int read = inputSream.read(buff);
            while (read > 0) {
                outputstream.write(buff, 0, read);
                read = inputSream.read(buff);
            }
            inputSream.close();
            outputstream.close();
            int status = connection.getResponseCode();
            res.setStatus(status);
            res.setContentType(connection.getContentType());
            // get response and send back
            inputSream = connection.getInputStream();
            outputstream = res.getOutputStream();
            read = inputSream.read(buff);
            while (read > 0) {
                outputstream.write(buff, 0, read);
                read = inputSream.read(buff);
            }
            inputSream.close();
            outputstream.close();
        } catch (SSLHandshakeException e) {
            logger.severe("forwarding this csr to the master failed because this node does not " +
                          "have the root cert in its trusted store. import root.cer into " +
                          "JAVA_HOME/jre/lib/security/cacerts " + e.getMessage());
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "cannot forward csr to master");
        }

        return;
    }

    private String rootkstore = null;
    private String rootkstorepasswd = null;
    private String rootkstoretype = KeyStore.getDefaultType();

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String AUTH_HEADER_NAME = "Authorization";
    public static final String ROUTED_FROM_PEER = "Routed-From-Peer";
}
