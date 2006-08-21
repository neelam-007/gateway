/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.server;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.LicenseException;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.objectmodel.FindException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;

/**
 * @see <a href="http://sarek.l7tech.com/mediawiki/index.php?title=SSG_Ping_Url">Ping servlet specification 3.6</a>
 * @author alex
 * @author rmak
 */
public class PingServlet extends AuthenticatableHttpServlet {
    private static final Logger _logger = Logger.getLogger(PingServlet.class.getName());

    /** Name of system property specifying the operating mode. */
    private static final String MODE_PROP_NAME = "com.l7tech.server.PingServlet.mode";

    /** Available operating modes. */
    private enum Mode { OFF, REQUIRE_CREDS, OPEN };

    /** Default operating mode. */
    private static final Mode DEFAULT_MODE = Mode.REQUIRE_CREDS;

    /** Operating mode. */
    private Mode _mode;

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

        // Determines the operating mode.
        final String modeString = System.getProperty(MODE_PROP_NAME);
        if (modeString == null) {
            _logger.config("System property not found (" + MODE_PROP_NAME +
                    "). Setting ping servlet mode to default (" + DEFAULT_MODE + ").");
            _mode = DEFAULT_MODE;
        } else {
            try {
                _mode = Mode.valueOf(modeString.trim());
                _logger.config("Setting ping servlet mode=" + _mode);
            } catch (IllegalArgumentException e) {
                _logger.warning("Invalid property value (" + MODE_PROP_NAME + "=" + modeString +
                        "). Setting ping servlet mode to default (" + DEFAULT_MODE + ").");
                _mode = DEFAULT_MODE;
            }
        }

        final WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (webApplicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        _clusterInfoManager = (ClusterInfoManager)webApplicationContext.getBean("clusterInfoManager");
        _roleManager = (RoleManager)webApplicationContext.getBean("roleManager");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (_mode == Mode.OFF) {
            respondNone(request, "mode=" + _mode);
            return;
        }

        final int port = request.getServerPort();
        final String protocol = request.isSecure() ? "https" : "http";

        if (_mode == Mode.REQUIRE_CREDS) {
            if ("http".equals(protocol) && port == 8080) {
                respondNone(request, "mode=" + _mode + ", protocol=" + protocol + ", port=" + port);
            } else if ("https".equals(protocol) && port == 8443) {
                final boolean hasCredentials = findCredentialsBasic(request) != null;
                if (!hasCredentials) {
                    respondChallenge(response, "mode=" + _mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials);
                    return;
                }

                boolean authenticated = false;
                try {
                    final AuthenticationResult[] results = authenticateRequestBasic(request);
                    if (results.length > 0) {
                        final User user = results[0].getUser();
                        authenticated = _roleManager.isPermittedForAllEntities(user, EntityType.CLUSTER_PROPERTY, OperationType.READ);
                    }
                } catch (AuthenticationException e) {
                    authenticated = false;
                } catch (LicenseException e) {
                    _logger.warning("Ping service is unlicensed; returning " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ".");
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    return;
                    // Not calling sendError because that will return an error page with detail build info.
                    // Should I kill response instead?
                } catch (FindException e) {
                    authenticated = false;
                } catch (CannotCreateTransactionException e) {
                    // Database unreachable.
                    respondDatabaseFailureMinimal(response, "mode=" + _mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials);
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("Failed to authenticate request: " + e.getMessage());
                    }
                    return;
                }

                if (authenticated) {
                    respondFull(response, "mode=" + _mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials + ", authenticated=" + authenticated);
                } else {
                    respondNone(request, "mode=" + _mode + ", protocol=" + protocol + ", port=" + port + ", hasCredentials=" + hasCredentials + ", authenticated=" + authenticated);
                }
            }
        } else if (_mode == Mode.OPEN) {
            if ("http".equals(protocol) && port == 8080) {
                respondMinimal(response, "mode=" + _mode + ", protocol=" + protocol + ", port=" + port);
            } else if ("https".equals(protocol) && port == 8443) {
                respondFull(response, "mode=" + _mode + ", protocol=" + protocol + ", port=" + port);
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
                Collection nodeInfos = _clusterInfoManager.retrieveClusterStatus();
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
            out.println("<html><head><title>SecureSpan Gateway</title></head><body><h1>FAILURE</h1><p>Cannot connect to database from " + InetAddress.getLocalHost().getHostName() + ".</p></body></html>");
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
