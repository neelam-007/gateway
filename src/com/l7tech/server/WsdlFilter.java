package com.l7tech.server;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;
import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import com.l7tech.common.http.HttpConstants;

/**
 * Filter that intercepts WSDL requests that would otherwise go to the message processor.
 *
 * <p>This filter is registered for any "/xml" requests to provide support for
 * ?wsdl WSDLs.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WsdlFilter implements Filter {

    /**
     * Get ServiceManager from the application context.
     *
     * @param filterConfig The configuration for the filter
     * @throws ServletException if an error occurs
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        // config
        String forwardUriVal = filterConfig.getInitParameter(PARAM_FORWARD_URI);
        if(forwardUriVal==null) {
            logger.config("Using default wsdl forward uri.");
            forwardUriVal = DEFAULT_FORWARD_URI;
        }
        wsdlForwardUri = forwardUriVal;
        logger.config("Wsdl forward uri is '"+wsdlForwardUri+"'.");
    }

    /**
     * Nothing to destroy.
     */
    public void destroy() {
    }

    /**
     * If the request is a GET and the query string is "wsdl" then attempt to resolve the url to a service.
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if(servletRequest instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            if(HttpConstants.METHOD_GET.equals(httpServletRequest.getMethod())) {
                String qs = httpServletRequest.getQueryString();
                if(qs!=null && QUERYSTRING_WSDL.equalsIgnoreCase(qs)) {
                    // Initialize processing context
                    String forwardUri = null;
                    try {
                        forwardUri = MessageFormat.format(wsdlForwardUri, new Object[]{httpServletRequest.getRequestURI()});
                    }
                    catch(IllegalArgumentException iae) {
                        logger.log(Level.WARNING, "Error generating wsdl forwarding uri.", iae);
                    }

                    if(forwardUri!=null) {
                        if(logger.isLoggable(Level.INFO)) {
                            logger.log(Level.INFO, "Forwarding WSDL request to '"+forwardUri+"'.");
                        }

                        RequestDispatcher rd =
                          servletRequest.getRequestDispatcher(forwardUri);
                        rd.forward(servletRequest, servletResponse);

                        return;
                    }
                }
            }
        }

        if(filterChain!=null) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WsdlFilter.class.getName());
    private static final String QUERYSTRING_WSDL = "wsdl";
    private static final String PARAM_FORWARD_URI = "wsdl-forward-uri";
    private static final String DEFAULT_FORWARD_URI = "/ssg/wsdl?uri=";

    private String wsdlForwardUri;

}
