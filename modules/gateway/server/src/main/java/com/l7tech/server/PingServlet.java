package com.l7tech.server;

import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.util.BuildInfo;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.common.io.ProcResult;
import com.l7tech.common.http.*;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.TextUtils;
import com.l7tech.util.SudoUtils;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.transport.ListenerException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see <a href="http://sarek.l7tech.com/mediawiki/index.php?title=SSG_Ping_Url">Functional specification for Ping servlet in 3.6</a>
 * @see <a href="http://sarek.l7tech.com/mediawiki/index.php?title=Expand_SSG_Ping">Functional specification for adding system info in 4.3</a>
 * @see <a href="http://sarek/mediawiki/index.php?title=SSG_Ping_MONITOR_Mode">Functional specification for adding MONITOR mode in Goatfish</a>
 * @author alex
 * @author rmak
 * @author jwilliams
 */
public class PingServlet extends AuthenticatableHttpServlet {
    private static final Logger _logger = Logger.getLogger(PingServlet.class.getName());

    /** Name of server config property specifying the operating mode. */
    private static final String MODE_PROP_NAME = "pingServletMode";

    /** Available operating modes. */
    private enum Mode { OFF, REQUIRE_CREDS, OPEN, MONITOR }

    /** Default operating mode. */
    private static final Mode DEFAULT_MODE = Mode.REQUIRE_CREDS;

    /** Filename of script to query additional system info. */
    private static final String SYSTEM_INFO_SCRIPT_NAME =  "systeminfo.sh";

    /** Expected request URI for the main page.
        NOTE: Make sure URI is included in servlet-mapping url-pattern in web.xml. */
    private static final String MAIN_REQUEST_URI = "/ssg/ping";

    /** Expected request URI for getting system info.
        NOTE: Make sure URI is included in servlet-mapping url-pattern in web.xml. */
    private static final String SYSTEM_INFO_REQUEST_URI = MAIN_REQUEST_URI + "/systemInfo";

    private ClusterInfoManager _clusterInfoManager;
    private RoleManager _roleManager;
    private GenericHttpClientFactory _httpClientFactory;
    private File _ssgApplianceLibexecDir;

    @Override
    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_MESSAGEPROCESSOR;
    }

    @Override
    protected SsgConnector.Endpoint getRequiredEndpoint() {
        return SsgConnector.Endpoint.PING;  // Ping service added into "Granularity in enabling built in services" (bug 11055)
    }

    /** Overrided to indicate we allow HTTP Basic without SSL. */
    @Override
    protected boolean isCleartextAllowed() {
        return true;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        final WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (webApplicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        _clusterInfoManager = (ClusterInfoManager)webApplicationContext.getBean("clusterInfoManager");
        _roleManager = (RoleManager)webApplicationContext.getBean("roleManager");
        _httpClientFactory = (GenericHttpClientFactory)webApplicationContext.getBean("internodeHttpClientFactory");

        _ssgApplianceLibexecDir = new File( config.getProperty( ServerConfigParams.PARAM_SSG_APPLIANCE_DIRECTORY ) + File.separator + "libexec");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Determines the operating mode.
        Mode mode = DEFAULT_MODE;
        final String modeString = config.getProperty( MODE_PROP_NAME );
        if (modeString == null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Property not found (" + MODE_PROP_NAME +
                             "). Setting ping servlet mode to default (" + DEFAULT_MODE + ").");
            }
        } else {
            try {
                mode = Mode.valueOf(modeString.trim());
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Ping servlet mode=" + mode);
                }
            } catch (IllegalArgumentException e) {
                _logger.warning("Invalid property value (" + MODE_PROP_NAME + "=" + modeString +
                        "). Setting ping servlet mode to default (" + DEFAULT_MODE + ").");
                mode = DEFAULT_MODE;
            }
        }

        final String uri = request.getRequestURI();
        if (MAIN_REQUEST_URI.equals(uri)) {
            doMainPage(request, response, mode);
        } else if (SYSTEM_INFO_REQUEST_URI.equals(uri)) {
            doSystemInfo(request, response, mode);
        } else {
            // servlet-mapping url-pattern in web.xml should have prevented the request from getting here.
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Responding with 404 for unknown request URI (should have been blocked by web.xml but wasn't): " + uri);
            }
            respondError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown request URI");
        }
    }

    /**
     * Handles request for the Ping main page.
     */
    private void doMainPage(final HttpServletRequest request,
                            final HttpServletResponse response,
                            final Mode mode)
            throws IOException {
        final int port = request.getServerPort();
        final boolean secure = request.isSecure();
        final String protocol = secure ? "https" : "http";

        switch (mode) {
            case OFF:
                respondNone(request, "mode=" + mode);
                break;
            case REQUIRE_CREDS:
                if (!secure) {
                    respondNone(request, "mode=" + mode + ", protocol=" + protocol + ", port=" + port);
                } else {
                    final boolean hasCredentials = findCredentialsBasic(request) != null;
                    if (!hasCredentials) {
                        respondChallenge(response, "mode=" + mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials);
                        return;
                    }

                    boolean authenticated = false;
                    try {
                        final AuthenticationResult[] results = authenticateRequestBasic(request, true);
                        if (results.length > 0) {
                            final User user = results[0].getUser();
                            authenticated = _roleManager.isPermittedForAnyEntityOfType(user, OperationType.READ, EntityType.CLUSTER_INFO);
                        }
                    } catch (AuthenticationException e) {
                        authenticated = false;
                    } catch (LicenseException e) {
                        _logger.warning("Ping service is unlicensed; returning " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ".");
                        respondError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Unlicensed");
                        return;
                    } catch (FindException e) {
                        authenticated = false;
                    } catch (CannotCreateTransactionException e) {
                        // Database unreachable.
                        respondDatabaseFailureMinimal(response, "mode=" + mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials);
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.fine("Failed to authenticate request: " + e.getMessage());
                        }
                        return;
                    } catch (ListenerException e) {
                        _logger.warning("Ping service is not enabled for this port; returning " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ".");
                        respondError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Connector not enabled");
                        return;
                    }

                    if (authenticated) {
                        respondFull(response, "mode=" + mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials + ", authenticated=" + authenticated);
                    } else {
                        respondNone(request, "mode=" + mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials + ", authenticated=" + authenticated);
                    }
                };
                break;
            case OPEN:
                if (!secure) {
                    respondMinimal(response, "mode=" + mode + ", protocol=" + protocol + ", port=" + port);
                } else {
                    respondFull(response, "mode=" + mode + ", protocol=" + protocol + ", port=" + port);
                }
                break;
            case MONITOR:
                respondMinimal(response, "mode=" + mode + ", protocol=" + protocol + ", port=" + port);
                break;
            default:
                throw new IllegalStateException("Unrecgonized mode");
        }
    }

    /**
     * Handles request for system info. The target node may not be this node if
     * this is a cluster.
     */
    private void doSystemInfo(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final Mode mode)
            throws IOException {

        if (mode == Mode.OFF || mode == Mode.MONITOR) {
            respondNone(request, "mode=" + mode);
        }

        if (request.isSecure() && findCredentialsBasic(request) == null) {
            respondChallenge(response, "system info request without credentials");
            return;
        }

        if (! isSystemInfoPermitted(request)) {
            respondNone(request, "system info request not permitted");
            return;
        }

        // Determines which SSG node's system info is requested for.
        final String nodeName = request.getParameter("node");
        if (nodeName == null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Missing node name parameter in system info request.");
            }
            respondError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing node name");
            return;
        }

        // If this SSG node is the target node, handle it; otherwise route it through.
        final String selfNodeName = _clusterInfoManager.getSelfNodeInf().getName();
        if (nodeName.equals(selfNodeName)) {
            if (_logger.isLoggable(Level.FINE)) {
                if (request.getHeader(SecureSpanConstants.HEADER_ORIGINAL_HOST) == null) {
                    _logger.fine("System info request is for this node (without routing).");
                } else {
                    _logger.fine("System info request is for this node (routed through " + request.getRemoteHost() + ").");
                }
            }
            reallyDoSystemInfo(response, nodeName);
        } else {
            _logger.fine("System info request is for another node.");
            routeSystemInfoRequest(request, response, nodeName);
        }
    }

    /**
     * Determines if system info request is permitted.
     *
     * @param request   the HTTP request
     * @return true if permitted
     */
    private boolean isSystemInfoPermitted(final HttpServletRequest request) {

        if (! request.isSecure()) return false;

        AuthenticationResult[] results;
        try {
            results = authenticateRequestBasic(request, true);
        } catch (BadCredentialsException e) {
            return false;
        } catch (MissingCredentialsException e) {
            return false;
        } catch (IssuedCertNotPresentedException e) {
            return false;
        } catch ( LicenseException e) {
            return false;
        } catch (ListenerException e) {
            return false;
        }
        if (results.length <= 0) return false;

        final User user = results[0].getUser();
        final Set<EntityType> allEntityTypes = EnumSet.complementOf(EnumSet.of(EntityType.ANY));
        try {
            if (! _roleManager.isPermittedForEntitiesOfTypes(user, OperationType.READ, allEntityTypes)) return false;
        } catch (FindException e) {
            return false;
        }

        return true;
    }

    /**
     * Handles request for system info for this node.
     */
    private void reallyDoSystemInfo(final HttpServletResponse response, final String nodeName) throws IOException {
        response.setContentType("text/html");
        final PrintWriter out = response.getWriter();
        try {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Gateway</title>");
            out.println("<style type=\"text/css\">");
            out.println("h1 { margin-top: 18px; margin-bottom: 12px; }");
            out.println("h2 { margin-top: 14px; margin-bottom: 8px; }");
            out.println("h3 { margin-top: 12px; margin-bottom: 4px; }");
            out.println("table { border: 2px solid black; border-spacing: 2px; border-collapse: collapse; }");
            out.println("table th, td{ border: 1px solid black; padding: 1px 4px 1px 4px; text-align: left; vertical-align: top; }");
            out.println(".right { text-align: right; }");
            out.println("#working {");
            out.println("    color: white;");
            out.println("    background-color: green;");
            out.println("    font-weight: bold;");
            out.println("    text-decoration: blink;"); // I know blink doesn't work for IE7; just a nice touch for Firefox.
            out.println("    position: absolute;");
            out.println("    left: 0px;");
            out.println("    top: 0px;");
            out.println("    padding: 3px 6px 3px 6px;");
            out.println("}");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div id=\"working\">Working ...</div>");
            out.flush();    // Displays working notice because it may take a while. To be cancelled below when whole page is done.
            out.print("<h1>"); out.print(nodeName); out.println("</h1>");


            //
            // Collects system info that can be done platform-independently in Java.
            //
            out.print("<h2>Operating System</h2>");
            out.println("<table>");
            out.print("<tr><th>name</th><td>");  out.print( SyspropUtil.getProperty( "os.name" ) );  out.println("</td></tr>");
            out.print("<tr><th>architecture</th><td>"); out.print( SyspropUtil.getProperty( "os.arch" ) ); out.println("</td></tr>");
            out.print("<tr><th>version</th><td>"); out.print( SyspropUtil.getProperty( "os.version" ) ); out.println("</td></tr>");
            out.println("</table>");

            out.print("<h2>Java Virtual Machine</h2>");
            out.println("<table>");
            out.print("<tr><th>vendor</th><td>"); out.print( SyspropUtil.getProperty( "java.vm.vendor" ) ); out.println("</td></tr>");
            out.print("<tr><th>name</th><td>"); out.print( SyspropUtil.getProperty( "java.vm.name" ) ); out.println("</td></tr>");
            out.print("<tr><th>version</th><td>"); out.print( SyspropUtil.getProperty( "java.vm.version" ) ); out.println("</td></tr>");
            out.println("</table>");

            final Runtime runtime = Runtime.getRuntime();
            out.println("<h3>JVM memory usage</h3>");
            out.println("<table>");
            out.print("<tr><th>total</th><td class=\"right\">");
            out.print(runtime.totalMemory());
            out.println(" bytes</td></tr>");
            out.print("<tr><th>free</th><td class=\"right\">");
            out.print(runtime.freeMemory());
            out.println(" bytes</td></tr>");
            out.print("<tr><th>maximum</th><td class=\"right\">");
            out.print(runtime.maxMemory());
            out.println(" bytes</td></tr>");
            out.println("</table>");

            //
            // Collects additional system info using external script.
            //
            final File scriptFile = new File(_ssgApplianceLibexecDir, SYSTEM_INFO_SCRIPT_NAME);
            if (scriptFile.exists()) {
                File sudo = null;
                try {
                    sudo = SudoUtils.findSudo();
                } catch (IOException e) {
                    /* FALLTHROUGH and do without */
                }

                File procFile;
                String[] procArgs;
                if (sudo == null) {
                    procFile = scriptFile;
                    procArgs = new String[0];
                } else {
                    procFile = sudo;
                    procArgs = new String[]{ scriptFile.getAbsolutePath() };
                }

                String scriptOutput;
                try {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("Running system info script: " + scriptFile.getAbsolutePath());
                    }
                    final ProcResult result = ProcUtils.exec(scriptFile.getParentFile(), procFile, procArgs);
                    if (result.getExitStatus() == 0) {
                        scriptOutput = new String(result.getOutput());
                    } else {
                        // Script failed. Prints tail part of output to log; for diagnostics.
                        // Servlet response gets only a generic error message for security reason.
                        _logger.warning("System info script exited with " + result.getExitStatus() + ". Last 10 lines of output:\n" + TextUtils.tail(new String(result.getOutput()), 10));
                        scriptOutput = "Script exited with " + result.getExitStatus() + ". See Gateway log for details.";
                    }
                } catch (IOException e) {
                    _logger.warning("Failed to run system info script: " + e.toString());
                    // Servlet response gets only a generic error message for security reason.
                    scriptOutput = "Failed to run script. See Gateway log for details.";
                }

                if (scriptOutput != null) {
                    out.println("<h2>Additional Info (script)</h2>");
                    out.println("<pre>");
                    out.println(scriptOutput.replace("<", "&lt;"));
                    out.println("</pre>");
                }
            } else {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Skipping system info script because it is not found: " + SYSTEM_INFO_SCRIPT_NAME);
                }
            }

            out.println("<script>document.getElementById(\"working\").style.display = \"none\";</script>"); // Cancels working notice.
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

    /**
     * Routes the system info request to the targeted node and passes the response back.
     */
    private void routeSystemInfoRequest(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final String nodeName)
            throws IOException {
        try {
            // Determines the numeric IP address of the target node.
            String nodeAddress = null;
            final Collection<ClusterNodeInfo> nodeInfos = _clusterInfoManager.retrieveClusterStatus();
            for (ClusterNodeInfo nodeInfo : nodeInfos) {
                if (nodeInfo.getName().equals(nodeName)) {
                    nodeAddress = nodeInfo.getAddress();
                    break;
                }
            }
            if (nodeAddress == null) {
                _logger.warning("Received system info request for unknown node: " + nodeName);
                respondError(response, HttpServletResponse.SC_BAD_REQUEST, "No such node");
                return;
            }

            // Routes the request to the target node using its numeric IP address.
            try {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Routing system info request to " + nodeName + " at " + nodeAddress);
                }

                final String requestPath = request.getRequestURI() + "?" + request.getQueryString();
                final URL routedURL = new URL(request.getScheme(), nodeAddress, request.getLocalPort(), requestPath);
                final GenericHttpRequestParams params = new GenericHttpRequestParams(routedURL);
                final LoginCredentials credentials = findCredentialsBasic(request);
                params.setPasswordAuthentication(new PasswordAuthentication(credentials.getName(), credentials.getCredentials()));
                params.setPreemptiveAuthentication(true);
                params.addExtraHeader(new GenericHttpHeader(SecureSpanConstants.HEADER_ORIGINAL_HOST, request.getRemoteHost()));
                params.addExtraHeader(new GenericHttpHeader(SecureSpanConstants.HEADER_ORIGINAL_ADDR, request.getRemoteAddr()));
                final GenericHttpRequest routedRequest = _httpClientFactory.createHttpClient().createRequest(HttpMethod.GET, params);

                final GenericHttpResponse routedResponse = routedRequest.getResponse();
                for (HttpHeader header : routedResponse.getHeaders().toArray()) {
                    response.addHeader(header.getName(), header.getFullValue());
                    if (_logger.isLoggable(Level.FINEST)) {
                        _logger.finest("Copied over HTTP header from routed system info response: " + header.getName() + "=" + header.getFullValue());
                    }
                }

                int numBytes = 0;
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = new BufferedInputStream(routedResponse.getInputStream());
                    out = new BufferedOutputStream(response.getOutputStream());
                    int buf;
                    while ((buf = in.read()) != -1) {
                        ++ numBytes;
                        out.write(buf);
                    }
                } finally {
                    if (in != null) in.close();
                    if (out != null) out.close();
                }

                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Routed system info response from " + nodeName + " at " + nodeAddress + ": " + numBytes + " bytes in body");
                }
            } catch (IOException e) {
                _logger.log(Level.WARNING, "Failed to routed system info request to " + nodeName + " at " + nodeAddress, e);
                respondError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to route");
            }
        } catch (FindException e) {
            _logger.log(Level.WARNING, "Failed to obtain cluster node information when routing system info request.", e);
            respondError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot find node");
        }
    }

    /** Kills response. */
    private void respondNone(final HttpServletRequest request, final String details) {
        request.setAttribute(ResponseKillerValve.ATTRIBUTE_FLAG_NAME,
                             ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Killed response to ping (" + details + ").");
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

    /** Responds with minimal info. */
    private void respondMinimal(final HttpServletResponse response, final String details)
            throws IOException {
        response.setContentType("text/html");
        final PrintWriter out = response.getWriter();
        try {
            try {
                _clusterInfoManager.retrieveClusterStatus();
            } catch (Exception e) {
                respondDatabaseFailureMinimal(response, details);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Failed to retrieve cluster status: " + e.getMessage());
                }
                return;
            }

            out.println("<html><head><title>Gateway</title></head><body><h1>OK</h1></body></html>");
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Responded to Ping with minimal info (" + details + ").");
            }
        } finally {
            out.close();
        }
    }

    /**
     * Responds with full info.
     */
    private void respondFull(final HttpServletResponse response, final String details)
            throws IOException {
        response.setContentType("text/html");
        final PrintWriter out = response.getWriter();
        try {
            Collection<ClusterNodeInfo> nodeInfos;
            try {
                //noinspection unchecked
                nodeInfos = _clusterInfoManager.retrieveClusterStatus();
            } catch (Exception e) {
                respondDatabaseFailureFull(response, details);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Failed to retrieve cluster status: " + e.getMessage());
                }
                return;
            }

            out.println("<html>");
            out.println("<head><title>Gateway</title></head>");
            out.println("<body>");
            out.println("<h1>OK</h1>");
            out.println("<form action=\"" + SYSTEM_INFO_REQUEST_URI + "\" method=\"GET\">");
            out.println("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\" style=\"border:gray solid 2px; border-collapse:collapse; text-align:left;\">");
            out.print("<tr><th>Node</th><th>Uptime</th><th>Status</th><th>System Info<br/><small>(Adminstrator/Operator only)</small></th>");
            out.println("</tr>");
            for (ClusterNodeInfo nodeInfo : nodeInfos) {
                final long timeStampAge = System.currentTimeMillis() - nodeInfo.getLastUpdateTimeStamp();
                if (timeStampAge >= 3600000) {
                    out.print("<tr><td>" + nodeInfo.getName() + "</td><td>?</td><td style=\"color:white; background:red;\"><b>FAIL</b></td>");
                } else {
                    final String status = timeStampAge >= 30000 ? "Warning" : "OK";
                    out.print("<tr><td>" + nodeInfo.getName() + "</td><td>" + durationAsString(nodeInfo.getUptime()) + "</td><td>" + status + "</td>");
                }
                out.print("<td><input type=\"submit\" name=\"node\" value=\"" + nodeInfo.getName() + "\"/></td>");
                out.println("</tr>");
            }
            out.println("</table>");
            out.println("</form>");
            out.println("<hr/><i><font size=\"-2\">" + BuildInfo.getLongBuildString() + "</font></i>");
            out.println("</body>");
            out.println("</html>");
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Responded to Ping with full info (" + details + ").");
            }
        } finally {
            out.close();
        }
    }

    /** Responds with an HTTP authentication challenge. */
    private void respondChallenge(final HttpServletResponse response, final String details) throws IOException {
        doHttpBasicChallenge( response );
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Responded to Ping with authentication challenge (" + details + ").");
        }
    }

    /** Responds with minimal info in the event of database access failure. */
    private void respondDatabaseFailureMinimal(final HttpServletResponse response, final String details)
            throws IOException {
        response.setContentType("text/html");
        final PrintWriter out = response.getWriter();
        try {
            out.println("<html><head><title>Gateway</title></head><body><h1>FAILURE</h1></body></html>");
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Responded to Ping with Failure and minimal info (" + details + ").");
            }
        } finally {
            out.close();
        }
    }

    /** Responds with full info in the event of database access failure. */
    private void respondDatabaseFailureFull(final HttpServletResponse response, final String details)
            throws IOException {
        response.setContentType("text/html");
        final PrintWriter out = response.getWriter();
        try {
            out.println("<html><head><title>Gateway</title></head><body><h1>FAILURE</h1><p>Cannot connect to database from " + InetAddress.getLocalHost().getCanonicalHostName() + ".</p></body></html>");
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Responded to Ping with Failure and full info (" + details + ").");
            }
        } finally {
            out.close();
        }
    }

    /**
     * Converts a time duration in milliseconds to a human friendly time string.
     *
     * <p>Example output:
     * <pre>
     * 0 min
     * 1 hour 0 min
     * 1 day 0 hour 0 min
     * 2 days 2 hours 2 mins
     * -1 day 0 hour -2 mins
     * </pre>
     *
     * @param millis    time duration in milliseconds
     * @return a human friendly time string
     */
    private String durationAsString(final long millis) {
        long secs = millis / 1000;
        long mins = secs / 60;
        secs %= 60;
        long hrs = mins / 60;
        mins %= 60;
        long days = hrs / 24;
        hrs %= 24;

        final StringBuilder sb = new StringBuilder();
        if (days != 0) {
            sb.append(days).append(Math.abs(days) <= 1 ? " day " : " days ");
        }
        if (days != 0 || hrs != 0) {
            sb.append(hrs).append(Math.abs(hrs) <= 1 ? " hour " : " hours ");
        }
        sb.append(mins).append(Math.abs(mins) <= 1 ? " min" : " mins");

        return sb.toString();
    }
}
