/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.cluster.ServiceUsage;
import com.l7tech.cluster.ServiceUsageManager;
import com.l7tech.objectmodel.FindException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A servlet that answers REST queries of the form GET /ssg/management/-n/.1.3.6.1.4.1.17304.7.1.1.1 with SNMP answers
 * of the form ".1.3.6.1.4.1.17304.7.1.1.2\ninteger\n432154\n".
 */
public class SnmpQueryServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(SnmpQueryServlet.class.getName());
    private static final String BASE = ".1.3.6.1.4.1.17304";
    private static final String BASE_PUBLISHED_SERVICES = ".7.1";
    private static final String CONTENT_TYPE_SNMP = "text/plain";

    private WebApplicationContext applicationContext;
    private ServiceUsage[] serviceTable = null;
    private long serviceTableTime = 0;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
    }

    /** Match URL containing /ssg/management/VERB/OBJECTID. */
    private final Pattern urlParser = Pattern.compile("^/ssg/management/([^/]*)/" + BASE + "([0-9.]*)(/.*)?$");

    /** Match string of the form ".1.4" */
    private final Pattern match2 = Pattern.compile("^\\.(\\d+)\\.(\\d+)$");

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        final Matcher matcher = urlParser.matcher(uri);
        if (!matcher.matches()) {
            badRequest(request, response);
            return;
        }

        String sw = matcher.group(1);
        String id = matcher.group(2);

        // TODO remove logging here, or it'll get huge
        logger.log(Level.FINER, "Received SNMP request: <" + sw + "> for id <" + id + ">");

        final boolean advance;
        if ("get".equalsIgnoreCase(sw)) {
            advance = false;
        } else if ("getnext".equalsIgnoreCase(sw)) {
            advance = true;
        } else {
            badRequest(request, response);
            return;
        }

        try {
            if (id.startsWith(BASE_PUBLISHED_SERVICES)) {
                respondToPublishedServiceRequest(request,
                                                 response,
                                                 id.substring(BASE_PUBLISHED_SERVICES.length()),
                                                 advance);
                return;
            }

            // Unrecognized query
            badRequest(request, response);
            return;

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            logger.log( Level.INFO, "Internal Server Error", e );

            response.getWriter().close();
            return;
        }
    }

    private void respondToPublishedServiceRequest(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  String id,
                                                  boolean advance)
            throws IOException, FindException
    {
        final int maxColumn = 5;
        int row = -1;
        int column = -1;

        Matcher matcher = match2.matcher(id);
        if (id == null || id.length() < 1) {
            if (advance) {
                row = 1;
                column = 1;
                advance = false;
            }
        } else if (matcher.matches()) {
            row = Integer.parseInt(matcher.group(1));
            column = Integer.parseInt(matcher.group(2));
        }

        if (row < 1 || column < 1 || column > maxColumn) {
            badRequest(request, response);
            return;
        }

        if (advance) {
            column++;
            if (column > maxColumn) {
                column = 1;
                row++;
            }
        }

        ServiceUsage[] table = getCurrentServiceUsage();

        if (row > table.length) {
            badRequest(request, response);
            return;
        }

        String addr = BASE + BASE_PUBLISHED_SERVICES + "." + row + "." + column;

        ServiceUsage su = table[row - 1]; // (row is still 1-based, not 0-based)
        switch (column) {
            case 1:
                send(request, response, addr, "integer", su.getServiceid());
                return;
            case 2:
                final String serviceName = su.getServiceName();
                send(request, response, addr, "string", serviceName != null ? serviceName : "null serviceName");
                return;
            case 3:
                send(request, response, addr, "integer", su.getRequests());
                return;
            case 4:
                send(request, response, addr, "integer", su.getAuthorized());
                return;
            case 5:
                send(request, response, addr, "integer", su.getCompleted());
                return;
            default:
                badRequest(request, response);
                return;
        }
    }

    /**
     * Get the ServiceUsage table.  This may return a cached version if one exists and has been previously used
     * within the past ten seconds; otherwise, any cached version is discarded and a new copy is read from the
     * database.
     * <p>
     * The cache timeout is restarted whenever the cached version of the table is used.  The only way to obtain
     * fresh data is to avoid issuing any queries for ten seconds.  This is to work-around the problem that
     * SNMP queries are stateless and query for each cell of a table individually, and that the cells are addressed
     * by position in a virtual table rather than using unique identifiers.  If a cached table
     * was discarded and refreshed for being over ten seconds old, while a client was still in the process of
     * sending queries iterating the table, the client might receive inconsistent data if rows were added or
     * removed if the meantime since (say) row #3 might now correspond with what used to appear in row #4.
     *
     * @return a complete ServiceUsage table.  May be empty but never null.
     */
    private synchronized ServiceUsage[] getCurrentServiceUsage() throws FindException {
        long now = System.currentTimeMillis();
        if (serviceTable != null && now - serviceTableTime > 10000)
            serviceTable = null;

        if (serviceTable != null) {
            serviceTableTime = now;
            return serviceTable;
        }

        ServiceUsageManager serviceUsageManager = (ServiceUsageManager)applicationContext.getBean("serviceUsageManager");
        List accum = new ArrayList();

        Collection collection = serviceUsageManager.getAll();
        for (Iterator i = collection.iterator(); i.hasNext();) {
            ServiceUsage serviceUsage = (ServiceUsage)i.next();
            accum.add(serviceUsage);
        }

        serviceTable = (ServiceUsage[])accum.toArray(new ServiceUsage[0]);
        serviceTableTime = System.currentTimeMillis();

        return serviceTable;
    }

    private void badRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().close();
    }

    /**
     *
     * @param request
     * @param response
     * @param next  next MIB node, as a suffix which will be appended to BASE.  ie, ".1.2".
     * @param type
     * @param value
     * @throws IOException
     */
    private void send(HttpServletRequest request, HttpServletResponse response, String next, String type, String value) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(CONTENT_TYPE_SNMP);
        Writer out = response.getWriter();
        out.write(BASE + (next != null ? next : ""));
        out.write('\n');
        out.write(type != null && type.length() > 0 ? type : "string");
        out.write('\n');
        out.write(value);
        out.flush();
        out.close();
    }

    /** Same as {@link #send(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, String, String, String)} but takes int value. */
    private void send(HttpServletRequest request, HttpServletResponse response, String next, String type, int value) throws IOException {
        send(request, response, next, type, String.valueOf(value));
    }

    /** Same as {@link #send(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, String, String, String)} but takes long value. */
    private void send(HttpServletRequest request, HttpServletResponse response, String next, String type, long value) throws IOException {
        send(request, response, next, type, String.valueOf(value));
    }

}
