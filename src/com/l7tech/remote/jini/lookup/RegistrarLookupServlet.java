/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.remote.jini.lookup;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import net.jini.config.*;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceRegistrar;
import org.apache.axis.encoding.Base64;
import org.apache.axis.transport.http.HTTPConstants;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.security.AccessControlException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The servlet that serializes the registrar obtained by the lookup
 * to the SSG manager console.
 * <p/>
 * The object received serves as a bootstrap <code>LookupLocator</code>
 * for the admin console.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistrarLookupServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(RegistrarLookupServlet.class.getName());

    private Configuration lookupConfig;
    private static final String LOOKUP_CONFIG = "com/l7tech/remote/jini/lookup/lookupservlet.config";
    private IdentityProviderConfigManager identityProviderConfigManager;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            URL url = getClass().getClassLoader().getResource(LOOKUP_CONFIG);
            if (url == null) {
                throw new
                  UnavailableException("Cannot locate configuration " + LOOKUP_CONFIG +
                  " in the current classpath");
            }
            String cf = url.toString();
            logger.info("Initializing servlet registrar lookup from " + cf);
            String[] configOptions = {cf};
            lookupConfig = ConfigurationProvider.getInstance(configOptions);
        } catch (ConfigurationNotFoundException e) {
            throw new ServletException(e);
        } catch (ConfigurationException e) {
            throw new ServletException(e);
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
        try {
            String authorizationHeader = request.getHeader(HTTPConstants.HEADER_AUTHORIZATION);
            if (authorizationHeader == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                logger.warning("Empty credentials received IP: " + request.getRemoteAddr());
                return;
            }
            authorizationHeader = authorizationHeader.trim();
            LoginCredentials creds = extractCredentials(authorizationHeader);

            User user = getConfigManager().getInternalIdentityProvider().authenticate(creds);
            if (user == null || !hasPermission(user)) {
                throw new AccessControlException(user.getName() + " does not have 'admin' privileges");
            }

            LookupLocator ll = getLookupLocator();
            logger.info("Obtained locator");
            ServiceRegistrar sr = ll.getRegistrar(5000);
            if (sr == null) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            // MIME type
            response.setContentType(DataFlavor.javaSerializedObjectMimeType);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(sr);
            out.flush();
            int size = bos.size();
            response.setContentLength(size);
            bos.writeTo(response.getOutputStream());
            response.getOutputStream().close();
            logger.info("Sending registrar as response to " + request.getRemoteAddr());
        } catch (BadCredentialsException e) {
            logger.log(Level.WARNING, "Bad credentials received", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Authentication exception", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (AuthenticationException e) {
            logger.log(Level.WARNING, "Authentication exception", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (AccessControlException e) {
            logger.log(Level.WARNING, "Authorization exception", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error obtaining the lookup locator", e);
            throw new ServletException(e);
        } finally {
            // todo: do this kind of things as a part of the service proxy (dyn proxy or similar)
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error closing the persistence context", e);

            }
        }
    }

    private boolean hasPermission(User user) {
        IdentityProvider provider = getConfigManager().getInternalIdentityProvider();
        GroupManager gman = provider.getGroupManager();
        try {
            for (Iterator i = gman.getGroupHeaders(user).iterator(); i.hasNext();) {
                EntityHeader grp = (EntityHeader)i.next();
                if (Group.ADMIN_GROUP_NAME.equals(grp.getName())) return true;
            }
            return false;
        } catch (FindException fe) {
            logger.log(Level.SEVERE, fe.getMessage(), fe);
            return false;
        }
    }

    /**
     * Get the service discovery manager.
     *
     * @return
     * @throws IOException
     * @throws net.jini.config.ConfigurationException
     *
     */
    private LookupLocator getLookupLocator()
      throws IOException, ConfigurationException {
        String entry = getClass().getName();
        try {
            LookupLocator locator =
              (LookupLocator)lookupConfig.getEntry(entry,
                "lookupLocator", LookupLocator.class);
            return locator;
        } catch (NoSuchEntryException e) {
            // use default
        }
        return new LookupLocator("jini://localhost");
    }

    /**
     * Extract the basic credentials from the string
     *
     * @param tokenBasic
     * @return
     * @throws BadCredentialsException
     */
    private LoginCredentials extractCredentials(String tokenBasic)
      throws BadCredentialsException {
        if (tokenBasic == null) {
            throw new BadCredentialsException("Null credentials passed");
        }

        if (!tokenBasic.startsWith("Basic ")) {
            throw new BadCredentialsException("Unknown header - expected basic");
        }
        String decodedCredentials = new String(Base64.decode(tokenBasic.substring(6)));
        if (decodedCredentials == null) {
            throw new BadCredentialsException("cannot decode basic header");
        }

        String login = null;
        String clearTextPasswd = null;

        int i = decodedCredentials.indexOf(':');
        if (i == -1) {
            throw new BadCredentialsException("invalid basic credentials " + tokenBasic);
        } else {
            login = decodedCredentials.substring(0, i);
            clearTextPasswd = decodedCredentials.substring(i + 1);
        }

        LoginCredentials creds = new LoginCredentials(login, clearTextPasswd.toCharArray(), null);
        return creds;
    }

    private IdentityProviderConfigManager getConfigManager() {
        if (identityProviderConfigManager != null) {
            return identityProviderConfigManager;
        }
        synchronized (this) {
            identityProviderConfigManager =
              (IdentityProviderConfigManager)Locator.getDefault().lookup(IdentityProviderConfigManager.class);
        }
        return identityProviderConfigManager;
    }
}
