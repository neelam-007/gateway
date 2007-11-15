/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.common.LicenseException;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.audit.ServiceMessages;
import com.l7tech.common.http.*;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.IssuedCertNotPresentedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.system.BackupEvent;
import com.l7tech.server.flasher.Exporter;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.HttpClientFactory;
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
import java.util.EnumSet;
import java.util.Set;
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
    private static final Set<EntityType> ALL_ENTITY_TYPES = EnumSet.complementOf(EnumSet.of(EntityType.ANY));

    private WebApplicationContext _webApplicationContext;
    private ClusterInfoManager _clusterInfoManager;
    private RoleManager _roleManager;
    private HttpClientFactory _httpClientFactory;
    private Auditor _auditor;

    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_MESSAGEPROCESSOR;
    }

    protected SsgConnector.Endpoint getRequiredEndpoint() {
        return SsgConnector.Endpoint.BACKUP;
    }

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

    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (!request.isSecure()) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_NOT_SSL, null);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "SSL required");
            return;
        }

        if (findCredentialsBasic(request) == null) {
            _logger.fine("Sending challenge response to backup request without credentials.");
            response.setStatus(HttpConstants.STATUS_UNAUTHORIZED);
            response.setHeader(HttpConstants.HEADER_WWW_AUTHENTICATE, "Basic realm=\"" + ServerHttpBasic.REALM + "\"");
            return;
        }

        AuthenticationResult[] results = null;
        try {
            results = authenticateRequestBasic(request);
        } catch (BadCredentialsException e) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_BAD_CREDENTIALS, e);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (IssuedCertNotPresentedException e) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_NO_CLIENT_CERT, e);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (LicenseException e) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_NOT_LICENSED, e);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (TransportModule.ListenerException e) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_BAD_CONNECTOR, e);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (results.length <= 0) {
            logAndAudit(getOriginalClientAddr(request), null, "Backup request blocked", ServiceMessages.BACKUP_NO_AUTHENTICATED_USER, null);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final User user = results[0].getUser();
        try {
            boolean isAdmin = false;
            for (Role role : _roleManager.getAssignedRoles(user)) {
                if (role.getOid() == Role.ADMIN_ROLE_OID) {
                    isAdmin = true;
                    break;
                }
            }
            if (! isAdmin) {
                logAndAudit(getOriginalClientAddr(request), user, "Backup request blocked", ServiceMessages.BACKUP_NO_PERMISSION, null, user.getName());
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } catch (FindException e) {
            logAndAudit(getOriginalClientAddr(request), user, "Backup request permission checked failed", ServiceMessages.BACKUP_PERMISSION_CHECK_FAILED, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
        Collection<ClusterNodeInfo> nodeInfos = null;
        try {
            nodeInfos = _clusterInfoManager.retrieveClusterStatus();
        } catch (FindException e) {
            _logger.warning("Failed to obtain cluster node information: " + e.toString());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // What partition am I?
        final String partitionName = System.getProperty(PartitionInformation.SYSTEM_PROP_PARTITIONNAME);

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
            out.println("<p><i>Note:</i> Only the current partition &ldquo;" + partitionName + "&rdquo; will be included.</p>");
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
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("backup", ".tmp");
            final String ssgHome = System.getProperty("com.l7tech.server.home");
            final File flasherHome = new File(ssgHome, "migration");
            final String partitionName = System.getProperty(PartitionInformation.SYSTEM_PROP_PARTITIONNAME);
            final Exporter exporter = new Exporter(flasherHome, null, null);
            exporter.doIt(partitionName, false, null, tmpFile.getCanonicalPath());
        } catch (Exception e) {
            logAndAudit(getOriginalClientAddr(request), user, "Backup failed", ServiceMessages.BACKUP_CANT_CREATE_IMAGE, e, nodeName);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (tmpFile != null) tmpFile.delete();
            return;
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            long size = -1;
            try {
                size = tmpFile.length();
                in = new BufferedInputStream(new FileInputStream(tmpFile));
            } catch (Exception e) {
                logAndAudit(getOriginalClientAddr(request), user, "Backup failed", ServiceMessages.BACKUP_CANT_READ_IMAGE, e, nodeName);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            if (size > Integer.MAX_VALUE) {
                logAndAudit(getOriginalClientAddr(request), user, "Backup failed", ServiceMessages.BACKUP_TOO_BIG, null, nodeName, Long.toString(size));
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            final String saveAsFilename = nodeName + ".zip";
            response.setContentType("text/html");
            response.setContentLength((int)size);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + saveAsFilename + "\"; size=" + size);
            response.setHeader("Accept-Ranges", "none");

            out = response.getOutputStream();
            HexUtils.copyStream(in, out);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
            if (tmpFile != null) tmpFile.delete();
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
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if (nodeAddress == null) {
            logAndAudit(getOriginalClientAddr(request), user, "Backup request routing failed", ServiceMessages.BACKUP_NO_SUCH_NODE, null, nodeName);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such node: " + nodeName);
            return;
        }

        // Routes the request to the target node using its numeric IP address.
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
            final GenericHttpRequest routedRequest = _httpClientFactory.createHttpClient().createRequest(GenericHttpClient.GET, params);

            // Copies over response headers.
            final GenericHttpResponse routedResponse = routedRequest.getResponse();
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
                if (in != null) in.close();
                if (out != null) out.close();
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Routed backup response from " + nodeName + " at " + nodeAddress + ": " + numBytes + " bytes in body");
            }
        } catch (IOException e) {
            logAndAudit(getOriginalClientAddr(request), user, "Backup request routing failed", ServiceMessages.BACKUP_ROUTING_IO_ERROR, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
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

