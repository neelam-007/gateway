package com.l7tech.policy.server;

import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.l7tech.util.Locator;
import com.l7tech.logging.LogManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
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
 * http://localhost:8080/ssg/policy/disco.modulator?urn=blah&soapaction=ugh
 *
 */
public class PolicyServlet extends HttpServlet {
    private static final String CERT_PATH = "../../kstores/ssg.cer";

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        // GET THE PARAMETERS PASSED
        String str_oid = httpServletRequest.getParameter("serviceoid");
        String urnParameter = httpServletRequest.getParameter("urn");
        String getCert = httpServletRequest.getParameter("getcert");
        String username = httpServletRequest.getParameter("username");
        String soapactionParamater = httpServletRequest.getParameter("soapaction");

        // See if it's actually a certificate download request
        if (getCert != null) {
            try {
                doCertDownload(httpServletRequest, httpServletResponse, username);
            } catch (Exception e) {
                  throw new ServletException("Unable to fulfil cert request", e);
            }
            return;
        }

        // RESOLVE THE SERVICE
        PublishedService targetService = null;
        if (str_oid != null && str_oid.length() > 0) targetService = resolveService(Long.parseLong(str_oid));
        else targetService = resolveService(urnParameter, soapactionParamater);
        // OUTPUT THE POLICY
        outputPublishedServicePolicy(targetService, httpServletResponse);
    }

    private void sendError(HttpServletResponse response, Exception e) {
        response.setStatus(500);
        response.setContentType("text/plain");
        try {
            e.printStackTrace(new PrintStream(response.getOutputStream()));
            response.flushBuffer();
        } catch (IOException e1) {
        }
    }

    /**
     * Slurp a stream into a byte array and return it.
     * @param stream
     * @param maxSize maximum size to read
     * @return a byte array.
     */
    private byte[] slurpStream(InputStream stream, int maxSize) throws IOException {
        byte[] bb = new byte[maxSize];
        int remaining = maxSize;
        int offset = 0;
        for (;;) {
            int n = stream.read(bb, offset, remaining);
            offset += n;
            remaining -= n;
            if (n < 1 || remaining < 1) {
                byte[] ret = new byte[maxSize - remaining];
                System.arraycopy(bb, 0, ret, 0, offset);
                return ret;
            }
        }
        /* NOTREACHED */
    }

    /**
     * Look up our certificate and transmit it to the client in PKCS#7 format.
     * If a username is given, we'll include a "Cert-Check: " header containing
     * SHA1(cert . H(A1)).  (where H(A1) is the SHA1 of the Base64'ed "username:password".)
     */
    private void doCertDownload(HttpServletRequest request, HttpServletResponse response, String username)
            throws SQLException, TransactionException, FindException, IOException, NoSuchAlgorithmException
    {
        // Find our certificate
        String gotpath = request.getSession().getServletContext().getRealPath(CERT_PATH);
        InputStream certStream = new FileInputStream(gotpath);
        byte[] cert;
        try {
            cert = slurpStream(certStream, 16384);
        } finally {
            certStream.close();
        }

        // Insert Cert-Check: header if we can.
        if (username != null) {
            User j = this.findUser(username.trim());
            if (j != null) {
                String ha1 = j.getPassword();

                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.reset();
                md5.update(cert);
                md5.update(ha1.getBytes());
                response.addHeader("Cert-Check", encodeDigest(md5.digest()));
                response.addHeader("Cert-Check-Status", "Ok");
            } else {
                response.addHeader("Cert-Check-Status", "User Unknown");
            }
        }

        response.setStatus(200);
        response.setContentType("application/x-x509-ca-cert");
        response.setContentLength(cert.length);
        response.getOutputStream().write(cert);
        response.flushBuffer();
    }

    /**
     * Encodes the 128 bit (16 bytes) MD5 into a 32 character String.
     *
     * @param binaryData Array containing the digest
     * @return A String containing the encoded MD5, or empty string if encoding failed
     */
    private static String encodeDigest(byte[] binaryData) {
        if (binaryData == null) return "";

        char[] hexadecimal ={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (binaryData.length != 16) return "";

        char[] buffer = new char[32];

        for (int i = 0; i < 16; i++) {
            int low = (binaryData[i] & 0x0f);
            int high = ((binaryData[i] & 0xf0) >> 4);
            buffer[i*2] = hexadecimal[high];
            buffer[i*2 + 1] = hexadecimal[low];
        }
        return new String(buffer);
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

    private PublishedService resolveService(String urn, String soapAction) {
        // todo
        return null;
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

    /**
     * Try to find a user given only the username.  This sort of sucks -- we have no way of knowing
     * which user we are looking for.
     *
     * We'll try the internal identity provider first, and if it fails, search all others for the user.
     *
     * @param username
     * @return
     * @throws FindException
     */
    private User findUser(String username) throws FindException {
        IdentityProviderConfigManager configManager = new IdProvConfManagerServer();

        try {
            try {
                User user = configManager.getInternalIdentityProvider().getUserManager().findByLogin(username);
                if (user != null)
                    return user;
            } catch (FindException e) {
                LogManager.getInstance().getSystemLogger().log(Level.WARNING, null, e);
                            // swallow, we'll try more stuff below
            }

            Collection idps = configManager.findAllIdentityProviders();
            for (Iterator i = idps.iterator(); i.hasNext();) {
                IdentityProvider provider = (IdentityProvider) i.next();
                try {
                    User user = provider.getUserManager().findByLogin(username);
                    if (user != null)
                        return user;
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

        return null;
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

