package com.l7tech.policy.server;

import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.l7tech.util.Locator;
import com.l7tech.common.util.HexUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;


/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 11, 2003
 *
 * This servlet returns policy documents (type xml).
 * The following parameters can be passed to resolve the PublishedService:
 * serviceoid : the internal object identifier of the PublishedService. if specified, this parameter is sufficient to
 *              retrieve the policy
 * urn : the urn of the service. if more than one service have the same urn, at least one more paramater will be
 *       necessary
 * soapaction : the soapaction of the PublishedService
 *
 * Pass the parameters as part of the url as in the samples below
 * http://localhost:8080/ssg/policy/disco.modulator?serviceoid=666
 *
 */
public class PolicyServlet extends HttpServlet {
    public static final String DEFAULT_CERT_PATH = "../../kstores/ssg.cer";
    public static final String PARAM_CERT_PATH = "CertPath";

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        // GET THE PARAMETERS PASSED
        String str_oid = httpServletRequest.getParameter("serviceoid");
        String getCert = httpServletRequest.getParameter("getcert");
        String username = httpServletRequest.getParameter("username");
        String nonce = httpServletRequest.getParameter("nonce");

        // See if it's actually a certificate download request
        if (getCert != null) {
            try {
                doCertDownload(httpServletRequest, httpServletResponse, username, nonce);
            } catch (Exception e) {
                  throw new ServletException("Unable to fulfil cert request", e);
            }
            return;
        }

        // RESOLVE THE SERVICE
        PublishedService targetService = null;
        if (str_oid != null && str_oid.length() > 0)
            targetService = resolveService(Long.parseLong(str_oid));
        // OUTPUT THE POLICY
        outputPublishedServicePolicy(targetService, httpServletResponse);
    }

    /**
     * Look up our certificate and transmit it to the client in PKCS#7 format.
     * If a username is given, we'll include a "Cert-Check: " header containing
     * SHA1(cert . H(A1)).  (where H(A1) is the SHA1 of the Base64'ed "username:password".)
     */
    private void doCertDownload(HttpServletRequest request, HttpServletResponse response,
                                String username, String nonce)
            throws FindException, IOException, NoSuchAlgorithmException
    {
        // Find our certificate
        String certPath = getServletConfig().getInitParameter( PARAM_CERT_PATH );
        if ( certPath == null || certPath.length() == 0 ) certPath = DEFAULT_CERT_PATH;

        String gotpath = request.getSession().getServletContext().getRealPath( certPath );
        InputStream certStream = new FileInputStream(gotpath);
        byte[] cert;
        try {
            cert = HexUtils.slurpStream(certStream, 16384);
        } finally {
            certStream.close();
        }

        // Insert Cert-Check-NNN: headers if we can.
        if (username != null) {
            ArrayList checks = findCheckInfos(username);
            for (Iterator i = checks.iterator(); i.hasNext();) {
                CheckInfo info = (CheckInfo) i.next();

                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.reset();
                if (nonce != null)
                    md5.update(nonce.getBytes());
                md5.update(String.valueOf(info.idProvider).getBytes());
                md5.update(cert);
                md5.update(info.ha1.getBytes());
                response.addHeader("Cert-Check-" + info.idProvider, HexUtils.encodeMd5Digest(md5.digest()));
            }
        }

        response.setStatus(200);
        response.setContentType("application/x-x509-ca-cert");
        response.setContentLength(cert.length);
        response.getOutputStream().write(cert);
        response.flushBuffer();
        LogManager.getInstance().getSystemLogger().log(Level.INFO, "Sent ssl cert: " + gotpath);
    }

    private PublishedService resolveService(long oid) {
        try {
            return getServiceManagerAndBeginTransaction().findByPrimaryKey(oid);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        } finally {
            try {
                endTransaction();
            } catch ( SQLException se ) {
                LogManager.getInstance().getSystemLogger().log(Level.WARNING, null, se);
            } catch ( TransactionException te ) {
                LogManager.getInstance().getSystemLogger().log(Level.WARNING, null, te);
            }
        }
    }

    private void outputPublishedServicePolicy(PublishedService service, HttpServletResponse response) throws IOException {
        if (service == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "ERROR cannot resolve target service");
            return;
        } else {
            response.setContentType("text/xml; charset=utf-8");
            response.getOutputStream().println(service.getPolicyXml());
        }
    }

    private class CheckInfo {
        public CheckInfo(long idp, String h) { idProvider = idp; ha1 = h; }
        public final long idProvider;
        public final String ha1;
    }

    /**
     * Given a username, find all matching users in every registered ID provider
     * and return the corresponding IdProv OID and H(A1) string.
     *
     * @param username
     * @return A collection of CheckInfo instances.
     * @throws FindException if the ID Provider list could not be determined.
     */
    private ArrayList findCheckInfos(String username) throws FindException {
        IdentityProviderConfigManager configManager = new IdProvConfManagerServer();
        ArrayList checkInfos = new ArrayList();

        try {
            Collection idps = configManager.findAllIdentityProviders();
            for (Iterator i = idps.iterator(); i.hasNext();) {
                IdentityProvider provider = (IdentityProvider) i.next();
                try {
                    User user = provider.getUserManager().findByLogin(username.trim());
                    if (user != null)
                        checkInfos.add(new CheckInfo(provider.getConfig().getOid(), user.getPassword()));
                } catch (FindException e) {
                    // Log it and continue
                    LogManager.getInstance().getSystemLogger().log(Level.WARNING, null, e);
                }
            }
        } finally {
            try {
                endTransaction();
            } catch (SQLException se) {
                LogManager.getInstance().getSystemLogger().log(Level.WARNING, null, se);
            } catch (TransactionException te) {
                LogManager.getInstance().getSystemLogger().log(Level.WARNING, null, te);
            }
        }

        return checkInfos;
    }

    private com.l7tech.service.ServiceManager getServiceManagerAndBeginTransaction() throws java.sql.SQLException, TransactionException {
        if (serviceManagerInstance == null){
            initialiseServiceManager();
        }
        PersistenceContext.getCurrent().beginTransaction();
        return serviceManagerInstance;
    }

    private void endTransaction() throws java.sql.SQLException, TransactionException {
        PersistenceContext context = PersistenceContext.getCurrent();
        context.commitTransaction();
        context.close();
    }

    private synchronized void initialiseServiceManager() throws ClassCastException, RuntimeException {
        serviceManagerInstance = (com.l7tech.service.ServiceManager)Locator.getDefault().lookup(com.l7tech.service.ServiceManager.class);
        if (serviceManagerInstance == null) throw new RuntimeException("Cannot instantiate the ServiceManager");
    }

    private ServiceManager serviceManagerInstance = null;
}

