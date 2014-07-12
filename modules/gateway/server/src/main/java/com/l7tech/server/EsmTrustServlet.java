package com.l7tech.server;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.gateway.common.admin.AdminLoginResult;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.User;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.admin.AdminLoginHelper;
import com.l7tech.util.*;
import com.l7tech.xml.saml.SamlAssertion;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import x0Assertion.oasisNamesTcSAML2.AssertionType;
import x0Assertion.oasisNamesTcSAML2.AttributeStatementType;
import x0Assertion.oasisNamesTcSAML2.AttributeType;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet used to handle trust bootstrapping for the ESM.
 * <p/>
 * The ESM redirects the user to post to this servlet over SSL.
 * <p/>
 * This servlet receives:
 * <ul>
 * <li>The admin user's client certificate (if any), from the SSL handshake
 * <li>The admin user's username and password.
 * <li>The ESM server's certificate.
 * <li>The ESM server's user ID which corresponds to the admin user's ID on this cluster.
 * </ul>
 */
public class EsmTrustServlet extends AuthenticatableHttpServlet {
    private static final Logger logger = Logger.getLogger(EsmTrustServlet.class.getName());
    private static final String ERROR_HTML = "/com/l7tech/server/resources/esmtrusterror.html";
    private static final String FORM_HTML = "/com/l7tech/server/resources/esmtrustform.html";
    private static final String FORM_CSS = "/com/l7tech/server/resources/esmtrust.css";
    private static final String FORM_JS = "/com/l7tech/server/resources/esmtrust.js";
    private static final String SERVLET_REQUEST_ATTR_X509CERTIFICATE = "javax.servlet.request.X509Certificate";

    private static final Map<String,String[]> RESOURCES = Collections.unmodifiableMap( new HashMap<String,String[]>(){
        {
            put( "/ssg/esmtrust/favicon.ico", new String[]{"/com/l7tech/server/resources/favicon.ico", "image/png"} );
            put( "/ssg/esmtrust/layer7_logo_small_32x32.png", new String[]{"/com/l7tech/server/resources/layer7_logo_small_32x32.png", "image/png"} );
            put( "/ssg/esmtrust/esmtrust.css", new String[]{ FORM_CSS, "text/css"} );
            put( "/ssg/esmtrust/esmtrust.js", new String[]{ FORM_JS, "text/javascript"} );
         }
    } );

    private static final String[] FIELDS = {
            "message",             // 2
            "esmid",               // 3
            "esmcertpem",          // 4
            "esmusername",         // 5
            "username",            // 6
            "returncookiehash",    // 7
            "returnurl",           // 8
            "esminfo",             // 9
            "esmuserdesc",         // 10
    };

    private static final String ATTR_EM_TRUST = "esmtrust";
    private static final String ATTR_EM_UUID = "EM-UUID";
    private static final String ATTR_EM_USER_UUID = "EM-USER-UUID";
    private static final String ATTR_EM_USER_DESC = "EM-USER-DESC";

    private TrustedEsmManager trustedEsmManager;
    private TrustedEsmUserManager trustedEsmUserManager;
    private AdminLogin adminLogin;
    private AdminLoginHelper adminLoginHelper;
    private LicenseManager licenseManager;
    private static final Random random = new SecureRandom();
    private byte[] secretData;

    @Override
    public void init( final ServletConfig config ) throws ServletException {
        super.init(config);
        this.trustedEsmManager = (TrustedEsmManager) config.getServletContext().getAttribute("trustedEsmManager");
        this.trustedEsmUserManager = (TrustedEsmUserManager) config.getServletContext().getAttribute("trustedEsmUserManager");
        this.adminLogin = (AdminLogin) config.getServletContext().getAttribute("adminLogin");
        this.adminLoginHelper = (AdminLoginHelper) config.getServletContext().getAttribute("adminLoginHelper");
        this.licenseManager = (LicenseManager) config.getServletContext().getAttribute("licenseManager");
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

    private void sendErrorWithoutEscapingMarkup(HttpServletResponse hresp, CharSequence htmlMessage) throws IOException {
        hresp.setStatus(400);
        hresp.setContentType("text/html; charset=UTF-8");
        final ServletOutputStream os = hresp.getOutputStream();
        os.write(MessageFormat.format(loadStringResource(ERROR_HTML), htmlMessage).getBytes(Charsets.UTF8));
    }

    private void sendError(HttpServletResponse hresp, CharSequence textMessage) throws IOException {
        sendErrorWithoutEscapingMarkup(hresp, escapeMarkup(textMessage));
    }

    private void sendHtml(HttpServletResponse hresp, String html) throws IOException {
        hresp.setContentType("text/html; charset=UTF-8");
        hresp.getOutputStream().write(html.getBytes(Charsets.UTF8));
    }

    @Override
    protected void doPost( final HttpServletRequest hreq, final HttpServletResponse hresp) throws ServletException, IOException {
        if (!hreq.isSecure()) {
            hresp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        ContentTypeHeader ctype = ContentTypeHeader.parseValue(hreq.getContentType());
        Message req = new Message();
        req.initialize(new ByteArrayStashManager(), ctype, hreq.getInputStream());
        req.attachHttpRequestKnob(new HttpServletRequestKnob(hreq));
        //noinspection unchecked
        FormParams param = new FormParams(req.getHttpRequestKnob().getParameterMap());

        // Check license
        if ( !licenseManager.isFeatureEnabled( GatewayFeatureSets.SERVICE_REMOTE_MANAGEMENT ) ) {
            String returnurl = param.get("returnurl");
            String returnString = "";
            URL url;
            if (returnurl != null && ((url = parseUrl(returnurl)) != null))
                returnString = "\n\n<p><a href=\"" + url.toExternalForm() + "\">Return to the ESM</a>\n";
            sendErrorWithoutEscapingMarkup( hresp, "Not licensed. Please install a Gateway license that enables this feature." + returnString );
            return;
        }

        if (isInitialRequest(param)) {
            // kick out any untrusted params
            param.remove("esmid");
            param.remove("esminfo");
            param.remove("esmcertpem");
            param.remove("esmusername");
            param.remove("esmuserdesc");

            String returnCookie = createReturnCookie();
            Cookie cook = new Cookie("returncookie", returnCookie);
            cook.setSecure(true);
            hresp.addCookie(cook);

            String message = "Enter details about the Enterprise Service Manager that you wish to use to manage this Gateway.";
            String token = param.get("token");
            if ( token != null) {
                try {
                    SamlAssertion assertion = SamlAssertion.newInstance(XmlUtil.parse( new StringReader(token), false ).getDocumentElement());
                    if ( assertion.hasEmbeddedIssuerSignature() ) {
                        assertion.verifyEmbeddedIssuerSignature();
                        XmlObject xmlObject = assertion.getXmlBeansAssertionType();
                        if (xmlObject instanceof AssertionType) {
                            AssertionType assertionType = (AssertionType) xmlObject;

                            if ( assertionType.getConditions() != null ) {
                                Calendar now = Calendar.getInstance();
                                Calendar notBefore = assertionType.getConditions().getNotBefore();
                                Calendar notOnOrAfter = assertionType.getConditions().getNotOnOrAfter();

                                if ( now.after(notBefore) && now.before(notOnOrAfter)) {
                                    String esmid = getAttributeValue( assertionType, ATTR_EM_UUID );
                                    String esmuserid = getAttributeValue( assertionType, ATTR_EM_USER_UUID );
                                    String esmuserdesc = getAttributeValue( assertionType, ATTR_EM_USER_DESC );

                                    if ( esmid == null || esmuserid == null || esmuserdesc == null ) {
                                        logger.warning("Ignoring SAML assertion with missing attributes.");
                                    } else {
                                        X509Certificate cert = assertion.getIssuerCertificate();
                                        if ( hreq.getParameter(ATTR_EM_TRUST) == null ) {
                                            // then it is an account mapping request only, so check the cert is already mapped.
                                            if ( isTrustedEsm(esmid, cert, hresp ) ) {
                                                message = "Enter Gateway credentials to map your Enterprise Service Manager account to your Gateway account.";
                                                param.put("esminfo", "-");
                                            } else {
                                                return;
                                            }
                                        } else {
                                            message = "Enter Gateway credentials if you wish to allow the Enterprise Service Manager shown below to manage this Gateway.";
                                            param.put("esminfo", formatCertInfo(cert));
                                        }

                                        param.put("esmid", esmid);
                                        param.put("esmcertpem", CertUtils.encodeAsPEM(cert));
                                        param.put("esmusername", esmuserid);
                                        param.put("esmuserdesc", esmuserdesc);
                                    }
                                } else {
                                    logger.warning("Ignoring SAML assertion that is expired or not yet valid ('"+notBefore+"'/'"+notOnOrAfter+"')");
                                    sendErrorWithoutEscapingMarkup(hresp, "Unable to establish trust relationship: A security token has expired or is not yet valid.<p>\n\n" +
                                                     "The ESM system clock may be too far off from this Gateway's system clock.<p>\n\n" +
                                                     "Please press your Back button to return.");
                                    return;
                                }
                            }
                        }
                    }
                } catch ( Exception e ) {
                    logger.log( Level.WARNING, "Error processing SAML assertion '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                }
            }

            param.put("message", message );
            param.put("returncookiehash", computeReturnCookieHash(returnCookie));
            sendForm(hresp, param);
            return;
        }

        if (checkForCrossSiteRequestForgery(hresp, param, req.getHttpRequestKnob()))
            return;

        try {
            handleTrustRequest(hreq, hresp, param);
        } catch (Exception e) {
            logger.log(Level.WARNING, "EsmTrustServlet request failed: " + ExceptionUtils.getMessage(e), e);
            sendError(hresp, "Unable to establish trust relationship: " + ExceptionUtils.getMessage(e));
        }
    }

    private boolean isTrustedEsm( final String esmid, final X509Certificate cert, final HttpServletResponse hresp ) throws IOException {
        boolean isOk = false;

        TrustedEsm esm;
        try {
            esm = trustedEsmManager.findEsmById(esmid);

            if ( esm != null && esm.getTrustedCert() != null &&
                 CertUtils.certsAreEqual( esm.getTrustedCert().getCertificate(), cert ) ) {
                isOk = true;
            } else {
                sendError( hresp, "ESM not trusted." );
            }
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error looking up esm for id '"+esmid+"'.", fe );
            sendError( hresp, "Error accessing ESM server information." );
        }

        return isOk;
    }

    private void handleTrustRequest(HttpServletRequest hreq, HttpServletResponse hresponse, FormParams param) throws IOException {
        try {
            doHandleTrustRequest(hreq, hresponse, param);
        } catch (AccessControlException e) {
            logger.log(Level.WARNING, "Unable to establish ESM trust: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            param.put("message", "The specified account does not have sufficient access to this Gateway to create a user mapping.");
            sendForm(hresponse, param);
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "Unable to establish ESM trust: " + ExceptionUtils.getMessage(e), e);
            param.put("message", "Invalid ESM PEM certificate.");
            sendForm(hresponse, param);
        } catch (CertificateMismatchException e) {
            logger.log(Level.WARNING, "Unable to establish ESM trust: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            param.put("message", "The specified ESM ID has already been registered with a different ESM certificate.");
            sendForm(hresponse, param);
        } catch (TrustedEsmUserManager.MappingAlreadyExistsException e) {
            logger.log(Level.INFO, "Unable to establish ESM trust: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            param.put("message", "The specified ESM username on that ESM instance has already been mapped on this Gateway.");
            sendForm(hresponse, param);
        } catch (LoginException e) {
            logger.log(Level.INFO, "Unable to establish ESM trust: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            param.put("message", "Unable to authenticate Gateway user: " + ExceptionUtils.getMessage(e));
            sendForm(hresponse, param);
        } catch (PolicyAssertionException e) {
            logger.log(Level.WARNING, "Unable to establish ESM trust: " + ExceptionUtils.getMessage(e), e);
            param.put("message", "Unable to authenticate.");
            sendForm(hresponse, param);
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Unable to establish ESM trust: " + ExceptionUtils.getMessage(e), e);
            param.put("message", "Unable to access information in database.");
            sendForm(hresponse, param);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to establish ESM trust: " + ExceptionUtils.getMessage(e), e);
            param.put("message", "An unexpected error occurred.");
            sendForm(hresponse, param);
        }
    }

    private X509Certificate decodePemCert(FormParams param, String paramName) throws CertificateException {
        String pem = param.get(paramName);
        if (pem == null || !CertUtils.looksLikePem(pem.getBytes()))
            throw new CertificateException("No PEM Certificate found in field " + paramName);
        try {
            return CertUtils.decodeFromPEM(pem);
        } catch (IOException e) {
            throw new CertificateException(e);
        }
    }

    private X509Certificate[] getClientCertificateChain(HttpServletRequest hreq) throws IOException {
        Object param = hreq.getAttribute(SERVLET_REQUEST_ATTR_X509CERTIFICATE);
        if (param == null)
            return null;
        if (param instanceof X509Certificate)
            return new X509Certificate[] { (X509Certificate)param };
        if (param instanceof X509Certificate[])
            return (X509Certificate[])param;
        throw new IOException("Request X509Certificate was unsupported type " + param.getClass());
    }

    private void doHandleTrustRequest(HttpServletRequest hreq, HttpServletResponse hresp, FormParams inParam) throws Exception {
        FormParams param = new FormParams();
        for (Map.Entry<String, String> entry : inParam.entrySet()) {
            if ( "password".equals(entry.getKey()) ) { // this is the "whitelist" of fields that are not stripped
                param.put(entry.getKey(), entry.getValue());
            } else {
                param.put(entry.getKey(), stripchars(entry.getValue()));
            }
        }

        String esmId = param.get("esmid");
        X509Certificate esmCert = decodePemCert(param, "esmcertpem");
        String esmUsername = param.get("esmusername");
        String esmDesc = param.get("esmuserdesc");
        User user = authenticateUser(hreq, param);

        try {
            configureUserMapping(hreq, esmId, esmCert, esmUsername, esmDesc, user);
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }

        String returnurl = param.get("returnurl");
        URL url;
        if (returnurl != null && ((url = parseUrl(returnurl)) != null)) {
            String redirectUrl = url.toExternalForm();
            String mappingInfo = "username=" +  URLEncoder.encode(param.get("username"), "UTF-8");
            if ( redirectUrl.indexOf('?') > -1 ) {
                mappingInfo = "&" + mappingInfo;
            } else {
                mappingInfo = "?" + mappingInfo;                                    
            }
            redirectUrl += mappingInfo;

            hresp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            hresp.addHeader("Location", redirectUrl);
            sendHtml(hresp, "<a href=\"" + redirectUrl + "\">Click here to continue</a>");
            return;
        }

        sendHtml(hresp, "<p>The specified user mapping has been configured.</p>");
    }

    // Throws LoginException if user can't authenticate.  Supports either password (via the form) or client cert auth
    private User authenticateUser(HttpServletRequest hreq, FormParams param) throws IOException, LoginException {
        X509Certificate[] clientCertChain = getClientCertificateChain(hreq);

        AdminLoginResult result;
        if (clientCertChain != null && clientCertChain[0] != null) {
            // Authenticate with client cert
            result = adminLoginHelper.login(clientCertChain[0]);
        } else {
            // Authenticate with username/password
            String username = param.get("username");
            String password = param.get("password");

            if ( username == null || username.isEmpty() ) {
                throw new LoginException("Empty username");
            }

            result = adminLogin.loginNew(username, password);
        }

        return result.getUser();
    }

    private void configureUserMapping(final HttpServletRequest hreq,
                                      final String esmId,
                                      final X509Certificate esmCert,
                                      final String esmUsername,
                                      final String esmUserDisplayName,
                                      final User user)
            throws PrivilegedActionException
    {
        Subject subject = new Subject();
        subject.getPrincipals().add(user);
        Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {
                RemoteUtils.callWithConnectionInfo(null, hreq, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        trustedEsmUserManager.configureUserMapping(user, esmId, esmCert, esmUsername, esmUserDisplayName);
                        return null;
                    }
                });
                return null;
            }
        });
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

    @Override
    protected void doGet(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        if (!hreq.isSecure()) {
            hresp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if ( handleResourceRequest( hreq, hresp ) ) {
            return;
        }

        // Check license
        if ( !licenseManager.isFeatureEnabled( GatewayFeatureSets.SERVICE_REMOTE_MANAGEMENT ) ) {
            sendError( hresp, "Not licensed. Please install a Gateway license that enables this feature." );
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
        params.put("message", "Enter details about the Enterprise Service Manager that you wish to use to manage this Gateway.");
        params.put("returncookiehash", computeReturnCookieHash(returnCookie));
        sendTemplate(hresp.getOutputStream(), params);
    }

    private void sendTemplate(OutputStream os, Map<String, String> params) throws IOException {
        PrintStream ps = new PrintStream(os);
        try {
            String template = loadStringResource(FORM_HTML);

            List<String> vals = new ArrayList<String>();
            vals.add(0, "");
            vals.add(1, "");
            for (String fields : FIELDS)
                vals.add(escapeMarkup(stripchars(params.get(fields))).toString());

            ps.print(MessageFormat.format(template, vals.toArray()));
        } finally {
            ps.flush();
        }
    }

    private String stripchars(String string) {
        return string == null ? "" : string.replaceAll("[^a-zA-Z0-9\\-\\r\\n +/=.,:;\\_\\?@&!#$%\\^*\\(\\){}'~`]", "");
    }

    private String loadStringResource(String resourcePath) throws IOException {
        Class c = getClass();
        URL url = c.getResource(resourcePath);
        if (url == null)
            throw new MissingResourceException("Missing resource", c.getName(), resourcePath);
        return new String(IOUtils.slurpUrl(url), Charsets.UTF8);
    }

    @Override
    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_MESSAGEPROCESSOR;
    }

    @Override
    protected SsgConnector.Endpoint getRequiredEndpoint() {
        return SsgConnector.Endpoint.ADMIN_REMOTE_ESM;
    }

    private String formatCertInfo( final X509Certificate cert ) throws GeneralSecurityException {
        StringBuilder builder = new StringBuilder();

        builder.append("Issuer       : ");
        builder.append( cert.getIssuerDN().toString() );
        builder.append('\n');
        builder.append("Serial Number: ");
        builder.append( hexFormat(cert.getSerialNumber().toByteArray()) );
        builder.append('\n');
        builder.append("Subject      : ");
        builder.append( cert.getSubjectDN().toString() );
        builder.append('\n');
        builder.append("Thumbprint   : ");
        builder.append( CertUtils.getCertificateFingerprint(cert, "SHA1").substring(5) );

        return builder.toString();
    }

    private String hexFormat( final byte[] bytes ) {
        StringBuilder builder = new StringBuilder();

        for ( int i=0; i<bytes.length; i++ ) {
            String byteHex = HexUtils.hexDump(new byte[]{bytes[i]});
            builder.append(byteHex.toUpperCase());
            if ( i<bytes.length-1 ) {
                builder.append(':');
            }
        }

        return builder.toString();
    }    

    private static String getAttributeValue( final AssertionType assertionType, final String attributeName ) {
        String value = null;

        if ( assertionType.getAttributeStatementArray() != null ) {
            out:
            for ( AttributeStatementType statement : assertionType.getAttributeStatementArray() ) {
                if ( statement != null && statement.getAttributeArray() != null ) {
                    for ( AttributeType attribute : statement.getAttributeArray() ) {
                        if ( attribute != null ) {
                            if ( attributeName.equals( attribute.getName() ) ) {                            
                                XmlObject[] values = attribute.getAttributeValueArray();
                                for ( XmlObject presentedValue : values ) {
                                    XmlCursor cursor = presentedValue.newCursor();
                                    try {
                                        if ( value == null ) {
                                            value = cursor.getTextValue();
                                        } else { // we only expect one value, anything else is invalid
                                            value = null;
                                            break out;
                                        }
                                    } finally {
                                        cursor.dispose();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return value;
    }

    /**
     * Escape any special chars for use in HTML 
     */
    public static CharSequence escapeMarkup( final CharSequence s )
	{
		if ( s == null ) {
			return null;
		} else {
			int len = s.length();
			final StringBuilder buffer = new StringBuilder((int)(len * 1.1));

			for (int i = 0; i < len; i++)
			{
				final char c = s.charAt(i);
				switch (c)
				{
					case '<' :
						buffer.append("&lt;");
						break;

					case '>' :
						buffer.append("&gt;");
						break;

					case '&' :
						if ((i < len - 1) && (s.charAt(i + 1) == '#')) {
							buffer.append(c);
						} else {
							buffer.append("&amp;");
						}
						break;

					case '"' :
						buffer.append("&quot;");
						break;

					case '\'' :
						buffer.append("&#039;");
						break;

					default :
    					buffer.append(c);
						break;
				}
			}

			return buffer;
		}
	}

    /**
     * Handle request for static resource file.
     *
     * @param hreq The HttpServletRequest
     * @param hresp The HttpServletResponse
     * @return true if the request has been handled (so no further action should be taken)
     * @throws java.io.IOException  if there's a problem reading the jar or sending the info
     * @throws javax.servlet.ServletException  if there is some other error
     */
    private boolean handleResourceRequest( final HttpServletRequest hreq,
                                           final HttpServletResponse hresp ) throws IOException, ServletException {
        boolean handled = false;

        String filePath = hreq.getRequestURI();
        String contextPath = hreq.getContextPath();

        if ( filePath != null && contextPath != null ) {
            String resourceName = filePath.substring(contextPath.length());

            if ( RESOURCES.containsKey( resourceName ) ) {
                String[] RESOURCE_PATH_AND_TYPE = RESOURCES.get( resourceName );
                InputStream resourceIn = EsmTrustServlet.class.getResourceAsStream( RESOURCE_PATH_AND_TYPE[0] );
                if ( resourceIn != null ) {
                    try {
                        handled = true;
                        hresp.setContentType( RESOURCE_PATH_AND_TYPE[1] );
                        IOUtils.copyStream( resourceIn, hresp.getOutputStream() );
                    } finally {
                        ResourceUtils.closeQuietly( resourceIn );
                    }
                }
            }
        }

        return handled;
    }
}
