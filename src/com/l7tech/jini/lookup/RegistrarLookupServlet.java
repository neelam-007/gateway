/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.jini.lookup;

import net.jini.config.*;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceRegistrar;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The servlet that serializes the registrar obtained by the lookup
 * to the SSG manager console.
 * <p>
 * The object received serves as a bootstrap <code>LookupLocator</code>
 * for the admin console.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistrarLookupServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(RegistrarLookupServlet.class.getName());

    private Configuration lookupConfig;
    private static final String LOOKUP_CONFIG = "com/l7tech/jini/lookup/lookupservlet.config";

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            URL url = getClass().getClassLoader().getResource(LOOKUP_CONFIG);
            if (url == null) {
                throw new
                  UnavailableException(
                    "Cannot locate configuration "+LOOKUP_CONFIG+
                    " in the current classpath"
                  );
            }
            String cf = url.toString();
            logger.info("Initializing servlet registrar lookup from "+cf);
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
            LookupLocator ll = getLookupLocator();
            logger.info("Obtained locator");
            ServiceRegistrar sr = ll.getRegistrar(5000);
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
            logger.info("Wrote locator as response");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error obtaining the lookup locator", e);
            throw new ServletException(e);
        }
    }

    /**
     * Get the service discovery manager.
     *
     * @return
     * @throws IOException
     * @throws net.jini.config.ConfigurationException
     */
    protected LookupLocator getLookupLocator()
      throws IOException, ConfigurationException {
        String entry = getClass().getName();
        try {
            LookupLocator locator =
              (LookupLocator) lookupConfig.getEntry(
                entry,
                "lookupLocator", LookupLocator.class);
            return locator;
        } catch (NoSuchEntryException e) {
            // use default
        }
        return new LookupLocator("jini://localhost");
    }
}
