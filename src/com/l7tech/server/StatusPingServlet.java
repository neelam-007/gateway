package com.l7tech.server;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A servlet that sends status information for use by failover.
 *
 * This servlet returns an xml file that contains server status information. The
 * failover code queries this servlet to assess whether the server is up and running
 * ok.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 6, 2004<br/>
 * $Id$
 *
 */
public class StatusPingServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Collection allStatuses = null;

        PersistenceContext context = null;
        try {
            context = PersistenceContext.getCurrent();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error getting persistence context. ", e);
            return;
        }
        try {
            allStatuses = ClusterInfoManager.getInstance().retrieveClusterStatus();
        } catch (FindException e) {
            logger.log(Level.SEVERE, "error getting status ", e);
            outputError(res, e.getMessage());
        } finally {
            context.close();
        }
        if (allStatuses == null || allStatuses.isEmpty()) {
            outputError(res, "can't get server status");
        }
        outputStatuses(res, allStatuses);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    private void outputStatuses(HttpServletResponse res, Collection statuses) throws IOException {
        res.setContentType("text/xml");
        res.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = res.getWriter();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<status>");
        for (Iterator i = statuses.iterator(); i.hasNext();) {
            ClusterNodeInfo nodeInfo = (ClusterNodeInfo)i.next();
            out.println("\t<server>");
                out.println("\t\t<name>" + nodeInfo.getName() + "</name>");
                out.println("\t\t<id>" + nodeInfo.getMac() + "</id>");
                out.println("\t\t<address>" + nodeInfo.getAddress() + "</address>");
                out.println("\t\t<uptime>" + humanReadableTime(nodeInfo.getUptime()) + "</uptime>");
                out.println("\t\t<avgload>" + nodeInfo.getAvgLoad() + "</avgload>");
                out.println("\t\t<lastUpdated>" +
                                  humanReadableTime(nodeInfo.getLastUpdateTimeStamp()) +
                                "</lastUpdated>");
            out.println("\t</server>");
        }
        out.println("</status>");
        out.close();
    }

    private void outputError(HttpServletResponse res, String error) throws IOException {
        res.setContentType("text/xml");
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        PrintWriter out = res.getWriter();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<status>");
            out.println("\t" + error);
        out.println("</status>");
        out.close();
    }

    private String humanReadableTime(long longtime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(longtime);
        String output = "" + cal.get(Calendar.YEAR) +
                        "/" + twoDigInt(cal.get(Calendar.MONTH)+1) +
                        "/" + twoDigInt(cal.get(Calendar.DATE)) +
                        " - " + twoDigInt(cal.get(Calendar.HOUR_OF_DAY)) +
                        ":" + twoDigInt(cal.get(Calendar.MINUTE)) +
                        ":" + twoDigInt(cal.get(Calendar.SECOND));
        return output;
    }

    private String twoDigInt(int val) {
        if (val < 10) return "0" + val;
        else return "" + val;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
