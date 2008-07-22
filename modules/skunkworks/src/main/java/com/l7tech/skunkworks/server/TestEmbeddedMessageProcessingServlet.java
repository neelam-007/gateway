package com.l7tech.skunkworks.server;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.LifecycleException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.util.logging.Logger;
import java.util.*;

import com.l7tech.util.ExceptionUtils;

/**
 *
 */
public class TestEmbeddedMessageProcessingServlet extends HttpServlet {
    protected static final Logger logger = Logger.getLogger(TestEmbeddedMessageProcessingServlet.class.getName());
    private String stylesheet = "<style type=\"text/css\">\n" +
                                "table {\n" +
                                "\tborder-width: thin thin thin thin;\n" +
                                "\tborder-spacing: 0px;\n" +
                                "\tborder-style: solid solid solid solid;\n" +
                                "\tborder-color: black black black black;\n" +
                                "\tborder-collapse: separate;\n" +
                                "\tbackground-color: white;\n" +
                                "}\n" +
                                "table th {\n" +
                                "\tborder-width: 1px 1px 1px 1px;\n" +
                                "\tpadding: 3px 3px 3px 3px;\n" +
                                "\tborder-style: solid solid solid solid;\n" +
                                "\tborder-color: black black black black;\n" +
                                "\tbackground-color: white;\n" +
                                "\t-moz-border-radius: 0px 0px 0px 0px;\n" +
                                "}\n" +
                                "table td {\n" +
                                "\tborder-width: 1px 1px 1px 1px;\n" +
                                "\tpadding: 3px 3px 3px 3px;\n" +
                                "\tborder-style: solid solid solid solid;\n" +
                                "\tborder-color: black black black black;\n" +
                                "\tbackground-color: white;\n" +
                                "\t-moz-border-radius: 0px 0px 0px 0px;\n" +
                                "}\n" +
                                "</style>";
    private static final String delPrefix = "DeletePort";


    public void init() throws ServletException {
        logger.info("servlet init() called");
    }


    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("servlet service() called");
        try {
            doService(req, resp);
        } catch (Exception e) {
            resp.setStatus(500);
            resp.setContentType("text/plain");
            PrintStream out = new PrintStream(new BufferedOutputStream(resp.getOutputStream()));
            try {
                out.println("<pre>\nError: " + ExceptionUtils.getMessage(e));
                e.printStackTrace(out);
            } finally {
                out.flush();
            }
        }
    }

    protected void doService(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(200);
        resp.setContentType("text/html");

        PrintStream out = new PrintStream(new BufferedOutputStream(resp.getOutputStream()));
        try {
            out.println("<html><head><title>Hi from embedded tomcat</title>");
            out.println(stylesheet);
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Hi from embedded tomcat</h1>");
            out.println("<p>This Tomcat instance is currently running embedded from within IDEA.");
            //noinspection unchecked
            Map<String, String[]> params = (Map<String, String[]>)req.getParameterMap();
            if (!params.isEmpty()) {
                //dumpFormParams(out, params);

                String command = getCommandParameter(params);

                if (command != null) {
                    out.println("<h2>Running command: " + command + "</h2>");

                    if (command.equals("StartConnector")) {
                        int port = getIntParameter(params, "port");
                        if (port == 9080) throw new IllegalArgumentException("This demo won't let you remove create a duplicate of the default connector");
                        String ssl = getStringParameter(params, "ssl");
                        if ("usessl".equalsIgnoreCase(ssl))
                            TestEmbeddedServerMain.getInstance().addHttpsConnector(port, true);
                        else
                            TestEmbeddedServerMain.getInstance().addHttpConnector(port);
                    } else if (command.startsWith(delPrefix)) {
                        int port = Integer.parseInt(command.substring(delPrefix.length()));
                        if (port == 9080) throw new IllegalArgumentException("This demo won't let you remove the default connector on port 9080,\neven though doing so works as expected (even if it results in no connectors at all),\nsince there's currently no way to remotely undo it if you remove the last connector");
                        TestEmbeddedServerMain.getInstance().removeHttpConnector(port);
                        out.println("<p>It may take a minute or two for the connector to stop accepting connections.");
                    }
                }
            }

            out.println("<p><form name='f' method='post' action='test'>");
            out.println("Current connectors:<p>");

            final List<Connector> connectors = new ArrayList<Connector>(TestEmbeddedServerMain.getInstance().getConnectors());
            makeTable(out, new String[] { "Name", "Protocol", "Scheme", "Port", "&nbsp;" }, new Iterator<String[]>() {
                public void remove() {}
                public boolean hasNext() { return !connectors.isEmpty(); }
                public String[] next() {
                    Connector connector = connectors.remove(0);
                    final int port = connector.getPort();
                    return new String[] {
                            connector.getObjectName().toString(),
                            connector.getProtocol(),
                            connector.getScheme(),
                            Integer.toString(port),
                            port == 9080 ? "&nbsp;" : "<input type='submit' name='DeletePort" + connector.getPort() + "' value='Destroy'/>"
                    };
                }
            });

            out.println("<p>Add new connector on port: <input type='text' name='port'/> <input type='checkbox' name='ssl' value='usessl'/>SSL <input type='submit' name='StartConnector' value='Start Connector'/>");
            out.println("</form>");

            out.println("</body>");
            out.println("</html>");
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            out.flush();
        }
    }

    private String getCommandParameter(Map<String, String[]> params) {
        String cmd = getStringParameter(params, "StartConnector");
        if (cmd != null) return "StartConnector";

        Set<String> names = params.keySet();
        for (String name : names) {
            if (name.startsWith(delPrefix) &&
                name.length() > delPrefix.length() &&
                "Destroy".equals(getStringParameter(params, name))) {
                return name;
            }
        }

        return null;
    }

    private String getStringParameter(Map<String, String[]> params, String name) {
        String[] values = params.get(name);
        if (values != null && values.length > 0) return values[0];
        return null;
    }

    private int getIntParameter(Map<String, String[]> params, String name) {
        String[] values = params.get(name);
        if (values != null && values.length > 0) return Integer.parseInt(values[0]);
        throw new IllegalArgumentException("not present or not an integer: " + name);
    }

    private void dumpFormParams(PrintStream out, Map<String, String[]> params) {
        out.println("Form parameters:<p>");
        List<String[]> rows = new ArrayList<String[]>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            final String name = entry.getKey();
            final String[] values = entry.getValue();
            final String value = values == null || values.length < 1 ? "<null>" : values[0];
            rows.add(new String[] { name, value } );
        }
        makeTable(out, new String[] { "Name", "Value" }, rows.iterator());
    }

    private void makeTable(PrintStream out, String[] headers, Iterator<String[]> rows) {
        out.println("<table border=0>");
        makeRow(out, headers, true);
        while (rows.hasNext()) {
            String[] row = rows.next();
            makeRow(out, row, false);
        }
        out.println("</table>");
    }

    private void makeRow(PrintStream out, String[] row, boolean isHeader) {
        out.print("<tr>");
        for (String cell : row) {
            out.print(isHeader ? "<th>" : "<td>");
            out.print(cell);
            out.print(isHeader ? "</th>" : "</td>");
        }
        out.println("</tr>");
    }
}
