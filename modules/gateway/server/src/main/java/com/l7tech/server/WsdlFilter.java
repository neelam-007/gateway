package com.l7tech.server;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
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

    //- PUBLIC

    /**
     * Get ServiceManager from the application context.
     *
     * @param filterConfig The configuration for the filter
     * @throws ServletException if an error occurs
     */
    @Override
    public void init( final FilterConfig filterConfig ) throws ServletException {
        // config
        String pprefixes = filterConfig.getInitParameter(PARAM_PASSTHROUGH_PREFIXES);
        if ( pprefixes == null || pprefixes.length() == 0 ) pprefixes = DEFAULT_PASSTHROUGH_PREFIXES;

        String forwardUriVal = filterConfig.getInitParameter(PARAM_FORWARD_URI);
        if(forwardUriVal==null) {
            logger.config("Using default wsdl forward uri.");
            forwardUriVal = DEFAULT_FORWARD_URI;
        }

        serverConfig = (ServerConfig) filterConfig.getServletContext().getAttribute("serverConfig");
        passthroughPrefixes = pprefixes.split(",\\s*");
        wsdlForwardUri = forwardUriVal;

        logger.config("Passthrough prefixes " + Arrays.asList(passthroughPrefixes));
        logger.config("Wsdl forward uri is '" + wsdlForwardUri + "'.");
    }

    /**
     * Nothing to destroy.
     */
    @Override
    public void destroy() {
    }

    /**
     * If the request is a GET and the query string is "wsdl" then attempt to resolve the url to a service.
     */
    @Override
    public void doFilter( final ServletRequest servletRequest,
                          final ServletResponse servletResponse,
                          final FilterChain filterChain) throws IOException, ServletException {
        if(isEnabled() && servletRequest instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            if(HttpConstants.METHOD_GET.equals(httpServletRequest.getMethod()) &&
               !isPassThrough(httpServletRequest.getRequestURI() )) {
                String qs = httpServletRequest.getQueryString();
                if( qs!=null && (QUERYSTRING_WSDL.equalsIgnoreCase(qs) || qs.toLowerCase().startsWith(QUERYSTRING_WSDL_PLUS))) {
                    // Initialize processing context
                    String forwardUri = null;
                    try {
                        forwardUri = MessageFormat.format(wsdlForwardUri, httpServletRequest.getRequestURI());
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
    private static final String QUERYSTRING_WSDL_PLUS = "wsdl&";
    public static final String PARAM_PASSTHROUGH_PREFIXES = "passthroughPrefixes";
    public static final String DEFAULT_PASSTHROUGH_PREFIXES = "/ssg/";
    private static final String PARAM_FORWARD_URI = "wsdl-forward-uri";
    private static final String DEFAULT_FORWARD_URI = "/ssg/wsdl?uri=";

    private ServerConfig serverConfig;
    private String wsdlForwardUri;
    private String[] passthroughPrefixes;  

    private boolean isEnabled() {
        return serverConfig.getBooleanPropertyCached("wsdlQuery", false, 60000);
    }

    private boolean isPassThrough( final String uri ) {
        boolean passThrough = false;

        if ( uri != null ) {
            for ( String prefix : passthroughPrefixes  ) {
                if ( uri.startsWith( prefix ) ) {
                    passThrough = true;
                    break;
                }
            }
        }

        return passThrough;
    }
}
