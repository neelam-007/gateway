/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.server;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.LicenseException;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.tomcat.ResponseKillerValve;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see <a href="http://sarek.l7tech.com/mediawiki/index.php?title=SSG_Ping_Url">Ping servlet specification 3.6</a>
 * @author alex
 * @author rmak
 */
public class PingServlet extends AuthenticatableHttpServlet {
    private static final Logger _logger = Logger.getLogger(PingServlet.class.getName());

    /** Name of server config property specifying the operating mode. */
    private static final String MODE_PROP_NAME = "pingServletMode";

    /** Available operating modes. */
    private enum Mode { OFF, REQUIRE_CREDS, OPEN };

    /** Default operating mode. */
    private static final Mode DEFAULT_MODE = Mode.REQUIRE_CREDS;

    private ClusterInfoManager _clusterInfoManager;
    private RoleManager _roleManager;

    protected String getFeature() {
        return GatewayFeatureSets.SERVICE_MESSAGEPROCESSOR;
    }

    /** Overrided to indicate we allow HTTP Basic without SSL. */
    protected boolean isCleartextAllowed() {
        return true;
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        final WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (webApplicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        _clusterInfoManager = (ClusterInfoManager)webApplicationContext.getBean("clusterInfoManager");
        _roleManager = (RoleManager)webApplicationContext.getBean("roleManager");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Determines the operating mode.
        Mode mode = DEFAULT_MODE;
        final String modeString = ServerConfig.getInstance().getProperty(MODE_PROP_NAME);
        if (modeString == null) {
            _logger.fine("Property not found (" + MODE_PROP_NAME +
                    "). Setting ping servlet mode to default (" + DEFAULT_MODE + ").");
        } else {
            try {
                mode = Mode.valueOf(modeString.trim());
                _logger.fine("Ping servlet mode=" + mode);
            } catch (IllegalArgumentException e) {
                _logger.warning("Invalid property value (" + MODE_PROP_NAME + "=" + modeString +
                        "). Setting ping servlet mode to default (" + DEFAULT_MODE + ").");
                mode = DEFAULT_MODE;
            }
        }

        if (mode == Mode.OFF) {
            respondNone(request, "mode=" + mode);
            return;
        }

        final int port = request.getServerPort();
        final boolean secure = request.isSecure();
        final String protocol = secure ? "https" : "http";

        if (mode == Mode.REQUIRE_CREDS) {
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
                    final AuthenticationResult[] results = authenticateRequestBasic(request);
                    if (results.length > 0) {
                        final User user = results[0].getUser();
                        authenticated = _roleManager.isPermittedForAnyEntityOfType(user, OperationType.READ, EntityType.CLUSTER_INFO);
                    }
                } catch (AuthenticationException e) {
                    authenticated = false;
                } catch (LicenseException e) {
                    _logger.warning("Ping service is unlicensed; returning " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ".");
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
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
                }

                if (authenticated) {
                    respondFull(response, "mode=" + mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials + ", authenticated=" + authenticated);
                } else {
                    respondNone(request, "mode=" + mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials + ", authenticated=" + authenticated);
                }
            }
        } else if (mode == Mode.OPEN) {
            if (!secure) {
                respondMinimal(response, "mode=" + mode + ", protocol=" + protocol + ", port=" + port);
            } else {
                respondFull(response, "mode=" + mode + ", protocol=" + protocol + ", port=" + port);
            }
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

            out.println("<html><head><title>SecureSpan Gateway</title></head><body><h1>OK</h1></body></html>");
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Responded to Ping with minimal info (" + details + ").");
            }
        } finally {
            out.close();
        }
    }

    /** Responds with full info. */
    private void respondFull(final HttpServletResponse response, final String details)
            throws IOException {
        response.setContentType("text/html");
        final PrintWriter out = response.getWriter();
        try {
            Collection<ClusterNodeInfo> nodeInfos = null;
            try {
                //noinspection unchecked
                nodeInfos = (Collection<ClusterNodeInfo>)_clusterInfoManager.retrieveClusterStatus();
            } catch (Exception e) {
                respondDatabaseFailureFull(response, details);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Failed to retrieve cluster status: " + e.getMessage());
                }
                return;
            }

            out.println("<html>");
            out.println("<head><title>SecureSpan Gateway</title></head>");
            out.println("<body>");
            out.println("<h1>OK</h1>");
            out.println("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\" style=\"border:gray solid 2px; border-collapse:collapse; text-align:left;\">");
            out.println("<tr><th>Node</th><th>Uptime</th><th>Status</th></tr>");
            for (ClusterNodeInfo nodeInfo : nodeInfos) {
                final long timeStampAge = System.currentTimeMillis() - nodeInfo.getLastUpdateTimeStamp();
                if (timeStampAge >= 360000) {
                    out.println("<tr><td>" + nodeInfo.getName() + "</td><td>?</td><td style=\"color:white; background:red;\"><b>FAIL</b></td></tr>");
                } else {
                    final String status = timeStampAge >= 30000 ? "Warning" : "OK";
                    out.println("<tr><td>" + nodeInfo.getName() + "</td><td>" + durationAsString(nodeInfo.getUptime()) + "</td><td>" + status + "</td></tr>");
                }
            }
            out.println("</table>");
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
    private void respondChallenge(final HttpServletResponse response, final String details) {
        response.setStatus(HttpConstants.STATUS_UNAUTHORIZED);
        response.setHeader(HttpConstants.HEADER_WWW_AUTHENTICATE, "Basic realm=\"\"");
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
            out.println("<html><head><title>SecureSpan Gateway</title></head><body><h1>FAILURE</h1></body></html>");
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
            out.println("<html><head><title>SecureSpan Gateway</title></head><body><h1>FAILURE</h1><p>Cannot connect to database from " + InetAddress.getLocalHost().getCanonicalHostName() + ".</p></body></html>");
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
