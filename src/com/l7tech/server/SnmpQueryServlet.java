/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.cluster.ServiceUsage;
import com.l7tech.cluster.ServiceUsageManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.ServiceAdmin;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    private static final String CONTENT_TYPE_SNMP = "application/x-snmp";

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
            badRequest(response);
            return;
        }

        String sw = matcher.group(1);
        String id = matcher.group(2);

        final boolean advance;
        if ("get".equalsIgnoreCase(sw)) {
            advance = false;
        } else if ("getnext".equalsIgnoreCase(sw)) {
            advance = true;
        } else {
            badRequest(response);
            return;
        }

        try {
            if (id.startsWith(BASE_PUBLISHED_SERVICES)) {
                respondToPublishedServiceRequest(response,
                                                 id.substring(BASE_PUBLISHED_SERVICES.length()),
                                                 advance);
                return;
            }

            // Unrecognized query
            badRequest(response);
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

    private void respondToPublishedServiceRequest(HttpServletResponse response,
                                                  String id,
                                                  boolean advance)
            throws IOException, FindException
    {
        final int maxField = 5;
        int service = -1;
        int field = -1;

        Matcher matcher = match2.matcher(id);
        if (id == null || id.length() < 1) {
            if (advance) {
                service = 1;
                field = 1;
                advance = false;
            } else {
                send(response, BASE_PUBLISHED_SERVICES, "string", "Service Usage");
                return;
            }
        } else if (matcher.matches()) {
            field = Integer.parseInt(matcher.group(1));
            service = Integer.parseInt(matcher.group(2));
        }

        if (service < 1 || field < 1 || field > maxField) {
            badRequest(response);
            return;
        }


        ServiceUsage[] table = getCurrentServiceUsage();

        if (advance) {
            service++;
            if (service > table.length) {
                service = 1;
                field++;
            }
        }

        if (service > table.length) {
            badRequest(response);
            return;
        }

        String addr = BASE_PUBLISHED_SERVICES + "." + field + "." + service;

        ServiceUsage su = table[service - 1]; // (column is still 1-based, not 0-based)
        switch (field) {
            case 1:
                send(response, addr, "integer", su.getServiceid());
                return;
            case 2:
                send(response, addr, "string", su.getServiceName());
                return;
            case 3:
                send(response, addr, "counter", su.getRequests());
                return;
            case 4:
                send(response, addr, "counter", su.getAuthorized());
                return;
            case 5:
                send(response, addr, "counter", su.getCompleted());
                return;
            default:
                badRequest(response);
                return;
        }
    }

    /**
     * Get the ServiceUsage table.  This may return a cached version if one exists and has been previously used
     * within the past 3.5 seconds; otherwise, any cached version is discarded and a new copy is read from the
     * database.
     * <p>
     * The 3.5s cache timeout is restarted whenever the cached version of the table is used.  The only way to obtain
     * fresh data is to avoid issuing any queries for 3.5 seconds.  This is to work-around the problem that
     * SNMP queries are stateless and query for each cell of a table individually, and that the cells are addressed
     * by position in a virtual table rather than using unique identifiers.  If a cached table
     * were discarded and refreshed for being over too old, while a client was still in the process of
     * sending queries iterating the table, the client might receive inconsistent data if rows were added or
     * removed in the meantime since (say) row #3 might now correspond with what used to appear in row #4.
     *
     * @return a complete ServiceUsage table.  May be empty but never null.
     */
    private synchronized ServiceUsage[] getCurrentServiceUsage() throws FindException, RemoteException {
        long now = System.currentTimeMillis();
        if (serviceTable != null && now - serviceTableTime > 3500)
            serviceTable = null;

        if (serviceTable != null) {
            serviceTableTime = now;
            return serviceTable;
        }

        ServiceUsageManager statsManager = (ServiceUsageManager)applicationContext.getBean("serviceUsageManager");
        Map statsByOid = new HashMap();
        Collection collection = statsManager.getAll();
        for (Iterator i = collection.iterator(); i.hasNext();) {
            ServiceUsage serviceUsage = (ServiceUsage)i.next();
            statsByOid.put(new Long(serviceUsage.getServiceid()), serviceUsage);
        }

        ServiceAdmin serviceAdmin = (ServiceAdmin)applicationContext.getBean("serviceAdmin");
        EntityHeader[] headers = serviceAdmin.findAllPublishedServices();
        ServiceUsage[] fullTable = new ServiceUsage[headers.length];
        for (int i = 0; i < headers.length; i++) {
            EntityHeader header = headers[i];
            ServiceUsage su = (ServiceUsage)statsByOid.get(new Long(header.getOid()));
            if (su != null) {
                fullTable[i] = su;
            } else {
                fullTable[i] = new ServiceUsage();
                fullTable[i].setServiceid(header.getOid());
            }
            fullTable[i].setServiceName(header.getName());
        }

        serviceTable = fullTable;
        serviceTableTime = System.currentTimeMillis();

        return serviceTable;
    }

    private void badRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().close();
    }

    /**
     *
     * @param response
     * @param next  next MIB node, as a suffix which will be appended to BASE.  ie, ".1.2".
     * @param type
     * @param value
     * @throws IOException
     */
    private void send(HttpServletResponse response, String next, String type, String value) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(CONTENT_TYPE_SNMP);
        Writer out = response.getWriter();
        out.write(BASE + (next != null ? next : ""));
        out.write('\n');
        out.write(type != null && type.length() > 0 ? type : "string");
        out.write('\n');
        out.write(value);
        out.write('\n');
        out.flush();
        out.close();
    }

    /** Same as {@link #send(javax.servlet.http.HttpServletResponse,String,String,String)} but takes long value. */
    private void send(HttpServletResponse response, String next, String type, long value) throws IOException {
        send(response, next, type, String.valueOf(value));
    }

}
