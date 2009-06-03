/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.common.http.*;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.common.io.ProcResult;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.ServiceMessages;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.IssuedCertNotPresentedException;
import com.l7tech.identity.MissingCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.event.system.BackupEvent;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.util.HttpClientFactory;
import com.l7tech.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This servlet provides remotely downloadable backup image which are created
 * on-demand using the Migration Utility (a.k.a. Flasher). The backup is accessed
 * through simple URL (e.g., https://ssghost:8443/ssg/backup?node=SSG1), thus
 * allowing customers to write scripts for automated download. An HTML page is
 * returned if the node parameter is not submitted (e.g., https://ssghost:8443/ssg/backup).
 *
 * @see <a href="http://sarek.l7tech.com/mediawiki/index.php?title=SSG_Backup_Service">Functional specification for Backup servlet in 4.3</a>
 * @since SecureSpan 4.3
 * @author rmak
 */
public class BackupServlet extends AuthenticatableHttpServlet {
    private static final Logger _logger = Logger.getLogger(BackupServlet.class.getName());

    private WebApplicationContext _webApplicationContext;
    private ClusterInfoManager _clusterInfoManager;
    private RoleManager _roleManager;
    private HttpClientFactory _httpClientFactory;
    private Auditor _auditor;

    @Override
    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_MESSAGEPROCESSOR;
    }

    @Override
    protected SsgConnector.Endpoint getRequiredEndpoint() {
        return SsgConnector.Endpoint.ADMIN_APPLET;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        _webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (_webApplicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        _clusterInfoManager = (ClusterInfoManager) _webApplicationContext.getBean("clusterInfoManager");
        _roleManager = (RoleManager) _webApplicationContext.getBean("roleManager");
        _httpClientFactory = (HttpClientFactory) _webApplicationContext.getBean("internodeHttpClientFactory");

        _auditor = new Auditor(this, _webApplicationContext, _logger);
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (!request.isSecure()) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_NOT_SSL, null);
            respondError(response, HttpServletResponse.SC_FORBIDDEN, "SSL required");
            return;
        }

        if (findCredentialsBasic(request) == null) {
            _logger.fine("Sending challenge response to backup request without credentials.");
            doHttpBasicChallenge( response );
            return;
        }

        AuthenticationResult[] results;
        try {
            results = authenticateRequestBasic(request);
        } catch (BadCredentialsException e) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_BAD_CREDENTIALS, e);
            respondError(response, HttpServletResponse.SC_FORBIDDEN, "Bad credentials");
            return;
        } catch (MissingCredentialsException e) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_BAD_CREDENTIALS, e);
            respondError(response, HttpServletResponse.SC_FORBIDDEN, "Missing credentials");
            return;
        } catch (IssuedCertNotPresentedException e) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_NO_CLIENT_CERT, e);
            respondError(response, HttpServletResponse.SC_FORBIDDEN, "No client certificate");
            return;
        } catch ( LicenseException e) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_NOT_LICENSED, e);
            respondError(response, HttpServletResponse.SC_FORBIDDEN, "Unlicensed");
            return;
        } catch (ListenerException e) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_BAD_CONNECTOR, e);
            respondError(response, HttpServletResponse.SC_FORBIDDEN, "Connector not enabled");
            return;
        }
        if (results.length <= 0) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_NO_AUTHENTICATED_USER, null);
            respondError(response, HttpServletResponse.SC_FORBIDDEN, "Unauthenticated");
            return;
        }

        final User user = results[0].getUser();
        try {
            boolean isAdmin = false;
            for (Role role : _roleManager.getAssignedRoles(user)) {
                if (role.getTag() == Role.Tag.ADMIN) {                    
                    isAdmin = true;
                    break;
                }
            }
            if (! isAdmin) {
                logAndAudit(getOriginalClientAddr(request), user, "Backup request blocked", ServiceMessages.BACKUP_NO_PERMISSION, null, user.getName());
                respondError(response, HttpServletResponse.SC_FORBIDDEN, "Not permitted");
                return;
            }
        } catch (FindException e) {
            logAndAudit(getOriginalClientAddr(request), user, "Backup request permission checked failed", ServiceMessages.BACKUP_PERMISSION_CHECK_FAILED, e);
            respondError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Permission check failed");
            return;
        }

        final String nodeName = request.getParameter("node");
        if (nodeName == null) {
            doMainPage(response);
        } else {
            final String selfNodeName = _clusterInfoManager.getSelfNodeInf().getName();
            if (nodeName.equals(selfNodeName)) {
                doBackup(request, response, selfNodeName, user);
            } else {
                routeBackup(request, response, nodeName, user);
            }
        }
    }

    /**
     * Responds with an HTML page for choosing which node to backup.
     *
     * @param response      the HTTP servlet response
     * @throws IOException if I/O error occurs
     */
    private void doMainPage(final HttpServletResponse response) throws IOException {
        Collection<ClusterNodeInfo> nodeInfos;
        try {
            nodeInfos = _clusterInfoManager.retrieveClusterStatus();
        } catch (FindException e) {
            _logger.warning("Failed to obtain cluster node information: " + e.toString());
            respondError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            return;
        }

        response.setContentType("text/html");
        final PrintWriter out = response.getWriter();
        try {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>SecureSpan Gateway Backup Service</title>");
            out.println("<style type=\"text/css\">");
            out.println("a.button {");
            out.println("    background-color: #E8E8E8;");
            out.println("    border: 2px outset gray;");
            out.println("    font-family: sans-serif;");
            out.println("    font-weight: bold;");
            out.println("    min-width: 70px;");
            out.println("    padding: 2px 6px 2px 6px;");
            out.println("}");
            out.println("a.button:link, a.button:visited, a.button:hover, a.button:active { color: black; text-decoration: none; }");
            out.println("a.button:hover { background-color: #D0D0D0; }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>SecureSpan Gateway Backup Service</h1>");
            out.println("<p>Please select a Gateway node to back up:</p>");
            for (ClusterNodeInfo nodeInfo : nodeInfos) {
                // Using <a> instead of <input type="submit"> because I want the
                // URL to be displayed in the browser status bar when hover.
                out.println("<p><a class=\"button\" href=\"?node=" + nodeInfo.getName() + "\">" + nodeInfo.getName() + "</a></p>");
            }
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

    /**
     * Creates a backup image of this node (and this partition), and sends
     * the image in the servlet response as a file download.
     *
     * @param request       the HTTP servlet request
     * @param response      the HTTP servlet response
     * @param nodeName      name of this node
     * @param user          the authenticated user making the request
     * @throws IOException if I/O error occurs
     */
    private void doBackup(final HttpServletRequest request,
                          final HttpServletResponse response,
                          final String nodeName,
                          final User user)
            throws IOException {
        File tmpFile  = File.createTempFile("backup", ".tmp");
        final String ssgHome = System.getProperty("com.l7tech.server.home");
        final File flasherHome = new File(ssgHome, "../../config/backup");

        try {
            ProcResult result = ProcUtils.exec(flasherHome, new File(flasherHome, "ssgbackup.sh"), new String[] { "-image", tmpFile.getCanonicalPath() }, null, false);
            logger.log( Level.INFO, "Backup completed with output: \n{0}", new String( result.getOutput()) );
        } catch (IOException e) {
            logAndAudit(getOriginalClientAddr(request), user, "Backup failed", ServiceMessages.BACKUP_CANT_CREATE_IMAGE, e, nodeName);
            respondError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Backup failed");
            //noinspection ResultOfMethodCallIgnored
            tmpFile.delete();
            return;
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            long size;
            try {
                size = tmpFile.length();
                in = new BufferedInputStream(new FileInputStream(tmpFile));
            } catch (Exception e) {
                logAndAudit(getOriginalClientAddr(request), user, "Backup failed", ServiceMessages.BACKUP_CANT_READ_IMAGE, e, nodeName);
                respondError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Backup failed");
                return;
            }

            if (size > Integer.MAX_VALUE) {
                logAndAudit(getOriginalClientAddr(request), user, "Backup failed", ServiceMessages.BACKUP_TOO_BIG, null, nodeName, Long.toString(size));
                respondError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Backup failed");
                return;
            }

            final String saveAsFilename = nodeName + ".zip";
            response.setContentType("application/zip");
            response.setContentLength((int)size);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + saveAsFilename + "\"; size=" + size);
            response.setHeader("Accept-Ranges", "none");

            out = response.getOutputStream();
            IOUtils.copyStream(in, out);
        } finally {
            ResourceUtils.closeQuietly(in, out);
            //noinspection ResultOfMethodCallIgnored
            tmpFile.delete();
        }

        logAndAudit(getOriginalClientAddr(request), user, "Backup downloaded", ServiceMessages.BACKUP_SUCCESS, null, nodeName, user.getName(), getOriginalClientHostAndAddr(request));
    }

    /**
     * Routes the backup request to the targeted node and passes the response back.
     *
     * @param request   the original HTTP servlet request
     * @param response  the original HTTP servlet response
     * @param nodeName  name of target node
     * @param user      the authenticated user making the request
     * @throws IOException if I/O error occurs
     */
    private void routeBackup(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final String nodeName,
                             final User user)
            throws IOException {
        String nodeAddress = null;
        try {
            // Determines the numeric IP address of the target node.
            final Collection<ClusterNodeInfo> nodeInfos = _clusterInfoManager.retrieveClusterStatus();
            for (ClusterNodeInfo nodeInfo : nodeInfos) {
                if (nodeInfo.getName().equals(nodeName)) {
                    nodeAddress = nodeInfo.getAddress();
                    break;
                }
            }
        } catch (FindException e) {
            logAndAudit(getOriginalClientAddr(request), user, "Backup request routing failed", ServiceMessages.BACKUP_NO_CLUSTER_INFO, e);
            respondError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot find node");
            return;
        }

        if (nodeAddress == null) {
            logAndAudit(getOriginalClientAddr(request), user, "Backup request routing failed", ServiceMessages.BACKUP_NO_SUCH_NODE, null, nodeName);
            respondError(response, HttpServletResponse.SC_BAD_REQUEST, "No such node");
            return;
        }

        // Routes the request to the target node using its numeric IP address.
        GenericHttpRequest routedRequest = null;
        GenericHttpResponse routedResponse = null;
        try {
            if (_logger.isLoggable(Level.FINE)) _logger.fine("Routing backup request to " + nodeName + " at " + nodeAddress);
            final String requestPath = request.getRequestURI() + "?" + request.getQueryString();
            final URL routedURL = new URL(request.getScheme(), nodeAddress, request.getLocalPort(), requestPath);
            final GenericHttpRequestParams params = new GenericHttpRequestParams(routedURL);
            final LoginCredentials credentials = findCredentialsBasic(request);
            params.setPasswordAuthentication(new PasswordAuthentication(credentials.getName(), credentials.getCredentials()));
            params.setPreemptiveAuthentication(true);
            params.addExtraHeader(new GenericHttpHeader(SecureSpanConstants.HEADER_ORIGINAL_HOST, request.getRemoteHost()));
            params.addExtraHeader(new GenericHttpHeader(SecureSpanConstants.HEADER_ORIGINAL_ADDR, request.getRemoteAddr()));
            routedRequest = _httpClientFactory.createHttpClient().createRequest(HttpMethod.GET, params);

            // Copies over response headers.
            routedResponse = routedRequest.getResponse();
            for (HttpHeader header : routedResponse.getHeaders().toArray()) {
                response.addHeader(header.getName(), header.getFullValue());
                if (_logger.isLoggable(Level.FINEST)) {
                    _logger.finest("Copied over HTTP header from routed backup response: " + header.getName() + "=" + header.getFullValue());
                }
            }

            // Copies over response body.
            int numBytes = 0;
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new BufferedInputStream(routedResponse.getInputStream());
                out = new BufferedOutputStream(response.getOutputStream());
                int buf;
                while ((buf = in.read()) != -1) {
                    out.write(buf);
                }
            } finally {
                ResourceUtils.closeQuietly( in );
                ResourceUtils.closeQuietly( out );
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Routed backup response from " + nodeName + " at " + nodeAddress + ": " + numBytes + " bytes in body");
            }
        } catch (IOException e) {
            logAndAudit(getOriginalClientAddr(request), user, "Backup request routing failed", ServiceMessages.BACKUP_ROUTING_IO_ERROR, e);
            respondError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Routing failed");
        } finally {
            ResourceUtils.closeQuietly( routedRequest );
            ResourceUtils.closeQuietly( routedResponse );
        }
    }

    /** Responds with HTTP error and page with status and message. */
    private void respondError(final HttpServletResponse response, final int status, final String msg)
            throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain");
        final PrintWriter out = response.getWriter();
        out.print(status);
        out.print(" ");
        out.print(msg);
        out.close();
    }

    /**
     * Convenience method to creates an audit event and an associated log message with the same level.
     *
     * @param clientAddr    IP address of client machine sending the request
     * @param user          user issuing the request; can be <code>null</code> if not available
     * @param eventMsg      audit detail message
     * @param logMsg        log message
     * @param e             exception associated with the log message
     * @param logParams     substitution parameters for the log message
     */
    private void logAndAudit(final String clientAddr,
                             final User user,
                             final String eventMsg,
                             final AuditDetailMessage logMsg,
                             final Throwable e,
                             final String... logParams) {
        if (e == null) {
            _auditor.logAndAudit(logMsg, logParams);
        } else {
            _auditor.logAndAudit(logMsg, logParams, e);
        }
        _webApplicationContext.publishEvent(new BackupEvent(this, logMsg.getLevel(), eventMsg, clientAddr, user));
    }

    private String getOriginalClientHost(final HttpServletRequest request) {
        String host = request.getHeader(SecureSpanConstants.HEADER_ORIGINAL_HOST);
        if (host == null) {
            host = request.getRemoteHost();
        }
        return host;
    }

    private String getOriginalClientAddr(final HttpServletRequest request) {
        String addr = request.getHeader(SecureSpanConstants.HEADER_ORIGINAL_ADDR);
        if (addr == null) {
            addr = request.getRemoteAddr();
        }
        return addr;
    }

    private String getOriginalClientHostAndAddr(final HttpServletRequest request) {
        final String host = getOriginalClientHost(request);
        final String addr = getOriginalClientAddr(request);
        if (addr.equals(host)) {
            return addr;
        } else {
            return host + " (" + addr + ")";
        }
    }
}

