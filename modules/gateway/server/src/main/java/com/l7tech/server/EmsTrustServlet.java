package com.l7tech.server;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.gateway.common.admin.AdminLoginResult;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.User;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet used to handle trust bootstrapping for the EMS.
 * <p/>
 * The EMS redirects the user to post to this servlet over SSL.
 * <p/>
 * This servlet receives:
 * <ul>
 * <li>The admin user's client certificate (if any), from the SSL handshake
 * <li>The admin user's username and password.
 * <li>The EMS server's certificate.
 * <li>The EMS server's user ID which corresponds to the admin user's ID on this cluster.
 * </ul>
 */
public class EmsTrustServlet extends AuthenticatableHttpServlet {
    private static final Logger logger = Logger.getLogger(EmsTrustServlet.class.getName());
    private static final String FORM_HTML = "/com/l7tech/server/resources/emstrustform.html";
    private static final String FORM_CSS = "/com/l7tech/server/resources/emstrust.css";
    private static final String FORM_JS = "/com/l7tech/server/resources/emstrust.js";

    private static final String[] FIELDS = {
            "message",             // 2
            "emsid",               // 3
            "emscertpem",          // 4
            "emsusername",         // 5
            "username",            // 6
            "returncookiehash",    // 7
            "returnurl"            // 8
    };

    private TrustedEmsUserManager trustedEmsUserManager;
    private AdminLogin adminLogin;
    private Random random;
    private byte[] secretData;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
        if (applicationContext == null)
            throw new ServletException("Couldn't get WebApplicationContext");
        this.trustedEmsUserManager = getBean("trustedEmsUserManager", TrustedEmsUserManager.class);
        this.adminLogin = getBean("adminLogin", AdminLogin.class);
        this.random = new SecureRandom();
        this.secretData = new byte[32];
        random.nextBytes(secretData);
    }

    private static class FormParams extends TreeMap<String, String> {
        private FormParams() {
            super(String.CASE_INSENSITIVE_ORDER);
        }

        private FormParams(Map<String, String[]> params) throws IOException {
            super(String.CASE_INSENSITIVE_ORDER);
            for (String key : params.keySet()) {
                String val = getSingleValue(params, key);
                put(key, val);
            }
        }

        private static String getSingleValue(Map<String, String[]> map, String key) throws IOException {
            String[] values = map.get(key);
            if (values == null || values.length == 0)
                return null;
            if (values.length > 1)
                throw new IOException("More than one value submitted for field " + key);
            return values[0];
        }
    }

    private void sendError(HttpServletResponse hresp, String message) throws IOException {
        hresp.setStatus(400);
        hresp.setContentType("text/plain; charset=UTF-8");
        hresp.getOutputStream().write(message.getBytes("utf8"));
    }

    private void sendHtml(HttpServletResponse hresp, String html) throws IOException {
        hresp.setContentType("text/html; charset=UTF-8");
        hresp.getOutputStream().write(html.getBytes("utf8"));
    }


    protected void doPost(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        if (!hreq.isSecure()) {
            hresp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        ContentTypeHeader ctype = ContentTypeHeader.parseValue(hreq.getContentType());
        Message req = new Message();
        req.initialize(new ByteArrayStashManager(), ctype, hreq.getInputStream());
        req.attachHttpRequestKnob(new HttpServletRequestKnob(hreq));
        //noinspection unchecked
        FormParams param = new FormParams(req.getHttpRequestKnob().getParameterMap());

        if (isInitialRequest(param)) {
            sendForm(hresp, param);
            return;
        }

        if (checkForCrossSiteRequestForgery(hresp, param, req.getHttpRequestKnob()))
            return;

        handleTrustRequest(req, hresp, param);
    }

    private void handleTrustRequest(Message req, HttpServletResponse hresponse, FormParams param) throws IOException {
        try {
            doHandleTrustRequest(req, hresponse, param);
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "Unable to establish EMS trust: " + ExceptionUtils.getMessage(e), e);
            param.put("message", "Invalid EMS PEM certificate.");
            sendForm(hresponse, param);
        } catch (LoginException e) {
            logger.log(Level.WARNING, "Unable to establish EMS trust: " + ExceptionUtils.getMessage(e), e);
            param.put("message", "Unable to authenticate Gateway user: Invalid username or password.");
            sendForm(hresponse, param);
        } catch (PolicyAssertionException e) {
            logger.log(Level.WARNING, "Unable to establish EMS trust: " + ExceptionUtils.getMessage(e), e);
            param.put("message", "Unable to authenticate.");
            sendForm(hresponse, param);
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Unable to establish EMS trust: " + ExceptionUtils.getMessage(e), e);
            param.put("message", "Unable to access information in database.");
            sendForm(hresponse, param);
        }
    }

    private X509Certificate decodePemCert(FormParams param, String paramName) throws CertificateException {
        String pem = param.get(paramName);
        if (pem == null)
            throw new CertificateException("No PEM Certificate found in field " + paramName);
        try {
            return CertUtils.decodeFromPEM(pem);
        } catch (IOException e) {
            throw new CertificateException(e);
        }
    }

    private void doHandleTrustRequest(Message req, HttpServletResponse hresp, FormParams inParam) throws IOException, CertificateException, PolicyAssertionException, LoginException, ObjectModelException {
        FormParams param = new FormParams();
        for (Map.Entry<String, String> entry : inParam.entrySet())
            param.put(entry.getKey(), stripchars(entry.getValue()));


        String emsId = param.get("emsid");
        X509Certificate emsCert = decodePemCert(param, "emscertpem");
        String emsUsername = param.get("emsusername");
        String username = param.get("username");
        String password = param.get("password");        

        // TODO support client cert auth
        //X509Certificate[] clientCertChain = req.getHttpRequestKnob().getClientCertificate();

        AdminLoginResult result = adminLogin.login(username, password);
        User user = result.getUser();

        trustedEmsUserManager.addUserMapping(user, emsId, emsCert, emsUsername);

        String returnurl = param.get("returnurl");
        URL url;
        if (returnurl != null && ((url = parseUrl(returnurl)) != null)) {
            hresp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            hresp.addHeader("Location", url.toExternalForm());
            sendHtml(hresp, "<a href=\"" + url.toExternalForm() + "\">Click here to continue</a>");
            return;
        }

        sendHtml(hresp, "<p>The specified user mapping has been configured.</p>");
    }

    private URL parseUrl(String urlstr) {
        try {
            return urlstr == null ? null : new URL(urlstr);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private boolean isInitialRequest(FormParams param) {
        // Check if this is an initial request
        String formDisplayed = param.get("formdisplayed");
        return (formDisplayed == null || !"1".equals(formDisplayed));
    }

    private void sendForm(HttpServletResponse hresp, FormParams param) throws IOException {
        // Send back prefilled form for confirmation from user
        hresp.setStatus(200);
        hresp.setContentType("text/html");
        sendTemplate(hresp.getOutputStream(), param);
    }

    // Returns true if the request has already been handled and an error emitted.  Throws if inconsistency detected.
    private boolean checkForCrossSiteRequestForgery(HttpServletResponse hresp, FormParams param, HttpRequestKnob hrk) throws IOException, ServletException {
        // Perform CSRF verification.
        // This requires that a random cookie named "returncookie", sent with the original form response but NOT
        // included in the form, be returned to use as BOTH a cookie "returncookie" and a form POST field named
        // "returncookie".  Further, a form field "returncookiehash", returned with the original form, must
        // be included in the request, and must match the hash of the returned cookie with our secret value.
        HttpCookie returnCookie = CookieUtils.findSingleCookie(hrk.getCookies(), "returncookie");
        if (returnCookie == null) {
            sendError(hresp, "Cookies required.  Please enable HTTP cookies for this domain and then retry.");
            return true;
        }
        String fromCookie = returnCookie.getCookieValue();
        String fromForm = param.get("returncookie");
        if ("nojavascript".equals(fromForm)) {
            sendError(hresp, "JavaScript required.  Please enable JavaScript for this domain and then retry.");
            return true;
        }

        if (fromCookie == null || fromForm == null)
            throw new ServletException("Missing return cookie");
        fromCookie = fromCookie.toLowerCase().trim();
        fromForm = fromForm.toLowerCase().trim();
        if (fromCookie.length() < 1 || fromForm.length() < 1 || !fromCookie.equals(fromForm)) {
            logger.log(Level.WARNING, "Missing or mismatching return cookie");
            sendError(hresp, "Invalid request.  Please retry the operation.");
            return true;
        }
        String hashFromForm = param.get("returncookiehash");
        if (!computeReturnCookieHash(fromForm).equals(hashFromForm)) {
            logger.log(Level.WARNING, "Mismatching return cookie hash");
            sendError(hresp, "This request has expired.  Please retry the operation.");
            return true;
        }
        return false;
    }

    private String createReturnCookie() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return new BigInteger(bytes).abs().toString(16);
    }

    private String computeReturnCookieHash(String returnCookie) {
        return HexUtils.hexDump(HexUtils.getSha512Digest(new byte[][] { secretData, returnCookie.getBytes() })).toLowerCase();
    }

    protected void doGet(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        if (!hreq.isSecure()) {
            hresp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Send back a form to be resubmitted
        hresp.setStatus(200);
        String returnCookie = createReturnCookie();
        Cookie cook = new Cookie("returncookie", returnCookie);
        cook.setSecure(true);
        hresp.addCookie(cook);
        hresp.setContentType("text/html");
        Map<String, String> params = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        params.put("message", "Enter details about the Enterprise Manager Server that you wish to use to manage this Gateway.");
        params.put("returncookiehash", computeReturnCookieHash(returnCookie));
        sendTemplate(hresp.getOutputStream(), params);
    }

    private void sendTemplate(OutputStream os, Map<String, String> params) throws IOException {
        PrintStream ps = new PrintStream(os);
        try {
            String template = loadStringResource(FORM_HTML);
            String css = loadStringResource(FORM_CSS);
            String js = loadStringResource(FORM_JS);

            List<String> vals = new ArrayList<String>();
            vals.add(0, css);
            vals.add(1, js);
            for (String fields : FIELDS)
                vals.add(stripchars(params.get(fields)));

            ps.print(MessageFormat.format(template, vals.toArray()));
        } finally {
            ps.flush();
        }
    }

    private String stripchars(String string) {
        return string == null ? "" : string.replaceAll("[^a-zA-Z0-9\\-\\r\\n +/=.,:;]", "");
    }

    private String loadStringResource(String resourcePath) throws IOException {
        Class c = getClass();
        URL url = c.getResource(resourcePath);
        if (url == null)
            throw new MissingResourceException("Missing resource", c.getName(), resourcePath);
        return new String(IOUtils.slurpUrl(url), "UTF-8");
    }

    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_MESSAGEPROCESSOR;
    }

    protected SsgConnector.Endpoint getRequiredEndpoint() {
        return SsgConnector.Endpoint.ADMIN_REMOTE;
    }
}
