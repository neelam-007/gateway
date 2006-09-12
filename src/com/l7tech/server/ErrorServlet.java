package com.l7tech.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet for handling server / http errors.
 *
 * <p>This servlet has an optional initialization parameter "debug", this should
 * be set to "true" to turn on stack trace dumping in the error page.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class ErrorServlet extends HttpServlet {

    //- PUBLIC

    /**
     *
     */
    public ErrorServlet() {
        rb =  new ListResourceBundle(){
                public Object[][] getContents() {
                    return MESSAGES;
                }
              };

        debug = false;
    }

    /**
     *
     */
    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);

        if("true".equalsIgnoreCase(sc.getInitParameter("debug"))){
            debug = true;
        }
    }

    /**
     *
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doError(request, response);
    }

    /**
     *
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doError(request, response);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ErrorServlet.class.getName());

    /**
     *
     */
    private static final String ERROR_MARKUP = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n<html>\n<head><title>{0}</title></head>\n<body>\n{1}\n</body>\n</html>";

    // These MUST be set by the container
    private static final String REQUEST_ATTR_STATUS_CODE = "javax.servlet.error.status_code";//    java.lang.Integer
    private static final String REQUEST_ATTR_EXCEPT_TYPE = "javax.servlet.error.exception_type";// java.lang.Class
    private static final String REQUEST_ATTR_MESSAGE     = "javax.servlet.error.message";//        java.lang.String
    private static final String REQUEST_ATTR_EXCEPTION   = "javax.servlet.error.exception";//      java.lang.Throwable
    private static final String REQUEST_ATTR_REQUEST_URI = "javax.servlet.error.request_uri";//    java.lang.String
    private static final String REQUEST_ATTR_SERVLET     = "javax.servlet.error.servlet_name";//   java.lang.String

    /**
     * Resource bundle for error messages
     */
    private ResourceBundle rb;

    /**
     * If debug is on then show exception stack trace.
     */
    private boolean debug;

    /**
     *
     */
    private Object[] getFormatArgs(String title, String message) {
        return new String[]{title, message};
    }

    /**
     *
     */
    private void doError(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Throwable thrown = (Throwable) request.getAttribute(REQUEST_ATTR_EXCEPTION);
        if(!response.isCommitted()) {
            Integer statusCode = (Integer) request.getAttribute(REQUEST_ATTR_STATUS_CODE);
            String message = (String) request.getAttribute(REQUEST_ATTR_MESSAGE);

            if(statusCode==null && thrown==null) { // page invoked directly (no error)
                response.sendError(404);
                statusCode = new Integer(404);
            }

            response.setContentType("text/html");
            response.setCharacterEncoding("utf-8");
            PrintWriter pw = response.getWriter();
            pw.write(MessageFormat.format(ERROR_MARKUP, getFormatArgs(getTitle(statusCode), getMessage(statusCode,thrown))));
            pw.flush();
            pw.close();
        }
        else {
            logger.log(Level.WARNING, "Servlet error occured after response was committed.", thrown);
        }
    }

    /**
     *
     */
    private String getTitle(Integer code) {
        return code + " " + getCodeName(code);
    }

    /**
     *
     */
    private String getMessage(Integer code, Throwable thrown) {
        StringBuffer messageBuffer = new StringBuffer(256);

        messageBuffer.append("<h1>");
        messageBuffer.append(getCodeName(code));
        messageBuffer.append("</h1>\n");

        try {
            String details = rb.getString("http."+code);

            messageBuffer.append("<h3>");
            messageBuffer.append(details);
            messageBuffer.append("</h3>\n");

        }
        catch(MissingResourceException mre) {
        }

        if(debug && thrown !=null) {
            messageBuffer.append("<hr/>");
            messageBuffer.append("<pre>");
            messageBuffer.append(toString(thrown));
            messageBuffer.append("</pre>");
        }

        return messageBuffer.toString();
    }

    /**
     *
     */
    private String getCodeName(Integer code) {
        String name = "";
        try {
            name = rb.getString("code."+code);
        }
        catch(MissingResourceException mre) {
        }
        return name;
    }


    /**
     * Turn a throwable into a huge stacktrace.
     *
     * @param t the throwable
     * @return the formatted string
     */
    private String toString(Throwable t) {
        String formatted = null;

        if(t!=null) {
            StringWriter stackWriter = new StringWriter(512);
            t.printStackTrace(new PrintWriter(stackWriter));
            formatted = stackWriter.toString();
        }

        return formatted;
    }

    /**
     * Messages for error pages.
     *
     * We can externalize these later if required.
     */
    private static final Object[][] MESSAGES = {
        // HTTP Status code info
        {"code.400", "Bad Request"},
        {"code.401", "Unauthorized"},
        {"code.402", "Payment Required"},
        {"code.403", "Forbidden"},
        {"code.404", "Not Found"},
        {"code.405", "Method Not Allowed"},
        {"code.406", "Not Acceptable"},
        {"code.407", "Proxy Authentication Required"},
        {"code.408", "Request Timeout"},
        {"code.409", "Conflict"},
        {"code.410", "Gone"},
        {"code.411", "Length Required"},
        {"code.412", "Precondition Failed"},
        {"code.413", "Request Entity Too Large"},
        {"code.414", "Request-URI Too Long"},
        {"code.415", "Unsupported Media Type"},
        {"code.416", "Requested Range Not Satisfiable"},
        {"code.417", "Expectation Failed"},
        {"code.422", "Unprocessable Entity"},
        {"code.423", "Locked"},
        {"code.500", "Internal Server Error"},
        {"code.501", "Not Implemented"},
        {"code.502", "Bad Gateway"},
        {"code.503", "Service Unavailable"},
        {"code.504", "Gateway Timeout"},
        {"code.505", "HTTP Version Not Supported"},
        {"code.507", "Insufficient Storage"},

        // Error message details
        {"http.400", "The request sent by the client was syntactically incorrect."},
        {"http.401", "This request requires HTTP authentication."},
        {"http.402", "Payment is required for access to this resource."},
        {"http.403", "Access to the specified resource has been forbidden."},
        {"http.404", "The requested resource is not available."},
        {"http.405", "The specified HTTP method is not allowed for the requested resource."},
        {"http.406", "The resource identified by this request is only capable of generating responses with characteristics not acceptable according to the request \"accept\" headers."},
        {"http.407", "The client must first authenticate itself with the proxy."},
        {"http.408", "The client did not produce a request within the time that the server was prepared to wait."},
        {"http.409", "The request could not be completed due to a conflict with the current state of the resource."},
        {"http.410", "The requested resource is no longer available, and no forwarding address is known."},
        {"http.411", "This request cannot be handled without a defined content length."},
        {"http.412", "A specified precondition has failed for this request."},
        {"http.413", "The request entity is larger than the server is willing or able to process."},
        {"http.414", "The server refused this request because the request URI was too long."},
        {"http.415", "The server refused this request because the request entity is in a format not supported by the requested resource for the requested method."},
        {"http.416", "The requested byte range cannot be satisfied."},
        {"http.417", "The expectation given in the \"Expect\" request header could not be fulfilled."},
        {"http.422", "The server understood the content type and syntax of the request but was unable to process the contained instruction."},
        {"http.423", "The source or destination resource of a method is locked."},
        {"http.500", "The server encountered an internal error that prevented it from fulfilling this request."},
        {"http.501", "The server does not support the functionality needed to fulfill this request."},
        {"http.502", "This server received an invalid response from a server it consulted when acting as a proxy or gateway."},
        {"http.503", "The requested service is not currently available."},
        {"http.504", "The server received a timeout from an upstream server while acting as a gateway or proxy."},
        {"http.505", "The server does not support the requested HTTP protocol version."},
        {"http.507", "The resource does not have sufficient space to record the state of the resource after execution of this method."}
    };
}
